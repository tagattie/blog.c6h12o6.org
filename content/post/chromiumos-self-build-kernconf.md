+++
title = "Chromium OSでバッテリ駆動時間は伸びるのか? NEC LaVie Zで試す - カーネルコンフィグ編"
date = "2018-04-08T21:39:00+09:00"
categories = ["ChromiumOS"]
tags = ["chromiumos", "chromeos", "nec", "lavie", "chromebook", "battery", "ubuntu", "linux", "kernel", "config"]
+++

[まえがき](/post/chromiumos-self-build-intro/)から[ビルド実行編](/post/chromiumos-self-build-build/)にわたって、Chromium OSを自分でビルドして、NEC LaVie Zで使ってみようと思い立ったきっかけから、実際に起動するまでの流れを紹介しました。[Chromium OS Developer Guide](https://www.chromium.org/chromium-os/developer-guide)に詳しく手順が紹介されているので、時間はかかったものの、すんなりビルドが完了してUSBメモリから起動できました。

以下の記事を見て、内蔵WiFiが使えないのではないかと心配していましたが、杞憂に終わったようで問題なく使えます。

- [無料で作るChromebook！インストールの落とし穴……内蔵ネットワークが使用不可で思わぬ出費も。(makkyon web)](https://www.makkyon.com/2015/05/04/chromebook-inspiron11z/)
- [Chrome OSを超低スペのPCにインストールしたらこうなった (LifeEdge パソコン、WEB、スマホ、ガジェットなどの総合情報サイト)](https://it-media2.net/chromeos/)

幸運にも、LaVie Zと`stabilize-10443.B`ブランチの組み合わせでは、内蔵デバイスが動作しないという問題は起きませんでした。しかし、機種とブランチの組み合わせによっては、やはりデバイスが認識されない、動作しないという問題が出ると思います。

デバイスが動作しない原因としては、デバイスに対応するドライバがない、対応するドライバがビルドされない、などが考えられます。ドライバがない場合には手の打ちようがありませんが、ドライバがビルドされない場合については、Linuxカーネルのコンフィグレーションを調整することで問題が解決できる可能性があります。

そこで本記事では、ドライバがビルドされない場合を想定して、Linuxカーネルのコンフィグレーションを編集する手順を紹介します。(カーネルコンフィグレーションを編集することで、ビルドするドライバを追加、削除したり、カーネルオプションを調整したりできます。) 以下の四つの記事を参考にします。

- [Chromium OS Board Porting Guide (The Chromium Projects)](https://www.chromium.org/chromium-os/how-tos-and-troubleshooting/chromiumos-board-porting-guide)
- [Simple developer workflow (The Chromium Projects)](https://www.chromium.org/chromium-os/developer-guide/developer-workflow)
- [Kernel Configuration (The Chromium Projects)](https://www.chromium.org/chromium-os/how-tos-and-troubleshooting/kernel-configuration)
- [Chromium OSのカーネルをVAIO Type P向けに再構築する (PCメモ)](http://pcmemo.take-uma.net/chromium%20os/kernel_config_vaiop)

以下の手順を行なう前に、[ビルド環境のセットアップ](/post/chromiumos-self-build-build/#ビルド環境のセットアップ)までを終わらせておきます。

### カーネルバージョンとコンフィグ名の確認
カーネルのコンフィグレーションを編集する前に、いくつか確認しておくべきことがあります。

まず、対象のボード(本記事では`amd64-generic`を想定)向けにビルドされる、カーネルのバージョンを確認しましょう。Chromium OSのソースコードには複数バージョンのカーネルが含まれています。次のコマンドを実行してみてください。

``` shell-session
(cr) ~/trunk/src/scripts $ ls ../third_party/kernel
experimental  v3.10  v3.14  v3.18  v3.8  v4.14  v4.4
```

3.8系、3.10系、3.14系、3.18系、4.4系、および4.14系と、6系統ものバージョンが含まれていることがわかります。以下のコマンドを実行して、`amd64-generic`ボードの場合にビルドされるカーネルバージョンを確認します。

``` shell-session
(cr) ~/trunk/src/scripts $ emerge-${BOARD} -pv virtual/linux-sources
(snip)
[ebuild  N     ] sys-kernel/chromeos-kernel-4_4-4.4.117-r1384::chromiumos to /build/amd64-generic/ USE="(snip)
(snip)
```

バージョンは4.4系であることがわかりました。

次に、どのカーネルコンフィグファイルを編集すればよいのかを調べます。[Kernel Configuration](http://www.chromium.org/chromium-os/how-tos-and-troubleshooting/kernel-configuration)の冒頭の図のように、Chromium OSではファミリー(Family)、アーキテクチャ(Architecture)、およびフレーバー(Flavour)という階層構造を用いて、カーネルのコンフィグレーションを管理しています。ボード一つに対して一つの完全なコンフィグファイルを用意する代わりに、ファイルを分割して共通部分をくくりだし、管理コストの削減を図っているわけですね。

特定の機種(ボード)に対して、ビルドするドライバの追加・削除などを行なう場合には、基本的にフレーバーに対応するファイルを編集することになります。

`amd64-generic`ボードに対応するフレーバーを調べてみましょう。[Chromium OS Board Porting Guide](https://www.chromium.org/chromium-os/how-tos-and-troubleshooting/chromiumos-board-porting-guide)によると、`CHROMEOS_KERNEL_SPLITCONFIG`という設定値にカーネルコンフィグレーション名(フレーバー)が格納されているようです。この値は、`overlay-${BOARD}/profiles/base/make.defaults`というファイルに記述があります。

以下のコマンドを実行して`CHROMEOS_KERNEL_SPLITCONFIG`の値を確認します。`amd64-generic`ボードの場合は`chromiumos-x86_64`であることがわかりました。

``` shell-session
(cr) ~/trunk/src/scripts $ grep CHROMEOS_KERNEL_SPLITCONFIG ../overlays/overlay-amd64-generic/profiles/base/make.defaults
CHROMEOS_KERNEL_SPLITCONFIG="chromiumos-x86_64"
```

以上で、カーネルバーションとコンフィグファイル名がわかりました。次は、実際にコンフィグファイルを編集します。

### カーネルパッケージの編集宣言
カーネルコンフィグレーションを編集する前に、「カーネルパッケージを編集する」という宣言をします。`amd64-generic`ボード向けには4.4系のカーネルがビルドされることがわかっていますので、`sys-kernel/chromeos-kernel-4_4`を編集状態にします。(参考: [Simple developer workflow](https://www.chromium.org/chromium-os/developer-guide/developer-workflow))

``` shell-session
(cr) ~/trunk/src/scripts $ cros_workon --board=${BOARD} start sys-kernel/chromeos-kernel-4_4
21:22:17: INFO: Started working on 'sys-kernel/chromeos-kernel-4_4' for 'amd64-generic'
```

### カーネルコンフィグレーションの編集
カーネルソースのディレクトリに移動して、以下のコマンドを実行します。先ほど確認した`chromiumos-x86_64`というフレーバー以外のコンフィグレーションについても編集するかを尋ねられますが、`chromiumsos-x86_64`**以外**については`s`で編集をスキップします。

``` shell-session
(cr) ~/trunk/src/scripts $ cd ../third_party/kernel/v4.4
(cr) ~/trunk/src/third_party/kernel/v4.4 $ ./chromeos/scripts/prepareconfig chromiumos-x86_64
(cr) ~/trunk/src/third_party/kernel/v4.4 $ ./chromeos/scripts/kernelconfig editconfig
running editconfig for x86_64 i386 armel arm64


***************************************
* Processing x86_64 (x86_64) ... 
* x86_64/chromeos-amd-stoneyridge.flavour.config: press <Enter> to edit, S to skip    # sでスキップ
(snip)
* x86_64/chromiumos-x86_64.flavour.config: press <Enter> to edit, S to skip           # エンターで編集
```

目当ての`chromiumos-x86_64`フレーバーでエンターキーを押すと、おなじみ(?)の`make menuconfig`時の画面になりますので、ドライバの追加などの必要な変更を行なってください。

![Chromium OS - カーネル - menuconfig](/img/chromiumos/chromiumos-kernel-reconfig.png)

変更が終わったら以下のコマンドを実行します。

``` shell-session
(cr) ~/trunk/src/third_party/kernel/v4.4 $ make mrproper
  CLEAN   .config
(cr) ~/trunk/src/third_party/kernel/v4.4 $ cd ../../../scripts
```

以上でカーネルコンフィグレーションの編集は終了です。

あとは、ビルド実行編の[コンソールユーザのパスワード設定](/post/chromiumos-self-build-build/#コンソールユーザのパスワード設定)に戻って、以降の作業を行なえばOKです。

### 参考文献
1. Chromium OS Developer Guide, https://www.chromium.org/chromium-os/developer-guide
1. 無料で作るChromebook！インストールの落とし穴……内蔵ネットワークが使用不可で思わぬ出費も。, https://www.makkyon.com/2015/05/04/chromebook-inspiron11z/
1. Chrome OSを超低スペのPCにインストールしたらこうなった, https://it-media2.net/chromeos/
1. Chromium OS Board Porting Guide, https://www.chromium.org/chromium-os/how-tos-and-troubleshooting/chromiumos-board-porting-guide
1. Simple developer workflow (The Chromium Projects, https://www.chromium.org/chromium-os/developer-guide/developer-workflow
1. Kernel Configuration, https://www.chromium.org/chromium-os/how-tos-and-troubleshooting/kernel-configuration
1. Chromium OSのカーネルをVAIO Type P向けに再構築する, http://pcmemo.take-uma.net/chromium%20os/kernel_config_vaiop
