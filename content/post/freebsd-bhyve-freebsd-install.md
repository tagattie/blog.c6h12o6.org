+++
title = "Bhyve on FreeBSDにFreeBSD-CURRENTをUEFIモードでインストールする - 本編"
date = "2018-03-24T23:34:00+09:00"
categories = ["FreeBSD"]
tags = ["freebsd", "bhyve", "vm", "kernel", "virtualization", "current", "uefi", "vm-bhyve", "vnc", "remmina"]
+++

[まえがき](/post/freebsd-bhyve-freebsd-intro/)では、FreeBSDが提供する仮想化機構について簡単に説明しました。また、仮想化機構の一つであるbhyveを用いて仮想マシン(Virtual Machine, VM)を管理するためのシェルスクリプト、[vm-bhyve](https://github.com/churchers/vm-bhyve)についてもあわせて紹介しました。

本記事では、vm-bhyveを用いて、FreeBSD-CURRENTが動作するVMを構築する手順について見ていきます。基本的には、vm-bhyveの[README](https://github.com/churchers/vm-bhyve/blob/master/README.md)のQuick-Startにそって進めます。

### 必要なソフトウェアのインストール
まず、vm-bhyveをインストールします。また、FreeBSDをUEFI (Unified Extensible Firmware Interface)モードで起動することにしましたので、UEFIファームウェアもあわせてインストールします。(bhyveそのものは、ベースシステムにすでに組み込まれていますのでインストールは不要です。)

``` shell
pkg install vm-bhyve bhyve-firmware
```

### VMディレクトリ(あるいはファイルシステム)の作成
次に、vm-bhyveのVM設定ファイルやVMのディスクイメージを格納するディレクトリ(あるいはファイルシステム)を作成します。vm-bhyveはZFSに対応していますので、VMの格納場所をZFS上に置くのであれば、ファイルシステムを作成するほうがおすすめです。

- ディレクトリの場合

    ``` shell
mkdir -p /<path>/<to>/<vms>
```

- ZFSファイルシステムの場合

    ``` shell
zfs create <pool>/<vms>
```

以降、本記事では、VM格納用のファイルシステムとして`ztank/vms`を使用します。また、本ファイルシステムのマウントポイントを`/mnt/tank/vms`とします。

### vm-bhyveの初期化
以下のコマンドを実行して、vm-bhyveの初期設定を行ないます。

``` shell
sysrc vm_enable=YES
sysrc vm_dir="zfs:ztank/vms"
vm init
```

ZFSを使用しない場合は、2行目を`sysrc vm_dir="/<path>/<to>/<vms>"`としてください。

### 仮想スイッチの作成
bhyve上のVMが外部のネットワークと通信を行えるように、仮想スイッチを作成します。

vm-bhyveのREADMEやマニュアルには、`vm switch`コマンドを用いて仮想スイッチを作成する手順が示されています。しかし、本手順にしたがって仮想スイッチを作成すると、(VM上の)ゲストOSから(bhyveを動作させている)ホストOS上のサービスへのアクセスに問題が生じることがわかっています。(例えば、ゲストOSからの`ping`にホストOSが応答しない、ゲストOSからホストOS上のsamba共有ディレクトリにアクセスできない、など。詳しくは以下の記事を参照。)

- IPv4での問題(例) - [strange network problem about bridge #146](https://github.com/churchers/vm-bhyve/issues/146)
- IPv6での問題(例) - [Bhyve + IPv6 can't ping from guest to host until host ping guest](https://forums.freebsd.org/threads/bhyve-ipv6-cant-ping-from-guest-to-host-until-host-ping-guest.62851/)

この問題を回避するため、本記事では**マニュアル**で仮想スイッチを作成します。まず、vm-bhyveのシステム設定ファイルを以下の内容で作成します。

- /mnt/tank/vms/.config/system.conf

    ``` conf
switch_list="public"
bridge_public="bridge0"
```

この状態で、仮想スイッチの一覧コマンド`vm switch list`を実行してみてください。

``` shell-session
# vm switch list
NAME            TYPE       IDENT       VLAN      NAT          PORTS
public          manual     bridge0     n/a       n/a          n/a
```

仮想スイッチにリストに`public`(タイプは`manual`)が追加されています。

さて、マニュアルで仮想スイッチを作成するということは、`public`という名前の仮想スイッチが`bridge0`というネットワークインターフェイスを使う、ということ以外はvm-bhyveは関知しません。`bridge0`のメンバーとなる物理ネットワークインターフェイスの名前をはじめとする、各種設定は手動で行なう必要があります。また、上記した問題も解決する必要があります。

上記問題の解決策は、ひと言でいうと「**ブリッジインターフェイスにアドレスを割り当てる**」ことです。FreeBSDハンドブックの[Bridgingの節](https://www.freebsd.org/doc/handbook/network-bridging.html)に以下の記述があります。

> If the bridge host needs an IP address, set it on the bridge interface, not on the member interfaces. The address can be set statically or via DHCP.

ブリッジインターフェイスを持つホスト(今回、bhyveのホストOSが稼働しているマシン)にIPアドレスを割り当てる場合は、「ブリッジのメンバーインターフェイスではなく、ブリッジインターフェイス自体にアドレスを割り当てる」ということですね。さらに、IPv6アドレスについては、ブリッジインターフェイスにリンクローカルアドレス**も**割り当てられるように、`auto_linklocal`フラグを指定します。(参考: [Bhyve + IPv6](https://nbari.com/post/bhyve-ipv6/))

説明が長くなりましたが、以上の要領をまとめたブリッジまわりのマニュアル設定を以下に示します。(`em0`はお使いの環境の物理ネットワークインターフェイス名に合わせて読み替えてください。) 以下の例ではIPv4/IPv6アドレスともに、スタティックにアドレスを割り当てていますが、DHCP (Dynamic Host Configuration Protocol)やSLAAC (Stateless Address Autoconfiguration)を用いることも可能です。

- /etc/rc.conf

    ``` conf
cloned_interfaces="bridge0 tap0"
ifconfig_bridge0="inet <IPv4アドレス> netmask <ネットマスク> addm em0 addm tap0"          # IPv4アドレスをブリッジインターフェイスに割り当て
ifconfig_bridge0_ipv6="inet6 <IPv6アドレス> prefixlen <プレフィクス長> auto_linklocal"    # IPv6アドレスをブリッジインターフェイスに割り当て
ifconfig_em0="up"                                                                         # 物理ネットワークインターフェイスはupさせるだけ
ifconfig_tap0="description 'dummy-tap' up"
autobridge_interfaces="bridge0"
autobridge_bridge0="tap* em0"
defaultrouter="<デフォルトルータのIPv4アドレス>"
ipv6_defaultrouter="<デフォルトルータのIPv6リンクローカルアドレス>%bridge0"
```

以上で仮想スイッチの設定は終了です。ブリッジインターフェイスを有効にするため、ここでいったんFreeBSDマシンを再起動しておきましょう。

### VMのテンプレート作成
FreeBSDをUEFIモードで起動させることにしましたので、VMのテンプレートファイルを作成します。(Legacyモードで起動させる場合は本項の作業は不要です。)

vm-bhyveをインストールしたときに、サンプルのテンプレートファイルもあわせてインストールされていますので、これをVMのテンプレートディレクトリにコピーします。

``` shell
cp /usr/local/share/examples/vm-bhyve/* /mnt/tank/vms/.templates/
cd /mnt/tank/vms/.templates
cp freebsd.conf freebsd-uefi.conf
```

FreeBSD向けのテンプレート`freebsd.conf`をコピーして、FreeBSD UEFIモード起動用のテンプレートを作成しましょう。ここでは、`freebsd-uefi.conf`というファイル名にしておきます。後ほどVMを作成する際に、この名前でテンプレートを指定します。ファイル中のコメント末尾に(*)を付したものは、お好みに合わせて調整してください。

- freebsd-uefi.conf

    ``` conf
loader_timeout=5               # ローダからOSが自動起動するまでの待ち時間(秒)(*)
uefi="yes"                     # UEFIファームウェアをロードする
cpu=4                          # VMのCPU数(*)
memory="4G"                    # VMのメモリサイズ(*)
utctime="yes"                  # VMの時計をUTCに合わせる(*)
disk0_type="virtio-blk"        # 準仮想化ブロックデバイスを指定
disk0_name="disk0.img"         # VMのディスクイメージファイル名(*)
disk0_size="64G"               # VMのディスクサイズ(*)
network0_type="virtio-net"     # 準仮想化ネットワークデバイスを指定
network0_switch="public"       # 仮想スイッチ名
graphics="yes"                 # グラフィカルコンソールを使用
graphics_port=5900             # コンソールを提供するVNCサーバの待ち受けポート(*)
graphics_res="1600x900"        # コンソールの画面サイズ(*)
xhci_mouse="yes"               # ポインティングデバイスを使用
```

### VMの作成とインストール
次はいよいよFreeBSDのインストールです。インストールに先立って、必要なイメージファイルをダウンロードします。FreeBSD-CURRENTをインストールするので、現時点で最新のスナップショットイメージをダウンロードします。

``` shell
vm iso ftp://ftp.jp.freebsd.org/pub/FreeBSD/snapshots/ISO-IMAGES/12.0/FreeBSD-12.0-CURRENT-amd64-20180322-r331345-disc1.iso
```

先ほど作成したテンプレートを指定してVMを作成します。ここでは、VM名を`freebsd-current`としました。

``` shell
vm create -t freebsd-uefi freebsd-current
```

VMの一覧を表示するコマンド`vm list`を実行してみましょう。

``` shell-session
# vm list
NAME            DATASTORE       LOADER      CPU    MEMORY    VNC                  AUTOSTART    STATE
freebsd-current default         uefi        4      4G        -                    No           Stopped
```

いま作成したVMがリストに表示されていますね。このVMを格納する場所として、ディレクトリ`freebsd-current`が作成され、その中にVM設定ファイルと空のディスクイメージが格納されています。ディレクトリの内容は以下のようになっています。

``` shell-session
$ ls -l /mnt/tank/vms/freebsd-current
total 5
-rw-r--r--  1 root  wheel  68719476736  3月 23 21:42 disk0.img
-rw-r--r--  1 root  wheel          278  3月 23 21:42 freebsd-current.conf
```

最後に、イメージファイルを使ってFreeBSD-CURRENTをインストールします。

``` shell
vm install freebsd-current FreeBSD-12.0-CURRENT-amd64-20180322-r331345-disc1.iso
```

このコマンドを実行すると、VMがVNCでの接続待ち状態になりますので、VNCクライアントを用いてVMに接続してください。VNCクライアントとしては、[先日の記事](/post/freebsd-windows-rdp/)でご紹介した[Remmina](https://www.remmina.org/)がおすすめです。RemminaでのVNC接続を行なう際に、一点だけ注意があります。色数の設定を24 bpp以上にしないと画面がうまく表示されないようなので、ご注意ください。(下図)

![RemminaのVNC設定画面](/img/bhyve/freebsd-remmina-vnc-config.png)

VNCで接続するとおなじみのスプラッシュ画面からインストーラに進みますので、いつものようにインストール作業を行なえばOKです。

![FreeBSDの起動画面](/img/bhyve/freebsd-remmina-freebsd-boot.png)

![FreeBSDのインストーラ画面](/img/bhyve/freebsd-remmina-freebsd-installer.png)

### VMの運用
無事、インストールできましたでしょうか? あとは、以下のようなコマンド群を用いて、起動、終了などの運用を行なうことができます。

``` shell
vm start freebsd-current       # VMの起動
vm stop freebsd-current        # VMのシャットダウン
vm reset freebsd-current       # VMの強制リセット
vm poweroff freebsd-current    # VMの強制パワーオフ
vm destroy freebsd-current     # VMの削除
```

また、ホストOSの起動時にVMも自動的に起動させたい場合は、以下のコマンドを実行します。

``` shell
sysrc vm_list+="freebsd-current"
sysrc vm_delay=5
```

VMのリストを表示させてみるとAUTOSTARTの欄が`Yes`になりました。

``` shell-session
# vm list
NAME            DATASTORE       LOADER      CPU    MEMORY    VNC                  AUTOSTART    STATE
freebsd-current default         uefi        4      4G        -                    Yes [1]      Stopped
```

追伸:  
FreeBSD-CURRENTの最近のスナップショットをいくつか試してみたのですが、インストールは成功裡に終了するものの、再起動をかけるとブートローダのところで止まってしまいますね…。11.1-RELEASEならば、インストールもその後の再起動も成功します。CURRENTを動かすことが目的ならば、11.1-RELEASEをインストール→自力でソースからビルドしてCURRENTに更新、のほうが早そうな気がします…。

### 参考文献
1. vm-bhyve, https://github.com/churchers/vm-bhyve
1. strange network problem about bridge #146, https://github.com/churchers/vm-bhyve/issues/146
1. Bhyve + IPv6 can't ping from guest to host until host ping guest, https://forums.freebsd.org/threads/bhyve-ipv6-cant-ping-from-guest-to-host-until-host-ping-guest.62851/
1. Bridging, https://www.freebsd.org/doc/handbook/network-bridging.html
1. Bhyve + IPv6, https://nbari.com/post/bhyve-ipv6/
