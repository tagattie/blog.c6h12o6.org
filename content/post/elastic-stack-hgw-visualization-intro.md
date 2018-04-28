+++
title = "FreeBSDとElasticスタックでホームゲートウェイRS-500KIのログを可視化する - まえがき"
date = "2018-04-28T19:27:03+09:00"
draft = true
categories = ["Network"]
tags = ["freebsd", "elasticstack", "elasticsearch", "kibana", "logstash", "beats", "filebeat", "homegateway", "rs-500ki"]
+++

[先日の記事](/post/home-gateway-rs500ki-syslog/)で、NTTの[ホームゲートウェイRS-500KI](http://web116.jp/shop/hikari_r/rs_500ki/rs_500ki_00.html)のログを取得して、syslogに出力するプログラムを紹介しました。その後、本プログラムを動かし続けてきた結果、だいぶんログが蓄積されました。そこで、収集したログの可視化をそろそろ行なってみたいと思います。

ログの収集・可視化ソフトウェアにはいろいろありますが、最近では[Elasticスタック](https://www.elastic.co/jp/products)の人気が高いようです。こういう分野に興味があるかたにとってはいうまでもないことですが、Elasticスタックとは、以下の4つのソフトウェアにより構成される、ログの収集、加工、蓄積、検索、および可視化のためのプラットフォームです。

- Beats - ログをElasticsearchなどに送信するデータシッパー
- Logstash - ログを受信し、加工や整形などを行なうログコレクター
- Elasticsearch - ログを蓄積、インデクシングし、検索を行なうデータベース
- Kibana - ログの検索、可視化のためのWebインターフェイス

ところで、Elasticスタックという名前について、Elasticsearch社の[ブログ](https://www.elastic.co/jp/blog/heya-elastic-stack-and-x-pack)にちょっとおもしろい話が掲載されています。

- [こんにちは、Elastic StackとX-Pack (Elasticsearch)](https://www.elastic.co/jp/blog/heya-elastic-stack-and-x-pack)

Elasticスタックは、もともとELKスタックという通称で呼ばれていました。Elasticsearch, Logstash, Kibanaの頭文字の組み合わせですね。スタック上での位置づけを考えると、頭文字の順序が入れ替わっていますが、これは英語でelkがヘラジカを意味しており、語呂がよかったためと思われます。

ところが、新しくBeatsがスタックに加わるにあたり、ELKにBを加えた名前を考えたそうですが、誰もよいアイディアが浮かばなかったとのこと。それで、Elasticスタックという名前に落ちついたそうです。成長するソフトウェアの悩みにはいろいろあるものですね。

話をそらしてしまいましたが、ELKスタックあらためElasticスタックを使って、ホームゲートウェイのログ可視化を試みていきたいと思います。可視化の全体イメージは、以下のブロック図に示すとおりです。

![Elasticスタックを用いたHGWログ可視化 - 全体イメージブロック図](/img/elastic/elastic-stack-hgw-log-visualization.png)

すでに、ログ取得プログラムを定期的に動かし続けていますので、ログがファイルとしてたまっています。まず、Beatsファミリーの一員であるFilebeatを使ってログファイルを逐次読み出していきます。Logstashでログデータの加工、整形を行ない、その結果をElasticsearchに蓄積します。そして、Kibanaを使ってログの検索、可視化を行なうという要領になります。

では、次回の記事以降、ログ可視化システムの構築手順について説明していきます。

### 参考文献
1. ホームゲートウェイ/ひかり電話ルータ (RS-500KI), http://web116.jp/shop/hikari_r/rs_500ki/rs_500ki_00.html
1. オープンソースのElastic Stack, https://www.elastic.co/jp/products
1. こんにちは、Elastic StackとX-Pack, https://www.elastic.co/jp/blog/heya-elastic-stack-and-x-pack
