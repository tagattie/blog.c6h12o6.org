+++
title = "Nexus 7 (2013)にAndroid Oreo (8.1)をインストールする"
date = "2018-03-04T22:09:00+09:00"
categories = ["Android"]
tags = ["android", "oreo", "nexus7", "aicp", "freebsd"]
+++

先日、Nexus 6にAndroid Oreo (8.1)をインストールする手順([ファイル準備](/post/nexus6-android-du-download/)、[ブートローダアンロック](/post/nexus6-android-oemunlock/)、および[フラッシュ](/post/nexus6-android-du-flash/))を紹介しました。この際にインストールしたのは、[Dirty Unicorns](https://dirtyunicorns.com/)というカスタムROMです。もう一つ、[AICP (Android Ice Cold Project)](http://aicp-rom.com/)というカスタムROMも名前を挙げましたが、こちらは試していませんでした。

[AICPのダウンロードサイト](http://dwnld.aicp-rom.com/)を見てみたところ、Nexus 6だけでなくNexus 7 (2013)向けのOreoビルドも公開されていることがわかりました。そういうわけで、本記事では、Nexus 7 (2013) LTE 32GBにAndroid Oreo (8.1)をインストールする手順を紹介します。

## !!注意!!
**ブートローダのアンロック、カスタムリカバリおよびカスタムROMのフラッシュはメーカの推奨しないソフトウェアの改造に相当します。したがって、本作業を行なうと、修理や交換などのメーカ保証は一切受けられなくなります。また、万が一の場合に備え、作業を行なう前には必ずバックアップをとってください。下記手順は一切無保証です。最悪の場合、端末が起動しなくなるおそれがありますが、自己責任での作業をお願いします。**

さらに注: 手元にあるNexus 7は**LTE版**であるため、以下では**LTE版を前提に手順を説明**します。WiFi版の場合は、必要なファイル一式において、ファイル名の`deb`の部分を`flo`に読み替えていただければ、LTE版と同じ手順でインストールできる可能性が高いです。

では、インストール手順について説明していきます。といっても、Nexus 6の場合とほぼ同じです。大まかにまとめると以下のようになります。

1. Android端末でのUSBデバッグの有効化 (Nexus 6向けの説明は[ここ](/post/freebsd-android-adb/))
1. 母艦端末(FreeBSDなど)でのADB接続の設定 (Nexus 6向けの説明は[ここ](/post/freebsd-android-adb/))
1. 母艦端末での必要なファイル一式の準備
1. Android端末のブートローダアンロック (Nexus 6向けの説明は[ここ](/post/nexus6-android-oemunlock/))
1. Android端末へのカスタムリカバリのフラッシュ (Nexus 6向けの説明は[ここ](/post/nexus6-android-du-flash/))
1. カスタムROMファイル一式のAndroid端末への転送
1. カスタムROMファイル一式のフラッシュ

本記事では、必要なファイル一式の準備、カスタムROMファイル一式のフラッシュについて説明します。その他の手順について詳しい説明が必要な場合は、Nexus 6向けの説明を参照してください。

### 必要なファイル一式の準備
Nexus 6のときと同じように、以下の各リンクからファイルをダウンロードしてください。

- カスタムリカバリ(ファイル名: `twrp-3.2.1-0-deb.img`)

    [ここ](https://twrp.me/asus/asusnexus72013lte.html)からダウンロード。(WiFi版の場合は[ここ](https://twrp.me/asus/asusnexus72013wifi.html)から。)
- カスタムROM本体(ファイル名: `aicp_deb_o-13.1-NIGHTLY-20180304.zip`)

    [ここ](http://dwnld.aicp-rom.com/?device=deb)からダウンロード。(WiFi版の場合は[ここ](http://dwnld.aicp-rom.com/?device=flo)から。)
- GAppsパッケージ(ファイル名: `open_gapps-arm-8.1-pico-20180228-UNOFFICIAL.zip`)

    [ここ](https://sourceforge.net/projects/unofficial-opengapps-8-1/files/arm/)からダウンロード。
- gapps-configファイル(ファイル名: `gapps-config.txt`)

    以下のテキストを`gapps-config.txt`というファイル名で保存します。

    ```text
    Exclude

    BasicDreams
Browser
CalculatorStock
CalendarStock
CameraStock
ClockStock
Email
Gallery
Launcher
LockClock
MMS
PhotoTable
PicoTTS
PrintServiceStock
SimToolKit
Terminal
```

    本ファイルは今回初めて出てきたものですので、少し説明を加えます。
    
    Nexus 7にカスタムROMファイル一式をフラッシュしようとしたところ、**システム領域の容量不足**のため、GAppsパッケージをフラッシュする際にエラーが発生しました。そこで、システム領域に余分な空きを作るため、カスタムROMにあらかじめ含まれているアプリのいくつかを削除することにします。
    
    OpenGAppsには、[gapps-config](https://github.com/opengapps/opengapps/wiki/Advanced-Features-and-Options)という、GAppsパッケージに含まれているファイルのうち指定したもののみをフラッシュする、あるいはROMに含まれているアプリのうち指定したものを削除する、という機能があります。本機能を制御するのが`gapps-config.txt`ファイルです。この例では、`Exclude`以下にリストしたアプリをROMから削除する、という処理を行なわせます。
    
    リストしたアプリについては、必要性が小さいと思われるもの、あるいはGoogle PlayからGoogle製の同等アプリがダウンロードできるもの、という観点で選択しました。

- Magiskパッケージ(ファイル名: `Magisk-v16.0.zip`) (ルート化が必要な場合)
- MagiskManagerパッケージ(ファイル名: `MagiskManager-v5.6.1.apk`) (ルート化が必要な場合)

    [ここ](https://forum.xda-developers.com/apps/magisk/official-magisk-v7-universal-systemless-t3473445)からダウンロード。

最後に、ダウンロード、あるいは作成したファイルの一覧を確認しておきましょう。

```shell-session
$ ls -1
Magisk-v16.0.zip
MagiskManager-v5.6.1.apk
aicp_deb_o-13.1-NIGHTLY-20180304.zip
gapps-config.txt
open_gapps-arm-8.1-pico-20180228-UNOFFICIAL.zip
twrp-3.2.1-0-deb.img
```

### カスタムROMファイル一式の転送
次に、準備したファイルをNexus 7 (2013)へ転送します。

```shell-script
adb shell mkdir -p /sdcard/ROM
adb push aicp_deb_o-13.1-NIGHTLY-20180304.zip            /sdcard/ROM
adb push open_gapps-arm-8.1-pico-20180228-UNOFFICIAL.zip /sdcard/ROM
adb push Magisk-v16.0.zip                                /sdcard/ROM    # ルート化が必要な場合
adb shell mkdir -p /sdcard/Open-GApps
adb push gapps-config.txt                                /sdcard/Open-GApps
```

`gapps-config.txt`ファイルは`/sdcard/Open-GApps`以下に格納される必要がありますので、このディレクトリ名は変更しないでください。

### カスタムROMファイル一式のフラッシュ
Nexus 6のときと同様の手順(フラッシュメモリのワイプとカスタムROMのフラッシュ)を実行してください。この際、フラッシュするファイルを追加する順番は以下のようになります。

- `aicp_deb_o-13.1-NIGHTLY-20180304.zip`
- `open_gapps-arm-8.1-pico-20180228-UNOFFICIAL.zip`
- `Magisk-v16.0.zip` (ルート化が必要な場合)

フラッシュが成功したら端末を再起動して、以降は画面の案内にしたがって設定を行なってください。

ちなみに、タブレット情報は以下のようになりました。

[![AICPのタブレット情報](/img/android-aicp-tablet-info-small.png)](/img/android-aicp-tablet-info.png)

### 参考文献
1. Dirty Unicorns, https://dirtyunicorns.com/
1. Android Ice Cold Project, http://aicp-rom.com/
1. OpenGApps, http://opengapps.org/
1. Advanced Features and Options, https://github.com/opengapps/opengapps/wiki/Advanced-Features-and-Options
1. Magisk, https://forum.xda-developers.com/apps/magisk
