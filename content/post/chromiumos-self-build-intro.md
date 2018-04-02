+++
title = "Chromium OSでバッテリ駆動時間は伸びるのか? NEC LaVie Zで試す - まえがき"
date = "2018-04-01T14:51:28+09:00"
draft = true
categories = ["ChromiumOS"]
tags = ["chromiumos", "chromeos", "nec", "lavie", "chromebook", "battery", "neverware", "cloudready"]
+++

2013年12月からNECのラップトップPC [Lavie G タイプZ](http://nec-lavie.jp/navigate/products/pc/133q/10/lavie/lvz/spec/pc-gl186y3az.html)を使っています。いまはLAVIE Hybrid ZEROと呼ばれている、超軽量ラップトップシリーズの二代目で、IGZO液晶搭載モデルです。一度、バッテリが極度に消耗して充電できなくなってしまったのですが、バッテリ交換サービスを経て息を吹き返しました。

主な用途は外出先での文書作成とインターネットブラウジングで、ごくたまにプログラミングにも使います。とにかく軽い(795g)ので持ち運びの負担も小さく、ほとんどいつも持ち歩いて愛用しています。

しかし、文書作成とWebブラウジングがほとんどであるにしては、バッテリの持ちが少々よくないと感じます。文書作成のみに集中しているときなら3~4時間くらいはもちますが、Webブラウズを連続して行なうと最悪1時間強くらいになってしまいます。カタログ公称値が9.2時間で、80%までしか充電せずに使っていることを考えると、妥当な数字なのかなという気もしますが…。

また、使っているアプリが比較的重い(Markdown文書の作成にAtom、WebブラウジングにChromeを使用)ことと、ネットワーク接続にVPN (Virtual Private Network)を使っていることも電池持ちを悪くしている要因かもしれません。

とにかく、もうちょっとバッテリを持たせる方法がないものかとネットを漁りまわっていたところ、Chromebookというものがあることに気がつきました。Chromebookの長所として必ずといっていいほど挙げられるのが、安価であること、起動が速いこと、常に最新のソフトウェアにアップデートされることなどに加え、**バッテリ駆動時間が長い**ということです。(例えば、以下の三記事)

- [Chromebookの特徴 (chromebooker)](https://chromebooker.net/aboutchromebook/feature)
- [まだ知らない人のためのChromebook（2015年7月更新版）(@IT)](http://www.atmarkit.co.jp/ait/articles/1402/26/news022.html)
- [4ヵ月間使ってわかったChromebookのホントのところ (週刊アスキー)](https://weekly.ascii.jp/elem/000/000/274/274120/)

ChromebookがWindows PCよりも、際立って容量の大きいバッテリを搭載しているとは考えにくいので、バッテリの持ちがよいのはハードウェアではなく、OSの違いによるところが大きいのではないかと思います。

自分のPC作業を振り返ってみると、文書作成とWebブラウジングがほとんどですので、どちらもChromebookで十分こなせる範囲です。しかも、Chrome OSのベースになっているのは、オープンソースのChromium OSです。[ソースからビルドする手順](https://www.chromium.org/chromium-os/developer-guide)も公開されています。だったら、自分でChromium OSをビルドして、LaVie Zで使ってみようと思い立ったのでした。

ちなみに、自分でビルドしなくても、[Neverware社](https://www.neverware.com/)からCloudReadyという、Chromium OSをベースにしたカスタムOSのビルドが提供されていますので、てっとり早く試したい場合はこちらを使うほうがよさそうです。

- [Thanks for choosing CloudReady: Home Edition (Neverware)](https://www.neverware.com/freedownload)

でも、ビルドもしてみたいので自分でビルドするのです。

さて、Chromium OSを使うことで本当にバッテリ駆動時間が伸びるのか、予想がつかないので不安ですが、興味を持ってくださったかたは次回以降もしばらくお付き合いいただけるとさいわいです。

### 参考文献
1. Chromebookの特徴, https://chromebooker.net/aboutchromebook/feature
1. まだ知らない人のためのChromebook（2015年7月更新版）, http://www.atmarkit.co.jp/ait/articles/1402/26/news022.html
1. 4ヵ月間使ってわかったChromebookのホントのところ, https://weekly.ascii.jp/elem/000/000/274/274120/
1. Chromium OS Developer Guide, https://www.chromium.org/chromium-os/developer-guide
1. Thanks for choosing CloudReady: Home Edition, https://www.neverware.com/freedownload
