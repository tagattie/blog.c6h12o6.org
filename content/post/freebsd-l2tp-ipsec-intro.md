+++
title = "FreeBSDでL2TP/IPSec VPNサーバを構築する - まえがき"
date = "2018-04-17T21:11:00+09:00"
categories = ["FreeBSD"]
tags = ["freebsd", "vpn", "l2tp", "ipsec", "chromiumos", "chromeos", "chromebook"]
+++

[連続的なWebブラウズシーンを模擬したバッテリ駆動時間のベンチマーク](/post/chromiumos-self-build-bench/)では、Windows 10に負けてしまいましたが、その後も、NEC LaVie ZでChromium OSを使い続けています。

実際に使っていると、やっぱりこちらのほうがバッテリの持ちがよい気がするんですね。その理由の一つとして、Windowsではさまざまなプログラムがバックグラウンドで動作しており、ときどきバックグラウンドプログラムの負荷が高くなる(= バッテリ駆動時間が短くなる)のに対して、Chromium OSのほうはそういうことがない、あるいは少ないのではないかと思います。

また、電源オフからの起動やスリープからの復帰が速いのも、好感度の高い点です。

さて、OSを変えたからといって作業内容が変わるわけはなく、LaVie Zの主な用途は外出先での文書作成とWebブラウズです。Chromium OSにはネットワーク接続が必須なので、外出先ではテザリングか公衆無線LAN接続のいずれかになります。公衆無線LANが使えればそちらを使うことが多いので、VPN (Virtual Private Network)が重要になってきます。

Chromium OSではもちろんVPNが使用可能で、OpenVPN、あるいはL2TP/IPSec (Layer 2 Tunneling Protocol / Internet Protocol Security)のいずれかを選択できます。(下図はChromium OSのVPN設定画面)

![Chromium OS - VPN設定](/img/chromiumos/chromiumos-vpn-setting.png)

[以前の記事](/post/freebsd-openvpn-server-server/)でOpenVPNサーバを構築済みですので、できればChromium OSでもOpenVPNを使いたいところです。しかし、Chromium OSでは証明書の登録がうまくいきません。証明書を登録しようとした瞬間に、設定画面ごと落ちてしまいます。(原因はいまのところ不明ですが、自分でビルドしているので手順にどこか誤りがあるのかもしれません。)

とはいえVPNは必要なので、もういっぽうのL2TP/IPSec (+ 事前共有鍵)を使うことにします。残念ながら、OpenVPNはL2TP/IPSecに対応していませんので、新たにL2TP/IPSecを用いるVPNサーバを構築します。

### L2TP/IPSecとは?
L2TP/IPSecについてはまったく知識がありませんでしたので、以下の記事やWebサイトを参考に少し勉強しました。

- [ここが知りたいVPN 第2回 IPsecの基本を知る (馬場達也氏)](http://www.tatsuyababa.com/NW-VPN/NW-200404-VPN02.pdf)
- [ここが知りたいVPN 第5回 L2TPを使用したリモートアクセスVPNの仕組み (馬場達也氏)](http://www.tatsuyababa.com/NW-VPN/NW-200407-VPN05.pdf)
- [L2TP/IPsec (ネットワーク入門サイト)](http://beginners-network.com/vpn_l2tp_ipsec.html)
- [L2TP/IPsec (ヤマハ)](http://www.rtpro.yamaha.co.jp/RT/docs/l2tp_ipsec/)

ざっくりまとめると、仮想ネットワークを実現するためのプロトコルであるL2TPと、安全な通信路を提供するための技術であるIPSecを組み合わせることで、「安全なVPN」を実現するものです。L2TPにはセキュリティを確保するための仕組みが規定されていないため、IPSecと組み合わせて用いることが多いようです。

「安全なVPN」が作られるまでの処理の流れを図にしてみました。(下図)

![L2TP/IPSecのシーケンス](/img/diagram/l2tp-ipsec-sequence.png)

- Step 1

    まず、VPNサーバとVPNクライアントの間で、事前に共有しておいた秘密鍵(Pre-Shared Key, PSK)、あるいはデジタル証明書を用いて鍵交換(Internet Key Exchange, IKE)を行ないます。この過程で、通信路の暗号化に用いる鍵が両者間で合意されます。ここまでで、暗号化されたIPSecトンネルが確立します。

- Step 2

    IPSecトンネルを使った通信ができるようになると、このトンネルを経由してL2TPの制御フレームをやり取りし、クライアントの認証と仮想ネットワーク内で用いるアドレスの割り当てを行ないます。これによって、IPSecトンネル内にさらにL2TPトンネルが作成されます。ここまでで、「安全なVPN」の作成は完了しました。

- Step 3

    以降は、L2TP/IPSecトンネルを経由してデータ通信を行ないます。この際、L2TPのデータフレーム内にIPパケットがカプセル化され、このデータフレームをIPSecのESP (Encapsulating Security Payload)で暗号化したデータをやり取りします。

これで、いちおう理屈はわかりましたので、今後実際にL2TP/IPSec VPNサーバの構築を行なっていくことにします。

### 参考文献
1. ここが知りたいVPN 第2回 IPsecの基本を知る, http://www.tatsuyababa.com/NW-VPN/NW-200404-VPN02.pdf
1. ここが知りたいVPN 第5回 L2TPを使用したリモートアクセスVPNの仕組み, http://www.tatsuyababa.com/NW-VPN/NW-200407-VPN05.pdf
1. L2TP/IPsec, http://beginners-network.com/vpn_l2tp_ipsec.html
1. L2TP/IPsec, http://www.rtpro.yamaha.co.jp/RT/docs/l2tp_ipsec/
