+++
title = "FreeBSDとElasticスタックでホームゲートウェイRS-500KIのログを可視化する - ダッシュボード編"
date = "2018-05-10T20:38:00+09:00"
categories = ["Network"]
tags = ["freebsd", "elasticstack", "kibana", "dashboard", "homegateway", "rs-500ki", "log", "vizualization"]
+++

[設定まとめ編](/post/elastic-stack-hgw-visualization-config-summary/)では、ログ可視化システムを構成する各ソフトウェアコンポーネントの設定をまとめて示しました。その後、各コンポーネントを起動して本格的な運用を開始しました。Elasticスタックのインストールと、その後の可視化システムの構築の流れを再掲します。(下図)

1. [Elasticsearch, Elasticsearch X-Packのインストール](/post/elastic-stack-hgw-visualization-install-es/)(済)
1. [Kibana, Kibana X-Packのインストール、Basicライセンスの取得と投入](/post/elastic-stack-hgw-visualization-install-kibana/)(済)
1. [Logstash, Logstash X-Packのインストール](/post/elastic-stack-hgw-visualization-install-logstash/)(済)
1. [Beats (Filebeat)のインストール](/post/elastic-stack-hgw-visualization-install-beats/)(済)
1. [Logstashのログ処理ルールの作成とテストデータでの動作確認](/post/elastic-stack-hgw-visualization-config-logstash/)(済)
1. [Filebeat→Logstash→Elasticsearchでの運用開始](/post/elastic-stack-hgw-visualization-config-summary/)(済)
1. **Kibanaでの検索、ダッシュボードの作成**(本記事)

![Elasticスタックを用いたHGWログ可視化 - 構築の流れ - ダッシュボード作成](/img/elastic/elastic-stack-log-viz-dashboard.png)

Elasticスタックを用いたホームゲートウェイのログ可視化シリーズも、本記事で最後となりました。今回は、一連の手順の中のハイライト、一番楽しい部分であるログデータの検索および可視化について説明します。

可視化を進めていくにあたり、以下に挙げる二つの記事を参考にします。一つめの記事は、Elasticsearch社公式のスタートガイドです。また、二つめの記事は、[@namutaka](https://qiita.com/namutaka)さんが[Qiita](https://qiita.com/)にポストされた記事です。検索式について本記事ではほんの少ししか触れていませんが、二つめの記事には初心者にとって必要十分な説明がありますので、ぜひ参考にしてみてください。

- [Getting Started (Elasticsearch)](https://www.elastic.co/guide/en/kibana/6.2/getting-started.html)
- [Kibanaの使い方 (Qiita)](https://qiita.com/namutaka/items/b67290e75cbd74cd9a2f)

さて、本記事におけるダッシュボード作成までの流れは以下のようになります。

1. [インデックスパターンの作成](#インデックスパターンの作成)
1. [検索式を用いたログデータ検索](#検索式を用いたログデータ検索)
1. [検索結果のグラフによる可視化](#検索結果のグラフによる可視化)
1. [可視化結果を組み合わせたダッシュボードの作成](#可視化結果を組み合わせたダッシュボードの作成)

では、以下、一つずつ説明していきます。

### インデックスパターンの作成
まず、「インデックスパターン」を作成する必要があります。「インデックス」とは、Elasticsearchにおいてデータセットに与えられた名前のことで、リレーショナルデータベースでいうテーブル名に相当します。Elasticsearchへのデータ投入にLogstashを用いる場合、デフォルトではインデックスとして`logstash-<日付>`(日付は`YYYY.MM.DD`の形式)という名前が用いられます。

Elasticsearchは、ログデータや性能監視データなどの「時系列データ」を扱うことが多いため、所定の時間単位でデータセットを区切って、おのおののデータセットに時間を関連付けるのが、データを管理する上ではわかりやすいのだと思います。今回のログ監視システムにおいても、[設定まとめ編](/post/elastic-stack-hgw-visualization-config-summary/)の最後で確認したように、`logstash-<日付>`という名前のインデックスが一日単位で作られています。

インデックスパターンは、複数のインデックスにマッチするパターンです。一日単位でインデックスが作成されている場合、たとえば`logstash-2018.05.*`というインデックスパターンは、2018年5月分のインデックスにマッチします。蓄積されているすべてのデータを検索対象にする場合は、**`logstash-*`**というインデックスパターンを用います。今回の場合も、ログデータすべてを検索対象にしたいので、このインデックスパターンを用いることにします。

では、ブラウザでKibanaのURL (`http://elastic.example.com:5601`)にアクセスしてください。(URL内のホスト名はお使いの環境に合わせて読み替えをお願いします。) そして、画面右上にある"Set up index patterns"ボタンをクリックします。

![ダッシュボード - インデックスパターン - 開始](/img/elastic/elastic-stack-log-viz-dashboard-start.png)

インデックスパターンを作成する画面が表示されますので、"Index pattern"の欄に、先ほど説明した**`logstash-*`**を入力します。すると、その下にマッチするインデックス(複数個あるかもしれません)が表示されます。これを確認できたら、右側にある"> Next step"ボタンをクリックします。

![ダッシュボード - インデックスパターン - パターン](/img/elastic/elastic-stack-log-viz-dashboard-index-pattern-1.png)

次は、タイムスタンプとして用いるフィールドを指定します。先ほども述べましたように、Elasticsearchは基本的に時系列データを扱いますので、データ内のどのフィールドを時刻として扱うかを指定する必要があります。通常は`@timestamp`フィールドを用いますので、ここでもこれをリストから選択して、"Create index pattern"ボタンをクリックします。

![ダッシュボード - インデックスパターン - タイムスタンプ](/img/elastic/elastic-stack-log-viz-dashboard-index-pattern-2.png)

作成したインデックスパターンの名前とともに、インデックスに含まれているフィールド名のリストが表示されます。インデックスパターンの作成は以上で終了です。

![ダッシュボード - インデックスパターン - 完了](/img/elastic/elastic-stack-log-viz-dashboard-index-pattern-3.png)

### 検索式を用いたログデータ検索
インデックスパターンが作成できましたので、さっそくログデータを検索してみましょう。

画面左側にあるメニューの"Discover"をクリックします。すると、インデックスパターン`logstash-*`のデータを検索する画面が現れます。

デフォルトの状態では、現在時刻からさかのぼって15分以内のタイムスタンプを持つデータがすべて表示されています。もし、データが表示されない場合は、右上にあるデータ表示期間を長くしてみてください(例えば過去30日以内)。あまり期間を長くし過ぎると、データの表示に時間がかかりますので注意が必要です。

![ダッシュボード - 検索 - 開始](/img/elastic/elastic-stack-log-viz-dashboard-search-1.png)

本記事の場合、Elasticsearchにはホームゲートウェイのログしか格納していませんので、特に検索式を組み立てて検索する必要はないのですが、せっかくなので少しなれておきたいと思います。検索式の詳細については、以下のURL、あるいは先ほど紹介した[Qiitaの記事](https://qiita.com/namutaka/items/b67290e75cbd74cd9a2f)にアクセスしてみてください。

- [Query string syntax (Elasticsearch)](https://www.elastic.co/guide/en/elasticsearch/reference/6.2/query-dsl-query-string-query.html#query-string-syntax)

検索式を構成する基本単位は、`<フィールド名>:<検索条件>`という形を取ります。これによって、「フィールド名で示されるフィールドの値が検索条件にマッチする」データが抽出されます。さらに、`AND`, `OR`, および`NOT`を用いて、検索条件を組み合わせることができます。

本記事での検索条件は、

- `Filebeat`という名前を持つBeatから送信されたデータであること、かつ
- `fields`の`logtype`フィールドの値が`firewall_hgw`であること

とします。これによって、(複数種類のログが混在すると想定する中で)ホームゲートウェイのログだけをすべて取り出すことができます。本検索条件を式になおすと以下のようになります。

`beat.name:Filebeat AND fields.logtype:firewall_hgw`

検索式の欄にこれを入力して、右側の虫メガネボタンをクリックしてください。すると、式にマッチするデータが画面下部に表示されます。デフォルトでは検索結果に含まれるデータのすべてのフィールドが表示されており、このままでは少し見にくく感じます。そこで、次は表示するフィールドを選択します。

インデックスパターン名の下にフィールドの一覧(Available Fields)が表示されていますので、表示したいフィールド名をクリックして選択していきましょう(フィールド名の上にマウスポインタを置くと"add"ボタンが表示されます)。上図の場合は、Timestamp (これは必ず表示されます), `source_ip`, `source_host`, `geoip.coutry_name`, `source_port`, `dest_ip`, および`dest_port`を表示対象として選択しました。"Selected Fields"に選択したフィールド名の一覧が表示されます。

検索条件と結果の表示に満足したら、画面上部にある"Save"をクリックして、名前をつけて検索条件を保存しておきましょう。

![ダッシュボード - 検索 - 保存](/img/elastic/elastic-stack-log-viz-dashboard-search-2.png)

### 検索結果のグラフによる可視化
次は、保存した検索条件にマッチするデータをグラフで可視化してみます。

画面左側にあるメニューの"Visualize"をクリックします。すると、保存されているビジュアライゼーションの一覧画面が表示されます。いまはまだビジュアライゼーションを作成していませんので、"+"ボタンが大きく表示されていると思います。画面中央にある"+ Create a visualization"ボタンをクリックしてください。

![ダッシュボード - 可視化 - 開始](/img/elastic/elastic-stack-log-viz-dashboard-visualize-1.png)

すると、可視化の形態(グラフの種類)を選択する画面が現れます。まずは、基本的なところで、ログ数のトレンドを示す折れ線グラフを作ってみましょう。"Basic Charts"の"Area"をクリックします。

![ダッシュボード - 可視化 - グラフ選択](/img/elastic/elastic-stack-log-viz-dashboard-visualize-2.png)

可視化のデータソースを選択する画面に遷移しますので、先ほど名前をつけて保存した検索条件を選択します。下方に表示されている"Home Gateway"をクリックします。

![ダッシュボード - 可視化 - データ選択](/img/elastic/elastic-stack-log-viz-dashboard-visualize-3.png)

すると、グラフのX軸、Y軸を設定する画面が現れます。Y軸はそのままログ数(Count)としておきます。時間経過にともなうログ数の変動を知りたいので、X軸は時刻にします。"Aggregation"から"Date Histogram"を選択し、時刻を表すフィールドとして"@timestamp"を選択します。すると、右側の領域にグラフが表示されます。

本記事では、ここまでで基本的なところしか触れませんが、グラフはカスタマイズ可能ですので、いろいろ試してみると楽しいと思います。

![ダッシュボード - 可視化 - グラフ設定](/img/elastic/elastic-stack-log-viz-dashboard-visualize-4.png)

グラフの表示に満足したら、検索条件のときと同様、画面上部にある"Save"をクリックして、名前をつけてグラフを保存しておきましょう。

![ダッシュボード - 可視化 - 保存](/img/elastic/elastic-stack-log-viz-dashboard-visualize-5.png)

### 可視化結果を組み合わせたダッシュボードの作成
最後は、作成したグラフを使ってダッシュボードを作成します。

画面左側にあるメニューの"Dashboard"をクリックします。すると、保存されているダッシュボードの一覧画面が表示されます。いまはまだダッシュボードを作成していませんので、"+"ボタンが大きく表示されていると思います。画面中央にある"+ Create a dashboard"ボタンをクリックして、さっそく作成しましょう。

![ダッシュボード - ダッシュボード - 開始](/img/elastic/elastic-stack-log-viz-dashboard-dashboard-1.png)

新しいダッシュボードが作成されましたね。しかし、まだ何もデータが表示されていません。

![ダッシュボード - ダッシュボード - 新規作成](/img/elastic/elastic-stack-log-viz-dashboard-dashboard-2.png)

画面上部の、あるいは空白のダッシュボードの中にある"Add"ボタンをクリックしてください。そうすると、保存されているビジュアライゼーション、あるいは検索条件を選択する画面になります。(上部のタブでビジュアライゼーションと検索条件を切り替えられます。) ここでは、保存しておいたグラフを選択してクリックします。

![ダッシュボード - ダッシュボード - グラフ選択](/img/elastic/elastic-stack-log-viz-dashboard-dashboard-3.png)

画面下方(ダッシュボード)に先ほど保存しておいたグラフが追加されました。追加した時は、グラフの横幅がダッシュボード幅のちょうど半分になっていると思いますが、サイズは領域右下のハンドルで変更可能です。また、右上の歯車アイコンをクリックすると、領域のタイトルやグラフの再カスタマイズを行なうことができます。

![ダッシュボード - ダッシュボード - グラフ設定](/img/elastic/elastic-stack-log-viz-dashboard-dashboard-4.png)

満足したら、再び、画面上部にある"Save"をクリックして、名前をつけてダッシュボードを保存します。

![ダッシュボード - ダッシュボード - 保存](/img/elastic/elastic-stack-log-viz-dashboard-dashboard-5.png)

はい、できあがりです。いまのところ、グラフをひとつ表示しているだけなので少々さびしい感じですが、他のグラフも組み合わせて楽しいダッシュボードを作ってみてください。

![ダッシュボード - ダッシュボード - 完成](/img/elastic/elastic-stack-log-viz-dashboard-dashboard-6.png)

参考までに、当方で作成しようと思っていたホームゲートウェイダッシュボードの事前イメージと、このイメージに沿って作成した実際のダッシュボードを載せておきます。

![ダッシュボード - イメージ](/img/elastic/elastic-stack-log-viz-dashboard-image.png)
[![ダッシュボード - 完成](/img/elastic/elastic-stack-log-viz-dashboard-final-small.png)](/img/elastic/elastic-stack-log-viz-dashboard-final.png)

本記事まで、まえがきから数えると8回にわたって、ホームゲートウェイのログ可視化システムを構築してきました。とても長かったですが、ここまで読んでくださった皆さん、お付き合いくださってありがとうございます。では、可視化を楽しんでください!

### 参考文献
1. Getting Started, https://www.elastic.co/guide/en/kibana/current/getting-started.html
1. Kibanaの使い方, https://qiita.com/namutaka/items/b67290e75cbd74cd9a2f
1. Query string syntax, https://www.elastic.co/guide/en/elasticsearch/reference/6.2/query-dsl-query-string-query.html#query-string-syntax
