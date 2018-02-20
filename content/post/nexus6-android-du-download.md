+++
title = "Nexus 6にAndroid Oreo (8.1)をインストールする - ファイル準備編"
date = "2018-02-18T11:49:00+09:00"
categories = ["Android"]
tags = ["android", "oreo", "nexus6", "dirtyunicorns"]
+++

[FreeBSDからAndroid端末にADBで接続する](/post/freebsd-android-adb/)では、FreeBSDマシンからAndroid端末にADB経由でアクセスできるようにしました。これで、`adb`コマンドを用いて、Android端末との間でのファイル送受信や、端末上でのシェルコマンド実行などが可能になったわけです。本記事では、本来の目的であった「カスタムROMのインストール」に必要なファイルを整理し、これらをダウンロードします。

引き続き、ターゲットとするAndroid端末はNexus 6として説明していきます。

Nexus 6向けのカスタムROMは、[XDA Developers](https://www.xda-developers.com/)のNexus 6関連フォーラム([Nexus 6 Android Development](https://forum.xda-developers.com/nexus-6/development)および[Nexus 6 Original Android Development](https://forum.xda-developers.com/nexus-6/orig-development))にアクセスすると、さまざまなものが開発されていることがわかります。もっともメジャーなのは、もともとCyanogenModという名前で開発され、現在は[LineageOS](https://www.lineageos.org/)と呼ばれているカスタムROMだと思います。しかし、残念ながら本記事執筆の時点(2018/2/18)で、Nexus 6向けにはNougatに相当するバージョンが最新のようです。

せっかくなのでOreoにしたいと思い、さらに調査したところ、[Dirty Unicorns](https://dirtyunicorns.com/)と[AICP](http://aicp-rom.com/)というROMが、比較的メジャーなものでOreo相当であることがわかりました。本記事では、Dirty Unicornsをインストールすることにして、以下説明していきます。

通常、カスタムROMをインストールするためには、以下の3つのファイルが必要です。(さらに、ルート化する場合には、あとの2つを加えた5つのファイルが必要。)

- カスタムリカバリ

    カスタムリカバリはカスタムROMそのものではなく、ROMを端末内のフラッシュメモリに書き込むために用いるソフトウェアです。
    
    デフォルトでインストールされているリカバリは、Googleから提供される公式のアップデートファイルを書き込むためのもので、カスタムROMのインストールには使えません。そこで、有志が開発・提供しているカスタムリカバリを用います。現時点では、[TWRP (TeamWin Recovery Project)](https://twrp.me/)によるリカバリがもっともメジャーであるため、本記事でもこれを用いることにします。
    
    [ここ](https://twrp.me/motorola/motorolanexus6.html)からNexus 6用のリカバリファイルをダウンロードしてください。Download Linksのところにある"Primary (Americas)"あるいは"Primary (Europe)"のいずれかのリンクからダウンロードできます。
- カスタムROM本体

    カスタムROMの本体です。
    
    [ここ](https://download.dirtyunicorns.com/?dir=shamu)からNexus 6用のイメージファイルをダウンロードしてください。Rcフォルダ内にある最新のファイルをダウンロードします。
- GAppsパッケージ

    カスタムROM本体だけでもAndroidとしての動作はしますが、Googleが提供する各種サービス(最も重要なのはGoogle Play)が利用できません。おおかたの場合は、Googleのサービスを利用できるほうが都合がよいと思われますので、これも有志がコンパイル・提供しているGoogle Appsのパッケージ([Open GApps](http://opengapps.org/))を使用します。
    
    [Dirty Unicornsのサイト](https://download.dirtyunicorns.com/?dir=gapps/%20OpenGapps)からUnofficialパッケージをダウンロードします。(まだ、公式の8.1 Oreo向けパッケージがリリースされていないため。)
- Magiskパッケージ(ルート化が必要な場合)
- Magisk Managerアプリ(ルート化が必要な場合)

    ROMのシステム領域を改変することなくルート権限を取得したり、ROMをカスタマイズしたりする機能を提供する統合パッケージです。
    
    [ここ](https://forum.xda-developers.com/apps/magisk/official-magisk-v7-universal-systemless-t3473445)から、MagiskパッケージのファイルとMagisk Manager (Magiskの各種機能を設定、制御するためのAndroidアプリ)のapkファイルをダウンロードします。Downloadsのところから、"Latest Magisk"と"Latest Magisk Manager"をダウンロードしてください。

後の利便のため、ダウンロードしたファイルを適当なディレクトリにまとめて格納しておきましょう。ダウンロードしたファイル一覧は以下のようになります。
```shell-session
$ ls -1
Magisk-v15.3.zip                                 # Magisk
MagiskManager-v5.6.0.apk                         # Magisk Manager
du_shamu-v12.0-20180214-1231-RC.zip              # カスタムROM
open_gapps-arm-8.1-nano-20180115-UNOFFICIAL.zip  # OpenGApps
twrp-3.2.1-0-shamu.img                           # カスタムリカバリ
```

### 参考文献
1. Dirty Unicorns, https://dirtyunicorns.com/
1. Android Ice Cold Project, http://aicp-rom.com/
1. TeamWin - TWRP, https://twrp.me/
1. The Open GApps Project, http://opengapps.org/
1. Magisk - Root & Universal Systemless Interface, https://forum.xda-developers.com/apps/magisk/official-magisk-v7-universal-systemless-t3473445
