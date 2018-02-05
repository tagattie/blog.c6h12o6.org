+++
title = "EdgeRouter LiteにFreeBSDをインストール - DS-Lite編"
date = "2018-02-05"
categories = ["FreeBSD"]
tags = ["freebsd", "edgerouter", "dslite", "ipv4", "ipv6"]
+++

[EdgeRouter LiteにFreeBSDをインストール - ブート編](/post/freebsd-edgerouter-lite-boot/)までで、[Ubiquti Networks社](https://www.ubnt.com/)の[EdgeRouter Lite](https://www.ubnt.com/edgemax/edgerouter-lite/)にFreeBSDをインストールし、ブートするところまでを説明しました。本DS-Lite編では、本来の目的であったIPoE+DS-Lite接続を行なうための設定について述べます。

そもそも、PPPoE接続からIPoE+DS-Lite接続への変更を思い立ったきっかけは、フレッツ網内にある[網終端装置の混雑による速度低下](http://techlog.iij.ad.jp/archives/1879)を回避したい、ということにあります。IPoE接続へと変更することにより、家庭内LANからインターネットへ接続する際の起点となるホームゲートウェイに直接(= PPPoE接続を経由せずに)IPv6アドレスが付与されるため、網終端装置の混雑を避けることができます(下図、緑色の線)。

[![EdgeRouter Liteを用いたIPoE+DS-Lite接続ネットワーク](/img/edgerouter-lite-dslite-network-thumbnail.png)](/img/edgerouter-lite-dslite-network.png)

しかし、IPv4については依然としてPPPoE接続のままです(上図、赤色の線)。最近はIPv6でアクセスできるサイトも増えてきましたが、IPv4でしかアクセスできないサイトもまだまだ多数あり、IPv6通信だけが高速化されても目的が達成されたとは言いがたい状況です。そこで登場するのが[DS-Lite](https://tools.ietf.org/html/rfc6333)という技術です。詳しい説明は[てくろぐの記事](http://techlog.iij.ad.jp/archives/1254)などを参照いただくとして、ざっくりいうとIPv6を使ってIPv4の通信もしちゃおう(上図、紫色の線)、ということです。これによって、網終端装置を経由しなくなるので、IPv4通信も高速化が期待できるわけです。

さて、前置きはこのくらいにして、実際の設定について説明していきます。てくろぐに[Mac OSでDS-Liteを利用するためのスクリプト例](http://techlog.iij.ad.jp/contents/dslite-macosx)がのっています。FreeBSDにも適用できますので、基本的にこの例のとおり設定していきます。

なお、以下の設定では上図に示したIPv4, IPv6アドレスを用いていますが、橙色で示したアドレスは説明用の仮のものですので、環境に合わせて適宜読み替えをお願いします。

また、インターネットマルチフィード網内のトンネル終端装置のIPv6アドレスについては、例えば、[AFTRのIPv6アドレス設定について](http://www.mfeed.ad.jp/transix/ds-lite/contents/yamaha_nvr500.html#aftripv6)に掲載されていますので、こちらも環境にあったアドレスをご使用ください。

- `/etc/rc.conf`

    ```conf
    cloned_interfaces="gif0"
    ifconfig_octe0="inet 192.168.0.253 netmask 255.255.255.0"
    ifconfig_octe0_ipv6="inet6 2001:db8:dead:beef::253 prefixlen 64"
    ifconfig_gif0_ipv6="inet6 tunnel 2001:db8:dead:beef::253 2404:8e00::feed:100 mtu 1500"
    ifconfig_gif0="inet 192.0.0.2 netmask 255.255.255.252 192.0.0.1"
    defaultrouter="192.0.0.1"
    gateway_enable="YES"
    ipv6_defaultrouter="2001:db8:dead:beef::254"
    ipv6_default_interface="octe0"
    ```

- `/etc/sysctl.conf`

    ```conf
    net.inet6.ip6.gifhlim=64
    ```

上記設定を行なった後、EdgeRouterを再起動すれば完了です。

同じネットワーク内のPCから http://ipv6-test.com/ にアクセスして、以下のようにISP名が表示されれば、DS-Liteでの接続が成功しています。**(IPv4のデフォルトルータをEdgeRouter Liteのアドレスに変更することをお忘れなく!)**

[![DS-Lite接続の確認画面](/img/ipv6-test.com-thumbnail.png)](/img/ipv6-test.com.png)

### 参考文献
1. てくろぐ IIJmioひかりの混雑の理由とバイパス手段(IPoE・DS-Lite対応), http://techlog.iij.ad.jp/archives/1879
1. RFC 6333 - Dual-Stack Lite Broadband Deployments Following IPv4 Exhaustion, https://tools.ietf.org/html/rfc6333
1. てくろぐ DS-LiteでIPv4してみませんか？, http://techlog.iij.ad.jp/archives/1254
1. てくろぐ DS-Lite(RFC6333)をMacOS Xで利用する, http://techlog.iij.ad.jp/contents/dslite-macosx
