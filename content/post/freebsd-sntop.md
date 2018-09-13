+++
title = "SNTopでホストの生死状況を監視する(いろんなTop系コマンドを使ってみる その4)"
date = "2018-09-12T13:04:43+09:00"
draft = true
categories = ["FreeBSD"]
tags = ["freebsd", "top", "sntop"]
+++

Unix-like OSユーザのみなさん、今日もtopコマンド使ってますか? この「いろんなTop系コマンドを使ってみる」シリーズでは、topに名前を借りたさまざまなコマンドを紹介しています。

[前回の記事](/post/freebsd-pftop/)では、Unix-like OSの中でも特にBSD系のOS (OpenBSD, FreeBSDなど)に備わっているファイアウォール"PF"の状態を確認する[pftop](https://repology.org/metapackage/pftop/information)コマンドを紹介しました。本コマンドでは、ファイアウォールがいまどんな通信をさばいているのかをリアルタイムで確認できます。

シリーズ四回めの本記事では、ホスト(マシン)の稼働状況を監視する[sntop](http://sntop.sourceforge.net/)コマンドを紹介します。本コマンドを用いると、あらかじめ設定ファイルに登録しておいた、LAN内あるいはインターネット上にあるホストの生死状況を一括で監視(死活監視)することができます。(ただし、生死確認は[fping](https://fping.org/)あるいはpingコマンドを使って行なわれますので、生きているならばpingに応答することが前提となります。)

では、さっそく詳細を見ていきたいと思います。

まず、以下のコマンドを実行してパッケージをインストールします。

``` shell
pkg install sntop
```

上でも述べましたが、本コマンドはホストの生死確認にfpingコマンドを用いますので、`fping`パッケージも同時にインストールされます。

### 設定ファイル
パッケージをインストールすると`/usr/local/etc`以下に設定ファイルのサンプルが配置されます(`sntoprc`および`sntoprc.sample`)。これを直接編集するか、あるいはサンプルをsntopコマンドを使いたいユーザのホームディレクトリに`${HOME}/.sntoprc`としてコピーしてから編集します。

設定ファイルのフォーマットは非常にシンプルなもので、ホスト一台につき以下に示す三つの情報を設定します。

- 監視対象ホストの表示名
- 監視対象ホストのIPアドレスまたはホスト名
- 監視対象ホストに関するコメント

監視対象のホストが複数ある場合は、ホスト数分だけ上記のエントリを作成します。ただし、エントリとエントリの間は一行空けます。以下、二台のホストを監視する場合の設定ファイル例を示します。

- `/usr/local/etc/sntoprc`あるいは`${HOME}/.sntoprc`

    {{< highlight conf >}}
Test Host 1            # 監視対象ホストの表示名
192.168.1.1            # 監視対象ホストのIPアドレス or ホスト名
This is test host 1    # 監視対象ホストに関するコメント

Test Host 2
192.168.1.2
This is test host 2
{{< /highlight >}}

### コマンド実行
設定ファイルの作成が終わったら、コマンドを起動してみましょう。

``` shell
sntop
```

コマンドを実行すると、設定ファイルに含まれる一連のホストの生死状況を示す表示になります。`HOST`列には設定ファイルにおける表示名、`STATUS`列に現在のホストの生死状況(`UP`あるいは`DOWN`)、および`COMMENT`列には設定ファイルにおけるコメントが表示されます。

以下は、筆者の自宅内LAN内ホストに対してsntopコマンドを実行した場合の一例を示します。ほとんどのホストが問題なく動作していますが、WiFiアクセスポイントがダウンしていることがわかります。

``` shell-session
(sntop) simple network top
HOST              STATUS        COMMENT
HomeGW            UP            RS-500KI
Loveland          UP            Intel NUC
Revelstoke        UP            PC/AT compatible
Sugarbush         UP            Raspberry Pi 2
Tamarack          UP            Raspberry Pi 3
Squawvalley       UP            Raspberry Pi 2
Heavenly          UP            BeagleBone Black
DSLiteGW          UP            EdgeRouter Lite
WiFiAP            DOWN          Aterm WG2600HP2


9 hosts polled: 8 up, 1 down
```

あれ、でもWiFi使えているのだけど…と思って、WiFiアクセスポイントの管理用Webインターフェイスにアクセスしようとしたら、確かにアクセスできませんしpingにも応答しませんね。思わぬところでsntopが役に立ちました。

コマンドを終了するには`q`キーを押下します。

ここで紹介した機能以外にも、状態変化を検知した時や最初にDOWNを検知した時に、指定したスクリプトを実行する機能などもあるようです。詳しくは、`sntop --help`および`man sntop`を実行してみてください。

大規模なネットワークの監視には向きませんが、ホストマシンが数十台程度までの小規模なネットワークであれば、インストールや設定が非常に簡単ですので死活監視ツールとして使いやすいのではないかと思います。

以上、本記事では複数ホストの死活監視を行なうsntopコマンドを紹介しました。

### 参考文献
1. sntop - simple network top, http://sntop.sourceforge.net/
1. fping, https://fping.org/
