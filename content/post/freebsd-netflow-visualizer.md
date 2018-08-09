+++
title = "NetFlowを用いてFreeBSDマシンのトラフィックを可視化する(Flow Visualizer編)"
date = "2018-08-06T13:01:39+09:00"
draft = true
categories = ["FreeBSD"]
tags = ["freebsd", "netflow", "network", "traffic", "monitor", "statistics", "visualization", "nfsen"]
+++

[Flow Collector編](/post/freebsd-netflow-collector/)では、NFDUMPおよびNfSenを用いたNetFlow collectorのセットアップ手順を説明しました。NetFlowシリーズ最終回の本記事では、flow collectorが収集・蓄積したデータを可視化するNetFlow visualizerのインストールと設定について見ていきます(下図青色部分)。

![FreeBSD - NetFlow - Flow Visualizer](/img/freebsd/freebsd-netflow-visualizer.png)

「インストール」と書きましたが、flow visualizerとしては、Flow Collector編ですでに導入済みのNfSenを使用します。ただし、NfSenの可視化機能はPHPで記述されたWebフロントエンドになっていますので、可視化データを表示するためにはPHPをサポートするWebサーバをインストールする必要があります。

オープンソースのWebサーバといえば、[Apache](https://httpd.apache.org/)と[Nginx](https://nginx.org/)が現在の双璧ですね。

NFDUMP/NfSenを設定したマシンにたまたまApacheがインストール済みでしたので、本記事ではWebサーバにApacheを使うことにします。(すでに、Webサーバがインストール済みでPHPとの連携も設定済みの場合は、すぐに[NfSenの動作確認](#動作確認)に進めると思います。)

WebサーバにNginxを使用する場合は、以下のガイド記事に詳しい説明がありますのでご参照ください。

- [CONFIGURE NGINX WEB SERVER TO DISPLAY NFSEN (The FreeBSD Forums)](https://forums.freebsd.org/threads/howto-monitor-network-traffic-with-netflow-nfdump-nfsen-on-freebsd.49724/#post-277775)

では、NfSenを使ったflow visualizerの設定手順について説明していきます。

注: 以下、本記事ではApache, PHPともに、NfSenを動作させるために必要な最低限の設定のみを行ないます。FreeBSD, Apache, およびPHPを組み合わせた一般的なサーバ設定についてはさまざまなサイトで解説されていますが、代表的と思われる記事を二つ挙げておきます。ご参考まで。

- [How To Install an Apache, MySQL, and PHP (FAMP) Stack on FreeBSD 10.1 (DigitalOcean)](https://www.digitalocean.com/community/tutorials/how-to-install-an-apache-mysql-and-php-famp-stack-on-freebsd-10-1)
- [FreeBSD 10.3 に PHP 5.7をインストールする手順(Apache 2.4) (No IT No Life - おすぎやん サーバの設計・構築)](https://server-network-info.blogspot.com/2016/10/freebsd-10.html)

### インストール
まず、必要なパッケージのインストールからです。Apache Webサーバ、PHP、およびApache用のPHP連携モジュールをインストールします。「PHPのバージョンが5.6なのは今さら感が強いよね」と思われる場合は、お好みに合うバージョンのPHPを代わりにインストールしてみてください(PHP 5.6以外での動作は未確認)。

``` shell
pkg install apache24 php56 php56-extensions mod_php56
```

注: PHPスクリプトがUnixドメインソケットを用いてNfSenプロセスと通信を行ないます。したがって、上記パッケージをインストールするのはNfSenが動作するのと同一マシンである必要があります。

### PHPの設定
パッケージのインストールが終わったら、必要な設定を順番に行なっていきましょう。まず、PHPの設定です。設定ファイルのサンプルが`/usr/local/etc`以下に配置されていますので、プロダクション環境用の設定サンプルをコピーして`php.ini`ファイルを作成します。

``` shell
cp /usr/local/etc/php.ini-production /usr/local/etc/php.ini
```

NfSenが正常動作するための最低限の設定として、ここではタイムゾーンの設定だけを行ないます。以下の例では日本標準時に設定していますが、ここはお住まいの地域に合わせて適宜読み替えをお願いします。

- `/usr/local/etc/php.ini`

    - L934-936

        ``` ini
date.timezone = Asia/Tokyo
```

### Apacheの設定
PHPの設定が終わったら、次はApacheの設定です。

最低限、URLマッピングの設定、およびApacheとPHP連携の設定を行ないます。(URLマッピングは、NfSenのPHPスクリプト群がApacheの`DocumentRoot`と異なるパスにインストールされるため必要です。) 具体的には、Apacheの設定ファイルに対して以下の変更を加えます。

- `/usr/local/etc/apache24/httpd.conf`

    - Aliasモジュール

        {{< highlight apache >}}
<IfModule alias_module>
    (snip)
    Alias /nfsen "/usr/local/www/nfsen"    # この行を追加
    (snip)
</IfModule>
{{< /highlight >}}

    - MIMEモジュール

        {{< highlight apache >}}
<IfModule mime_module>
    (snip)
    AddType application/x-httpd-php .php            # この二行を追加
    AddType application/x-httpd-php-source .phps    #
    (snip)
</IfModule>
{{< /highlight >}}

### NfSenの設定(オプション)
NetFlow Collector編で、[NfSenのデータ格納ディレクトリをFreeBSDの標準的な構成に合わせた場合](/post/freebsd-netflow-collector/#nfsenの設定-オプション)、本項の設定を行なってください。

PHPスクリプト(`conf.php`)内で、NfSenプロセスと通信するためのソケットファイルのパスが指定されています。これを、Flow Collector編での設定と整合するように変更します。

- `/usr/local/www/nfsen/conf.php`

    - L4

        ``` php
$COMMSOCKET = "/var/run/nfsen/nfsen.comm";
```

### 自動起動の設定
FreeBSD起動時にWebサーバが自動的に起動されるよう設定をしておきましょう。

``` shell
sysrc apache24_enable=YES
```

最後に、以下のコマンドを実行するか、FreeBSDマシンを再起動してWebサーバを起動します。

```shell
service apache24 start
```

### 動作確認
では、簡単に動作確認をしておきましょう。Webブラウザを起動して以下のURLにアクセスしてください。

- `http://<Webサーバのホスト名 or IPアドレス>/nfsen/nfsen.php`

このURLにアクセスすると、まずデータの一覧画面(Home)が表示されます(下図)。

本画面は、すべてのソースからのNetFlowデータを一覧するオーバービュー画面になっています。(下図の例ではソースが一つだけですが、複数のソースがある場合はそれぞれが指定した色で表示されます。) 表示されるデータは、フロー毎秒、パケット毎秒、ビット毎秒というトラフィックデータで、これらが日、週、月、および年単位のスケールでそれぞれ表示されます。

[![FreeBSD - NfSen - Overview](/img/freebsd/freebsd-nfsen-overview-small.png)](/img/freebsd/freebsd-nfsen-overview.png)

上図のいずれかのグラフをクリックすると詳細画面(Details)に遷移します。

詳細画面はおおまかにいって、上部、下部に分かれます。まず、上部(下図)では、プロトコル(TCP, UDP, ICMP, およびその他)ごとのフロー数の推移が小グラフで表示され、これらを合算したものが大きなグラフで表示されています。また、グラフの下の表は、グラフ上で選択したタイムスロット(5分間)、あるいは複数のタイムスロットにわたる期間におけるフロー数などの統計情報が表示されます。

[![FreeBSD - NfSen - Details - Top](/img/freebsd/freebsd-nfsen-details-small.png)](/img/freebsd/freebsd-nfsen-details.png)

次に、詳細画面の下部(下図)では、GUI経由でデータソース、フィルタ、およびオプションを指定してnfdumpコマンドを実行できます。下図の例では、上部のグラフで選択した期間における宛先(DST)IPアドレスのトップ10をフロー数が多い順に表示しています。

[![FreeBSD - NfSen - Details - Bottom - 1](/img/freebsd/freebsd-nfsen-netflow-1-small.png)](/img/freebsd/freebsd-nfsen-netflow-1.png)

また、トップ10の表示においてIPアドレスの部分をクリック(下図)すると、そのIPアドレスに対するDNSの逆引き情報、およびWHOIS情報が表示されます。

[![FreeBSD - NfSen - Details - Bottom - 2](/img/freebsd/freebsd-nfsen-netflow-2-small.png)](/img/freebsd/freebsd-nfsen-netflow-2.png)

以上、動作確認をかねてNfSenのGUIを簡単に説明しました。

いかがでしたでしょうか? 正直なところ、今回の記事ではデータソースがFreeBSDマシン一台だけでしたので、可視化結果について少々もの足りない、といったところでしょうか。家庭内ネットワークよりは、やはりもう少し規模の大きいネットワークに適用してみたいところですね。

### 参考文献
1. The Apache HTTP Server Project, https://httpd.apache.org/
1. Nginx, https://nginx.org/
1. CONFIGURE NGINX WEB SERVER TO DISPLAY NFSEN, https://forums.freebsd.org/threads/howto-monitor-network-traffic-with-netflow-nfdump-nfsen-on-freebsd.49724/#post-277775
