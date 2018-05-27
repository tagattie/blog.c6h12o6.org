+++
title = "ScrcpyでFreeBSDからAndroid端末の画面を表示・操作する"
date = "2018-05-27T21:38:00+09:00"
categories = ["FreeBSD"]
tags = ["freebsd", "android", "scrcpy", "screen", "display", "control", "mirroring", "remote"]
+++

Scrcpyというソフトウェアをご存知でしょうか?

Androidエミュレータの[Genymotion](https://www.genymotion.com/)で有名な[Genymobile社](https://www.genymobile.com/)が開発したOSS (Open Source Software)で、USBあるいはWiFi経由で接続したAndroid端末の画面をPC上に表示したり、Android端末をPCから操作することができます。Android端末へアプリをインストールする必要はありません。

- [Genymobile/scrcpy - Display and control your Android device (GitHub)](https://github.com/Genymobile/scrcpy)

日本語の簡単な紹介記事もあります。

- [Scrcpy - Android端末をPCから操作 (MOONGIFT)](https://www.moongift.jp/2018/04/scrcpy-android%E7%AB%AF%E6%9C%AB%E3%82%92pc%E3%81%8B%E3%82%89%E6%93%8D%E4%BD%9C/)

GitHubリポジトリの[README](https://github.com/Genymobile/scrcpy/blob/master/README.md)にあるとおり、Linux/Mac/Windows向けのソフトウェアなのですが、FreeBSDでも使えないかと試しにビルドしてみました。すると、驚くほどあっさりと使えてしまいました。使えるとしても、多少のソースコード変更は必要だろうと予想したのですが、ビルド手順を少し変更しただけでソースにはまったく手をつけていません。

以下、本記事ではFreeBSDにおけるScrcpyのビルド手順と、Scrcpyを使ってAndroid端末の画面を表示および操作するための手順を記録していきます。

### ビルド
以下のコマンドを実行します。sudoパッケージをインストールしていない場合、`sudo`コマンドの部分は、代わりにrootユーザでの実行をお願いします。また、この例ではディレクトリ`/home/example`以下にリポジトリをクローンしていますが、お使いの環境に合わせて読み替えをお願いします。

``` shell
sudo pkg install git meson pkgconf ffmpeg sdl2 android-tools-adb                            # 必要なパッケージのインストール
cd /home/example                                                                            # 任意のディレクトリにcd
git clone https://github.com/Genymobile/scrcpy.git                                          # GitHubリポジトリのクローンを作成
cd scrcpy                                                                                   # クローンしたディレクトリにcd
fetch https://github.com/Genymobile/scrcpy/releases/download/v1.1/scrcpy-server-v1.1.jar    # ビルド済みのサーバjarファイルをダウンロード
meson x --buildtype release --strip -Db_lto=false \
    -Dprebuilt_server=/home/example/scrcpy/scrcpy-server-v1.1.jar                           # ビルドディレクトリxを作成(xは任意の名前でOK)
cd x                                                                                        # ビルドディレクトリxにcd
ninja                                                                                       # ビルド
sudo ninja install                                                                          # インストール
```

一連のコマンドを実行すると、以下の二つのファイルが`/usr/local`以下にインストールされます。

- `/usr/local/bin/scrcpy`
- `/usr/local/share/scrcpy/scrcpy-server.jar`

### 実行
まず、USBケーブルを用いてAndroid端末をFreeBSDマシンに接続します。Android端末が認識されていることを、以下のコマンドを実行して確認します。(参考: [FreeBSDからAndroid端末にADBで接続する](/post/freebsd-android-adb/))

``` shell-session
$ adb devices
List of devices attached
xxxxxxx	device              # xxxxxxxはAndroid端末のシリアル番号
```

端末が認識されていれば、あとは以下のコマンドを実行すればOKです。

``` shell
scrcpy
```

コマンドを実行すると、下図右側のような、Android端末の画面を表示するウィンドウが現れますので、あとはお好みの操作を行なってください。(ちなみに、下図左側のターミナルは「FreeBSD上で動いてます」という一応の証拠(?)画像です。)

[![FreeBSD - Scrcpy](/img/freebsd/freebsd-scrcpy-small.png)](/img/freebsd/freebsd-scrcpy.png)

操作は基本的にマウスの左ボタンを使って行ないます。クリックがタップ、ドラッグがスワイプになります。その他にも操作のためのショートカットがいくつか定義されていますので、詳しくは以下のURLを参照してください。

- [Shortcuts (GitHub)](https://github.com/Genymobile/scrcpy#shortcuts)

### WiFi経由での接続
Scrcpyでは、USBの代わりにWiFi接続を使うこともできます。

- [Open Source Project: Scrcpy now works wirelessly! (Genymotion)](https://www.genymotion.com/blog/open-source-project-scrcpy-now-works-wirelessly/)
- [Wi-Fi を介した端末への接続 (Android Developers)](https://developer.android.com/studio/command-line/adb#wireless)

WiFi接続のためには下準備が必要です。いったん、USBケーブルを用いてAndroid端末をFreeBSDマシンに接続してください。そして、以下のコマンドを実行します。すると、Android端末が指定されたポートで接続を待ち受ける状態になります。

``` shell
adb tcpip 5555
```

上記コマンドを実行したら、USBケーブルを外してもだいじょうぶです。次に、以下のコマンドを実行して、TCP/IP経由でFreeBSDマシンからAndroid端末に接続してください。

``` shell
adb connect <Android端末のIPアドレス>:5555
```

USB接続のときと同様、正しく認識されているかを念のため確認しておきましょう。

``` shell-session
$ adb devices
List of devices attached
<Android端末のIPアドレス>:5555	device
```

接続が確認できたらあとはUSBの場合と同じです。`scrcpy`コマンドを実行してください。

### 参考文献
1. Genymobile/scrcpy - Display and control your Android device, https://github.com/Genymobile/scrcpy
1. Scrcpy - Android端末をPCから操作, https://www.moongift.jp/2018/04/scrcpy-android%E7%AB%AF%E6%9C%AB%E3%82%92pc%E3%81%8B%E3%82%89%E6%93%8D%E4%BD%9C/
1. Open Source Project: Scrcpy now works wirelessly!, https://www.genymotion.com/blog/open-source-project-scrcpy-now-works-wirelessly/
1. Wi-Fi を介した端末への接続, https://developer.android.com/studio/command-line/adb#wireless
