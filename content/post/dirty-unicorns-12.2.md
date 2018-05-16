+++
title = "Dirty Unicorns 12.2 (Android Oreo 8.1)がリリース"
date = "2018-05-16T16:16:05+09:00"
draft = true
categories = ["Android"]
tags = ["android", "oreo", "dirtyunicorns", "custom", "rom", "nexus6", "smartpixel"]
+++

やや旧聞になりますが、2018/5/11付で[Dirty Unicorns](https://dirtyunicorns.com/) 12.2 (Android Oreo 8.1)がリリースされました。12.1からのマイナーアップデート版です。

{{< tweet 995023933463318528 >}}

ダウンロードURLに変更があったため、(12.2以降に更新しない限り) Updaterアプリでのファイルダウンロードはできないようです。(更新アプリを使うためにまず更新が必要というデッドロック状況ですね…) ですので、Nexus 6 (shamu)向けのビルドは以下のURLから、ブラウザなどを用いてダウンロードしてください。

- [Dirty Unicorns (shamu)](https://download.dirtyunicorns.com/?device=shamu)

公式サイトの[変更点一覧](https://dirtyunicorns.com/2018/05/11/du-12-2-is-here/)では、本リリースについて、フルワイプした後にクリーンフラッシュすることを推奨しています。ただ、手もとのNexus 6にはワイプせずにダーティフラッシュしましたが、今までのところ特に問題は発生していません。(もちろん、カスタムROMのフラッシュについては自己責任で行なっていただくようお願いします。)

すべての変更点については[先ほどのURL](https://dirtyunicorns.com/2018/05/11/du-12-2-is-here/)を参照いただくとして、個人的な注目ポイントは以下のとおりです。

- 最新セキュリティパッチの適用(パッチレベル: 2018年5月5日)
- スマートピクセル機能

    OLEDディスプレイを搭載する端末(たとえばNexus 6)向けの省電力機能です。本機能を有効にすると、ディスプレイを構成するピクセルをすべて点灯するのではなく、設定した割合のピクセルを消灯します。これによって、ディスプレイの消費電力を抑える効果があります。

    本機能を設定するには、設定アプリを起動して「電池→Smart Pixels」を選択します。設定画面には、本機能のON/OFFスイッチ、およびバッテリーセーバー有効時に本機能を自動的に有効化するスイッチがあります。

    ![設定 - Smart Pixel - メイン](/img/android/android-smart-pixels-main.png)

    本機能をONにすると、消灯するピクセルの割合(%)を設定できます。

    ![設定 - Smart Pixel - 消灯ピクセル割合](/img/android/android-smart-pixels-disable-percentage.png)

    また、点灯するピクセルを特定のピクセルに常時限っていると、点灯している分のピクセルだけが焼き付いてしまいます。そこで、点灯するピクセルを一定時間ごとに切り替える機能も用意されています。

    ![設定 - Smart Pixel - 焼き付き防止](/img/android/android-smart-pixels-burn-protection.png)

- フォントセレクタにフォント追加

    [Dirty Unicorns 12.1](/post/dirty-unicorns-12.1/)で追加された、システムフォントを選択できる機能ですが、いくつかフォントが追加されました。なつかしのソニーエリクソンフォント(Sony Sketch)も収録されていますので、ソニーファンにはうれしいかも? 個人的にはGoogle Sansを気に入って使っています。(下図のような表示になります。) 

    ![設定 - Google Sansフォント](/img/android/android-font-google-sans.png)

### 参考文献
1. Dirty Unicorns, https://dirtyunicorns.com/
1. DU 12.2 is here!, https://dirtyunicorns.com/2018/05/11/du-12-2-is-here/
