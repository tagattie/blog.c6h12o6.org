+++
title = "あなたのVPNクライアント、お漏らししていませんか?"
date = "2018-04-23T21:18:00+09:00"
categories = ["Network"]
tags = ["windows", "openvpn", "dns", "leak", "leakage"]
+++

これまでにいくつかの記事で、VPNサーバの構築手順([OpenVPN](/post/freebsd-openvpn-server-server/)および[L2TP/IPSec](/post/freebsd-l2tp-ipsec-l2tp/))を説明してきました。また、構築したVPNサーバにアクセスするための、クライアント側の設定(OpenVPN([Windows](/post/freebsd-openvpn-server-client/), [Android](/post/freebsd-openvpn-server-android/))および[L2TP/IPSec](/post/freebsd-l2tp-ipsec-client/))についても説明しました。

VPNサーバ構築のおもな目的は、公衆無線LANの安全な利用のためなのですが、VPN経由でインターネットにアクセスするメリットをまとめると、以下の二点に集約されるのではないかと思います。

- 安全性 - 暗号化されたトンネルを経由する通信であるため、データの盗聴や改ざんなどを防げる
- 匿名性 - 自IPアドレスがVPNサーバのものになるため、IPアドレスからの位置特定などのプライバシー侵害を防げる

しかし、これらの利点は、**すべての通信がVPN経由で行なわれる**ことが大前提です。

Windows以外のOSをクライアントに使用している場合は心配いりません。問題は、クライアントOSに**Windows 8以降**を使っている場合です。

以下の記事によれば、Windows 8から導入された"Smart multi-homed name resolution"という機能により、VPNを使っている場合でも、一部の通信内容(DNS (Domain Name System)の名前解決)がVPNの外に漏れることがあるようです。

- [Turn off smart multi-homed name resolution in Windows (GHacks)](https://www.ghacks.net/2017/08/14/turn-off-smart-multi-homed-name-resolution-in-windows/)

"Smart multi-homed name resolution"とは、ざっくりいうと、DNSの名前解決(ドメイン名をIPアドレスに変換すること)を行なうときに、使用可能なすべてのネットワークアダプタに対して、つまり、いまの場合は、VPNのネットワークアダプタに対しても、WiFiのネットワークアダプタに対しても、DNSの問い合わせ(クエリ)を送信するものです。(イメージ下図)

![DNSリーク - イメージ](/img/openvpn/dns-leakage-diagram.png)

そして、最も早く応答が返ってきたネットワークアダプタの名前解決結果を使用します。通信を高速化するという観点ではリーズナブルな機能ですが、プライバシー保護の観点からは問題が生じてきます。

なぜ、この機能が問題になるのでしょうか? 以下の二点がその理由です。

1. DNSクエリが暗号化されずに送信されるため、無線経由で周囲にその内容が漏洩するおそれがある。つまり、アクセスしようとしているサイト名などの情報が傍受される可能性がある。(例: www.google.comへアクセスしようとしている)
1. DNSクエリがWiFiプロバイダのDNSサーバにも送信されるため、WiFiプロバイダにその内容が漏洩する。つまり、アクセス先サイトの名前などがWiFiプロバイダに知られる。

注: 漏洩する可能性があるのは、**アクセス先コンピュータのドメイン名(FQDN, Fully Qualified Domain Name)**であり、アクセス先のコンピュータとやり取りされる通信内容そのものが漏洩することはありません。

では、どうすればこの「DNSリーク」を防げるのでしょうか? [GHacksの記事](https://www.ghacks.net/2017/08/14/turn-off-smart-multi-homed-name-resolution-in-windows/)では、レジストリの編集やグループポリシーの編集で対応しています。しかし、Windows + OpenVPNという組み合わせであれば、OpenVPNクライアントの設定ファイルに、以下の一行を書き加えるのがいちばん簡単です。

``` conf
block-outside-dns
```

この設定を追加することによって、トンネル経由以外のDNSクエリが行なわれなくなります。

最後に例を見てみましょう。この例では、以下の公衆無線LANアクセスポイントに接続して、リークテストサイトにアクセスした実例を示します。

- アクセスポイント名: [`tullys_Wi-Fi`](https://www.tullys.co.jp/wifi/)
- WiFiプロバイダ: [(株)ワイヤ・アンド・ワイヤレス](https://wi2.co.jp/jp/)  
  ワイヤ・アンド・ワイヤレスはKDDIの子会社
- VPNプロバイダ: [ConoHa VPS](https://www.conoha.jp/)上のOpenVPNサーバ  
  ConoHa VPSの運営会社は[GMOインターネット(株)](https://www.gmo.jp/)
- リークテストサイト: [Doileak.com](https://www.doileak.com/)

まず、VPNを使わない場合です。アクセス元のIPアドレス、DNSクエリの送信元アドレスが、いずれもKDDIのネットワークのものであると表示されています。これは、KDDIの子会社が運営するWiFiスポットを使っているので納得ですね。

![Doileak.com - VPNを使わない場合](/img/openvpn/doileak-no-vpn.png)

次に、WindowsでOpenVPNを使用している場合です。VPNを経由したアクセスですので、アクセス元のIPアドレスは、GMOインターネットのものであると表示されています。ここで、DNSサーバのアドレスに注目してください。DNSクエリの送信元アドレスに、KDDIのネットワークのIPアドレスが含まれています。これが**DNSリーク**です。

![Doileak.com - VPNを使う場合(DNSリークあり)](/img/openvpn/doileak-vpn-with-leak.png)

最後に、WindowsのOpenVPNで`block-outside-dns`設定を追加した場合です。VPN以外のネットワークアダプタに対してはDNSクエリが送信されないため、アクセス元のIPアドレス、DNSクエリの送信元アドレスともに、GMOインターネットのものであると表示されています。DNSリークが防止されたことがわかりました。

![Doileak.com - VPNを使う場合(DNSリークなし)](/img/openvpn/doileak-vpn-without-leak.png)

WindowsでOpenVPNを利用している場合は、一度クライアント設定を見なおしてみることをおすすめします。

### 参考文献
1. Turn off smart multi-homed name resolution in Windows, https://www.ghacks.net/2017/08/14/turn-off-smart-multi-homed-name-resolution-in-windows/
1. OpenVPN 2.4 Manual Page, https://community.openvpn.net/openvpn/wiki/Openvpn24ManPage
1. Doileak.com, https://www.doileak.com/
