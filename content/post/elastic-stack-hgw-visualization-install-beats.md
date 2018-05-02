+++
title = "FreeBSDとElasticスタックでホームゲートウェイRS-500KIのログを可視化する - インストール編(Beats)"
date = "2018-04-29T06:35:35+09:00"
draft = true
categories = ["Network"]
tags = ["freebsd", "elasticstack", "beats", "filebeat", "homegateway", "rs-500ki", "log", "vizualization"]
+++

[インストール編(Logstash)](/post/elastic-stack-hgw-visualization-install-logstash/)では、Logstashのインストールと動作確認のための設定を行ないました。Elasticスタックのインストールと、その後の可視化システムの構築の流れを再掲します。(下図)

1. [Elasticsearch, Elasticsearch X-Packのインストール](/post/elastic-stack-hgw-visualization-install-es/)(済)
1. [Kibana, Kibana X-Packのインストール、Basicライセンスの取得と投入](/post/elastic-stack-hgw-visualization-install-kibana/)(済)
1. [Logstash, Logstash X-Packのインストール](/post/elastic-stack-hgw-visualization-install-logstash/)(済)
1. **Beats (Filebeat)のインストール**(本記事)
1. Logstashのログ処理ルールの作成とテストデータでの動作確認
1. Filebeat→Logstash→Elasticsearchでの運用開始
1. Kibanaでの検索、ダッシュボードの作成

![Elasticスタックを用いたHGWログ可視化 - 構築の流れ - Beatsインストール](/img/elastic/elastic-stack-log-viz-install-beats.png)

本記事では、構築手順の4番めであるBeatsのインストールと設定を行ないます。インストールにあたり、Elasticsearch社の公式ガイド(以下の記事)を参考にします。

- [Getting Started With Filebeat (Elasticsearch)](https://www.elastic.co/guide/en/beats/filebeat/6.2/filebeat-getting-started.html)

また、[インストール編(Logstash)](/post/elastic-stack-hgw-visualization-install-logstash/)に引き続き、FreeBSD向けのインストールガイド的記事である、以下のURLも参考にしながら進めます。

- [Elastic Stackでシステム監視 FreeBSDのportsで6.2.3に更新 (がとらぼ)](https://gato.intaa.net/archives/12499)

### Beatsのインストール
これまでと同様に、以下のコマンドを実行してBeatsをインストールします。BeatsにはX-Packがありませんので、インストールはこれで完了です。

``` shell
pkg install beats
```

本コマンドにより、Filebeat, Heartbeat, Metricbeat, およびPacketbeatの四つのBeatがインストールされます。本記事ではホームゲートウェイのログファイルを扱いますので、以下ではFilebeatの設定について確認します。Beats全般について興味がある場合は、以下のURLを参照してみてください。

- [Beats: Data Shippers for Elasticsearch (Elasticsearch)](https://www.elastic.co/products/beats)

### Filebeatの設定ファイルの編集
では、Filebeatの設定ファイルについて見ていきます。設定ファイルは`/usr/local/etc/filebeat.yml`です。設定ファイルの主要部分を以下に示します。ただし、コメント部分は除きます。また、インストール時のデフォルトから変更していない部分についても省略しています。 

設定ファイルの完全なリファレンスが必要な場合は、以下のURLを参照してください。

- [filebeat.reference.yaml (Elasticsearch)](https://www.elastic.co/guide/en/beats/filebeat/6.2/filebeat-reference-yml.html)

以下、設定ファイルの内容を示します。

- `/usr/local/etc/filebeat.yml`

    {{< highlight yaml >}}
filebeat.prospectors:            # ログデータのソースを指定するパート(設定ファイルのメイン部分)
                                 ### テスト用データソース
- type: log                      # ソースタイプはログファイル
  enabled: true                  # テスト用の設定をとりあえず有効にしておく
  paths:                         # ログファイルのパス(複数指定も可能)
    - /tmp/hgw.log
  include_lines: ['hgw']         # ログファイルの'hgw'を含む行だけを送信
  fields:                        # 送信データに付け加えるメタデータ
    logtype: firewall_hgw        # ログの種別としてホームゲートウェイのログであることを示すタグを追加(任意の文字列)
- type: log                      ### 本番用データソース(ログファイルのパスのみ動作確認用設定から変更)
  enabled: false                 # 本番用設定はとりあえず無効化しておく
  paths:
    - /var/log/hgw.log
  include_lines: ['hgw']
  fields:
    logtype: firewall_hgw
(snip)
name: "<ノード名>"               # ノード名(コメントのままでも可)
(snip)
{{< /highlight >}}

### 自動起動の設定
FreeBSDの起動時に、Filebeatが自動的に起動されるよう設定しておきましょう。

``` shell
sysrc filebeat_enable=YES
```

ただし、本記事の時点では自動起動の設定を行なうだけにとどめておきます。

以上で、Beats (Filebeat)のインストールと設定は完了です。次回の記事では、Logstashの動作確認とログ加工処理の詳細について説明します。

### 参考文献
1. Getting Started With Filebeat, https://www.elastic.co/guide/en/beats/filebeat/6.2/filebeat-getting-started.html
1. Elastic Stackでシステム監視 FreeBSDのportsで6.2.3に更新, https://gato.intaa.net/archives/12499
1. Beats: Data Shippers for Elasticsearch, https://www.elastic.co/products/beats
1. filebeat.reference.yaml, https://www.elastic.co/guide/en/beats/filebeat/6.2/filebeat-reference-yml.html
