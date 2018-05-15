+++
title = "Cookie AutoDeleteを使って不要なHTTP Cookieを自動削除する"
date = "2018-05-15T16:37:59+09:00"
draft = true
categories = ["WorldWideWeb"]
tags = ["web", "browser", "http", "cookie", "manage", "delete", "chromium", "chrome", "firefox", "autodelete"]
+++

Webサイトがブラウザとの間でのセッション管理を行なったり、サイトを訪れるユーザをトラッキングするための技術である[HTTP Cookie](http://www.atmarkit.co.jp/ait/articles/1704/20/news024.html)ですが、いまでは使われていないサイトはないといっていいくらいではないでしょうか。

わたしはメインブラウザにChromium (Chrome)、サブブラウザとしてFirefoxを使用していますが、必要以上のトラッキングは遠慮したいです。そこで、いずれのブラウザでもクッキー管理拡張機能(アドオン)を用いて、指定したクッキー以外の不要なものはブラウザの終了時に削除しています。

使っている機能拡張(アドオン)は以下の二つです。

- [Vanilla Cookie Manager (Chrome拡張機能)](https://chrome.google.com/webstore/detail/vanilla-cookie-manager/gieohaicffldbmiilohhggbidhephnjj)
- [CookieKeeper (Firefoxアドオン)](https://addons.mozilla.org/firefox/addon/cookiekeeper/)

いえ、CookieKeeperについては、「使っていた」といういいかたが正しいですね。本アドオンは[WebExtensions](https://developer.mozilla.org/Add-ons/WebExtensions)に対応していませんので、Firefox Quantumでは使えなくなってしまいました。

Vanilla Cookie Managerには満足しておりなんの問題もないのですが、Firefoxのほうをどうしようかと思っていた矢先に、以下の記事(一つめ)を見つけました。Cookie AutoDeleteというChrome向けのクッキー管理拡張機能の紹介記事です。Web検索してみると、Firefox向けのアドオンもあることがわかりました。(二つめの記事)

- [Cookie AutoDelete is my new favorite Chrome extension (Android Central)](https://www.androidcentral.com/cookie-autodelete-my-new-favorite-chrome-extension)
- [CookieKeeperに代わるアドオンCookie AutoDelete (有馬総一郎のブログ(彼氏の事情))](https://arimasou16.com/blog/2017/11/08/00241/)

そこで、この機会にクッキー管理機能を統一しようと、乗り換えてみることにしました。

- [Cookie AutoDelete (Chrome拡張機能)](https://chrome.google.com/webstore/detail/cookie-autodelete/fhcgjolkccmbidfldomjliifgaodjagh)
- [Cookie AutoDelete (Firefoxアドオン)](https://addons.mozilla.org/firefox/addon/cookie-autodelete/)

注: Chromeには、"Cookie Auto Delete" (AutoとDeleteの間に空白)という拡張機能もあるようですが、これは別ものですのでご注意ください。

使い方は説明の必要がないくらい簡単です。拡張機能(アドオン)をインストールすると、ブラウザのツールバーにCookie AutoDeleteのアイコンが追加されます。インストールしただけでは機能がオフになっていますので、アイコンをクリックして機能をオンにしましょう(下図参照)。すると、タブを閉じてから所定の時間が経過した後に、そのタブで開いていたサイトに関するクッキーが自動的に削除されます。

特定サイトのクッキーを削除せずに保持しておきたい場合はどうすれば?

クッキーを保持しておきたいサイトを開いているときに、拡張機能(アドオン)のアイコンをクリックしてください。すると、下図の画面が表示されます。

![Chromium - Cookie AutoDelete - メイン](/img/chromium/cookie-autodelete-main.png)

ドメイン名の完全一致(`google.com`)あるいはワイルドカード(`*.google.com`)のどちらかに決めて、グレイリスト(Greylist)あるいはホワイトリスト(Whitelist)に追加(+)すればOKです。グレイリストとホワイトリストの違いは以下のとおりです。

- グレイリスト - **ブラウザ終了時まで**クッキーを保持する(ブラウザ再起動時に削除)
- ホワイトリスト - **クッキーの有効期限まで**クッキーを保持する(ブラウザを再起動しても削除しない)

また、画面上部には機能全体のオン/オフスイッチ、クッキー削除時の通知をオン/オフするスイッチ、手動でのクッキー削除ボタン、設定ボタンがあります。

設定ボタンを押すと以下の画面が表示されます。タブを閉じてからクッキーを削除するまでの待ち時間などを設定することができます。

![Chromium - Cookie AutoDelete - 設定](/img/chromium/cookie-autodelete-settings.png)

ChromiumとFirefoxでクッキー管理を統一できて満足、満足。みなさまもぜひお試しあれ。

### 参考文献
1. HTTP State Management Mechanism, https://tools.ietf.org/html/rfc6265
1. 超入門HTTP Cookie, http://www.atmarkit.co.jp/ait/articles/1704/20/news024.html
1. Vanilla Cookie Manager, https://chrome.google.com/webstore/detail/vanilla-cookie-manager/gieohaicffldbmiilohhggbidhephnjj
1. CookieKeeper, https://addons.mozilla.org/firefox/addon/cookiekeeper/
1. Cookie AutoDelete is my new favorite Chrome extension, https://www.androidcentral.com/cookie-autodelete-my-new-favorite-chrome-extension
1. CookieKeeperに代わるアドオンCookie AutoDelete, https://arimasou16.com/blog/2017/11/08/00241/
1. Cookie AutoDelete, https://chrome.google.com/webstore/detail/cookie-autodelete/fhcgjolkccmbidfldomjliifgaodjagh
1. Cookie AutoDelete, https://addons.mozilla.org/firefox/addon/cookie-autodelete/
<!--stackedit_data:
eyJoaXN0b3J5IjpbNTcyNTUzNjI5LDE5NjE1MjA4MzMsLTYzND
Y0ODkyLC0xODgyMDYyODI3LDU0NDg0NDI2NywtMTY0MzMyNzY0
NywxMjEwNzc2NTg4LC0yNTY2NTA2MzQsOTc4Mjk0NjY3LC05Mz
M5Mzk5ODIsLTEwMDcwMTMzODMsMjA1Njg5NDU4NCwtMjA2MjM3
MzMyMyw1MjE0NzIwODcsLTEzMzU2MjYwOSwxNjM5NzUzMDAsLT
cwNTI3NjgwNCwxMTg3ODA1NjQ1LDEwNjMwNDAxODYsLTkwNzcx
NzAxNF19
-->
