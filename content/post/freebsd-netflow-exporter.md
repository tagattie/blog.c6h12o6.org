+++
title = "NetFlowを用いてFreeBSDマシンのトラフィックを可視化する(Flow Exporter編)"
date = "2018-08-02T20:56:00+09:00"
lastmod = "2018-08-06T18:58:00+09:00"
categories = ["FreeBSD"]
tags = ["freebsd", "netflow", "network", "traffic", "monitor", "statistics", "export", "netgraph"]
+++

[まえがき](/post/freebsd-netflow-intro/)では、システム監視ソフトウェアの[Munin](http://munin-monitoring.org/)を簡単に紹介しました。また、ネットワーク通信の詳細な統計をとる技術であるNetFlowについても少し述べました。今回からは、NetFlowによるネットワークの統計情報生成から、本情報を可視化するまでの流れを説明していきます。

本記事では、FreeBSDマシンを対象にNetFlow exporter、すなわちNetFlowによるトラフィック統計情報生成および送信機能(下図青色部分)、をセットアップする手順を説明します。

![FreeBSD - NetFlow - Flow Exporter](/img/freebsd/freebsd-netflow-exporter.png)

まえがきにも述べましたが、FreeBSDカーネルにはNetFlowデータを生成する機能が組み込まれています。したがって、NetFlow exporterを設定するにあたり、追加のソフトウェアをインストールする必要はありません。NetFlowデータ生成機能は、[netgraph](https://people.freebsd.org/~julian/netgraph.html)と呼ばれるフレームワークにそって動作する`ng_netflow(4)`カーネルモジュールが提供します。

### Netgraphとは
NetFlow exporterを設定する前に、少しだけnetgraphフレームワークについて見ておきましょう。

マニュアル(`man 4 netgraph`)によると、netgraphは「多様なネットワーク機能を実現するオブジェクト(グラフノード)を実装するための、統一的かつモジュール化された方法を提供する」とあります。

Netgraphフレームワークでは、グラフのノードに相当するオブジェクトが単一のネットワーク機能を実現します。ノードにはフックと呼ばれる「手」が一つ以上備わっています。ノード同士が手をつなぐ、すなわちノードに備わったフックとフックとを接続することにより、グラフのエッジが構成されます。このようにして、順次ノードをエッジで連結していくことで、より複雑なネットワーク機能を実現するわけです。

Unix的な考え方になじみのあるかたであれば、単一かつシンプルな機能を実現するコマンドをパイプで接続して、より複雑な処理を実現していくのと同様だというとわかりやすいでしょうか。コマンドに相当するのがノード、パイプなどのプロセス間通信路に相当するのがフック同士を結んだエッジということになります。

抽象的な話ばかりではうんざりしてしまいますね。netgraphのさらなる詳細についてはマニュアルを参照いただくとして、次からはNetFlow exporterを設定する具体的な手順を説明していきます。

### Netgraphを用いたNetFlow Exporter
netgraphフレームワークベースのNetFlowモジュールですが、本フレームワークにもとづいて動作するモジュール群を統一的に制御するコマンドが`ngctl(8)`です。NetFlow exporterを設定する手順をひとことで言うと、`ngctl`コマンド(とそのサブコマンド群)を駆使してNetFlow exporterの機能を実現するグラフを作り上げる、ということになります。

使用する具体的なコマンドについては、以下の二つの記事に説明があります。

- [CONFIGURE IN KERNEL NETFLOW EXPORT WITH netgraph(4) (The FreeBSD Forums)](https://forums.freebsd.org/threads/howto-monitor-network-traffic-with-netflow-nfdump-nfsen-on-freebsd.49724/#post-277772)
- [NetFlow v9 Exporting from FreeBSD routers/firewalls (Dan's Blog)](https://www.dan.me.uk/blog/2016/06/01/netflow-v9-exporting-from-freebsd-routersfirewalls/)

記事をご覧になっていかがでしょうか? スムーズに理解できたでしょうか?

わたしはというと、カーネルモジュールをロードするところはOKです。しかし、`ngctl`コマンドがいけません。数個のサブコマンドを実行するのですが、その字面を見ただけではどんなグラフができるのかイメージできませんでした。

そこで、なんとか理解を進めるために、サブコマンドの実行にともなうグラフの変化をステップごとに図にしてみました。以下、作成した図にもとづいて、手順を一つづつ説明していきます。また、本記事の最後に[自動実行用のスクリプト](#自動起動の設定)を用意していますので、詳細な説明が不要な場合はそちらにジャンプしてください。

### カーネルモジュールのロード
まず、関連するカーネルモジュールをロードします。今回は、イーサネットインターフェイスに対応する`ng_ether(4)`、NetFlowによる統計情報を生成する`ng_netflow(4)`、および生成された統計情報をネットワーク経由で送信する`ng_ksocket(4)`の各モジュールを使用します。

以下のコマンドを実行してカーネルモジュールをロードします。

``` shell
kldload netgraph ng_ether ng_netflow ng_ksocket
```

### `ngctl`コマンドを用いたグラフ構築
次からは、`ngctl`コマンドを用いてグラフを作成していきます。

**注**: 以下のコマンドを一つづつ実行する場合、Step 1の`mkpeer`コマンドを実行すると同時にネットワーク**接続が切れます**。したがって、(ssh経由などではなく)**コンソールで実行**するようにしてください。

#### Step 0 (初期グラフ)
実は、カーネルモジュールをロードした時点で自動的に作成されるnetgraphノードがあります。

マシンが備えるイーサネットインターフェイスに対応する`ng_ether`タイプのノードが自動的に作成されます(下図)。

本ノードの名前はインターフェイス名と同じ(下図の場合は`em0`)になります。また、本ノードには`lower`, `upper`, および`orphans`という三つのフックがあります。(`orphans`は今回まったく使用しませんので、忘れていただいても大丈夫です。)

![FreeBSD - NetFlow Exporter - Step 0](/img/freebsd/freebsd-netflow-netgraph-step-0.png)

この時点では、それぞれのフックに何もノードがつながっていませんので、送受信されるパケットは本ノードを素通りしていきます。

#### Step 1 (`ng_netflow`タイプノードの作成)
``` shell-session
# ngctl
Available commands:
(snip)
+ mkpeer em0: netflow lower iface0
```

では、`ngctl`コマンドを使ってグラフを作成していきましょう。本コマンドを実行すると、使用可能なサブコマンドのリストと`+`プロンプトが表示されます。`+`プロンプトのところで、実行したいサブコマンドを入力していきます。

まず、`mkpeer`コマンドを用いて、NetFlow統計情報を生成する`ng_netflow`タイプのノードを生成します。この際、ノード`em0`の`lower`フックと新規生成ノードの`iface0`フックとを接続します(下図)。

![FreeBSD - NetFlow Exporter - Step 1](/img/freebsd/freebsd-netflow-netgraph-step-1.png)

この時点ではノード`em0`の`upper`フックと新規作成ノードの`out0`フックが接続されていないので、カーネルとイーサネットインターフェイスのあいだでパケットが流れない(= 接続が切れている)状態になっていることに注意してください。

#### Step 2 (`ng_netflow`タイプノードへ名前付け)
``` shell-session
+ name em0:lower netflow
```

作成しただけの状態ではノードに名前がついていません。そこで、`name`コマンドを用いてStep 1で作成したノード、つまりノード`em0`の`lower`フックにつながっているノードに`netflow`という名前をつけます(下図)。

注: `netflow`という名前をつけましたが、これは任意の名前でかまいません。

![FreeBSD - NetFlow Exporter - Step 2](/img/freebsd/freebsd-netflow-netgraph-step-2.png)

#### Step 3 (`em0`と`netflow`のフックの接続)
``` shell-session
+ connect em0: netflow: upper out0
```

次に、`connect`コマンドを用いて、ノード`em0`の`upper`フックとノード`netflow`の`out0`フックとを接続します(下図)。

![FreeBSD - NetFlow Exporter - Step 3](/img/freebsd/freebsd-netflow-netgraph-step-3.png)

この時点で、カーネルとイーサネットインターフェイスの間で再びパケットが流れるようになりました。

ここまでで、グラフノードの作成、ノードへの名前付け、フック同士の接続という基本的なコマンドが出てきましたね。これで、送受信されるパケットがすべてノード`netflow`を通るようになり、NetFlow統計情報の生成が始まりました。

次は、ノード`netflow`が生成したNetFlow統計情報を外部に送信するためのノードを作成しましょう。

#### Step 4 (`ng_ksocket`タイプノードの作成)
``` shell-session
+ mkpeer netflow: ksocket export9 inet/dgram/udp
```

`netflow`の時と同様、`mkpeer`コマンドを用いて今度は`ng_ksocket`タイプのノードを生成します。この際、ノード`netflow`の`export9`フックと新規生成ノードの`inet/dgram/udp`フックとを接続します(下図)。

**注**: `ng_netflow`タイプのノードには、NetFlow統計情報を外部に送信するためのフックとして`export`および`export9`の二つが備わっています。`export`はNetFlow v5データの送信、`export9`はNetFlow v9データの送信に用います。本記事ではIPv6のトラフィックに関する統計情報も取りたいので、NetFlow v9を送信する`export9`フックを使用しました。

![FreeBSD - NetFlow Exporter - Step 4](/img/freebsd/freebsd-netflow-netgraph-step-4.png)

#### Step 5 (`ng_ksocket`タイプノードへ名前付け)
``` shell-session
+ name netflow:export9 nfsock
```

作成しただけではノードに名前がついていませんので、ノード`netflow`のときと同じく、`name`コマンドを用いてStep 4で作成したノード、つまりノード`netflow`の`export9`フックにつながっているノードに`nfsock`という名前をつけます(下図)。

**注**: 実行するコマンドはあと一つだけなので、わざわざ`ng_ksocket`タイプのノードに名前をつける必要は実はありません。しかし、名無しというのもなんとなく気持ち悪いので、とりあえず名前をつけることにしました。

![FreeBSD - NetFlow Exporter - Step 5](/img/freebsd/freebsd-netflow-netgraph-step-5.png)

#### Step 6 (Flow Collectorへデータ送信開始)
``` shell-session
+ msg nfsock: connect inet/<ipaddress>:<port>
```

最後のコマンドです。`msg`コマンドは指定したノードに対して、指定した制御メッセージを送るコマンドです。送る制御メッセージは`connect`です。本メッセージは`inet/dgram/udp`フックを通じて受け取ったデータ、すなわちNetFlow統計データを、メッセージにおいて指定したIPアドレスおよびポート宛に送信するよう指示します(下図)。

![FreeBSD - NetFlow Exporter - Step 6](/img/freebsd/freebsd-netflow-netgraph-step-6.png)

まとめとして、これまでに述べた各ステップをアニメーション化してみました。(すみません、単にAnimated PNGを使ってみたかったんです。)

![FreeBSD - NetFlow Exporter - Slide](/img/freebsd/freebsd-netflow-netgraph.png)

### 自動起動の設定
はぁ、長かったですね。あとから見てみると、そんなに複雑なことをやっているわけではないことがわかりますね。ステップごとに図を描いてみることでようやく理解できました。

さて、以上でNetFlow exporterの設定ができたわけですが、FreeBSDを起動するたびにいちいち手動で上記の手続きを行なうのはとてもめんどうです。そこで、マシンの起動時にNetFlow exporterが自動的に設定、起動されるようにスクリプト化を行ないました。(といっても、ほとんど[このスクリプト](https://github.com/woodsb02/conf/blob/master/freenas/rc.d/ng_netflow)をコピーしただけですが。)

- [FreeBSD-Netflow-Export - A script for automatically starting a Netflow exporter on boot for FreeBSD](https://github.com/tagattie/FreeBSD-Netflow-Export)

スクリプトを使用するには、まず適当なディレクトリに上記のリポジトリをクローンしてください。その後、`ng_netflow`ファイルを`/usr/local/etc/rc.d`以下にコピーします。

``` shell
cd <適当なディレクトリ>
git clone https://github.com/tagattie/FreeBSD-Netflow-Export.git
cp ./FreeBSD-Netflow-Export/ng_netflow /usr/local/etc/rc.d
```

その後、以下の各コマンドを実行して、自動起動設定、NetFlow統計対象のネットワークインターフェイス指定、NetFlow collectorのIPアドレス、およびポート番号(オプション)を指定します。

``` shell
sysrc ng_netflow_enable=YES              # システム起動時にNetFlow exporterを自動起動
sysrc ng_netflow_interface=em0           # NetFlow統計生成対象のネットワークインターフェイス
sysrc ng_netflow_collect_addr=X.X.X.X    # NetFlow collectorのIPアドレス
sysrc ng_netflow_collect_port=XXXX       # デフォルト(4444)以外を使用する場合のみ
```

あとはFreeBSDを再起動するか、`service ng_netflow start`を実行すればOKです。

[次回の記事](/post/freebsd-netflow-collector/)では、NetFlowデータを受信し蓄積するFlow Collectorの設定手順について説明します。

### 参考文献
1. All About Netgraph, https://people.freebsd.org/~julian/netgraph.html
1. CONFIGURE IN KERNEL NETFLOW EXPORT WITH netgraph(4), https://forums.freebsd.org/threads/howto-monitor-network-traffic-with-netflow-nfdump-nfsen-on-freebsd.49724/#post-277772
1. NetFlow v9 Exporting from FreeBSD routers/firewalls, https://www.dan.me.uk/blog/2016/06/01/netflow-v9-exporting-from-freebsd-routersfirewalls/
