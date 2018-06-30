+++
title = "ローカルのChromiumソースを使ってChromium OSをビルドする"
date = "2018-06-30T21:41:00+09:00"
categories = ["ChromiumOS"]
tags = ["chromiumos", "chromeos", "chromebook", "chromium", "chrome", "local", "source", "nec", "lavie"]
+++

以前、[Chromium OS (Chrome OS)](https://www.chromium.org/chromium-os)をソースコードからビルドして、NEC LaVie Zで使う、というシリーズ記事を書きました。([まえがき](/post/chromiumos-self-build-intro/)、[ビルド用VM構築編](/post/chromiumos-self-build-vm/)、[ビルド環境設定編](/post/chromiumos-self-build-env/)、[ビルド実行編](/post/chromiumos-self-build-build/)、[カーネルコンフィグ編](/post/chromiumos-self-build-kernconf/)、および[ベンチマーク編](/post/chromiumos-self-build-bench/))

LaVie ZはもともとWindowsラップトップですが、Chromium OSを使い出してからはWindowsの出番が少なくなりました。

USBメモリから起動しますので、Chromium OSをバージョンアップするたびにUSBメモリを作成しなおしていますが、それほど苦になりません。起動後すぐに以前の環境がほぼ再現されますので、再設定を行なうコストが小さいためです。Chromium OS、とても気に入って使っています。

ところが、最近トラブルに見舞われました。

最新の安定版であるR67 (`stabilize-10575.54.B`ブランチ)をビルドしようとしたのですが、Chromium (Chrome)のソースコードを取得するステップでエラーが発生し、ビルドに失敗します。何度かトライしてはみるものの状況は改善しません。

デフォルト設定ではビルドのつど、Chromiumのソースをサーバから取得するようになっていますが、これだけで1時間以上かかりますのでエラーになるとダメージが大きいです。

なんとかならないかと調査した結果、「Chromiumのソースを事前に取得しておき、Chromium OSのビルド時に(サーバから取得する代わりに)取得しておいたソースを使う」という方法があることがわかりました。

- [Build Chromium for Chromium OS and Deploy to real device (GitHub / ds-hwang)](https://github.com/ds-hwang/wiki/wiki/Build-Chromium-for-Chromium-OS-and-Deploy-to-real-device)

この方法であれば、Chromiumのソースを事前に取得する段階で、エラーがあれば解決を模索できます。さらに、Chromium OSのビルド時にいちいちChromiumのソースを取得せずに済むので、時間的にもネットワーク資源的にも効率化が期待できます。

そういうわけで、前置きがたいへん長くなりましたが、本記事では、ローカルに格納したChromiumのソースコードを用いてChromium OSをビルドする手順を紹介します。

大まかな流れは以下のとおりです。基本は[ビルド実行編](/post/chromiumos-self-build-build/)で紹介した手順と同じですが、Chromiumのソースコード取得やビルド時にローカルソースを使うようにする設定などが加わっています。

- [Chromium OSのソースコードを取得](#chromium-osのソースコード取得)
- [取得したChromium OSのブランチに対応するChromiumのバージョンを確認](#chromiumのバージョン確認)
- [Chromiumのソースコードを取得](#chromiumのソースコード取得)
- [gclientの設定ファイルを更新(R68以降のビルド時のみ)](#gclientの設定更新-r68以降のみ)
- [Chromium OSのブランチに対応するChromiumのソースコードをチェックアウト](#chromiumの指定バージョンチェックアウト)
- [Chromium OSのビルド時にローカルのChromiumソースコードが使用されるよう設定](#chromium-osのビルド準備)
- [Chromium OSのビルド](#chromium-osのビルド実行)


### Chromium OSのソースコード取得
まず、ビルドするChromium OSのブランチを決めてください。本記事では`release-R67-10575.B`ブランチをビルドしますが、これはお好みに合わせて読み替えをお願いします。

注: 最新のstableチャネルリリースに最も近いのは`stabilize-10575.54.B`ですが、本バージョンはセキュリティ的に問題があることがわかっていますので、これよりやや新しい`release-R67-10575.B`ブランチをビルドすることにしました。

Chromium OSには、大まかにいってmaster, release, stabilizeという三種類のブランチがあります。masterは開発の先端ブランチ、releaseは次期リリースの準備を行なうためのブランチ、stabilizeはリリース後のメンテナンスを行なうブランチ、という具合ではないかと思います。

では、Chromium OSのソースコード一式をチェックアウトしましょう。(参考: [Chromium OS Developer Guide / Get the Source](https://chromium.googlesource.com/chromiumos/docs/+/master/developer_guide.md#Get-the-Source))

``` shell
export BRANCH=release-R67-10575.B
cd ~
mkdir chromiumos-${BRANCH}
cd chromiumos-${BRANCH}
repo init -u https://chromium.googlesource.com/chromiumos/manifest.git --repo-url https://chromium.googlesource.com/external/repo.git -b ${BRANCH}
repo sync -j 4
```

### Chromiumのバージョン確認
たいへん長い時間がかかりましたが、Chromium OSのソース一式のチェックアウトが終わりました。次は、チェックアウトしたChromium OSのブランチに対応するChromiumのバージョンを確認します。

以下のコマンドを実行してください。すると、Chromium OSにおいてChromiumをビルドする際に使われる、ebuildファイルなどの一覧が得られます。

``` shell-session
$ ls -l ./src/third_party/chromiumos-overlay/chromeos-base/chromeos-chrome
total 156
-rw-r--r-- 1 example example 46664 Jun 29 06:37 chromeos-chrome-67.0.3396.0-r1.ebuild
-rw-r--r-- 1 example example 46046 Jun 29 06:37 chromeos-chrome-67.0.3396.105_rc-r1.ebuild
-rw-r--r-- 1 example example 46047 Jun 29 06:37 chromeos-chrome-9999.ebuild
drwxr-xr-x 2 example example  4096 Jun 29 06:37 files
-rw-r--r-- 1 example example  3245 Jun 29 06:37 Manifest
-rw-r--r-- 1 example example  2185 Jun 29 06:37 metadata.xml
```

上記の例では、

- `chromeos-chrome-67.0.3396.0-r1.ebuild`
- `chromeos-chrome-67.0.3396.105_rc-r1.ebuild`
- `chromeos-chrome-9999.ebuild`

という三つのebuildファイルがあることがわかりました。このうち、実際にビルドに使用されるのは「9999を除いてもっとも大きなバージョン番号」です。つまり、この例では**`67.0.3396.105`**が対応するChromiumのバージョンとなります。

この番号を覚えておきましょう。

### Chromiumのソースコード取得
Chromiumのバージョン番号がわかりましたので、次はChromiumのソースコードを取得します。

ホームディレクトリに戻って、Chromiumのソースコードを格納するディレクトリを作成し、そこにソースをチェックアウトします。(参考: [Checking out and building Chromium on Linux / Get the code](https://chromium.googlesource.com/chromium/src/+/master/docs/linux_build_instructions.md#Get-the-code))

``` shell
cd ~
mkdir chromium
cd chromium
fetch --nohooks chromium
cd src
./build/install-build-deps.sh
```

Chromiumソースをチェックアウトしたら、その中に含まれているスクリプトを実行して、ビルドに必要な依存パッケージもインストールしておきましょう。

### gclientの設定更新(R68以降のみ)
**注: 本節の手順はR68以降のChromium OSをビルドする場合にのみ行なってください。**

前節の参考に挙げた記事のタイトルのとおり、上記のコマンドを実行した段階では、Linux上で動作するChromiumをビルドするの必要なファイル一式がチェックアウトされた状態になっています。

Chromium OS向けのChromiumをビルドするためには、さらに追加の依存ファイルを取得する必要があります。次節において`gclient sync`コマンドを実行しますが、この際に必要な依存関係が取得されるよう、gclientコマンドの設定ファイルに一部追記します。(参考: [Chrome OS Build Instructions (Chromium OS on Linux) / Updating your gclient config](https://chromium.googlesource.com/chromium/src/+/master/docs/chromeos_build_instructions.md#updating-your-gclient-config))

カレントディレクトリの親ディレクトリに`.gclient`という名前のファイルがあると思いますので、エディタなどを使用して本ファイルの末尾に次の一行を書き加えてください。(すでにこの行が存在する場合はファイル修正の必要はありません。)

``` python
target_os = ['chromeos']
```

### Chromiumの指定バージョンチェックアウト
前々節でChromiumのソースコードをチェックアウトした段階では、masterブランチ、すなわち開発の先端バージョンのソースコードが取得された状態になっています。そこで、先ほど調べておいたバージョンのソースコードを改めてチェックアウトします。(参考: [Working with Release Branches / Syncing and building a release tag](https://www.chromium.org/developers/how-tos/get-the-code/working-with-release-branches#TOC-Syncing-and-building-a-release-tag))

バージョン番号を指定してチェックアウトを行ないます。なお、チェックアウトの際のブランチ名に関して、以下の例では`branch-<バージョン番号>`としていますが、これは任意の名前でかまいません。

``` shell
git fetch --tags
git checkout -b branch-67.0.3396.105 tags/67.0.3396.105
gclient sync --with_branch_heads --with_tags --reset
```

### Chromium OSのビルド準備
所望するバージョンのChromiumソースコードがチェックアウトできました。

では、Chromium OSのソースディレクトリに戻りましょう。そして、Chromium OSをビルドするためのchroot環境であるChromium OS SDK (CrOS SDK)を起動します。(参考: [Chromium OS Developer Guide / Building Chromium OS](https://chromium.googlesource.com/chromiumos/docs/+/master/developer_guide.md#Building-Chromium-OS))

このとき、`--chrome_root`オプションを指定して、Chromiumのソースコードをチェックアウトしたディレクトリのパスをchroot環境に渡すのを忘れないようにします。

``` shell
cd ~/chromiumos-${BRANCH}
cros_sdk --enter --chrome_root=${HOME}/chromium    # Chromiumのソースディレクトリを--chrome_rootオプションとして渡す
```

以降の操作はCrOS SDK上で行ないます。

ビルド対象ボードの設定、ビルド環境のセットアップに加え、Chromiumのビルド時にローカルソースが使われるよう`cros_workon`コマンドの実行、および環境変数の設定を行ないます。

``` shell
export BOARD=amd64-generic
./setup_board --board=${BOARD}
cros_workon --board=${BOARD} start chromeos-base/chromeos-chrome    # ローカルのChromiumソースが使われるようにするおまじない
export CHROME_ORIGIN=LOCAL_SOURCE                                   # CHROME_ORIGIN環境変数としてLOCAL_SOURCEを指定
export USE="-kernel-4_4 kernel-4_14 chrome_media"
```

注: `USE`環境変数を用いて、以下に示す二つの設定をさらに行なっています。

- Linux Kernel 4.4系の代わりに4.14系を使用(`-kernel-4_4 kernel-4_14`)  
(4.4系の場合、ビルドには成功するもののLaVie Zではブートしなかったため)
- 追加のメディアコーデックをビルド(`chrome_media`)

### Chromium OSのビルド実行
ここまでくれば、あとはいつものとおりビルドを実行すればOKです。相変わらずビルドには非常に長時間を要しますが、気長に終わるのを待ちましょう。

``` shell
./build_packages --board=${BOARD}
```

### 参考文献
1. Build Chromium for Chromium OS and Deploy to real device, https://github.com/ds-hwang/wiki/wiki/Build-Chromium-for-Chromium-OS-and-Deploy-to-real-device
1. Chromium OS Developer Guide, https://chromium.googlesource.com/chromiumos/docs/+/master/developer_guide.md
1. Checking out and building Chromium on Linux, https://chromium.googlesource.com/chromium/src/+/master/docs/linux_build_instructions.md
1. Working with Release Branches, https://www.chromium.org/developers/how-tos/get-the-code/working-with-release-branches
