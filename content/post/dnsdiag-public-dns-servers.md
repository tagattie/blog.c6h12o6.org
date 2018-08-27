+++
title = "パブリックDNSサーバの応答時間を計測する"
date = "2018-08-22T13:49:58+09:00"
draft = true
categories = ["Network"]
tags = ["dns", "performance", "response", "dnsdiag", "dnseval"]
+++

[前回の記事](/post/freebsd-dnsdiag/)では、[DNSDiag](https://dnsdiag.org/)に含まれる`dnsping`コマンドを用いて、DNSサーバの応答時間を計測する方法を紹介しました。この際わかったことの一つが、「DNSサーバの応答時間は、クエリに対応するレコードがキャッシュ内に存在するか否か、によって大きく影響を受ける」ということです。

自宅ネットワークに設置したキャッシュDNSサーバの場合、キャッシュヒット時の応答時間が約1msだったのに対し、キャッシュミス時の応答時間は約450msとなり、その差は数百倍にものぼりました。

すべてのクエリに対してキャッシュミスする、ということは実際にはきわめて起こりにくいでしょうから、DNSサーバの現実的な応答時間は1msと450msとの間のおそらく1ms寄りの場所に落ち着くと思われます。これをどれだけ1msに近づけられるか、というところがDNSサーバ運用のチューニングポイントの一つであり、ノウハウになるのでしょうね。

ここで興味を持ったのが、パブリックDNSサーバの場合はどうなんだろう、ということです。プライベートなキャッシュDNSサーバと比較すると、パブリックサーバは、

- ネットワーク的にクライアントから遠い位置にあるので、キャッシュヒット時とミス時の差(応答時間の比)はプライベートDNSサーバの場合より小さくなるのではないか、
- インターネットの基幹に近い部分に設置されているので、キャッシュミスした場合でもクエリ結果が得られるまでの待ち時間が小さいのではないか、
- キャッシュのサイズや保持期間がチューニングされているので、クエリ結果がキャッシュにヒットする可能性が高いのではないか、

ということが考えられます。

三つめの点については今回は確認できないのですが、本記事では、主に一つめ、二つめの点を確認するために、パブリックDNSサーバを対象にして、キャッシュヒット時、およびキャッシュミス時の応答時間を計測してみたいと思います。

計測のためのツールとしては、前回の記事で紹介した[DNSDiag](https://dnsdiag.org/)に含まれる`dnseval`コマンドを用いることにします。主なチェックポイントは以下に挙げる三点になります。

- DNSプロバイダによる応答時間の違い(キャッシュヒット時およびミス時)
- DNSプロトコル(DNSあるいはDNS over TLS)による応答時間の違い
- ネットワークプロコトル(IPv4あるいはIPv6)による応答時間の違い

### 計測条件など
計測結果の前に、当方のネットワーク環境や計測条件などについて簡単に記しておきます。

まず、ネットワーク環境は以下のとおりで、インターネットマルチフィード社が提供するtransixサービス(IPoE接続)を利用しています。IPv4については、DS-Liteを用いたIPv4 over IPv6接続になります。

- IPv4 - IPoE + DS-Lite (プロバイダ: IIJmio + transix)
- IPv6 - IPoE (プロバイダ: IIJmio + transix)

次に、計測対象とするDNSプロバイダおよび計測条件についてまとめます。計測の対象は、主にパブリックDNSサーバですが、比較のためネットワークプロバイダ(IIJmio)が提供するDNSサーバ、および自宅内に設置したプライベートDNSサーバについても計測の対象に含めます。また、名前解決の対象とするドメイン名は`blog.c6h12o6.org`(このブログをホストしているサーバ)とします。

- 計測対象DNSサーバ

    - パブリックDNSサーバ - [Cloudflare](https://1.1.1.1/), [Google](https://developers.google.com/speed/public-dns/), [OpenDNS](https://www.opendns.com/setupguide/), [Quad9](https://www.quad9.net/), および[VeriSign](https://www.verisign.com/en_US/security-services/public-dns/index.xhtml) (5つ)  
    注: Cloudflareについては、DNSプロトコルおよびDNS over TLS (DoT)プロトコルでの応答時間を計測する  
    注2: DoTプロトコルでの計測のときは、DNSとDoTとの間の変換を行なうプロキシである[stubby](https://dnsprivacy.org/wiki/display/DP/DNS+Privacy+Daemon+-+Stubby)を経由する
    - プロバイダDNSサーバ - IIJmio
    - プライベートDNSサーバ - 自宅内キャッシュDNSサーバ

- 名前解決対象ドメイン名 - `blog.c6h12o6.org`
- 応答時間計測コマンド

    ``` shell
dnseval -f <DNSサーバIPv4アドレスリストファイル> -t A -c 10 blog.c6h12o6.org          # キャッシュヒットの計測(IPv4)
dnseval -f <DNSサーバIPv6アドレスリストファイル> -t AAAA -c 10 blog.c6h12o6.org       # キャッシュヒットの計測(IPv6)
dnseval -f <DNSサーバIPv4アドレスリストファイル> -t A -c 10 -m blog.c6h12o6.org       # キャッシュミスの計測(IPv4)
dnseval -f <DNSサーバIPv6アドレスリストファイル> -t AAAA -c 10 -m blog.c6h12o6.org    # キャッシュミスの計測(IPv6)
```

応答時間計測テストの構成を図にまとめておきます。

![パブリックDNSサーバ - 応答時間 - 計測](/img/freebsd/public-dns-servers-test.png)

### 計測結果
では、計測結果です。

**注**: 本計測結果は、あくまである日時におけるあるクライアントからのクエリに対する応答時間を計測したものであり、おのおののDNSサーバのトータルな性能を示すものではないことにご注意ください。ご参考にとどめおいてくださるようお願いします。

まず、クエリに対応するレコードがDNSサーバのキャッシュにヒットした場合の応答時間を示します。

![パブリックDNSサーバ - 応答時間 - キャッシュヒット](/img/freebsd/public-dns-servers-hit.png)

次に、キャッシュミスした場合の応答時間を示します。

![パブリックDNSサーバ - 応答時間 - キャッシュミス](/img/freebsd/public-dns-servers-miss.png)

いかがでしょうか? キャッシュヒット時、キャッシュミス時を通じて、以下の二点を除きパブリックDNSサーバの応答時間はおおむね同等であるという感想です。

- キャッシュヒット時について、Googleが30~40ms前後となっており、他の10ms前後と比べてやや長くなっている
- キャッシュヒット、ミスにかかわらず、VeriSignのプライマリサーバは他と比べて非常に応答時間が長くなっている

さらにくわしく見ていくと、キャッシュヒット時、およびキャッシュミス時について以下のようなことが読み取れると思います。

- キャッシュヒット、ミス時共通
    - DoT (DNS over TLS)の場合、DNSプロトコルと比べると30ms程度のオーバヘッドが発生
- キャッシュヒット時
    - 自宅内に設置したプライベートDNSサーバの応答時間が最短で約1ms (当然ではありますが…)
    - IPv4, IPv6による応答時間の違いはほぼ見られない
- キャッシュミス時
    - IIJmioがもっとも応答時間が小さく、Google, OpenDNS, Cloudflare, Quad9と続く
    - IPv4, IPv6を比較するとほぼすべてのパブリックDNSサーバでIPv6の応答時間が短い

応答時間を総合的に見た場合、**IIJmioのDNSサーバがトップパフォーマー**であるといえそうです。(ただし、IIJmioのサーバはパブリックDNSでは**ありません**が。) ネットワークプロバイダとしてIIJmioを使用しているので当たり前の結果なのかもしれませんが、やはりネットワークで定評のあるIIJmioならではの結果であり、さすがだと感じます。(使っててよかったIIJmio ^^)

今回計測した5つのパブリックDNSサーバに話を限ると、VeriSign以外ならいずれを選択しても大きな違いはなさそうです。

以上、パブリックDNSサーバの応答時間計測結果をお知らせしました。

### 参考文献
1. DNSDiag, https://dnsdiag.org/
1. 1.1.1.1, https://1.1.1.1/
1. Google Public DNS, https://developers.google.com/speed/public-dns/
1. Set Up OpenDNS on Your Device, https://www.opendns.com/setupguide/
1. Quad9, https://www.quad9.net/
1. Verisign Public DNS, https://www.verisign.com/en_US/security-services/public-dns/index.xhtml
1. DNS Privacy Daemon - Stubby, https://dnsprivacy.org/wiki/display/DP/DNS+Privacy+Daemon+-+Stubby
