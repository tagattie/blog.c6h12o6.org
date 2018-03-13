+++
title = "FreeBSDでVPNサーバを構築する - ネットワーク設計編"
date = "2018-03-13T19:34:00+09:00"
categories = ["FreeBSD"]
tags = ["freebsd", "vpn", "server", "openvpn", "wifi"]
+++

[まえがき](/post/freebsd-openvpn-server-intro/)では、特に暗号化されていない公衆無線LANを安全に利用する技術として、VPN (Virtual Private Network, 仮想私設網)が有効であることを説明しました。本記事からは、FreeBSD上に[OpenVPN](https://openvpn.net/)を用いたVPNサーバを構築し、VPNクライアントとの間で安全な通信を行なえるようになるまでを説明します。

この目的を達成するためには、おおまかにいって以下の手順が必要になります。

1. VPNのネットワーク設計
1. OpenVPNサーバのインストール・設定
1. OpenVPNクライアントのインストール・設定
1. OpenVPNクライアントとサーバ間での導通確認
1. OpenVPNサーバのファイアウォール・NATルータの設定

本記事では、一つ目のパートであるネットワーク設計について説明します。「設計」といっても大げさなものではなく、VPNサーバを構築しようとしているFreeBSDマシン(以降、サーバ機器といいます)に現在割り当てられているIPアドレスなどを確認し、VPN内で使用するIPアドレスを決定する、というのが主な内容です。

注: 本記事では、サーバ機器に**固定のIPv4およびIPv6アドレスが、それぞれ1つずつ割り当て**られているという前提で説明を進めていきます。IPv4アドレスが動的に割り当てられる環境でもVPNサーバを構築することは可能ですが、ドメイン名解決のために**ダイナミックDNSの併用が必要**です。

注2: 本記事で用いるIPアドレスは**VPN内で用いるものを除き、説明用の[IPv4](https://tools.ietf.org/html/rfc5737)および[IPv6](https://tools.ietf.org/html/rfc3849)アドレス**です。このアドレスをそのまま用いても動作しませんので、必ずお使いの環境に合わせてアドレスの読み替えをお願いします。

では、まずサーバ機器に割り当てられているドメイン名およびIPアドレスを確認しましょう。

- ドメイン名

    `hostname`コマンドを用いて確認します。

    ``` shell-session
$ hostname -f
vpnserver.example.com
```

    ドメイン名は`vpnserver.example.com`です。本ドメイン名は、サーバ証明書を発行する際のコモンネーム(Common Name, CN)として使います。

- IPアドレス

    `ifconfig`コマンドを用いて確認します。指定するインターフェイス名はお使いの環境に合わせてください。

    ``` shell-session
$ ifconfig vtnet0
vtnet0: flags=8943<UP,BROADCAST,RUNNING,PROMISC,SIMPLEX,MULTICAST> metric 0 mtu 1500
        options=6c06bb<RXCSUM,TXCSUM,VLAN_MTU,VLAN_HWTAGGING,JUMBO_MTU,VLAN_HWCSUM,TSO6,LRO,VLAN_HWTSO,LINKSTATE,RXCSUM_IPV6,TXCSUM_IPV6>
        ether xx:xx:xx:xx:xx:xx
        hwaddr xx:xx:xx:xx:xx:xx
        inet 203.0.113.1 netmask 0xffffff00 broadcast 203.0.113.255        # IPv4アドレス
        inet6 fe80::xxxx:xxxx:xxxx:xxxx%vtnet0 prefixlen 64 scopeid 0x1 
        inet6 2001:db8::203:0:113:1 prefixlen 64                           # IPv6アドレス
        nd6 options=21<PERFORMNUD,AUTO_LINKLOCAL>
        media: Ethernet 10Gbase-T <full-duplex>
        status: active
```

    IPアドレスとネットマスク/プレフィクス長は以下のとおりです:
    - IPv4アドレス/ネットマスク: `203.0.113.1/24`
    - IPv6アドレス/プレフィクス長: `2001:db8::203:0:113:1/64`

次に、VPN内で使用するIPアドレスを決定します。

先に述べたとおり、サーバ機器に割り当てられているアドレスはIPv4, IPv6とも1つだけなので、VPN内でグローバルアドレスを用いることはできません。そこで、IPv4については[プライベートアドレス](https://tools.ietf.org/html/rfc1918)、IPv6については[ユニークローカルアドレス](https://tools.ietf.org/html/rfc4193)(IPv4のプライベートアドレスに相当するもの)を使うことにします。この制約のなかであれば、アドレスは自由に決定してかまいません。本記事では、以下のように決めることにします。

- VPN内で用いるアドレス空間
    - IPv4アドレス/ネットマスク: `172.16.0.0/24`
    - IPv6アドレス/プレフィクス長: `fd20:10d:b800::/64`

VPNサーバおよびVPNクライアントには以下のようにアドレスを割り当てることにしましょう。また、VPNクライアントには、VPN内で用いるドメイン名を割り当てます。本ドメイン名は、クライアント証明書を発行する際のコモンネーム(Common Name, CN)として使います。

- VPNサーバ
   - IPv4: `172.16.0.1`
   - IPv6: `fd20:10d:b800::172:16:0:1`

- VPNクライアント
    - IPv4: `172.16.0.11`
    - IPv6: `fd20:10d:b800::172:16:0:11`
    - ドメイン名: `vpnclient.example.org`

ネットワーク設計の結果をまとめると、以下のブロック図のようになります。

![VPNサーバのネットワーク構成図](/img/openvpn-network-config.png)

次回はネットワーク設計の結果を用いて、OpenVPNサーバのインストールおよび設定を行ないます。

### 参考文献
1. OpenVPN, https://openvpn.net/
1. RFC 5737, IPv4 Address Blocks Reserved for Documentation, https://tools.ietf.org/html/rfc5737
1. RFC 3849, IPv6 Address Prefix Reserved for Documentation, https://tools.ietf.org/html/rfc3849
1. RFC 1918, Address Allocation for Private Internets, https://tools.ietf.org/html/rfc1918
1. RFC 4193, Unique Local IPv6 Unicast Addresses, https://tools.ietf.org/html/rfc4193
