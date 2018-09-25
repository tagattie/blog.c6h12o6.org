+++
title = "Nexus 6にAndroid 9 Pieをインストールする - まえがき"
date = "2018-09-20T19:11:00+09:00"
lastmod = "2018-09-25T19:55:00+09:00"
categories = ["Android"]
tags = ["android", "pie", "nexus6"]
+++

2015年2月から最近まで、メインのスマートフォンとしてNexus 6を使っていました。三世代前のリファレンス機で、公式のセキュリティアップデートがすでに打ち切られてしまった旧機種です。古くはなりましたが処理性能的に不満はありませんので、バッテリ交換をしてもう少し延命しようと思っていました。

そんな中、Webサイトをいろいろと漁りまわっていると、[Essential Products社](https://www.essential.com/jp)のEssential Phone PH-1がだいぶ安くなっていることに気づきました。米国Amazonの今年のプライムデーセールでは[\$249.99まで値下げ](http://mochigadget.hatenablog.com/entry/Essential-Phone-PrimeDay)されていたそうです。現在(2018/9/20時点)でも、[米Amazonでは\$325.98](https://www.amazon.com/dp/B078SQ7GWK)で販売されていますね。

- [Essential Phone in Halo Gray – 128 GB Unlocked Titanium and Ceramic phone with Edge-to-Edge Display (Amazon.com)](https://www.amazon.com/dp/B078SQ7GWK)

Pixelシリーズが日本では発売されていない現状で、リファレンス機に最も近い存在といえるEssential Phoneは魅力的ですよね。そういうわけで、購入しました。ちなみに、購入したショップは[公式サイト](https://shop.essential.com/)です。本体、360カメラ、カメラケース、それにヘッドフォンの4点セットで、\$399 + \$32.11 (送料)の計\$431.11でした。

- [Essential Phone (Essential Products)](https://shop.essential.com/)

本体だけなら米Amazonのほうが安いのですが、Black Moonが欲しかった(米Amazonで安いのはHalo Grayのみ)のと、カメラとヘッドフォンもセットでお得な気がしたので公式サイトで購入したしだいです。~~在庫売りつくしセール的に値下げしているのだと思うので、現在でもこの価格で購入できるのではないかと思います~~。

**注**: 念のため公式販売サイトを確認したところ、2018年9月20日現在では$499 (4点セット価格)となっていました…。

そういうわけで、Essential Phone PH-1をメインのスマートフォンとして使い始めました。ただし、Nexus 6もまだまだ十分使えますので、サブ機として活躍してもらうつもりです。

といったところで興味深いニュースが飛び込んできました。三世代めにしてようやくPixelシリースが日本でも発売されそうだということです。

- [Google Pixel 3、日本市場で発売か (すまほん!!)](https://smhn.info/201809-google-pixel-3-rumors)

半信半疑でしたが、9月19日付でGoogleから正式な発表が出ました。最近Essential Phoneを購入したばかりの身としてはちょっと悔しい気もしますが、とにかくリファレンス機が日本でも再び入手しやすくなるということは喜ばしいことですね。

{{< twitter 1042216711175458816 >}}

なんだかハードウェアの話題ばかりですね。そろそろNexus 6向けAndroid 9 Pieのことについて書いていきます。

ご存知のとおり、Android 9 Pieは2018年8月に正式リリースとなりました。Essential Phoneは準リファレンス機といえる位置にいるだけあって、すでに手もとにある端末もPieに更新ずみです。しかし、Nexus 6はすでにサポート期間を過ぎていますので、当然ながら公式のアップデートはありません。

Nexus 6ではPieが使えるようになるかなあと思っていたところ、「Nexus 6とNexus 5X向けにAndroid Pieが移植」というニュースがXDA Developersで報じられました。

- [Google Nexus 6 and Nexus 5X get AOSP Android Pie ports (XDA Developers)](https://www.xda-developers.com/nexus-6-nexus-5x-aosp-android-pie/)

Nexus 6でAndroid Pieが使えるようになるのは(仮に使えるようになるとしても)だいぶん先だと思っていましたので、これはうれしいニュースです。勇躍して、さっそくXDA DevelopersのNexus 6関連フォーラムをのぞいてみました。すると、ざっと見た限りでは、以下の二つのカスタムROMがすでにAndroid Pieにアップデートされているようです。

- [$[$9$]$$[$UNOFFCIAL$]$$[$BETA$]$ CarbonBeta | cr-7.0 $[$shamu$]$ (XDA Developers)](https://forum.xda-developers.com/nexus-6/orig-development/9-carbonbeta-cr-7-0-t3831917)
- [$[$ROM$]$ ► $[$9.0.0_r7$]$ ► $[$shamu$]$ ► Nitrogen OS Pie ► Substratum ► (14.Sept.2018) (XDA Developers)](https://forum.xda-developers.com/nexus-6/development/rom-nitrogen-os-n-substratum-11-10-2016-t3478365)

いずれもベータ版あるいはテスト版という扱いですが、たしかに"9"や"Pie"という文字が見えます。

Android 9 Pieに対応したカスタムROMが見つかりましたので、次回の記事ではこれらを実際にインストールしてみたいと思います。ただし、上に挙げた両ROMには以下に示す制限がありますので、あらかじめご注意願います。

**注**: 上記の両ROMは、現時点で**ストレージの暗号化に対応していません**。したがって、インストールの前にストレージの**暗号化を解除**する必要があります。つまり、ストレージを**フォーマットし直す**必要があります。これにより、端末上に保存しているアプリ、写真、 動画などを含む**すべてのデータが消去**されます。必ずバックアップを取り、バックアップデータを別マシンに退避してから作業してください。

では、[次回の記事](/post/nexus6-android-pie-install/)でまたお会いしましょう。

### 参考文献
1. エッセンシャルフォン, https://www.essential.com/jp
1. Essential Phone PH-1を購入しました (Amazon.com プライムデー個人輸入), http://mochigadget.hatenablog.com/entry/Essential-Phone-PrimeDay
1. Essential Phone in Halo Gray – 128 GB Unlocked Titanium and Ceramic phone with Edge-to-Edge Display, https://www.amazon.com/dp/B078SQ7GWK
1. Essential Phone, https://shop.essential.com/
1. Google Pixel 3、日本市場で発売か, https://smhn.info/201809-google-pixel-3-rumors
1. Google Nexus 6 and Nexus 5X get AOSP Android Pie ports, https://www.xda-developers.com/nexus-6-nexus-5x-aosp-android-pie/
1. $[$9$]$$[$UNOFFCIAL$]$$[$BETA$]$ CarbonBeta | cr-7.0 $[$shamu$]$, https://forum.xda-developers.com/nexus-6/orig-development/9-carbonbeta-cr-7-0-t3831917
1. $[$ROM$]$ ► $[$9.0.0_r7$]$ ► $[$shamu$]$ ► Nitrogen OS Pie ► Substratum ► (14.Sept.2018), https://forum.xda-developers.com/nexus-6/development/rom-nitrogen-os-n-substratum-11-10-2016-t3478365
