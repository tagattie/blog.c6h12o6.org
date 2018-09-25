+++
title = "Nexus 6にAndroid 9 Pieをインストールする - 本編"
date = "2018-09-25T19:53:00+09:00"
categories = ["Android"]
tags = ["android", "pie", "nexus6", "carbonrom", "nitrogenos"]
+++

[前回の記事](/post/nexus6-android-pie-intro/)では、「Essential Phoneを購入」だとか「新Pixelがとうとう日本に上陸」だとか、Nexus 6に関係のない話ばかりしてしまいました。本記事では心を入れ替えて、Nexus 6にAndroid 9 Pieをインストールする手順を説明していきます。

前回記事の後半で少しふれましたが、2018年9月25日現在、Android 9 PieにアップデートされているNexus 6向けのカスタムROMには以下の二つがあります。

- [CarbonBeta (XDA Developers)](https://forum.xda-developers.com/nexus-6/orig-development/9-carbonbeta-cr-7-0-t3831917)
- [Nitrogen OS (XDA Developers)](https://forum.xda-developers.com/nexus-6/development/rom-nitrogen-os-n-substratum-11-10-2016-t3478365)

どちらをインストールするか少し迷ったのですが、本記事ではNitrogen OSをインストールすることにします。

**注**: CarbonBetaをインストールする場合でも、フラッシュするカスタムROM本体のファイル名が異なるだけで、手順そのものは変わりません。CarbonBetaを試してみようという場合には、適宜カスタムROMのファイル名を読み替えていただければと思います。

さて、Android 9 Pie対応のカスタムROMをNexus 6にフラッシュする具体的な手順は以下のとおりです。

- [必要なファイル一式のダウンロード](#ファイル一式のダウンロード)
- [ADB経由での母艦マシンと端末との接続セットアップ](#adb経由での接続セットアップ)
- [ブートローダのアンロック](#ブートローダのアンロック)
- [カスタムリカバリのフラッシュ](#カスタムリカバリのフラッシュ)
- [端末ストレージのフォーマット](#ストレージのフォーマット)
- [ファイル一式の端末への転送](#ファイル一式の転送)
- [ファイル一式のフラッシュ](#ファイル一式のフラッシュ)

以下で、上記手順について順番に説明していきます。なお、本手順はNexus 6に[Dirty Unicorns](https://dirtyunicorns.com/) (Android Oreo相当のカスタムROM)をインストールしたときと基本的には同じですので、こちらもあわせて参考にしていただければと思います。

- [Nexus 6にAndroid Oreo (8.1)をインストールする - ファイル準備編](/post/nexus6-android-du-download/)
- [FreeBSDからAndroid端末にADBで接続する](/post/freebsd-android-adb/)
- [Nexus 6にAndroid Oreo (8.1)をインストールする - ブートローダアンロック編](/post/nexus6-android-oemunlock/)
- [Nexus 6にAndroid Oreo (8.1)をインストールする - フラッシュ編](/post/nexus6-android-du-flash/)

## !!注意!!

**ブートローダのアンロック、カスタムリカバリのフラッシュ、およびカスタムROMのフラッシュはメーカの推奨しないソフトウェアの改造に相当します。したがって、これらの作業を行なった時点で修理や交換などのメーカ保証は一切受けられなくなります。また、万が一の場合に備え、作業を行なう前には必ずバックアップをとってください。下記手順は一切無保証です。最悪の場合、端末が起動しなくなるおそれがありますが、自己責任での作業をお願いします。**

### ファイル一式のダウンロード
カスタムROMをフラッシュするのに必要なファイルの一覧を下表に示します。カスタムROM本体については、インストールするほういずれか一方のみダウンロードすればOKです。それぞれのファイルの位置づけや役割については、[この記事](/post/nexus6-android-du-download/)をご参照ください。

|ソフトウェア種別|ソフトウェア名|ダウンロードURL|最新版ファイル名(2018年9月25日現在)|
|:---|:---|:---|:---|
|カスタム<br>リカバリ|TWRP|https://twrp.me/motorola/motorolanexus6.html|twrp-3.2.3-0-shamu.img|
|カスタム<br>ROM本体|CarbonBeta|https://dl.myself5.de/shamu/CarbonROM|CARBON-CR-7.0-TBA-UNOFFICIAL-shamu-20180905-1412.zip|
|カスタム<br>ROM本体|Nitrogen OS|https://sourceforge.net/projects/nitrogen-project/files/shamu/|Nitrogen-OS-P-shamu-20180914.zip|
|GApps<br>パッケージ|Open GApps|https://opengapps.org/|open_gapps-arm-9.0-stock-20180924.zip|
|Root化ツール|Magisk|https://github.com/topjohnwu/Magisk/releases/|Magisk-v17.2.zip|
|Root権限<br>マネージャ|Magisk Manager|https://github.com/topjohnwu/Magisk/releases/|MagiskManager-v6.0.0.apk|

三つのファイル(カスタムリカバリ、カスタムROM、およびGAppsパッケージ)をダウンロードして、一か所のディレクトリにまとめておきます。(端末のRoot化を行なう場合は、前記に加えてRoot化ツール、およびRoot権限マネージャもダウンロードしておきます。)

Nitrogen OSをインストールする場合で、同時にRoot化も行なう場合、必要なファイル一式は以下のとおりとなります。

``` shell-session
$ ls -1 <ファイル一式を格納したディレクトリ>
Magisk-v17.2.zip
MagiskManager-v6.0.0.apk
Nitrogen-OS-P-shamu-20180914.zip
open_gapps-arm-9.0-stock-20180924.zip
twrp-3.2.3-0-shamu.img
```

**注**: Open GAppsについて、Webサイトにアクセスするとわかりますが、パッケージにはいくつか種類(Stock, Full, Mini, など)があり、それぞれファイルサイズが異なります。カスタムROMの種類によっては、Open GAppsのフラッシュ時に**システム領域が不足してエラー**となる場合があります。そのような場合は、**よりサイズの小さな**Open GAppsパッケージを試してみることをおすすめします。

### ADB経由での接続セットアップ
次は、母艦端末からADB (Android Debug Bridge)経由でNexus 6に接続するための設定を行ないます。

母艦マシンがFreeBSDの場合は、[この記事](/post/freebsd-android-adb/)を参照して設定を行なってください。母艦マシンのOSがその他(Linux, macOS, あるいはWindows)の場合は、たとえば以下の記事を参考にして設定をお願いします。

- [Using ADB and fastboot (LineageOS Wiki)](https://wiki.lineageos.org/adb_fastboot_guide.html)
- [How to Install ADB on Windows, macOS, and Linux (XDA Developers)](https://www.xda-developers.com/install-adb-windows-macos-linux/)

### ブートローダのアンロック
ADB経由での接続が確認できたら、次はNexus 6のブートローダのロック解除(アンロック)を行ないます。

[この記事](/post/nexus6-android-oemunlock/)を参照してアンロックを行なってください。すでにアンロック済みの場合、本項の作業は不要です。

**注**: ブートローダのアンロックにより、端末が工場出荷状態にリセットされます。端末にインストールされているアプリや保存されている写真、音楽、動画などのファイルはすべて消去されますので、作業を行なう前には必ずバックアップをとってください。

### カスタムリカバリのフラッシュ
ブートローダのアンロックが済んだら、次はカスタムリカバリのフラッシュを行ないます。

[この記事](/post/nexus6-android-du-flash/)の項番1を参照してフラッシュを行なってください。

### ストレージのフォーマット
**注**: 本項は通常のカスタムROMフラッシュ手順とは異なります。

前回記事の最後でも述べましたが、CarbonBetaおよびNitrogen OSのAndroid 9 Pie対応版は、現時点では**端末ストレージの暗号化に未対応です**。したがって、端末ストレージをフォーマットしなおして、**暗号化を解除**する必要があります。

カスタムリカバリのフラッシュが終わったら、`fastboot reboot-bootloader`コマンドを実行して、Nexus 6をfastbootモードで起動しなおします。そして、ボリュームダウンボタンを2回押下します。すると、"START"と表示されていた部分が"RECOVERY MODE"になりますので、この状態で電源ボタンを押下してください。カスタムリカバリ(TWRP)が起動します。(下図左)

内蔵ストレージをフォーマットしなおすため、次の操作を行ないます。Wipeをタップし、次にFormat Data (下図中)をタップします。すると、本当にフォーマットを行なってよいかを確認する画面が現れます。OKの場合は、オンスクリーンキーボードを用いて`yes`と入力し、右下のチェックマークキーをタップします。(下図右)

|図左|図中|図右|
|:---:|:---:|:---:|
|![TWRP - メイン](/img/android/twrp-main.png)|![TWRP - ワイプ](/img/android/twrp-wipe.png)|![TWRP - フォーマット](/img/android/twrp-format.png)|

以上の操作によって、内蔵ストレージがフォーマットされ、ストレージの暗号化が解除されます。

もし、フォーマットの際にエラーが発生した場合は、もう一度fastbootモードでの起動、カスタムリカバリの起動を行ない、上記のフォーマット手順を実行しなおしてください。

### ファイル一式の転送
さあ、内蔵ストレージのフォーマットが終わりました。あともうひといきです。

次は、ダウンロードして一か所のディレクトリにまとめておいたファイルを端末へ転送します。(最後の一つはRoot化を行なう場合のみ。)

``` shell
cd <ファイル一式を格納したディレクトリ>
adb shell mkdir -p /sdcard/ROM
adb push Nitrogen-OS-P-shamu-20180914.zip      /sdcard/ROM
adb push open_gapps-arm-9.0-stock-20180924.zip /sdcard/ROM
adb push Magisk-v17.2.zip                      /sdcard/ROM
```

### ファイル一式のフラッシュ
最後はカスタムROMのフラッシュです。

[この記事](/post/nexus6-android-du-flash/)の項番4を参照してファイル一式のフラッシュを行なってください。

**注**: Open GAppsのフラッシュ時にエラーが発生する場合は、**よりサイズの小さな**Open GAppsパッケージをフラッシュしてみてください。

Root化を行なった場合は、最後に[この記事](/post/nexus6-android-du-flash/)の項番6を参照してMagisk Managerをインストールしておきましょう。

ちなみに、Nitrogen OSをフラッシュしたあとの端末情報は以下のようになりました。

![Nitrogen OS - 端末情報](/img/android/nitrogen-os-info.png)

では、Android 9 Pieを楽しんでください!

### 参考文献
1. CarbonBeta, https://forum.xda-developers.com/nexus-6/orig-development/9-carbonbeta-cr-7-0-t3831917
1. Nitrogen OS, https://forum.xda-developers.com/nexus-6/development/rom-nitrogen-os-n-substratum-11-10-2016-t3478365
1. Using ADB and fastboot, https://wiki.lineageos.org/adb_fastboot_guide.html
1. How to Install ADB on Windows, macOS, and Linux, https://www.xda-developers.com/install-adb-windows-macos-linux/
