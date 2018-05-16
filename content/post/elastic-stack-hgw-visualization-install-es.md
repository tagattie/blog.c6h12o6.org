+++
title = "FreeBSDとElasticスタックでホームゲートウェイRS-500KIのログを可視化する - インストール編(Elasticsearch)"
date = "2018-04-29T22:08:00+09:00"
categories = ["Network"]
tags = ["freebsd", "elasticstack", "elasticsearch", "homegateway", "rs-500ki", "log", "visualization"]
+++

[まえがき](/post/elastic-stack-hgw-visualization-intro/)では、ログデータの収集・可視化プラットフォームである[Elasticスタック](https://www.elastic.co/products)について簡単に紹介しました。今回からは、Elasticスタックを用いてホームゲートウェイのログを可視化するための、具体的な手順について説明していきます。

まずは、Elasticスタックのインストールから始めましょう。すでにまえがきで説明しましたが、Elasticスタックは以下の4つのソフトウェアにより構成されます。さらに、Logstash, Elasticsearch, およびKibanaの三者には、セキュリティ、アラート通知、モニタリングといった機能を追加する"X-Pack"という拡張パッケージがそれぞれ用意されています。

- Beats - ログをElasticsearchなどに送信するデータシッパー
- Logstash - ログを受信し、加工や整形などを行なうログコレクター**(X-Packあり)**
- Elasticsearch - ログを蓄積、インデクシングし、検索に供するデータベース**(X-Packあり)**
- Kibana - ログの検索、可視化のためのWebインターフェイス**(X-Packあり)**

X-Packを使用するためには、ユーザ登録を行なってライセンスの発行を受ける必要があります。ちなみに、最も安価なBasicライセンスは**無料**なのでご安心ください。ライセンスの種類によって使用できるX-Packの機能は異なりますので、詳しくは以下のURLを参照願います。

- [Subscriptions that Go to Work for You (Elasticsearch)](https://www.elastic.co/subscriptions)

今回、最も使いたいX-Packの機能は、Elasticスタックの各コンポーネントをモニタする機能です。本機能については、Basicライセンスで使用できる範囲に含まれますので、後ほどライセンスを取得することにしてX-Packもインストールしておきたいと思います。

Elasticスタックのインストールとその後の可視化システム構築は、以下のような流れで進めます。(下図)

1. **Elasticsearch, Elasticsearch X-Packのインストール**(本記事)
1. Kibana, Kibana X-Packのインストール、Basicライセンスの取得と投入
1. Logstash, Logstash X-Packのインストール
1. Beats (Filebeat)のインストール
1. Logstashのログ処理ルールの作成とテストデータでの動作確認
1. Filebeat→Logstash→Elasticsearchでの運用開始
1. Kibanaでの検索、ダッシュボードの作成

![Elasticスタックを用いたHGWログ可視化 - 構築の流れ - ESインストール](/img/elastic/elastic-stack-log-viz-install-es.png)

では、1番めのElasticsearchとElasticsearch X-Packのインストールからスタートです。インストールにあたり、Elasticsearch社の公式ガイド(以下の二つの記事)を参考にします。

- [Install Elasticsearch with .zip or .tar.gz (Elasticsearch)](https://www.elastic.co/guide/en/elasticsearch/reference/6.2/zip-targz.html)
- [Installing X-Pack in Elasticsearch (Elasticsearch)](https://www.elastic.co/guide/en/elasticsearch/reference/6.2/installing-xpack-es.html)

また、Elasticスタックのインストール全体をとおして、FreeBSDにおけるElasticスタックのインストール手順が詳しく説明されている、以下の二つの記事も参考にしながら進めます。

- [ELK Stackのインストール (がとらぼ)](https://gato.intaa.net/archives/11302)
- [Elastic Stackでシステム監視 FreeBSDのportsで6.2.3に更新 (がとらぼ)](https://gato.intaa.net/archives/12499)

さて、インストールの前に一点準備をしておきましょう。

### 必要なファイルシステムのマウント
ElasticsearchはJavaで書かれたプログラムです。Javaプログラムが動作するために、FreeBSDの側で少し設定が必要です。具体的には、プロセスファイルシステムとファイルディスクリプタファイルシステムがマウントされている必要があります。

以下の2行を`/etc/fstab`に追加してください。(もしなければ)

- `/etc/fstab`

    ``` conf
fdesc		/dev/fd		fdescfs		rw	0	0
proc		/proc		procfs		rw	0	0
```

その後、以下のコマンドを実行、あるいはFreeBSDマシンを再起動して、上記の各ファイルシステムをマウントします。

``` shell
mount fdesc
mount proc
```

以上で準備は完了です。

### Elasticsearchのインストール
ようやく、Elasticsearchのインストールまでこぎつけました。インストールは非常にかんたんで、以下のコマンドを実行すればOKです。

``` shell
pkg install elasticsearch6
```

### Elasticsearch X-Packのインストール
Elasticsearchがインストールできたら、次にX-Packをインストールします。以下のコマンドを実行してください。途中で2か所、処理を進めてよいか確認を求められますので、`y`を入力します。

``` shell-session
# /usr/local/lib/elasticsearch/bin/elasticsearch-plugin install x-pack
-> Downloading x-pack from elastic
[=================================================] 100%?? 
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
@     WARNING: plugin requires additional permissions     @
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
* java.io.FilePermission \\.\pipe\* read,write
* java.lang.RuntimePermission accessClassInPackage.com.sun.activation.registries
* java.lang.RuntimePermission getClassLoader
* java.lang.RuntimePermission setContextClassLoader
* java.lang.RuntimePermission setFactory
* java.net.SocketPermission * connect,accept,resolve
* java.security.SecurityPermission createPolicy.JavaPolicy
* java.security.SecurityPermission getPolicy
* java.security.SecurityPermission putProviderProperty.BC
* java.security.SecurityPermission setPolicy
* java.util.PropertyPermission * read,write
See http://docs.oracle.com/javase/8/docs/technotes/guides/security/permissions.html
for descriptions of what these permissions allow and the associated risks.

Continue with installation? [y/N]y                                                    # ←yを入力
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
@        WARNING: plugin forks a native controller        @
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
This plugin launches a native controller that is not subject to the Java
security manager nor to system call filters.

Continue with installation? [y/N]y                                                    # ←yを入力
Elasticsearch keystore is required by plugin [x-pack-security], creating...
-> Installed x-pack with: x-pack-logstash,x-pack-ml,x-pack-graph,x-pack-core,x-pack-deprecation,x-pack-monitoring,x-pack-security,x-pack-upgrade,x-pack-watcher
```

### 関連ファイルのグループ変更
以上でインストールは終了ですが、一部、関連ファイルのオーナーグループに不備があるのでこれを修正します。

``` shell
cd /usr/local/etc/elasticsearch
chgrp -R elasticsearch elasticsearch.keystore x-pack
```

### 設定ファイルの編集
次に設定ファイルを編集します。設定ファイルは`/usr/local/etc/elasticsearch/elasticsearch.yml`です。ファイルの内容を以下に示します。(コメント部分は除いています。)

- `/usr/local/etc/elasticsearch/elasticsearch.yml`

    ``` yaml
cluster.name: "クラスタ名"           # クラスタ名(任意の文字列、コメントのままでも可)
node.name: "ノード名"                # ノード名(任意の文字列、コメントのままでも可)
path.data: /var/db/elasticsearch     # データを格納するディレクトリ
path.logs: /var/log/elasticsearch    # ログを格納するディレクトリ
xpack.ml.enabled: false              # FreeBSDではMachine Learning (機械学習)機能が使えないので無効化しておく
```

### 自動起動の設定
FreeBSDの起動時に、Elasticsearchが自動的に起動されるよう設定しておきましょう。

``` shell
sysrc elasticsearch_enable=YES
```

設定ができたら、以下のコマンドを実行、あるいはFreeBSDマシンを再起動してElasticsearchを起動します。

``` shell
service elasticsearch start
```

### Elasticsearchの動作確認
Elasticsearchが起動しましたので、ここで一度動作確認を行なっておきましょう。`curl`コマンドを用いて以下のURLにアクセスします。

``` shell-session
$ curl -X GET 'http://localhost:9200/?pretty'
{
  "error" : {
    "root_cause" : [
      {
        "type" : "security_exception",
        "reason" : "missing authentication token for REST request [/?pretty]",
        "header" : {
          "WWW-Authenticate" : "Basic realm=\"security\" charset=\"UTF-8\""
        }
      }
    ],
    "type" : "security_exception",
    "reason" : "missing authentication token for REST request [/?pretty]",
    "header" : {
      "WWW-Authenticate" : "Basic realm=\"security\" charset=\"UTF-8\""
    }
  },
  "status" : 401
}
```

認証に失敗してエラーになっていますね。次は、Elasticsearchのユーザとパスワードの設定を行ないます。

### Elasticsearchのパスワード設定(elastic, kibana, logstash_systemユーザ)
以下のコマンドを実行します。`elastic`, `kibana`, および`logstash_system`ユーザのパスワードを設定するよう求められますので、それぞれのパスワードを入力してください。パスワードを手動で設定する代わりにランダムなパスワードを自動生成させたい場合は、コマンドライン末尾の`interactive`を`auto`に変更します。

``` shell-session
# bash /usr/local/lib/elasticsearch/bin/x-pack/setup-passwords interactive
Initiating the setup of passwords for reserved users elastic,kibana,logstash_system.
You will be prompted to enter passwords as the process progresses.
Please confirm that you would like to continue [y/N]y    # ←yを入力


Enter password for [elastic]:                            # ←パスワードを設定
Reenter password for [elastic]:                          # ←パスワードを再入力
Enter password for [kibana]:                             # ←パスワードを設定
Reenter password for [kibana]:                           # ←パスワードを再入力
Enter password for [logstash_system]:                    # ←パスワードを設定
Reenter password for [logstash_system]:                  # ←パスワードを再入力
Changed password for user [kibana]
Changed password for user [logstash_system]
Changed password for user [elastic]
```

### Elasticsearchの動作確認(再)
認証情報を設定したら、再度動作確認を行ないます。今度は、ユーザ`elastic`とパスワードを指定してアクセスしてみましょう。

``` shell-session
$ curl -u elastic:<elasticユーザのパスワード> -X GET 'http://localhost:9200/?pretty'
{
  "name" : "<ノード名>",
  "cluster_name" : "<クラスタ名>",
  "cluster_uuid" : "xxxxxxxxxxxxxxxxxxxxxx",
  "version" : {
    "number" : "6.2.3",
    "build_hash" : "c59ff00",
    "build_date" : "2018-03-13T10:06:29.741383Z",
    "build_snapshot" : false,
    "lucene_version" : "7.2.1",
    "minimum_wire_compatibility_version" : "5.6.0",
    "minimum_index_compatibility_version" : "5.0.0"
  },
  "tagline" : "You Know, for Search"
}
```

Elasticsearchのノード名、クラスタ名、バージョン情報などの基本情報が表示されました。認証成功です。

以上で、Elasticsearchのインストールと設定は完了です。次回の記事では、Kibanaのインストールと設定を行ないます。

### 参考文献
1. The Open Source Elastic Stack, https://www.elastic.co/products
1. Subscriptions that Go to Work for You, https://www.elastic.co/subscriptions
1. Install Elasticsearch with .zip or .tar.gz, https://www.elastic.co/guide/en/elasticsearch/reference/6.2/zip-targz.html
1. Installing X-Pack in Elasticsearch, https://www.elastic.co/guide/en/elasticsearch/reference/6.2/installing-xpack-es.html
1. ELK Stackのインストール, https://gato.intaa.net/archives/11302
1. Elastic Stackでシステム監視 FreeBSDのportsで6.2.3に更新, https://gato.intaa.net/archives/12499
