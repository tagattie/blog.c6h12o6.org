+++
title = "DNSTopでDNSクエリおよびレスポンスを監視する(いろんなTop系コマンドを使ってみる その2)"
date = "2018-09-03T17:08:55+09:00"
draft = true
categories = ["FreeBSD"]
tags = ["freebsd", "top", "dnstop"]
+++

「いろんなTop系コマンドを使ってみる」シリーズでは、Unix系OSにおける有名コマンドtopに名前を借りたいろいろなコマンドを紹介しています。

[前回の記事](/post/freebsd-usbtop/)では、USBデバイスへのアクセス速度をリアルタイムに表示する[usbtop](https://github.com/aguinet/usbtop)コマンドを紹介しました。FreeBSDでの使用にはやや難ありでしたが、USBメモリなどとの間でのファイル移動・コピーの際にアクセス速度を確認する役に立ちそうです。

シリーズ二回めの本記事では、DNSを監視する[dnstop](http://dns.measurement-factory.com/tools/dnstop/)コマンドを紹介します。本コマンドは、ネットワークインターフェイスを流れるパケットをリアルタイムにキャプチャして、あるいはパケットキャプチャ結果を保存したpcapファイルを読み込んで、DNSクエリおよびレスポンスに関する統計情報を表示してくれるツールです。

本記事ではFreeBSDマシンでの使用を想定して紹介しますが、Linux (CentOS)におけるインストール、使用については以下の記事で詳しく紹介されていますので、こちらも合わせて参照いただければと思います。

- [DNS Queryのリアルタイム統計をサーバ内で表示（dnstop編）(Qiita / @rotekxyz)](https://qiita.com/rotekxyz/items/2e4dae559d4d1367fc52)

まず、以下のコマンドを実行してパッケージをインストールしましょう。

``` shell
pkg install dnstop
```

パッケージがインストールできたら早速起動してみます。

リアルタイムの統計情報を知りたい場合は**ネットワークインターフェイス名**、キャプチャしたファイルを読み込ませたい場合は**ファイル名**をコマンドの引数に指定します。また、ネットワークインターフェイス名を指定して起動する場合、パケットキャプチャにBPF (Berkeley Packet Filter)を使いますので、root権限で起動する必要があります。

以下の例では、ネットワークインターフェイス名`ue0`を指定して起動しています。(自宅ネットワーク向けキャッシュDNSサーバが動作しているRaspberry Pi 2上での起動例です。)

``` shell
sudo dnstop -QR ue0
```

### クエリソースによるランキング
コマンドを起動すると、DNSクエリの送信元アドレスによるランキングが表示されます。コマンド起動時からの総クエリ数およびレスポンス数の下に、クエリのソースアドレス、当該アドレスからのクエリ数、全クエリに占めるパーセンテージ、および上位n位までの累積パーセンテージが表示されます。

`s`キーを押下することで、任意の表示画面からクエリソースのランキング画面に遷移できます。

注: 以下の例では、プライバシー保護のためIPv6アドレスの一部を伏せ字にしています。

``` shell-session
Queries: 0 new, 5458 total                             Mon Sep  3 20:20:36 2018
Replies: 0 new, 5458 total

Sources                    Count      %   cum%
------------------------ --------- ------ ------
XXXX:XXXX:XXXX:XXXX::34       1351   24.8   24.8
192.168.1.35                   837   15.3   40.1
XXXX:XXXX:XXXX:XXXX::35        691   12.7   52.7
192.168.1.152                  498    9.1   61.9
XXXX:XXXX:XXXX:XXXX::251       442    8.1   70.0
XXXX:XXXX:XXXX:XXXX::247       348    6.4   76.3
XXXX:XXXX:XXXX:XXXX::25        237    4.3   80.7
192.168.1.22                   216    4.0   84.6
XXXX:XXXX:XXXX:XXXX::243       167    3.1   87.7
XXXX:XXXX:XXXX:XXXX::245       136    2.5   90.2
192.168.1.32                   102    1.9   92.1
XXXX:XXXX:XXXX:XXXX::28        102    1.9   93.9
192.168.1.31                    94    1.7   95.7
XXXX:XXXX:XXXX:XXXX::19         91    1.7   97.3
192.168.1.12                    53    1.0   98.3
192.168.1.19                    31    0.6   98.9
192.168.1.16                    21    0.4   99.2
```

### クエリタイプによるランキング
クエリタイプによるランキング表示です。任意の画面で`t`キーを押下すると表示できます。

クエリのソースアドレス部分がクエリタイプに変わったこと以外は、表示内容はクエリソースによるランキング表示の場合と同様です。

``` shell-session
Queries: 0 new, 5461 total                             Mon Sep  3 20:20:53 2018
Replies: 0 new, 5461 total

Query Type     Count      %   cum%
---------- --------- ------ ------
PTR?            2224   40.7   40.7
A?              1727   31.6   72.3
AAAA?           1477   27.0   99.4
TXT?              27    0.5   99.9
SRV?               4    0.1  100.0
MX?                2    0.0  100.0
```

### レスポンスによるランキング
レスポンスコードによるランキング表示です。任意の画面で`r`キーを押下すると表示できます。

``` shell-session
Queries: 0 new, 5461 total                             Mon Sep  3 20:21:01 2018
Replies: 0 new, 5461 total

Rcode        Count      %   cum%
-------- --------- ------ ------
Noerror       4531   83.0   83.0
Nxdomain       819   15.0   98.0
Servfail       111    2.0  100.0
```

### クエリ対象ドメイン名によるランキング
一番興味深いであろう、クエリ対象のドメイン名によるランキングです。

`1`キーを押下するとトップレベルドメイン名でのランキング、`2`キーを押下するとセカンドレベルドメイン名まででのランキング、…というように`1`~`9`キーの押下により任意のレベルのドメイン名まででのランキングを表示できます。(以下の例では、`1`キーの押下によりトップレベルドメインでのランキングを表示)

**注**: レベル3以上のドメイン名までの表示を行なうためには、コマンド起動時に`-l n` (`n`は表示したい最大レベル)を指定しておく必要があります。ただし、nの値を大きくすると、より多くのメモリとCPUリソースを消費することにご注意ください。

``` shell-session
Queries: 0 new, 5461 total                             Mon Sep  3 20:21:21 2018
Replies: 0 new, 5461 total

Query Name       Count      %   cum%
------------ --------- ------ ------
in-addr.arpa      1728   31.6   31.6
com               1593   29.2   60.8
jp                1201   22.0   82.8
ip6.arpa           522    9.6   92.4
net                187    3.4   95.8
org                 61    1.1   96.9
eu                  51    0.9   97.8
sk                  27    0.5   98.3
is                  18    0.3   98.7
io                  16    0.3   99.0
asia                 8    0.1   99.1
tv                   6    0.1   99.2
fm                   6    0.1   99.3
goog                 6    0.1   99.4
gov                  6    0.1   99.5
to                   4    0.1   99.6
uk                   4    0.1   99.7
```

`2`キーを押下すると、以下のようにセカンドレベルドメイン名まででのランキング表示になります。

注: プライバシー保護のため、一部のドメイン名を伏せ字にしています。

``` shell-session
Queries: 0 new, 5463 total                             Mon Sep  3 20:21:30 2018
Replies: 0 new, 5463 total

Query Name             Count      %   cum%
------------------ --------- ------ ------
XXXXXX.jp                894   16.4   16.4
2.ip6.arpa               522    9.6   25.9
googlesource.com         442    8.1   34.0
210.in-addr.arpa         350    6.4   40.4
127.in-addr.arpa         272    5.0   45.4
google.com               201    3.7   49.1
192.in-addr.arpa         167    3.1   52.1
133.in-addr.arpa         136    2.5   54.6
googleapis.com           122    2.2   56.9
co.jp                     77    1.4   58.3
146.in-addr.arpa          66    1.2   59.5
transip.eu                51    0.9   60.4
iijmio.jp                 51    0.9   61.3
176.in-addr.arpa          51    0.9   62.3
5.in-addr.arpa            51    0.9   63.2
doubleclick.net           44    0.8   64.0
audioscrobbler.com        40    0.7   64.7
```

また、本記事では例示しませんが`!`~`(`キー (ASCII配列キーボードにおいて`shift` + 数字で入力される文字)を押下すると、クエリのソースアドレスとクエリ対象のドメイン名を組み合わせたランキング表示を行なうこともできます。

コマンドを終了するには`ctrl` + `x`を押下します。

以上、本記事ではDNSクエリおよびレスポンスに関する統計情報を表示するdnstopコマンドを紹介しました。

### 参考文献
1. dnstop, http://dns.measurement-factory.com/tools/dnstop/
1. DNS Queryのリアルタイム統計をサーバ内で表示（dnstop編）, https://qiita.com/rotekxyz/items/2e4dae559d4d1367fc52