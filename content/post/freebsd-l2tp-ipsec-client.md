+++
title = "FreeBSDでL2TP/IPSec VPNサーバを構築する - クライアント編"
date = "2018-04-17T19:54:00+09:00"
draft = true
categories = ["FreeBSD"]
tags = ["freebsd", "vpn", "l2tp", "ipsec", "chromiumos", "chromeos", "chromebook", "android"]
+++

[IPSec編](/post/freebsd-l2tp-ipsec-ipsec/)から[NAT・ファイアウォール編](/post/freebsd-l2tp-ipsec-firewall/)までかけて、L2TP/IPSecサーバのセットアップとルータ兼ファイアウォールの設定を行ないました。これでようやく、クライアントがL2TP/IPSec VPN経由でインターネットにアクセスする準備がととのったわけです。

本記事では、L2TP/IPSecサーバに接続するための、クライアント側の設定について説明していきます。対象のクライアントOSは、Chromium OS (Chrome OS)およびAndroidとします。[IPSec編](/post/freebsd-l2tp-ipsec-ipsec/)で確認、設定した**サーバのIPアドレス**と**事前共有鍵**、および[L2TP編](/post/freebsd-l2tp-ipsec-l2tp/)で設定した**ユーザ名**、**パスワード**が必要になりますので、手もとに準備をお願いします。

OpenVPNサーバを構築したときは、公開鍵証明書を用いましたので手順がやや煩雑でしたが、今回は(やむを得ずですが)、事前共有鍵を使いますので手順は比較的簡単になっています。また、L2TP/IPSecの場合、クライアント機能がいずれのOSにも含まれていますので、OpenVPNでは必要だった**アプリのインストールが不要**です。では、Chromium OSとAndroidそれぞれについて、VPNクライアントの設定を見ていきましょう。

- [Chromium OSの場合](#chromium-os)
- [Androidの場合](#android)

### Chromium OS
最初に、アカウント画像をクリックします。すると、アカウント情報の表示や各種設定を行なうサブウィンドウが表示されますので、設定(歯車)アイコンをクリックします。

![Chromium OS - 設定サブウィンドウ](/img/chromiumos/chromiumos-shelf-setting.png)

ネットワークの「接続の追加」を展開し、「OpenVPN / L2TPを追加…」をクリックします。

![Chromium OS - ネットワーク - 接続の追加](/img/chromiumos/chromiumos-settings-network.png)

すると、「プライベートネットワークを追加」の画面になりますので、手もとに用意しておいたIPアドレスなどの情報をもとに、設定画面の各項目を埋めていきます。そして、最後に接続ボタンをクリックします。

![Chromium OS - ネットワーク - プライベートネットワークの追加](/img/chromiumos/chromiumos-settings-l2tp-ipsec-psk.png)

ネットワークに「VPN」の項目が現れ、接続を試みている旨のメッセージが表示されます。

![Chromium OS - ネットワーク - VPN接続中](/img/chromiumos/chromiumos-settings-network-connecting.png)

接続が成功すると、WiFiアイコンに鍵マークが追加され、VPN経由での接続中であることを示します。

![Chromium OS - 鍵付きWiFiアイコン](/img/chromiumos/chromiumos-shelf-network-key.png)

以上でChromium OSのVPN接続は完了です。安全な接続でインターネットをお楽しみください。

### Android
まず、設定アプリを起動して「ネットワークとインターネット」→「VPN」を選択します。VPNの一覧表示画面になりますので、右上の+ (プラス)アイコンをタップします。

![Android - ネットワーク - VPN一覧](/img/android/android-network-vpn-none.png)

すると、「VPNプロファイルの編集」ダイアログが表示されますので、手もとに用意しておいたIPアドレスなどの情報をもとに、ダイアログの各項目を埋めていきます。必要な情報が入力できたら、右下の保存ボタンをタップします。

![Android - ネットワーク - VPNプロファイル編集](/img/android/android-network-vpn-add.png)

再びVPNの一覧表示画面に戻りますので、いま追加したVPNの名前が表示されていることを確認してください。OKであれば、VPNの名前をタップします。

![Android - ネットワーク - VPN一覧2](/img/android/android-network-vpn-connect.png)

そうすると、認証情報の入力画面になります。すでに、ユーザ名とパスワードが入力済みになっていると思いますので、右下の接続ボタンをタップしてください。もし、VPNプロファイルの編集画面でユーザ名とパスワードを設定しなかった場合は、ここでユーザ名とパスワードの入力をお願いします。

![Android - ネットワーク - 認証画面](/img/android/android-network-vpn-auth.png)

接続が成功すると、通知バーに鍵アイコンが追加され、VPN経由での接続中であることを示します。また、VPNの一覧画面にも接続が成功した旨のメッセージが表示されていると思います。

![Android - ネットワーク - VPN一覧3](/img/android/android-network-vpn-connected.png)

以上でAndroidのVPN接続は完了です。安全な接続でインターネットをお楽しみください。

### イメージクレジット
本記事において、Chromium OSのデスクトップ壁紙として使用している画像は、ロシア・キーロフスク在住の[Fox Grom氏](https://vk.com/id153817456)によるものです。氏のアルバムは以下のURLで閲覧できます。

- [Fox's photos (VK)](https://vk.com/albums153817456)
