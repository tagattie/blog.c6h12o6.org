+++
title = "FreeBSDでVPNサーバを構築する - OpenVPNクライアント(Windows)編"
date = "2018-03-15T21:11:00+09:00"
categories = ["FreeBSD"]
tags = ["freebsd", "vpn", "server", "openvpn", "wifi", "windows", "client"]
+++

[OpenVPNサーバ編](/post/freebsd-openvpn-server-server/)では、FreeBSDマシンにOpenVPNをインストールし、VPNサーバの設定を行ないました。本記事では、三つ目のパートとして、Windowsマシンへの[OpenVPN](https://openvpn.net/)のインストールとVPNクライアントの設定を行ないます。その後、VPNクライアントとVPNサーバとの間で最低限の通信ができることを確認します。

### クライアント証明書の発行
OpenVPNをインストールする前に、まずクライアント証明書を発行しておきましょう。手順は「[証明書を発行](/post/freebsd-private-ca-cert/)」で述べたとおりです。コモンネーム(Common Name, CN)には、[ネットワーク設計編](/post/freebsd-openvpn-server-network/)で決定しておいたドメイン名`vpnclient.example.org`を指定します。

後ほど、以下のファイルをWindowsマシンにコピーしますので覚えておいてください。

- `root-ca.crt`: Root CAの証明書
- `vpnclient.crt.full`: クライアントのフルチェーン証明書
- `vpnclient.key`: クライアントの秘密鍵(パスフレーズあり)

クライアントの秘密鍵について、パスフレーズなしのものを用いるのでもかまいませんが、安全のため、クライアントの秘密鍵はパスフレーズで保護されているものを使うことを推奨します。(VPN接続を開始するたびにパスフレーズの入力を求められますが、安全重視でお願いします。)

### OpenVPNのインストール
次に、OpenVPNをインストールします。

OpenVPNの[Community Downloads](https://openvpn.net/index.php/download/community-downloads.html)からWindows用のインストーラをダウンロードします。ダウンロードが終わったら、インストーラをダブルクリックしてインストールを開始してください。途中いくつか確認を要する画面が現れますが、どれもデフォルト設定のままでOKです。

デフォルトでは`C:\Program Files\OpenVPN`以下に一連のファイルがインストールされます。ディレクトリ構成は以下のようになります。

```
C:\Program Files\OpenVPN
├ bin            - 実行ファイル、DLL
├ config         - 設定ファイル
├ doc            - ドキュメント
├ log            - ログ
└ sample-config  - サンプル設定ファイル
```

サンプル設定ファイルのディレクトリに`client.ovpn`というファイルがあります。今回はクライアントの設定をしますので、このファイルをひな型として使用します。本ファイルを設定ファイルのディレクトリにコピーしましょう。ファイル名は、接続先を示すものに変更しておくとわかりやすいです。本記事では`vpnserver.example.com`に接続しに行きますので、`vpnserver.ovpn`という名前に変更します。

また、[クライアント証明書の発行](#クライアント証明書の発行)であげた3つのファイルを設定ファイルのディレクトリにコピーします。さらに、[OpenVPNサーバ編](/post/freebsd-openvpn-server-server/)で生成しておいた、HMAC用の共有秘密鍵(`/usr/local/etc/openvpn/ta.key`)も設定ファイルのディレクトリにコピーします。

結果として、設定ファイルディレクトリ(`C:\Program Files\OpenVPN\config`)には以下の6つのファイルがあるはずです。

```
README.txt          - もともとあったREADME.txt
root-ca.crt         - Root CAの証明書
ta.key              - HMAC用の共有秘密鍵
vpnclient.crt.full  - クライアントのフルチェーン証明書
vpnclient.key       - クライアントの秘密鍵(パスフレーズあり)
vpnserver.ovpn      - クライアント設定ファイル
```

### OpenVPNの設定
では、次にOpenVPNクライアントの設定ファイル`vpnserver.ovpn`の内容について、主要な部分を見ていきます。完全な設定ファイルは以下のGitHubリポジトリに置いてありますので、必要に応じでご参照ください。

- [FreeBSD-OpenVPN](https://github.com/tagattie/FreeBSD-OpenVPN) - Sample configuration files for setting up an OpenVPN server/client on FreeBSD

- `vpnserver.ovpn`

    - [L13-16](https://github.com/tagattie/FreeBSD-OpenVPN/blob/master/client/vpnserver.ovpn#L13): クライアントであることを宣言

        ``` conf
# Specify that we are a client and that we
(snip)
client
```

        クライアントであることを最初に宣言します。

    - [L18-24](https://github.com/tagattie/FreeBSD-OpenVPN/blob/master/client/vpnserver.ovpn#L18): ネットワークデバイスの設定
    
        ``` conf
# Use the same setting as you are using on
(snip)
;dev tap
dev tun
```

        デフォルトの`tun`デバイスを使います。サーバ側の設定と同じにしてください。

    - [L33-37](https://github.com/tagattie/FreeBSD-OpenVPN/blob/master/client/vpnserver.ovpn#L33): トランスポートプロトコルの設定
    
        ``` conf
# Are we connecting to a TCP or
(snip)
;proto tcp
proto udp
```

        デフォルトのトランスポートはUDPです。サーバ側の設定と同じにしてください。

    - [L39-43](https://github.com/tagattie/FreeBSD-OpenVPN/blob/master/client/vpnserver.ovpn#L33): 接続先サーバの設定

        ``` conf
# The hostname/IP and port of the server.
(snip)
remote vpnserver.example.com 1194
;remote my-server-2 1194
```

        接続先VPNサーバのドメイン名とポート番号を指定します。[ネットワーク設計編](/post/freebsd-openvpn-server-network/)で確認しておいた、サーバのドメイン名を指定します。また、サーバのデフォルト待ち受けポート番号は1194です。こちらはサーバ側の設定と同じにしてください。

    - [L82-90](https://github.com/tagattie/FreeBSD-OpenVPN/blob/master/client/vpnserver.ovpn#L82): クライアント証明書の設定

        ``` conf
# SSL/TLS parms.
(snip)
ca ca.crt
cert vpnclient.crt.full
key vpnclient.key
```

        設定ファイルディレクトリにコピーしておいた、証明書関連の各ファイルのファイル名を指定します。

    - [L110-117](https://github.com/tagattie/FreeBSD-OpenVPN/blob/master/client/vpnserver.ovpn#L110): 暗号・ハッシュ関数の設定

        ``` conf
# Select a cryptographic cipher.
(snip)
cipher AES-256-CBC
auth SHA256
```

        認証に使うハッシュ関数SHA256を追加で指定します。(デフォルトはSHA1で強度が不十分なため。)

    - [L130-131](https://github.com/tagattie/FreeBSD-OpenVPN/blob/master/client/vpnserver.ovpn#L130): パケットサイズの設定

        ``` conf
fragment 1354
mssfix 1354
```

         パケットのフラグメンテーションを防ぐため小さめの値を設定してあります。お使いの環境に合わせて調整していただいてかまいませんが、サーバ側の設定値と同じにしてください。

以上でクライアントの設定は終了です。

### OpenVPNの起動
設定ができましたので、次はいよいよOpenVPNの起動です。スタートメニュー→OpenVPNと進み、OpenVPN GUIを選択してGUIプログラムを起動します。そうすると、ツールバーにOpenVPN GUIのアイコンが追加されます。

![OpenVPN GUIのツールバーアイコン](/img/openvpn/openvpn-windows-toolbar-icon-small.png)

このアイコンを右クリックするとメニューが出ますので、「接続」を選びます。

![OpenVPN GUIアイコンを右クリック](/img/openvpn/openvpn-windows-menu-small.png)

GUIが起動しログ画面が表示されます。また、パスワードを求められますので、秘密鍵のパスフレーズを入力します。

![OpenVPN GUIのパスフレーズ入力画面](/img/openvpn/openvpn-windows-passphrase-small.png)

すると、接続処理が始まります。接続が成功すると、アドレスが割り当てられた旨の通知が画面右下に出ます。以上で接続完了です。

![OpenVPNの接続完了通知](/img/openvpn/openvpn-windows-connect-success-small.png)

### クライアント−サーバ間の導通確認
最後に、VPNサーバとの通信ができるか確認しましょう。最低限の通信ができることを`ping`コマンドを用いて確認します。VPN内でのVPNサーバのIPアドレスである`172.16.0.1`に対して`ping`を実行します。

``` cmd-session
C:\Users\XXXXXX>ping 172.16.0.1

172.16.0.1 に ping を送信しています 32 バイトのデータ:
172.16.0.1 からの応答: バイト数 =32 時間 =6ms TTL=63
172.16.0.1 からの応答: バイト数 =32 時間 =7ms TTL=63
172.16.0.1 からの応答: バイト数 =32 時間 =6ms TTL=63
172.16.0.1 からの応答: バイト数 =32 時間 =6ms TTL=63

172.16.0.1 の ping 統計:
    パケット数: 送信 = 4、受信 = 4、損失 = 0 (0% の損失)、
ラウンド トリップの概算時間 (ミリ秒):
    最小 = 6ms、最大 = 7ms、平均 = 6ms
```

上記のように応答が返ってくればOKです。これで、OpenVPNサーバとの間に安全な通信路(トンネル)が確立しました。クライアントからの通信はすべてトンネルを経由するよう、サーバ側で設定してありますので、暗号化されていない公衆無線LANでも安全な通信が可能です。

めでたし、といいたいところなのですが、実はいままでの設定だけでは**VPNサーバとだけ**しか通信できません。そうではなくて、VPNサーバを経由してインターネット上のいろいろなサービスを安全に利用すること、がやりたいことですよね?

次回は、VPNサーバを介してVPNクライアントがインターネットと通信できるよう、VPNサーバをNATルータとして動作させるための設定を説明します。

### 参考文献
1. OpenVPN, https://openvpn.net/
