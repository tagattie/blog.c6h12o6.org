+++
title = "PFTopでファイアウォールのステータスを監視する(いろんなTop系コマンドを使ってみる その3)"
date = "2018-09-05T17:05:50+09:00"
draft = true
categories = ["FreeBSD"]
tags = ["freebsd", "top", "pftop", "pf", "firewall"]
+++

Unix-like OSをお使いのみなさんならご存知のtopコマンド。本コマンドに名前を借りたいろいろなコマンドを紹介している「いろんなTop系コマンドを使ってみる」シリーズです。

[前回の記事](/post/freebsd-dnstop/)では、DNS関連の情報を表示する[dnstop](http://dns.measurement-factory.com/tools/dnstop/)コマンドを紹介しました。本コマンドではクエリのソースアドレス、クエリタイプ、レスポンスタイプ、あるいはクエリ対象ドメイン、といったさまざまな観点でDNSクエリおよびレスポンスの統計情報を確認できます。

シリーズ三回めの本記事では、BSD系のUnix-like OSに組み込まれているファイアウォールである「PF (Packet Filter)」を監視する[pftop](https://repology.org/metapackage/pftop/information)コマンドを紹介します。本コマンドにより、PF内部に保持されているステート(ステートフルパケットフィルタのステート)や、アクティブなルール一覧をリアルタイムで確認することができます。

注: PF (Packet Filter)はもともとOpenBSD向けに開発されたファイアウォールソフトウェアですが、現在ではすべてのBSD系Unix-like OS (OpenBSD, FreeBSD, NetBSD, およびDragonFly BSD)に取り込まれています。

ちなみに、以下に挙げる二つの記事にもpftopの紹介がありますので、あわせて参考にしていただければと思います。

- [Keeping an eye on things with pftop (Peter N. M. Hansteen)](https://home.nuug.no/~peter/pf/en/pftop.html)
- [BSD PF Firewall: Displays Active Packetfilter States And Rules (nixCraft)](https://www.cyberciti.biz/faq/bsd-pf-viewing-active-connections-with-pftop/)

では、さっそく紹介していきます。(以下、FreeBSDマシンへのインストールを想定します。)

まず、以下のコマンドを実行してパッケージをインストールします。

``` shell
pkg install pftop
```

特に設定などはありませんので、インストールができたらコマンドを起動してみましょう。デバイスファイル`/dev/pf`へアクセスしますので、本コマンドはroot権限で起動する必要があることに注意してください。

``` shell
sudo pftop
```

コマンドを起動するとデフォルトのビューが表示されます(下図)。本ビューでは、ファイアウォールの現在のステートエントリの総数、現在のビュー、現在のソートキーの表示とともに、ステートエントリの一覧が表示されます。

[![PFTop - Default](/img/freebsd/pftop-default-small.png)](/img/freebsd/pftop-default.png)

各ステートエントリの内容は、プロトコル(`PR`)、コネクションの向き(`DIR`)、ソースアドレス(`SRC`)、デスティネーションアドレス(`DEST`)、ファイアウォールステート(`STATE`)、エントリ作成からの経過時間(`AGE`)、エントリの有効期限(`EXP`)、累積パケット数(`PKTS`)、および累積バイト数(`BYTES`)となります。

注: 本表示内容はターミナルの横幅に応じて適宜トリミングされますので、できる限り幅を広くした状態でコマンドを起動することをおすすめします。

`o`キーでエントリのソートに用いる項目を変更できます。デフォルトではソートなし(`none`)となっていますが、累積バイト数(`bytes`)、エントリの有効期限(`expiry`)、累積パケット数(`packets`)、エントリ作成からの経過時間(`age`)、ソースアドレス(`source addr`)、デスティネーションアドレス(`dest addr`)、ソースポート(`source port`)、デスティネーションポート(`dest port`)、通信速度(`rate`)、あるいはピーク速度(`peak`)を選択できます。

また、`r`キーでソートの順序を逆順にできます。

デフォルトのビュー以外には、大別して二種類のビューがあります。ひとつはファイアウォールのステートに関するステートビュー、もういっぽうはファイアウォールのルールに関するルールビューです。ステートおよびルールの各ビューは、さらに以下のように細分されます。

- ステート - long (下図左), state, time, size, speed
- ルール - rules (下図右), label

|ステート(long)|ルール(rules)|
|:---:|:---:|
|[![PFTop - Long](/img/freebsd/pftop-long-small.png)](/img/freebsd/pftop-long.png)|[![PFTop - Rules](/img/freebsd/pftop-rules-small.png)](/img/freebsd/pftop-rules.png)|

デフォルトビューを含むこれらの各ビューは`v`キーで切り替えることができます。

主なキーバインドについて以下にまとめておきます。(`?`でキーバインド一覧が表示されます。)

- `o` - ソート項目の切り替え
- `r` - ソート順序を逆順に
- `v` - ビューの切り替え

以上、本記事ではファイアウォールPFのステートおよびルールを表示するpftopコマンドを紹介しました。

### 参考文献
1. Information for  pftop, https://repology.org/metapackage/pftop/information
1. Keeping an eye on things with pftop, https://home.nuug.no/~peter/pf/en/pftop.html
1. BSD PF Firewall: Displays Active Packetfilter States And Rules, https://www.cyberciti.biz/faq/bsd-pf-viewing-active-connections-with-pftop/
