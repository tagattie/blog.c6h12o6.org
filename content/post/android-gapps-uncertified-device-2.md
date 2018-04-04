+++
title = "Google非認証端末やカスタムROMでのGoogle Appsブロック対策まとめ"
date = "2018-04-04T20:06:00+09:00"
categories = ["Android"]
tags = ["android", "gapps", "custom", "rom", "uncertified", "device"]
+++

[3月27日](/post/android-gapps-uncertified-device/)および[28日](/post/android-gapps-blocking-confirmed/)の記事で、Google非認証端末やカスタムROMをインストールした端末での、Google Apps (Playストアをはじめとする基本的アプリ群)の使用がブロックされ始めたことと、このブロックを回避する方法について報告しました。本件に関して、XDA Developersが[アップデート](https://www.xda-developers.com/google-removes-100-device-registration-limit-uncertified-device-page/)を掲載しています。

- [Google Removes the 100 Device Registration Limit from the Uncertified Device Page (XDA Developers)](https://www.xda-developers.com/google-removes-100-device-registration-limit-uncertified-device-page/)

ざっくりいうと、ブロックを回避するためにGoogleに登録できるIDが、当初は1アカウントにつき100個までとされていましたが、この**制限が撤廃**されたということです。カスタムROMを頻繁にフラッシュする、というかたにとってはうれしいニュースですね。

また、登録するIDはAndroid IDだとか、いやIMEIを登録するんだとか、GSF IDでいいんだけど10進数字にしないといけないとか、少々混乱があったようですが、登録のためのサイトも更新されて(下図)、落ちついたように見えます。

![非認証デバイスを登録するためのGoogleサイト](/img/android/google-register-uncertified-device.png)

あらためて、端末が認証されているかどうかの確認方法と、認証されていない場合の対策方法についてまとめておきたいと思います。

### 端末認証の確認方法
Playストアを起動して、メニューから設定を選択します。一番下に端末の認証状態が表示されます。(下図)

![Playストアでの端末認証状態表示](/img/android/play-store-certified.png)

### 非認証の場合の対策方法
以下のいずれかを実施します。

- Google Services Framework (GSF) IDをGoogleのサイトで登録

    公式の対応方法です。当初、IDの登録数は1アカウントあたり最大100個とされていましたが、この**制限は撤回**されました。

    [端末の登録 (Google)](https://www.google.com/android/uncertified/)
    
    Google Services Framework (GSF) IDは**64ビットの整数**で、**16進文字列**として表現されます。本IDを、上記GoogleのWebサイトで登録します。これによって、該当するGSF IDを持つ端末では、非認証の状態であってもGoogle Appsを使えるようになります。GSF IDは、以下のいずれかの方法で確認できます。
    
    - `adb`コマンドを使用(上記Googleサイトで紹介されている方法)
    
        Android端末を母艦端末にケーブルで接続して、以下のコマンドを実行します。
	
        ``` shell
adb root
adb shell 'sqlite3 /data/data/com.google.android.gsf/databases/gservices.db "select * from main where name = \"android_id\";"'
```

    - アプリを使用
    
        以下のいずれかのアプリをインストールします。

        - [Device ID (by Evozi) (Google Play)](https://play.google.com/store/apps/detail
s?id=com.evozi.deviceid)
        - [Device ID (by Evozi) (APKMirror)](https://www.apkmirror.com/apk/evozi/device-id/)
        - [Device ID (by CodeKiemCom) (Google Play)](https://play.google.com/store/apps/
details?id=com.redphx.deviceid)
        - [Device ID (by CodeKiemCom) (APKMirror)](https://www.apkmirror.com/apk/redphx/
device-id-2/)

        アプリを起動すればGSF IDが表示されます。一番目のアプリ(by Evozi)ならGoogle Services Framework (GSF)の欄、二番目のアプリ(by CodeKiemCom)ならDevice IDの欄がGSF IDの値となります。二番目のアプリのほうは、インターネット接続のパーミッションがありませんので、不安な場合は二番目のアプリを使うのがよさそうです。

- Magiskをインストール

    非公式な方法ですが、Magiskをインストールする副作用(?)として、認証済み端末にする(見せかける)ことができます。

### 参考文献
1. Google Removes the 100 Device Registration Limit from the Uncertified Device Page, https://www.xda-developers.com/google-removes-100-device-registration-limit-uncertified-device-page/
1. 端末の登録, https://www.google.com/android/uncertified/
