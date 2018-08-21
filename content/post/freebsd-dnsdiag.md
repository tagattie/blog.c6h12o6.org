+++
title = "DNSPingでDNSサーバの応答時間を計測する"
date = "2018-08-21T10:47:06+09:00"
draft = true
categories = ["Network"]
tags = ["dns", "performance", "response", "dnsping", "dnsdiag", "freebsd"]
+++

[先日の記事](/post/unbound-dnssec-dns-over-tls/)で「DNSSECおよびDNS over TLSに対応したキャッシュDNSサーバ」を構築する手順を紹介しました。要点を簡単にふり返っておきますと、

- DNS over TLS (暗号化およびサーバ認証)により、DNS通信の盗聴やDNSサーバのなりすましを防止できる、
- DNSSEC (レコードの署名検証)により、DNSレコード(DNSクエリに対する応答内容)の改ざんを防止できる、

の二点で、DNS (Domain Name System)に関するセキュリティが強化されるということでした。

Webサイトにアクセスするとき、電子メールを送るとき、LINEでメッセージを送るとき、などなど、インターネットで通信するほぼすべての場面で、DNSによる名前解決(DNSクエリ)が実行されます。(クエリが実行されていることをユーザが意識することはまずないと思いますが…)

インターネットの基盤を構成するDNSなので、そのセキュリティを強化することはとても大切です。ただ、インターネット通信のあらゆる場面に関わってくるDNSなので、セキュリティに加えて**性能(DNSクエリに対する応答時間)**も非常に気になりますよね。

クエリに対する応答が速ければ、それだけWebアクセスなどが高速になります。逆に、DNSの性能がよくなければ、インターネットを使う際の快適性が落ちてしまうわけです。

普段自分の使っているDNSサーバの応答時間はどうなんだろう? 気になりますね。

そこで、本記事ではクエリに対するDNSサーバの応答時間を計測するツールとして、DNSPingを紹介します。

- [Advanced Ping: httping, dnsping, smtpping (Blog Webernetz.net)](https://blog.webernetz.net/advanced-ping-httping-dnsping-smtpping/)
- [Pingの発展版 : httping, dnsping, smtpping (POSTD)](https://postd.cc/advanced-ping-httping-dnsping-smtpping/) (上記記事の日本語訳)

DNSPingは、DNSに関する調査ツールであるDNSDiagに含まれるコマンドです。

- [DNSDiag - DNS Diagnostics and Performance Measurement Tools](https://dnsdiag.org/)

以下、FreeBSDマシンを対象にDNSDiagパッケージのインストール、およびDNSPingコマンドの使い方について説明していきます。

まずは、パッケージのインストールからです。以下のコマンドを実行してDNSDiagパッケージをインストールしてください。

``` shell
pkg install py36-dnsdiag
```

パッケージには`dnsping`, `dnstraceroute`, および`dnseval`の各コマンドが含まれます。おのおのの機能を簡単に見ておきましょう。

- `dnsping` - DNSサーバに対して任意のクエリを送信し応答時間を計測
- `dnstraceroute` - DNSサーバまでのDNSクエリの送信経路を調査、通常の`traceroute`と比較することでDNSクエリが不審な経路を通っていないかをチェック可能
- `dnseval` - 複数のDNSサーバに対して一度にDNSクエリを送信、複数DNSサーバの性能評価に使用可能

さて、各コマンドの概要を理解したところで、さっそく`dnsping`を使ってみたいと思います。以下の例では、

- DNSサーバ: `192.168.1.243` ([先日の記事](/post/unbound-dnssec-dns-over-tls/)で構築した家庭内ネットワーク向けのキャッシュDNSサーバです)

に対して、

- ドメイン名: `blog.c6h12o6.org` (このブログをホストしているサーバです)

上記のドメイン名からIPv4アドレスを解決するためのクエリを送信して、その応答時間を見ています。

``` shell-session
$ dnsping -s 192.168.1.243 blog.c6h12o6.org
dnsping DNS: 192.168.1.243:53, hostname: blog.c6h12o6.org, rdatatype: A
46 bytes from 192.168.1.243: seq=0   time=445.779 ms
46 bytes from 192.168.1.243: seq=1   time=1.062 ms
46 bytes from 192.168.1.243: seq=2   time=1.032 ms
46 bytes from 192.168.1.243: seq=3   time=0.967 ms
46 bytes from 192.168.1.243: seq=4   time=0.945 ms
46 bytes from 192.168.1.243: seq=5   time=1.180 ms
46 bytes from 192.168.1.243: seq=6   time=1.434 ms
46 bytes from 192.168.1.243: seq=7   time=1.236 ms
46 bytes from 192.168.1.243: seq=8   time=1.163 ms
46 bytes from 192.168.1.243: seq=9   time=0.996 ms

--- 192.168.1.243 dnsping statistics ---
10 requests transmitted, 10 responses received, 0% lost
min=0.945 ms, avg=45.579 ms, max=445.779 ms, stddev=140.616 ms
```

ご覧のとおり、上記コマンドを実行すると、`-s`オプションで指定したDNSサーバに対してクエリが10回(デフォルト、`-c`オプションで変更可能)送信されます。送信したおのおののクエリに対するレスポンスのサイズ、および応答時間が表示されます。また、10回のクエリ実行後には応答時間の最小値、平均値、最大値、および標準偏差が表示されます。

以上です。使い方、簡単でしたね。通常の`ping`コマンドと同じ感覚で、DNSサーバの応答時間を計測できるDNSPingの紹介でした。

おっと、一つ大切なことを書き忘れました。

コマンド実行結果を見てお気づきになったでしょうか?

**一回目のクエリに対する応答時間だけ**が他のクエリと比較すると**極めて長く**なっています。

これには、DNSサーバのキャッシュ内における、`blog.c6h12o6.org`に対応するAレコードの有無が大きく影響しています。

一回目のクエリでは該当レコードがキャッシュになかったため、クエリを上流サーバに送信しその応答を待つ時間が含まれています。いっぽう、二回目以降のクエリではこのレコードがキャッシュ内にあるため、即応答を返しています。この違いが、一度目の応答時間が約450ms、二度目以降の応答時間が約1msという大きな差になって現れたわけです。

今回用いた家庭内ネットワーク向けキャッシュDNSサーバの場合、クエリ結果がキャッシュにヒットすれば**極めて高速な応答**が得られることがわかります。いっぽう、キャッシュミスの場合は、応答時間に関して**数百倍のペナルティ**がかかってしまうこともわかります。

こうなると、家庭内ネットワーク向けキャッシュDNSサーバを用意してこれを用いるのがよいのか、それとも世界最速を謳うパブリックDNSサーバである1.1.1.1 (Cloudflare社とAPNICが共同提供)、あるいはGoogle社などのパブリックDNSサーバを直接使うほうがよいのか、そういうことが気になってきます。

次回の記事では、この疑問を解決すべく(?)、今回インストールしたDNSDiagパッケージに含まれる`dnseval`コマンドを用いて、いくつかのパブリックDNSサーバの応答時間を計測してみたいと思います。

### 参考文献
1. Advanced Ping: httping, dnsping, smtpping, https://blog.webernetz.net/advanced-ping-httping-dnsping-smtpping/
1. Pingの発展版 : httping, dnsping, smtpping, https://postd.cc/advanced-ping-httping-dnsping-smtpping/
1. DNSDiag, https://dnsdiag.org/
