+++
title = "Jumper EZbook X4にChromium OSをインストールする(ビルド・インストール編)"
date = "2019-02-05T19:20:00+09:00"
categories = ["ChromiumOS"]
tags = ["chromiumos", "jumper", "ezbook", "gemini-lake", "install"]
+++

[Jumper社](http://www.jumper.com.cn/)のラップトップEZbook X4 ([Gemini Lake世代](https://ark.intel.com/products/codename/83915/Gemini-Lake)のCeleron N4100を搭載)へ、[Chromium OS](https://www.chromium.org/chromium-os)のインストールを試みる記事の二回目です。

[前回の記事](/post/ezbook-x4-chromiumos-investigate/)では、EZbook X4におけるChromium OSのブート不具合の調査結果について報告しました。わかった後だからいえることですが、原因は非常に単純で、ソフトウェアコンポーネントのバグに加え、必要なファイルのインストール不備があったためでした。

本記事では調査結果をふまえ、EZbook X4で起動可能なUSBメモリイメージの作成、および内蔵SSDへのインストール手順について説明します。

**注**: 動作確認はEZbook X4でのみ行なっていますが、**Gemini Lake世代のCPUを搭載するPC**であれば本記事の手順によりChromium OSが動作する可能性があります。

実はChromium OSのビルド手順については、これまでに二回説明しています(下記記事)。

- [Chromium OSでバッテリ駆動時間は伸びるのか? NEC LaVie Zで試す - ビルド実行編](/post/chromiumos-self-build-build/)
- [ローカルのChromiumソースを使ってChromium OSをビルドする](/post/chromiumos-self-build-local-chromium/)

ですので、本記事ではビルド全体の流れをざっと再掲するとともに、EZbook X4で動作させるためのポイントについて説明したいと思います。ビルド手順の個々のステップの詳細については、必要に応じて過去記事をご参照ください。

では、手順を見ていきましょう。(本記事では簡単のため、ビルド時にオンデマンドでChromiumのソースを取得する方法について説明します。あらかじめ取得済みのChromiumソースを用いてビルドしたい場合は、[上記記事(ローカルのChromiumソースを使って…)](/post/chromiumos-self-build-local-chromium/)を参照願います。

### Chromium OSのソースコード取得
まず、ビルド対象のブランチを決めます(本記事執筆時点での最新の安定版ブランチは`stabilize-11151.113.B`)。そして、当該ブランチのソースコード一式をチェックアウトします。(Stable, BetaおよびDevの各[チャネル](https://support.google.com/chromebook/answer/1086915)に対応するブランチ名を確認するには、以下のURLが便利です。)

- [Chrome Releases (The Chrome Team)](https://chromereleases.googleblog.com/search/label/Chrome%20OS)

``` shell
BRANCH=stabilize-11151.113.B    # ビルド対象のブランチ名を指定
cd ~                            # ソースコードを格納するディレクトリに移動(ホームディレクトリ以外でもOK)
mkdir chromiumos-${BRANCH}
cd chromiumos-${BRANCH}
repo init -u https://chromium.googlesource.com/chromiumos/manifest.git --repo-url https://chromium.googlesource.com/external/repo.git -b ${BRANCH}
repo sync -j 4
```

### Chromium OSのビルドとUSBメモリイメージ作成
次に、ビルドのためのchroot環境であるChromium OS SDK (CrOS SDK)を起動します。

``` shell
cros_sdk --enter
```

CrOS SDKが起動したら、必要な設定を行なった後、ビルドおよびUSBメモリイメージの作成を行ないます。(以下、CrOS SDK上で実行するコマンドには`(cr)$`を表示しました。)

[前回記事](/post/ezbook-x4-chromiumos-investigate/)で述べた二つめの不具合(必要なファイルのインストール不備)を、このステップで解決します。でも、解決の前にちょっとだけ前置きを。

Chromium OSでは、OSに含めるソフトウェアパッケージを管理するために、[Gentoo Linux](https://www.gentoo.org/)に由来する[Portage](https://wiki.gentoo.org/wiki/Portage)という仕組みを使っています。Portageにおいて、特定の機能やコンポーネントの有効化・無効化を指定するためのスイッチが[USEフラグ](https://wiki.gentoo.org/wiki/USE_flag)です。このUSEフラグを活用することで不具合を解決できます。

具体的には、Linuxファームウェアパッケージ`sys-kernel/linux-firmware`に用意されているフラグ`linux_firmware_i915_glk`を`USE`環境変数に追加指定します。これにより、ファームウェアパッケージにGemini Lake CPU用のグラフィックスファームウェアが含まれ、インストールされるようになります。

``` shell-session
(cr)$ export BOARD=amd64-generic                     # ハードウェア構成としてIntel/AMD 64ビットCPU搭載機の一般的構成を指定
(cr)$ export USE="-kernel-4_4 kernel-4_14"           # Linux 4.14系を指定(4.4系ではGemini Lake未対応のため)
                                                     # (R71以降はデフォルトで4.14系が使用されるため本指定は不要)
(cr)$ export USE="${USE} chrome_media"               # 追加のメディアコーデックをビルドする指定
(cr)$ export USE="${USE} linux_firmware_i915_glk"    # Gemini Lake向けのグラフィックスファームウェアをインストールする指定
(cr)$ ./setup_board --board=${BOARD}
(cr)$ ./set_shared_user_password.sh
(cr)$ ./build_packages --board=${BOARD}
(cr)$ ./build_image --board=${BOARD} --noenable_rootfs_verification dev
```

参考: `sys-kernel/linux-firmware`パッケージに用意されている他のUSEフラグに興味がある場合は、以下のコマンドを実行してみてください。`<use>`タグ内に、用意された個々のフラグが記述されています。

``` shell-session
(cr)$ less ../third_party/chromiumos-overlay/sys-kernel/linux-firmware/metadata.xml
```

### USBメモリイメージの修正
さて、以上でUSBメモリイメージの作成が完了しました。以下のディレクトリにイメージファイル(`chromiumos_image.bin`)ができていると思います。ちょっと確認してみましょう。

``` shell-session
(cr)$ ls ../build/images/amd64-generic/latest
boot.config           esp                   partition_script.sh
boot.desc             license_credits.html  umount_image.sh
chromiumos_image.bin  mount_image.sh        unpack_partitions.sh
config.txt            pack_partitions.sh    vmlinuz.bin
```

OKでしたね。さっそくこのファイルを用いて、起動用USBメモリ作成→ブートへと進みたいところです。しかし、[前回記事](/post/ezbook-x4-chromiumos-investigate/)で述べた一つめの問題(GRUBのバグ)がまだ解決されていません。ブートを試す前にこの問題を解決しておきましょう。

#### パッチ済みGRUBファイルの入手
[前回記事](/post/ezbook-x4-chromiumos-investigate/)でも挙げたとおり、Apollo Lake世代CPU (Gemini Lake世代にもあてはまる)におけるGRUBの不具合について、以下の記事に詳しい説明があります。

- [Grub 2 issue on Intel N4200 (Pawit Pornkitprasan)](https://medium.com/@pawitp/grub-2-issue-on-intel-n4200-97c12d4db8af)
- [Grub for Chromium OS on Intel N4200 (GitHub / pawitp)](https://github.com/pawitp/grub2-chromiumos-n4200/)

これらの記事を参考にして手もとでビルドを行なう、あるいはビルド済みのファイルをダウンロードすることにより、パッチ済みのGRUBファイル(`grubx64.efi`)を入手します。(以下の例では、簡単のためビルド済みのファイルをダウンロード入手しています。)

``` shell
cd ~/chromiumos-${BRANCH}    # Chromium OSのソースをチェックアウトしたディレクトリにcd
# パッチ済みのGRUBファイルをダウンロード↓
curl -L -O https://github.com/pawitp/grub2-chromiumos-n4200/releases/download/v2.02-r1/grubx64.efi
```

この例では、**CrOS SDKの外部**で`curl`コマンドを用いてパッチ済みのファイルをダウンロードしていますが、CrOS SDK上でダウンロードしてもかまいません。`grubx64.efi`をCrOS SDKからアクセス可能な場所に保存しさえすれば、ファイルの入手方法は任意です。(以下では、Chromium OSのソースコードをチェックアウトしたトップディレクトリに保存したものとして説明します。)

#### パッチ済みGRUBファイルへの置き換え
パッチ済みのファイルが入手できました。では、USBメモリイメージ内のバグを含むGRUBファイルを、入手したパッチ済みのもので置き換えます。CrOS SDK上で以下のコマンドを実行してください。

``` shell-session
(cr)$ ./mount_gpt_image.sh --board=${BOARD} --esp_mountpt=/tmp/esp --most_recent            # イメージファイルをマウント(ESPを/tmp/espにマウント)
(cr)$ cd /tmp/esp/efi/boot
(cr)$ sudo cp bootx64.efi bootx64.efi.orig                                                  # バグありのGRUBファイルを一応バックアップ
(cr)$ sudo cp ~/trunk/grubx64.efi bootx64.efi.patched                                       # 入手したパッチ済みのGRUBファイルをコピー
(cr)$ sudo cp bootx64.efi.patched bootx64.efi                                               # バグありのファイルをパッチ済みのもので置き換え
(cr)$ cd ~/trunk/src/scripts
(cr)$ ./mount_gpt_image.sh --board=${BOARD} --esp_mountpt=/tmp/esp --most_recent --unmount  # イメージファイルをアンマウント
```

あるいは、FreeBSDでの操作のほうが慣れているという場合は、USBメモリイメージをCrOS SDKからFreeBSDマシンへ(sftpなどで)転送して、以下のコマンドを実行するのでもOKです。

``` shell-session
$ sudo mdconfig -f chromiumos_image.bin              # イメージファイルをメモリディスクととして設定
md0                                                  # メモリディスクのデバイス名がmd0として返される
$ mkdir /tmp/esp
$ sudo mount -t msdosfs /dev/md0p12 /tmp/esp         # パーティション12をマウント
$ cd /tmp/esp/efi/boot
$ sudo cp bootx64.efi bootx64.efi.orig               # バグありのGRUBファイルを一応バックアップ
$ sudo cp <path/to/grubx64.efi> bootx64.efi.patched  # 入手したパッチ済みのGRUBファイルをコピー
$ sudo cp bootx64.efi.patched bootx64.efi            # バグありのファイルをパッチ済みのもので置き換え
$ cd ~
$ sudo umount /tmp/esp                               # メモリディスクをアンマウント
$ sudo mdconfig -d -u md0                            # メモリディスクの設定を解除
```

**注**: FreeBSDで操作を行なう場合には、ESP (EFI System Partition)として、明示的にパーティション12を指定してマウントする必要があることに注意してください。Chromium OSのディスクパーティション構成については、以下の記事に説明があります。

- [Drive partitions (The Chromium Projects)](https://www.chromium.org/chromium-os/chromiumos-design-docs/disk-format#TOC-Drive-partitions)

### SSDへのインストール
イメージファイルの修正が完了し、[前回記事](/post/ezbook-x4-chromiumos-investigate/)で述べた二つの問題が解決されました。

さあ、待ちかねた実機でのブートです。

起動用のUSBメモリ作成、USBメモリからのブート、Googleアカウントでのサインインを行ないます。その後、ブラウザウィンドウで`Ctrl`+`Alt`+`T`を押下してcroshターミナルを起動し、以下のコマンドを実行すると内蔵SSDへのインストールが行なわれます。

**注: 内蔵SSDの既存データはすべて消去されますので、必要なファイルはあらかじめバックアップをお忘れなく!**

``` shell-session
crosh> install /dev/sda
```

なお、内蔵ドライブへのインストールに関する詳細が必要な場合は、たとえば以下の記事を参照してみてください。

- [Chromium OSをUSBで起動、そしてハードディスクへのインストールの手順 (Cloud-Work.net)](http://cloud-work.net/chromium-os/chromiumos_install/)
- [Chromium OSをネットブックのHDDにインストール (時候随想録)](https://memo.flexpromotion.com/index.php?p=7202)

### ESP (EFI System Partition)の修正
無事インストールできましたか?

でも再起動はちょっと待ってください。その前に**もうひと手間必要**です。

SSDへのインストール時に、SSD上のESPにはバグありのGRUBファイルがインストールされてしまいます。二度手間になりますが、再度これをパッチ済みのものに置き換えなおします。

まず、croshターミナルのプロンプトで`shell`と入力して、インタラクティブシェル環境(bash)を起動します。

``` shell-session
crosh> shell
```

シェルが起動したら、以下のコマンドを実行します。

``` shell
mkdir /tmp/esp
sudo mount /dev/sda12 /tmp/esp             # 内蔵SSDのパーティション12をマウント
cd /tmp/esp/efi/boot
sudo cp bootx64.efi.patched bootx64.efi    # バグありのGRUBファイルをパッチ済みのもので置き換え
cd /
sudo umount /tmp/esp
```

これでGRUBファイルが再びパッチ済みのものに置き換わりました。

ここでようやく再起動です。Chromium OSが起動して初期設定画面になるのを待ちましょう。その後、Googleアカウントでのサインインを行なえば通常どおり使えると思います。

では、自家製Chromebookを楽しみましょう!

### 参考文献
1. Chromium OS, https://www.chromium.org/chromium-os
1. Gentoo Linux, https://www.gentoo.org/
1. Portage, https://wiki.gentoo.org/wiki/Portage
1. USE flag, https://wiki.gentoo.org/wiki/USE_flag
1. Grub 2 issue on Intel N4200, https://medium.com/@pawitp/grub-2-issue-on-intel-n4200-97c12d4db8af
1. Grub for Chromium OS on Intel N4200, https://github.com/pawitp/grub2-chromiumos-n4200/
1. Drive partitions, https://www.chromium.org/chromium-os/chromiumos-design-docs/disk-format#TOC-Drive-partitions
1. Chromium OSをUSBで起動、そしてハードディスクへのインストールの手順, http://cloud-work.net/chromium-os/chromiumos_install/
1. Chromium OSをネットブックのHDDにインストール, https://memo.flexpromotion.com/index.php?p=7202
