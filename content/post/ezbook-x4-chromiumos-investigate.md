+++
title = "Jumper EZbook X4にChromium OSをインストールする(不具合調査編)"
date = "2019-01-18T14:19:47+09:00"
draft = true
categories = ["ChromiumOS"]
tags = ["chromiumos", "jumper", "ezbook", "install"]
+++

先日、といってももう2018年8月のことですが、[Jumper Computer Technology社](http://www.jumper.com.cn/)のEZbook X4というラップトップを[購入しました](/post/ezbook-x4-freebsd-intro/)。愛用していたNEC LaVie Zのバッテリ持ちがかなり悪くなってしまったので、その代替機という位置づけです。

LaVie Zでもそうしていたように、オープンソースの[Chromium OS](https://www.chromium.org/chromium-os)をビルドして、自家製Chromebook (正確にはChromiumbookとでもいうべきでしょうか)にして使うつもりでした。しかし、当時は(調査不足のため)USBメモリからの起動ができず、Chromebook化を一時的に断念せざるを得ませんでした。

なんとかEZbook X4でChromium OSが使えないものかと、その後も調査を続けていました。そんななか、Chromium OSのカスタマイズ版として有名な[Neverware社](https://www.neverware.com/)の[CloudReady](https://www.neverware.com/freedownload/)での起動を試してみたところ、ブートが成功し初期設定画面に進めることがわかりました。

こうなると、自前ビルドしたUSBメモリでの起動ができないのは、ビルド段階で何らかのミスをしている、あるいはビルド手順に何か過不足がある可能性が高くなってきます。ブート実績ができたことに断然勇気づけられて、さらに調査を進めました。その結果、EZbook X4で自前ビルドのChromium OSを起動することに成功しました!!

本記事では、調査の結果判明した、起動に失敗していた原因を報告します。

なお、本記事(および次回記事)で述べる内容はEZbook X4のみで確認していますが、Intel社の[Gemini Lake世代CPU](https://ark.intel.com/products/codename/83915/Gemini-Lake)を搭載したマシンならば同様に適用できる可能性があります。(EZbook X4はGemini Lake世代のCeleron N4100を搭載)

さて、ブートできない原因は何だったのか、結論は以下の二点です。

- [ブートローダGRUBにバグがあった](#grubのバグ)
- [Gemini Lake CPUに必要なグラフィックスファームウェアがインストールされていなかった](#グラフィックスファームウェアのインストール不備)

### GRUBのバグ
GRUB (Grand Unified Boot Loader)は、主要なLinuxディストリビューションでOSの起動に用いられているブートローダであり、Chromium OSでも使用されます。ブートローダは、大まかにいうとBIOSやUEFIから制御を受け取った後に、実際にOS(カーネル)を起動する役割を持ちます。GRUBの詳細については以下の各記事をご参照ください。

- [GNU GRUB (Free Software Foundation)](https://www.gnu.org/software/grub/)
- [GNU GRUB (Wikipedia)](https://ja.wikipedia.org/wiki/GNU_GRUB)

GRUBの最新リリースは現在バージョン2.02ですが、本版にはバグがあります。Gemini Lake世代のCPUを搭載したマシンでは、GRUBが**フリーズしてしまい、OSを起動するに至りません**。以下の記事に本バグの詳しい説明があります。(記事では[Apollo Lake世代](https://ark.intel.com/products/codename/80644/Apollo-Lake)のPentium N4200で問題が発生するとのことですが、Gemini Lake世代のCeleron N4100でもまったく同様です。)

- [Grub 2 issue on Intel N4200 (Pawit Pornkitprasan)](https://medium.com/@pawitp/grub-2-issue-on-intel-n4200-97c12d4db8af)

さいわいなことに、本問題を解消するパッチがすでに存在し、GRUBのGitリポジトリに取り込まれています。したがって、リポジトリから先端のソースコードをチェックアウト、ビルドすることで解決可能です。上記記事の著者であるPawitさんのGitHubリポジトリにパッチ、ビルド手順、さらにビルド済みのファイルがまとめられていますので、次回記事では本リポジトリの内容を参考に作業を進めることにします。

- [Compile of Grub 2 compatible with N4200 CPU (GitHub / pawitp)](https://github.com/pawitp/grub2-chromiumos-n4200/)

### グラフィックスファームウェアのインストール不備
手もとでLinuxをお使いでしたら`/lib/firmware`以下をのぞいてみてください。さまざまなファイルが格納されていることがわかると思います。これらは[バイナリ・ブロブ](https://ja.wikipedia.org/wiki/%E3%83%90%E3%82%A4%E3%83%8A%E3%83%AA%E3%83%BB%E3%83%96%E3%83%AD%E3%83%96)なんです。メーカーがバイナリ形式のみでリリースしているファームウェアやマイクロコードがこのディレクトリにまとめられているのですね。

一般的なLinuxディストリビューションには`linux-firmware`というパッケージがあり、OSのインストール時に本パッケージもインストールされます。これにより、特定のハードウェアの動作にバイナリ・ブロブが必要であることをユーザが意識することは通常ありません。各Linuxディストロの`linux-firmware`パッケージについては、たとえば以下を参照してください。

- [linux-firmwareパッケージ (Ubuntu)](https://packages.ubuntu.com/search?keywords=linux-firmware)
- [linux-firmwareパッケージ (Gentoo)](https://packages.gentoo.org/packages/sys-kernel/linux-firmware)

ちなみに、上記パッケージのおおもととなるファイル群は、以下のGitリポジトリで管理されています。

- [linux-firmwareリポジトリ (kernel.org)](https://git.kernel.org/pub/scm/linux/kernel/git/firmware/linux-firmware.git/)

一般的なLinuxディストリビューションでは、インストール対象となるマシンのハードウェア構成は事前にわかりません。構成に含まれるパーツにはさまざまなものがありえます。したがって、`linux-firmware`パッケージを作成・管理する立場としては、多様なハードウェアに対応できるよう、可能な限り多くのバイナリ・ブロブを含めることになります。

いっぽう、Chrome OSはChromebookの機種ごとにカスタマイズしてビルドされます。**ハードウェア構成は事前にわかっている**ので、**本構成のみを満たせばOK**です。したがって、バイナリ・ブロブについても必要最小限のファイルのみをインストールする、という考え方になっているようです。

EZbook X4はChromebookではありませんので、Intel/AMD 64bit CPU搭載機向けの一般的なハードウェア(ボード)定義`amd64-generic`を用いてビルドを行なうのですが、**この指定だけでは必要なバイナリ・ブロブがインストールされない**ということがわかりました。(Gemini Lake世代CPUでは、内蔵グラフィックスハードウェアの一部機能の使用にバイナリ・ブロブが必要ですが、これがインストールされません。)

本問題への対応は、「必要なバイナリ・ブロブがインストールされるようビルド時に指定を追加する」ことです。具体的な指定内容については次回記事で述べます。

以上、EZbook X4でChromium OSが起動しなかった原因が判明しました。次回記事では、本調査結果をふまえたビルド手順、およびインストール手順を説明します。

### 参考文献
1. GNU GRUB, https://www.gnu.org/software/grub/
1. GNU GRUB, https://ja.wikipedia.org/wiki/GNU_GRUB
1. Grub 2 issue on Intel N4200, https://medium.com/@pawitp/grub-2-issue-on-intel-n4200-97c12d4db8af
1. Compile of Grub 2 compatible with N4200 CPU, https://github.com/pawitp/grub2-chromiumos-n4200/
1. バイナリ・ブロブ, https://ja.wikipedia.org/wiki/%E3%83%90%E3%82%A4%E3%83%8A%E3%83%AA%E3%83%BB%E3%83%96%E3%83%AD%E3%83%96
