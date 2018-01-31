+++
title = "EdgeRouter LiteにFreeBSDをインストール - ビルド編"
date = "2018-01-30T21:50:19+09:00"
categories = ["FreeBSD"]
tags = ["freebsd", "edgerouter"]
+++

Ubiquiti Networks社の[EdgeRouter Lite](https://www.ubnt.com/edgemax/edgerouter-lite/)には、デフォルトでLinuxベースのEdgeOSが搭載されていますが、FreeBSDでの動作実績([ここ](http://rtfm.net/FreeBSD/ERL/)、[ここ](http://www.daemonology.net/blog/2016-01-10-FreeBSD-EdgeRouter-Lite.html))もあります。そこで、本記事では、ソースコードの取得から、リリースイメージのUSBメモリの書き込みまでの手順を記録していきます。

なお、以下のビルドのためには、すでに動作しているFreeBSD環境が必要です。

[こちら](http://rtfm.net/FreeBSD/ERL/)でビルド済みのUSBメモリイメージが配布されていますので、てっとりばやく試したい場合には便利です。

1. ソースコードの取得

    ビルドしたいバージョンのソースコードを取得します。本記事では、11-RELEASE系の最新バージョンであるreleng/11.1ブランチ(記事執筆時点で11.1-RELEASE-p6)のソースコードを取得します。ソースコードの格納先とするディレクトリは任意ですが、ここでは`/usr/src`とします。
    ```shell
    svnlite checkout https://svn.freebsd.org/base/releng/11.1 /usr/src
    ```

1. パッチ

    EdgeRouter Lite用のカーネルコンフィグレーションは11-RELEASE系に用意されていますが、デフォルトではカーネルモジュールをビルドしないようになっています。しかし、ファイアウォールとしてPFを使用したい(IPFWはうまく動作しないようです)ので、以下のパッチをあて、ビルドが失敗するモジュールのみを除き、カーネルモジュールのビルドを有効にします。
    
    カーネルコンフィグレーションは`/usr/src/sys/mips/conf`以下にあります。
    ```diff
    --- ERL.orig    2018-01-30 21:20:23.550243000 +0900
    +++ ERL 2018-01-30 21:20:50.505064000 +0900
    @@ -25,7 +25,8 @@
     makeoptions    LDSCRIPT_NAME=ldscript.mips.octeon1
    
     # Don't build any modules yet.
    -makeoptions    MODULES_OVERRIDE=""
    +#makeoptions   MODULES_OVERRIDE=""
    +makeoptions    WITHOUT_MODULES="cxgbe mwlfw netfpga10g otusfw ralfw rtwnfw urtwnfw usb"
     makeoptions    KERNLOADADDR=0xffffffff80100000
    
     # We don't need to build a trampolined version of the kernel.
    ```

1. ビルド

    ソースををビルドします。
    ```shell
    cd /usr/src
    make -DDB_FROM_SRC -DNO_FSCHG TARGET=mips TARGET_ARCH=mips64 buildworld
    make -DDB_FROM_SRC -DNO_FSCHG TARGET=mips TARGET_ARCH=mips64 KERNCONF=ERL buildkernel
    make -DDB_FROM_SRC -DNO_FSCHG TARGET=mips TARGET_ARCH=mips64 DESTDIR=/mnt/edgerouter installkernel
    make -DDB_FROM_SRC -DNO_FSCHG TARGET=mips TARGET_ARCH=mips64 DESTDIR=/mnt/edgerouter installworld
    make -DDB_FROM_SRC -DNO_FSCHG TARGET=mips TARGET_ARCH=mips64 DESTDIR=/mnt/edgerouter distribution
    ```
    以上で`/mnt/edgerouter`以下にバイナリ一式が作成されました。

1. 設定ファイルの用意

    次に、手動で作成する必要のあるファイル(`/etc/fstab`および`/etc/rc.conf`)を追加します。

    ```shell
    cat << EOF > /mnt/edgerouter/etc/fstab
    # Device        Mountpoint         FStype  Options         Dump    Pass#
    /dev/ufs/rootfs /                  ufs     rw              1       1
    /dev/msdosfs/MSDOSBOOT /boot/msdos msdosfs rw,noatime      0       0
    tmpfs           /tmp               tmpfs   rw,mode=1777,size=64m 0 0
    EOF
    ```
    ```shell
    cat << EOF > /mnt/edgerouter/etc/rc.conf
    hostname="erl"
    ifconfig_octe0="inet DHCP"
    ifconfig_octe0_ipv6="inet6 accept_rtadv"
    sshd_enable="YES"
    growfs_enable="YES"
    EOF
    ```

    最後に、初回起動であることを示す番兵ファイルを設置します(初回起動時に、パーティションサイズをUSBメモリサイズいっぱいまで拡張するgrowfs処理を行なわせるため)。また、ブートパーティションをマウントするためのプレースホルダーを作っておきます。
    
    ```shell
    touch /mnt/edgerouter/firstboot
    mkdir /mnt/edgerouter/boot/msdos
    ```
    
以上で、USBメモリイメージを作成する準備ができました。

### 参考文献
1. EdgeRouter Lite, https://www.ubnt.com/edgemax/edgerouter-lite/
1. FreeBSD 11.x on Ubiquiti EdgeRouter Lite, http://rtfm.net/FreeBSD/ERL/
1. FreeBSD on EdgeRouter Lite - no serial port required, http://www.daemonology.net/blog/2016-01-10-FreeBSD-EdgeRouter-Lite.html
