+++
title = "Chromium OSでバッテリ駆動時間は伸びるのか? NEC LaVie Zで試す - ビルド用VM構築編"
date = "2018-04-03T19:55:00+09:00"
categories = ["ChromiumOS"]
tags = ["chromiumos", "chromeos", "nec", "lavie", "chromebook", "battery", "freebsd", "bhyve", "ubuntu"]
+++

[まえがき](/post/chromiumos-self-build-intro/)では、Chromium OSを自分でビルドしてLaVie Zで試してみよう、と思い立った経緯について紹介しました。また、PCでお手軽にChromium OSを試せる、Neverware社の[CloudReady](https://www.neverware.com/freedownload/)についても触れました。

Chromium OSをビルドする環境について、[Chromium OS Developer Guide](https://www.chromium.org/chromium-os/developer-guide)では[Ubuntu](https://www.ubuntu.com/) 14.04の使用を推奨しています。筆者の常用しているマシンではFreeBSDが稼働しているので、本マシン上で直接ビルドすることはできません。そこで、[先日紹介](/post/freebsd-bhyve-freebsd-intro/)した、FreeBSDの仮想化機構[bhyve](https://wiki.freebsd.org/bhyve)を活用します。まずは、Chromium OSをビルドするための環境として、Ubuntu 14.04が動作する仮想マシン(Virtual Machine, VM)をbhyve上に構築したいと思います。

以下、本記事ではbhyve上にUbuntu 14.04をセットアップする手順を説明します。仮想スイッチを作成するところまでは、[先日の記事](/post/freebsd-bhyve-freebsd-install/)を参考にしてください。

### VMの作成とVM設定の調整
まず、VMを作成します。ここではVM名を`cros-build`としました。また、ディスクサイズは64GBにしています。

``` shell
vm create -t ubuntu -s 64G cros-build
```

デフォルトの`ubuntu`テンプレートはかなり控えめにリソースが設定されており、Chromium OSをビルドするには不足です。そこで、十分にリソースが割り当てられるよう、設定を調整します。

``` shell
vm configure cros-build
```

以下のようにCPU数、メモリサイズを調整しました。[Chromium OS Developer Guide](https://www.chromium.org/chromium-os/developer-guide)のなかで、メモリサイズについては「Chromeのリンクを行なうために8~28GB程度必要」とありますので、これを参考にして真ん中あたりの16GBとしています。(手もとのマシンはメモリを32GB積んでいるので、VMに16GB割り当てることも可能ですが、これはビルドする環境を選びますね…。)

``` conf
cpu=4
memory="16G"
```

### Ubuntu 14.04のISOイメージダウンロード
VMの作成ができましたので、次はUbuntu 14.04のISOイメージをダウンロードします。以下のURLからダウンロードできます。

- [Ubuntu 14.04.5 LTS (Trusty Tahr)](http://ftp.riken.jp/Linux/ubuntu-releases/trusty/)

今回、Ubuntuの用途はChromium OSのビルド専用ですのでGUIは不要です。そこで、よりサイズの小さいserver版をダウンロードします。(ファイル名: `ubuntu-14.04.5-server-amd64.iso`)

``` shell
vm iso http://ftp.riken.jp/Linux/ubuntu-releases/trusty/ubuntu-14.04.5-server-amd64.iso
```

### Ubuntuのインストール
以下のコマンドを実行して、ISOイメージからUbuntuをインストールします。

``` shell
vm -f install cros-build ubuntu-14.04.5-server-amd64.iso
```

上記のコマンドを実行すると、VMのシリアルコンソールに接続されますので、あとは画面の指示にしたがってインストールを進めていけばOKです。途中、デフォルトと異なる設定を行なったところで、重要なのは以下の一点です。

ディスクのパーティションを設定する画面です。デフォルトでは"Guided - use entire disk and set up LVM"が選択されていますが、このままインストールを進めると、完了後の再起動でGRUBのプロンプトから進まなくなってしまいました。そこで、代わりに"**Guided - use entire disk**"を選択してインストールを行ないます。(下図)

![Ubuntuディスクパーティションの設定画面](/img/bhyve/ubuntu-installer-disk-partition.png)

その他について、特に困るところはないと思います。

### 参考文献
1. Chromium OS Developer Guide, https://www.chromium.org/chromium-os/developer-guide
1. Ubuntu, The leading operating system for PCs, IoT devices, servers and the cloud, https://www.ubuntu.com/
1. bhyve, the BSD Hypervisor, https://wiki.freebsd.org/bhyve
