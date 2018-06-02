+++
title = "Pwned-checkでパスワードが流出していないかチェックする"
date = "2018-06-02T21:28:00+09:00"
categories = ["FreeBSD"]
tags = ["freebsd", "pwned-check", "password", "leak", "leakage", "check", "chromium", "chrome", "passprotect", "firefox"]
+++

[前回の記事](/post/chromium-password-generator/)では、ChromeおよびFirefox向けのパスワード自動生成機能(アドオン)を紹介しました。ブラウザにランダムなパスワードを自動生成させ、かつブラウザに覚えさせることで、パスワードを自分で覚える(そして使い回す)必要をなくせます。

しかし、自動生成のパスワードを使っても安全とはいいきれません。可能性は小さいですが、そのパスワードが過去に流出したことのあるパスワードと**たまたま**一致するかもしれないのです。自動生成でなく自分で考えた良いパスワードも、もしかすると流出パスワードと一致するかもしれません。

[Troy Hunt氏](https://www.troyhunt.com/)が提供している[Have I Been Pwned: Pwned Passwords](https://haveibeenpwned.com/Passwords) (以下、HIBPと呼びます)では、自分のパスワードが過去に漏えいしたものの中に含まれていないかをチェックできます。(ちなみに、"Have I Been Pwned"は日本語に訳すと「やられちゃってない?」という感じでしょうか。)

- [Have I Been Pwned: Pwned Passwords (Troy Hunt)](https://haveibeenpwned.com/Passwords)

たとえば、`password`というパスワードで試してみると、(当然ですが)過去に漏えいした中に含まれていることがわかります。約330万件の漏えい事例に含まれていたようですね。(下図)

![Chromium - Pwned Passwords - password](/img/chromium/chromium-pwned-passwords-password.png)

非常にありがたいサービスなのですが、自分が実際に使っているパスワードを第三者のサイトに入力するのってちょっとためらわれませんか。そういう場合のために、HIBPでは流出した全パスワードのリストをダウンロードできるようにしてくれています。(約5億件、圧縮ファイルのサイズにして9GBにものぼります。)

さらに、FreeBSDには[pwned-check](https://www.freshports.org/security/pwned-check/)というパッケージがあり、自分のパスワードと流出パスワードのリストを**ローカル環境**で照合できます。以下、本記事ではpwned-checkのインストールと使い方について説明します。

FreeBSDには興味がないけど、自分のパスワードと流出パスワードをブラウザで簡単にチェックしたいというかたは[PassProtect](#passprotect)へどうぞ!

### Pwned-check
まず、パッケージをインストールします。

``` shell
sudo pkg install pwned-check
```

そのままでは、設定ファイル`/usr/local/etc/pwned-check.conf`の内容に一部不備がありますので、以下のパッチをあてます。(パッチをあてなくても動作には影響ありませんが、コマンド実行時にエラーメッセージが表示されます。)

``` diff
--- pwned-check.conf.orig	2018-06-01 15:09:47.578976000 +0900
+++ pwned-check.conf	2018-06-01 15:09:57.585061000 +0900
@@ -1,2 +1,2 @@
-DBDIR=		/var/db/pwned-check
-URLBASE=	https://downloads.pwnedpasswords.com/passwords
+DBDIR=/var/db/pwned-check
+URLBASE=https://downloads.pwnedpasswords.com/passwords
```

次は、HIBPから流出パスワードリストをダウンロードします。(以下の例では、`example`ユーザでコマンドを実行することを想定しています。本ユーザはご使用の環境にあわせて読み替えてください。)

``` shell
sudo chown -R example /var/db/pwned-check
pwned-check -u                               # ここはかなり時間がかかります
sudo chown -R root /var/db/pwned-check
```
以上でパスワードを照合する準備は完了です。

では、さっそくチェックしてみたいと思います。以下の実行例では、テスト用に4つのパスワード(`password`, `passwordneverleaked`, `dragon`, および`wyvern`)を含むテキストファイルを作成しました。このファイルを`pwned-check`コマンドの標準入力に与えます。すると、流出パスワードリストに含まれているものだけが表示されます。つまり、以下の例では`password`, `dragon`, および`wyvern`が過去に流出しているとわかります。

``` shell-session
$ cat passwordlist.txt
password
passwordneverleaked
dragon
wyvern
$ cat passwordlist.txt | pwned-check
password
dragon
wyvern
```

### PassProtect
こちらは、自分のパスワードと流出パスワードを自動的にチェックしてくれるChrome拡張機能です。(Firefox向けアドオンは開発中の模様。)

- [OktaのPassProtectはあなたのパスワードがどこにもリークしてないか調べてくれる (TechCrunch Japan)](https://jp.techcrunch.com/2018/05/24/2018-05-23-oktas-passprotect-checks-your-passwords-with-have-i-been-pwned/)
- [PassProtect (Okta)](https://www.passprotect.io/)

拡張機能をインストールするだけで使えます。いまのところ、特に設定などは必要ありません。

Webサイトにログインするとき(あるいはサインアップするとき)に、パスワードを入力してログイン(あるいはサインアップ)ボタンをクリックすると、HIBPのパスワードリストとの照合を行ない、入力パスワードが流出したものに含まれていれば、ダイアログをポップアップしてパスワード変更をうながしてくれます。(下図)

注: HIBPのWeb APIを用いて流出パスワードのチェックを行ないますので、自己責任での使用をお願いします。

![Chromium - PassProtect](/img/chromium/chromium-passprotect.png)

### 参考文献
1. Pwned Passwords, https://haveibeenpwned.com/Passwords
1. pwned-check - Check whether password is known to have been exposed in data breaches, https://www.freshports.org/security/pwned-check/
1. OktaのPassProtectはあなたのパスワードがどこにもリークしてないか調べてくれる, https://jp.techcrunch.com/2018/05/24/2018-05-23-oktas-passprotect-checks-your-passwords-with-have-i-been-pwned/
1. PassProtect, https://www.passprotect.io/
