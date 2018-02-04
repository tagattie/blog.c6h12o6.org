+++
title = "EdgeRouter LiteにFreeBSDをインストール - ブート編"
date = "2018-02-04T14:02:52+09:00"
categories = ["FreeBSD"]
tags = ["freebsd", "edgerouter"]
+++

[EdgeRouter LiteにFreeBSDをインストール - イメージ作成編](/post/freebsd-edgerouter-lite-image/)では、ビルドしたバイナリ一式からイメージファイルを作成し、これをUSBメモリに書き込むところまでを説明しました。本ブート編では、USBメモリの交換とFreeBSDをブートするためのU-Bootコマンドの設定について説明します。

1. ケースを開いてUSBメモリを差し替え

    まず、背面にある3か所のネジをプラスドライバーを用いてはずします。
    [![EdgeRouter Lite外観](/img/edgerouter-lite-outlook-thumbnail.jpg)](/img/edgerouter-lite-outlook.jpg)
    次に、上蓋を持ち上げるようにしてはずします。向かって左端にUSBメモリがささっているところがありますので、元のUSBメモリを抜いて、FreeBSDを書き込んだUSBメモリを挿入します。(コネクタが少し固めなので、はずすときにはやや力が要ります。)
    [![EdgeRouter Lite内部](/img/edgerouter-lite-innerlook-thumbnail.jpg)](/img/edgerouter-lite-innerlook.jpg)
    差し替えたら上蓋を閉じます。

1. シリアルコンソール経由でEdgeRouterに接続

    EdgeRouter Liteのコンソールポートに、シリアルケーブルを接続します。ケーブルはCisco互換のものを用います。(例えば[これ](https://www.amazon.co.jp/dp/B01EJQB7SE)。他にも、安いものでは1,000円くらいからで入手可能なようです。) ケーブルをPCのUSBポートに接続し、以下のコマンドを実行してEdgeRouterのシリアルコンソールに接続します。
    ```shell
    cu -l /dev/cuaU0 -s 115200
    ```

1. 電源投入

    電源を投入(ACアダプタの端子を本体に挿入)すると、U-Bootのブートメッセージが出てきますので、スペースキーなどを連打して、ブートプロンプト(`Octeon ubnt_e100#`)を表示させます。
    ```uboot
    Looking for valid bootloader image....
    Jumping to start of image at address 0xbfc80000


    U-Boot 1.1.1 (UBNT Build ID: 4670715-gbd7e2d7) (Build time: May 27 2014 - 11:16:22)

    BIST check passed.
    UBNT_E100 r1:2, r2:18, f:4/71, serial #: XXXXXXXXXXXX
    MPR 13-00318-18
    Core clock: 500 MHz, DDR clock: 266 MHz (532 Mhz data rate)
    DRAM:  512 MB
    Clearing DRAM....... done
    Flash:  4 MB
    Net:   octeth0, octeth1, octeth2
    
    USB:   (port 0) scanning bus for devices... 
          USB device not responding, giving up (status=0)
    1 USB Devices found
           scanning bus for storage devices...
    No device found. Not initialized?
     0
    Octeon ubnt_e100#
    ```

1. ブートコマンドの書き換え

    もともとのブートコマンドはLinuxをブートするようになっています。念のため、これを`bootcmd_orig`としてバックアップしておきます。FreeBSDをブートするためのコマンドを新たに`bootcmd_freebsd`として定義します。そして、[USBメモリとの相性問題](/post/edgerouter-lite-usb-drive-compatibility/)の対策のために、FreeBSDをブートする前にいったんUSBバスをリセットするように、最終的なブートコマンド`bootcmd`を指定します。最後に変更を保存します。
    ```uboot
    Octeon ubnt_e100# setenv bootcmd_orig 'fatload usb 0 $loadaddr vmlinux.64;bootoctlinux $loadaddr coremask=0x3 root=/dev/sda2 rootdelay=15 rw rootsqimg=squashfs.img rootsqwdir=w mtdparts=phys_mapped_flash:512k(boot0),512k(boot1),64k@1024k(eeprom)'
    Octeon ubnt_e100# setenv bootcmd_freebsd 'fatload usb 0 $loadaddr kernel/kernel;bootoctlinux $loadaddr coremask=0x3'
    Octeon ubnt_e100# setenv bootcmd 'sleep 5;usb reset;sleep 2;$(bootcmd_freebsd)'
    Octeon ubnt_e100# saveenv
    ```
    
1. 再起動

    ここで再起動をかけてやればFreeBSDが起動してきます。
    ```uboot
    Octeon ubnt_e100# reset
    ```
    シリアルコンソールから抜けるには`~.`(チルダ、ピリオド)を入力します。
