+++
title = "FreeBSDとElasticスタックでホームゲートウェイRS-500KIのログを可視化する - 設定まとめ編"
date = "2018-05-07T21:24:55+09:00"
draft = true
categories = ["Network"]
tags = ["freebsd", "elasticstack", "elasticsearch", "kibana", "logstash", "beats", "filebeat", "homegateway", "rs-500ki", "log", "vizualization"]
+++

[設定編(Logstash)](/post/elastic-stack-hgw-visualization-config-logstash/)では、Logstashによるログデータ加工のための設定と動作確認を行ないました。Elasticスタックのインストールと、その後の可視化システムの構築の流れを再掲します。(下図)

1. [Elasticsearch, Elasticsearch X-Packのインストール](/post/elastic-stack-hgw-visualization-install-es/)(済)
1. [Kibana, Kibana X-Packのインストール、Basicライセンスの取得と投入](/post/elastic-stack-hgw-visualization-install-kibana/)(済)
1. [Logstash, Logstash X-Packのインストール](/post/elastic-stack-hgw-visualization-install-logstash/)(済)
1. [Beats (Filebeat)のインストール](/post/elastic-stack-hgw-visualization-install-beats/)(済)
1. [Logstashのログ処理ルールの作成とテストデータでの動作確認](/post/elastic-stack-hgw-visualization-config-logstash/)(済)
1. **Filebeat→Logstash→Elasticsearchでの運用開始**(本記事)
1. Kibanaでの検索、ダッシュボードの作成

![Elasticスタックを用いたHGWログ可視化 - 構築の流れ - 本格運用開始](/img/elastic/elastic-stack-log-viz-start-operation.png)

本記事では、これまでに説明してきた各ソフトウェアコンポーネント([Elasticsearch](https://www.elastic.co/products/elasticsearch), [Kibana](https://www.elastic.co/products/kibana), [Logstash](https://www.elastic.co/products/logstash), および[Filebeat](https://www.elastic.co/products/beats/filebeat))の設定ファイルの最終的な内容を示します。そして、おのおののコンポーネントを起動して、ログ可視化システムの本格運用を開始します。

### 各コンポーネントの設定ファイル
設定ファイルの内容を示すにあたり、ファイルの内容をそのまま本記事内に載せていってもかまわないのですが、数が多いのでかえってわかりにくくなりそうです。そこで、設定ファイルを格納したリポジトリをGitHub上に作成しました。

- [RS-500KI-ElasticStack](https://github.com/tagattie/RS-500KI-ElasticStack) - Sample configuration files for Elastic Stack for visualizing RS-500KI logs on FreeBSD

最終的な設定ファイルを取得するには、上記リポジトリをチェックアウトしてください。

``` shell
cd <適当なディレクトリ>
git clone https://github.com/tagattie/RS-500KI-ElasticStack.git
```

リポジトリの構成は以下に示すとおりです。各コンポーネントの設定ファイルを、(そのコンポーネントを動作させるマシンの)「コピー先」ディレクトリにコピーしてください。後でも述べますが、本記事では「マシン1」でFilebeatを、「マシン2」で残りのコンポーネント(Logstash, Elasticsearch, およびKibana)を動作させる前提で設定を行なっています。

```
トップディレクトリ           コピー先
├elasticsearch/
│  └elasticsearch.yml ---> /usr/local/etc/elasticsearch/elasticsearch.yml
├kibana/
│  └kibana.yml        ---> /usr/local/etc/kibana/kibana.yml
├logstash/
│  ├logstash.yml      ---> /usr/local/etc/logstash/logstash.yml
│  ├logstash.conf     ---> /usr/local/etc/logstash/logstash.conf
│  └patterns/
│      └hgw           ---> /usr/local/etc/logstash/patterns/hgw
└filebeat/
    └filebeat.yml      ---> /usr/local/etc/filebeat.yml
```

運用を始める前に、お使いの環境に合わせて変更が必要な部分(主としてホスト名やIPアドレスに関わる設定)がいくつかあります。該当する部分に`# CHANGEME`というコメントを付与していますので、確認、変更をお願いします。コメントを付与した部分は以下のコマンドで確認できます。

``` shell-session
$ cd <リポジトリのトップディレクトリ>
$ find . -type f -exec grep '# CHANGEME' /dev/null {} \;
./logstash/logstash.conf:        nameserver => [ "192.168.1.251", "192.168.1.247" ]                     # CHANGEME
./logstash/logstash.conf:    hosts => [ "localhost:9200" ]                                              # CHANGEME
./logstash/logstash.yml:node.name: Logstash                                                             # CHANGEME
./logstash/logstash.yml:queue.type: persisted                                                           # CHANGEME
./logstash/logstash.yml:xpack.monitoring.elasticsearch.password: <password for user logstash_system>    # CHANGEME
./kibana/kibana.yml:server.host: "elastic.example.com"                                                  # CHANGEME
./kibana/kibana.yml:elasticsearch.password: "<password for user kibana>"                                # CHANGEME
./filebeat/filebeat.yml:name: Filebeat                                                                  # CHANGEME
./filebeat/filebeat.yml:  hosts: ["elastic.example.com:5044"]                                           # CHANGEME
./elasticsearch/elasticsearch.yml:cluster.name: Elasticsearch                                           # CHANGEME
./elasticsearch/elasticsearch.yml:node.name: Elasticsearch                                              # CHANGEME
```

また、先ほども述べましたが、本記事の設定は下図に示すマシン構成を前提にしています。ホスト名(FQDN)、待ち受けポート番号、およびDNSサーバアドレスを設定する際に、設定ファイルと見比べて参考にしてください。

- マシン1 - `filebeat.example.com`
	- Filebeat
- マシン2 - `elastic.example.com`
	- Logstash (待ち受けポート5044)
		- DNSプラグインで用いるネームサーバアドレス - 192.168.1.251, 192.167.1.247
	- Elasticsearch (待ち受けポート9200)
	- Kibana (待ち受けポート5601)

![ホームゲートウェイログ可視化システム - マシン構成](/img/elastic/elastic-stack-log-viz-network-config.png)

### Elasticスタックの起動と動作確認
各マシンへの設定ファイルのコピーと編集が終わったら、次は各コンポーネントを起動しましょう。前節に引き続き、ログ可視化システムがマシン1およびマシン2から構成されるという前提で説明を進めます。

もし、コンポーネントの配備が異なっている場合は、適宜環境に合わせて説明を読み替えてください。いずれにしても、コンポーネントを起動する順番はElasticsearch→Kibana→Logstash→Filebeatというようになります。

#### マシン2
では、マシン2上のコンポーネントからです。

まず、`/etc/rc.conf`でElasticsearch, Kibana, およびLogstashの自動起動設定が有効になっていることを確認してください。

``` shell
elasticsearch_enable="YES"
kibana_enable="YES"
logstash_enable="YES"
```

次に、念のため、Logstashのデータディレクトリとログファイルディレクトリのオーナー、グループを再度修正しておきます。

```shell
chown -R logstash:logstash /usr/local/logstash/data /var/log/logstash
```

最後に、以下の各コマンドを実行して各コンポーネントを起動します。

``` shell
service elasticsearch start    # すでに起動している場合は不要
service kibana start           # すでに起動している場合は不要
service logstash start
```

#### マシン1
次は、マシン1上のコンポーネントです。

`/etc/rc.conf`において、Filebeatの自動起動設定が有効になっていることをまず確認してください。

``` shell
filebeat_enable="YES"
```

前回の記事の最後で削除済みだと思いますが、念のため、あらためて`registry`ファイルを削除しておきます。

``` shell
rm -f /var/db/beats/filebeat/data/registry
```

最後に、以下のコマンドを実行してFilebeatを起動します。

``` shell
service filebeat start
```

以上でシステムの起動は完了です。Elasticsearchにログデータが蓄積されていっているかを確認して終わりにしましょう。

Filebeatを起動した後しばらく待って、**マシン2 (Elasticsearchが動作しているマシン)**で以下のコマンドを実行してみてください。ログデータの日付に応じた`logstash-<日付>`というインデックスが作成されていればOKです。(デフォルトでは、一日ごとにインデックスが作成されます。)

``` shell-session
curl -X GET http://localhost:9200/_cat/indices|grep -E 'logstash-....\...\...'
(snip)
yellow open logstash-2018.04.01               2QUe80f6Rtqfjsmm27bRvA 5 1  10213     0   4.2mb   4.2mb
yellow open logstash-2018.04.02               dOio7OhlSy6gWYfeftFvmg 5 1   4044     0     3mb     3mb
yellow open logstash-2018.04.03               kPOAiT8pTdqFvhrzkAPrKA 5 1   8445     0   4.3mb   4.3mb
yellow open logstash-2018.04.04               p_9D_W-QR5CjFuOJb1eueg 5 1  13488     0   5.3mb   5.3mb
yellow open logstash-2018.04.05               93ixGHphR0yAWvtnN_pyzw 5 1  13388     0   5.6mb   5.6mb
(snip)
```

これで、ログファイルの内容がElasticsearchに蓄積され始めました。次回は、Kibanaを用いたログデータの検索と、ダッシュボードの作成について説明します。
