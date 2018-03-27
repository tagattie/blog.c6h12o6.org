+++
title = "Google非認証端末やカスタムROMでGoogle Appsはブロックされるのか?"
date = "2018-03-27T15:52:20+09:00"
draft = true
categories = ["Android"]
tags = ["android", "gapps", "custom", "rom", "uncertified", "device", "nexus", "nexus7", "nexus6", "jelly"]
+++

[XDA Developersの記事](https://www.xda-developers.com/google-blocks-gapps-uncertified-devices-custom-rom-whitelist/)で、Googleが非認証端末でのGoogle Apps (Playストアをはじめとする基本的アプリ群)の使用をブロックし始めたと報じられました。(XDA Developersをもとにした日本語記事は[こちら](https://www.dream-seed.com/weblog/news/google-block-gapps-uncertified-device))

- [Google now blocks GApps on uncertified devices, but lets custom ROM users be whitelisted (XDA Developers)](https://www.xda-developers.com/google-blocks-gapps-uncertified-devices-custom-rom-whitelist/)
- [Google、非認証デバイスでGAppsのブロックを開始。カスタムROMはホワイトリストに登録可能 (Dream Seed)](https://www.dream-seed.com/weblog/news/google-block-gapps-uncertified-device)

ひと言でいうと、以下の二つの条件を満たす端末からGoogleアカウントへのサインインが行えなくなる、ということです。この結果、Playストアが使えなくなりますので、アプリのインストールや更新が非常にめんどうになり、端末使用時の利便性が著しく損なわれます。

- 非認証端末であること

    Googleが定義するCTS (Compatibility Test Suite)にパスしていない端末や、カスタムROMがインストールされている端末などが該当します。

- 2018/3/16以降にビルドされたファームウェアがインストールされていること

    Googleがどのようにしてビルド日時の判定を行なっているかは、今のところ不明のようです。(上記、XDA Developersの記事では、ビルドのフィンガープリントプロパティを用いているようだ、との記述あり。)
    
    ちなみに、本題とはあまり関係ありませんが、ビルド番号からビルドの日付を取得するには以下のURLが便利です。

    - [Android Build Number Date Calculator (Android Police)](https://www.androidpolice.com/android-build-number-date-calculator/)

さて、ブロックされる条件はわかったのですが、どうすればいいのでしょうか?

まず、手もとの端末が認証済みかを確認しましょう。確認する方法はとても簡単です。Playストアを起動して設定メニューを選択してください。一番下に端末の認証状態が表示されます。(下図)

![端末の認証状態を確認](/img/android/play-store-certified.png)

認証済みでしたか? おめでとうございます! これ以上読み進めていただく必要はありません。ほかのもっと楽しいサイトを訪れましょう!

残念ながら「認証されていません」と表示されてしまいましたか? もし、カスタムROMなどの改造ファームウェアをインストールしていない状態であれば、端末自体が認証されていない、ということになりますね。この場合はどうしたらいいのでしょうね? 端末メーカーに認証を取得するよう働きかけるか、今後のファームウェアアップデートをあきらめるか、になるのでしょうか。

カスタムROMをインストールしている場合は対応策があり、下記のいずれかを実行することでGoogle Appsのブロックを回避できるようです。

- Android IDをGoogleに[登録](https://www.google.com/android/uncertified/)

    [端末の登録 (Google)](https://www.google.com/android/uncertified/)

    こちらが公式の対応策です。上記、GoogleのWebサイト上でAndroid IDを登録します。これによって、該当するAndroid IDを持つシステムではGoogle Appsを使えるようにするものです。
    
    Android IDを確認するには、Android端末を母艦端末(FreeBSDマシンなど)に接続して、以下のコマンドを実行します。
    
    ``` shell
adb shell settings get secure android_id
```

    - 登録できるAndroid IDは、一つのGoogleアカウントにつき最大100個までとなっています。したがって、頻繁にファクトリリセット(端末のワイプ)とカスタムROMのフラッシュを繰り返すような使い方では、100個使い切らないように気を付ける必要があります。

    - [Titanium Backup](https://play.google.com/store/apps/details?id=com.keramidas.TitaniumBackup) **[Pro](https://play.google.com/store/apps/details?id=com.keramidas.TitaniumBackupPro)**には、ファクトリリセット前のAndroid IDを復元する機能があります。しかし、復元がうまくいかないことがありますので注意が必要です。(少なくとも、Dirty Unicorns 12 (Oreo 8.1)をフラッシュした後は復元できませんでした。)
    
        [Android Developers Blogの記事](https://android-developers.googleblog.com/2017/04/changes-to-device-identifiers-in.html)で、OreoではAndroid IDがデバイス単位からアプリ、ユーザ単位のIDに変更になったことなどが述べられており、これが関係していそうな気がします。(参考: [Changes to Device Identifiers in Android O](https://android-developers.googleblog.com/2017/04/changes-to-device-identifiers-in.html))

- Magiskをインストール
    
    [Google is starting to block GApps on 'uncertified' devices, but you can register an exemption for ROMs (Android Police)](https://www.androidpolice.com/2018/03/25/google-starting-block-gapps-uncertified-devices-can-register-exemption-roms/)

    別の対応策として、上記[Android Policeの記事](https://www.androidpolice.com/2018/03/25/google-starting-block-gapps-uncertified-devices-can-register-exemption-roms/)で述べられている方法です。非公式な方法ですが、Magiskをインストールする副作用(?)として、認証済み端末にする(見せかける)ことができます。

最後にご参考として、手もとにある端末(Galaxy Nexus, Nexus 7 (2013), Nexus 6, Jelly Pro)の状況をまとめて示しておきます。

|端末|ファームウェア|端末情報、認証状態|
|:---|:---|:---|
|Galaxy Nexus|[Unlegacy Android](https://github.com/Unlegacy-Android) 7.1.2 (Nougat) + Magisk|![Galaxy Nexusの端末認証状態](/img/android/galaxy-nexus-certified.png)|
|Nexus 7 (2013)|[AICP](http://aicp-rom.com/) 13.1 (Oreo) + Magisk|![Nexus 7 (2013)の端末認証状態](/img/android/nexus-7-2013-certified.png)|
|Nexus 6|[Dirty Unicorns](https://dirtyunicorns.com/) 12 (Oreo) + Magisk|![Nexus 6の端末認証状態](/img/android/nexus-6-certified.png)|
|[Jelly Pro](https://www.unihertz.com/ja/jelly.html)|Stock (Nougat)|![Jelly Proの端末認証状態](/img/android/jelly-pro-uncertified.png)|

Jelly Proは非認証端末だったんですね…。近々Oreoにアップデートする計画があるときいていますけれど、どうなるのかなあ…。

いまのところのまとめとしては、この四つの端末のいずれもGoogleアカウントへのサインインはブロックされていません。いずれでも、Playストアも問題なく使えています。

### 参考文献
1. Google now blocks GApps on uncertified devices, but lets custom ROM users be whitelisted, https://www.xda-developers.com/google-blocks-gapps-uncertified-devices-custom-rom-whitelist/
1. Google、非認証デバイスでGAppsのブロックを開始。カスタムROMはホワイトリストに登録可能, https://www.dream-seed.com/weblog/news/google-block-gapps-uncertified-device
1. Changes to Device Identifiers in Android O, https://android-developers.googleblog.com/2017/04/changes-to-device-identifiers-in.html
1. Google is starting to block GApps on 'uncertified' devices, but you can register an exemption for ROMs, https://www.androidpolice.com/2018/03/25/google-starting-block-gapps-uncertified-devices-can-register-exemption-roms/
