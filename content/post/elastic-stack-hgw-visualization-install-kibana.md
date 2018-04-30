+++
title = "FreeBSDとElasticスタックでホームゲートウェイRS-500KIのログを可視化する - インストール編(Kibana)"
date = "2018-04-30T22:19:00+09:00"
categories = ["Network"]
tags = ["freebsd", "elasticstack", "kibana", "homegateway", "rs-500ki", "log", "visualization"]
+++

[インストール編(Elasticsearch)](/post/elastic-stack-hgw-visualization-install-es/)では、Elasticsearchのインストールと設定を行ないました。Elasticスタックのインストールと、その後の可視化システムの構築の流れを再掲します。(下図)

1. [Elasticsearch, Elasticsearch X-Packのインストール](/post/elastic-stack-hgw-visualization-install-es/)(済)
1. **Kibana, Kibana X-Packのインストール、Basicライセンスの取得と投入**(本記事)
1. Logstash, Logstash X-Packのインストール
1. Beats (Filebeat)のインストール
1. Logstashのログ処理ルールの作成とテストデータでの動作確認
1. Filebeat→Logstash→Elasticsearchでの運用開始
1. Kibanaでの検索、ダッシュボードの作成

![Elasticスタックを用いたHGWログ可視化 - 構築の流れ - Kibanaインストール](/img/elastic/elastic-stack-log-viz-install-kibana.png)

本記事では、構築手順の2番めであるKibanaのインストールと設定を行ないます。また、KibanaのGUIを用いて、Basic Subscriptionライセンスの取得とElasticsearchへのライセンス投入も行ないます。インストールにあたり、Elasticsearch社の公式ガイド(以下の二つの記事)を参考にします。

- [Install Kibana with .tar.gz (Elasticsearch)](https://www.elastic.co/guide/en/kibana/6.2/targz.html)
- [Installing X-Pack in Kibana (Elasticsearch)](https://www.elastic.co/guide/en/kibana/6.2/installing-xpack-kb.html)

また、[インストール編(Elasticsearch)](/post/elastic-stack-hgw-visualization-install-es/)に引き続き、FreeBSD向けのインストールガイド的記事である、以下の二つのURLも参考にしながら進めます。

- [ELK Stackのインストール (がとらぼ)](https://gato.intaa.net/archives/11302)
- [Elastic Stackでシステム監視 FreeBSDのportsで6.2.3に更新 (がとらぼ)](https://gato.intaa.net/archives/12499)

### Kibanaのインストール
まず、Kibanaをインストールしましょう。Elasticsearchと同様、以下のコマンドを実行すればOKです。

``` shell
pkg install kibana6
```

### Kibana X-Packのインストール
Kibanaのインストールが終わったら、次にX-Packをインストールします。以下のコマンドを実行してください。"Optimizing and caching browser bundles"のところでかなり時間がかかりますが、しんぼう強く待ちましょう。

``` shell-session
# /usr/local/www/kibana6/bin/kibana-plugin install x-pack
Attempting to transfer from x-pack
Attempting to transfer from https://artifacts.elastic.co/downloads/kibana-plugins/x-pack/x-pack-6.2.3.zip
Transferring 270035965 bytes....................
Transfer complete
Retrieving metadata from plugin archive
Extracting plugin archive
Extraction complete
Optimizing and caching browser bundles...                                       # ここはかなり時間がかかります
Plugin installation complete
```

### Kibanaの設定ファイルの編集
次に設定ファイルを編集します。設定ファイルは`/usr/local/etc/kibana/kibana.yml`です。ファイルの内容を以下に示します。(コメント部分は除いています。)

- `/usr/local/etc/kibana/kibana.yml`

    ``` yaml
server.host: "<Kibanaサーバのホスト名あるいはIPアドレス>"    # サーバのホスト名あるいはIPアドレス
elasticsearch.username: "kibana"                             # Elasticsearchに接続するためのユーザ名
elasticsearch.password: "<ユーザkibanaのパスワード>"         # ユーザkibanaのパスワード
```

    パスワードについては、インストール編(Elasticsearch)の[パスワード設定](/post/elastic-stack-hgw-visualization-install-es/#elasticsearchのパスワード設定-elastic-kibana-logstash-systemユーザ)で設定したものを記入してください。

### 自動起動の設定
FreeBSDの起動時に、Kibanaが自動的に起動されるよう設定しておきましょう。

``` shell
sysrc kibana_enable=YES
```

設定ができたら、以下のコマンドを実行、あるいはFreeBSDマシンを再起動してKibanaを起動します。初回の起動時には、ブラウザ経由でアクセス可能になるまでにかなり時間がかかりますが、しんぼう強く待ちましょう。

``` shell
service kibana start
```

### Basicライセンスの取得と投入
Kibanaが起動しましたので、さっそく以下のURLにブラウザでアクセスします。

- `http://<Kibanaサーバのホスト名 or IPアドレス>:5601`

ログイン画面が表示されたら

- ユーザ: `elastic`
- パスワード: `<ユーザelasticのパスワード>`

でログインしてください。パスワードについては、インストール編(Elasticsearch)の[パスワード設定](/post/elastic-stack-hgw-visualization-install-es/#elasticsearchのパスワード設定-elastic-kibana-logstash-systemユーザ)で設定したものを使用します。

![Kibana - ログイン](/img/elastic/elastic-stack-kibana-login.png)

ログインすると、ダッシュボード的な画面が表示されます。左側にあるメニューから"Management"を選んでクリックします。

![Kibana - ダッシュボード](/img/elastic/elastic-stack-kibana-dashboard.png)

すると、Managementの画面になります。ソフトウェアコンポーネントごとにいくつか項目が表示されていますが、中ほどにあるElasticsearchの"License Management"をクリックします。

![Kibana - 管理](/img/elastic/elastic-stack-kibana-management.png)

クリックすると、現在のライセンス状況を示す画面に遷移します。これまでの手順以外に何も操作を行なっていなければ、Trialライセンスが有効な状態(1か月間有効)になっていると思います。

Trialライセンスでは、Basicライセンスで使用できない機能も有効化されています(例えば、認証機能やアラート機能など)。Trialライセンスでのみ有効な機能を試用したい場合は、これ以降の手順を行なうのをTrialライセンスが終了するまで見合わせるのがよいと思います。

Trialライセンスが期限切れの場合、あるいはTrialライセンスでの試用に興味がない場合は、Basicライセンスの取得手続きに進みましょう。左下の"Get Basic"をクリックして、ライセンス取得者情報の入力画面へ進みます。

![Kibana - 管理 - ライセンス管理](/img/elastic/elastic-stack-kibana-lisence-management.png)

氏名、メールアドレス、会社名、および国名の入力を求められますので、それぞれ記入します。会社名については、個人かつ非商用での使用の場合は"Personal Use"や"Private Use"などと入力しておけば問題ないでしょう。入力後、"Send"ボタンをクリックします。

![Kibana - ライセンス - 登録](/img/elastic/elastic-stack-kibana-registration.png)

すると、登録を受け付けた旨のメッセージが表示されます。この後すぐに、手続きを継続するためのURLが、登録したメールアドレス宛に届きます。メール内に記されたURLをクリックするなどしてアクセスします。

![Kibana - ライセンス - 登録受付](/img/elastic/elastic-stack-kibana-thank-you.png)

メールで受け取ったURLにアクセスすると、Basicライセンスの提供条件に対する同意を求められます。同意する場合は左下のチェックボックスをチェックし、"Send"ボタンをクリックします。

![Kibana - ライセンス - 同意](/img/elastic/elastic-stack-kibana-register-for-basic.png)

同意すると、ライセンスファイルのダウンロード画面が表示されます。Elasticsearch 5.x/6.x向けのライセンスファイル(JSON形式)をダウンロードし、適当な場所に保存します。

![Kibana - ライセンス - ダウンロード](/img/elastic/elastic-stack-kibana-register-for-basic-2.png)

ファイルをダウンロードしたら、先ほどのライセンス状況を示す画面に戻りましょう。今度は一番下にある"Install it now"をクリックします。

![Kibana - 管理 - ライセンス管理(再)](/img/elastic/elastic-stack-kibana-lisence-management-again.png)

ライセンスファイルをアップロードする画面が表示されますので、"Browse"ボタンをクリックして、先ほどダウンロードしたライセンスファイルを選択します。その後、"Upload"ボタンをクリックします。

![Kibana - 管理 - ライセンスアップロード](/img/elastic/elastic-stack-kibana-lisence-upload.png)

先ほど述べましたが、Basicライセンスに変更した時点で、Trialライセンスで使用可能な機能の一部が使えなくなります。ここで、本件に関する確認画面が表示されます。問題なければ"Confirm"ボタンをクリックします。

![Kibana - 管理 - ライセンス確認](/img/elastic/elastic-stack-kibana-lisence-upload-confirm.png)

みたび、ライセンスの状況を示す画面に戻りましょう。Basicライセンスが有効になっていることと、ライセンスの有効期限(1年間有効)が正しく表示されていることを確認してください。

![Kibana - 管理 - ライセンス管理(再々)](/img/elastic/elastic-stack-kibana-lisence-upload-done.png)

以上で、Basicライセンスの取得と投入は完了です。次回の記事では、Logstashのインストールと設定を行ないます。

### 参考文献
1. Install Kibana with .tar.gz, https://www.elastic.co/guide/en/kibana/6.2/targz.html
1. Installing X-Pack in Kibana, https://www.elastic.co/guide/en/kibana/6.2/installing-xpack-kb.html
1. ELK Stackのインストール, https://gato.intaa.net/archives/11302
1. Elastic Stackでシステム監視 FreeBSDのportsで6.2.3に更新, https://gato.intaa.net/archives/12499
