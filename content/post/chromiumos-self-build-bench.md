+++
title = "Chromium OSでバッテリ駆動時間は伸びるのか? NEC LaVie Zで試す - ベンチマーク編"
date = "2018-04-13T15:19:08+09:00"
draft = true
categories = ["ChromiumOS"]
tags = ["chromiumos", "chromeos", "nec", "lavie", "chromebook", "battery", "benchmark"]
+++

[まえがき](/post/chromiumos-self-build-intro/)から[ビルド実行編](/post/chromiumos-self-build-build/)にわたって、Chromium OSを自分でビルドして、NEC LaVie Zで使ってみようと思い立ったきっかけから、起動用のUSBメモリを作成するまでの流れを紹介しました。

無事USBメモリからの起動もできましたので、Chromium OSを試すに至った動機、つまり「Chromium OSではWindowsよりもバッテリ駆動時間が伸びるのではないか」が正しいのかを確かめたいと思います。

まえがきでは、「文書作成のみに集中しているときなら3~4時間くらいはもつが、Webブラウズを連続して行なうと最悪1時間強くらいになってしまう」と書きました。本記事では、後者のWebブラウズを連続して行なう状況を想定して、バッテリによる連続駆動時間を計測したいと思います。

### 連続Webブラウズのシミュレーション
まず、使用するブラウザについてですが、Windows 10とChromium OSで共通にするためChrome (Chromium)を使用します。

本来ならば、ブラウザを用いてさまざまなWebサイトにアクセスしながら、バッテリ残量を記録していくべきところです。しかし、手動で連続的にブラウジングを行ないながら、バッテリ残量と経過時間を確認、記録するのは少し大変なので、以下の条件を設定して連続的ブラウジングを模擬することにしました。

- Chromeのウィンドウを5つ開く
- Chromeの各ウィンドウでは以下の各Webサイトへアクセス
    - Yahoo! Japan (https://www.yahoo.co.jp/)
    - Yahoo! (https://www.yahoo.com/)
    - Amazon Japan (https://www.amazon.co.jp/)
    - Amazon (https://www.amazon.com/)
    - 楽天 (https://www.rakuten.co.jp/)
    
    各ウィンドウは60秒毎に同一URLをリロードします。ただし、それぞれのウィンドウをリロードするタイミングは10秒ずつずらします。リロード作業を自動化するため、Chrome拡張の[Super Auto Refresh](https://chrome.google.com/webstore/detail/super-auto-refresh/kkhjakkgopekjlempoplnjclgedabddk)を使用します。(イメージ下図)

    ![Chrome - 5つのウィンドウ](/img/chromiumos/windows-chrome-5-windows.png)

### 追加の計測条件
Webブラウズのシミュレーション以外に、以下の条件を加えます。

- ディスプレイの明るさは40%
- WiFiとBluetoothはともにオン
    - WiFiアクセスポイント(802.11a)に接続
    - VPN接続は行なわない

これらの条件のもとで、バッテリ残量が**80%から20%**になるまでの所要時間を計測します。

### バッテリ残量と所要時間の計測方法
Windowsについては、[BBench (バッテリーベンチマーク)](http://www.vector.co.jp/soft/win95/util/se211432.html)というソフトウェアを使います。以下の二記事でも紹介されていますが、本ソフトウェアには、バッテリ残量(%)とその残量に至るまでの計測開始からの経過秒数を自動で記録する機能が備わっています。大手IT系メディアのレビューでもこのソフトが使用されており、かなりメジャーなものだそうです。

- [バッテリーベンチマークソフト　bbenchの使い方 (豆ガジェ通信)](http://mamegadget.jugem.jp/?eid=40)
- [ノートパソコンのバッテリー駆動時間計測方法 (こまめブログ)](http://little-beans.net/battery-test/)

また、このソフトウェアには、設定した時間間隔でキー入力を行なったりWebサイトにアクセスしたりすることで、いろいろなWebサイトを参照しながら文書作成を行なう、という利用シーンをシミュレートする機能もありますが、今回はこの機能は使用しません。

Chromium OSの場合は、自動でバッテリ残量や経過時間を記録するソフトウェアが見つかりませんでしたので、ストップウォッチを使って手動で記録を行なうことにします。

### 計測結果
グラフをご覧ください。当然Chromium OSのほうが駆動時間は長くなるだろう、とややバイアスがかかった予想をしていたのですが、みごとに裏切られました。実際には、バッテリ残量が80%から20%に達するまでの所要時間は、Windowsのほうが**約7分長い**という結果になりました。

![計測結果 - グラフ](/img/chromiumos/chromiumos-windows-battery-chart.png)

具体的には、バッテリ残量が80%から20%に達するまでに要した時間はそれぞれ、

- Windows 10 - 5,462秒
- Chromium OS - 5,048秒

でした。

うーん、率直にいって普段の使用感と異なりますねぇ。実際の利用シーンと今回の計測で大きく異なるのは、VPNを使うか使わないかなのですが…。VPNを使うようにしてもう一度計測し直してみようかなあ…。

### 参考文献
1. Super Auto Refresh, https://chrome.google.com/webstore/detail/super-auto-refresh/kkhjakkgopekjlempoplnjclgedabddk
1. BBench (バッテリーベンチマーク), http://www.vector.co.jp/soft/win95/util/se211432.html
1. バッテリーベンチマークソフト　bbenchの使い方, http://mamegadget.jugem.jp/?eid=40
1. ノートパソコンのバッテリー駆動時間計測方法, http://little-beans.net/battery-test/
