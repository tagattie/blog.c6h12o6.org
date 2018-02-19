+++
title = "Nexus 6にAndroid Oreo (8.1)をインストールする - ブートローダアンロック編"
date = "2018-02-18T19:18:30+09:00"
draft = true
categories = ["Android"]
tags = ["android", "oreo", "nexus6"]
+++

[ファイル準備編](/post/nexus6-android-du-download/)では、カスタムROMのインストールに必要なファイル一式について説明し、これらをFreeBSDマシンにダウンロードしました。本記事では、カスタムROMをインストールするためにNexus 6側に必要な準備、つまりブートローダのロック解除(アンロック)を行なう手順について説明します。

## !!注意!!
**ブートローダのアンロックはメーカの推奨しないソフトウェアの改造に相当します。したがって、ブートローダのアンロックを行なった時点で、修理や交換などのメーカ保証は一切受けられなくなります。また、アンロックを行なうことで、端末が工場出荷状態にリセットされます。端末にインストールされているアプリや保存されている写真、音楽などのファイルはすべて消去されますので、作業を行なう前には必ずバックアップをとってください。下記手順は一切無保証です。最悪の場合、端末が起動しなくなるおそれがありますが、自己責任での作業をお願いします。**

[アンドロイドラバーの記事](http://androidlover.net/smartphone/nexus-6/nexus6-bootloader-unlock.html)でも、Nexus 6のブートローダアンロックについて説明されています。バックアップのためのアプリについても紹介されていますので、あわせて参考にしていただければと思います。

では、ブートローダのアンロック手順について説明していきます。

1. OEMロックの解除を有効化

    Nexus 6上で設定アプリを起動して、システム→開発者向けオプションと進み、「OEMロック解除」をONにしてください(下図左)。これを行なわない場合、3.で説明するアンロックコマンドの実行時にエラーとなってしまいます。本項目をタップすると、PINやパターンなどでの端末ロックを設定している場合、端末ロックの解除を求められます。端末ロックの解除を行なうと、OEMロック解除の許可を求めるポップアップが表示されますので、ここで「有効にする」をタップすればOKです(下図右)。

    |図左|図右|
    |:---:|:---:|
    |![OEMロック解除の有効化](/img/android-enable-oem-unlock.png)|![OEMロック解除の有効化確認](/img/android-enable-oem-unlock-confirm.png)|

1. fastbootモードで端末を起動

    Nexus 6の電源をいったんOFFにし、あらためてボリュームダウンボタンを押しながら電源をONにします。そうすると、fastbootモードで起動し、ドロイド君のおなかが開いているアイコンが表示された画面になります(下図、すでにアンロック済みになっていますがお気になさらないようお願いしますm(__)m)。
    [![fastbootモード](/img/android-fastboot-small.jpg)](/img/android-fastboot.jpg)
    この状態で、FreeBSDマシンにNexus 6をUSBケーブルを用いて接続します。
    
    注: FreeBSDからAndroid端末へアクセスするには**事前準備が必要**なので、もしまだの場合は[こちら](/post/freebsd-android-adb/)を参照して準備を行なってください。

1. ブートローダのアンロック

    まず、fastbootモードで起動したNexus 6がFreeBSDに認識されているかを確認します。
    ```shell-session
    $ fastboot devices
    XXXXXXX fastboot
    ```
    上記のように`端末のシリアル番号 fastboot`が表示されればOKです。
    
    次に、ブートローダをアンロックします。
    
    注: 下記コマンドを実行すると、インストールされているアプリだけでなく、写真や音楽ファイルなども含めたNexus 6内のすべてのデータが消去されます。必ず**バックアップ**を取っておきましょう。  
    注2: 以下のコマンドを実行する前に、必ず**バックアップ**を取ってください。  
    注3: くどいようですが**バックアップ**は取りましたか?
    ```shell-session
    $ fastboot oem unlock
    (bootloader) slot-count: not found
    (bootloader) slot-suffixes: not found
    (bootloader) slot-suffixes: not found
    ...
    (bootloader) Device state transition will erase userdata.
    (bootloader) Are you sure you want to continue this transition?
    (bootloader) 
    (bootloader) Press POWER key to continue.
    (bootloader) Press VOL UP or VOL DOWN key to cancel state transition.
    ```
    本コマンドを実行するとNexus 6上に、ブートローダのアンロックを行なってよいかの確認画面が表示されます(下図)。本当にアンロックしてよい場合は電源ボタンを押してください。(キャンセルする場合は、ボリュームアップあるいはボリュームダウンのボタンを押します。)
    [![ブートローダアンロックの確認画面](/img/android-fastboot-oem-unlock-small.jpg)](/img/android-fastboot-oem-unlock.jpg)

以上でブートローダのアンロックは完了です。

### 参考文献
1. Nexus6のブートローダーアンロック方法。, http://androidlover.net/smartphone/nexus-6/nexus6-bootloader-unlock.html
