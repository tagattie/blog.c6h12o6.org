+++
title = "FreeBSDとElasticスタックでホームゲートウェイRS-500KIのログを可視化する - 設定編(Logstash)"
date = "2018-05-07T21:14:00+09:00"
categories = ["Network"]
tags = ["freebsd", "elasticstack", "logstash", "grok", "filebeat", "homegateway", "rs-500ki", "log", "visualization"]
+++

[インストール編(Beats)](/post/elastic-stack-hgw-visualization-install-beats/)では、BeatsのインストールとFilebeatの設定を行ないました。Elasticスタックのインストールと、その後の可視化システムの構築の流れを再掲します。(下図)

1. [Elasticsearch, Elasticsearch X-Packのインストール](/post/elastic-stack-hgw-visualization-install-es/)(済)
1. [Kibana, Kibana X-Packのインストール、Basicライセンスの取得と投入](/post/elastic-stack-hgw-visualization-install-kibana/)(済)
1. [Logstash, Logstash X-Packのインストール](/post/elastic-stack-hgw-visualization-install-logstash/)(済)
1. [Beats (Filebeat)のインストール](/post/elastic-stack-hgw-visualization-install-beats/)(済)
1. **Logstashのログ処理ルールの作成とテストデータでの動作確認**(本記事)
1. Filebeat→Logstash→Elasticsearchでの運用開始
1. Kibanaでの検索、ダッシュボードの作成

![Elasticスタックを用いたHGWログ可視化 - 構築の流れ - Logstashルール](/img/elastic/elastic-stack-log-viz-config-logstash.png)

本記事では、テスト用のログデータを用いて、FilebeatからLogstashにデータが送信されることを確認します。その後、Logstashによるログデータ加工のための処理ルールについて説明していきます。そして最後に、再びテストデータを用いて、Logstashの処理ルールが正しく動作することを確認します。

### テスト用ログファイルの作成
まず、テスト用のログデータを用意しましょう。用意といっても、新たにテストデータを作るわけではなく、すでにあるホームゲートウェイのログから適当な一行を切り出せばOKです。[インストール編(Beats)](/post/elastic-stack-hgw-visualization-install-beats/)で、テスト用のログファイルパスを`/tmp/hgw.log`と指定しましたので、例えば以下の内容のファイルを`/tmp/hgw.log`として保存します。

- `/tmp/hgw.log`

    ``` log
Apr 28 00:15:00 filebeat hgw: 2018/4/28 00:14:51 SRC=62.210.180.80/5283 DST=203.0.113.1/5090 UDP table=spi
```

### FilebeatからLogstashへのテストデータ送信
テストファイルが作成できましたので、さっそくFilebeatからLogstashへ送信してみましょう。まず、Logstashを起動します。

``` shell
/usr/local/logstash/bin/logstash --path.settings /usr/local/etc/logstash --path.logs /var/log/logstash
```

次に、Filebeatを起動します。

``` shell
service filebeat start
```

一瞬の後に、以下のような出力がLogstashを起動したターミナルに表示されます。出力内容の中に`message`という要素が含まれており、用意したテスト用ログの一行がそのまま含まれていることを確認してください。このように、Logstashが受信したログデータは、特に指定を行なわなければ`message`という要素に格納されます。これ以外のデータ要素はFilebeat、あるいはLogstashが付加したメタデータです。

``` ruby
{
        "source" => "/tmp/hgw.log",
    "@timestamp" => 2018-04-30T05:27:34.305Z,
       "message" => "Apr 28 00:15:00 filebeat hgw: 2018/4/28 00:14:51 SRC=62.210.180.80/5283 DST=203.0.113.1/5090 UDP table=spi",
          "tags" => [
        [0] "beats_input_codec_plain_applied"
    ],
          "host" => "filebeat.example.com",
        "fields" => {
        "logtype" => "firewall_hgw"
    },
      "@version" => "1",
          "beat" => {
            "name" => "Filebeat",
        "hostname" => "filebeat.example.com",
         "version" => "6.2.3"
    },
        "offset" => 109,
    "prospector" => {
        "type" => "log"
    }
}
```

### Logstashを用いたログデータの加工
テストデータの送受信はうまく行きましたか? いいですね。では、いったんLogstashとFilebeatを停止させておいてください。

次は、本記事で構築しようとしているログ可視化システムの肝になる部分、Logstashによるログデータの加工について説明します。

以下の記事では、Logstashを用いてApacheログを加工する手順について詳しく紹介しています。本記事ではホームゲートウェイのログを扱いますので、ソースデータの種類は異なります。しかし、基本的な処理の流れは同じですので、こちらの記事も参考にしていただければと思います。

- [Logstashを利用したApacheアクセスログのインポート (@johtaniの日記 2nd)](http://blog.johtani.info/blog/2014/11/21/import-apache-accesslog-using-logstash/)

さて、本記事におけるログ処理の流れは以下のようになります。

1. 入力元からログデータを受信
1. 受信ログをパース
1. `message`フィールドを削除(パース成功時のみ)
1. タイムスタンプを修正
1. `source_host`フィールドを追加
1. アクセス元の地理的位置を検索
1. アクセス元のドメイン名を逆引き
1. 加工済みのログデータを送信

本処理を記述する設定ファイルは`/usr/local/etc/logstash/logstash.conf`です。まず、ファイルの全体を示しておきます。

- `/usr/local/etc/logstash/logstash.conf`

    {{< highlight ruby >}}
input {                                                         # 1. 入力元からログデータを受信
  beats {
    port => 5044
    host => "0.0.0.0"
  }
}

filter {
  if [fields][logtype] == "firewall_hgw" {
    grok {                                                      # 2. 受信ログをパース
      patterns_dir => [ "/usr/local/etc/logstash/patterns" ]    
      match => {
        "message" => [
          "%{SYSLOGBASE} %{HGWLOG}",
          "%{SYSLOGBASE} %{GREEDYDATA:raw_message}"
        ]
      }
    }
    if "_grokparsefailure" not in [tags] {
      mutate {                                                  # 3. messageフィールドを削除(パース成功時のみ)
        remove_field => [ "message" ]
      }
    }
    date {                                                      # 4. タイムスタンプを修正
      match => [ "hgw_timestamp", "yyyy/M/d HH:mm:ss" ]
    }
    mutate {                                                    # 5. source_hostフィールドを追加
      add_field => { "source_host" => "%{source_ip}" }
    }
    geoip {                                                     # 6. アクセス元の地理的位置を検索
      source => "source_ip"
      target => "geoip"
    }
    dns {                                                       # 7. アクセス元のドメイン名を逆引き
      reverse => [ "source_host" ]
      action => "replace"
      nameserver => [ "192.168.1.251", "192.168.1.247" ]
      failed_cache_size => 1000
      failed_cache_ttl => 300
      hit_cache_size => 1000
      hit_cache_ttl => 300
    }
  }
}

output {                                                        # 8. 加工済みのログデータを送信
  stdout { codec => rubydebug }
}
{{< /highlight >}}

1. 入力元からログデータを受信

    {{< highlight ruby >}}
input {
  beats {
    port => 5044
    host => "0.0.0.0"
  }
}
{{< /highlight >}}

    [Beatsプラグイン](https://www.elastic.co/guide/en/logstash/6.2/plugins-inputs-beats.html)を用いて、Filebeatからログデータを受信します。本パートはテスト用の設定から変更していません。

1. 受信ログをパース

    {{< highlight ruby >}}
if [fields][logtype] == "firewall_hgw" {
  grok {
    patterns_dir => [ "/usr/local/etc/logstash/patterns" ]
    match => {
      "message" => [
        "%{SYSLOGBASE} %{HGWLOG}",
        "%{SYSLOGBASE} %{GREEDYDATA:raw_message}"
      ]
    }
  }
{{< /highlight >}}

    [インストール編(Beats)](/post/elastic-stack-hgw-visualization-install-beats/)でFilebeatの設定を行ないましたが、この際、送信データに付け加えるメタデータとして、`fields`に`logtype: firewall_hgw`を設定したことを覚えていますか? フィルタパートの一番はじめにこの値を確認します。`logtype`が`firewall_hgw`のときにのみ、後続の処理を行なうことにご注意ください。(今後、複数種類のログを扱うことを考慮しているためです。)
    
    `logtype`が`firewall_hgw`に一致する場合は、[Grokプラグイン](https://www.elastic.co/guide/en/logstash/6.2/plugins-filters-grok.html)を用いて、ログデータ(文字列)と正規表現(以下、パターンと呼びます)とのマッチングを行ない、ログを複数のフィールド(文字列)に分解します。
    
    上記の例では、`message`要素に格納されている一行分のログと二つのパターン(`%{SYSLOGBASE} %{HGWLOG}`および`%{SYSLOGBASE} %{GREEDYDATA:raw_message}`)のいずれかとのマッチングを試みます。マッチ処理が成功した場合、マッチした部分文字列がパターン内で指定したフィールドに格納されます。
    
    Grokプラグインを用いてログをパースする本部分は、一連の手順の中でももっとも複雑で難しいところですので、もう少し説明を加えます。
    
    マッチングに用いるパターンの基本的な形は以下のとおりです。

    ``` grok
%{SYNTAX:semantics}
```

    `SYNTAX`が「ログとのマッチング対象となる正規表現」、`semantics`が「ログのマッチした部分文字列に対応させるフィールド名」です。部分文字列に対するフィールド名の対応付けが必要ない場合は`:semantics`の部分を省略します。

    正規表現ときいて、おぞけをふるうかたもいらっしゃる(かくいうわたしもその一人です)でしょうが、それほど心配する必要はなさそうです。というのは、Grokプラグインには、はじめから多くの[組み込みパターン](https://github.com/logstash-plugins/logstash-patterns-core/tree/master/patterns)が用意されていますので、ゼロから正規表現を構築する必要があるケースはかなり少ないと考えられるためです。

    組み込みパターンをできるだけ活用しつつ、既存のパターンから変更したい部分、あるいは不足しているパターンのみを作成する、というやり方が大半になるのではないかと思います。本記事の例でも、まったく新しい正規表現を書き起こすことはやっていません。

    では、パターンを作成するため、もう一度テスト用のログデータを見直してみましょう。

    ``` log
Apr 28 00:15:00 filebeat hgw: 2018/4/28 00:14:51 SRC=62.210.180.80/5283 DST=203.0.113.1/5090 UDP table=spi
```

    [以前の記事](/post/home-gateway-rs500ki-syslog/)で述べましたように、本ログは`logger(1)`経由でsyslogに出力しています。したがって、ログは[syslogのフォーマット](https://tools.ietf.org/html/rfc3164)にそった形式になっています。組み込みパターン([grok-patterns](https://github.com/logstash-plugins/logstash-patterns-core/blob/master/patterns/grok-patterns))に`SYSLOGBASE`という正規表現がすでに定義されていますので、上記メッセージの`Apr 28 00:15:00 filebeat hgw:`までの部分については、本パターンを使ってマッチングを行ないます。

    以降の部分はホームゲートウェイRS-500KIの独自形式ですので、新しくパターンを作成する必要があります。この部分のフォーマットは以下のようになっています。

    ```
<タイムスタンプ> SRC=<ソースIPアドレス>/<ソースポート> DST=<デスティネーションIPアドレス>/<デスティネーションポート> <プロトコル> <残りのメッセージ>
```

    これにマッチするように定義したパターン`HGWLOG`を以下に示します。ご覧いただければわかると思いますが、ゼロから正規表現を書き起こすことはしていません。IPアドレスやポート番号にマッチする正規表現は、すでに組み込みパターン([grok-patterns](https://github.com/logstash-plugins/logstash-patterns-core/blob/master/patterns/grok-patterns))の中で定義済みですので、適宜これを組み合わせることで目的を達しています。
    
    新たに定義したカスタムパターンは`hgw`というファイル内に記述し、`patterns_dir`で指定したディレクトリ`/usr/local/etc/logstash/patterns`以下に格納しました。

    - `/usr/local/etc/logstash/patterns/hgw`

        ``` grok
DATE_YMD %{YEAR}[/-]%{MONTHNUM}[/-]%{MONTHDAY}
TIMESTAMP_HGW %{DATE_YMD}[- ]%{TIME}
HGWLOG %{TIMESTAMP_HGW:hgw_timestamp} SRC=%{IPORHOST:source_ip}/%{POSINT:source_port} DST=%{IPORHOST:dest_ip}/%{POSINT:dest_port} %{DATA:protocol} %{GREEDYDATA:message}
```

    `SYSLOGBASE`と`HGWLOG`を組み合わせて、最終的にはログデータを`timestamp`, `logsource`, `program`, `hgw_timestamp`, `source_ip`, `source_port`, `dest_ip`, `destport`, `protocol`, および`message`の各フィールドに分解しました。(下図)

    ![カスタムパターンを用いたログデータのパース](/img/elastic/elastic-stack-hgw-grok-pattern.png)
    
    ゼロから正規表現を書き起こす必要はほとんどないとはいえ、やはり、Grokプラグインで用いるパターンを作成するのがLogstashの設定における最難関であることに変わりはありません。そこで、パターンを作成するときに役立つ支援ツールを二つ紹介しておきたいと思います。

    一つめは、X-Packに含まれているGrok Debuggerです(下図)。テスト用のログデータ(一行分)とパターンを記述すると、パターンに対するマッチング処理の結果をJSON形式でアウトプットしてくれます。カスタムパターンもサポートされています。
    
    [![Kibana X-Pack - Grok Debugger](/img/elastic/elastic-stack-hgw-grok-debugger-small.png)](/img/elastic/elastic-stack-hgw-grok-debugger.png)

    二つめは、X-PackのGrok Debuggerとほぼ同様の機能を提供する以下のWebサイトです。こちらは、テストデータやパターンを変更すると自動的にマッチング処理を走らせてくれますので、試行錯誤しながらパターンを開発する場合にはこちらのほうが便利ですね。

    - [Grok Debugger (Heroku)](https://grokdebug.herokuapp.com/)

1. `message`フィールドを削除(パース成功時のみ)

    {{< highlight ruby >}}
if "_grokparsefailure" not in [tags] {
  mutate {
    remove_field => [ "message" ]
  }
}
{{< /highlight >}}

    [Mutateプラグイン](https://www.elastic.co/guide/en/logstash/6.2/plugins-filters-mutate.html)を用いて、`message`フィールドをログデータから削除します。
    
    ただし、`tags`フィールドに`_grokparsefailure`が含まれていない場合に限ります。これは、Grokプラグインでのパースが成功し、`message`が所定のフィールドに分解できた場合に対応します。
    
    もちろん、`message`に格納されている、もともとの(分解前の)ログデータを保持しておいてもかまわないのですが、分解済みの各フィールドと両方保持するのは冗長であるため、本記事では`message`フィールドを削除することにしました。

1. タイムスタンプを修正

    {{< highlight ruby >}}
date {
  match => [ "hgw_timestamp", "yyyy/M/d HH:mm:ss" ]
}
{{< /highlight >}}
    
    [Dateプラグイン](https://www.elastic.co/guide/en/logstash/6.2/plugins-filters-date.html)を用いて、ログデータのタイムスタンプ(`@timestamp`フィールド)の値を修正します。
    
    デフォルトでは、ログデータのタイムスタンプとして、Logstashがデータを受信した時刻が使われます。しかし、ログデータを可視化する観点では、Logstashのデータ受信時刻には関心がありません。それよりも、ホームゲートウェイがログを記録した時刻、つまりパース処理時に抽出した`hgw_timestamp`の値をタイムスタンプとして使えるほうが便利です。
    
    フィールド名と、そのフィールドに含まれるタイムスタンプのフォーマットを指定してタイムスタンプの値を取り出し、`@timestamp`フィールドに格納します。

1. `source_host`フィールドを追加

    {{< highlight ruby >}}
mutate {
  add_field => { "source_host" => "%{source_ip}" }
}
{{< /highlight >}}

    再び[Mutateプラグイン](https://www.elastic.co/guide/en/logstash/6.2/plugins-filters-mutate.html)を用います。今度は、新たに`source_host`というフィールドを追加します。フィールドの内容については`source_ip`フィールドからコピーします。
    
    こんなことをする理由は、この先で行なうドメイン名の逆引き結果を格納するフィールドをあらかじめ作っておくためです。(ドメイン名の逆引きでは、既存のフィールドの値を逆引き結果で上書きする形をとります。)

1. アクセス元の地理的位置を検索

    {{< highlight ruby >}}
geoip {
  source => "source_ip"
  target => "geoip"
}
{{< /highlight >}}

    [Geoipプラグイン](https://www.elastic.co/guide/en/logstash/6.2/plugins-filters-geoip.html)を用いて、`source_ip`フィールドに格納されているアクセス元のIPアドレスから、その地理的位置を検索します。検索結果は`geoip`フィールドに格納します。

1. アクセス元のドメイン名を逆引き

    {{< highlight ruby >}}
dns {
  reverse => [ "source_host" ]
  action => "replace"
  nameserver => [ "192.168.1.251", "192.168.1.247" ]
  failed_cache_size => 1000
  failed_cache_ttl => 300
  hit_cache_size => 1000
  hit_cache_ttl => 300
}
{{< /highlight >}}

    [DNSプラグイン](https://www.elastic.co/guide/en/logstash/6.2/plugins-filters-dns.html)を用いて、先ほど`source_ip`フィールドをコピーして作成した、`source_host`に格納されているアクセス元のIPアドレスの逆引き検索を行ないます。逆引きを行なうと、IPアドレスに対応するドメイン名(FQDN)が得られます。逆引きが成功した場合は、その結果を用いて`source_host`フィールドの内容を置き換えます。
    
    注: 手もとの環境では`nameserver`を明示的に指定しないと逆引きがうまく行きませんでしたので、適切なネームサーバのIPアドレスを指定することをおすすめします。

1. 加工済みのログデータを送信

    {{< highlight ruby >}}
output {
  stdout { codec => rubydebug }
}
{{< /highlight >}}

    まだ動作テストの段階ですので、出力パートについては変更せず、標準出力に出力されるようにしておきます。

### FilebeatからLogstashへのテストデータ送信(再)
あー、長かったですね。やっとLogstashの設定がほぼ終わりました。では、再度テスト用のログデータを用いて、Logstashの動作確認を行ないましょう。まず、Logstashを起動します。

``` shell
/usr/local/logstash/bin/logstash --path.settings /usr/local/etc/logstash --path.logs /var/log/logstash
```

次はFilebeatの起動ですが、その前にひと手間必要です。Filebeatは、(再起動された場合などに)すでに送信済みのログを再び送信することがないよう、[`registry`](https://www.elastic.co/guide/en/beats/filebeat/6.2/how-filebeat-works.html#_how_does_filebeat_keep_the_state_of_files)というファイルを用いて送信済みデータなどの情報を管理しています。今回は、テストのため送信済みデータを再度送信したいので、このファイルを起動前に削除しておきます。

``` shell
rm /var/db/beats/filebeat/data/registry
```

その後、Filebeatを起動します。

``` shell
service filebeat start
```

一瞬の後に、以下のような出力がLogstashを起動したターミナルに表示されると思います。設定したLogstashの処理がすべて問題なく行なわれているか、出力メッセージを目視して確認します。主要な確認ポイントは以下のとおりです。

- `timestamp`, `logsource`, `program`, `hgw_timestamp`, `source_ip`, `source_port`, `dest_ip`, `dest_port`, および`protocol`の各フィールドが存在すること
- `message`フィールドが削除されていて存在しないこと
- `@timestmp`と`hgw_timestamp`が同一の時刻を指していること(おそらく`@timestamp`はUTCで、`hgw_timestamp`はJSTで表示されていると思います)
- `geoip`フィールドが存在し、地理的位置情報が格納されていること
- `source_host`フィールドが存在し、FQDNあるいは(逆引きが失敗した場合)IPアドレスが格納されていること

``` ruby
{
          "dest_ip" => "203.0.113.1",
        "timestamp" => "Apr 28 00:15:00",
           "source" => "/tmp/hgw.log",
           "fields" => {
        "logtype" => "firewall_hgw"
    },
             "host" => "filebeat.example.com",
        "source_ip" => "62.210.180.80",
      "source_port" => "5283",
        "dest_port" => "5090",
           "offset" => 109,
             "beat" => {
        "hostname" => "filebeat.example.com",
            "name" => "Filebeat",
         "version" => "6.2.3"
    },
            "geoip" => {
         "country_code2" => "FR",
              "location" => {
            "lat" => 48.8582,
            "lon" => 2.3387000000000002
        },
             "longitude" => 2.3387000000000002,
                    "ip" => "62.210.180.80",
              "timezone" => "Europe/Paris",
              "latitude" => 48.8582,
         "country_code3" => "FR",
        "continent_code" => "EU",
          "country_name" => "France"
    },
    "hgw_timestamp" => "2018/4/28 00:14:51",
       "prospector" => {
        "type" => "log"
    },
       "@timestamp" => 2018-04-27T15:14:51.000Z,
          "program" => "hgw",
      "source_host" => "62-210-180-80.rev.poneytelecom.eu",
             "tags" => [
        [0] "beats_input_codec_plain_applied"
    ],
         "@version" => "1",
         "protocol" => "UDP",
        "logsource" => "filebeat"
}
```

以上で、Logstashの処理ルールについての説明は終了です。FilebeatおよびLogstashを停止させ、再度registryファイルを削除しておいてください。

次回の記事では、これまでに説明してきた各コンポーネントの設定ファイルの最終的な内容を示します。その後、システム全体を(再)起動して、本格運用を開始します。

### 参考文献
1. Logstashを利用したApacheアクセスログのインポート, http://blog.johtani.info/blog/2014/11/21/import-apache-accesslog-using-logstash/
1. Beats input plugin, https://www.elastic.co/guide/en/logstash/6.2/plugins-inputs-beats.html
1. Grok filter plugin, https://www.elastic.co/guide/en/logstash/6.2/plugins-filters-grok.html
1. RFC 3164, The BSD syslog Protocol, https://tools.ietf.org/html/rfc3164
1. Grok Debugger, https://grokdebug.herokuapp.com/
1. Mutate filter plugin, https://www.elastic.co/guide/en/logstash/6.2/plugins-filters-mutate.html
1. Date filter plugin, https://www.elastic.co/guide/en/logstash/6.2/plugins-filters-date.html
1. Geoip filter plugin, https://www.elastic.co/guide/en/logstash/6.2/plugins-filters-geoip.html
1. Dns filter plugin, https://www.elastic.co/guide/en/logstash/6.2/plugins-filters-dns.html
1. How Filebeat works, https://www.elastic.co/guide/en/beats/filebeat/6.2/how-filebeat-works.html
