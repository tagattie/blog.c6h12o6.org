+++
title = "Jumper EZbook X4にFreeBSD 11.2-RELEASEをカスタムインストールする(まえがき)"
date = "2018-09-08T18:05:44+09:00"
draft = true
categories = ["FreeBSD"]
tags = ["freebsd", "jumper", "ezbook", "install"]
+++

中国・深圳にあるメーカー、[Jumper Computer Technology (中柏电脑技术)社](http://www.jumper.com.cn/)のEZbook X4というラップトップPCを入手しました。[Gemini Lake世代](https://ark.intel.com/products/codename/83915/Gemini-Lake)の[Celeron N4100](https://ark.intel.com/products/128983/Intel-Celeron-N4100-Processor-4M-Cache-up-to-2_40-GHz)を搭載した14インチのノートです。長年お世話になってきた、現在Chromium OSマシンとして使用中のNEC LaVie Zの電池持ちがだいぶん悪くなってきましたので、これの代替機という位置づけです。

ちなみに、購入したショップは[GearBest](https://www.gearbest.com/)です。わたしが買ったときは33,600円くらいでしたが、いまはもう少し安くなっているようです。本製品にはディスプレイにTN液晶を使用したものとIPS液晶のものがありますが、IPS液晶版を購入しました。

- [JUMPER EZbook X4 Laptop 14.0 inch IPS Screen - SILVER (GearBest)](https://www.gearbest.com/laptops/pp_009340600116.html)

本機のスペックや使用感などについては、たとえば以下のサイトに詳しい実機レビューが掲載されていますので、興味のあるかたはご参照ください。(ただし、レビュー対象機はいずれもTN液晶版のようです。)

- [EzBook X4実機レビュー：Celeron N4100搭載で順当進化した14型ノート (PASOJU)](https://pasoju.com/jumper-ezbook-x4-review/)
- [Jumper EZBook X4 実機レビュー – 3万円ちょっとで買えるハイコスパノートPC (Till0196のぼーびろく)](https://till0196.com/post3510)

さて、わたしの最大の興味はChromium OSを動かすことにあったわけですが、最新の安定版であるR68では残念ながらブートできませんでした…。がっかり(泣)。

しかし、せっかく買ったものを使わずに放置しておくのは悔しいので、いったんChromium OSをあきらめてFreeBSDをインストールしてみることにしました。Windowsを使うのでもかまわないのですが、ラップトップでFreeBSDというのも面白いですよね(虚勢)。

そういうわけで、今回はEZbook X4にFreeBSDをカスタムインストールする手順を紹介します。でも、カスタムインストールの前に、まずは通常のインストールを試してみました。Chromium OSがブートしなかったので不安でしたが、FreeBSD 11.2-RELEASEの場合、USBメモリでの起動からガイドにそったインストールまで、あっけないくらい簡単に終わりました。(さすがはFreeBSDですね!)

ガイドにそった通常のインストールには、個人的に一点だけ不満があります。それは、インストール先のファイルシステムとしてZFSを選択(Root on ZFS)すると、ファイルシステムの構成がカスタマイズできないということです。

Root on ZFSかつ自由なファイルシステム構成でインストールするには、マニュアルでの操作が必要になります。マニュアル操作というとなんだか難しそうな気がしますが、以下の記事でこの手順がていねいに解説されています。(やはり、自由なファイルシステム構成でインストールしたいという動機から作成された記事のようです。)

- [FreeBSD 11.1-RELEASEを自由なZFSパーティション構成でインストールする (クソゲ〜製作所)](https://decomo.info/wiki/freebsd/install/install_freebsd_11_1_by_manually_zfs_partitioning)

説明されているコマンドをそのまま実行していくだけでOKなようになっていますので、EZbookへのインストールについても基本的に上記記事をなぞっていきます。ただし、想定する環境が一部異なるなど、以下の二点で相違があります。

- UEFIブートのみを想定

	参考記事ではレガシーブートとUEFIブートの両方をサポートする構成となっています。いっぽう、EZbookへのインストールについてはUEFIブートのみを想定します。これにより、パーティション構成やブートコードの書き込み手順が異なります。

- リモートからSSHログインしての作業を想定

	マニュアルでの一連の操作をコンソールですべて行なうのは面倒ですし、タイプミスも心配です。そこで、リモートからSSHログインしてインストール作業を行ないます。実行するコマンドをあらかじめテキストファイルに書き出しておいて、コピペしてコマンドを実行することでタイプミスもなくせます。このため、リモートアクセスを可能にするための手順をインストール作業の前に追加しています。

まえがきがたいへん長くなってしまいました。まえがきはこのくらいにして、次回の記事では実際のインストール手順について紹介していきます。

**追伸**:  
マニュアル作業でインストールを完了したあとに、以下の記事を見つけました。

- [bsdinstallを使用したFreeBSDの自動インストールについて (Qiita /@kunst1080)](https://qiita.com/kunst1080/items/80829f2d4e6478831b28)

FreeBSD 9.0-RELEASEからベースシステムに組み込まれている`bsdinstall(8)`コマンドを活用すると、スクリプトによってインストール内容を柔軟にカスタマイズできるというものです。なんと、ZFS on Root + 自由なファイルシステム構成でのインストールを行なうこともできるようです。(これを覚えておいて、次回新しいマシンにインストールするときは本コマンドを活用しようと思います。)

### 参考文献
1. JUMPER EZbook X4 Laptop 14.0 inch IPS Screen - SILVER, https://www.gearbest.com/laptops/pp_009340600116.html
1. FreeBSD 11.1-RELEASEを自由なZFSパーティション構成でインストールする, https://decomo.info/wiki/freebsd/install/install_freebsd_11_1_by_manually_zfs_partitioning
1. bsdinstallを使用したFreeBSDの自動インストールについて, https://qiita.com/kunst1080/items/80829f2d4e6478831b28
