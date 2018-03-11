+++
title = "Dirty Unicorns 12 (Android Oreo 8.1)のオフィシャル版がリリース"
date = "2018-03-11T21:55:54+09:00"
draft = true
categories = ["Android"]
tags = ["android", "oreo", "dirtyunicorns", "opengapps", "nexus6"]
+++

先日、Nexus 6にAndroid Oreo (8.1)をインストールする手順を紹介しました。このときに使ったのが[Dirty Unicorns](https://dirtyunicorns.com/)というカスタムROMです。

2018/3/9付でDirty Unicorns 12 (Android Oreo 8.1)のオフィシャル版がリリースされました。

{{< tweet 972185412331540481 >}}

また、合わせて、というわけではないと思いますがOreo (8.1)向けのOpenGAppsパッケージもオフィシャルサイトからダウンロードできるようになりました。

改めて、Nexus 6にDirty Unicornsをフラッシュするのに必要なファイル一式をまとめておきます(カッコ内はファイル名)。先の記事で紹介したリリース候補版を使っている場合でも、改めてフルワイプ(Internal Storage以外全て消去)が必要になりますのでご注意ください。

- カスタムリカバリ(`twrp-3.2.1-0-shamu.img`)

    [ここ](https://twrp.me/motorola/motorolanexus6.html)からダウンロード。
- カスタムROM本体(`du_shamu-v12.0-20180309-1314-OFFICIAL.zip`)

    [ここ](https://download.dirtyunicorns.com/?dir=shamu/Official)からダウンロード。
- OpenGAppsパッケージ(`open_gapps-arm-8.1-<variant>-<date>.zip`)

    [ここ](http://opengapps.org/)からダウンロード。Platform, AndroidはそれぞれARM, 8.1。Variantについては、システム領域に比較的余裕があるのでStock以下であればフラッシュできると思います。
- Magiskパッケージ(`Magisk-v16.0.zip`)(ルート化が必要な場合のみ)

    [ここ](https://forum.xda-developers.com/apps/magisk/official-magisk-v7-universal-systemless-t3473445)からダウンロード。
