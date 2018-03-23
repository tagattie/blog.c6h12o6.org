+++
title = "Bhyve on FreeBSDにFreeBSD-CURRENTをUEFIモードでインストールする - まえがき"
date = "2018-03-22T09:38:47+09:00"
draft = true
categories = ["FreeBSD"]
tags = ["freebsd", "bhyve", "vm", "kernel", "virtualization", "current", "uefi", "vm-bhyve"]
+++

Linuxには[KVM (Kernel-based Virtual Machine)](https://www.linux-kvm.org/)というカーネルレベルの仮想化基盤があります。

FreeBSDは、古く(マニュアルによれば4.0の頃)からコンテナ型の仮想化機能としてjailを提供しており、広く使われています。しかし、jailはあくまでも、仮想化されたFreeBSDサブシステムを動作させるものであって、LinuxやWindowsを動かしたい、というニーズには応えられません。

FreeBSDにはこれまでKVMに相当する仮想化機能がなかったため、LinuxやWindowsなどの仮想マシンを構築する場合は、[VirtualBox](https://www.virtualbox.org/)を使用するのが一般的になっています。VirtualBoxは、手軽に仮想マシンを扱えるGUIと、コマンドラインから仮想マシンを管理、制御するためのCLIをともに備えるすぐれたソフトウェアです。

こういった状況で、LinuxのKVMに相当するカーネルレベルの仮想化基盤として、bhyveが新たに開発されました。KVMやVirtualBoxがGPLv2ライセンスをとるのに対し、bhyveはBSDライセンスを採用しています。また、10.0-RELEASEからはFreeBSDのベースシステムに取り込まれています。

 ちなみに、bhyveとはもともとBSD Hypervisorの省略形で、beehive (ハチの巣箱)のもじりになっているようです。ハチが仮想マシンでハチの巣が仮想化基盤ということですね。(参考: [bhyve, the BSD Hypervisor](https://wiki.freebsd.org/bhyve))

ところで、以下の記事では、VirtualBoxと比較してのbhyveの長所は軽量かつベースシステムに組み込まれていること、短所は動作する環境を選ぶ(amd64アーキテクチャのみで、CPUの仮想化サポートが必要)ことだと述べられています。

- [FreeBSD で仮想化ホストをする VirtualBox と bhyve の比較](http://uyota.asablo.jp/blog/2018/01/10/8767051)

手もとの環境では上記の短所は問題にならないので、bhyveでの仮想マシン構築を試してみることにします。まず手始めに、FreeBSD 11.1-RELEASE上でFreeBSD-CURRENTを動作させてみようと思います。

FreeBSDハンドブックの[bhyveの節](https://www.freebsd.org/doc/handbook/virtualization-host-bhyve.html)を参照すると、仮想マシンの管理は`bhyve`および`bhyvectl`というコマンドを用いて行なうことがわかります。また、仮想マシン上でFreeBSDを動作させるのであれば、`/usr/share/examples/bhyve/vmrun.sh`というサンプルスクリプトを使うこともできるようです。が、ちょっと大変そう、という印象です。

そこで、もう少し簡易にbhyve仮想マシンを管理、運用できるツール的なものはないかと探してみたところ、[vm-bhyve](https://github.com/churchers/vm-bhyve)を見つけました。シェルスクリプトなのでインストールが簡単ですし、小規模での運用には必要十分な機能を提供しているようです。以下のような参考記事もあります。

- [vm-bhyveでお手軽にbhyveを使う](http://decomo.info/wiki/freebsd/bhyve/freebsd_11.1r_use_vm-bhyve)
- [FreeBSD bhyve + vm-bhyveでゲストにFreeBSD環境を入れてみる](https://blog.bixr.com/2016/06/1090/)

本記事ではここまでにして、次回は、vm-bhyveを使ってFreeBSD-CURRENT仮想マシンを構築する、具体的な手順について紹介したいと思います。

### 参考文献
1. KVM, https://www.linux-kvm.org/
1. VirtualBox, https://www.virtualbox.org/
1. bhyve, the BSD Hypervisor, https://wiki.freebsd.org/bhyve
1. FreeBSD で仮想化ホストをする VirtualBox と bhyve の比較, http://uyota.asablo.jp/blog/2018/01/10/8767051
1. FreeBSD as a Host with bhyve, https://www.freebsd.org/doc/handbook/virtualization-host-bhyve.html
1. vm-bhyve - FreeBSD Bhyve VM Management, https://github.com/churchers/vm-bhyve
1. vm-bhyveでお手軽にbhyveを使う, http://decomo.info/wiki/freebsd/bhyve/freebsd_11.1r_use_vm-bhyve
1. FreeBSD bhyve + vm-bhyveでゲストにFreeBSD環境を入れてみる, https://blog.bixr.com/2016/06/1090/
