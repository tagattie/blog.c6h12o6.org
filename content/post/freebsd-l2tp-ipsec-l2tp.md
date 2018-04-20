+++
title = "FreeBSDでL2TP/IPSec VPNサーバを構築する - L2TP編"
date = "2018-04-17T19:53:52+09:00"
draft = true
categories = ["FreeBSD"]
tags = ["freebsd", "vpn", "l2tp", "ipsec", "chromiumos", "chromeos", "chromebook"]
+++

[IPSec編](/post/freebsd-l2tp-ipsec-ipsec/)では、IPSecソフトウェア[strongSwan](https://www.strongswan.org/)のインストールと設定を行ないました。本記事では、必要なもう一つのソフトウェアであるL2TPサーバのインストールと設定を行ないます。

LinuxではL2TPといえば、[xl2tpd](https://github.com/xelerance/xl2tpd)を使うのが定番のようです。FreeBSDの場合にも定番があり、[MPD (Multi-link PPP daemon for FreeBSD)](http://mpd.sourceforge.net/)と呼ばれるのがそれです。本記事でもMPDを使用することにします。

### インストール
strongSwanと同様、こちらもインストールは非常に簡単です。以下のコマンドを実行してください。

``` shell
pkg install mpd5
```

### 設定
インストールが終わったら設定を行ないます。内容に関しては、以下の二記事を参考にしました。主に二つめの記事を参考に、一部一つめの記事の設定をマージするという形をとっています。

- [L2TP over IPSec (IT notes)](https://nbari.com/post/l2tp-ipsec/)
- [FreeBSD 9.1をIPsec対応L2TP VPNサーバにする (プラスα空間)](https://oichinote.com/plus/2013/04/l2tp-vpn-server-in-freebsd-9-1.html)

プラスα空間の記事は、FreeBSD 9.1向けということで少々内容が古くなってはいますが、ゼロからL2TP/IPSecサーバを構築するためのひととおりの手順が網羅されており、非常によくまとまっています。(いまここで書いている記事が必要ないです…。)

FreeBSD 11.1ではカーネルの再構築と、これにともなうファイアウォールの設定が必要なくなったくらいで、それ以外はそのまま、この記事に沿って作業を進めていけば大丈夫だと思われます。IPSecの実装として、strongSwanでなくIPsec-Toolsを使いたい場合はおすすめの記事です。

では、以降は主に自分向けのメモとして設定を見ていきます。[プラスα空間の記事](https://oichinote.com/plus/2013/04/l2tp-vpn-server-in-freebsd-9-1.html)をベースにしながら、MPDの動作に関する`mpd.conf`と、認証に使うユーザ名とパスワードを格納する`mpd.secret`の二つについて説明します。

- `/usr/local/etc/mpd5/mpd.conf`

    設定ファイルの内容は以下のとおりです。コメントをつけましたので参考にしてください。コメント先頭に`**`をつけたものは必須です。

    ``` conf
    startup:
        log +ipcp +ipv6cp +lcp +link +auth +ecp +ccp        # 各機能に関するログ出力を有効化
    
    default:
        load l2tp_server                                    # **デフォルトで設定"l2tp_server"をロード
    
    l2tp_server:
        set ippool add pool_l2tp 172.16.1.2 172.16.1.254    # **L2TPクライアントに割り当てるアドレスプール(172.16.1.1はサーバのアドレス、172.16.1.255はブロードキャストアドレスなので除外)
    
        create bundle template B_l2tp                       # **バンドルテンプレートを作成
        set iface enable proxy-arp                          # クライアントに対する代理ARP (Address Resolution Protocol)応答機能を有効化
        set iface enable tcpmssfix.                         # メッセージサイズの自動調整機能を有効化
        set iface group ng                                  # インターフェイスグループをngにセット(ファイアウォールルールで使うため)
        set ipcp ranges 172.16.1.1/24 ippool pool_l2tp      # **サーバ側アドレスとクライアント側アドレスの組み合わせを指定
        set ipcp dns 1.1.1.1 1.0.0.1                        # **クライアントに通知するDNSサーバのアドレスを指定
        set ipcp enable vjcomp                              # Van Jacobson TCPヘッダ圧縮を有効化
        set bundle enable compression.                      # CCP (Compression Control Procotol)のネゴシエーションを有効化
        set ccp enable mppc                                 # MPPC (Microsoft Point-to-Point Compression)を有効化
        set mppc enable e40                                 # 40ビットMPPC暗号化を有効化
        set mppc enable e128                                # 128ビットMPPC暗号化を有効化
        set mppc enable stateless                           # ステートレスモードを有効化
    
        create link template L_l2tp l2tp                    # **リンクテンプレートを作成
        set link action bundle B_l2tp                       # **リンクで使用するバンドルテンプレートを指定
        set link mtu 1354                                   # MTUサイズを小さく調整(ESPパケットのフラグメント防止のため)
        set link keep-alive 10 60                           # 10秒毎にハートビートを送信、60秒の無応答で接続断
        set link no pap chap eap                            # **PAP (Password Authentication Protocol), CHAP (Challenge Handshake Authentication Protocol), EAP (Extensible Authetication Protocol)をいったん無効化
        set link enable chap-msv2                           # Microsoft CHAPv2を有効化
        set link enable chap                                # **CHAPを有効化
        set link enable multilink                           # マルチリンクPPPを有効化(MTUサイズが小さい時に有効)
        set link enable acfcomp protocomp                   # アドレス、制御、プロトコルの各フィールドの圧縮を有効化
        set auth enable system-acct                         # 接続実績のアカウンティングを有効化
    
        set l2tp self 213.0.113.1                           # **L2TPサーバのグローバルIPアドレスを指定
        set l2tp enable length                              # データパケットの長さヘッダを有効化
        set l2tp disable dataseq                            # データパケットのシーケンス番号ヘッダを無効化
    
        set link enable incoming                            # **起動したら接続待ち受け状態にする
    ```

- `/usr/local/etc/mpd5/mpd.secret`

    本ファイル内にユーザ名とパスワード(平文)を記載します。

    ``` conf
<ユーザ名>    "<パスワード>"
```

    ユーザ名、パスワードを記入したら、rootユーザ以外が読み書きできないようにパーミッションを変更しておきます。

    ``` shell
chmod 600 /usr/local/etc/mpd5/mpd.secret    # あるいは chmod 400 ...
```

### 自動起動の設定
最後に、FreeBSDの起動時にMPDも自動的に起動されるよう設定しておきましょう。

``` shell
sysrc mpd_enable=YES
```

設定ができたらFreeBSDマシンを再起動、あるいは以下のコマンドを実行してMPDを起動します。

``` shell
service mpd5 start
```

以上で、L2TPサーバのインストールと設定は完了です。次回の記事では、L2TP/IPSecサーバをファイアウォール・NATルータとして動作させるための設定について説明します。

### 参考文献
1. Official Xelerance fork of L2TPd, https://github.com/xelerance/xl2tpd
1. MPD - Multi-link PPP daemon for FreeBSD, http://mpd.sourceforge.net/
1. L2TP over IPSec, https://nbari.com/post/l2tp-ipsec/
1. FreeBSD 9.1をIPsec対応L2TP VPNサーバにする, https://oichinote.com/plus/2013/04/l2tp-vpn-server-in-freebsd-9-1.html
