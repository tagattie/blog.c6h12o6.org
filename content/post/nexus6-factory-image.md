+++
title = "ファクトリイメージを使ってNexus 6を工場出荷状態に戻す"
date = "2018-03-17T21:45:00+09:00"
categories = ["Android"]
tags = ["android", "nexus6", "factory", "reset", "initialize", "freebsd"]
+++

[先日のポスト](/post/dirty-unicorns-12/)でもご紹介しましたが、[Dirty Unicorns](https://dirtyunicorns.com/) 12 (Android Oreo 8.1)のオフィシャル版がリリースされたということで、さっそくNexus 6にフラッシュしてみました。

フラッシュ自体は何事もなく成功しました。端末を再起動すると初期設定が始まります。まず、WiFiのアクセスポイントを設定して…、と進めていくと、端末のセキュリティ保護手段を選択する画面が出てきました。パターン、PIN (Personal Identification Number)、スライド、なし、など画面のロックを解除するときの手段を選ぶところですね。

PINを使うので、「PIN」を選択するところまではよかったのです。その後、いつもはスキップする「Androidの起動時にPINを要求する」を有効にしてしまったんです。(これを有効にすると、端末が起動してホーム画面が表示される前にPINの入力を求められます。) まあ、いいか、とそのまま設定を進めてしまったのが運の尽きでした。

再起動してしばらく待つとPINの入力画面になります。設定したPINを入力すると、番号が間違っていると。何度やってもダメ。ホーム画面に行けません。

しょうがない、もう一度ファイル一式をフラッシュし直すか、ということでカスタムリカバリを起動しました。すると、いつもは求められない「暗号化を解除するためのパスワードを入力せよ」との表示が。さきほど設定したPINを入力してみるも、ここでもやっぱりダメ。暗号化を解除できずファイルシステムをマウントできないとのメッセージが。

ああ、やってしまった…。

最後の手段として、ファクトリイメージを使って工場出荷状態に戻すことにしました。リファレンス機なので当たり前といえば当たり前なのですが、ファクトリイメージが公式に提供されていることはNexus / Pixelシリーズの魅力の一つですね。

では、以下、工場出荷状態に戻す手順を記していきます。

1. ファクトリイメージの取得

    [ここ](https://developers.google.com/android/images#shamu)からNexus 6 (shamu)向けのイメージをダウンロードします。2017年10月のビルドN6F27Mが最新ですね。過去にリリースされたすべてのイメージが掲載されているので、いかなる時点にも戻せるところがすごいです。(いまとなっては、最新のイメージに戻す以外はないと思いますが。)

1. 必要なFreeBSDパッケージのインストール

    ファクトリイメージの展開に必要なunzipパッケージ、およびNexus 6のフラッシュメモリの消去、書き込みに必要なfastbootパッケージをインストールします。

    ``` shell
pkg install unzip android-tools-fastboot
```

    FreeBSDからNexus 6に接続するための設定については、[こちら](/post/freebsd-android-adb/)を参照してください。

1. ファクトリイメージの展開

    `unzip`コマンドでファクトリイメージを展開します。zipファイルの中に、さらにzipファイルがあるので2回コマンドを実行します。

    ``` shell-session
$ unzip shamu-n6f27m-factory-bf5cce08.zip
Archive:  shamu-n6f27m-factory-bf5cce08.zip
   creating: shamu-n6f27m/
  inflating: shamu-n6f27m/radio-shamu-d4.01-9625-05.45+fsg-9625-02.117.img
  inflating: shamu-n6f27m/flash-all.bat
  inflating: shamu-n6f27m/flash-base.sh
  inflating: shamu-n6f27m/flash-all.sh
 extracting: shamu-n6f27m/image-shamu-n6f27m.zip
  inflating: shamu-n6f27m/bootloader-shamu-moto-apq8084-72.04.img 
$ cd shamu-n6f27m
$ unzip image-shamu-n6f27m.zip
Archive:  image-shamu-n6f27m.zip
  inflating: android-info.txt
  inflating: cache.img
  inflating: system.img
  inflating: boot.img
  inflating: recovery.img
  inflating: userdata.img
```

1. フラッシュメモリの消去

    Nexus 6のフラッシュメモリの各領域を消去します。

    ``` shell
fastboot erase boot
fastboot erase cache
fastboot erase recovery
fastboot erase system
fastboot erase userdata
```

1. ファクトリイメージのフラッシュ

    `image-shamu-n6f27m.zip`に含まれていた5つのイメージをフラッシュします。

    ``` shell
fastboot flash boot boot.img
fastboot flash cache cache.img
fastboot flash recovery recovery.img
fastboot flash system system.img
fastboot flash userdata userdata.img
```

以上で工場出荷状態へのリセットは完了です。Nexus 6を再起動すると初期設定の画面が現れますので、そのまま設定を進めるか、改めてカスタムROMをフラッシュしましょう!

### 参考文献
1. Dirty Unicorns, https://dirtyunicorns.com/
1. Factory Images for Nexus and Pixel Devices, https://developers.google.com/android/images
