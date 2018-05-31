+++
title = "Chrome (Chromium)のパスワード生成機能を使う(Firefoxならパスワード生成アドオンを)"
date = "2018-05-30T16:12:01+09:00"
draft = true
categories = ["WorldWideWeb"]
tags = ["chromium", "chrome", "password", "generator", "firefox", "secure"]
+++

オンラインアカウント、いくつくらい持っていますか?

SNSや通販サイトに加え、スマートフォンを持っていればGoogleやiCloudのアカウントなど、片手どころか両手両足使っても数えきれないのではないでしょうか。

アカウントを守るためのパスワード、使い回していませんか?

[Trend Micro社の調査(2017年)](https://www.trendmicro.com/ja_jp/about/press-release/2017/pr-20171005-01.html)によると、パスワードを使い回しているというかたが8割以上にものぼるそうです。たしかに、アカウントごとのパスワードを考えるのがまず面倒です。それに、数が多くなるととても覚えていられません。だから、少数のパスワードを使いまわしたくなる気持ちはよくわかります。

- [－パスワードの利用実態調査 2017－ パスワードを使いまわしている利用者が8割以上 (Trend Micro)](https://www.trendmicro.com/ja_jp/about/press-release/2017/pr-20171005-01.html)

しかし、パスワードの使い回しがなぜいけないのでしょうか? あなたが使っているサービスの一つから、IDとパスワードが流出したとしましょう。同じパスワードを使っていたら、流出していない他のサービスにも簡単に(不正)アクセスできてしまいますね。(下記記事の冒頭のイラストがわかりやすいです。)

- [パスワード使い回しがダメな理由〜パスワードリスト攻撃って？ (せぐなべ)](https://www.segunabe.com/2017/11/17/passclipnews171117/)

では、どうすればいいのでしょうか?

解決策の一つはパスワードマネージャーを使う、つまり、自分で覚えずにマシン(PC)に覚えさせることです。ついでに、パスワードを考えるのもやめましょう。わたしは最近まで知らなかったのですが、Chrome (Chromium)にはパスワードを自動生成する機能が備わっています。

- [複数のデバイスでパスワードを同期する (Google)](https://support.google.com/accounts/answer/6197437)
- [パスワードを生成する (Google)](https://support.google.com/chrome/answer/7570435)

使い方はいたって簡単。まず、パスワードを入力する欄でマウスを右クリックします。すると、「パスワードを生成…」というメニューが一番上に出ますので、これを選択します。

![Chromium - メニュー - パスワードを生成](/img/chromium/chromium-menu-generate-password.png)

パスワードが自動的に生成、表示されます。

![Chromium - メニュー - 生成されたパスワード](/img/chromium/chromium-menu-generated-password.png)

表示されたパスワードをクリックすると、パスワードの欄にこれが入力されます。パスワードの欄だけでなく、確認用の再入力欄にも自動的に入力してくれるので気がきいていますね。また、生成されたパスワードは自動的にブラウザに記憶されますので、自分で覚えておく必要はありません。

![Chromium - メニュー - パスワードを入力](/img/chromium/chromium-menu-input-password.png)

「英字、数字、記号を必ず含める」とか「10文字にする」などの細かい調節はできないようですが、デフォルトで使える手軽さがうれしいところではないでしょうか。

Chromeだけなのですか? Firefoxは?

いまのところ、Firefoxにはパスワードを記憶する機能はあるものの、パスワードを自動生成する機能はないようです。といってもあきらめる必要はありません。Firefoxの場合は、パスワードを自動生成するアドオンを使いましょう。たとえば、[Secure Password Generator](https://addons.mozilla.org/firefox/addon/secure-password-generator/)があります。

- [Secure Password Generator (Firefox Add-ons)](https://addons.mozilla.org/firefox/addon/secure-password-generator/)

本アドオンの場合は、生成するパスワードについてさまざまな設定を行なうことができます。

![Firefox - アドオン - Secure Password Generator - 設定](/img/firefox/firefox-secure-password-generator-settings.png)

使い方はこちらも簡単で、Chromeのパスワード生成機能とほぼ同じです。まず、パスワードを入力する欄でマウスを右クリックします。その後、メニューから"Secure Password Generator"→"Generate Password"を選択します。

![Firefox - アドオン - Secure Password Generator - 生成](/img/firefox/firefox-secure-password-generator-generate.png)

すると、パスワード欄に自動生成したパスワードが入力されます。確認用の再入力欄にパスワードを入力する場合は、再入力欄で右クリックし"Secure Password Generator"→"Insert Previous Password"を選択してください。

![Firefox - アドオン - Secure Password Generator - 入力](/img/firefox/firefox-secure-password-generator-input.png)

面倒なことはマシンにまかせて、安全に楽しくネットサービスを利用しましょう!

### 参考文献
1. －パスワードの利用実態調査 2017－ パスワードを使いまわしている利用者が8割以上, https://www.trendmicro.com/ja_jp/about/press-release/2017/pr-20171005-01.html
1. パスワード使い回しがダメな理由〜パスワードリスト攻撃って？, https://www.segunabe.com/2017/11/17/passclipnews171117/
1. 複数のデバイスでパスワードを同期する, https://support.google.com/accounts/answer/6197437
1. パスワードを生成する, https://support.google.com/chrome/answer/7570435
1. Secure Password Generator, https://addons.mozilla.org/firefox/addon/secure-password-generator/
