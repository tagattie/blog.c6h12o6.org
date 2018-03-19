+++
title = "Nexus 6の\"(bootloader) slot-count: not found\"は気にしなくてよい(と思う)"
date = "2018-03-19T20:09:31+09:00"
draft = true
categories = ["Android"]
tags = ["android", "nexus6", "fastboot", "bootloader", "slot", "seamless", "update"]
+++

先日、Nexus 6にAndroid Oreo 8.1のカスタムROM [Dirty Unicorns](https://dirtyunicorns.com/)を[フラッシュする手順](/post/nexus6-android-du-flash/)をご紹介しました。カスタムROMのフラッシュにはカスタムリカバリが必要になりますので、`fastboot`コマンドを用いてカスタムリカバリをフラッシュする工程を含んでいました。

このときに、`fastboot`コマンドが、以下のようなメッセージを出力することに気づかれたかたがいらっしゃると思います。

``` shell-session
$ fastboot flash recovery twrp-3.2.1-0-shamu.img
(bootloader) slot-count: not found           # この4行
(bootloader) slot-suffixes: not found        #
(bootloader) slot-suffixes: not found        #
(bootloader) has-slot:recovery: not found    #
(snip)
```

ブートローダが`slot-count`, `slot-suffixes`, `has-slot:recovery`といったデータ(?)を見つけられない、ということのようです。これはいったい何を意味しているのでしょうかね? 何だろう、と思っていたのですが、おそらくその意味するところはこれだろう、という見当がついたので記録しておきます。

**注: 以下の記述は筆者の推測に基づくものであり、確認された事実ではありません。読み進められるかたはこの点をご承知おき願います。**

2016年のGoogle I/Oで、「シームレスアップデート」という機能がAndroid Nougat以降でサポートされる、という発表があったことを覚えていらっしゃるでしょうか?

ざっくりいうと、端末にシステムパーティションを2つ(AパーティションとBパーティション)用意します。Aパーティションのシステムで端末を動作させておきながら、アップデートの際にはBパーティションのシステムを更新します。端末を再起動すると、今度はBパーティションのシステムが起動します。このように、通常動作と並行して更新を行なうことによって、システム更新の待ち時間を大幅に短縮するというものです。

もうちょっと詳しい説明は以下の2つのURLをご参照ください。

- [Android N、アップデートを大幅短縮するシームレスアップデートに対応 既存のNexusは利用不可](http://mobilelaby.com/blog-entry-current-nexus-device-will-not-get-the-seamless-update-android-7-0-n.html)
- [A/B (Seamless) System Updates](https://source.android.com/devices/tech/ota/ab/)

一つめの記事のタイトルにもありますように、Nexus 6はシームレスアップデートに対応していません。当然、システムパーティションも一つだけです。ADB (Android Debug Bridge)を用いたチェックでも下記のようにサポートされていないことがわかります。

- 参考: [How to check if your Android device supports Seamless Updates](https://www.xda-developers.com/how-to-check-android-device-supports-seamless-updates/)

``` shell-session
$ adb shell
shell@android:/ $ getprop ro.boot.slot_suffix

shell@android:/ $ getprop ro.build.ab_update
 
```

いっぽうで、Nexus 6にインストールされているブートローダだけは、シームレスアップデートに対応しているのではないでしょうか? 全部でいくつのシステムパーティション(slot-count)があるか、複数パーティションがあったときの各パーティションのサフィックス名(_aとか_bとか)(slot-suffixes)は何か、をチェックするようになっているのだと思います。

ブートローダがチェックを試みるも、シームレスアップデートに対応していないNexus 6はそのような情報を持っておらず、結果的にnot foundというメッセージが表示されているのではないかと推測します。

結論として、Nexus 6で見られる`(bootloader) slot-count: not found`といったメッセージを気にする必要はない、ということだと思います。

### 参考文献
1. Dirty Unicorns, https://dirtyunicorns.com/
1. Android N、アップデートを大幅短縮するシームレスアップデートに対応 既存のNexusは利用不可, http://mobilelaby.com/blog-entry-current-nexus-device-will-not-get-the-seamless-update-android-7-0-n.html
1. A/B (Seamless) System Updates, https://source.android.com/devices/tech/ota/ab/
