+++
title = "FreeBSDでVPNサーバを構築する - OpenVPNサーバ編"
date = "2018-03-14T20:20:00+09:00"
categories = ["FreeBSD"]
tags = ["freebsd", "vpn", "server", "openvpn", "wifi"]
+++

[ネットワーク設計編](/post/freebsd-openvpn-server-network/)では、VPNサーバを構築する一つ目のパートとして、ネットワークアドレスに関する確認と設計を行ないました。本記事では、二つ目のパートとして、FreeBSDマシンへの[OpenVPN](https://openvpn.net/)のインストールとVPNサーバの設定を行ないます。

### サーバ証明書の発行
OpenVPNをインストールする前に、まずサーバ証明書を発行しておきましょう。手順は「[証明書を発行](/post/freebsd-private-ca-cert/)」で述べたとおりです。コモンネーム(Common Name, CN)には、[ネットワーク設計編](/post/freebsd-openvpn-server-network/)で確認しておいたドメイン名`vpnserver.example.com`を指定します。証明書の発行が終わったら、以下のファイルをFreeBSDマシンに格納しておいてください。

- `/etc/ssl/root-ca.crt`: Root CAの証明書
- `/etc/ssl/certs/vpnserver.crt.full`: サーバのフルチェーン証明書
- `/etc/ssl/private/vpnserver_nopass.key`: サーバの秘密鍵(パスフレーズなし)

パスフレーズなしの秘密鍵を用いるのは、OpenVPNの起動時に毎回パスフレーズを入力する必要をなくすためです。

### OpenVPNのインストール
次に、OpenVPNをインストールします。

``` shell
pkg install openvpn
```

以下のディレクトリに設定ファイルのサンプルがインストールされます。

``` shell-session
$ ls /usr/local/share/examples/openvpn/sample-config-files
README                loopback-server       static-home.conf
client.conf           loopback-server.orig  static-office.conf
firewall.sh           loopback-server.test  tls-home.conf
home.up               office.up             tls-office.conf
loopback-client       openvpn-shutdown.sh   xinetd-client-config
loopback-client.orig  openvpn-startup.sh    xinetd-server-config
loopback-client.test  server.conf
```

たくさんファイルがありますね。今回はサーバの設定をするので`server.conf`をひな型として使用します。本ファイルをOpenVPNの設定ディレクトリにコピーします。

``` shell
mkdir -p /usr/local/etc/openvpn
cp /usr/local/share/examples/openvpn/sample-config-files/server.conf /usr/local/etc/openvpn/openvpn.conf
```

### セキュリティ関連の追加ファイルの生成
ファイルをコピーしたら設定ディレクトリに`cd`します。ひな型ファイルの中にも説明がありますが、セキュリティに関連してローカルに生成する必要のあるファイルが2つあります。以下のコマンドを実行してこれらのファイルを生成します。

``` shell
cd /usr/local/etc/openvpn
openssl dhparam -out dh2048.pem 2048    # Diffie-Helmanパラメータの生成
openvpn --genkey --secret ta.key        # HMAC用の共有秘密鍵の生成
```

### OpenVPNの設定
では、いよいよOpenVPNの設定ファイル`openvpn.conf`の内容について、主要な部分を見ていきます。完全な設定ファイルは以下のGitHubリポジトリに置いてありますので、必要に応じてご参照ください。

- [FreeBSD-OpenVPN](https://github.com/tagattie/FreeBSD-OpenVPN) - Sample configuration files for setting up an OpenVPN server/client on FreeBSD

- `openvpn.conf`

    - [L27-32](https://github.com/tagattie/FreeBSD-OpenVPN/blob/master/server/openvpn.conf#L27): 待ち受けポートの設定

        ``` conf
# Which TCP/UDP port should OpenVPN listen on?
(snip)
port 1194
```

        デフォルトの待ち受けポートは1194です。変更の必要がなければデフォルトを使います。

    - [L34-36](https://github.com/tagattie/FreeBSD-OpenVPN/blob/master/server/openvpn.conf#L34): トランスポートプロトコルの設定

        ``` conf
# TCP or UDP server?
;proto tcp
proto udp
```

        デフォルトのトランスポートはUDPです。変更の必要がなければデフォルトを使います。
	
        注: VPNクライアントがファイアウォールの内側にあって、HTTPプロキシ経由でしか外部のネットワークにアクセスできないような場合にはTCPを使う必要があります。

    - [L38-53](https://github.com/tagattie/FreeBSD-OpenVPN/blob/master/server/openvpn.conf#L38): トンネルデバイスの設定

        ``` conf
# "dev tun" will create a routed IP tunnel,
# "dev tap" will create an ethernet tunnel.
(snip)
;dev tap
dev tun
```

        デフォルトの`tun`デバイスを使います。

    - [L63-80](https://github.com/tagattie/FreeBSD-OpenVPN/blob/master/server/openvpn.conf#L63): サーバ証明書の設定

        ``` conf
# SSL/TLS root certificate (ca), certificate
# (cert), and private key (key).  Each client
(snip)
ca /etc/ssl/root-ca.crt
cert /etc/ssl/certs/vpnserver.crt.full
key /etc/ssl/private/vpnserver_nopass.key  # This file should be kept secret
```

        [サーバ証明書の発行](#サーバ証明書の発行)で格納しておいた、証明書関連の各ファイルのパスを指定します。

    - [L87-92](https://github.com/tagattie/FreeBSD-OpenVPN/blob/master/server/openvpn.conf#L87): クライアントごとに/30ネットワークを割り当てるかの設定

        ``` conf
# Network topology
# Should be subnet (addressing via IP)
(snip)
topology subnet
```

        デフォルトでは`net30` (クライアントごとに/30ネットワークを割り当てる)となっていますが、バージョンの古いOpenVPNクライアントをサポートする必要が今回はありませんので`subnet`を指定します。

    - [L94-102](https://github.com/tagattie/FreeBSD-OpenVPN/blob/master/server/openvpn.conf#L94): VPNのネットワークアドレスの設定

        ``` conf
# Configure server mode and supply a VPN subnet
(snip)
server 172.16.0.0 255.255.255.0
server-ipv6 fd20:10d:b800::/64
```

        [ネットワーク設計編](/post/freebsd-openvpn-server-network/)で決定しておいた、VPNのネットワークアドレス空間を指定します。

    - [L165-172](https://github.com/tagattie/FreeBSD-OpenVPN/blob/master/server/openvpn.conf#L165): クライアントごとに固定IPアドレスを割り当てる設定

        ``` conf
# EXAMPLE: Suppose you want to give
# Thelonious a fixed VPN IP address of 10.9.0.1.
# First uncomment out these lines:
client-config-dir ccd
route 172.16.0.0 255.255.255.0
route-ipv6 fd20:10d:b800::/64
# Then add this line to ccd/Thelonious:
#   ifconfig-push 10.9.0.1 10.9.0.2
```

        個々のVPNクライアントごとに固定のIPアドレスを割り当てるための設定です。[ネットワーク設計編](/post/freebsd-openvpn-server-network/)で決定しておいた、VPNのネットワークアドレス空間を指定します。
        
        さらに、以下のファイルを作成します。ファイル名は**クライアントのコモンネーム(Common Name, CN)と同じ**にする必要があります。
    
        - `/usr/local/etc/openvpn/ccd/vpnclient.example.org`
    
            ``` conf
ifconfig-push 172.16.0.11 255.255.255.0
ifconfig-ipv6-push fd20:10d:b800::172:16:0:11/64
```
    
            [ネットワーク設計編](/post/freebsd-openvpn-server-network/)で決定しておいた、VPNクライアントのアドレスを指定します。

        注: もし、クライアントへのアドレス割り当てを固定にする必要がなければ、本項の設定を行なう必要はありません。コメントアウトしておいてください。

    - [L186-195](https://github.com/tagattie/FreeBSD-OpenVPN/blob/master/server/openvpn.conf#L186): **クライアントからの通信をすべてVPNサーバを経由させる設定(重要)**

        ``` conf
# If enabled, this directive will configure
(snip)
push "redirect-gateway def1 bypass-dhcp"
push "route-ipv6 2000::/3"
```

        VPNクライアントが行なう通信を、IPv4およびIPv6ともに**すべてVPNサーバを経由**させるようにする設定です。この設定を行なわないとクライアントからの通信がまったくVPNサーバを経由しません。今回、VPNサーバを構築する主目的が「暗号化されていない公衆無線LANで安全に通信する」ですので、すべての通信が暗号化されたトンネルを通るよう、本設定を**必ず**行なってください。

    - [L197-206](https://github.com/tagattie/FreeBSD-OpenVPN/blob/master/server/openvpn.conf#L197): クライアントに提供するネットワーク関連オプションの設定

        ``` conf
# Certain Windows-specific network settings
(snip)
push "dhcp-option DNS 208.67.222.222"
push "dhcp-option DNS 208.67.220.220"
push "dhcp-option DNS6 2620:0:ccc::2"
push "dhcp-option DNS6 2620:0:ccd::2"
```

        ここはお好みで設定してください。本記事の例では、IPv4およびIPv6のDNSサーバとしてOpenDNS社が提供しているパブリックDNSサーバを使うよう設定しています。

    - [L251-258](https://github.com/tagattie/FreeBSD-OpenVPN/blob/master/server/openvpn.conf#L251): 暗号・ハッシュ関数の設定 

        ``` conf
# Select a cryptographic cipher.
(snip)
cipher AES-256-CBC
auth SHA256
```

        認証に使うハッシュ関数`SHA256`を追加で指定します。(デフォルトは`SHA1`で強度が不十分なため。)

    - [L275-281](https://github.com/tagattie/FreeBSD-OpenVPN/blob/master/server/openvpn.conf#L275): ユーザ・グループの設定

        ``` conf
# It's a good idea to reduce the OpenVPN
(snip)
user nobody
group nobody
```

        `nobody`ユーザおよびグループを使います。

    - [L323](https://github.com/tagattie/FreeBSD-OpenVPN/blob/master/server/openvpn.conf#L323): パケットサイズの設定

        ``` conf
fragment 1354
```

        パケットのフラグメンテーションを防ぐため小さめの値を設定してあります。お使いの環境に合わせて調整していただいてかまいません。

以上、`openvpn.conf`の設定について確認してきました。これまでの作業の結果、設定ディレクトリの内容は以下のようになっていると思います。

``` shell-session
$ ls -R /usr/local/etc/openvpn
ccd/          dh2048.pem    openvpn.conf  ta.key

/usr/local/etc/openvpn/ccd:
vpnclient.example.org
```

### 自動起動の設定
最後に、FreeBSDの起動時にOpenVPNも自動的に起動されるよう設定しておきましょう。

``` shell
sysrc openvpn_enable=YES
```

設定ができたらFreeBSDマシンを再起動、あるいは以下のコマンドを実行してOpenVPNを起動します。

``` shell
service openvpn start
```

### 参考文献
1. OpenVPN, https://openvpn.net/
