+++
title = "FreeBSDでオーディオCDをリッピングする - 本編"
date = "2018-02-28T21:36:00+09:00"
categories = ["FreeBSD"]
tags = ["freebsd", "desktop", "audio", "cd", "ripping", "asunder"]
+++

[FreeBSD on Intel NUC (Kaby Lake)でXorgを設定する](/post/freebsd-xorg-nuc/)では、FreeBSDをデスクトップ用途に使うための最初の一歩であるXorgの設定について説明しました。本記事では、デスクトップ機で行なう作業の一つとして、FreeBSD上でのオーディオCDのリッピングについて説明します。

### リッピング
説明といっても、ややこしい手順を踏む必要はなく、基本的には必要なパッケージをインストールするだけです。おすすめのリッピングソフトウェアは[Asunder](http://littlesvr.ca/asunder/)です。シンプルなUIと必要十分な機能をかね備えているので愛用しています。これ1つで、CDからの楽曲の吸い出し、MP3やFLACなどのファイル形式へのエンコード、CDDBからのディスクデータ(タイトル、アーティスト、曲名データ)の自動取得の機能がそろっています。

まず、パッケージをインストールしましょう。

```shell-script
pkg install asunder
```

MP3形式へのエンコードを行なう場合には`lame`コマンドが必要になりますが、特許的な問題でバイナリパッケージが提供されていないので、以下のコマンドにより、portsを手元でビルドしてインストールします。(FLAC形式へのエンコードについては、**追加のインストールは不要**です。)

```shell-script
cd /usr/ports/audio/lame && make install
```

注: 以下のFraunhofer社のサイトおよびAV Watchの記事によると、2017年4月23日にMP3の特許ライセンスプログラムが終了、つまり特許権が消滅したとのことですので、将来的にFreeBSDにおいてバイナリパッケージが提供される可能性はありそうです。

- Fraunhofer社サイト, https://www.iis.fraunhofer.de/en/ff/amm/prod/audiocodec/audiocodecs/mp3.html
- AV Watch記事, https://av.watch.impress.co.jp/docs/news/1059708.html

インストールが済んだら、さっそく以下のコマンドで起動します。もし、GnomeやKDEなどのデスクトップ環境を使用中であれば、メニューに登録されていると思いますので、そちらから起動してもOKです。

```shell-script
asunder
```

![Asunderの起動画面](/img/asunder-home-no-disc.png)

設定はほぼ必要ないのですが、楽曲ファイルを保存するフォルダと出力ファイルのエンコード形式だけは最低限設定する必要があります。設定ボタンをクリックすると設定画面が現われます。Generalタブ(下図左)とEncodeタブ(下図右)を設定します。Encodeについては、MP3形式の場合はVBRの最大ビットレート、FLAC形式の場合は最大圧縮レベルに設定するのがおすすめです。

|下図左|下図右|
|:---:|:---:|
|![General設定](/img/asunder-general.png)|![Encode設定](/img/asunder-encode.png)|

以上で設定は完了です。CDをドライブに挿入すると、自動的にCDDBからデータを取得してきて、タイトル、アーティスト、楽曲リストが表示されますので、あとは"Rip"ボタンをクリックして完了を待ちましょう。

### リプレイゲインの追加
以上で終了、でもかまわないのですが、最後に[リプレイゲイン](http://wiki.hydrogenaud.io/index.php?title=ReplayGain)を追加しておくことをおすすめします。

リプレイゲインとは、アルバムや楽曲ごとに異なる音量を均一化するための技術です。本技術によって、楽曲ごとに音量が小さすぎたり大きすぎたりして、スピーカーのボリュームをいちいち調節する、などという手間を省くことができます。

MP3にリプレイゲインを追加するために、以下のパッケージをインストールします。FLACについては、`asunder`パッケージのインストールと同時にすでに必要なコマンドがインストールされていますので追加は不要です。

```shell-script
pkg install mp3gain
```

楽曲が保存されているフォルダに`cd`して、以下のコマンドを実行してください。(`-p`, `--preserve-modtime`の各オプションはファイルの変更日時が変わらないようにするものなので、お好みで外していただいてだいじょうぶです。)

```shell-script
mp3gain -p -s r *.mp3                                 # MP3ファイルにリプレイゲインを追加
metaflac --preserve-modtime --add-replay-gain *.flac  # FLACファイルにリプレイゲインを追加
```

以上でリプレイゲインの追加は完了です。

### 参考文献
1. Asunder, http://littlesvr.ca/asunder/
1. ReplayGain, http://wiki.hydrogenaud.io/index.php?title=ReplayGain
