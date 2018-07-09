+++
title = "Chrome OS (Chromium OS)のシステム情報/リソース状況を確認する"
date = "2018-07-09T21:07:00+09:00"
categories = ["WorldWideWeb"]
tags = ["chrome", "chromium", "chromeos", "chromiumos", "chromebook", "system", "information"]
+++

セルフビルドした[Chromium OS](https://www.chromium.org/chromium-os)をNEC LaVie Zで使っています。[トラブルにあったり](/post/chromiumos-self-build-local-chromium/)もしますが、Chromium OS、とても気に入っています。

ところで、たびたびあるわけではありませんが、使っているPCのシステム情報を確認したいことってありますよね。ここで、システム情報とは、PCに搭載されているCPUやメモリなどのハードウェアスペック、あるいは現在のCPU負荷や使用メモリ量、ネットワークアドレスなどのリソース情報のことをいいます。

Windowsであれば、その名のとおり「システム情報」アプリを使って確認できます。

- [Windows 10でシステム情報を確認する方法 (NEC LAVIE公式サイト)](https://121ware.com/qasearch/1007/app/servlet/relatedqa?QID=018426)

または、[HWiNFO](https://www.hwinfo.com/)や[Speccy](https://www.ccleaner.com/speccy)などのサードパーティソフトウェアを使うという方法もありますね。

しかし、Chromium OSには、いまのところシステム情報を確認するプログラムは組み込まれていないようです。とはいえ、Chromium OSでもシステム情報を確認したいというニーズはあります。そこで、本記事ではChromium OSのシステム情報を確認できるChromeアプリを二つ紹介します。

- [System (Chrome Web Store)](https://chrome.google.com/webstore/detail/system/ocjnemjmlhjkeilmaidemofakmpclcbi)
- [Cog - System Info Viewer (Chrome Web Store)](https://chrome.google.com/webstore/detail/cog-system-info-viewer/difcjdggkffcfgcfconafogflmmaadco)

### [System](https://chrome.google.com/webstore/detail/system/ocjnemjmlhjkeilmaidemofakmpclcbi)
Systemは以下に挙げる六つのタブに分けて、システム情報を表示してくれます。

- 基本情報(CPU、メモリ、OSなど)
- ストレージ
- ディスプレイ
- ネットワーク
- メディアギャラリー
- 現在位置

[![Chrome - アプリ - System](/img/chromium/chromium-system-small.png)](/img/chromium/chromium-system.png)

以下の記事でも紹介されていますので、あわせて参考にしていただければと思います。

- [System はChromebookのスペックが確認できるChromeアプリ。 (サイゴンのうさぎ)](http://usagisaigon.blogspot.com/2014/07/system-chromebookchrome.html)

### [Cog - System Info Viewer](https://chrome.google.com/webstore/detail/cog-system-info-viewer/difcjdggkffcfgcfconafogflmmaadco)
Systemはシステム情報を複数のタブに分けて表示するようになっていました。いっぽう、Cogはすべての情報を一つのウィンドウで一覧するようになっています。Systemと比較しての違いは以下のようになります。

- バッテリ情報の表示ができる
- CPU負荷および温度の表示ができる
- メディアギャラリーと現在位置の表示はできない

[![Chrome - アプリ - Cog](/img/chromium/chromium-cog-small.png)](/img/chromium/chromium-cog.png)

見た目がすっきりしているので、個人的にはこちらが好みです。

いずれにしても機能はほぼ同等ですので、上記の機能差異、あるいは見た目の好みで選択すればよさそうです。

### 参考文献
1. Windows 10でシステム情報を確認する方法, https://121ware.com/qasearch/1007/app/servlet/relatedqa?QID=018426
1. HWiNFO, https://www.hwinfo.com/
1. Speccy, https://www.ccleaner.com/speccy
1. System, https://chrome.google.com/webstore/detail/system/ocjnemjmlhjkeilmaidemofakmpclcbi
1. Cog - System Info Viewer, https://chrome.google.com/webstore/detail/cog-system-info-viewer/difcjdggkffcfgcfconafogflmmaadco
1. System はChromebookのスペックが確認できるChromeアプリ。, http://usagisaigon.blogspot.com/2014/07/system-chromebookchrome.html
