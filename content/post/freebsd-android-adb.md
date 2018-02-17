+++
title = "FreeBSDからAndroid端末にADBで接続する"
date = "2018-02-17T12:58:00+09:00"
categories = ["FreeBSD", "Android"]
tags = ["freebsd", "android", "adb"]
+++

**更新: 2018/2/17**  
fastbootモード、recoveryモードでも非ルートユーザでNexusd端末にアクセスできるよう、adb.confの内容をアップデートしました。

2015年の2月からメインのスマートフォンとしてNexus 6を使っています。もう3世代前のリファレンス機で、公式のセキュリティアップデートも昨年10月で打ち切られてしまいました。新しい端末に切り替えたいところですが、最新のリファレンス機は日本では発売されていないし、ほかにこれというものが見つからないので、もうしばらくNexus 6を使い続けるつもりです。

OSのバージョンアップはもちろん、月次のセキュリティアップデートもなくなってしまうのはさびしいので、代わりにカスタムROMをインストールすることにしました。本記事では、その準備として、FreeBSDからAndroid端末にADB (Android Debug Bridge)経由で接続するための設定について説明します。

1. USBデバッグの有効化

    まず、Android端末側でUSBデバッグを有効化します。設定アプリを起動(あるいは、通知領域などから設定を起動)して、システム→端末情報へ進みます。下の方に「ビルド番号」があるので、ここを7回タップしてください(下図左)。すると、開発者モードが有効になります。その後、設定のシステム→開発者オプションへと進み、USBデバッグをONにします(下図右)。以上でAndroid端末側の設定は終了です。

    |図左|図右|
    |:---:|:---:|
    |![ビルド番号を7回タップ](/img/android-developer-7taps.png)|![USBデバッグをON](/img/android-developer-usb-debug.png)|

1. adbとfastbootをインストール

    ここからはFreeBSDマシン側の設定になります。まず、パッケージから`adb`および`fastboot`コマンドをインストールします。
    ```shell-script
    pkg install android-tools-adb android-tools-fastboot
    ```
    注: FreeBSDにはもともと`/sbin/fastboot`コマンドがインストールされています。本項のfastbootコマンドは`/usr/local/bin/fastboot`としてインストールされますので、間違えないようご注意ください(パスの設定など)。

1. devdの設定

    次に、ルート以外のユーザがADB接続できるように、devdの設定を行ないます。具体的には、Android端末(ここではNexus 6の場合を例示)を接続した際に、自動的にデバイスファイルのパーミッションを修正するよう設定を加えます。

    - USBケーブルを用いてAndroid端末をFreeBSDマシンに接続します。Android端末が認識されているかを確認するため、以下のコマンドを実行します。
    ```shell-session
    $ dmesg
    (snip)
    ugen0.9: <motorola Nexus 6> at usbus0
    ```
    端末が認識されていれば、上記のような一行が表示されるはずです。`ugen0.9`として認識されていますので、これを覚えておきます。

    - Android端末のUSBベンダIDとプロダクトIDを確認するため、以下のコマンドを実行します。`ugenX.Y`のX.Yを`-d`オプションの引数として指定します。(参考: [USB on FreeBSD](https://wiki.freebsd.org/USB))
        ```shell-session
        $ usbconfig -d 0.9 dump_device_desc
        ugen0.9: <motorola Nexus 6> at usbus0, cfg=0 md=HOST spd=HIGH (480Mbps) pwr=ON (500mA)
        
          bLength = 0x0012 
          bDescriptorType = 0x0001 
          bcdUSB = 0x0200 
          bDeviceClass = 0x0000  <Probed by interface class>
          bDeviceSubClass = 0x0000 
          bDeviceProtocol = 0x0000 
          bMaxPacketSize0 = 0x0040 
          idVendor = 0x18d1           # ベンダIDと
          idProduct = 0x4ee7          # プロダクトIDを覚えておく
          bcdDevice = 0x0223 
          iManufacturer = 0x0001  <motorola>
          iProduct = 0x0002  <Nexus 6>
          iSerialNumber = 0x0003  <XXXXXXX>
          bNumConfigurations = 0x0001 
        ```
    `idVendor`がベンダID、`idProduct`がプロダクトIDの値なので、これを覚えておきます。

    - devdの設定ファイルを追加します。`/usr/local/etc/devd`以下に、例えば`adb.conf`というファイル名で以下の内容を記述します。  
    (プロダクトIDが複数記述されているのは、Android端末の起動モードによりプロダクトIDが変化するためです。)
        ```conf
        # Allows non-root users to have access to Android phones via ADB.
        
        # Google Nexus 6
        notify 100 {
            match "system" "USB";
            match "subsystem" "DEVICE";
            match "type" "ATTACH";
            match "vendor" "0x18d1";                   # ベンダIDの値
            match "product" "(0x4ee0|0x4ee2|0x4ee7)";  # プロダクトIDの値
            action "chgrp users /dev/$cdev && chmod 660 /dev/$cdev";
        };
        ```
    action行については、各々のニーズに合わせて適宜変更してください。ここでは、`users`グループに属するユーザに対してADB経由でのアクセス権限を与えています。
    最後に、devdを再起動します。
    ```shell-script
    service devd restart
    ```

1. Android端末を(再)接続

    USBケーブルを用いてAndroid端末をFreeBSDマシンに接続しなおします。

1. ADBを用いた接続の確認

    最後に、`adb`コマンドを用いたアクセスができるかを確認します。`adb devices`
コマンドを実行した際に、`device`の代わりに`unauthorized`と表示される場合は、Android端末側にデバッグを許可するかのダイアログが表示されていると思いますので、そこで許可をすればOKです。

```shell-session
$ adb start-server
* daemon not running; starting now at tcp:5037
* daemon started successfully
$ adb devices
List of devices attached
XXXXXXX     device
```

### 参考文献
1. USB on FreeBSD, https://wiki.freebsd.org/USB
