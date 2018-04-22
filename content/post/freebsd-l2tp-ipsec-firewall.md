+++
title = "FreeBSDでL2TP/IPSec VPNサーバを構築する - NAT・ファイアウォール編"
date = "2018-04-21T21:26:00+09:00"
lastmod = "2018-04-22T17:14:00+09:00"
categories = ["FreeBSD"]
tags = ["freebsd", "vpn", "l2tp", "ipsec", "chromiumos", "chromeos", "chromebook"]
+++

**追記: 2018/4/22**  
uRPFが失敗したパケットを拒否すると、VPNクライアントにもともとプライベートアドレスが割り当てられている場合に通信ができなくなります。これは困りますので、以下の行を設定から削除しました。

``` conf
block in log quick from urpf-failed    # uRPFが失敗したパケットは拒否
```

___

[L2TP編](/post/freebsd-l2tp-ipsec-l2tp/)では、L2TPサーバMPDのインストールと設定を行ないました。strongSwanとMPDが起動している状態で、VPNクライアントからの接続が行なわれると、クライアント―サーバ間の安全な通信路であるL2TP/IPSecトンネルが確立します。

しかし、いまのままでは、クライアントはサーバとだけしか通信ができません。クライアントがサーバ越しにインターネットと通信を行なうためには、L2TP/IPSecサーバマシンがNAT (Network Address Translation)ルータとして振る舞う必要があります。

本記事では、サーバマシンをルータ兼ファイアウォールとして動作させるための設定を説明していきます。

### ルータ
では、まずルータとしての設定を行ないます。

[IPSec編](/post/freebsd-l2tp-ipsec-ipsec/)のブロック図に示したとおり、VPNサーバはインターネットとつながる外側インターフェイス(`vtnet0`)を持っています。また、VPNクライアントからの接続があると、トンネルインターフェイス(`ngX`)がクライアントごとに作られます。クライアントがインターネットと通信するためには、VPNサーバがこれらのインターフェイス間でパケットの転送を行なう必要があります。

デフォルトでは、インターフェイス間のパケット転送機能はオフになっていますので、以下のコマンドを実行してIPv4パケットのフォワーディング(転送)機能を有効に設定します。今回はIPv6を使いませんので、IPv4パケットの転送機能のみ有効化します。

``` shell
sysrc gateway_enable=YES    # IPv4パケット転送の有効化
```

ルータとしての設定は以上で終了です。

### NAT・ファイアウォール
次に、NATとファイアウォールの設定をあわせて行ないます。

[IPSec編](/post/freebsd-l2tp-ipsec-ipsec/)のブロック図に示したとおり、VPNネットワークにはプライベートアドレスを割り当てました。プライベートアドレスに対しては、インターネットからパケットを送ることができません。つまり、いまのままでは、VPNクライアントからインターネットにパケットを送り出すことができても、それに対する返事が戻ってこないということです。

そこで、プライベートアドレスを、インターネットからのパケットの送り先になれるアドレス、つまりVPNサーバの外側インターフェイス(`vtnet0`)のアドレスに書き換えてからパケットを送信する必要があります。このアドレス書き換えをNATといいます。

[OpenVPNサーバを構築](/post/freebsd-openvpn-server-router/)した際は、NATとファイアウォールを別々のソフトウェアで実現しました。そのときにも一部で用いましたが、OpenBSD由来のファイアウォールである[PF (Packet Filter)](https://www.openbsd.org/faq/pf/)にはNAT機能も含まれているので、今回はPFを用いて、NATおよびファイアウォールをまとめて実現したいと思います。

ファイアウォールのルール作成に際しては以下の三記事を参考にしました。

- [OpenBSD PF - Building a Router (OpenBSD)](https://www.openbsd.org/faq/pf/example1.html)
- [Securing the PF Firewall, FreeBSD Server Guide (Cullum Smith's Blog)](https://www.c0ffee.net/blog/freebsd-server-guide#pf)
- [FreeBSD 9.1をIPsec対応L2TP VPNサーバにする (プラスα空間)](https://oichinote.com/plus/2013/04/l2tp-vpn-server-in-freebsd-9-1.html)

ルール作成の際のポリシーは以下のとおりとします。

- 基本はパケットをすべて拒否、ただし以下の例外を認める
- 信頼するインターフェイス(ローカルインターフェイス、およびトンネルインターフェイス)の出入パケットをすべて許可
- 外部インターフェイスでのICMP (Internet Control Message Protocol)の出入パケットを許可
- 外部インターフェイスからのその他の入パケットは明示的に許可した以下のプロトコル、ポートのみ許可
    - TCP: 22 (SSH)
    - UDP: 500 (IPSecの鍵交換), 1701 (L2TP), 4500 (IPSecのNATトラバーサル)
    - ESP (Encapsulating Security Payload)
- 外部インターフェイスからの出パケットをすべて許可、前記のパケットに関連する外部インターフェイスからの入パケットを許可

上記のポリシーにしたがって作成した、PFの設定ファイルの内容を以下に示します。コメントをつけましたので参考にしていただければと思います。

- `/etc/pf.conf`

    ``` conf
    ext_if="vtnet0"                                             # 外部インターフェイス名
    trusted_ifs = "{ lo ng }"                                   # 信頼インターフェイスを指定
    
    table <martians4> const { \                                 # 到達不能なアドレスブロックのテーブル
        0.0.0.0/8, 10.0.0.0/8, 100.64.0.0/10, 127.0.0.0/8, \
        169.254.0.0/16, 172.16.0.0/12, 192.0.2.0/24, 198.18.0.0/15, \
        192.168.0.0/16, 198.51.100.0/24, 203.0.113.0/24, 224.0.0.0/4, \
        240.0.0.0/4 \
    }
    
    ssh_port = "22"                                             # sshのポート番号
    tcp_services = "{" $ssh_port "}"                            # 許可するTCPポート番号
    udp_services = "{ isakmp, l2tp, sae-urn }                   # 許可するUDPポート番号
    
    set block-policy drop                                       # 拒否したパケットは廃棄
    set loginterface $ext_if                                    # ログ取得の対象を外部インターフェイスとする
    set skip on $trusted_ifs                                    # 信頼インターフェイスについてはチェックをスキップ
    
    scrub in on $ext_if all fragment reassemble                 # 外部インターフェイスからの入パケットがフラグメント化されていれば再アセンブル
    
    nat on $ext_if inet from !$ext_if:0 to any -> ($ext_if)     # 外部インタフェースからの出パケットを必要に応じてNAT
    
    block log all                                               # 基本はパケットをすべて拒否
    antispoof log for $ext_if                                   # 外部インターフェイスでのアンチスプーフ処理を有効化
    block in quick on $ext_if from any to { <martians4> }       # 外部インターフェイスへの到達不能アドレス宛て入パケットは拒否
    pass quick on $trusted_ifs                                  # 信頼インターフェイスについてはすべて許可
    pass quick on $ext_if proto icmp                            # 外部インターフェイスのICMPパケットは許可
    pass in quick on $ext_if proto tcp to port $tcp_services    # 外部インターフェイスへのTCPパケットは指定したもののみ許可
    pass in quick on $ext_if proto udp to port $udp_services    # 外部インターフェイスへのUDPパケットは指定したもののみ許可
    pass in quick on $ext_if proto esp                          # 外部インターフェイスへのESPパケットを許可
    pass out quick on $ext_if all keep state                    # 外部インターフェイスからの出パケットは許可
    ```

設定ファイルを作成したら、以下のコマンドを実行して機能の有効化を行ないます。

``` shell
sysrc pf_enable=YES
```

最後にFreeBSDマシンを再起動してすべて完了です。

### 参考文献
1. OpenBSD PF - Building a Router, https://www.openbsd.org/faq/pf/example1.html
1. Securing the PF Firewall, FreeBSD Server Guide, https://www.c0ffee.net/blog/freebsd-server-guide#pf
1. FreeBSD 9.1をIPsec対応L2TP VPNサーバにする, https://oichinote.com/plus/2013/04/l2tp-vpn-server-in-freebsd-9-1.html
