+++
title = "FreeBSDでL2TP/IPSec VPNサーバを構築する - IPSec編"
date = "2018-04-19T20:37:00+09:00"
categories = ["FreeBSD"]
tags = ["freebsd", "vpn", "l2tp", "ipsec", "chromiumos", "chromeos", "chromebook"]
+++

[まえがき](/post/freebsd-l2tp-ipsec-intro/)では、Chromium OS向けにL2TP/IPSec VPN (Virtual Private Network)サーバを構築する必要が生じた事情、およびL2TP/IPSecの概要について説明しました。本記事からは、FreeBSD上にL2TP/IPSecサーバを構築し、VPNクライアントとの間で安全な通信を行なえるようになるまでを説明します。

### ネットワーク設計
まず、ネットワーク設計を行ないましょう。基本的な前提はOpenVPNサーバを構築したときと同様、以下のとおりです。

- VPNサーバを構築するマシンには、グローバルIPv4アドレスが**固定で1つ**割り当てられている  
(IPアドレスが動的に割り当てられる場合は、DDNS (Dynamic DNS)の併用が必要になりますが、本記事では扱いません)
- VPN内で用いるIPアドレスは、**[プライベートアドレス](https://tools.ietf.org/html/rfc1918)**とする

L2TP/IPSecでは、IPv4だけでなくIPv6を用いることも可能です。しかしながら、Chromium OSのL2TP/IPSecクライアントは、いまのところIPv6に対応していないように見えますので、今回はIPv4のみを使用する構成とします。

では、VPNサーバおよびクライアントについて、ネットワーク関連情報の確認とアドレスの決定を行ないます。

1. サーバマシンのFQDN (Fully Qualified Domain Name)とIPアドレスを確認します
1. VPNサーバおよびクライアントが、それぞれVPN内で用いるIPアドレスを決めます
1. VPNクライアントへ通知するDNSサーバのアドレスを決めます

サーバのFQDNおよびIPアドレスの確認方法については、OpenVPNサーバの[ネットワーク設計編](/post/freebsd-openvpn-server-network/)を参照してください。

本記事では、以下のように確認、決定したものとします。

- VPNサーバ - FQDN: `vpnserver.example.com`
- VPNサーバ - グローバルIPv4アドレス/ネットマスク: `203.0.113.1/24`
- VPNサーバ - VPNでのIPv4アドレス/ネットマスク: `172.16.1.1/24`
- VPNクライアント - VPNでのIPv4アドレス/ネットマスク: `172.16.1.2/24`
- VPNクライアント - 通知するDNSサーバアドレス: `1.1.1.1`, `1.0.0.1`

注: サーバのFQDNは説明用の仮の名前です。実際の環境に合わせて読み替えをお願いします。

注2: サーバのグローバルIPv4アドレスは[説明用のアドレス](https://tools.ietf.org/html/rfc5737)です。実際の環境に合わせて読み替えをお願いします。

注3: VPN内で用いるアドレスは、プライベートアドレスであれば本記事と異なってもかまいません。

注4: クライアントに通知するDNSサーバのIPアドレスとして、[APNIC (Asia Pacific Network Information Centre)](https://www.apnic.net/)と[Cloudflare社](https://www.cloudflare.com/)が共同で提供する[パブリックDNSサービス](https://1.1.1.1/)を指定しています。こちらもお好みにあわせて変更してかまいません。

ネットワーク設計の結果をまとめると、下図のようになります。

![L2TP/IPSecサーバ - ネットワーク構成](/img/diagram/l2tp-ipsec-network-config.png)

### IPSec
#### インストール
ネットワーク設計が終わりましたので、次は必要なソフトウェアのインストールを行ないましょう。

IPSecとL2TPはそれぞれ別のプログラムとして実装されていますので、二つのソフトウェアをインストールします。まず、本記事ではIPSecソフトウェアのインストールと設定までを行ないます。

IPSecのオープンソース実装としては、[IPsec-Tools](http://ipsec-tools.sourceforge.net/) (racoonといったほうが通りがよいかもしれません)と[strongSwan](https://www.strongswan.org/)が有名です。IPsec-Toolsは[WIDEプロジェクト](http://www.wide.ad.jp/)内の[KAMEプロジェクト](http://www.kame.net/)の活動成果をもとにしたものです。いっぽう、strongSwanはもともとLinux向けのIPSec実装である[FreeS/WAN](http://www.freeswan.org/)が前身です。現在はいずれもFreeBSDで使うことができます。

[Wikipediaの記事](https://ja.wikipedia.org/wiki/StrongSwan)で「設定方法が直接的でわかりやすい」と紹介されていますので、本記事ではstrongSwanを使うことにしました。インストールは非常に簡単で、以下のコマンドを実行するだけです。

``` shell
pkg install strongswan
```

#### 設定
インストールが終わったら設定を行ないます。内容については以下の三記事を参考にしました。一つめの記事はFreeBSDにおけるstrongSwanの設定の参考として、残りの二つは主に暗号スイートの選択に関して参考にしました。

- [L2TP over IPSec (IT notes)](https://nbari.com/post/l2tp-ipsec/)
- [strongSwan でVPN(IKEv2) を 構築する Part.1 (Tech Beans)](http://soymsk.hatenablog.com/entry/2016/10/09/154355)
- [Security Recommendations (strongSwan)](https://wiki.strongswan.org/projects/strongswan/wiki/SecurityRecommendations)

暗号スイートに関しては、可能な組み合わせが非常に多いため、どれを選択、組み合わせるのがよいのか分かりかねるところですが、以下の二点に留意したつもりです。

- Security Recommendationsに挙がっている、すでに破られているアルゴリズムは使用しないこと
- 少なくとも、Chromium OSとAndroidから接続できること

では、strongSwanの動作に関する`ipsec.conf`と、事前共有鍵を格納する`ipsec.secrets`の二つを作成、編集します。

- `/usr/local/etc/ipsec.conf`

    ファイルの内容は以下は以下のとおりです。コメントをつけましたので参考にしていただければと思います。

    ``` conf
    config setup
        ## 各機能に関するデバッグレベルを2に設定(デフォルトは1)
        charondebug = "dmn 2, mgr 2, ike 2, cfg 2, knl 2, net 2, esp 2"

    conn %default
        ## IKE (鍵交換)に用いる暗号スイートの候補リスト
        ike = aes128-sha256-ecp256,aes128-sha256-modp3072,aes128-sha256-modp2048,aes256-sha384-ecp384,aes256-sha384-modp3072,aes256-sha384-modp2048,aes256-sha512-modp1024,aes256-sha384-modp1024!
        ## ESP (パケット暗号化)に用いる暗号スイートの候補リスト
        esp = aes128gcm16-ecp256,aes128-sha256-ecp256,aes128-sha256-modp2048,aes128gcm16,aes128-sha256,aes256gcm16-ecp384,aes256-sha384-ecp384,aes256-sha384-modp4096,aes256gcm16,aes256-sha256!
        closeaction = clear                 # クライアント側からのトンネル断検出で接続を閉じる
        dpdaction = clear                   # 通信断を検出したら接続を閉じる

    conn L2TP-IPSec-PSK
        type = transport                    # IPSecトランスポートモードを使用
        authby = psk                        # 認証には事前共有鍵を使用
        keyexchange = ikev1                 # L2TPと組み合わせるのでIKEはVersion 1を使用
        ## leftがサーバ側(接続を受け付ける側)、rightがクライアント側
        leftid = @vpnserver.example.com     # サーバのID (認証時に使用)
        left = 213.0.113.1                  # サーバのIPアドレス
        leftsubnet = 0.0.0.0/0[udp/l2tp]    # サーバ経由でアクセスするネットワークアドレスとネットマスク(左記の場合は全インターネットを意味)
        leftauth = psk                      # サーバ側の認証には事前共有鍵を使用
        right = %any                        # クライアント側のIPアドレスは任意(任意のアドレスから接続可能)
        rightauth = psk                     # クライアント側の認証には事前共有鍵を使用
        auto = add                          # 起動時に接続待ち受け状態にする
    ```

- `/usr/local/etc/ipsec.secrets`

    本ファイル内に事前共有鍵を記載します。フォーマットは以下のとおり、`<leftid> : PSK "<事前共有鍵>"`となります。

    ``` conf
@vpnserver.example.com : PSK "<事前共有鍵>"
```

    事前共有鍵はセキュリティの要になりますので、十分にランダムで長い文字列を使うようおすすめします。strongSwanによるおすすめの鍵生成方法は[ここ](https://wiki.strongswan.org/projects/strongswan/wiki/SecurityRecommendations#Preshared-Keys-PSKs)にあります。

    注: 上記の生成方法を使う場合、その中で使用されている`base64`コマンドがFreeBSDに標準ではインストールされていません。`base64`パッケージをインストールするか、`openssl`コマンドで代用してください。

    事前共有鍵を記入したら、rootユーザ以外が読み書きできないようにパーミッションを変更しておきます。

    ``` shell
chmod 600 /usr/local/etc/ipsec.secrets    # あるいは chmod 400 ...
```

#### 自動起動の設定
最後に、FreeBSDの起動時にstrongSwanも自動的に起動されるよう設定しておきましょう。

``` shell
sysrc strongswan_enable=YES
```

設定ができたらFreeBSDマシンを再起動、あるいは以下のコマンドを実行してstrongSwanを起動します。

``` shell
service strongswan start
```

以上で、IPSecソフトウェアのインストールと設定は完了です。次回の記事ではL2TPソフトウェアのインストールと設定を行ないます。

### 参考文献
1. RFC 1918, Address Allocation for Private Internets, https://tools.ietf.org/html/rfc1918
1. RFC 5737, Pv4 Address Blocks Reserved for Documentation, https://tools.ietf.org/html/rfc5737
1. IPsec-Tools, http://ipsec-tools.sourceforge.net/
1. strongSwan, https://www.strongswan.org/
1. L2TP over IPSec, https://nbari.com/post/l2tp-ipsec/
1. strongSwan でVPN(IKEv2) を 構築する Part.1, http://soymsk.hatenablog.com/entry/2016/10/09/154355
1. Security Recommendations, https://wiki.strongswan.org/projects/strongswan/wiki/SecurityRecommendations
