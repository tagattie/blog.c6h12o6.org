+++
title = "Nexus 6にAndroid Oreo (8.1)をインストールする - フラッシュ編"
date = "2018-02-20T19:22:00+09:00"
lastmod = "2018-03-11T22:50:00+09:00"
categories = ["Android"]
tags = ["android", "oreo", "nexus6", "dirtyunicorns"]
+++

**追記: 2018/3/11**  
Dirty Unicorns 12 (Android Oreo 8.1)のオフィシャル版がリリースされました。最新版のファイル一覧については、[こちらの記事](/post/dirty-unicorns-12/)をご参照ください。

___

[ブートローダアンロック編](/post/nexus6-android-oemunlock/)では、カスタムROMのインストール準備として必要な、Nexus 6のブートローダアンロック手順を説明しました。本記事では、実際にカスタムROMをNexus 6のフラッシュメモリに書き込む(フラッシュする)手順について説明します。

## !!注意!!
**カスタムリカバリおよびカスタムROMのフラッシュはメーカの推奨しないソフトウェアの改造に相当します。したがって、本作業を行なうと、修理や交換などのメーカ保証は一切受けられなくなります。また、万が一の場合に備え、作業を行なう前には必ずバックアップをとってください。下記手順は一切無保証です。最悪の場合、端末が起動しなくなるおそれがありますが、自己責任での作業をお願いします。**

カスタムROMのフラッシュを実行する前に、[ファイル準備編](/post/nexus6-android-du-download/)でダウンロードしておいたファイルを格納してあるディレクトリに`cd`しておきましょう。ファイル一覧は以下のようになっていると思います。
```shell-session
$ ls -1
Magisk-v15.3.zip                                 # Magisk
MagiskManager-v5.6.0.apk                         # Magisk Manager
du_shamu-v12.0-20180214-1231-RC.zip              # カスタムROM
open_gapps-arm-8.1-nano-20180115-UNOFFICIAL.zip  # OpenGApps
twrp-3.2.1-0-shamu.img                           # カスタムリカバリ
```

では、カスタムROM (Dirty Unicorns)のフラッシュ手順について説明していきます。

1. カスタムリカバリのフラッシュ

    まず、端末のフラッシュメモリにカスタムROMを書き込むのに必要な、カスタムリカバリを端末にフラッシュします。Nexus 6を(ボリュームダウンを押しながら電源を入れて) fastbootモードで起動してください。その後、USBケーブルを用いてFreeBSDマシンに接続します。端末を接続したら、FreeBSDマシンに端末が認識されているかを確認します。
    ```shell-session
    $ fastboot devices
    XXXXXXX fastboot
    ```
    認識されていれば、次にカスタムリカバリをフラッシュします。具体的には、すでに端末に書き込まれているデフォルトのリカバリを消去して、あらためてカスタムリカバリを書き込みます。
    ```shell-session
    $ fastboot erase recovery                           # デフォルトのリカバリの消去
    (bootloader) slot-count: not found
    (bootloader) slot-suffixes: not found
    (bootloader) slot-suffixes: not found
    (bootloader) has-slot:recovery: not found
    erasing 'recovery'...
    (bootloader) Erase allowed in unlocked state
    OKAY [  0.032s]
    finished. total time: 0.032s
    $ fastboot flash recovery twrp-3.2.1-0-shamu.img    # カスタムリカバリのフラッシュ
    (bootloader) slot-count: not found
    (bootloader) slot-suffixes: not found
    (bootloader) slot-suffixes: not found
    (bootloader) has-slot:recovery: not found
    target reported max download size of 536870912 bytes
    sending 'recovery' (12027 KB)...
    OKAY [  1.645s]
    writing 'recovery'...
    OKAY [  0.241s]
    finished. total time: 1.886s
    ```
    書き込みが終了したら、念のため、再度fastbootモードで端末を起動しなおします。(今度は`fastboot`コマンドを用いて、fastbootモードで再起動させます。もちろん、電源をいったんOFFにして、ボリュームダウンを押しながら電源を入れるのでもかまいません。)
    ```shell-session
    $ fastboot reboot-bootloader
    (bootloader) slot-count: not found
    (bootloader) slot-suffixes: not found
    (bootloader) slot-suffixes: not found
    rebooting into bootloader...
    OKAY [  0.081s]
    finished. total time: 0.081s
    ```

1. カスタムリカバリの起動とフラッシュメモリ内容の消去

    fastbootモードで端末が起動したら、ボリュームダウンボタンを2回押下します。すると、"START"と表示されていた部分が"RECOVERY MODE"になります。そうしたら、電源ボタンを押下してください。Googleのロゴが表示された後、しばらく待つとカスタムリカバリ(TWRP)が起動し、下図左のような画面になります。

    現在のフラッシュメモリの内容を消去するため、次の操作を行ないます。Wipe→Advanced Wipe (下図中)と進み、Dalvik / ART Cache, System, Data, Cacheにチェックを入れます。つまり、**Internal Storage以外すべて**をチェックします。そうして、下のSwipe to Wipeスライダを右にスワイプします(下図右)。すると、システム領域、データ領域、キャッシュ領域、および仮想マシンのキャッシュが消去されます。

    |図左|図中|図右|
    |:---:|:---:|:---:|
    |![TWRPの起動画面](/img/android-twrp-home.png)|![TWRP Wipe](/img/android-twrp-wipe.png)|![TWRP Advanced Wipe](/img/android-twrp-advanced-wipe.png)|

    消去が済んだら画面下中央のホームボタンを押して、カスタムリカバリのホーム画面に戻っておきます。

1. ファイルの端末への転送

    次に、ダウンロードしておいたファイルを端末へ転送します(最後の1つはルート化が必要な場合のみ)。
    ```shell-script
    adb shell mkdir -p /sdcard/ROM
    adb push du_shamu-v12.0-20180214-1231-RC.zip             /sdcard/ROM
    adb push open_gapps-arm-8.1-nano-20180115-UNOFFICIAL.zip /sdcard/ROM
    adb push Magisk-v15.3.zip                                /sdcard/ROM
    ```

1. カスタムROMのフラッシュ

    次はいよいよカスタムROMの書き込みです。カスタムリカバリのホーム画面の"Install"をタップします(下図左)。すると、ファイルマネージャ的な画面が表示されますので、さきほどファイルを転送しておいたディレクトリ`/sdcard/ROM`へ移動します(下図中)。

    |図左|図中|図右|
    |:---:|:---:|:---:|
    |![TRWP Install](/img/android-twrp-install.png)|![TWRP Installファイル選択](/img/android-twrp-install-select-file.png)|![TWRP Installスワイプ](/img/android-twrp-install-add-more.png)|
    
    Nexus 6へ転送しておいた2つ、あるいは3つのファイルが表示されていると思いますので、まずカスタムROMの本体である`du_shamu-v12.0-20180214-1231-RC.zip`をタップします。そうすると、上図右の画面が表示されます。これまでに選択済みのファイルの数、直前に選択したファイルの名前などが表示されています。ここで、下のスライダをスワイプして書き込みを行なってもかまわないのですが、TWRPには複数のファイルを一度にフラッシュする機能がありますので、これを活用します。

    "Add more Zips"をタップすると、再びファイルマネージャ画面が表示されますので、次は`open_gapps-arm-8.1-nano-20180115-UNOFFICIAL.zip`をタップします。すると、再び上図右のような画面になります。ルート化を行なう場合は、もう一度"Add more Zips"をタップして`Magist-v15.3.zip`を追加してください。
    
    再掲しておきますと、ファイルを追加する順序は以下のようになります:
    - `du_shamu-v12.0-20180214-1231-RC.zip`
    - `open_gapps-arm-8.1-nano-20180115-UNOFFICIAL.zip`
    - `Magisk-v15.3.zip` (ルート化が必要な場合のみ)
    
    必要な分だけのファイルを追加し終わったら、画面下部のスライダを右にスワイプします。これで、いま選択したファイルが端末に書き込まれます。(下図、zipファイルの書き込みの様子)

    |Dirty Unicorns|OpenGApps|Magisk|
    |:---|:---|:---|
    |![Dirty Unicornsの書き込み](/img/android-twrp-flash-dirtyunicorns.png)|![OpenGAppsの書き込み](/img/android-twrp-flash-opengapps.png)|![Magiskの書き込み](/img/android-twrp-flash-magisk.png)|

1. 端末の再起動

    カスタムROMのフラッシュが成功すると、カスタムリカバリの画面に"Wipe Cache/Dalvik"と"Reboot"のボタンが表示されますので、"Reboot"をタップして端末を再起動します。端末が起動すると、初期設定の画面になりますので、以降は画面の内容にしたがって設定を進めていってください。
    
    注: フラッシュ直後の起動には普段よりも時間がかかりますので、心配せずにしんぼう強く待っていれば正しく起動してくれるものと思います。

1. Magisk Managerのインストール(ルート化した場合のみ)

    もし、ルート化を行なったのであれば、最後にMagisk Managerをインストールしておきましょう。「[Android端末にADBで接続](/post/freebsd-android-adb/)」で説明した、USBデバッグの有効化を再度行なっておきましょう。そうして、USBケーブルを用いてNexus 6をFreeBSDマシンに接続します。Nexus 6がADB経由で認識されているかのチェックもしましょう。
    ```shell-session
    $ adb devices
    List of devices attached
    XXXXXXX device
    ```
    問題なければ、以下のコマンドを実行すればMagisk Managerがインストールされます。(この方法以外にも、APKファイルをNexus 6のほうに転送しておいて、手動でAPKをインストールするやり方でもOKです。)
    ```shell-session
    $ adb install MagiskManager-v5.6.0.apk
    Success
    ```

以上で、Nexus 6でカスタムROM Dirty Unicornsが使えるようになりました。

ちなみに、端末情報は以下のようになりました。
![Dirty Unicornsインストール後の端末情報](/img/android-dirtyunicorns-system-info.png)
