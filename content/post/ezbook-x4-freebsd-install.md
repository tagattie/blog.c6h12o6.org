+++
title = "Jumper EZbook X4にFreeBSD 11.2-RELEASEをカスタムインストールする(本編)"
date = "2018-09-11T21:04:00+09:00"
categories = ["FreeBSD"]
tags = ["freebsd", "jumper", "ezbook", "install"]
+++

[前回の記事](/post/ezbook-x4-freebsd-intro/)では、[Jumper社](http://www.jumper.com.cn/)のラップトップPC EZbook X4を入手したことを書きました。Chromium OSマシンとして働いてもらおうと期待していたのですが、残念ながら最新の安定版であるR68ではブートできませんでした。(自分でビルドしたものですので何か手順をミスしている可能性はあります。)

なんとかChromium OSマシンとして使いたいので、もう少し調査を続けるつもりです。しかし、すぐに解決するのは難しそうに思えますので、いったんChromium OSはおいておいてFreeBSDのインストールを試してみることにしました。

本記事では、EZbook X4にFreeBSDをカスタムインストールする手順を紹介していきます。手順を大まかに分けると以下のようになります。

- [USBメモリのカスタマイズ](#usbメモリのカスタマイズ)
- [USBメモリでの起動とリモートログイン](#usbメモリでの起動とリモートログイン)
- [ディスクのパーティショニング](#ディスクのパーティショニング)
- [ファイルシステムの作成](#ファイルシステムの作成)
- [インストールおよびその後の設定](#インストールおよびその後の設定)

### USBメモリのカスタマイズ
まずはインストールの準備からです。インストーラのイメージファイルをUSBメモリに書き込んで、そのUSBメモリを用いて起動する、というのは通常の手順と同じです。ただし、提供されているイメージでは、EZbook X4のWiFiインターフェイスが使えないという問題があります。また、EZbook X4のコンソールではなく、リモートからSSHログインして作業を行ないたいので、USBメモリを少しカスタマイズします。

まず、すでに稼働しているFreeBSDマシンなどを用いて、通常通りイメージファイルをUSBメモリに書き込みます。以下の例ではUSBメモリデバイスが`da0`となっていますが、これはお使いの環境に合わせて読み替えをお願いします。

``` shell
dd if=/path/to/image/FreeBSD-11.2-RELEASE-amd64-memstick.img of=/dev/da0 bs=1m
```

USBメモリが作成できたら、次はこれをマウントして内容のカスタマイズを行ないます。

``` shell
mkdir /tmp/usbstick
mount -t ufs /dev/da0s2a /tmp/usbstick
```

カスタマイズするのは以下の三点です。

- WiFiネットワークインターフェイスの有効化

    EZbook X4はWiFiモジュールとしてIntel社の[Wireless-AC 3165](https://ark.intel.com/products/89450/Intel-Dual-Band-Wireless-AC-3165)を搭載していますが、残念ながら起動時に自動的に有効にはなりません。そこで、適切なカーネルモジュールをロードして、WiFiインターフェイスを有効化します。

    ``` shell
cat >> /tmp/usbstick/boot/loader.conf << EOF
if_iwm_load="YES"
iwm7265Dfw_load="YES"
EOF
```
	
- WiFiアクセスポイントへの接続

    リモートからのSSHログインができるように、手もとの適当なアクセスポイントへ接続する設定を行ないます。アクセスポイントのSSIDおよび事前共有キー(パスワード)については、お使いの環境に合わせて適切な内容を設定してください。

    {{< highlight shell >}}
# 起動時にWiFiインターフェイスを有効化(アドレスはDHCPで取得)
cat >> /tmp/usbstick/etc/rc.conf << EOF
wlans_iwm0="wlan0"
create_args_wlan0="country jp"
ifconfig_wlan0="WPA SYNCDHCP"
EOF
# アクセスポイントへの接続設定情報
cat > /tmp/usbstick/etc/wpa_supplicant.conf << EOF
network={
  ssid="<your ssid>"
  psk="<your pre-shared key>"
}
EOF
{{< /highlight >}}


- RootユーザでのSSHの(一時的)有効化

    リモートからSSHログインしてインストール作業が行えるように、rootユーザでのSSHログインを一時的に許可します。

    ``` shell
sed -i '' 's/^#PermitRootLogin no/PermitRootLogin yes/g' /tmp/usbstick/etc/ssh/sshd_config
```

USBメモリのカスタマイズは以上です。USBメモリをアンマウントしておきましょう。

``` shell
umount /tmp/usbstick
rmdir /tmp/usbstick
```

### USBメモリでの起動とリモートログイン
カスタマイズが終わったら、さっそくEZbook X4のUSBポート(左右にひとつづつあります)にUSBメモリを挿入して、本メモリからブートします。(何もしないとWindowsが立ち上がると思いますので、その場合はBIOSをいったん起動してブートデバイスにUSBメモリを選択します。)

カーネルの起動メッセージがずらずらと表示された後、Installer, Shell, あるいはLive CDのいずれかを選択する画面になりますので、Live CDを選択してください。

通常のログインプロンプトが現れますので、rootでログインします。(この際パスワードは不要です。) ログインしたら安全のためrootのパスワードをまず設定し、その後sshdを起動します。

``` shell
mount -rw /              # ルートファイルシステムを読み書き可能なように再マウント
passwd                   # rootユーザのパスワードを設定
service sshd onestart    # sshdを起動
mount -r /               # ルートファイルシステムを読み出し専用に戻す
```

以上でSSHを用いたリモートアクセスが可能になりました。以降は使い慣れたマシンからリモートログインして作業を継続します。(ログイン先となるEZbook X4のIPアドレスについては、上記コンソールでの操作時に`ifconfig`コマンドを実行するなどして確認しておきます。)

``` shell
ssh root@<EZbookのIPアドレス>
```

### ディスクのパーティショニング
ここからは、前回の記事で参考文献としてあげた、以下の記事にそって作業を続けていきます。想定するブート環境、およびファイルシステム構成が一部異なるだけで基本的には記事の内容をそのまま実行しています。

- [FreeBSD 11.1-RELEASEを自由なZFSパーティション構成でインストールする (クソゲ〜製作所)](https://decomo.info/wiki/freebsd/install/install_freebsd_11_1_by_manually_zfs_partitioning)

ZFSを扱いますので、まず必要なカーネルモジュールをロードします。また、ディスク(SSD)へのアクセス単位が4Kセクタ単位となるようカーネル変数を調整します。

``` shell
kldload zfs
sysctl vfs.zfs.min_auto_ashift=12
```

ディスクのパーティションを切る前に、念のため既存のパーティション情報を破壊しておきます。(今回は、もともとWindowsが格納されているSSDへのインストールですので、この手順は不要なのですが一応実行しておきます。)

``` shell
zpool labelclear -f ada0
gpart destroy -F ada0
dd if=/dev/zero of=/dev/ada0 bs=16k count=1
```

既存のパーティション情報をクリアしたら、改めてGPT (GUID Partition Table)スキームを用いてパーティションテーブルを作成します。今回はUEFIブート環境のみを想定しますので、ESP (EFI System Partition)、スワップ、およびZFSルートプール用の各パーティションを作成します。

``` shell
gpart create -s GPT ada0
gpart add -a 4k -t efi          -s 512m -l efi-ada0  ada0
gpart add -a 4k -t freebsd-swap -s 8g   -l swap-ada0 ada0
gpart add -a 4k -t freebsd-zfs          -l data-ada0 ada0
```

パーティションを作成したら、各パーティションの既存のラベル情報を削除しておきます。これも念のため一応です。

``` shell
zpool labelclear -f ada0p1
zpool labelclear -f ada0p2
zpool labelclear -f ada0p3
```

最後に、ESPにUEFIブート用のブートローダを書き込みます。

``` shell
gpart bootcode -p /boot/boot1.efifat -i 1 ada0
```

### ファイルシステムの作成
パーティショニングが終わりました。

次は、ZFSパーティション(パーティションラベル: `data-ada0`)にZFSを用いたファイルシステムを作成します。ストレージプールの名前は`zroot`としていますが、これはお好みにあわせて変更していただいてかまいません。また、プロパティとして`atime`および`compression`を指定していますが、このあたりもお好み次第です。

``` shell
zpool create -o altroot=/mnt -o cachefile=/tmp/zpool.cache -O mountpoint=none -O atime=off -O compression=lz4 zroot /dev/gpt/data-ada0
```

次に、`zroot`ストレージプール上に各ファイルシステムを作成します。ファイルシステムの構成はお好み次第ですが、最後にストレージプールの`bootfs`プロパティの値を設定するのを忘れないようにしましょう。

``` shell
zfs create -o mountpoint=/ -o canmount=noauto -p zroot/ROOT/default
zfs mount zroot/ROOT/default

zfs create -o mountpoint=/tmp           -o setuid=off                                zroot/tmp
zfs create -o mountpoint=/usr           -o canmount=off                              zroot/usr
zfs create -o mountpoint=/usr/home                                                   zroot/usr/home
zfs create -o mountpoint=/usr/local                                                  zroot/usr/local
zfs create -o mountpoint=/usr/local/man -o compression=off -o exec=off -o setuid=off zroot/usr/local/man
zfs create -o mountpoint=/usr/share                                                  zroot/usr/share
zfs create -o mountpoint=/usr/share/man -o compression=off -o exec=off -o setuid=off zroot/usr/share/man
zfs create -o mountpoint=/var           -o canmount=off                              zroot/var
zfs create -o mountpoint=/var/audit     -o exec=off -o setuid=off                    zroot/var/audit
zfs create -o mountpoint=/var/crash     -o exec=off -o setuid=off                    zroot/var/crash
zfs create -o mountpoint=/var/db        -o exec=off -o setuid=off                    zroot/var/db
zfs create -o mountpoint=/var/empty     -o exec=off -o setuid=off                    zroot/var/empty
zfs create -o mountpoint=/var/log       -o exec=off -o setuid=off                    zroot/var/log
zfs create -o mountpoint=/var/mail      -o atime=on -o exec=off -o setuid=off        zroot/var/mail
zfs create -o mountpoint=/var/run       -o exec=off -o setuid=off                    zroot/var/run
zfs create -o mountpoint=/var/tmp       -o setuid=off                                zroot/var/tmp
chmod 555 /mnt/var/empty
chmod 1777 /mnt/tmp /mnt/var/tmp
zfs set readonly=on zroot/var/empty

zpool set bootfs=zroot/ROOT/default zroot
```

### インストールおよびその後の設定
ようやくFreeBSDをインストールする準備が整いました。

実は面倒なパートはすでに終わっていて、インストールそのものはベースシステムおよびカーネル一式の圧縮ファイルを`tar`コマンドを使って展開するだけです。これらのファイルは`/usr/freebsd-dist`に格納されています。

``` shell
cd /usr/freebsd-dist
tar -xvpzf base.txz -C /mnt
tar -xvpzf kernel.txz -C /mnt
```

ファイル一式の展開が終わったら、FreeBSDを再起動する前にOSまわりの最小限の設定を行なっておきましょう。

インストールしたシステムのルートディレクトリ(現在`/mnt`としてマウントされているはずです)に`chroot`します。

``` shell
chroot /mnt /bin/csh -l
```

以下は最小限の設定例です。

``` shell
tzsetup    # タイムゾーンの設定
passwd     # rootのパスワードの設定

# ブートローダの設定
cat > /boot/loader.conf << EOF
if_iwm_load="YES"
iwm7265Dfw_load="YES"
zfs_load="YES"
EOF

# ファイルシステムのマウント設定
cat > /etc/fstab << EOF
# Device           Mountpoint           FStype  Options         Dump    Pass#
/dev/gpt/swap-ada0 none                 swap    sw              0       0
EOF

# ホスト名、WiFiネットワークインターフェイス、などの設定
cat > /etc/rc.conf << EOF
hostname="<your hostname>"
zfs_enable="YES"
wlans_iwm0="wlan0"
create_args_wlan0="country jp"
ifconfig_wlan0="WPA SYNCDHCP"
sshd_enable="YES"
EOF

# WiFiアクセスポイントへの接続情報
cat > /etc/wpa_supplicant.conf << EOF
network={
  ssid="<your ssid>"
  psk="<your pre-shared key>"
}
EOF

exit
```

設定が終わったら、chroot環境から`exit`して一連のインストール作業は完了です。`reboot`してFreeBSDが起動してくるのを待ちましょう。

**注**: FreeBSDでなくEFIシェルが起動してしまう場合は、BIOSの設定でブートデバイスの優先順位を見直してみてください。

### 参考文献
1. Intel® Dual Band Wireless-AC 3165, https://ark.intel.com/products/89450/Intel-Dual-Band-Wireless-AC-3165
1. FreeBSD 11.1-RELEASEを自由なZFSパーティション構成でインストールする, https://decomo.info/wiki/freebsd/install/install_freebsd_11_1_by_manually_zfs_partitioning
