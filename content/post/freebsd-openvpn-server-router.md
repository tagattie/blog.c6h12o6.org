+++
title = "FreeBSDでVPNサーバを構築する - NAT・ファイアウォール編"
date = "2018-03-16T21:22:00+09:00"
categories = ["FreeBSD"]
tags = ["freebsd", "vpn", "server", "openvpn", "wifi", "firewall", "nat", "router"]
+++

[OpenVPNクライアント編](/post/freebsd-openvpn-server-client/)では、WindowsマシンにOpenVPNをインストールし、VPNクライアントの設定を行ないました。前回まででクライアント−サーバ間の安全な通信路(トンネル)はすでに確立されていますが、トンネルの先に広がるインターネットと通信できないという課題が残っています。

本記事では、最後のパートとして、VPNサーバが動作するFreeBSDマシンをNAT (Network Address Translation)ルータとして動作するように設定していきます。本設定を行なうことで、VPNクライアントとインターネットとの間で通信が可能になります。

また、外部からVPNサーバマシンに対してのアクセスは、必要最小限のポートだけに絞るようファイアウォールの設定もあわせて行ないます。ファイアウォールは絶対に必要というわけではありませんが、安全のために導入しておくことをおすすめします。

では、まずNATルータとしての設定を行ないます。

### NATルータ
#### パケットフォワーディングの設定
[ネットワーク設計編](/post/freebsd-openvpn-server-network/)のブロック図に示した通り、VPNサーバはインターネットとつながる外側インターフェイス(`vtnet0`)と、VPNにつながるトンネルインターフェイス(`tun0`)を持っています。VPNクライアントはトンネルインターフェイスに接続するわけですが、クライアントがインターネットと通信するためには、VPNサーバが2つのインターフェイス間でパケットを転送しなければなりません。

デフォルトでパケット転送機能はオフになっていますので、以下のコマンドを実行してIPv4およびIPv6パケットのフォワーディング(転送)機能を有効にします。

``` shell
sysrc gateway_enable=YES         # IPv4パケット転送の有効化
sysrc ipv6_gateway_enable=YES    # IPv6パケット転送の有効化
```

#### NATの設定(IPv4)
パケット転送を有効にすることで、VPNクライアントからのパケットをインターネットに送信できるようになります。しかし、まだクライアントとインターネットとの間の通信は成功しません。

[ネットワーク設計編](/post/freebsd-openvpn-server-network/)で述べたとおり、VPNのネットワークアドレスには、プライベート(IPv4)およびユニークローカル(IPv6)アドレスを割り当てました。これらのアドレスに対しては、インターネットからパケットを送ることができません。つまり、今のままでは、VPNクライアントからインターネットにパケットを送り出すことはできても、それに対する返事が戻ってこないのです。

そこで、プライベートアドレスやユニークローカルアドレスを、インターネットからのパケットの送り先になれるアドレス、つまりVPNサーバの外側インターフェイス(`vtnet0`)のアドレスに書き換えてからパケットを送信する必要があります。このアドレス書き換えをNAT (Network Address Translation)といいます。

では、NAT設定の具体的な内容を見ていきます。まず、IPv4です。IPv4のNATには`natd(8)`を使います。以下に`natd`の設定ファイル`/etc/natd.conf`の内容を示します。

- `/etc/natd.conf`

    ``` conf
log                  # ログの記録
log_denied           # 受信を拒否したパケットのログを記録
use_sockets          # ソケットを使用
same_ports           # 入りと出で同じポート番号をキープ
#verbose             # natdプロセスをバックグラウンド化しない(デバッグ用)
unregistered_only    # プライベートアドレスのパケットのみNATする
log_ipfw_denied      # NATした後ファイアウォールで破棄されたパケットを記録
```

`natd`はファイアウォール`ipfw`経由でパケットを受け取りますので、`natd`と同時にファイアウォール(`ipfw`)も有効化します。

``` shell
sysrc natd_enable=YES                   # natdの有効化
sysrc natd_interface=vtnet0             # NATを適用するインターフェイスの指定
sysrc natd_flags="-f /etc/natd.conf"    # natdの設定ファイルの指定
sysrc firewall_enable=YES               # ipfwの有効化
sysrc firewall_logging=YES              # ipfwのログ記録の有効化
```

#### NAT設定(IPv6)
次は、IPv6です。IPv6のNATには`pf(4)`を使います。以下に`pf`の設定ファイル`/etc/pf.conf`の内容を示します。

- `pf.conf`

    ``` conf
    ext_if="vtnet0"                         # ネットワーク設計編で確認した外側インターフェイス
    ext_ipv6addr="2001:db8::203:0:113:1"    # 外側インターフェイスのIPv6アドレス
    int_if="tun0"                           # トンネルインターフェイス
    int_ipv6net="fd20:10d:b800::/64"        # VPNのIPv6サブネットアドレス
    
    no nat on $ext_if inet6 from $ext_ipv6addr to any
# 外側インターフェイスへのパケット送出前にVPNのIPv6アドレスを外側IPv6アドレスに書き換え
nat on $ext_if inet6 from $int_ipv6net to any -> $ext_ipv6addr
```

設定ファイルを作成したら`pf`を有効化します。

``` shell
sysrc pf_enable=YES
```

### ファイアウォール
最後に、(必須ではありませんが)ファイアウォールの設定をしておきましょう。基本的な方針は以下のとおりです。

- VPNクライアントからインターネットへの通信はすべて許可
- インターネットからVPNクライアントへの通信は上記の通信に関わるものは許可、それ以外はすべて拒否
- VPNサーバへの接続はssh (ポート22)とopenvpn (ポート1194)のみ許可

FreeBSDのファイアウォール設定ファイル`/etc/rc.firewall`には、いくつかのタイプがプリセットされています。このうち、`simple`というファイアウォールタイプが今回必要な形に最も近いので、これを少しだけカスタマイズして使うことにします。オリジナルの`simple`ルールとの違いは以下のとおりです。

- ポート25 (smtp)へのTCP接続(許可→拒否)
- ポート53 (domain)へのTCP接続(許可→拒否)
- ポート80 (http)へのTCP接続(許可→拒否)
- ポート22 (ssh)へのTCP接続を許可(新規)
- ポート1194 (openvpn)へのUDP接続を許可(新規)

以上の変更を行なって、オリジナルファイルとの`diff`をとった結果を以下に示します。

``` diff
--- /etc/rc.firewall    2017-07-21 11:11:06.000000000 +0900
+++ rc.firewall 2018-03-16 08:52:03.152890000 +0900
@@ -381,15 +381,22 @@
        ${fwcmd} add pass all from any to any frag
 
        # Allow setup of incoming email
-       ${fwcmd} add pass tcp from any to me 25 setup
+       #${fwcmd} add pass tcp from any to me 25 setup
 
        # Allow access to our DNS
-       ${fwcmd} add pass tcp from any to me 53 setup
+       #${fwcmd} add pass tcp from any to me 53 setup
        ${fwcmd} add pass udp from any to me 53
        ${fwcmd} add pass udp from me 53 to any
 
        # Allow access to our WWW
-       ${fwcmd} add pass tcp from any to me 80 setup
+       #${fwcmd} add pass tcp from any to me 80 setup
+
+       # Allow access to our SSH
+       ${fwcmd} add pass tcp from any to me 22 setup
+
+       # Allow access to OpenVPN
+       ${fwcmd} add pass udp from any to me 1194
+       ${fwcmd} add pass udp from me 1194 to any
 
        # Reject&Log all setup of incoming connections from the outside
        ${fwcmd} add deny log ip4 from any to any in via ${oif} setup proto tcp
```

設定ファイルの変更が終わったら、以下のコマンドを実行してファイアウォールのルール設定を有効化します。

``` shell
sysrc firewall_type=simple
sysrc firewall_simple_iif=tun0                        # ネットワーク設計編で決定したトンネルインターフェイス名
sysrc firewall_simple_inet=172.16.0.0/24              # トンネルインターフェイスのIPv4サブネットアドレス
sysrc firewall_simple_oif=vtnet0                      # 外側インターフェイス名
sysrc firewall_simple_onet=203.0.113.0/24             # 外側インターフェイスのIPv4サブネットアドレス
sysrc firewall_simple_iif_ipv6=tun0                   # トンネルインターフェイス名
sysrc firewall_simple_inet_ipv6=fd20:10d:b800::/64    # トンネルインターフェイスのIPv6サブネットアドレス
sysrc firewall_simple_oif_ipv6=vtnet0                 # 外側インターフェイス名
sysrc firewall_simple_onet_ipv6=2001:db8::/64         # 外側インターフェイスのIPv6サブネットアドレス
```

最後にFreeBSDマシンを再起動してすべて完了です。

なお、本記事で説明した設定ファイルの完全なものは、以下のGitHubリポジトリの`router`ディレクトリに置いてあります。必要に応じてご参照ください。

- [FreeBSD-OpenVPN](https://github.com/tagattie/FreeBSD-OpenVPN) - Sample configuration files for setting up an OpenVPN server/client on FreeBSD
