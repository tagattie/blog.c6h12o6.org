+++
title = "Bhyve on FreeBSDにWindows 10 Insider Previewをインストールする"
date = "2018-03-26T19:46:00+09:00"
categories = ["FreeBSD"]
tags = ["freebsd", "bhyve", "vm", "virtualization", "windows", "windows10"]
+++

[Bhyve上にFreeBSD-CURRENTをインストール](/post/freebsd-bhyve-freebsd-install/)では、FreeBSDが提供する仮想化機構の一つである、bhyve上にFreeBSD-CURRENTをインストールする手順を紹介しました。が、タイミングが悪かったせいか、インストールに使用したスナップショットでは、インストール自体は成功するもののブートできないという事態になってしまいました。FreeBSD-CURRENTは先端の開発ブランチですので、ビルドやブートの失敗は珍しいことではありません。

そこで、代わりに(というわけでもないですが)本記事では、bhyve上にWindows 10のInsider Previewをインストールしてみたいと思います。

基本的な手順はFreeBSD-CURRENTのときと変わりません。仮想スイッチの作成が終わっていれば、あとはVMを作成してOSをインストールする、という手順でOKです。これから新規にWindows 10をインストールしてみようというかたは、[先日の記事](/post/freebsd-bhyve-freebsd-install/)を参考にして、[仮想スイッチの作成](/post/freebsd-bhyve-freebsd-install/#仮想スイッチの作成)までを終わらせておきましょう。

bhyve上でWindows 10を動作させる際のポイントは、OSのインストール終了後に準仮想化ネットワークドライバをインストールする必要がある、というところです。(Microsoftが提供するWindowsのISOイメージには準仮想化ドライバが含まれていないため。)

では、以下の記事も参考にしながら、インストールを進めていきます。

- [bhyve Windows Virtual Machines](https://wiki.freebsd.org/bhyve/Windows)
- [FreeBSD 11.0-RELEASE の ｂhyve に Windows 7 をインストールする。](http://www.mousou.org/node/412)

### VMの作成とVM設定の調整
まず、VMを作成します。ここでは、VM名を`windows-insider`としました。

``` shell
vm create -t windows windows-insider
```

デフォルトの`windows`テンプレートでは、VMのCPU数が1、メモリサイズが2GBになっており、個人的には少々物足りませんので、設定を少し変更することにします。以下のコマンドを用いて、VMの設定ファイルをエディタで開きます。(環境変数`EDITOR`に指定されたエディタが開きます。)

``` shell
vm configure windows-insider
```

以下のようにCPU数、メモリサイズ、コンソール画面サイズなどの調整を行ないました。(追加、変更した行のみ示しています。)

``` conf
cpu=4
memory="4G"
graphics="yes"
graphics_port=5900
graphics_res="1600x900"
xhci_mouse="yes"
```

### Windows Insider ISOイメージのダウンロード
VMの作成ができましたので、次はWindows 10 Insider BuildのISOイメージをダウンロードします。以下のURLからダウンロードできます。なお、ダウンロードのためにはWindows Insiderへの登録が必要になりますので、ご承知おきください。

- [Windows Insider Preview Downloads](https://www.microsoft.com/en-us/software-download/windowsinsiderpreviewadvanced)

ダウンロードが完了したら、ISOイメージをvm-bhyveのイメージ格納用ディレクトリにコピーします。

``` shell
cp /<download>/<dir>/Windows10_InsiderPreview_Client_x64_ja-jp_17115_3.iso /mnt/tank/vms/.iso/
```

### Windowsのインストール
以下のコマンドを実行して、ISOイメージからWindowsをインストールします。

``` shell
vm install windows-insider Windows10_InsiderPreview_Client_x64_ja-jp_17115_3.iso
```

インストール手順は物理マシンへのインストールのときと同様ですが、一つだけ注意点があります。途中でネットワークに接続するステップが出てきますが、現時点ではネットワークドライバがありませんのでスキップします。(下図)

![ネットワーク接続はとりあえずスキップ](/img/bhyve/freebsd-remmina-windows-network-skip.png)

インストールが完了したら、Windowsをいったんシャットダウンしてください。

### 準仮想化ネットワークドライバのインストール
次は、準仮想化ネットワークドライバをインストールします。まず、ドライバディスクのISOイメージをダウンロードします。以下のURLからダウンロードできます。"Stable virtio-win iso"をクリックしてダウンロードしてください。

- [Creating Windows virtual machines using virtIO drivers - Direct downloads](https://docs.fedoraproject.org/quick-docs/en-US/creating-windows-virtual-machines-using-virtio-drivers.html#virtio-win-direct-downloads)

ダウンロードが完了したら、ISOイメージを**`windows-insider` VM**のディレクトリにコピーします。

``` shell
cp /<download>/<dir>/virtio-win-0.1.141.iso /mnt/tank/vms/windows-insider/
```

そして、Windows起動時にこのディスクイメージがマウントされるよう、VMの設定に一部追記します。再び、エディタで設定ファイルを開きます。

``` shell
vm configure windows-insider
```

以下の2行を追記してください。

``` conf
disk1_type="ahci-cd"
disk1_name="virtio-win-0.1.141.iso"
```

その後、VMを起動します。

``` shell
vm start windows-insider
```

Windowsにログインしたら画面右下を見てみてください。まだ、ネットワークドライバがインストールされていないので、ネットワークアイコンに×マークがついていることがわかります。(下図)

![ネットワークアイコンに×印がついている](/img/bhyve/freebsd-remmina-windows-network-error.png)

ドライバのディスクイメージがEドライブとしてマウントされていますので、ネットワークドライバを選択してインストールします。ファイル`E:\NetKVM\w10\amd64\netkvm.inf`を右クリックしてドライバをインストールしてください。(下図)

[![準仮想化ネットワークドライバをインストール](/img/bhyve/freebsd-remmina-windows-network-install-small.png)](/img/bhyve/freebsd-remmina-windows-network-install.png)

インストール完了後、再度画面右下のネットワークアイコンを確認してみてください。×印が消えていることがわかると思います。

![ネットワークアイコンの×印が消えた](/img/bhyve/freebsd-remmina-windows-network-ok.png)

以上でWindows 10 Insider Previewのインストールは終了です。

ドライバのディスクイメージをマウントするために追記した2行はもう不要ですので、VM設定ファイルから削除、あるいはコメントアウトしておいてください。

### 参考文献
1. bhyve Windows Virtual Machines, https://wiki.freebsd.org/bhyve/Windows
1. FreeBSD 11.0-RELEASE の ｂhyve に Windows 7 をインストールする。, http://www.mousou.org/node/412
1. Windows Insider Preview Downloads, https://www.microsoft.com/en-us/software-download/windowsinsiderpreviewadvanced
1. Creating Windows virtual machines using virtIO drivers, https://docs.fedoraproject.org/quick-docs/en-US/creating-windows-virtual-machines-using-virtio-drivers.html
