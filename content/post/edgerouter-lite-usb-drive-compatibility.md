+++
title = "EdgeRouter LiteとUSBメモリの相性"
date = "2018-01-28T16:57:36+09:00"
categories = ["Hardware"]
tags = ["edgerouter", "usb", "compatibility"]
+++

わが家では、フレッツ光+IIJmioでインターネットに接続しています。だいぶん前から、夕方から夜間にかけての速度低下が顕著になってきたため、PPPoE接続からIPoE+DS-Lite接続に切り替えることにしました。

DS-Liteを使うためには、対応しているルータを用意する必要がありますが、Buffalo、IO-DATA、ヤマハなどから対応製品が発売されています。以前使っていたことがあるヤマハを採用したいところですが、いかんせん価格が高いので、もうすこし調べてみました。

すると、Ubiquiti Networks社の[EdgeRouter Lite](https://www.ubnt.com/edgemax/edgerouter-lite/)という製品がDS-Liteに対応しており、Amazonで15,000円程度で入手できることがわかりました。さらに、FreeBSDの動作実績([ここ](http://rtfm.net/FreeBSD/ERL/)とか[ここ](http://www.daemonology.net/blog/2016-01-10-FreeBSD-EdgeRouter-Lite.html))もあるようです。さっそく、FreeBSDをインストールするためのUSBメモリと合わせて購入することにしました。(DS-LiteからFreeBSDを動かすことに、目的がちょっとずれてます。)

しかし、もう少し調べると、[USBメモリの相性問題](http://luxion.jp/2015/09/erlite3%E3%81%AEusb%E3%83%A1%E3%83%A2%E3%83%AA%E3%82%92%E4%BA%A4%E6%8F%9B%E3%81%97%E3%81%9F/)があることがわかりました。[Gentoo Linuxのフォーラム](https://wiki.gentoo.org/wiki/MIPS/ERLite-3#The_USB_Flash_Drive)を見ると、SandiskのCruzer Fit (SDCZ33-032G-B35)が動作実績があるとのことなので、ダメ元で、別型番ですがSDCZ33-032G-J57を購入することに。商品到着後、USBメモリとの相性を確認するため、ささっていたオリジナルのUSBメモリの内容を、購入したUSBメモリにまるごとコピーします。
```shell-session
dd if=/dev/da0 of=/dev/da1 bs=1m
```

そして、コピーしたUSBメモリをさして電源オン。問題なければ、EdgeRouter LiteのデフォルトOSであるEdgeOSが起動するはずですが、残念ながら`No device found`となってしまいました。
```other
U-Boot 1.1.1 (UBNT Build ID: 4670715-gbd7e2d7) (Build time: May 27 2014 - 11:16:22)

BIST check passed.
UBNT_E100 r1:2, r2:18, f:4/71, serial #: F09FC2123175
MPR 13-00318-18
Core clock: 500 MHz, DDR clock: 266 MHz (532 Mhz data rate)
DRAM:  512 MB
Clearing DRAM....... done
Flash:  4 MB
Net:   octeth0, octeth1, octeth2

USB:   (port 0) scanning bus for devices... 
      USB device not responding, giving up (status=0)
1 USB Devices found
       scanning bus for storage devices...
No device found. Not initialized?
 0
```

なんとかならないかと、さらに調べたところ、[EdgeRouterのフォーラム](https://community.ubnt.com/t5/EdgeMAX/New-U-Boot-image-for-better-USB-drive-compatibility/m-p/851038)で単純かつ確実と思われる解決方法を発見。USBバスをいったんリセットしてやればよかったんですね。
```other
Octeon ubnt_e100# usb reset
```
これで、無事にUSBメモリを認識してくれました。
```other
(Re)start USB...
USB:   (port 0) scanning bus for devices... 1 USB Devices found
       scanning bus for storage devices...
  Device 0: Vendor: SanDisk Prod.: Cruzer Fit Rev: 1.00
            Type: Removable Hard Disk
            Capacity: 30528.0 MB = 29.8 GB (62521344 x 512)
```

Power over Ethernetに対応しているEdgeRouter PoEにもこの問題があるようです。[同じやり方で解決](http://toshipp.hatenablog.com/entry/2017/04/09/233817)しているかたがすでにいらっしゃいますね。

### 参考文献
1. EdgeRouter Lite, https://www.ubnt.com/edgemax/edgerouter-lite/
1. FreeBSD 11.x on Ubiquiti EdgeRouter Lite, http://rtfm.net/FreeBSD/ERL/
1. FreeBSD on EdgeRouter Lite - no serial port required, http://www.daemonology.net/blog/2016-01-10-FreeBSD-EdgeRouter-Lite.html
1. ERLite3のUSBメモリを交換した, http://luxion.jp/2015/09/erlite3%E3%81%AEusb%E3%83%A1%E3%83%A2%E3%83%AA%E3%82%92%E4%BA%A4%E6%8F%9B%E3%81%97%E3%81%9F/
1. MIPS/ERLite-3, https://wiki.gentoo.org/wiki/MIPS/ERLite-3#The_USB_Flash_Drive
1. Re: New U-Boot image for better USB drive compatibility?, https://community.ubnt.com/t5/EdgeMAX/New-U-Boot-image-for-better-USB-drive-compatibility/m-p/851038
1. EdgeRouter PoE の復旧, http://toshipp.hatenablog.com/entry/2017/04/09/233817
