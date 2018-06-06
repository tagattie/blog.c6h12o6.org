+++
title = "FreeBSD on Intel NUC (Kaby Lake)でグラフィックス・アクセラレーションを使う"
date = "2018-06-05T21:32:47+09:00"
draft = true
categories = ["FreeBSD"]
tags = ["freebsd", "nuc", "kabylake", "xorg", "desktop", "accelerated", "graphics", "acceleration", "drm", "kernel", "module"]
+++

自宅のメインデスクトップ機、Intel NUCキット[NUC7i5BNH](https://www.intel.co.jp/content/www/jp/ja/products/boards-kits/nuc/kits/nuc7i5bnh.html)でFreeBSDを使っています。[先日の記事](/post/freebsd-xorg-nuc/)では、本マシンでXorgが使えるように設定を行ないました。その記事の最後に「11.2-RELEASEではKaby Lake世代のCPUでもアクセラレーテッド・グラフィックスが使えるようになりそう」と書きました。(正確には「Broadwell世代以降のCPUでも」でしたね。)

さて、6月2日に、11.2-RELEASEのリリース候補版である11.2-RC1が利用可能になりました。リリース版まで待ちきれなかったので、11.2-RC1でグラフィックス・アクセラレーションを実現するドライバ`drm-next-kmod`を試しました。本記事ではその手順を記録していきます。以下の記事も参考にしながら進めます。

- [FreeBSD and Intel Video Cards (srobb.net)](http://srobb.net/freebsdintel.html)

注: グラフィックス・アクセラレーションを実現するドライバ(カーネルモジュール)には、`drm-next-kmod`および`drm-stable-kmod`の二つがあります。前者はより開発の先端に近いバージョン、後者はより安定指向のバージョンだと考えればOKです。

### ビルドおよびインストール
いまのところ、FreeBSD 11向けにはビルド済みのパッケージが提供されていません。(11.2-RELEASEのリリース後に提供されると思います。) したがって、現時点ではportを用いて自前でビルドする必要があります。

まず、ビルドを行なうマシンを11.2-RC1(以降)に更新しましょう。本記事では、ベースシステムの更新については扱いませんので、下記11.2-RC1の[アナウンスメール](https://lists.freebsd.org/pipermail/freebsd-stable/2018-June/089053.html)の`=== Upgrading ===`の部分を参照するなどして更新を行なってください。

- [FreeBSD 11.2-RC1 Now Available (The FreeBSD Project)](https://lists.freebsd.org/pipermail/freebsd-stable/2018-June/089053.html)

ベースシステムの更新が終わったら、`drm-next-kmod`(あるいは`drm-stable-kmod`)のビルドに進みましょう。

本portをビルドするためには、FreeBSDベースシステムのソースコードが必要になります。すでに、ソースが`/usr/src`に展開されて**いない**場合、以下のコマンドを実行してソースを取得します。

``` shell
sudo svnlite checkout https://svn.freebsd.org/base/releng/11.2 /usr/src
```

次は、portsのファイル一式を取得します。以下のコマンドを実行してください。もし、portsファイルを取得するのが今回初めてという場合は、後者のコマンドは(`sudo portsnap update`の代わりに)**`sudo portsnap extract`**を実行してください。

``` shell
sudo portsnap fetch
sudo portsnap update
```

さあ、カーネルモジュールをビルド、インストールしましょう。

``` shell
cd /usr/ports/graphics/drm-next-kmod
sudo make install clean
```

以上で、`drm-next-kmod`のビルドとインストールは終了です。

### 設定
カーネルモジュールのインストール時に表示されるメッセージにしたがって設定を行ないます。本メッセージを再度表示するには、以下のコマンドを実行してください。

``` shell
pkg info -D drm-next-kmod
```

ベースシステムに含まれるモジュール(`/boot/kernel/i915kms.ko`)ではなく、いまインストールしたモジュール(`/boot/modules/i915kms.ko`)が起動時にロードされるよう、`/etc/rc.conf`に設定を追加します。以下のコマンドを実行してください。

``` shell
sudo sysrc kld_list+="/boot/modules/i915kms.ko"
```

また、グラフィックス環境を使用するユーザは`video`グループのメンバーでなければいけませんので、以下のコマンドを実行して該当するユーザをすべて追加してください。(以下の例では`example`ユーザを追加しています。本ユーザはご使用の環境に合わせて読み替えをお願いします。)

``` shell
sudo pw groupmod video -M example
```

次節で述べるドライバのアンインストールを行なえば、Xorgのビデオドライバに関する設定は特に不要だと思います。何らかの理由で不要になったビデオドライバを残しておきたい場合は、以下のようなファイルを用意して、`modesetting`ドライバの使用を明示的に指定します。

- `/usr/local/etc/X11/xorg.conf.d/driver-modesetting.conf`

    ``` conf
    Section "Device"
        Identifier  "Intel Card with Modesetting Driver"
        Driver      "modesetting"
    EndSection
    ```

以上でXorgを使用する準備が完了しました。

### 不要ドライバのアンインストール
本節の手順は必須ではありませんが、意図しないドライバがロードされることがないよう、不要なものは削除しておくと安心です。以下のコマンドを実行すると、インストールされているビデオドライバが全て削除されます。

``` shell
sudo pkg delete 'xf86-video-*'
```

前節で設定した`modesetting`ドライバは`xorg-server`パッケージに含まれていますので、上記コマンドで削除されることはありません。心配ご無用です。

最後にFreeBSDを再起動してください。

あとは、ちょっとドキドキしながら、ディスプレイマネージャーが起動してくるのを待つか`xinit`(or `startx`)してみてください!

### 参考文献
1. インテル® NUC キット NUC7i5BNH, https://www.intel.co.jp/content/www/jp/ja/products/boards-kits/nuc/kits/nuc7i5bnh.html
1. FreeBSD and Intel Video Cards, http://srobb.net/freebsdintel.html
1. FreeBSD 11.2-RC1 Now Available, https://lists.freebsd.org/pipermail/freebsd-stable/2018-June/089053.html
