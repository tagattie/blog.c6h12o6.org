+++
title = "NetFlowを用いてFreeBSDマシンのトラフィックを可視化する(Flow Collector編)"
date = "2018-08-06T18:56:00+09:00"
categories = ["FreeBSD"]
tags = ["freebsd", "netflow", "network", "traffic", "monitor", "statistics", "collect", "nfdump", "nfsen"]
+++

[Flow Exporter編](/post/freebsd-netflow-exporter/)では、FreeBSDのカーネルモジュールを活用して、NetFlow exporterをセットアップする手順を説明しました。本記事では、flow exporterからの統計データを受信し蓄積する、NetFlow collectorのインストールおよび設定について見ていきます(下図青色部分)。

![FreeBSD - NetFlow - Flow Collector](/img/freebsd/freebsd-netflow-collector.png)

NetFlowはもともとCisco System社が開発した、ルータ向けのトラフィック統計技術です。したがって、本技術のターゲットとなるユーザはおもにネットワークサービス事業者、あるいは一般企業の基幹ネットワークの管理者であると思われます。このせいか、NetFlow collector/visualizerには商用製品が多く、一般消費者がフリーで使用できるものは少ないという印象を持ちました。

「無料かつオープンソース」という条件をつけて調査した結果、flow collector/visualizerをオープンソース・ソフトウェアで実現するには、以下の二つのパターンがありそうです。

- 汎用の時系列データ処理スタックを活用

    汎用の時系列データプラットフォームとしては、[Elasticスタック](https://www.elastic.co/products)や[TICKスタック](https://www.influxdata.com/time-series-platform/)が有名です。

    Elasticスタックに含まれるデータコレクタLogstashはNetFlowデータを受信できます。いっぽう、TICKスタックのデータコレクタであるTelegrafはNetFlowをサポートしていないようですが、[Fluentd](https://www.fluentd.org/)とNetFlow/InfluxDB pluginを活用することで、InfluxDBへNetFlowデータを投入できたという事例が報告されています。
    - [ELKではじめるお手軽Flowコレクター (外山さん@創風システム)](http://enog.jp/archives/1337)
    - [GrafanaによるNetFlow v9トラフィック分析 (Qiita / @ishizaghi)](https://qiita.com/ishizaghi/items/b0ecd9fa50e503362165)

- 専用ソフトウェアを使用

    NetFlow専用のソフトウェアにはやはり商用製品が多いという印象です。オープンソースの実装としては、NetFlowデータを受信、蓄積するNFDUMP、および蓄積したデータを可視化するNfSenというものがあります。
    - [NFDUMP (GitHub / phaag)](https://github.com/phaag/nfdump)
    - [NfSen (SourceForge)](http://nfsen.sourceforge.net/)

わたし自身がNetFlowに関してまったくの初心者であること、また以下のガイド記事で詳しく設定手順が解説されていることから、今回はNetFlow専用のNFDUMP/NfSenを用いてNetFlow collector/visualizerを実現することにしました。

- [CONFIGURE NETFLOW STORAGE AND DISPLAY WITH NFDUMP AND NfSen (The FreeBSD Forum)](https://forums.freebsd.org/threads/howto-monitor-network-traffic-with-netflow-nfdump-nfsen-on-freebsd.49724/#post-277774)

汎用プラットフォームを用いるほうが可視化の際の柔軟性は高いかもしれませんが、設定はより複雑になりそうな気がします。

では、上記記事を参考にしながらNFDUMP/NfSenのインストール、および設定を行なっていきます。(というか上記記事が詳しいので、ほぼなぞることになります。)

### インストール
まずは、必要なパッケージのインストールからです。`nfsen`および`nfdump`パッケージをインストールします。(`nfdump`パッケージは`nfsen`パッケージの依存関係としてインストールされますので、あえて指定しなくてもOKです。)

``` shell
pkg install nfsen nfdump
```

### NfSenの設定(必須)
`nfsen`パッケージをインストールすると、設定ファイル(`nfsen.conf`)が`/usr/local/etc`以下に配置されます。ほとんど設定変更の必要はないのですが、一か所(NetFlowデータソース)だけは必ず環境に合わせた設定を行なう必要があります。

- `/usr/local/etc/nfsen.conf`
    - L150

        NetFlowデータソースを指定します。以下の例では、データソースの識別名として`example`、本識別名に対応するNetFlowデータを待ち受けるポート番号として`4444`を指定しました。(ポート番号は、flow exporterで指定した送信先ポート番号と合わせる必要があることに注意してください。)
	
        データソースが複数ある場合は、同様のフォーマットをカンマ(`,`)で区切って列挙します。異なるデータソースには異なるポート番号を使用する必要があります。

        {{< highlight perl >}}
%sources = (
  'example' => { 'port' => '4444', 'col' => '#0000ff', 'type' => 'netflow' },
);
{{< /highlight >}}

### NfSenの設定(オプション)
NfSenデータのディレクトリ構成をFreeBSDの標準的な構成に合わせたい場合に、本項の設定を行なってください。デフォルト設定で問題ない場合は、本項をとばして[サービス起動](#サービス起動)へ進みます。

注: FreeBSDの標準的なルールでは、アプリケーションデータを格納するのは`/var`以下です。いっぽう、NfSenのデフォルト設定では`/usr/local/var`以下を使うようになっています。

- `/usr/local/etc/nfsen.conf`を編集
    - L46

        NfSenのデータディレクトリを変更します。

        {{< highlight perl >}}
$VARDIR = "/var/nfsen";
{{< /highlight >}}

    - L49
    
        プロセスIDファイルを格納するディレクトリを変更します。

        {{< highlight perl >}}
$PIDDIR = "/var/run/nfsen";
{{< /highlight >}}

- `/usr/local/etc/rc.d/nfsen`を編集
    - L35, L38
    
        自動起動スクリプトの中にデフォルトディレクトリがハードコーディングされている部分があります。これを標準的なディレクトリに変更します。

        {{< highlight shell >}}
/usr/local/var/nfsen/profiles-stat/live/profile.dat
{{< /highlight >}}
        を以下に変更:
	
        {{< highlight shell >}}
/var/nfsen/profiles-stat/live/profile.dat
{{< /highlight >}}

- ディレクトリの移動とオーナ・グループ修正

    インストール時に作成されたディレクトリを、上記で変更した(FreeBSDの標準的)構成に合うように移動します。また、NfSenが動作するユーザ`www`での読み書きができるように、ディレクトリのオーナ、グループを`www:www`に変更します。

    ``` shell
mv /usr/local/var/nfsen/run /var/run/nfsen
mv /usr/local/var/nfsen /var
chown -R www:www /var/nfsen /var/run/nfsen
```

### 自動起動の設定
FreeBSDの起動時にNfSenが自動的に起動されるよう設定します。

``` shell
sysrc nfsen_enable=YES
```

### サービス起動
ようやくNfSenの設定が終わりましたね。では、以下のコマンドを実行して起動してみましょう。

``` shell-session
# service nfsen start
Configured sources do not match existing sources. Run 'nfsen reconfig' first!
```

一回目の起動時には、設定したデータソースに対応するNetFlowデータを保存するディレクトリが存在しないため、再コンフィグを行なうよう促されます。メッセージに従って、以下のコマンドを実行します。

``` shell-session
# nfsen reconfig
New sources to configure : example    # sourceで指定した識別名が表示される
Continue? [y/n] y                     # ここでyを入力

Add source 'example'

Reconfig done!
```

設定したデータソースの識別名が表示され、本ソースをコンフィグして良いか確認されますので`y`を入力します。その後、再度以下のコマンドを実行するとNfSenが起動します。

``` shell
service nfsen start
```

以上で、NfSenが起動し、NetFlowデータの受信、蓄積が始まりました。

### 動作確認
ガイド記事にそっていくつか動作確認を行なっておきましょう。

まず、必要なプロセス(`nfcapd`, `nfsend`, および`nfsend-comm`)が起動しているかを確認します。

``` shell-session
$ ps auxww | grep /usr/local/bin/nf | grep -v grep
www           63858   0.0  0.0    27360    3332  -  S    15:11       0:00.00 /usr/local/bin/nfcapd -w -D -p 4444 -u www -g www -B 200000 -S 1 -P /var/run/nfsen/p4444.pid -z -I example -l /var/nfsen/profiles-data/live/example
www           63860   0.0  0.2    42832   31868  -  Is   15:11       0:00.24 /usr/local/bin/perl /usr/local/bin/nfsend
www           63861   0.0  0.2    38296   27512  -  Is   15:11       0:00.01 /usr/local/bin/nfsend-comm (perl)
```

NetFlowデータがファイルに書き出されるまでにはしばらく時間がかかりますが、10分くらい待てば確実にファイルが作成されているはずです。以下のコマンドを実行して、ファイルが作成されていることを確認します。

``` shell-session
$ ls -lah /var/nfsen/profiles-data/live/example/2018/08/05    # "2018/08/05"の部分は実際の日付に合わせてください
total 2001
drwxr-xr-x  2 www  www   225B  8月  5 18:35 ./
drwxr-xr-x  5 www  www     5B  8月  5 00:05 ../
-rw-r--r--  1 www  www   6.6K  8月  5 00:05 nfcapd.201808050000
-rw-r--r--  1 www  www   6.4K  8月  5 00:10 nfcapd.201808050005
(snip)
```

また、`nfdump`コマンドを用いて、蓄積されたフローデータの解析結果を確認してみましょう。(以下の例では、プライバシー保護のためIPv6アドレスの一部を伏せ字にしています。)

``` shell-session
$ nfdump -R /var/nfsen/profiles-data/live/example -o extended -s record/bytes
Aggregated flows 100402
Top 10 flows ordered by bytes:
Date first seen          Duration Proto      Src IP Addr:Port          Dst IP Addr:Port   Flags Tos  Packets    Bytes      pps      bps    Bpp Flows
2018-08-05 00:01:58.000  1770.000 TCP       192.168.1.34:35527 ->    192.168.1.247:9103  .AP.SF   0   83.3 M  124.9 G    47089  564.6 M   1498    31
2018-08-03 15:12:00.000 256907.000 TCP   XXXX:11..00::252.2049  -> XXXX:11..900::34.714   .AP...   0    3.4 M    4.8 G       13   148942   1420  3934
2018-08-05 00:01:59.000  1769.000 TCP      192.168.1.247:9103  ->     192.168.1.34:35527 .AP.SF   0   41.9 M    2.2 G    23680    9.9 M     52     1
2018-08-05 23:27:01.000    21.000 TCP       192.168.1.34:43870 ->    192.168.1.247:9103  .AP.SF   0   628920  941.6 M    29948  358.7 M   1497     2
2018-08-04 02:43:26.000  1094.000 TCP   XXXX:11..900::34.22    -> XXXX:11..00::247.56045 .AP.SF   0   445328  622.6 M      407    4.6 M   1398     1
2018-08-06 02:45:15.000  1201.000 TCP   XXXX:11..900::34.22    -> XXXX:11..00::247.47412 .AP.SF   0   444010  620.8 M      369    4.1 M   1398     1
2018-08-05 02:42:22.000   954.000 TCP   XXXX:11..900::34.22    -> XXXX:11..00::247.58530 .AP.SF   0   399392  554.4 M      418    4.6 M   1388     1
2018-08-03 23:17:43.000    18.000 TCP       192.168.1.34:17388 ->    192.168.1.247:9103  .AP.SF   0   353185  529.4 M    19621  235.3 M   1498     2
2018-08-03 15:11:00.000 256967.000 TCP   XXXX:11..900::34.714   -> XXXX:11..00::252.2049  .AP...   0    1.9 M  163.7 M        7     5095     88  3818
2018-08-03 15:11:12.000 255916.000 TCP   XXXX:11..900::34.9100  -> XXXX:11..900::35.58740 .AP...   0    68264   89.6 M        0     2801   1312   141
Summary: total flows: 310540, total bytes: 136945671247, total packets: 137522957, avg bps: 4233578, avg pps: 531, avg bpp: 995
Time window: 2018-08-03 14:41:28 - 2018-08-06 14:34:28
Total flows processed: 310540, Blocks skipped: 0, Bytes read: 26988128
Sys: 0.187s flows/second: 1659585.6  Wall: 3.183s flows/second: 97531.8   
```

以上で、NetFlow collectorの設定と動作確認は完了です。

次回の記事では、蓄積したNetFlowデータを可視化するNetFlow visualizerについて説明します。

### 参考文献
1. The Elastic Stack, https://www.elastic.co/products
1. Open Source Time Series Platform, https://www.influxdata.com/time-series-platform/
1. ELKではじめるお手軽Flowコレクター, http://enog.jp/archives/1337
1. GrafanaによるNetFlow v9トラフィック分析, https://qiita.com/ishizaghi/items/b0ecd9fa50e503362165
1. NFDUMP, https://github.com/phaag/nfdump
1. NfSen, http://nfsen.sourceforge.net/
1. CONFIGURE NETFLOW STORAGE AND DISPLAY WITH NFDUMP AND NfSen, https://forums.freebsd.org/threads/howto-monitor-network-traffic-with-netflow-nfdump-nfsen-on-freebsd.49724/#post-277774
