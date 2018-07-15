+++
title = "FreeBSDで複数のルーティングテーブルを使い分ける"
date = "2018-07-13T17:25:32+09:00"
draft = true
categories = ["FreeBSD"]
tags = ["freebsd", "multiple", "routing", "table", "fib", "setfib", "ipoe", "dslite", "pppoe"]
+++

わが家ではフレッツ光 + IIJmioでインターネットに接続しています。

夕方から夜間にかけての速度低下を避けるため、IPoE (IPv6) + DS-Lite (IPv4 over IPv6)という接続環境で通信しています。IPoE + DS-Liteの場合、PPPoEにおける網終端装置(**本装置の混雑が速度低下の原因**)を経由しないため、いまのところ速度低下が起きないか、起きても影響が小さいと考えられます。

IPoE + DS-Liteについて詳しくは、以下の二つの記事をご参照ください。(てくろぐのほうが詳しいです。)

- [EdgeRouter LiteにFreeBSDをインストール - DS-Lite編 (C6H12O6)](/post/freebsd-edgerouter-lite-dslite/)
- [DS-LiteでIPv4してみませんか？ (てくろぐ)](http://techlog.iij.ad.jp/archives/1254)

メリットの大きいIPoE + DS-Lite接続ですが、ひとつ大きな問題があります。

サービスの公開、つまりWebサーバなどを自宅LANに設置してインターネットからアクセスすること、ができないのです。大多数のネットユーザには問題にならないと思いますが、外出先から自宅サーバにアクセスしたい、わたしのような者にとっては困ったことです。

さいわい、利用している[IIJmio FiberAccess/NF](https://www.iijmio.jp/guide/outline/nbd/)では、IPoE + DS-LiteによるIPv4接続とPPPoEによるIPv4接続を同時に使用することができます。

したがって、宅内LANにおいて、外部からアクセスしたいマシンだけは、そのデフォルトルートをPPPoE接続のほうに向けておくことで、LAN内の他のマシンでは速度低下のない通信を楽しみつつ、公開マシンに対するインターネットからのアクセスを実現できます。(下図) (ちなみに、IPv6の場合は宅内LANの全マシンにグローバルアドレスが割り当てられますので、こういった問題は生じません。)

![Network - IPoE+DS-LiteとPPPoEの同時使用](/img/freebsd/freebsd-pppoe-dslite.png)

めでたしめでたし、といいたいところですが、公開マシンはデフォルトルートがPPPoE接続を向いていますので、本マシンの通信すべてが夜間の速度低下の影響を受けてしまいます。妥協できる範囲内ではあるのですが、せっかくなので「公開マシン」ではなく「公開サービス」のみデフォルトルートをPPPoEに向け、速度低下の影響をさらに低減することを試みます。

サービスごとにデフォルトルートを使い分けるために、FreeBSD 7.1-RELEASEから導入された「複数ルーティング(経路)テーブル機能」を使用します。

- [FreeBSDの複数経路について (アクロスのwiki)](https://wiki.across.gr.jp/tech/freebsd/routetables)

通常、経路テーブルはマシンインスタンス単位でひとつだけです。しかし、上記機能を使うことにより、単一マシン内に経路テーブルを複数用意し、プロセスごとに使用するテーブルを選択できます。上図と比較すると次のようなイメージになります。

![FreeBSD - 複数ルーティングテーブル](/img/freebsd/freebsd-multiple-fibs.png)

今回の場合は、「デフォルトルートがIPoE + DS-Lite接続を指す経路テーブル0 (FIB 0)」、および「デフォルトルートがPPPoE接続を指す経路テーブル1 (FIB 1)」の二つを用意します。そして、外部に公開するサービス(サーバプロセス)のみ経路テーブル1を使うようにし、その他すべては経路テーブル0を使うようにします。

では、さっそくやりかたを見ていきましょう。

### カーネルの再構築
複数の経路テーブルを使用するにはカーネルの再構築が必要です。以下のオプションを追加して、カーネルの再構築を行なってください。(以下の例では二つの経路テーブルが使用可能になります。)

``` conf
options ROUTETABLES=2
```

本記事ではカーネルの再構築は扱いませんので、詳細については、たとえばFreeBSD Handbookの第8章を参考にしてください。

- [Chapter 8. Configuring the FreeBSD Kernel (FreeBSD Handbook)](https://www.freebsd.org/doc/handbook/kernelconfig.html)

カーネルの再構築、インストールが終わったら、FreeBSDマシンを再起動します。

**注**: 以下の記事によると、カーネルパラメータ`net.fibs`の値を変更することにより、ルーティングテーブルの数を(**カーネルの再構築なしで**)変更できるようです。参考にしてみてください。(こちらのほうが手軽ですね。)

- [FreeBSDでIPoE/DS-LiteとPPPoEを同時に使う (Medium / Yusuke Ito)](https://medium.com/@yusukeito/freebsd%E3%81%A7ipoe-ds-lite%E3%81%A8pppoe%E3%82%92%E5%90%8C%E6%99%82%E3%81%AB%E4%BD%BF%E3%81%86-5fdd635581ce)

### 経路情報の設定
さて、これで複数の経路テーブルを使えるようになりました。FreeBSDでは経路テーブルのことをFIB (Forwarding Information Baseの略?)と呼んでいますので、本記事でも以下FIB 0, FIB 1のように呼びます。特段の設定を行なわなければ、FIB 0がデフォルトの経路テーブルとなります。

FIB 0のデフォルトルートについては、システム起動時に適切に設定されていることを仮定します。たとえば、手動でIPアドレスの設定を行なっている場合は、アドレスなどの指定に加えて以下のような一行が`/etc/rc.conf`に存在するだろうと思います。

``` shell
defaultrouter="192.168.0.254"                   # FIB 0のデフォルトルートはIPoE+DS-Lite向き
```

二つめの経路テーブル(FIB 1)にはデフォルトルートが設定されていません。そこで、以下のコマンドを用いて、PPPoE接続に向けたデフォルトルートを設定します。

``` shell
setfib 1 route add -net default 192.168.0.1     # FIB 1のデフォルトルートをPPPoEに向ける
```

上記のように、`setfib <FIB番号> コマンド`という形式で、指定したFIBを使うようにして任意のコマンドを実行できます。

### 動作確認
FIB 0および1に異なるデフォルトルートを設定しました。実際に異なる経路が使われるかを確認してみましょう。ためしに、`www.iijmio.jp`に対して`traceroute`コマンドを実行してみます。(プライバシー保護のため、コマンド出力の一部を伏せ字にしています。)

- FIB 0の場合(`setfib 0`の部分は省略可能)

    {{< highlight shell-session >}}
$ setfib 0 traceroute www.iijmio.jp
traceroute to www.iijmio.jp (160.13.17.45), 64 hops max, 40 byte packets
 1  dslite (192.168.0.254)  0.659 ms  0.548 ms  0.435 ms
 2  XXX.transix.jp (XX.XX.XX.XX)  4.805 ms  5.436 ms  5.022 ms
 3  XXX.transix.jp (XX.XX.XX.XX)  5.522 ms  5.445 ms  4.993 ms
 4  210.138.9.29 (210.138.9.29)  6.045 ms  4.960 ms  5.228 ms
 5  tky009bb00.IIJ.Net (58.138.112.229)  5.610 ms
    tky009bb00.IIJ.Net (58.138.112.1)  5.606 ms
    tky009bb00.IIJ.Net (58.138.112.221)  5.992 ms
 6  ngy003bb00.IIJ.Net (58.138.89.65)  10.961 ms  11.062 ms  10.542 ms
 7  mte001agr02.IIJ.Net (210.138.115.226)  20.924 ms  21.476 ms  21.551 ms
 8  helpmio-front1100.per.2iij.net (160.13.17.45)  20.679 ms !Z  21.227 ms !Z  21.230 ms !Z
{{< /highlight >}}

- FIB 1の場合

    {{< highlight shell-session >}}
$ setfib 1 traceroute www.iijmio.jp
traceroute to www.iijmio.jp (160.13.17.45), 64 hops max, 40 byte packets
 1  pppoe (192.168.0.1)  1.064 ms  0.880 ms  0.476 ms
 2  XXX.flets.2iij.net (XX.XX.XX.XX)  4.162 ms  4.785 ms  4.571 ms
 3  XXX.flets.2iij.net (XX.XX.XX.XX)  4.357 ms  4.527 ms  4.204 ms
 4  tky008lip31.iij.net (210.130.183.245)  5.867 ms  5.498 ms  4.930 ms
 5  tky008bb01.IIJ.Net (210.130.142.93)  6.065 ms  5.619 ms  5.287 ms
 6  tky008bb00.IIJ.Net (58.138.85.181)  6.735 ms
    tky008bb00.IIJ.Net (58.138.85.173)  5.839 ms
    tky008bb00.IIJ.Net (58.138.85.181)  5.935 ms
 7  ngy003bb00.IIJ.Net (58.138.89.57)  11.049 ms  10.726 ms  10.919 ms
 8  mte001agr02.IIJ.Net (210.138.115.226)  20.405 ms  20.640 ms  21.030 ms
 9  helpmio-front1100.per.2iij.net (160.13.17.45)  20.376 ms !Z  23.159 ms !Z  20.026 ms !Z
{{< /highlight >}}

上記のように、結果が異なることが確認できました。

### 公開サービスの起動
ここまでくればあとは簡単です。前記したように、使用するFIB番号を指定してサーバプログラムを起動します。

``` shell
setfib 1 service apache24 onestart
setfib 1 service sshd onestart
```

などのようにすればOK。

注: `/etc/rc.conf`で対象のサービスを起動している場合はコメントアウトしておきましょう。今後は、以下のように`/etc/rc.local`経由で起動します。

### 自動化
ここまでに行なった、

- FIB 1へのデフォルトルートを追加、
- FIB 1を使用しての公開サービスを起動

の手順がマシン起動時に自動的に実行されるよう、`/etc/rc.local`を作成します。

- `/etc/local`

    {{< highlight shell >}}
#! /bin/sh
# Setup an alternative routing table (fib 1)
setfib 1 route add -net default 192.168.1.1
# Start service(s) which use(s) the alternative routing table
setfib 1 service apache24 onestart
setfib 1 service sshd onestart
{{< /highlight >}}

これで、公開したいサービスのみがPPPoE接続をデフォルトルートとして使い、その他のすべてのプログラムはIPoE + DS-Lite接続を使って通信するように設定できました。

### 参考文献
1. DS-LiteでIPv4してみませんか？, http://techlog.iij.ad.jp/archives/1254
1. FreeBSDの複数経路について, https://wiki.across.gr.jp/tech/freebsd/routetables
1. Chapter 8. Configuring the FreeBSD Kernel, https://www.freebsd.org/doc/handbook/kernelconfig.html
1. FreeBSDでIPoE/DS-LiteとPPPoEを同時に使う, https://medium.com/@yusukeito/freebsd%E3%81%A7ipoe-ds-lite%E3%81%A8pppoe%E3%82%92%E5%90%8C%E6%99%82%E3%81%AB%E4%BD%BF%E3%81%86-5fdd635581ce
