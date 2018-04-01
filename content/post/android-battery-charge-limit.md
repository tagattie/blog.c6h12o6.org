+++
title = "Battery Charge LimitでAndroid端末のバッテリ寿命を伸ばす"
date = "2018-04-01T21:31:00+09:00"
categories = ["Android"]
tags = ["android", "app", "battery", "charge", "limit", "life", "prolong", "nexus6"]
+++

[先日の記事](/post/android-accubattery/)で、バッテリの劣化状況を教えてくれるAndroidアプリとして[AccuBattery](https://play.google.com/store/apps/details?id=com.digibites.accubattery)を紹介しました。まる三年使っているNexus 6のバッテリが、相当消耗していることを感じてはいましたが、現在の推定容量はもとの約70%であると数字で知ることができてよかったです。

ところで、AccuBatteryには指定した残量(デフォルトでは80%)まで充電が完了すると、ケーブルを外すように知らせる機能もあります。バッテリの劣化防止をサポートするためなのですが、なぜ80%で充電をやめるのがよいのでしょうね?

AccuBatteryによる記事([Charging - research and methodology](https://accubattery.zendesk.com/hc/en-us/articles/210224725-Charging-research-and-methodology))と、手もとにあるNexus 6のAccuBatteryアプリ画面を参考にすると、以下の二つが主な理由ではないかと推測します。

- 満充電すると標準電圧を上回る

    リチウムイオン電池の満充電状態での標準電圧は4.2Vとされているそうです(参考: [リチウムイオン電池の話 9. 充電方法（定電流定電圧 パルス充電）(ベイサン)](http://www.baysun.net/ionbattery_story/lithium09.html))。しかし、手もとにあるNexus 6が100%充電された状態でAccuBatteryを起動してみると、電圧が**4,373mV**となっており、4.2Vを上回ってしまっています。(下図) AccuBatteryの記事では、標準電圧(4.2V)を超えて充電するとバッテリが大きく劣化すると述べられています。
    
    ![AccuBatteryでの100%時の電圧](/img/android/accubattery-over-voltage-at-100.png)

- 80%程度の容量まで充電されると定電圧充電に入る

    上記、ベイサンの記事を参考にすると、リチウムイオン電池は標準電圧(4.2V)に達するまでは、一定の電流を流して充電する定電流充電が行なわれます。標準電圧に達してからは、その電圧を保ちながら徐々に流す電流を少なくしていく定電圧充電が行なわれます。AccuBatteryの記事では、充電時におけるバッテリ劣化の要因としてトップオフ充電(= 定電圧充電)が挙げられています。

上記二つの要因を合わせると、100%まで充電を行なうことでバッテリが劣化し、その後も充電器につなぎっぱなしにしておくとさらに劣化する、ということになりますね。わたしは、就寝前にスマートフォンを充電開始して、起床時にケーブルを外すというスタイルなのですが、上記二要因が正しいとすると、バッテリ寿命にとっては最悪な使い方をしているようです…。

AccuBatteryのお知らせ機能も、寝ている最中では残念ながら役に立ってくれません。そこで、指定した残量まで充電が完了したら、お知らせしてくれる代わりに充電自体を止めてくれるアプリはないかと探してみました。

すると、ありました。[Battery Charge Limit](https://play.google.com/store/apps/details?id=com.slash.batterychargelimit)という、まさにそのとおりの名前のアプリが見つかりました。

注: ただし、このアプリには一つだけ**大きな制約**があります。それは**端末のルート化**が必要ということです。しかし、この制約さえクリアできれば、役立ってくれること間違いなしです。

使い方はめっぽうかんたんで、バッテリ残量の上限(デフォルトは80%)とバッテリ残量の下限(デフォルトは78%)を指定して有効化するだけです。残量の上限に達すると充電を一時的に停止し、下限を下回ると再び充電を行なうことで、バッテリ残量を上限と下限の間に保ってくれます。(下図)

![Battery Charge Limitのメイン画面](/img/android/battery-charge-limit-main.png)

えっ? 上限で充電を停止するのはいいけど、下限を下回っても充電を始めてくれませんか?

同じトラブルにわたしも見舞われました。

この場合は、設定画面を開いて"Advanced Settings"の"Always Write Ctrl. File"にチェックを入れてみてください。(下図) これでうまくいくのではないかと思います。少なくとも、手もとのNexus 6ではこれで問題が解決しました。

![Battery Charge Limitの設定画面](/img/android/battery-charge-limit-settings.png)

Battery Charge Limitを有効化した後、しばらく充電ケーブルをつなぎっぱなしにしてバッテリ残量グラフを見てみました。(下図) うまく動いてくれているようです。

![GSam Battery Monitorでのバッテリ残量グラフ画面](/img/android/gsam-battery-monitor-graph.png)

Nexus 6のバッテリを交換したら、このアプリを使ってバッテリの寿命をできるだけ伸ばしたいと思います。

### 参考文献
1. AccuBattery, https://play.google.com/store/apps/details?id=com.digibites.accubattery
1. Charging - research and methodology, https://accubattery.zendesk.com/hc/en-us/articles/210224725-Charging-research-and-methodology
1. リチウムイオン電池の話 9. 充電方法（定電流定電圧 パルス充電）, http://www.baysun.net/ionbattery_story/lithium09.html
1. Battery Charge Limit, https://play.google.com/store/apps/details?id=com.slash.batterychargelimit
