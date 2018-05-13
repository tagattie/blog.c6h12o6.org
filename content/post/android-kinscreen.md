+++
title = "KinScreenでAndroid端末のスクリーンタイムアウトを自在に制御する"
date = "2018-05-13T21:18:00+09:00"
categories = ["Android"]
tags = ["android", "kinscreen", "screen", "timeout", "sleep", "backlight", "control", "sensor"]
+++

Android端末には、一定時間操作を行なわないと自動的に画面を消灯する機能があります。みなさん、スクリーンタイムアウト(画面消灯までの待ち時間)はどのくらいに設定していますか?

長くするとバッテリの無駄な消費が心配です。しかし、短くしすぎると少し操作しなかっただけで画面が消灯してしまい、使い勝手が良くありません。画面消灯後すぐに端末ロックをかけるよう設定している場合はさらに、たびたびロック解除操作を行なうハメになってイラッときます。(注: Androidに備わっている[Smart Lock機能](https://support.google.com/android/answer/6093922)を用いることで、たびたびロック解除操作を行なう必要性は緩和できます。)

バッテリセーブと利便性を考慮して、1分くらいまでに設定しているかたが多いのではないでしょうか(完全な想像ですが…)。それでも、状況によって、画面消灯までの時間を長くしたり短くしたいことがあります。たとえば、電子書籍を読んでいたり、動画を見ているときなどは、画面操作をしなくても消灯しないようにしたくありませんか?

そこで本記事では、[KinScreen](https://play.google.com/store/apps/details?id=com.teqtic.kinscreen)というアプリを紹介します。本アプリは、端末に搭載されているセンサを活用し、状況に応じてスクリーンタイムアウトを細かくコントロールします。また、スクリーンタイムアウトの制御に加えて、特定の状況で自動的に画面をオンにすることもできます。

 - [KinScreen 🥇 Most advanced screen control (Google Play)](https://play.google.com/store/apps/details?id=com.teqtic.kinscreen)
 
では、画面をまじえながら機能を説明していきたいと思います。アプリを起動すると以下のような画面になります。基本的にアプリの画面はこれ一枚だけで、とてもシンプルです。ご覧のように、グリーンとグレイの部分に分かれており、

- グリーン部分 - アプリ全体のオン/オフスイッチ、端末センサの状態、センサキャリブレーションボタン
- グレイ部分 - 画面点灯/消灯を制御する各種条件設定

となっています。

![KinScreen - 起動画面](/img/android/kinscreen-calibration.png)

使い始める前に、端末を水平かつ振動のない場所に置いて、センサのキャリブレーションを行なってください。一度行なうと、本ボタンは表示されなくなります。(注: 画面左上のメニューから再キャリブレーションを行なうことも可能です。)

キャリブレーションが終わったら、各種条件設定の部分(グレイ部分)について見ていきましょう。グレイ部分全体をとおして、

- チェックマークを入れた条件はOR結合、つまりチェックを入れたいずれかの条件が成立すれば動作が実行される
- 黄色の部分はカスタマイズ可能(一部のカスタマイズ項目は要課金)
- 橙色の枠で囲った部分は(わたしが使っている)おすすめの条件

です。

### 所定の条件で画面を自動的にオン
まず、画面が消灯している状態から、画面を点灯させる条件の設定です。(下図)

![KinScreen - 画面オン](/img/android/kinscreen-turn-screen-on.png)

おすすめ条件:

- 近接センサの覆いを外したとき

    本条件をチェックすることで、端末を伏せて置いた状態から手に取る、あるいはポケットやバッグから取り出す、といった操作を行なうだけで画面が自動的に点灯します。(ちなみに、手もとのNexus 6の場合、センサが覆われているという判断には、センサと覆いとの距離を1cmくらいまで近づける必要があります。)

### 所定の条件で画面のオン状態をキープ
次は、画面点灯をキープさせる条件の設定です。(下図)

![KinScreen - 画面オンキープ](/img/android/kinscreen-keep-screen-on.png)

おすすめ条件:

- 動きを検知しているとき

    本条件をチェックすることで、端末の動きが検知されている間は画面をずっと点灯させておけます。たとえば、案内図や地図をときどき見ながら歩いているときに、その間じゅう画面をつけたままにしておく、といったことが可能です。

- ただし、端末の傾きがx度以下である時を除く

    さらに本条件をチェックすることで、端末を水平、あるいは水平に近い状態に保っているときは所定の時間で画面が消灯します。たとえば、新幹線の座席テーブルのようなガタガタ動く場所に端末を置いた場合、動きを検知していてもちゃんと画面を消灯してくれます。

- 傾きがx度~y度のとき

    本条件をチェックすることで、端末をスタンドに載せたり、スマホリングで立てておいて画面を見ているような場合に、ずっと画面を点灯させておけます。

- 特定のアプリを使用しているとき

    本条件をチェックすることで、電子書籍アプリを立ち上げて読んでいるときや、動画再生アプリで動画を閲覧しているときに、ずっと画面を点灯させておけます。

### 所定の条件で画面を自動的にオフ
その次は、画面が点灯している状態から、画面を消灯させる条件の設定です。(下図)

![KinScreen - 画面オフ](/img/android/kinscreen-turn-screen-off.png)

おすすめ条件:

- 近接センサを覆ってx秒経過したとき

    本条件ををチェックすることで、端末を伏せて置く、あるいはポケットやバッグに入れるだけで画面がすぐに消灯します。

### 追加オプション
最後は、その他の追加オプションの設定です。(下図)

![KinScreen - 追加オプション](/img/android/kinscreen-more-options.png)

ロック画面でも同様に点灯/消灯の条件を適用する、および画面点灯/消灯時に端末をバイブさせる設定ができます。

以上、ざっと機能を見てきましたが、かなりいたれりつくせりで条件がそろっているのではないでしょうか。興味をお持ちのかたはぜひお試しください。本アプリを使用してスマートフォン、タブレット端末をよりいっそう便利に活用いただければと思います。

### 参考文献
1. 端末が自動的にロック解除されるように設定する, https://support.google.com/android/answer/6093922
1.  KinScreen 🥇 Most advanced screen control, https://play.google.com/store/apps/details?id=com.teqtic.kinscreen