+++
title = "USBTopでUSBデバイスのアクセス速度を監視する(いろんなtop系コマンドを使ってみる その1)"
date = "2018-09-01T19:46:00+09:00"
categories = ["FreeBSD"]
tags = ["freebsd", "top", "usbtop"]
+++

Linux, macOS, あるいはBSD系OSといったUnix-like OSをお使いのみなさん、topコマンドはお好きですか? 稼働中のプロセスを一覧表示したり、おのおののプロセスのPID (Process ID)、CPU使用率、メモリ使用量など、システムリソースに関する情報を総合的に確認できるコマンドです。

ご多分にもれず(?)、わたしもtopが大好きです。これといった理由もないのにtopを実行したり、わりと負荷が高いコマンドであることがわかっているにもかかわらず、常にどこかのターミナルウィンドウでtopを起動していたりします。topといえば、知名度、使用頻度ともにUnix系OSの中ではトップ10に入る有名コマンドではないでしょうか。(根拠があるわけではなく勝手な推測ですが。)

topが有名なので(?)、xxxtopというような、topから名前を借りたコマンドがUnix系OSにはいろいろと存在します。

そこで、この「いろんなtop系コマンドを使ってみる」シリーズでは、topから名前を拝借したコマンドをいくつか紹介していきたいと思います。一回めの本記事では、USBデバイスのアクセス速度をリアルタイムで表示する[usbtop](https://github.com/aguinet/usbtop)コマンドを紹介します。(以下、FreeBSDマシンへのインストールを想定します。)

まず、以下のコマンドを実行してパッケージをインストールしましょう。

``` shell
pkg install usbtop
```

インストールが終わったらさっそく起動してみます。

``` shell-session
$ usbtop
Error while capturing traffic from usbus0: usbus0: You don't have permission to capture on that device ((cannot open device) /dev/bpf: Permission denied)
FATAL ERROR: couldn't open any USB buses with pcap. Did you load the usbmon module (sudo modprobe usbmon)?
You might also need to run this software as root.
```

おっと、デバイスファイルが開けない旨のエラーメッセージが出て起動しませんね。では、root権限で再度起動してみることにします。

``` shell-session
$ sudo usbtop
```

無事起動しました。しかし、Bus IDが表示されるだけで、そのバスに属しているデバイスのリストが表示されませんね。USBキーボードやUSBマウスを操作してみるものの、やはりデバイスが表示されません。

そこで、手もとにあったUSBメモリを挿入し、`dd`コマンドを用いてUSBメモリからのデータ読み出しを行なってみました。ここでようやく、以下のようなデバイスリストおよびアクセス速度が表示されました。(ある程度トラフィックがないと動作しないようになっているのですかね?) 表示の更新頻度を指定するコマンドオプションはありませんが、目視した限りでは500msくらいごとに更新されるようです。

``` shell-session
Bus ID 0 (USB bus number 0)     To device       From device
  Device ID 0 :                 0.00 kb/s       0.00 kb/s
  Device ID 2 :                 0.00 kb/s       35128.08 kb/s
  Device ID 3 :                 0.00 kb/s       28.36 kb/s
```

手もとの物理デバイスの操作状況と照らし合わせると、

- Device ID 2 - USBメモリ
- Device ID 3 - USBマウス

となっているようです。

しかし、デバイスからの読み取り(From device)については速度が表示されるものの、デバイスへの書き込み(To device)については0.00のまま動きません。

試しに`dd`コマンドで、今度はUSBメモリへの書き込みを行なってみると…? あれ、やっぱりFrom deviceのほうしか速度が表示されません(汗)。読み書きが区別されず、両方の速度の合計が読み出しとして表示されているのでしょうかね。Linuxでは問題なく動作するんだと思いますが、FreeBSD (11.2-RELEASE)だとちょっと不具合がありますね。

コマンドを終了するには`ctrl` + `c`を押下します。

<!--
リアルタイムで表示が更新されますので、大きなファイルをUSBメモリから、あるいはUSBメモリへコピーするようなときに、アクセス状況を確認するのに役立ちそうですね。
-->

ところで、さきほどDevice ID 2がUSBメモリ、ID 3がUSBマウスであると見当をつけました。しかし、`usbtop`コマンドだけでは、実際にどの物理デバイスがどのIDに対応しているかが確認できません。少し調べたところでは、ここで表示されるDevice IDは`lsusb`コマンドで表示されるものに一致するようだということがわかりました。

そこで、[usbutils](https://github.com/gregkh/usbutils)パッケージをインストールして確認してみることにしました。

``` shell
pkg install usbutils
```

usbutilsパッケージに含まれる`lsusb`コマンドを実行します。

``` shell-session
$ sudo lsusb
Bus /dev/usb Device /dev/ugen0.9: ID 0a6b:000f Green House Co., Ltd FlashDisk
Bus /dev/usb Device /dev/ugen0.8: ID 0bda:0411 Realtek Semiconductor Corp. 
Bus /dev/usb Device /dev/ugen0.7: ID 8087:0a2b Intel Corp. 
Bus /dev/usb Device /dev/ugen0.6: ID 046d:c52b Logitech, Inc. Unifying Receiver
Bus /dev/usb Device /dev/ugen0.5: ID 067b:2303 Prolific Technology, Inc. PL2303 Serial Port
Bus /dev/usb Device /dev/ugen0.4: ID 0403:6001 Future Technology Devices International, Ltd FT232 Serial (UART) IC
Bus /dev/usb Device /dev/ugen0.3: ID 0bda:5411 Realtek Semiconductor Corp. 
Bus /dev/usb Device /dev/ugen0.2: ID 0853:0104 Topre Corporation 
Bus /dev/usb Device /dev/ugen0.1: ID 0000:0000
```

あれ、FreeBSDで実行するとDevice IDの欄は数字ではなく、デバイスファイルのパスが表示されるのですね。これではusbtopとの対応が取れない(汗)。

う〜ん…。

以上、USBデバイスのアクセス速度を確認できるusbtopコマンドの紹介でした(汗)。

### 参考文献
1. usbtop, https://github.com/aguinet/usbtop
1. usbutils, https://github.com/gregkh/usbutils
