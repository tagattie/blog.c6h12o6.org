+++
title = "EdgeRouter LiteにFreeBSDをインストール - イメージ作成編"
date = "2018-01-31T22:24:11+09:00"
categories = ["FreeBSD"]
tags = ["freebsd", "edgerouter"]
+++

[EdgeRouter LiteにFreeBSDをインストール - ビルド編](/post/freebsd-edgerouter-lite-build/)では、FreeBSDのソースコードをチェックアウトするところから、バイナリ一式のビルドまでを記録しました。本イメージ作成編では、バイナリ一式からUSBメモリイメージを作成する手順を説明していきます。

イメージ作成の手順は大まかに書くと、

- イメージファイルの作成
- パーティションスキームの作成
- ブートパーティションの作成
- BSDパーティションの作成

となります。以下、詳しく手順を述べます。

1. イメージファイルの作成とメモリディスクの作成

    まず、空のイメージファイルを作成し、メモリディスクとしてマウントできる状態にします。ここでは、3GiBのイメージファイルを作成しています。(初回起動時に、USBメモリのサイズいっぱいまで使用領域を拡張するため、ここでは最低限必要なサイズを指定すればOKです。)

    ```shell
    truncate -s 3221225472 edgerouter.img
    mdconfig -a -t vnode -x 63 -y 255 -f edgerouter.img
    ```
    
    以下、`mdconfig`コマンドによりデバイス`md0`が作成されたものとして、手順を説明していきます。

1. パーティションスキームの作成

    MBR形式でパーティションスキームを作成します。

    ```shell
    gpart create -s MBR md0
    ```

1. ブートパーティションの作成と必要なファイルのコピー

    ブート用のMS-DOSパーティションを作成し、必要なファイルをコピーします。
    
    ```shell
    gpart add -a 63 -b 63 -s 524287 -t !12 md0
    gpart set -a active -i 1 md0
    newfs_msdos -L msdosboot -F 16 /dev/md0s1
    mkdir /tmp/boot
    mount -t msdosfs -l /dev/md0s1 /tmp/boot
    cd /mnt/edgerouter/boot
    find kernel -print -depth | cpio -pd /tmp/boot
    umount /tmp/boot
    rmdir /tmp/boot
    ```

1. BSDパーティションの作成とUFSファイルシステムイメージの作成

    次にFreeBSD一式を格納するパーティションを作成、big endianのファイルシステムイメージを作成します。その後、ファイルシステムイメージを、メモリディスク上のBSDパーティションに書き込みます。

    ```shell
    gpart add -t freebsd md0
    gpart create -s BSD md0s2
    gpart add -a 65536 -t freebsd-ufs md0s2
    makefs -B big -f 1572864 -t ffs -o label=rootfs -o version=2 -s 2885681152 edgerouter.ufs /mnt/edgerouter
    dd if=edgerouter.ufs of=/dev/md0s2a bs=1m
    ```

1. USBメモリへの書き込み

    最後に、メモリディスクをアンマウントし、出来上がったイメージをUSBメモリに書き込んで完了です。(`da0`は適宜USBメモリのデバイス名に読み替えてください。)

    ```shell
    mdconfig -d -u md0
    dd if=edgerouter.img of=/dev/da0 bs=1m
    ```
