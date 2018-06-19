+++
title = "Chrome (Chromium)の拡張機能をワンクリックでオン/オフする"
date = "2018-06-18T18:34:36+09:00"
draft = true
categories = ["WorldWideWeb"]
tags = ["chrome", "chromium", "browser", "extension", "1-click", "enable", "disable"]
+++

[先日の記事](/post/freebsd-pwned-check/)で、PassProtectというChrome拡張機能を紹介しました。本拡張は、パスワードの漏えいチェックをしてくれるものです。

拡張機能を紹介するときに、ふだん使っている拡張を一時的にオフにして、紹介するものだけを有効にしたいことがあります。特に、スクリーンショットを撮るときは、別の拡張がオンになっていると話題にしている拡張がどれなのかわかりにくいですし、そもそも画面がビジーに感じられます。

また、通常のブラウジングには使わないけれども、たとえば、開発しているWebアプリの動作テストを行なうときにだけ使いたい拡張機能もあるかもしれません。

本記事では、上記のようなニーズに応じて、Chrome (Chromium)の拡張機能のオン/オフをワンクリックで切り替えられる拡張機能を三つ紹介したいと思います。

### [Extensity](https://chrome.google.com/webstore/detail/extensity/jjmflmamggggndanpgfnpelongoepncg)
- [Chrome拡張機能のON／OFFを1クリックで管理：『Extensity』(lifehacker)](https://www.lifehacker.jp/2013/05/130518extensity.html)

本記事で紹介する拡張機能の中で、もっとも機能が充実しています。おおかたの場合は、これをインストールしておけば事足りそうです。拡張機能の個別オン/オフ、全拡張機能の一括オン/オフ(下図左、および右)に加えて、以下のような機能も提供されています。

- Chromeアプリを起動するためのショートカット
- 有効にする機能拡張のセットに名前をつけて、プロファイルとして保存可能

    通常用、開発用、などのブラウザの利用シーンに合わせて有効化する拡張機能群を指定可能
 
 欲をいえば、Extension Switchで提供されているような、ホワイトリスト機能(特定の拡張機能をオン/オフ操作の対象からはずし、常に有効に保つ)があるとさらに便利なのではないかと思います。

|図左|図右(全拡張オフ)|
|:---:|:---:|
|![Chromium - 拡張機能 - Extensity - オン](/img/chromium/chromium-extensity-on.png)|![Chromium - 拡張機能 - Extensity - オフ](/img/chromium/chromium-extensity-off.png)|

### [Extension Switch](https://chrome.google.com/webstore/detail/extension-switch/gnphfcibcphlpedmaccolafjonmckcdn)

- [Chrome Extension: Extension Switch (gotohayato)](https://gotohayato.com/work/chrome-extension-switch)

ひとことでいうと、Extensityから全拡張機能の一括オン/オフ機能を取り除いたような感じです。(下図)

ふだんは使わないけれども、特定のシーンでだけ有効にしたい拡張機能がある場合に役立ちます。いつも必ず使う拡張機能は、オン/オフの対象からはずすホワイトリストオプションも備わっています。

![Chromium - 拡張機能 - Extension Switch](/img/chromium/chromium-extension-switch.png)

### [Disable Extensions Temporarily](https://chrome.google.com/webstore/detail/disable-extensions-tempor/lcfdefmogcogicollfebhgjiiakbjdje)

- [Disable Extensions Temporarily Chrome拡張 すべての拡張機能をワンクリックで無効化できる (Chrome拡張のいいところ)](https://webiitoko.blog.fc2.com/blog-entry-862.html)

本記事で紹介する拡張機能の中で、もっともシンプルなものです。本拡張をインストールすると、ツールバーに電源ボタンのようなアイコンが現れます。本アイコンをクリックすると、全拡張機能の一括オン/オフを行なえます。(下図左、および右)

|図左(全拡張オン)|図右(全拡張オフ)|
|:---:|:---:|
|![Chromium - 拡張機能 - Disable Extensions Temporarily - オン](/img/chromium/chromium-disable-extensions-temporarily-on.png)|![Chromium - 拡張機能 - Disable Extensions Temporarily - オフ](/img/chromium/chromium-disable-extensions-temporarily-off.png)|

最後に、紹介した拡張機能の提供する機能を簡単にまとめておきます。

||全拡張の一括オン/オフ|拡張の個別オン/オフ|その他機能
|:---|:---:|:---:|:---|
|Extensity|○|○|アプリショートカットあり<br>プロファイル設定可能
|Extension Switch|×|○|オン/オフの対象外にする拡張を指定可能
|Disable Extensions Temporarily|○|×||

### 参考文献
1. Extensity, https://chrome.google.com/webstore/detail/extensity/jjmflmamggggndanpgfnpelongoepncg
1. Chrome拡張機能のON／OFFを1クリックで管理：『Extensity』, https://www.lifehacker.jp/2013/05/130518extensity.html
1. Extension Switch, https://chrome.google.com/webstore/detail/extension-switch/gnphfcibcphlpedmaccolafjonmckcdn
1. Chrome Extension: Extension Switch, https://gotohayato.com/work/chrome-extension-switch
1. Disable Extensions Temporarily, https://chrome.google.com/webstore/detail/disable-extensions-tempor/lcfdefmogcogicollfebhgjiiakbjdje
1. Disable Extensions Temporarily Chrome拡張 すべての拡張機能をワンクリックで無効化できる, https://webiitoko.blog.fc2.com/blog-entry-862.html
