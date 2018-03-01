+++
title = "FreeBSDでオーディオCDをリッピングする - CDDB編"
date = "2018-03-01T21:32:00+09:00"
categories = ["FreeBSD"]
tags = ["freebsd", "desktop", "audio", "cd", "ripping", "cddb", "editor"]
+++

注: 本記事では、Gracenote社が運営するCD情報データベース**ではなく**、有志により運営されている**[freedb](http://www.freedb.org/)のことを指してCDDBといいます**。Asunderでも、デフォルト設定でfreedbからディスクデータを取得するようになっています。

[FreeBSDでオーディオCDをリッピングする - 本編](/post/freebsd-audio-cd/)では、[Asunder](http://littlesvr.ca/asunder/)を用いたオーディオCDのリッピング手順を説明しました。

Asunderはとても便利なソフトウェア(作者さまに感謝!)なのですが、一つだけ不満があります。CDDBから取得したディスクデータの内容に不備があったり、そもそもディスクデータがCDDBに登録されていないときに、Asunderではデータの修正や新規作成を行なうことができません。

こんなときにはどうしましょうか? Windowsであれば、タグエディタの機能を備えたCDリッピングソフトウェア(例: [Exact Audio Copy](http://www.exactaudiocopy.de/en/))を用いて楽曲データを手動で追加できます。また、FreeBSDの場合でも、とりあえず楽曲吸い出しとエンコードを行なっておいて、後から[EasyTAG](https://wiki.gnome.org/Apps/EasyTAG)などのタグエディタで楽曲データを修正できます。

しかし、あるときだけWindowsでリッピングするのはめんどうだし、タグエディタも使い慣れないので、別のやり方を模索しました。AsunderがCDDBから取得したディスクデータは、テキストファイルとして保存されます。なので、これを普段から使っているテキストエディタで編集、あるいは新規作成するのが手っ取り早いという結論に達しました。

本記事では、テキストエディタを用いてCDDBのディスクデータを作成、編集する手順について説明します。

ディスクデータを手動で編集するには、対象となるディスクのIDを知らなければなりません。まずは、そのためのパッケージをインストールします。

```shell-script
pkg install cd-discid
```

ここからは、手元にあるCD ([スーパーゼビウス](https://ja.wikipedia.org/wiki/%E3%82%B9%E3%83%BC%E3%83%91%E3%83%BC%E3%82%BC%E3%83%93%E3%82%A6%E3%82%B9_(%E3%82%A2%E3%83%AB%E3%83%90%E3%83%A0)))を例に説明を進めていきます。ディスクをドライブに挿入して、Asunderでディスクデータが取得できるか確認してみましょう。

![スーパーゼビウスのディスクデータ](/img/asunder-super-xevious.png)

スーパーゼビウスはすでにデータベースに登録されていますね。では、次にディスクIDを確認します。

```shell-session
$ cd-discid /dev/cd0
2d04a104 4 150 39992 63995 72092 1187
```

いくつかデータが出力されましたが、それそれの意味は以下のとおりです。

- `2d04a104`: ディスクID
- `4`: トラック数
- `150 39992 63995 72092`: 各トラックの開始位置オフセット
- `1187`: 演奏時間(秒)

ディスクIDがわかったので、ディスクデータがファイルとして書き出されている場所を探します。下記コマンドからわかるように、**ディスクIDと同じファイル名**で保存されます。

```shell-session
$ find ~/.cddbslave -type f -name 2d04a104 -print
/home/example/.cddbslave/misc/2d04a104
```

注: CDDBにデータが登録されていないときは、対応するファイルがありませんので、既存のファイルをコピーするか、以下の説明を参考にして新規に作成してください。このとき、以下の各項目に注意をお願いします。

- ファイル名はディスクIDと等しいこと
- ファイルの文字コードはUTF-8であること
- 任意のサブディレクトリにファイルを保存すること(上記の例の場合、`misc`というサブディレクトリ)  
(ジャンルと一致していなくてもかまわないようです)

では、テキストエディタでファイル`2d04a104`を開いてみましょう。以下のような内容になっています。(右側の`#`以降は独自に追加したコメント)

```cddb
# xmcd CD database file                     # 最初の行は# xmcdで始まること
#
# Track frame offsets:
#       150                                 # トラックの開始位置オフセット
#       39992                               # (一行に1トラックずつ)
#       63995                               #
#       72092                               #
#
# Disc length: 1187 seconds                 # 演奏時間(秒)
#
# Revision: 3
# Processed by: cddbd v1.5.2PL0 Copyright (c) Steve Scherf et al.
# Submitted via: CDex 1.51
#
DISCID=2d04a104                             # ディスクID
DTITLE=ナムコ / スーパーゼビウス            # アーティスト / アルバムタイトル
DYEAR=1984                                  # リリース年
DGENRE=ゲーム音楽                           # ジャンル
TTITLE0=SUPER XEVIOUS                       # トラックタイトル
TTITLE1=GAPLUS                              # (0から始まることに注意)
TTITLE2=THE TOWER OF DRUAGA                 #
TTITLE3=SUPER XEVIOUS  (Gust Notch Mix)     #
EXTD= YEAR: 1984                            # 追加データ(もしあれば)
EXTT0=                                      #
EXTT1=                                      #
EXTT2=                                      #
EXTT3=                                      #
PLAYORDER=
.
```

意に沿うようにファイルの内容を編集します。例えば、トラックタイトルをすべてカタカナに変更して、Asunderの"CDDB Lookup"ボタンをクリックすると以下のような画面に変わります。

![スーパーゼビウスのディスクデータ編集後](/img/asunder-super-xevious-katakana.png)

エディタでの編集結果が反映されました。

ちなみに、CDDBのディスクデータファイルフォーマットは[ここ](http://ftp.freedb.org/pub/freedb/latest/DBFORMAT)で公開されていますので、興味のあるかたはご参照ください。

### 参考文献
1. Asunder, http://littlesvr.ca/asunder/
1. freedb, http://www.freedb.org/
1. Exact Audio Copy, http://www.exactaudiocopy.de/en/
1. EasyTAG, https://wiki.gnome.org/Apps/EasyTAG
1. THE FREEDB FILE FORMAT, http://ftp.freedb.org/pub/freedb/latest/DBFORMAT
