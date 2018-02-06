+++
title = "EdgeRouter LiteにFreeBSDをインストール - ファイアウォール編"
date = "2018-02-07T08:24:00+09:00"
categories = ["FreeBSD"]
tags = ["freebsd", "edgerouter", "firewall", "pf"]
+++

[EdgeRouter LiteにFreeBSDをインストール - DS-Lite編](/post/freebsd-edgerouter-lite-dslite)では、FreeBSDをインストールしたEdgeRouter LiteをDS-Lite接続のためのゲートウェイとして用いるための設定について説明しました。

DS-Lite接続の導入により、混雑が最もひどくなる夜間についても快適にネットを利用できるようになりました。正確には測っていませんが、遅くても10Mbpsくらいはでているようです。参考までに、[PPPoEとIPoE+DS-Lite接続でのping応答時間の違い](/post/flets-v4-pppoe-ipoe+dslite)を計測してみましたが、その差は一目瞭然です。通信速度の測定結果ではありませんが、PPPoE接続が夜間に混雑していることがよくわかると思います。

さて、速度的には満足のいく結果となりましたが、いまのままではファイアウォールが有効になっておらず、セキュリティ的な不安があります。(FreeBSDはデフォルトではファイアウォールが有効になっていません。)

FreeBSDにはipfw (IP Firewall), ipfilterおよび[pf (Packet Filter)](https://www.openbsd.org/faq/pf/)という3種類のファイアウォールが用意されており、ユーザが好きなものを選択して使うことができます。今回は、EdgeRouter Lite上のFreeBSDにファイアウォールを設定するわけですが、いまのところEdgeRouter Liteではipfwがうまく動作しないようです(カーネルモジュールのロード時にエラーが発生してしまいます)。 ipfilterは使ったことがなく、ネット上の情報も少ないようなので、OpenBSD由来のpfを使ってみることにしました。

特にサーバを公開するわけでもない(というか、DS-Lite接続ではトンネル終端装置のグローバルアドレスをユーザが共有することになりますので、サーバ公開はできません)ので、以下のポリシーで最低限のルールを設定することにします。

- 内部ネットワークから外向きへのパケットはすべて許可
- 外部ネットワークから内向きへのパケットは上の項のパケットに対応するもののみ許可
- ループバックインターフェイスや内部ネットワーク内のパケットはすべて許可

これにそって作成したファイアウォールルールを以下に示します。

- `/etc/pf.conf`

    ```pf
    ext_if="gif0"   # 外側インターフェイス
    int_if="octe0"  # 内側インターフェイス

    set skip on lo  # ループバックインターフェイスについては処理しない

    # 外側インターフェイスの入る方向パケットについて
    # フラグメント化されたパケットは再アセンブルする
    scrub in on $ext_if all fragment reassemble

    # 外側インターフェイスの入る方向パケットをログを取った上でブロック
    # (ただし下記ステートがキープされている場合を除く)
    block in log on $ext_if all
    # 外側インターフェイスのすべての出る方向パケットを通し、ステートをキープ
    pass out on $ext_if all keep state

    # 内側インターフェイスのパケットは入る・出る方向ともすべて通す
    pass quick on $int_if
    ```

次に、ファイアウォールを有効化する方法ですが、デフォルトでインストールされているため、非常に簡単です。pf自体の有効化のため、およびログ出力のための設定を`/etc/rc.conf`に追記すればOKです。

- `/etc/rc.conf`

    ```conf
    pf_enable="YES"
    pflog_enable="YES"
    ```
    
ログはデフォルトで`/var/log/pflog`に、テキストではなくバイナリで出力されます。内容を確認するには`tcpdump`コマンドを用いますが、いちいち`tcpdump`を起動するのが面倒なので、[こちら](https://home.nuug.no/~peter/pf/newest/log2syslog.html)を参考にして、テキストファイルとして出力するようにしました。以下の各行を`/etc/rc.local`および`/etc/syslog.conf`に追加します。

- `/etc/rc.local`

    ```conf
    nohup tcpdump -lnetttt -i pflog0 | logger -t pf -p local2.info &
    ```
- `/etc/syslog.conf`

    ```conf
    local2.info                                    /var/log/pf.log
    ```

以上でファイアウォールの設定は完了です。EdgeRouterを再起動すればファイアウォールが有効になります。

### 参考文献
1. OpenBSD PF - User's Guide, https://www.openbsd.org/faq/pf/
1. Building The Network You Need With PF, The OpenBSD Packet Filter: Log To Syslog, https://home.nuug.no/~peter/pf/newest/log2syslog.html
