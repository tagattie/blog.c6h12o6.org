+++
title = "FreeBSD on Intel NUC (Kaby Lake)でXorgを設定する"
date = "2018-02-26T21:04:00+09:00"
categories = ["FreeBSD"]
tags = ["freebsd", "nuc", "kabylake", "xorg", "desktop"]
+++

自宅のメインデスクトップ機でFreeBSDを使っています。これまで、Zotac社のミニベアボーン[ZBOX nano ID64](https://www.ask-corp.jp/products/zotac/mini-pc/zbox-nano/zbox-nano-id64.html) (Ivy Bridge世代)を使っていたのですが、電源が突然落ちる事象に見舞われるようになりました。そこで、代替機としてIntel社のNUCキット[NUC7i5BNH](https://www.intel.co.jp/content/www/jp/ja/products/boards-kits/nuc/kits/nuc7i5bnh.html) (Kaby Lake世代)を購入しました。

デスクトップ用途なので、グラフィックスシステムとしてX Window System (Xorg)を使います。Xorgの設定は、ずっと前に`X -configure`で生成したひな型を、手直ししつつ使ってきていたのですが、これを機に見直すことにしました。

こういうときに頼りになるのが、FreeBSDに関することがらを網羅したドキュメント「FreeBSDハンドブック」です。

- [FreeBSD Handbook (英語版)](https://www.freebsd.org/doc/handbook/)
- [FreeBSDハンドブック(日本語版)](https://www.freebsd.org/doc/ja_JP.eucJP/books/handbook/)

さっそく、[5.4. Xorgの設定](https://www.freebsd.org/doc/ja_JP.eucJP/books/handbook/x-config.html)の節を見てみました。なんと、いまでは**何も設定しなくていい**んですね、知らなかった…。

ビデオカードのドライバは、とりあえず以下の3つをインストールしました。

```shell-session
$ pkg info 'xf86-video-*'
xf86-video-intel-2.99.917.20180111
xf86-video-scfb-0.0.4_5
xf86-video-vesa-2.3.4_1
```

`intel`ドライバはハンドブックによるとIvy Bridge世代までを、[FreeBSD Graphics Wiki](https://wiki.freebsd.org/Graphics)でもHaswell世代までをサポートとあるので、Kaby Lake世代にはダメだろうけれども一応インストール。`scfb`ドライバを実際に使うつもりで、`vesa`ドライバは万が一`scfb`ドライバが使えなかったときのためにインストールしました。(ちなみに、試してみましたが、`intel`ドライバはやはりKaby Lake世代には対応していませんでした。)

さて、`/usr/local/etc/X11`以下にある、既存の設定ファイルをバックアップしておいてXorgを起動してみます。

```shell-session
$ xinit

X.Org X Server 1.18.4
Release Date: 2016-07-19
X Protocol Version 11, Revision 0
Build Operating System: FreeBSD 11.1-RELEASE-p5 amd64 
Current Operating System: FreeBSD example.com 11.1-RELEASE-p6 FreeBSD 11.1-RELEASE-p6 #0 r326738: Fri Dec 15 18:23:19 JST 2017     root@example.com:/usr/obj/usr/src/sys/EXAMPLE amd64
Build Date: 13 December 2017  04:27:27PM
 
Current version of pixman: 0.34.0
	Before reporting problems, check http://wiki.x.org
	to make sure that you have the latest version.
Markers: (--) probed, (**) from config file, (==) default setting,
	(++) from command line, (!!) notice, (II) informational,
	(WW) warning, (EE) error, (NI) not implemented, (??) unknown.
(==) Log file: "/var/log/Xorg.0.log", Time: Sun Feb 25 18:39:11 2018
(==) Using default built-in configuration (39 lines)
scfb trace: probe start
scfb trace: probe done
(EE) 
Fatal server error:
(EE) Cannot run in framebuffer mode. Please specify busIDs        for all framebuffer devices
(EE) 
(EE) 
Please consult the The X.Org Foundation support 
	 at http://wiki.x.org
 for help. 
(EE) Please also check the log file at "/var/log/Xorg.0.log" for additional information.
(EE) 
(EE) Server terminated with error (1). Closing log file.
xinit: giving up
xinit: unable to connect to X server: Connection refused
xinit: server error
```

む、、、エラーが出て起動しませんね。"Please specify busIDs for all framebuffer devices"というメッセージが出ていますが、そもそもデバイスの指定すらしていないので、以下のような最低限の設定ファイルを用意します。

- `/usr/local/etc/X11/xorg.conf.d/driver-scfb.conf`

    ```conf
Section "Device"
        Identifier      "Scfb Card"
        Driver          "scfb"
EndSection
```

再度`xinit`を実行すると、無事xtermが表示されました。すごい、ほとんど何もしなくても本当に使えました!

JIS配列のキーボードを使っていますので、もう少しだけ設定を追加します。(デフォルト設定ではASCII配列となります。) `XkbOptions` の設定はお好みで追加してください。

- `/usr/local/etc/X11/xorg.conf.d/keyboard-jp106.conf`

    ```conf
Section "InputClass"
        Identifier      "Keyboard0"
        Driver          "keyboard"
        MatchIsKeyboard "On"
        Option          "XkbRules"      "xorg"
        Option          "XkbModel"      "jp106"
        Option          "XkbLayout"     "jp"
        Option          "XkbOptions"    "compose:rctrl"
        Option          "XkbOptions"    "ctrl:nocaps"
EndSection
```

最後に、ご参考として現在使用中のハードウェア一式を挙げておきます。

- PC: Intel NUC7i5BNH
- ディスプレイ: 三菱電機 Diamondcrysta Wide RDT231WM-S(BK)
- キーボード: 東プレ REALFORCE108UBK
- マウス: ロジクール マラソンマウス M705

追伸:  
以下のツイートによると`graphics/drm-next-kmod`が11-STABLEでも使えるようになったとのこと。[11.2-RELEASE](https://www.freebsd.org/releases/11.2R/schedule.html)ではKaby Lake世代でもAccelerated Graphicsが使えるようになりそうです。
{{< tweet 965504184375128064 >}}

### 参考文献
1. ZBOX nano ID64シリーズ, https://www.ask-corp.jp/products/zotac/mini-pc/zbox-nano/zbox-nano-id64.html
1. インテル® NUC キット NUC7i5BNH, https://www.intel.co.jp/content/www/jp/ja/products/boards-kits/nuc/kits/nuc7i5bnh.html
1. 5.4. Xorg の設定, FreeBSDハンドブック, https://www.freebsd.org/doc/ja_JP.eucJP/books/handbook/x-config.html
1. FreeBSD Graphics, https://wiki.freebsd.org/Graphics
1. FreeBSD 11.2 Release Process, https://www.freebsd.org/releases/11.2R/schedule.html
