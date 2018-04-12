+++
title = "FreeBSDの通信性能が悪いと感じたらLRO, TSOオプションをオフにしてみよう"
date = "2018-04-12T19:51:00+09:00"
categories = ["FreeBSD"]
tags = ["freebsd", "network", "poor", "performance", "speed", "ifconfig", "option", "lro", "tso"]
+++

先日、FreeBSD上にOpenVPNを用いたVPNサーバを構築する手順([ネットワーク設計編](/post/freebsd-openvpn-server-network/)、[OpenVPNサーバ編](/post/freebsd-openvpn-server-server/)、および[NAT・ファイアウォール編](/post/freebsd-openvpn-server-router/))を紹介しました。その後、無事クライアントからの接続もでき、公衆WiFiでも安全な通信が可能になって満足しています。

なにも問題はなく順調といいたいところですが、OpenVPN以外で一つ問題が起きました。

くだんのVPNサーバは、VPS (Virtual Private Server)を借りてその上に構築しています。ときどきVPSにSSH (Secure Shell)で接続して、メンテナンスなどの作業を行ないますが、使っているうちに奇妙なことに気がつきました。

コマンドの実行には問題がありません。ファイル編集も問題なくできます。しかし、通信がそこそこ連続的に起こるような状況(たとえば、ファイル数の多いディレクトリで`ls -l`を実行してファイル一覧を表示する、など)で、通信速度が極端に遅くなったり、SSH接続が途切れてしまう、という現象が起こります。

原因を求めてネットを検索すると、FreeBSDで通信速度が遅くなる事例として、以下の記事を見つけました。

- [のろいの em(4)/fxp(4)+natd(8) 解決編!! の巻 (Thank ~~GOD~~ Daemon, it's BSD)](http://www.bsddiary.net/d/20100324.html)
- [FreeBSD 9.0 で ipfw + natd + em で通信速度が遅くなった (Matsup's blog)](http://matsup.blogspot.jp/2012/12/freebsd-90-ipfw-natd-em.html)

いずれも、Intel社のNIC (Network Interface Card)を搭載したマシン上で`ipfw`と`natd`を併用し、NAT・ファイアウォール**ルータ**としてFreeBSDを使っているケースです。くだんのVPNサーバは、VPS上に構築しているのでIntel NICは直接使っていませんが、`ipfw`と`natd`を併用するところは共通しており、関係がありそうです。

上二つの記事では、ネットワークインターフェイスの[TSO (TCP Segmentation Offload)](https://en.wikipedia.org/wiki/Large_send_offload)オプションをオフにすることで問題を解決しています。(TSOは、データ送信時の処理を一部NICに行なわせることで、CPUの負荷を低減する技術です。)

ちなみに、一つめの記事によると、FreeBSD 7.0ですでにこの問題の発生が報告されているようです。FreeBSD 7.0は2008年2月にリリースされていますので、10年来の由緒正しき(?)問題のようですね…。

少し話がそれましたが、もう少し調べると、pfSenseのドキュメントに決定的といえる記述がありました。(pfSenseはFreeBSDベースのソフトウェアルータです。)

- [Networking, Advanced Setup (pfSense)](https://doc.pfsense.org/index.php/Advanced_Setup#Networking)

[LRO (Large Receive Offload)](https://en.wikipedia.org/wiki/Large_receive_offload)についての記述です(LROは、データ受信時にCPUの負荷を軽減する技術)。

> Hardware Large Receive Offloading (LRO)
> 
> LRO works by aggregating multiple incoming packets from a single stream into a larger buffer before they are passed higher up the networking stack, thus reducing the number of packets to be processed. LRO should not be used on machines acting as routers as it breaks the end-to-end principle and can significantly impact performance. pfSense is most frequently used as a router or an equivalent. 

ざっくりいうと、(FreeBSDを)ルータとして動作させる場合はLROを有効にすべきでない。なぜなら、LROはエンドツーエンド原則に反するし、**性能に大きな(悪)影響を及ぼす**から、ということです。TSOについても同様だと述べられています。

では、くだんのVPNサーバのネットワークインターフェイスオプションを確認してみましょう。

``` shell-session
$ ifconfig vtnet0 | grep options
	options=6c07bb<RXCSUM,TXCSUM,VLAN_MTU,VLAN_HWTAGGING,JUMBO_MTU,VLAN_HWCSUM,TSO4,TSO6,LRO,VLAN_HWTSO,LINKSTATE,RXCSUM_IPV6,TXCSUM_IPV6>
	nd6 options=21<PERFORMNUD,AUTO_LINKLOCAL>
```

オプションにTSO4, TSO6, LROが含まれており、LROおよびTSOがいずれも有効になっていることがわかります。これを無効化してしまいましょう。`/etc/rc.conf`のネットワークインターフェイスを設定する行にLRO, TSOを無効化する指示(`-lro -tso`)を書き加えます。

- `/etc/rc.conf`

    ``` shell
ifconfig_vtnet0="inet <IPアドレス> netmask <ネットマスク> -lro -tso"
```

設定を変更したらFreeBSDを再起動します。もう一度、オプションを確認しましょう。

``` shell-session
$ ifconfig vtnet0|grep options
	options=6c00bb<RXCSUM,TXCSUM,VLAN_MTU,VLAN_HWTAGGING,JUMBO_MTU,VLAN_HWCSUM,VLAN_HWTSO,LINKSTATE,RXCSUM_IPV6,TXCSUM_IPV6>
	nd6 options=21<PERFORMNUD,AUTO_LINKLOCAL>
```

TSO4, TSO6, LROが消えましたね。この状態で、冒頭に挙げた操作(ファイル数が多いディレクトリで`ls -l`を実行)しても問題が起きないことを確認できました。

FreeBSDルータの通信速度が遅いと感じたら、**LRO, TSOオプションをオフ**にしてみましょう。試して損はないと思います。

### 参考文献
1. のろいの em(4)/fxp(4)+natd(8) 解決編!! の巻, http://www.bsddiary.net/d/20100324.html
1. FreeBSD 9.0 で ipfw + natd + em で通信速度が遅くなった, http://matsup.blogspot.jp/2012/12/freebsd-90-ipfw-natd-em.html
1. Large send offload, https://en.wikipedia.org/wiki/Large_send_offload
1. Large receive offload, https://en.wikipedia.org/wiki/Large_receive_offload
1. Networking, Advanced Setup, https://doc.pfsense.org/index.php/Advanced_Setup#Networking
