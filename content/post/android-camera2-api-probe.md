+++
title = "Camera2 API ProbeでAndroid端末のカメラAPIサポートレベルを確認する"
date = "2018-06-11T18:23:02+09:00"
categories = ["Android"]
tags = ["android", "camera", "camera2", "api", "probe", "support", "level", "manual"]
+++

ハードウェアのスペックを調べたり、比較したりするのって楽しいですよね。Android端末を入手すると、[AIDA64](https://play.google.com/store/apps/details?id=com.finalwire.aida64)や[CPU-Z](https://play.google.com/store/apps/details?id=com.cpuid.cpu_z)などのアプリをインストールして、必ずハードウェア詳細を確認してしまいます。

本記事では、このようなスペック確認系(?)アプリのうちでも、端末のカメラ機能の確認に特化した[Camera2 API Probe](https://play.google.com/store/apps/details?id=com.airbeat.device.inspector)というアプリを紹介します。

- [How To Find What Camera2 API Level Your Android Phone Supports (AddictiveTips)](https://www.addictivetips.com/android/camera2-api-level-your-android-phone/)
- [Camera2 API Probe (Google Play)](https://play.google.com/store/apps/details?id=com.airbeat.device.inspector)

Androidでは従来のCamera APIに代わって、Lollypop (Android 5.0)からCamera2 APIが導入されました。ハードウェアに応じたAPIのサポートレベルには4段階あり、下からLegacy, Limited, Full, Level 3となっています。(各レベルの意味するところについては[APIドキュメント](https://developer.android.com/reference/android/hardware/camera2/CameraCharacteristics#INFO_SUPPORTED_HARDWARE_LEVEL)を参照してください。)

- [高度なカメラ機能のための Camera API (Android Developers)](https://developer.android.com/about/versions/android-5.0#Camera-v2)

Camera2 API Probeを使うと前記のAPIサポートレベルを含め、端末に搭載されているカメラが、本APIで定義されている機能のどの程度までをサポートしているか確認できます。

使い方はきわめて簡単で、インストール後アプリを起動するだけです。すると、搭載されているカメラ(例えば、背面カメラおよび前面カメラ)ごとに、APIサポートレベルを表示してくれます。(下図、Nexus 6での表示例)

![Android - Camera2 API Probe - 1](/img/android/camera2-api-probe-1.png)
![Android - Camera2 API Probe - 2](/img/android/camera2-api-probe-2.png)

スペック確認愛好家としてはカメラ詳細を確認できてめでたしめでたしです。でも、端末標準のカメラアプリを使っているかぎり、これ以上のメリットはなさそうですね。次のような場合には、実用的な価値がありそうです。[Manual Camera](https://play.google.com/store/apps/details?id=pl.vipek.camera2)や[Camera FV-5](https://play.google.com/store/apps/details?id=com.flavionet.android.camera.pro)などの、マニュアル撮影が可能なカメラアプリを使いたいと思ったとき、ハードウェアがサポートする機能を事前に確認できます。

### 参考文献
1. How To Find What Camera2 API Level Your Android Phone Supports, https://www.addictivetips.com/android/camera2-api-level-your-android-phone/
1. Camera2 API Probe, https://play.google.com/store/apps/details?id=com.airbeat.device.inspector
1. 高度なカメラ機能のための Camera API, https://developer.android.com/about/versions/android-5.0#Camera-v2
