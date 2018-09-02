+++
title = "FreeBSDでのChromium (Chrome)タブハングアップ問題が解決"
date = "2018-09-02T20:05:31+09:00"
draft = true
categories = ["FreeBSD"]
tags = ["freebsd", "chromium", "chrome", "browser", "hang", "stability"]
+++

FreeBSDでChromium (Chrome)をブラウザとしてお使いのかたに朗報です。長いこと悩まされ続けてきた「Chromiumのタブがしばしばハングアップする」という問題がついに解決しました!

Chromiumユーザであればおなじみの不具合だと思いますが、いちおうおさらいしておきましょう。発生していたおもな症状は以下の二つです。

- URLを開くときにローディング状態のまま進まない(既存タブ、新規タブいずれの場合でも発生)
- マウスやキーボードでの操作に反応しなくなる(例: スクロールの途中で反応しなくなる)

さらにひどくなるとブラウザ全体がフリーズしてしまい、操作にまったく反応しなくなることもありました。こうなると、タブのクローズや切り替えすらもできなくなり、コマンドラインから強制的に`kill`せざるを得なくなってしまいます。

[先日の記事](/post/freebsd-chromium-stability/)で、この問題を緩和する方法をいくつか紹介しましたが、根本的な解決にはいたらず、症状が発生する頻度をやや低くする程度の効果しかありませんでした。

このような状況だったわけですが、8/4付けでカーネルのソケットバッファまわりに以下の修正が入りました。(8/17付けで[stable/11ブランチにもマージ](https://svnweb.freebsd.org/base?view=revision&revision=337975)されています。)

- [Revision 337328 - Don't check rcv sockbuf limits when sending on a unix stream socket](https://svnweb.freebsd.org/base?view=revision&revision=337328)

Chromiumのタブがハングする問題に関するバグレポートスレッド(Bug 212812)で、上記の修正によってタブハング問題が解決されたとのコメント、あるいはこれに同意するコメントが見られます(Comment 96あたりから)。(ちなみにこのバグ、報告されたのが2016年9月なので、もうまる二年になる息の長い問題だったのですね。)

- [Bug 212812 - www/chromium: tabs "hang" 10% of the time](https://bugs.freebsd.org/bugzilla/show_bug.cgi?id=212812#c96)

一刻も早くこの恩恵にあずかりたいと思い、手もとのマシン(11.2-RELEASE-p2が動作)に対して上記の変更を適用してみました。すると、確かにはっきりとした効果があります。(変更を適用してから数日間Chromiumを使用していますが、まだタブがハングする現象にあっていません。)

ただし、上記の変更をそのまま11.2-RELEASE-p2に適用すると、一部パッチがうまく当たらない部分がありました。そこで、11.2-RELEASE-p2向けのパッチファイルを作成しました(本記事の末尾参照)。本ファイルを適当な場所に保存して以下の手順を実行すると恩恵にあずかることができます。

``` shell
svnlite checkout https://svn.freebsd.org/base/releng/11.2 /usr/src    # 手もとにソースがない場合のみ
cd /usr/src
patch -p0 < /path/to/patch/file
make buildkernel
make installkernel
reboot
```

### 参考文献
1. Revision 337328 - Don't check rcv sockbuf limits when sending on a unix stream socket, https://svnweb.freebsd.org/base?view=revision&revision=337328
1. Revision 337975 - MFC r337328, https://svnweb.freebsd.org/base?view=revision&revision=337975
1. Bug 212812 - www/chromium: tabs "hang" 10% of the time, https://bugs.freebsd.org/bugzilla/show_bug.cgi?id=212812

### 11.2-RELEASE-p2に対するパッチ
**注**: 手もとのマシンで問題なく動作することを確認していますが、パッチの使用は自己責任でおねがいします。
{{< gist tagattie f3835a4b3311c72e7e8fff72d4f3aa46 >}}
