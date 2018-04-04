+++
title = "Googleが非認証端末やカスタムROMでのGoogle Appsブロックを認める(対応策の更新あり)"
date = "2018-03-28T19:47:00+09:00"
lastmod = "2018-04-04T20:04:00+09:00"
categories = ["Android"]
tags = ["android", "gapps", "custom", "rom", "uncertified", "device"]
+++

**追記: 2018/4/4**  
[まとめ記事](/post/android-gapps-uncertified-device-2/)をポストしました。最新情報についてはこちらをご参照ください。

___

[昨日の記事](/post/android-gapps-uncertified-device/)で、「Google非認証端末やカスタムROMをインストールした端末でのGoogle Appsの使用がブロックされ始めた」と[XDA Developersが報じた](https://www.xda-developers.com/google-blocks-gapps-uncertified-devices-custom-rom-whitelist/)こと、およびこのブロックを回避する方法を紹介しました。さいわい、手もとにあるGoogle非認証端末やカスタムROM端末ではまだブロックされていませんが、この種の端末を使っている立場としては見逃せないところです。

- [Google now blocks GApps on uncertified devices, but lets custom ROM users be whitelisted (XDA Developers)](https://www.xda-developers.com/google-blocks-gapps-uncertified-devices-custom-rom-whitelist/)

本件に関して、Android Policeが「Googleが非認証端末などでのGoogle Appsブロックを認める」との[記事](https://www.androidpolice.com/2018/03/27/google-confirms-blocking-google-apps-uncertified-android-devices-heres-deal/)をポストしました。

- [Google confirms it's blocking Google Apps on "uncertified" Android devices - here's how to deal with it (Android Police)](https://www.androidpolice.com/2018/03/27/google-confirms-blocking-google-apps-uncertified-android-devices-heres-deal/)

この記事の中で、Android PoliceがGoogleから得たコメントとして、以下の文章を掲載しています。

> Certified Android devices offer users consistent experiences when using apps from Google and the Play Store, as well as various security benefits through Google Play Protect. We acknowledge that some manufacturers are building and selling devices that have not been certified by Google. Please see the [website](http://android.com/certified) for more details.

このコメント自体はなんだか国会答弁のようですね。しかし、参照されているWebサイトのFAQ (Frequently Asked Questions)(以下に原文引用)によれば、「非認証デバイスでは、GoogleやPlayストアが提供する基本的なセキュリティ機能やアプリなどが、意図どおりに動作することを保証しない」と述べられていますので、結果としてGoogle Appsのブロックを否定しない、ということになるのでしょうか。

> What do I do if my device is not certified?
> 
> If your device is not certified, we have not ensured basic security features or apps from Google and the Play Store can work as intended. Please keep in mind that your device may not be secure and may not function properly. 
> 
> We recommend you contact your device manufacturer or retailer to ask for a fully tested, certified device.

さて、[昨日の記事](/post/android-gapps-uncertified-device/)では、カスタムROMユーザがブロックを回避する方法として、以下の二つの方法を紹介しました。

- Android IDをGoogleに登録 - 公式の対応策
- Magiskをインストール - 非公式の対応策

公式の対応策に関して更新があります。

[Android Police](https://www.androidpolice.com/2018/03/27/google-confirms-blocking-google-apps-uncertified-android-devices-heres-deal/)および[XDA Developers](https://www.xda-developers.com/how-to-fix-device-not-certified-by-google-error/)によれば、"Android ID"ではなく"Google Services Framework (GSF) ID"を登録するのだということです。(ただし、ID登録のためのGoogleのサイトでは今でもAndroid IDと書かれていますし、記事のコメント欄には「IMEIを登録する」というコメントがあったりするので、いささか混乱しているようです。しばらく様子を見守る必要がありそうです。)

### Google Services Framework (GSF) IDの取得方法
以下のいずれかのアプリをインストールしてください。

- [Device ID (by Evozi) (Google Play)](https://play.google.com/store/apps/details?id=com.evozi.deviceid)
- [Device ID (by CodeKiemCom) (Google Play)](https://play.google.com/store/apps/details?id=com.redphx.deviceid)

アプリを起動すれば、GSF IDが表示されます。一番目のアプリならGoogle Services Framework (GSF)の欄、二番目のアプリならDevice IDの欄がGSF IDの値となります。二番目のアプリのほうは、インターネット接続のパーミッションがありませんので、不安なかたは二番目のアプリを使うのがよさそうです。

また、Googleアカウントにサインインできなくなった時の備えとして、apkファイルをダウンロードしておくと安心です。以下のURLからダウンロードできます。

- [Device ID (by Evozi) (APKMirror)](https://www.apkmirror.com/apk/evozi/device-id/)
- [Device ID (by CodeKiemCom) (APKMirror)](https://www.apkmirror.com/apk/redphx/device-id-2/)

### 参考文献
1. Google now blocks GApps on uncertified devices, but lets custom ROM users be whitelisted, https://www.xda-developers.com/google-blocks-gapps-uncertified-devices-custom-rom-whitelist/
1. Google confirms it's blocking Google Apps on "uncertified" Android devices - here's how to deal with it, https://www.androidpolice.com/2018/03/27/google-confirms-blocking-google-apps-uncertified-android-devices-heres-deal/
1. Certified Android devices: safe and secure, https://www.android.com/certified/
1. How to Fix the “Device is not Certified by Google” Error, https://www.xda-developers.com/how-to-fix-device-not-certified-by-google-error/
1. Device ID (by Evozi), https://play.google.com/store/apps/details?id=com.evozi.deviceid
1. Device ID (by CodeKiemCom), https://play.google.com/store/apps/details?id=com.redphx.deviceid
