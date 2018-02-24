+++
title = "ホームゲートウェイRS-500KIのセキュリティログをSyslogに出力する"
date = "2018-02-24T15:11:00+09:00"
categories = ["Network"]
tags = ["flets", "homegateway", "rs-500ki", "log", "syslog", "freebsd"]
+++

わが家では、フレッツ光+IIJmioでインターネットに接続しています。ホームゲートウェイ(Home Gateway, HGW)として、NTT東日本から提供された[RS-500KI](http://web116.jp/shop/hikari_r/rs_500ki/rs_500ki_00.html)という機種を使っています。

問題なく元気に動いていると書きたいところなのですが、このHGW、無線LAN機能が不安定で、数時間から数日の間隔ですべての機能もろともフリーズしてしまうという問題があります。(pingにも応答しなくなってしまい、対応は本体の再起動スイッチを押して再起動するのみ。) [こちら](http://strategics.hatenablog.com/entry/2016/04/18/141956)でも、同一機種で無線LANの問題が報告されていますので、うちの個体固有の問題ではないのかもしれません。とにかく、早々に無線LAN機能はあきらめてOFFにし、別のWiFiルータを購入しました。

いきなりグチをこぼしてしまいましたが、本題は以下です。

本HGWは、ファイアウォールルータとしての機能はひととおりそろっており、IPv4およびIPv6ともパケットフィルタリングの機能があります。フィルタリングのログについてはWebブラウザを用いて確認できるようになっています。(下図)

[![RS-500KIのセキュリティログ表示画面](/img/rs500ki-packet-filter-log-small.png)](/img/rs500ki-packet-filter-log.png)

しかしながら、上図中にも表示されているとおり、ログはIPv4/IPv6おのおのについて最新の100件だけが表示されます。これより古いログについては今のところ取り出す方法がわかりません。(おそらく、100件を超える分については破棄され、取り出すことができない仕様ではないかと推測します。)

これはちょっともったいないので、このログを定期的に取り出してSyslog経由でファイルに出力することを考えます。そうして保存しておけば、後々のログ分析にも使えます。

ブラウザでは右側のフレーム内にログが表示されますが、以下のURLで直接ログデータにアクセスすることができます。

- IPv4: `http://<HGWのIPアドレス>/ntt/information/v4SecurityLog`
- IPv6: `http://<HGWのIPアドレス>/ntt/information/v6SecurityLog`

取得したHTMLのソースを見てみると、
```html
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd"><html><head><meta http-equiv="Content-Type" content="text/html; charset=UTF-8"><meta http-equiv="default-style" content="text/css"><link rel="stylesheet" type="text/css" href="/ntt/resources/css/style.css"><script type="text/javascript" src="/ntt/resources/js/jquery.js"></script><script type="text/javascript" src="/ntt/resources/js/jquery.i18n.properties.js"></script><script type="text/javascript" src="/ntt/resources/js/common.js"></script><script type="text/javascript" src="/ntt/webgui/resources/js"></script><title>セキュリティログ(IPv4)</title></head><body><div id="title"><h1>セキュリティログ(IPv4)</h1><div id="help"><a href="/ntt/webgui/help/information/v4securityLog.html" target="_blank"><img src="/ntt/resources/image/help.png" alt="help" /></a></div></div><div id="breadcrumb">トップページ ＞ 情報 ＞ <b>セキュリティログ(IPv4)</b></div><div class="section"><h2>[ セキュリティログ(IPv4) ]</h2><pre class="log">ip4_security : There are 100 entries.

  1. 2018/2/23 08:47:59 SRC=71.6.158.166/36040 DST=XX.XX.XX.XX/6969 UDP table=spi
  2. 2018/2/23 08:23:19 SRC=134.119.213.127/6211 DST=XX.XX.XX.XX/5060 UDP table=spi
  3. 2018/2/23 07:40:49 SRC=51.15.208.144/5083 DST=XX.XX.XX.XX/5060 UDP table=spi
(snip)
 98. 2018/2/22 13:35:30 SRC=51.15.208.144/5079 DST=XX.XX.XX.XX/5060 UDP table=spi
 99. 2018/2/22 13:16:32 SRC=216.218.206.95/7666 DST=XX.XX.XX.XX/111 UDP table=spi
100. 2018/2/22 13:04:11 SRC=134.119.213.127/6205 DST=XX.XX.XX.XX/5060 UDP table=spi
</pre> </div><script type="text/javascript">$(function(){refreshMenu("60-100",1512580197847);});</script></body></html>
```
という内容で、パケットフィルタのログが新しい順に並んでいるようです。HTMLの体裁は取っているけれども、1-2行目と最終行を除けばプレーンテキストとして扱えそうですね、かなり乱暴ですが。

そこで、以下のような要領のシェルスクリプトを`cron`を使って定期的に実行し、パケットフィルタログの更新された分だけを取り出して、`logger`に渡してやることにします。

1. 保存しておいたログスナップショットの先頭行(つまりもっとも新しいログエントリ)を取り出す
1. HGWからログを取得
1. 1.のログエントリが2.で取得したログの何行目にあるかを調べる(n行目とする)
1. 2.で取得したログのn-1行目までを出力する
1. 2.で取得したログをスナップショットとして保存する

できたシェルスクリプトが[RS-500KI-Logget](https://github.com/tagattie/RS-500KI-Logget)です。

これを`crontab`に登録して、定期的に実行させます。以下の例では、5分ごとに実行するようにしています。
```crontab
#minute hour mday month wday command
0/5     *    *    *     *    /<path>/<to>/<script>/RS-500KI-Logget/logget-rs500ki.sh
```
デフォルトのログファシリティ、レベルはそれぞれ`local3`および`info`としていますので、以下の一行を`/etc/syslog.conf`に追加すれば、指定したファイルにパケットフィルタログが保存されます。
```conf
local3.info     /var/log/hgw.log
```

### 参考文献
1. ホームゲートウェイ/ひかり電話ルータ (RS-500KI), http://web116.jp/shop/hikari_r/rs_500ki/rs_500ki_00.html
1. 不安定なRS-500KIのWiFiの高速化の顛末, http://strategics.hatenablog.com/entry/2016/04/18/141956
