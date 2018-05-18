+++
title = "IntraアプリでAndroid PのプライベートDNS機能をひと足先に試す"
date = "2018-05-18T20:22:00+09:00"
categories = ["Android"]
tags = ["android", "security", "intra", "dns", "tls", "https", "private"]
+++

先日開催された[Google I/O 2018](https://events.google.com/io/)にあわせて[Android Pのベータ版](https://www.android.com/beta)提供が始まりました。もう入手されたかたはいらっしゃいますか? [Project Treble](https://source.android.com/devices/architecture/treble)の成果によって、ベータ版の対象端末がPixelシリーズ以外にも増えたのはうれしいかぎりですね。

わたしはというと、手もとにあるNexus 6が対象に含まれるわけもなく、当分は指をくわえて眺めているだけになりそうです。

以下の二記事によれば、Android Pの新機能として、WiFiアクセスポイントとの間の通信遅延時間(RTT, Round Trip Time)を用いた屋内測位機能の導入、切り欠き(ノッチ)画面のサポート、メッセージ通知の改善、複数カメラ搭載端末のためのマルチカメラAPIの導入、などがあるそう	です。

- [Previewing Android P (Android Developers Blog)](https://android-developers.googleblog.com/2018/03/previewing-android-p.html)
- [Android P開発者プレビュー公開。ノッチのサポートや通知欄の変更、HEIF画像対応も (Engadget)](https://japanese.engadget.com/2018/03/07/android-p-heif/)

また、以下の二記事によると、セキュリティ関連の機能としてDNS over TLSが新たにサポートされるようですね。Android Pでは「プライベートDNS」と呼ばれています。

- [DNS over TLS support in Android P Developer Preview (Android Developers Blog)](https://android-developers.googleblog.com/2018/04/dns-over-tls-support-in-android-p.html)
- [次期 Android では 「DNS over TLS」 がサポートされる (WWW WATCH)](https://hyper-text.org/archives/2018/05/android_p_dns_over_tls_support.shtml)

DNS over TLSとは、文字どおり、DNS通信をTLS (Transport Layer Security)というセキュアなコネクションを用いて行なうものです。みなさんご存知のHTTPSはHTTP通信をTLS上で行ないますので、HTTPが安全になるのと同様の理屈でDNSも安全になると考えていただければOKです。

DNS over TLS、あるいはAndroid Pで「プライベートDNS」と呼ばれているセキュアDNS通信ですが、実はAndroid Pを待たなくても試すことができます。その方法は、以下の記事で紹介されている[Intra](https://play.google.com/store/apps/details?id=app.intra)というアプリを使う、というものです。

- ['Intra' brings Android P's DNS-over-TLS to older devices (Android Police)](https://www.androidpolice.com/2018/05/16/intra-brings-one-android-ps-best-features-older-devices/)
- [Intra (Google Play)](https://play.google.com/store/apps/details?id=app.intra)

正確にいうと、Intraで試せるのはDNS over TLSではなくDNS over HTTPSなのですが、いずれにしてもセキュアなコネクションを用いてDNS通信を行なうという点では変わりません。ユーザ視点では大きな違いはないと思って大丈夫です。

Intraアプリを起動すると以下の画面が表示されます。画面下部の"System details"を見ると、セキュアでない(暗号化されていない)DNSプロトコルを使用中であること、およびネットワークプロバイダ(MNO or MVNO)が提供するDNSサーバのIPアドレスが表示されていると思います。DNS over HTTPSを有効化するには、画面上部の"ENABLE PROTECTION"をタップします。

![Intra - 無効](/img/android/intra-disabled.png)

すると、IntraアプリがVPN接続をリクエストしてきますので、許可する場合はOKをタップします。

![Intra - VPN有効化](/img/android/intra-enable-vpn.png)

DNS問い合わせ(クエリ)をセキュアコネクションを用いて行なうための操作はこれだけです。画面表示が以下のように変化し、DNSクエリが保護されていることがわかります。

![Intra - 有効](/img/android/intra-enabled.png)

DNS over HTTPSに対応するDNSサーバは、Google社提供のものとCloudflare社提供のものから選択できます。

![Intra - DNSプロバイダ選択](/img/android/intra-choose-dns-provider.png)

また、画面下部の"Show recent queries"にチェックを入れると、直近のDNSクエリを表示できます。

![Intra - 直近のクエリ](/img/android/intra-recent-queries.png)

以上、セキュアなDNS通信をAndroid Oreo以前でも試すことのできるIntraアプリの紹介でした。

### 参考文献
1. Previewing Android P, https://android-developers.googleblog.com/2018/03/previewing-android-p.html
1. Android P開発者プレビュー公開。ノッチのサポートや通知欄の変更、HEIF画像対応も, https://japanese.engadget.com/2018/03/07/android-p-heif/
1. DNS over TLS support in Android P Developer Preview, https://android-developers.googleblog.com/2018/04/dns-over-tls-support-in-android-p.html
1. 次期 Android では 「DNS over TLS」 がサポートされる, https://hyper-text.org/archives/2018/05/android_p_dns_over_tls_support.shtml
1. 'Intra' brings Android P's DNS-over-TLS to older devices, https://www.androidpolice.com/2018/05/16/intra-brings-one-android-ps-best-features-older-devices/
1. Intra, https://play.google.com/store/apps/details?id=app.intra
