+++
title = "iPhone X/Android P風のジェスチャー操作を可能にするアプリ3つを比較する"
date = "2018-05-23T21:28:00+09:00"
categories = ["Android"]
tags = ["android", "iphone", "gesture", "navigation", "operation", "control", "oneplus", "adb"]
+++

ジェスチャーを用いてコンピュータを操作する、というのは古くからあるアイディアです。たとえば、マウスジェスチャーでWebブラウザを操作したり、ゲーム機のコントローラをシェイクしたり傾けたりしてプレイする、などがすぐに思い浮かぶのではないでしょうか。そもそも、スマートフォンの基本的な操作であるタップやスワイプもジェスチャーの一種ですね。

このジェスチャー操作ですが、スマートフォンの世界で最近注目されているようです。きっかけは、Apple社のiPhone Xが長年使用してきたホームボタンを廃止して、アプリの切り替えなどに使用するジェスチャーを導入したことのようですね。これを機に、Android端末でも同様のジェスチャー操作を導入する動きが現れています。

代表的といえるのが、先日のGoogle I/O 2018でアナウンスされたAndroid Pの新しいホームボタンです。従来の「バック、ホーム、マルチタスク」の3ボタンからなるナビゲーションバーの代わりに、Android Pでは横長の角丸長方形をしたホームボタンが1つだけ配置されます。本ボタンのタップやスワイプでナビゲーションを行ないます。

- [Androidの新しいジェスチャーはiPhone Xそっくり (TechCrunch Japan)](https://jp.techcrunch.com/2018/05/09/2018-05-08-android-blatantly-copies-the-iphone-x-navigation-gestures/)

このように、次期Androidでは新たなナビゲーションが導入されそうですが、端末メーカーが独自ジェスチャーを導入する動きもあります。たとえば、OnePlus社のOnePlus 5Tや6ではAndroid Pのものとは少し異なるジェスチャー操作が導入されています。

- [OnePlus 5T officially updated with iPhone X-like navigation gestures (Android Central)](https://www.androidcentral.com/oneplus-5t-officially-updated-iphone-x-navigation-gestures)

このような動きの中、XDA Developersで[アナウンス](https://www.xda-developers.com/navigation-gestures-iphone-x-app-xda/)された[Navigation Gestures](https://play.google.com/store/apps/details?id=com.xda.nobar)というアプリが注目を集めています。本アナウンスを受けた[日本語の速報記事](https://www.dream-seed.com/weblog/news/navigation-gestures-xda)もアップされています。

- [Navigation Gestures by XDA brings iPhone X-style gesture controls to Android devices (XDA Developers)](https://www.xda-developers.com/navigation-gestures-iphone-x-app-xda/)
- [AndroidでiPhone X風のジェスチャーコントロールを可能にする「Navigation Gestures」公開 (Dream Seed)](https://www.dream-seed.com/weblog/news/navigation-gestures-xda)

そこで本記事では、Nexus 6などのAndroid Pを試せない端末のために、Android PでなくてもiPhone X/Android P風のジェスチャー操作を可能にするアプリを三つ取りあげて、簡単に比較してみたいと思います。

ただ残念なことに、iPhone X/Android Pのジェスチャー操作で個人的に最も魅力を感じる、ホームボタン(あるいは画面下部領域)の左右スワイプでのアプリケーション切り替えは、いずれのアプリでも実現できません…。

さて、取りあげるのは以下の三つのアプリです。

- [Gesture Control - Next level navigation (Google Play)](https://play.google.com/store/apps/details?id=com.conena.navigation.gesture.control)
- [Navigation Gestures (Google Play)](https://play.google.com/store/apps/details?id=com.xda.nobar)
- [OnePlus Gestures — Gesture Control (Google Play)](https://play.google.com/store/apps/details?id=com.ivianuu.oneplusgestures)

いずれのアプリも、インストール後起動して設定を行なえば使えます。ただし、以下の三点に気をつけてください。

- ユーザー補助(Accessibility)のパーミッションを許可する必要があります
- Navigation GesturesおよびOnePlus Gesturesについては、設定の途中でAndroid端末を母艦端末にいったん接続し、以下のADBコマンドをそれぞれ実行する必要があります(ナビゲーションバーを画面から消去するため)(端末がルート化されていれば、アプリのルート権限を許可するのでもOKです)  
(参考: [FreeBSDからAndroid端末にADBで接続する](/post/freebsd-android-adb/))
    - Navigation Gestures

        ``` shell
adb shell pm grant com.xda.nobar android.permission.WRITE_SECURE_SETTINGS
```

    - OnePlus Gestures

        ``` shell
adb shell pm grant com.ivianuu.oneplusgestures android.permission.WRITE_SECURE_SETTINGS
```
- Navigation GesturesおよびOnePlus Gesturesについて、アンインストールの際は**必ず機能を停止**してからアンインストールしてください。そうしないとナビゲーションバーが消えたままになり、その後の操作ができなくなるおそれがあります。

以下に各アプリの価格、ジェスチャー数、割り当て可能操作数、ナビゲーションバーを単体で消去できるか、および画面イメージをまとめましたので、参考にしていただければと思います。

||[Gesture Control](https://play.google.com/store/apps/details?id=com.conena.navigation.gesture.control)|[Navigation Gestures](https://play.google.com/store/apps/details?id=com.xda.nobar)|[OnePlus Gestures](https://play.google.com/store/apps/details?id=com.ivianuu.oneplusgestures)|
|:---|:---|:---|:---|
|価格|無料<br>(有料版￥400)|無料<br>(有料版￥110)|￥170|
|ジェスチャー数<br>(タップ)|0(無料)<br>3(有料)|3|0|
|ジェスチャー数<br>(スワイプ)|8|5|8|
|割り当て可能操作数|40以上(無料)<br>60以上(有料)|7(無料)<br>14(有料)|14|
|ナビバー消去<br>(非Root)|不可|可|可|
|画面イメージ|![Gesture Control](/img/android/android-gesture-control.png)|![Navigation Gestures](/img/android/android-navigation-gestures.png)|![OnePlus Gestures](/img/android/android-oneplus-gestures.png)|

Gesture Controlは割り当て可能な操作数が他の二つと比べて圧倒的に多いです。しかし、単体でナビゲーションバーを消去できないのが残念なところですね。なので、S7世代以前までのSamsung Galaxyシリーズのように、物理キーを搭載する端末で使うならばGesture Controlはかなりよさそうです。Nexus 6などのソフトキー端末だと、やはりナビゲーションバーを消せることが大きく効いてくるので、Navigation GesturesあるいはOnePlus Gesturesに軍配が上がりそうです。

自分がチョイスするとすれば以下のような感じですね。

||物理キーあり端末|物理キーなし端末|
|:---|:---|:---|
|無料|Gesture Control|Navigation Gestures|
|有料|OnePlus Gestures|OnePlus Gestures|

OnePlus Gesturesの左、中、右のスワイプアップというのが意外に使いやすくて気に入りました。そういうわけで、OnePlus Gesturesを購入してしばらく使ってみようと思っています。いずれにしても、こういうものは実際に使ってみないとなかなか感覚がつかめないので、試してみた上のお好みで選んでいただくとよいと思います。

### 参考文献
1. Androidの新しいジェスチャーはiPhone Xそっくり, https://jp.techcrunch.com/2018/05/09/2018-05-08-android-blatantly-copies-the-iphone-x-navigation-gestures/
1. OnePlus 5T officially updated with iPhone X-like navigation gestures, https://www.androidcentral.com/oneplus-5t-officially-updated-iphone-x-navigation-gestures
1. Navigation Gestures by XDA brings iPhone X-style gesture controls to Android devices, https://www.xda-developers.com/navigation-gestures-iphone-x-app-xda/
1. AndroidでiPhone X風のジェスチャーコントロールを可能にする「Navigation Gestures」公開, https://www.dream-seed.com/weblog/news/navigation-gestures-xda
1. Gesture Control - Next level navigation, https://play.google.com/store/apps/details?id=com.conena.navigation.gesture.control
1. Navigation Gestures, https://play.google.com/store/apps/details?id=com.xda.nobar
1. OnePlus Gestures — Gesture Control, https://play.google.com/store/apps/details?id=com.ivianuu.oneplusgestures
