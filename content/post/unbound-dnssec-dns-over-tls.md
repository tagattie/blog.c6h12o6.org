+++
title = "UnboundでDNSSEC & DNS over TLS対応のキャッシュDNSサーバを構築する"
date = "2018-07-23T19:52:00+09:00"
categories = ["Network"]
tags = ["unbound", "dnssec", "dns", "tls", "resolver", "cache", "freebsd"]
+++

わが家のネットワークには、スマートフォン、タブレット、クライアントPC、およびサーバマシンを含めて10台を超える端末が接続されています。

数が多いので、端末へのアクセスにIPアドレスを用いるのはめんどうです。覚えきれません。そこで、マシン名(ホスト名)とIPアドレスの対応づけを管理するために、家庭内ネットワーク専用のDNS (Domain Name System)サーバを、オープンソース・ソフトウェアである[BIND](https://www.isc.org/downloads/bind/)を用いて構築、運用しています。

これまでずっと、おもに**ホスト名とIPアドレスとの対応づけを管理する「権威DNSサーバ機能」**と、主としてインターネット上のサービスにアクセスする際に、**ホスト名からIPアドレスを解決するための「キャッシュDNSサーバ機能」**を、単一のBINDサーバに同居させてきました。

しかしながら、権威DNSサーバとキャッシュDNSサーバの機能をひとつのサーバで兼ねることは、おもにセキュリティの観点から、**よくない**プラクティスであることが広く知られています。

- [ＤＮＳの安全性・安定性向上のためのキホン～お使いのＤＮＳサーバーは大丈夫ですか？～ (日本レジストリサービス)](https://jprs.jp/related-info/guide/020.pdf)

家庭内LANでしか使わず、インターネットからはアクセスできないサーバなので、問題があることがわかっていながら放置していました。

ところで、[先日の記事](/post/android-intra/)で、次期Android Pからは[DNS over TLS](https://tools.ietf.org/html/rfc7858)がサポートされること、およびセキュアなDNSサービスを提供するAndroidアプリである[Intra](https://play.google.com/store/apps/details?id=app.intra)を紹介しました。

DNS over TLSではその名のとおり、ホスト名からIPアドレスの解決を行なう際、UDPあるいはTCPの代わりにTLS (Transport Layer Security)というセキュアなプロトコルをトランスポートとして使用します。TLSを用いることで、盗聴や改ざんといったセキュリティ上の脅威からDNS通信を守ることができます。

この機会に、家庭内LANのDNSサーバをDNS over TLSに対応させることにしました。ポイントは以下の三つです。

- 権威サーバとキャッシュサーバの機能を分離して別サーバとする  
→キャッシュサーバを新設し、BINDには家庭内LAN向け権威サーバの機能のみ残す
- キャッシュサーバはDNS over TLSに対応するパブリックDNSサーバに対して名前解決要求を送信する  
→DNS通信を盗聴や改ざんから保護する
- キャッシュサーバは[DNSSEC](https://www.nic.ad.jp/ja/newsletter/No43/0800.html)を用いて名前解決結果の署名を検証する  
→DNSリソースレコード(名前解決の結果)を改ざんから保護する

三つのポイントをまとめたブロック図を以下に示します。

![Unbound - DNSSEC + DNS over TLS](/img/freebsd/unbound-dnssec-dns-over-tls.png)

あらたに構築するキャッシュサーバには、BINDの代わりに[Unbound](https://www.unbound.net/)というオープンソース・ソフトウェアを用いることにしました。

以下、本記事ではFreeBSD上に、Unboundを用いたDNSSEC & DNS over TLS対応のキャッシュサーバを構築する手順について説明します。構築にあたっては、以下の二つの記事を参考にしました。

- [Unbound DNS Tutorial (CALOMEL)](https://calomel.org/unbound_dns.html)
- [Actually secure DNS over TLS in Unbound (Ctrl blog)](https://www.ctrl.blog/entry/unbound-tls-forwarding)

### Unboundのインストール
まずは、必要なパッケージのインストールからです。

**注**: FreeBSDのベースシステムには、すでにunboundが含まれています(FreeBSD 11.2-RELEASEにはunbound 1.5.10が同梱)。しかし、本バージョンはDNS over TLSに未対応のようですので、パッケージからunbound 1.7.3 (2018年7月23日現在)をインストールすることにします。

``` shell
pkg install unbound ca_root_nss
```

`unbound`パッケージに加えて、`ca_root_nss`パッケージもインストールします。`ca_root_nss`パッケージはルート証明機関の証明書(ルート証明書)をひとまとめにしたもので、unboundがパブリックDNSサーバを認証するときに必要となります。

### Unboundの設定
`unbound`パッケージをインストールすると、サンプルの設定ファイル(`unbound.conf.sample`)が`/usr/local/etc/unbound`以下に配置されます。本サンプルをコピーして、上に挙げた二つのサイトも参考にしながら、必要に応じて内容を変更していきます。

``` shell
cp /usr/local/etc/unbound/unbound.conf.sample /usr/local/etc/unbound/unbound.conf
```

では、設定ファイルの主要な部分を見ていきましょう。(以下の例ではセクションごとに設定ファイルを分け、計四つのファイルを作成しています。ファイルの分割は任意ですので、お好みにあわせて単一の設定ファイルにまとめてもかまいません。この場合、設定ファイルは`unbound.conf`にまとめます。)

完全な設定ファイルを以下のGitHubリポジトリに置いてありますので、必要に応じてご参照ください。

- [Unbound-DNSSEC-DNS-over-TLS - Configuration files for Unbound as a caching DNS server with DNSSEC validation and DNS over TLS forwarding](https://github.com/tagattie/Unbound-DNSSEC-DNS-over-TLS)

- `/usr/local/etc/unbound/unbound.conf`

    Unboundのメイン設定ファイルです。(ファイルを分割した場合、)他のファイルを読み込ませるには本ファイルの末尾などで`include: <ファイルパス>`を指定します。

    - [L24-30](https://github.com/tagattie/Unbound-DNSSEC-DNS-over-TLS/blob/master/unbound.conf#L24): 待ち受けアドレス

        ``` conf
interface: ::0
interface: 0.0.0.0
```

        Unboundがクライアントからのクエリ(名前解決要求)を待ち受けるアドレスです。`::0`および`0.0.0.0`を指定することにより、IPv6, IPv4ともに全ネットワークインターフェイスでクエリを待ち受けます。

    - [L53-67](https://github.com/tagattie/Unbound-DNSSEC-DNS-over-TLS/blob/master/unbound.conf#L53): アクセス制御

        ``` conf
access-control: 0.0.0.0/0 refuse
access-control: 127.0.0.0/8 allow
access-control: 192.168.0.0/24 allow
access-control: ::0/0 refuse
access-control: ::1 allow
access-control: ::ffff:127.0.0.1 allow
access-control: 2001:db8::/64 allow
```

        本記事で構築するキャッシュサーバは家庭内LAN専用で、インターネットからのアクセスは受け付けません。家庭内LANからのみアクセスが可能となるよう適切にアクセス制御を行ないましょう。上記の設定では、ローカルホストおよび家庭内LANのアドレスのみを許可しています。(家庭内LANのアドレスについては上図を参照ください。本アドレスは例示用のものですので、お使いの環境にあわせて適宜読み替えをお願いします。)

        「インターネットからアクセスできないサーバなのでアクセス制御は不要」と思われるかもしれませんが、万一オープンリゾルバとなってしまわないよう、必ずアクセス制御の設定は行ないましょう。

    - [L88-96](https://github.com/tagattie/Unbound-DNSSEC-DNS-over-TLS/blob/master/unbound.conf#L88): トラストアンカー

        ``` conf
auto-trust-anchor-file: "/usr/local/etc/unbound/root.key"
```

        DNSSECによるリソースレコード(名前解決の結果)を検証する際に必要となる[トラストアンカー](https://www.nic.ad.jp/ja/basics/terms/trust-anchor.html)のファイルパスを指定します。本ファイルは、[RFC 5011](https://tools.ietf.org/html/rfc5011)の規定にしたがってunboundが自動更新します。(後述する[トラストアンカーの取得](#トラストアンカーの取得)もあわせてご参照ください。)

    - [L102-103](https://github.com/tagattie/Unbound-DNSSEC-DNS-over-TLS/blob/master/unbound.conf#L102): ルート証明書

        ``` conf
tls-cert-bundle: "/etc/ssl/cert.pem"
```

        クライアントからのクエリ(名前解決要求)を転送する先の、パブリックDNSサーバとの間でTLSコネクションを確立する際に、当該DNSサーバを認証するのに必要なルート証明書のファイルパスを指定します。`ca_root_nss`パッケージをインストールした場合は、上記のパスを指定してください。

    以下は、家庭内LAN内のみで用いるプライベートドメイン、および本ドメイン用の権威サーバを運用している場合のみ必要です。

    - [L105-119](https://github.com/tagattie/Unbound-DNSSEC-DNS-over-TLS/blob/master/unbound.conf#L105): プライベートドメイン

        ``` conf
private-domain: "example.jp"                                   # プライベートドメインの指定
domain-insecure: "example.jp"                                  # DNSSECの署名検証をしない
domain-insecure: "0.0.0.0.0.0.0.0.8.b.d.0.1.0.0.2.ip6.arpa"    # DNSSECの署名検証をしない
local-zone: "168.192.in-addr.arpa." nodefault                  # LAN内アドレスのデフォルトレコードを無効化
```

        `example.jp`が家庭内LANのみで運用するプライベートドメイン名であることを指定します。また、本ドメインではDNSSECを使用していないことをあわせて指定します。本ドメインに関するクライアントからのクエリは、後ほど説明する`stub-zone`で指定されたDNSサーバに転送されます。(本ドメイン名は例示用のものですので、お使いの環境にあわせて適宜読み替えをお願いします。)

- `/usr/local/etc/unbound/remote-control.conf`

    `unbound-control(8)`コマンドを用いたunboundの制御に関する設定です。本記事の例では、unboundを動作させているのと同一のホストからのみ、制御を可能とする設定としました。

    {{< highlight conf >}}
remote-control:
    control-enable: yes
    control-interface: /var/run/unbound.ctl
    control-use-cert: no
{{< /highlight >}}


- `/usr/local/etc/unbound/forward-zone.conf`

    クライアントから受け付けたクエリの転送先となるDNSサーバ(群)をドメイン名ごとに指定します。
    
    本記事の設定では、`.` (ルートドメイン)、すなわちすべてのドメインに関するクエリを、`forward-addr`でリストアップしたDNSサーバに転送します。`forward-tls-upstream: yes`を指定することにより、DNSサーバへの接続にTLSが用いられます。

    パブリックDNSサーバとしては、以下の記事に挙げられているものから、CloudflareおよびQuad9が提供しているサーバを選択しました。ポート番号として、DNS標準のポート53ではなく、DNS over TLS用のポート853を指定していることに注意してください。

    - [DNS over TLS - Public DNS Servers (Wikipedia)](https://en.wikipedia.org/wiki/DNS_over_TLS#DNS_over_TLS_-_Public_DNS_Servers)

    {{< highlight conf >}}
forward-zone:
    name: "."
    forward-first: no
    forward-tls-upstream: yes                 # 上流のサーバに対してTLSで接続
                                              # 以下、DNS over TLS対応のパブリックDNSサーバを指定
    forward-addr: 2606:4700:4700::1111@853    # CloudFlare primary
    forward-addr: 2606:4700:4700::1001@853    # CloudFlare secondary
    forward-addr: 2620:fe::fe@853             # Quad9 primary
    forward-addr: 2620:fe::9@853              # Quad9 secondary
    forward-addr: 1.1.1.1@853                 # CloudFlare primary
    forward-addr: 1.0.0.1@853                 # CloudFlare secondary
    forward-addr: 9.9.9.9@853                 # Quad9 primary
    forward-addr: 149.112.112.112@853         # Quad9 secondary
{{< /highlight >}}

- `/usr/local/etc/unbound/stub-zone.conf`

    家庭内LAN専用のプライベートドメインを運用している場合は本ファイルを追加します。プライベートドメインに関するクライアントからのクエリ(正引きおよび逆引き)については、本ファイルで指定した家庭内LAN向けの権威サーバに送信します。

    **注**: 家庭内ネットワークに設定したDNSサーバについては、DNSSEC、DNS over TLSとも非対応であるものと想定しています。

    {{< highlight conf >}}
stub-zone:
    name: "example.jp"                # プライベートドメインの名前解決は
    stub-addr: 2001:db8::53           # 家庭内LAN向けの権威サーバに問い合わせ
    stub-addr: 192.168.0.53           #
stub-zone:
    name: "0.168.192.in-addr.arpa"    # 逆引き(IPv4)も同様
    stub-addr: 2001:db8::53
    stub-addr: 192.168.0.53
stub-zone:
    name: "0.0.0.0.0.0.0.0.8.b.d.0.1.0.0.2.ip6.arpa"    # 逆引き(IPv6)も同様
    stub-addr: 2001:db8::53
    stub-addr: 192.168.0.53
{{< /highlight >}}

### トラストアンカーの取得
設定ファイルの部分でも述べましたが、DNSSECを用いたリソースレコードの検証を行なうためにはトラストアンカーファイルが必要となります。Unboundを**起動する前**に、以下のコマンドを実行してトラストアンカーの初期ファイルを取得します。また、本ファイルはunboundが必要に応じて更新しますので、オーナーを`unbound`ユーザに変更しておきます。

``` shell
/usr/local/sbin/unbound-anchor -a /usr/local/etc/unbound/root.key
chown unbound /usr/local/etc/unbound/root.key
```

### 自動起動の設定
最後に、FreeBSDの起動時にunboundも自動的に起動されるよう設定しておきましょう。

``` shell
sysrc unbound_enable=YES
```

設定ができたらFreeBSDマシンを再起動、あるいは以下のコマンドを実行してunboundを起動します。

``` shell
service unbound start
```

### 動作確認
ようやくunboundサーバを起動できました。さっそく、正しく名前解決ができるかを確認してみましょう。以下の例では、`www.nic.ad.jp`というホスト名(ドメイン名)に対応するAレコード(IPアドレス)の解決を試みています。

確認ポイントは以下のとおりです。

- Unboundに同梱されている`drill`コマンドを用いて確認
- DNSSECの署名検証を有効にするために`-D`オプションを指定
- コマンドの実行結果について、
	- `rcode`が`NOERROR`であること
	- `flags`に`ad`が含まれていること
	- `ANSWER SECTION`にAレコード(IPアドレス)と本レコードに対応する署名(RRSIGレコード)が含まれていること

**注**: DNSSECに対応していないドメインについては、コマンドの実行結果についての後ろ二者(`flags`および`ANSWER SECTION`のRRSIGに関する確認)は適用外となります。

``` shell-session
$ drill -D www.nic.ad.jp. A @192.168.0.1
;; ->>HEADER<<- opcode: QUERY, rcode: NOERROR, id: 10041
;; flags: qr rd ra ad ; QUERY: 1, ANSWER: 2, AUTHORITY: 0, ADDITIONAL: 0 
;; QUESTION SECTION:
;; www.nic.ad.jp.       IN      A

;; ANSWER SECTION:
www.nic.ad.jp.  269     IN      A       192.41.192.145
www.nic.ad.jp.  269     IN      RRSIG   A 8 4 300 20181018010414 20180720010414 34431 nic.ad.jp. qcrwMmDJb9mZjusNAZ8nQk4Oq6C/N7me9Skvmso1vhtW0YMObCa2+C+MD/6koAljbeHAGpA4u5BzvEPflPmzvqhSMP8jv+4dUYc5Gghkor5GPcgsBPyoz870oJ9De7BRJZke4vfpzDE4+bcbzAaQ998/XUDKVcUv3GPctIJlRdVCwkNoxKsksmAAuHM9sNTuPhr4Sd08wmtqxEFJViRlpbH4CDBP329TkQq4JEiw3eVvr4tpoMgjaDLgUF54CIS/M21FzWyn1xOeq4K0lyPl8qYDbqTaN9rycaTxnqd4XdhJDXIbP923oGrOZqO61yx3HTyk588MtvSEF3oUWBeDvA==

;; AUTHORITY SECTION:

;; ADDITIONAL SECTION:

;; Query time: 1 msec
;; EDNS: version 0; flags: do ; udp: 4096
;; SERVER: 192.168.0.1
;; WHEN: Sun Jul 22 19:16:12 2018
;; MSG SIZE  rcvd: 355
```

### クライアントの設定
最後に、今回構築したキャッシュDNSサーバを使用するように、クライアント側の設定についても見直しておきましょう。DNSクライアントの設定ファイルは一般的に`/etc/resolv.conf`です。`nameserver`のアドレスをキャッシュDNSサーバのアドレスに指定しておきましょう。

- `/etc/resolv.conf`

    ``` conf
domain      example.jp
search      example.jp
nameserver  2001:db8::1    # 構築したキャッシュサーバのアドレスを指定
nameserver  192.168.0.1    # (同上)
options     edns0
```

以上で、Unboundを用いたDNSSEC & DNS over TLS対応のキャッシュDNSサーバの構築は完了です。

### 参考文献
1. BIND, https://www.isc.org/downloads/bind/
1. ＤＮＳの安全性・安定性向上のためのキホン～お使いのＤＮＳサーバーは大丈夫ですか？～, https://jprs.jp/related-info/guide/020.pdf
1. RFC 7858, Specification for DNS over Transport Layer Security (TLS), https://tools.ietf.org/html/rfc7858
1. DNSSEC, https://www.nic.ad.jp/ja/newsletter/No43/0800.html
1. Unbound, https://www.unbound.net/
1. Unbound DNS Tutorial, https://calomel.org/unbound_dns.html
1. Actually secure DNS over TLS in Unbound, https://www.ctrl.blog/entry/unbound-tls-forwarding
1. トラストアンカーとは, https://www.nic.ad.jp/ja/basics/terms/trust-anchor.html
1. RFC 5011, Automated Updates of DNS Security (DNSSEC) Trust Anchors, https://tools.ietf.org/html/rfc5011
1. DNS over TLS - Public DNS Servers, https://en.wikipedia.org/wiki/DNS_over_TLS#DNS_over_TLS_-_Public_DNS_Servers
