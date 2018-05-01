+++
title = "FreeBSDとElasticスタックでホームゲートウェイRS-500KIのログを可視化する - インストール編(Logstash)"
date = "2018-04-29T06:35:31+09:00"
draft = true
categories = ["Network"]
tags = ["freebsd", "elasticstack", "logstash", "homegateway", "rs-500ki", "log", "visualization"]
+++

[インストール編(Kibana)](/post/elastic-stack-hgw-visualization-install-kibana/)では、Kibanaのインストールと設定を行ないました。Elasticスタックのインストールと、その後の可視化システムの構築の流れを再掲します。(下図)

1. [Elasticsearch, Elasticsearch X-Packのインストール](/post/elastic-stack-hgw-visualization-install-es/)(済)
1. [Kibana, Kibana X-Packのインストール、Basicライセンスの取得と投入](/post/elastic-stack-hgw-visualization-install-kibana/)(済)
1. **Logstash, Logstash X-Packのインストール**(本記事)
1. Beats (Filebeat)のインストール
1. Logstashのログ処理ルールの作成とテストデータでの動作確認
1. Filebeat→Logstash→Elasticsearchでの運用開始
1. Kibanaでの検索、ダッシュボードの作成

![Elasticスタックを用いたHGWログ可視化 - 構築の流れ - Logstashインストール](/img/elastic/elastic-stack-log-viz-install-logstash.png)

本記事では、構築手順の3番めであるLogstashのインストールと設定を行ないます。インストールにあたり、Elasticsearch社の公式ガイド(以下の二つの記事)を参考にします。

- [Installing Logstash (Elasticsearch)](https://www.elastic.co/guide/en/logstash/6.2/installing-logstash.html)
- [Installing X-Pack in Logstash (Elasticsearch)](https://www.elastic.co/guide/en/logstash/6.2/installing-xpack-log.html)

また、[インストール編(Kibana)](/post/elastic-stack-hgw-visualization-install-kibana/)に引き続き、FreeBSD向けのインストールガイド的記事である、以下の二つのURLも参考にしながら進めます。

- [ELK Stackのインストール (がとらぼ)](https://gato.intaa.net/archives/11302)
- [Elastic Stackでシステム監視 FreeBSDのportsで6.2.3に更新 (がとらぼ)](https://gato.intaa.net/archives/12499)

### Logstashのインストール
まず、Logstashをインストールしましょう。ElasticsearchやKibanaと同様、以下のコマンドを実行すればOKです。

``` shell
pkg install logstash6
```

### Logstash X-Packのインストール
Logstashのインストールが終わったら、次にX-Packをインストールします。以下のコマンドを実行してください。

``` shell-session
# /usr/local/logstash/bin/logstash-plugin install x-pack
Downloading file: https://artifacts.elastic.co/downloads/logstash-plugins/x-pack/x-pack-6.2.3.zip
Downloading [=============================================================] 100%
Installing file: /tmp/studtmp-bc39b326ef855a76a0fd2f7de4cb5f7c40b29eb6b643966badc64a06b56c/x-pack-6.2.3.zip
Install successful
```

### Logstashの設定ファイルの編集
次に、設定ファイルの内容を確認します。設定ファイルは、ディレクトリ`/usr/local/etc/logstash`以下に格納されています。このディレクトリを`ls`すると、設定ファイルが複数あるのがわかると思いますが、本記事では以下の二つのファイルの内容を見ていきます。

- `/usr/local/etc/logstash/logstash.yml` - Logstashの動作パラメータなどの設定
- `/usr/local/etc/logstash/logstash.conf` - Logstashがログデータを加工するルールの設定

まず、Logstashの動作パラメータなどを設定する`logstash.yml`の内容を見てみましょう。(コメント部分は除いています。)

- `/usr/local/etc/logstash/logstash.yml`

    ``` yaml
node.name: "<ノード名>"                                                           # ノード名(コメントのままでも可)
path.config: /usr/local/etc/logstash/logstash.conf                                # Logstashにおけるログ加工処理を記述する設定ファイルのパス
queue.type: persisted                                                             # 受信ログを格納するキューの種別
xpack.monitoring.elasticsearch.username: logstash_system                          # Elasticsearchに接続するためのユーザ名
xpack.monitoring.elasticsearch.password: "<ユーザlogstash_systemのパスワード>"    # ユーザlogstash_systemのパスワード
```

    パスワードについては、インストール編(Elasticsearch)の[パスワード設定](/post/elastic-stack-hgw-visualization-install-es/#elasticsearchのパスワード設定-elastic-kibana-logstash-systemユーザ)で設定したものを記入してください。

    設定ファイルの内容について、一点注意をお願いします。
    
    キューの種別(`queue.type`)ですが、指定を行なわない場合はデフォルトのインメモリキュー(対応する設定値は`memory`)が使用されます。おそらく多くの場合、デフォルトのインメモリキューを使用すれば問題ないものと思います。

    しかし、本記事ではインメモリキューではなく、受信したログデータをいったんディスクに書き出してから処理を行なわせる、パーシステッド(永続化)キュー(対応する設定値は`persisted`)を使用するように変更しました。インメモリキューを使用した場合、ログ処理の途中にDNSを用いたIPアドレスからのホスト名逆引き処理を含めると、一部不具合が発生したためです。

    スループットの観点ではインメモリキューのほうが優れていると考えられます。キュー種別としてどちらを使用するかについては、まずインメモリキューを試してみて、問題が発生した場合にパーシステッドキューに切り替える、という方針でよさそうに思います。

次に、Logstashにおけるログ加工処理を記述する`logstash.conf`について見てみましょう。(コメント部分は除きます。)

注: 以下の内容は、構築手順の5番め(次々回の記事)で動作確認を行なうときに用いる、もっとも単純な設定であることに注意してください。本番運用時のログ加工ルールはもっと複雑なものになりますが、これについても次々回の記事で説明したいと思います。

- `/usr/local/etc/logstash/logstash.conf`

    ファイルは以下の三パートから構成されます。

    - 入力 - ログデータのソースを指定します。以下の例では、Beatsからログデータを受信します。
    - フィルタ - Logstashにおけるログ加工の肝になる部分です。以下の例では、最初の動作確認用なので、まだ何も処理を記述していません。したがって、受信ログをそのまま出力に渡します。
    - 出力 - ログデータの出力先を指定します。以下の例では、標準出力に出力します。

    ``` ruby
    input {                   # 入力
      beats {                 # ログソースはBeats
        port => 5044          # Logstashサーバの待ち受けポートは5044
        host => "0.0.0.0"     # Logstashサーバの全ネットワークインターフェイスで待ち受け
      }
    }
    
    filter {                  # フィルタ
    }                         # なにも処理しないでログをそのまま出力に渡す
    
    output {                  # 出力
      stdout {                # 出力先は標準出力
        codec => rubydebug    # RubyのAwesome Printを使用して見やすく整形
      }
    }
    ```

### 自動起動の設定
まだ完全に設定が終わったわけではありませんが、ひとまず、FreeBSDの起動時にLogstashが自動的に起動されるよう設定しておきましょう。

``` shell
sysrc logstash_enable=YES
```

ただし、自動起動の設定を行なうだけにとどめておきます。

### Logstashの起動確認
動作確認のための設定ができましたので、ここで一度、Logstashが起動することを確認しておきましょう。以下のコマンドを実行して、Logstashをテスト起動します。マシンスペックによっては起動までにかなり時間がかかりますので、しんぼう強くお待ちください。

``` shell
/usr/local/logstash/bin/logstash --path.settings /usr/local/etc/logstash --path.logs /var/log/logstash
```

うまく起動すれば、ログファイル`/var/log/logstash/logstash-plain.log`の最後に次のようなメッセージが表示されると思います。ログ加工用のパイプラインである`main`パイプラインと、モニタリング用のもう一つのパイプラインを合わせて、二つのパイプラインが動作中であることがわかればOKです。

``` log
[2018-04-30T13:57:33,230][INFO ][logstash.pipeline        ] Starting pipeline {:pipeline_id=>"main", "pipeline.workers"=>4, "pipeline.batch.size"=>125, "pipeline.batch.delay"=>50}
[2018-04-30T13:57:33,364][INFO ][logstash.inputs.beats    ] Beats inputs: Starting input listener {:address=>"0.0.0.0:5044"}
[2018-04-30T13:57:33,425][INFO ][logstash.pipeline        ] Pipeline started succesfully {:pipeline_id=>"main", :thread=>"#<Thread:0x430c412e sleep>"}
[2018-04-30T13:57:33,455][INFO ][org.logstash.beats.Server] Starting server on port: 5044
[2018-04-30T13:57:33,548][INFO ][logstash.agent           ] Pipelines running {:count=>2, :pipelines=>[".monitoring-logstash", "main"]}
[2018-04-30T13:57:33,586][INFO ][logstash.inputs.metrics  ] Monitoring License OK
```

無事起動を確認できたら、いったんLogstashを終了します。`Ctrl`+`c`で終了させてください。

最後に念のため、Logstashのデータディレクトリとログファイルディレクトリのオーナー、グループを修正しておきます。

``` shell
chown -R logstash:logstash /usr/local/logstash/data /var/log/logstash
```

以上で、Logstashのインストールと動作確認のための設定は終了です。次回の記事では、Beatsのインストールと設定を行ないます。Logstashの動作確認とログ加工処理の詳細については、次々回の記事で説明したいと思います。

### 参考文献
1. Installing Logstash, https://www.elastic.co/guide/en/logstash/6.2/installing-logstash.html
1. Installing X-Pack in Logstash, https://www.elastic.co/guide/en/logstash/6.2/installing-xpack-log.html
1. ELK Stackのインストール, https://gato.intaa.net/archives/11302
1. Elastic Stackでシステム監視 FreeBSDのportsで6.2.3に更新, https://gato.intaa.net/archives/12499
