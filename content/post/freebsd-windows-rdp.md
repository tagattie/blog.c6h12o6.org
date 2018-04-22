+++
title = "FreeBSDからWindows 10へリモートデスクトップで接続する"
date = "2018-03-22T16:25:00+09:00"
categories = ["FreeBSD"]
tags = ["freebsd", "windows", "rdp", "vnc", "remote", "desktop", "remmina"]
+++

自宅のメインデスクトップ機でFreeBSDを使っています。先日、デスクトップ機で行なう作業の一つとして、[オーディオCDのリッピング](/post/freebsd-audio-cd/)について説明しました。いつもはFreeBSDを使っているのですが、サブ機にWindowsマシンもありますので、ときどきはWindowsを使う機会もあります。そんなとき、わざわざコンソールを使うのが面倒でリモートデスクトップを使いたくなることがあります。

本記事では、FreeBSDマシンからWindowsマシンへリモートデスクトップで接続する手順を説明します。

説明といってもややこしい手順を踏む必要はなく、基本的には必要なパッケージをインストールするだけです。おすすめのリモートデスクトップ接続ソフトウェアは[Remmina](https://www.remmina.org/)です。リモートデスクトップ(RDP, Remote Desktop Protocol)だけでなく、VNC (Virtual Network Computing)にも対応しているので、KVMやbhyve上の仮想マシンコンソールに接続するときなどにも使えるすぐれものです。

注: ご存知かと思いますが、リモートデスクトップ接続のためには、接続対象マシンのWindows 10が[Home Editionではなく**Pro Edition**以上](https://www.microsoft.com/ja-jp/windows/compare)でなければなりません。ご注意ください。Windows 10 Home Editionでもリモートデスクトップ接続を可能にする方法があるようですが、本記事では扱いません。

以下のURLにLinux向けの詳しい説明がありますので、これを参考にしながら設定を進めていきます。

- [LinuxからWindows10へリモートデスクトップ(Remmina)](https://gato.intaa.net/linux/remmina)

### Windowsマシンでの準備
ます、Windowsマシンのほうでリモートデスクトップを有効にします。

スタートメニュー→歯車アイコンをクリックして、Windowsの設定を表示します。

![Windowsメニューから歯車アイコンを選択](/img/windows/windows-menu-gear-icon.png)

システムアイコンをクリックします。

![システムアイコンを選択](/img/windows/windows-system-settings.png)

リモートデスクトップをクリックし、右側にある「リモートデスクトップを有効にする」をオンにします。

![リモートデスクトップの設定](/img/windows/windows-remote-desktop-settings.png)

以上で、Windowsマシン側の設定は完了です。

### Remminaのインストールとリモートデスクトップ接続
ここからはFreeBSDマシンでの作業になります。まずは、パッケージをインストールしましょう。

``` shell
pkg install remmina remmina-plugins
```

インストールが済んだら、さっそく以下のコマンドで起動します。もし、GnomeやKDEなどのデスクトップ環境を使用中であれば、メニューに登録されていると思いますので、そちらから起動してもOKです。

``` shell
remmina
```

起動すると以下の画面になりますので、+(プラス)アイコンをクリックして接続先情報の設定画面を表示させます。

![Remmina起動画面](/img/remmina/freebsd-remmina-startup.png)

接続先に関する基本的な情報を入力します。

![Remmina接続先の設定](/img/remmina/freebsd-remmina-connect-to.png)

次に「高度な設定」タブをクリックして、画質とサウンドの再生(再生しない、ローカルマシンで再生、リモートマシンで再生、など)を設定します。

![Remmina接続先の設定(高度)](/img/remmina/freebsd-remmina-connect-to-advanced.png)

設定が終了したら"Save"をクリックします。いま作成したエントリが、接続先の候補に追加されました。

![Remmina接続先リスト](/img/remmina/freebsd-remmina-host-list.png)

接続したいエントリを選択して右クリックすると、メニューが現れますので、"Connect"を選択すればOKです。

![Remmina Windows接続画面](/img/remmina/freebsd-remmina-connected.png)

無事、Windows 10にリモートデスクトップで接続することができました。

<!-- しかし、困ったことに日本語の入力ができないですね。日本語入力モードで、"a", "i", "u", "e", "o"の順にキーを押しても「あいうえお」にならず「aiueo」になってしまいます。う〜ん、これはどうしたものか。日本語配列のキーボードでもちゃんとキートップの刻印どおりの文字が入力されてすばらしいのですが、ローマ字だけでかなが出ない…。 -->

<!-- 解決策をお持ちのかたがいらっしゃいましたら、お知らせいただけるとさいわいです。 -->

### 参考文献
1. Remmina - A FEATURE RICH REMOTE DESKTOP APPLICATION FOR LINUX AND OTHER UNIXES, https://www.remmina.org/
1. Windows 10 エディション別 比較表, https://www.microsoft.com/ja-jp/windows/compare
1. LinuxからWindows10へリモートデスクトップ(Remmina), https://gato.intaa.net/linux/remmina

### イメージクレジット
本記事において、Windowsのデスクトップ壁紙として使用している画像は、ロシア・キーロフスク在住の[Fox Grom氏](https://vk.com/id153817456)によるものです。氏のアルバムは以下のURLで閲覧できます。

- [Fox's photos (VK)](https://vk.com/albums153817456)
