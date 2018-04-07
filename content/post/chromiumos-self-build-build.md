+++
title = "Chromium OSでバッテリ駆動時間は伸びるのか? NEC LaVie Zで試す - ビルド実行編"
date = "2018-04-01T14:51:28+09:00"
draft = true
categories = ["ChromiumOS"]
tags = ["chromiumos", "chromeos", "nec", "lavie", "chromebook", "battery", "ubuntu"]
+++

[ビルド環境設定編](/post/chromiumos-self-build-env/)では、Ubuntu 14.04上でChromium OSをビルドするための環境設定について説明しました。Google API利用の準備がなかなか大変でしたが、なんとか終えることができました。本記事では、いよいよChromium OSのソースコードを取得してビルドを行ない、起動のためのイメージをUSBメモリに書き込むまでの流れを見ていきます。

引き続き、[Chromium OS Developer Guide](https://www.chromium.org/chromium-os/developer-guide)に沿って作業を進めていきます。手順は大きく分けて以下の三つのパートからなります。

- [ソースコードの取得](#ソースコードの取得)
- [Chromium OSのビルド](#chromium-osのビルド)
- [起動用USBメモリの作成](#起動用usbメモリの作成)

### ソースコードの取得
まず、ソースコードを格納するためのディレクトリを作りましょう。

ご存知だと思いますが、Chrome OSには複数の[ソフトウェアチャネル(Stable, BetaおよびDev)](https://support.google.com/chromebook/answer/1086915)があります。いっぽう、Chromium OSの開発は、大別すると以下の三つのブランチ群を使って行なわれているようです。ソフトウェアチャネルとブランチ群の対応関係ははっきりとはわかりませんが、おおむね以下のようになっているのではないかと思います。

- master - 開発の先端ブランチ、Devチャネルよりも新しい、ビルドできないこともある
- release - stabilizeブランチより少し新しい、Betaチャネルに対応している?
- stabilize - Stableチャネルに対応している

本記事では、最新のstabilizeブランチをビルドします。(執筆時点での最新は`stabilize-10443.B`) Chromium OSソースコードのブランチ一覧は以下のURLで確認できます。

- https://chromium.googlesource.com/chromiumos/manifest/+refs

任意のディレクトリの下にソースコード格納用ディレクトリを作成します。ディレクトリ名は自由につけてかまいませんが、本記事では`chromiumos-<ブランチ名>`としています。作成したディレクトリに`cd`します。

``` shell
export BRANCH=stabilize-10443.B
cd ~                               # ホームディレクトリ以外でもかまいません
mkdir chromiumos-${BRANCH}
cd chromiumos-${BRANCH}
```

次に、リポジトリの初期化を行ないます。以下のコマンドを実行してください。[Gitの設定](/post/chromiumos-self-build-env/#gitの設定-developer-guide対応部分-http-commondatastorage-googleapis-com-chrome-infra-docs-flat-depot-tools-docs-html-depot-tools-tutorial-html-bootstrapping-configuration)で設定した名前やメールアドレスを含む、下記のようなメッセージが最終的に表示されればOKです。

``` shell-session
$ repo init -u https://chromium.googlesource.com/chromiumos/manifest.git --repo-url https://chromium.googlesource.com/external/repo.git -b ${BRANCH}
(snip)
Your identity is: Your Name <youraddress@example.com>
If you want to change this, please re-run 'repo init' with --config-name

repo has been initialized in /home/example/chromiumos-stabilize-10443.B
```

ソースコードをチェックアウトします。これにはかなり時間がかかります。下記のようなメッセージが最終的に表示されればOKです。

``` shell-session
$ repo sync -j 4
(snip)
Your sources have been sync'd successfully.
```

### Chromium OSのビルド
ソースコードがチェックアウトできたら、同じディレクトリで`cros_sdk`コマンドを実行します。これによって、Chromium OS SDK (chrootされたChromium OSのビルド環境)が起動します。初回起動時には必要なファイルのダウンロードや展開などが行なわれますので、しんぼう強くお待ちください。

``` shell-session
$ cros_sdk
[sudo] password for example: 
20:29:20: NOTICE: Mounted /home/example/chromiumos-stabilize-10443.B/chroot.img on chroot
20:29:20: NOTICE: Downloading SDK tarball...
(snip)
(cr) (stabilize-10443.B/(8e3381b...)) example@cros-build ~/trunk/src/scripts $
```

cros_sdk環境に移行すると、コマンドプロンプトが以下のように変化します。これによって、同環境での作業中であることがわかるようになっています。

```
(cr) (<ブランチ名>/(<GitのコミットID)) <ユーザ名>@<ホスト名> <カレントディレクトリ> $
```

プロンプトがかなり長いので、以下では`(cr) <カレントディレクトリ> $`のように省略して表記します。

#### ビルド対象ボードの設定
ここからは、cros_sdk環境での作業になります。

まず、Chromium OSをビルドする対象のターゲットハードウェアを設定します。

Chrome OSを搭載したChromebookが各社から発売されていますね。まとめてChromebookという名前で呼ばれますが、具体的なハードウェア構成(たとえば、CPUやメモリ容量、搭載するWiFiデバイスなど)は、それぞれの機種ごとに異なります。Chromium OSでは、具体的なハードウェア構成(あるいは、Chromebookの機種といってもいいでしょう)のことを「ボード」と呼びます。

下記のコマンドを実行してみてください。`overlay-<ボード名>`というディレクトリが多数存在することがわかると思います。

``` shell-session
(cr) ~/trunk/src/scripts $ ls ../overlays
```

ちなみに、ボード名とChromebookの機種の対応については[以下のURL](https://www.chromium.org/chromium-os/developer-information-for-chrome-os-devices)で確認できます。

- [Developer Information for Chrome OS Devices (The Chromium Projects)](https://www.chromium.org/chromium-os/developer-information-for-chrome-os-devices)

しかし、当然のことながら、Windows PCであるNEC LaVie Zに対応するボードは定義されていません。そこで、Intel CPU (64ビット)を搭載した一般的なPCのボード定義である`amd64-generic`に設定します。以下のコマンドを実行して`BOARD`環境変数を定義します。

``` shell-session
(cr) ~/trunk/src/scripts $ export BOARD=amd64-generic
```

注: Intel CPU (32ビット)搭載の一般的なPC向けボード定義`x86-generic`もありますが、[以下の記事](http://chromiumosde.gozaru.jp/20170826.html)によると、32ビット版Chromium OSの開発はすでに打ち切られており、ビルドできない可能性が高いようです。

- [2017.08.26 $[$重要：予告$]$ カスタムビルド配布終了のお知らせ (Chromium OS Custom Build)](http://chromiumosde.gozaru.jp/20170826.html)

#### ビルド環境のセットアップ
ターゲットとするボードを設定しましたので、そのボード向けのビルド環境をセットアップします。以下のコマンドを実行してください。コンパイラなどを含めた、`amd64-generic`向けのクロスビルド環境が構築されます。

``` shell-session
(cr) ~/trunk/src/scripts $ ./setup_board --board=${BOARD}
INFO    : Elapsed time (run_chroot_version_hooks): 0m0s
INFO    : Updating chroot
INFO    : Clearing shadow utils lockfiles under /
INFO    : Updating cross-compilers
(snip)
INFO    : Elapsed time (setup_board): 4m5s
Done!
The SYSROOT is: /build/amd64-generic
```

#### コンソールユーザのパスワード設定
通常、Chromium OSはGUI上で初期設定を行ない、その後の使用もすべてGUIベースになります。しかしながら、主に開発やデバッグ用途として、テキストベースのコンソールも用意されています。本コンソールへのログインには`chronos`というユーザを使います。ここで`chronos`ユーザのパスワードを設定します。

``` shell-session
(cr) ~/trunk/src/scripts $ ./set_shared_user_password.sh
Enter password for shared user account: Password set in /etc/shared_user_passwd.txt
```

#### ビルドの実行
ようやくビルドを行なうところまでこぎつけました。以下のコマンドを実行して、ターゲットボード向けにChromium OSをビルドします。たいへん長時間を要しますので、覚悟のうえでお待ちください。

``` shell-session
(cr) ~/trunk/src/scripts $ ./build_packages --board=${BOARD}
Chromium OS version information:
    CHROME_BASE=
    CHROME_BRANCH=66
    CHROME_VERSION=
    CHROMEOS_BUILD=10443
    CHROMEOS_BRANCH=4
    CHROMEOS_PATCH=2018_04_06_2231
    CHROMEOS_VERSION_STRING=10443.4.2018_04_06_2231
(snip)
Merge complete
Done
Builds complete
INFO    : Elapsed time (build_packages): 1281m3s
Done
```

#### USBメモリイメージの作成
ようやくビルドが完了しました。次は、USBメモリからChromium OSを起動するためのイメージを作成します。イメージには三つのタイプを指定することができます。(参考: [Build a disk image for your board](https://www.chromium.org/chromium-os/developer-guide#TOC-Build-a-disk-image-for-your-board))

- test - SSHでのリモートアクセスを受け付けるテスト用パッケージを含むイメージ
- dev - 開発者向けの追加パッケージを含むイメージ
- base - 上記のパッケージを含まない素のイメージ(Chrome OSにいちばん近い)

本記事ではdevイメージを作成します。

``` shell-session
(cr) ~/trunk/src/scripts $ ./build_image --board=${BOARD} --noenable_rootfs_verification dev
Chromium OS version information:
    CHROME_BASE=
    CHROME_BRANCH=66
    CHROME_VERSION=
    CHROMEOS_BUILD=10443
    CHROMEOS_BRANCH=4
    CHROMEOS_PATCH=2018_04_07_2129
    CHROMEOS_VERSION_STRING=10443.4.2018_04_07_2129
(snip)
INFO    : Done. Image(s) created in /mnt/host/source/src/build/images/amd64-generic/R66-10443.4.2018_04_07_2129-a1

INFO    : Developer image created as chromiumos_image.bin
INFO    : To copy the image to a USB key, use:
INFO    :   cros flash usb:// ../build/images/amd64-generic/R66-10443.4.2018_04_07_2129-a1/chromiumos_image.bin
INFO    : To convert it to a VM image, use:
INFO    :   ./image_to_vm.sh --from=../build/images/amd64-generic/R66-10443.4.2018_04_07_2129-a1 --board=amd64-generic 

INFO    : Elapsed time (build_image): 18m44s
```

以上で、Chromium OSのビルドは終了です。上記のメッセージにも出力されていますが、USBメモリイメージは`chromiumos_image.bin`というファイル名です。以下の場所に保存されています。

``` shell-session
(cr) ~/trunk/src/scripts $ ls ../build/images/amd64-generic/latest
boot.config           esp                   partition_script.sh
boot.desc             license_credits.html  umount_image.sh
chromiumos_image.bin  mount_image.sh        unpack_partitions.sh
config.txt            pack_partitions.sh    vmlinuz.bin
```

### 起動用USBメモリの作成
ここまでとても長かったですが、ようやくUSBメモリイメージが作成できました。次は、イメージをUSBメモリに書き込みます。

上記メッセージに出力されていますように、`cros`コマンドを使ってUSBメモリへの書き込みを行なうことができるのですが、ビルド環境をVM上に構築しましたので、そこからは直接USBメモリへアクセスすることができません。

そこで、SFTPを使ってイメージファイルをVMからFreeBSDマシンへ転送し、FreeBSD上でUSBメモリへの書き込みを行なうことにします。

``` shell-session
$ cd <任意のディレクトリ>
$ sftp <Ubuntu VMホスト名>
sftp> cd <ソース格納の親ディレクトリ>/chromiumos-stabilize-10443.B/src/build/images/amd64-generic/latest
sftp> get chromiumos_image.bin
Fetching /home/example/chromiumos-stabilize-10443.B/src/build/images/amd64-generic/R66-10443.4.2018_04_07_2129-a1/chromiumos_image.bin to chromiumos_image.bin
/home/example/chromiumos-stabilize-10443.B/s 100% 4252MB  94.5MB/s   00:45
sftp> exit
$ dd if=chromiumos_image.bin of=/dev/da0 bs=1m
```

以上でUSBメモリの作成は完了です。起動するか試してみることにします。

起動しました!

[![Chromium OS - ビルド情報](/img/chromiumos/chromiumos-build-info-small.png)](/img/chromiumos/chromiumos-build-info.png)

### 参考文献
1. Chromium OS Developer Guide, https://www.chromium.org/chromium-os/developer-guide
1. Stable、Beta、Dev チャンネルを切り替える, https://support.google.com/chromebook/answer/1086915
1. Developer Information for Chrome OS Devices, https://www.chromium.org/chromium-os/developer-information-for-chrome-os-devices
1. 2017.08.26 $[$重要：予告$]$ カスタムビルド配布終了のお知らせ, http://chromiumosde.gozaru.jp/20170826.html
