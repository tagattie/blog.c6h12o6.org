+++
title = "FreeBSDでVPNサーバを構築する - OpenVPNクライアント(Android)編"
date = "2018-03-18T21:37:00+09:00"
categories = ["Android"]
tags = ["freebsd", "vpn", "server", "openvpn", "wifi", "android", "client"]
+++

[OpenVPNクライアント(Windows)編](/post/freebsd-openvpn-server-client/)では、Windowsマシンに[OpenVPN](https://openvpn.net/)をインストールし、VPNクライアントの設定を行ないました。VPNを構築するそもそもの目的は、「暗号化されていない公衆無線LANで安全に通信する」だったわけです。PCだと、カフェなどで作業するときにインターネットを使いたいから公衆WiFiを使う、というシーンはよくあります。

しかし、持ち歩いている人の数から考えると、外出中にスマートフォンやタブレットでネットを使う際に、「ギガを節約したいから」という理由などで公衆WiFiを利用するというケースのほうが多いような気がしてきました。

そこで、本記事ではAndroid端末を対象として、OpenVPNクライアントのインストールおよび設定を説明していきます。

### クライアント証明書の発行
Windowsマシンにクライアントをセットアップしたときと同様、まずクライアント証明書を発行しておきましょう。後ほどAndroid端末にコピーする証明書関連のファイルはWindowsのときと同様、以下の3つです。

- `root-ca.crt`: Root CAの証明書
- `vpnclient.crt.full`: クライアントのフルチェーン証明書
- `vpnclient.key`: クライアントの秘密鍵(パスフレーズあり)

証明書の発行手順は[こちら](/post/freebsd-private-ca-cert/)を参照してください。

### 設定ファイルの準備
今回紹介するOpenVPNクライアントは、いずれも設定ファイルをインポートする機能を備えています。モバイル端末上でややこしい設定をするのは大変なので、Windows/Mac PCやFreeBSDマシンであらかじめ設定ファイルを準備して、これをモバイル端末にコピーし、クライアントにインポートするやり方が便利です。

OpenVPNサーバを構築したFreeBSDマシンで準備する場合は、クライアント用のサンプル設定がインストールされていますので、これをコピーして編集します。

``` shell
cd <任意のディレクトリ>
cp /usr/local/share/examples/openvpn/sample-config-files/client.conf ./vpnserver.ovpn
```

設定ファイルの内容はWindowsクライアントと**まったく同じ**です。[OpenVPNクライアント(Windows)編](/post/freebsd-openvpn-server-client/)をご参照ください。また、以下のGitHubリポジトリの`client`ディレクトリに完全な設定ファイルがありますので、必要に応じてこちらもご参照ください。

- [FreeBSD-OpenVPN](https://github.com/tagattie/FreeBSD-OpenVPN) - Sample configuration files for setting up an OpenVPN server/client on FreeBSD

### 証明書と設定ファイルの端末へのコピー
Windowsのときと同様、コピーするファイルは全部で以下の5つです。

- `root-ca.crt`: Root CAの証明書
- `vpnclient.crt.full`: クライアントのフルチェーン証明書
- `vpnclient.key`: クライアントの秘密鍵(パスフレーズあり)
- `ta.key`: HMAC用の共有秘密鍵
- `vpnserver.ovpn`: クライアントの設定ファイル

FreeBSDマシンで準備作業を行なった場合は、これらのファイルを1つのディレクトリにまとめておいて、以下の要領でファイルをAndroid端末に転送します。

``` shell-session
$ cd <任意のディレクトリ>                                             # ディレクトリにcd
$ ls                                                                  # ディレクトリの内容確認
root-ca.crt         vpnclient.crt.full  vpnserver.ovpn
ta.key              vpnclient.key
$ adb shell mkdir -p /sdcard/OpenVPN                                  # 端末上にOpenVPN用のフォルダを作成
$ adb push * /sdcard/OpenVPN/                                         # 端末のOpenVPNフォルダにファイルをコピー
root-ca.crt: 1 file pushed. 0.1 MB/s (2844 bytes in 0.037s)
ta.key: 1 file pushed. 0.1 MB/s (636 bytes in 0.006s)
vpnclient.crt.full: 1 file pushed. 0.3 MB/s (2059 bytes in 0.007s)
vpnclient.key: 1 file pushed. 0.4 MB/s (1766 bytes in 0.005s)
vpnserver.ovpn: 1 file pushed. 0.6 MB/s (3649 bytes in 0.006s)
5 files pushed. 0.1 MB/s (10954 bytes in 0.074s)
```

### OpenVPNクライアントのインストール
ここからは端末側での作業になります。まず、OpenVPNクライアントをインストールしましょう。以下のいずれかをGoogle Playからインストールしてください。

- [OpenVPN for Android](https://play.google.com/store/apps/details?id=de.blinkt.openvpn) - 個人的にはこちらが好き
- [OpenVPN Connect](https://play.google.com/store/apps/details?id=net.openvpn.openvpn) - OpenVPN Technologies社公式アプリ

OpenVPN ConnectはOpenVPN Technologies社が開発する公式アプリです。個人的な好みはOpenVPN for Androidですが、どちらでもそれほど使用感にかわりはないと思います。

ただし、OpenVPN Connectを使う場合は一点だけ**注意**をお願いします。それは、**`fragment`ディレクティブに対応していない**、ということです。したがって、OpenVPN Connectを使う場合は、サーバ設定およびクライアント設定の以下の部分を削除するかコメントアウトしてください。

- サーバ設定

    ``` conf
fragment 1354
```

- クライアント設定

    ``` conf
fragment 1354
mssfix 1354
```

いずれも、それぞれの設定ファイルの一番最後の部分です。

### OpenVPNアプリの設定
さあ、残る作業は端末に転送しておいた設定ファイルをOpenVPNアプリにインポートして、サーバへの接続確認をするだけです。2つのアプリケーションそれぞれについて手順を示しました。インストールしたほうの手順を参考にしていただければと思います。

- [OpenVPN for Android](#openvpn-for-androidの場合)
- [OpenVPN Connect](#openvpn-connectの場合)

#### OpenVPN for Androidの場合
アプリを起動すると以下の画面が現れます。右上にあるインポートアイコン(箱に下向きの矢印が付いているアイコン)をタップします。

![OpenVPN for Android起動画面](/img/openvpn/openvpn-for-android-initial.png)

すると、ファイルを選択する画面になります。さきほどファイルを転送しておいたフォルダ`/sdcard/OpenVPN`に移動します。そして、`vpnserver.ovpn`ファイルをタップします。

![OpenVPN for Android ovpnファイル選択画面](/img/openvpn/openvpn-for-android-select-ovpn.png)

すると、証明書関連のファイルを選択する画面になりますので、CA証明書(`root-ca.crt`)、TLS認証ファイル(`ta.key`)、クライアント証明書のキー(`vpnclient.key`)、クライアント証明書(`vpnclient.crt.full`)をそれぞれ選択してください。(すべて同じフォルダにファイルがありますので楽だと思います。) そして、最後に右上のチェックマークをタップします。

![OpenVPN for Android証明書選択画面](/img/openvpn/openvpn-for-android-select-cert.png)

プロファイルのインポートが完了すると、プロファイルの一覧画面になります。`vpnserver`をタップして接続を開始します。

![OpenVPN for Android接続画面](/img/openvpn/openvpn-for-android-connect.png)

秘密鍵のパスフレーズを求められますので入力します。

![OpenVPN for Androidパスフレーズ入力画面](/img/openvpn/openvpn-for-android-passphrase.png)

接続が完了すると以下の画面になります。右上のほうにVPNの状態が表示されていますので、「接続しました」となっていることを確認してください。

![OpenVPN for Android接続成功画面](/img/openvpn/openvpn-for-android-success.png)

以上でVPN接続は完了です。

#### OpenVPN Connectの場合
アプリを起動すると以下の画面が現れます。一番下にある"OVPN Profile"をタップします。

![OpenVPN Connect起動画面](/img/openvpn/openvpn-connect-initial.png)

すると、プロファイルをインポートする画面になります。`vpnserver.ovpn`をタップし、右側にチェックマークがついていることを確認した後、右上の"IMPORT"をタップします。

![OpenVPN Connect ovpnファイル選択画面](/img/openvpn/openvpn-connect-select-ovpn.png)

すると、プロファイルのタイトル情報入力画面になります。自分のわかりやすいようなタイトルを入力した後、右上の"ADD"をタップします。

![OpenVPN Conectプロファイルタイトル入力画面](/img/openvpn/openvpn-connect-set-profile-title.png)

プロファイルのインポートが完了すると、プロファイルの一覧画面になります。インポートしたプロファイルが表示されているのを確認した後、これをタップして接続を開始します。

![OpenVPN Connect接続画面](/img/openvpn/openvpn-connect-connect.png)

秘密鍵のパスフレーズを求められますので入力します。

![OpenVPN Connectパスフレーズ入力画面](/img/openvpn/openvpn-connect-passphrase.png)

接続が完了すると以下の画面になります。プロファイルの表示が緑色に変わり、"CONNECTED"と表示されていることを確認してください。

![OpenVPN Connect接続成功画面](/img/openvpn/openvpn-connect-success.png)

以上でVPN接続は完了です。

### 参考文献
1. OpenVPN, https://openvpn.net/
1. OpenVPN for Android, https://play.google.com/store/apps/details?id=de.blinkt.openvpn
1. OpenVPN Connect (Android版), https://play.google.com/store/apps/details?id=net.openvpn.openvpn
1. OpenVPN Connect (iOS版), https://itunes.apple.com/jp/app/openvpn-connect/id590379981
