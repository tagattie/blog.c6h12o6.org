+++
title = "FreeBSDに二要素認証(2FA)をセットアップする - Duo Security編"
date = "2018-02-10T14:53:00+09:00"
categories = ["FreeBSD"]
tags = ["freebsd", "login", "ssh", "2fa", "mfa", "duo"]
+++

みなさんは、お使いのネットサービスのアカウントセキュリティに関して気をつけていらっしゃるでしょうか。セキュリティの要はもちろんパスワードですが、さらに守りを強化するための手段として、多要素認証(Multi-Factor Authentication, MFA)があります。特に、MFAのうち、パスワードの他にもう一つの手段を組み合わせるものを、二要素認証(Two-Factor Authentication, 2FA)と呼びます。

「もう一つの手段」には、指定した携帯電話にSMSで送信されるワンタイムパスワードを使ったり、認証アプリを用いて生成したワンタイムパスワードを使ったりします。認証アプリの例としては、Google Authenticator (Google認証システム)やMicrosoft Authenticatorが挙げられます。メジャーなネットサービスでは2FAに対応しているところも多いので、すでに利用している方もいらっしゃることと思います。

ちなみに、2FAに対応しているサービスは[Two Factor Auth (2FA)](https://twofactorauth.org/)で検索できます。

本記事では、FreeBSDが動作するサーバに対してコンソールログイン、およびSSH経由でのリモートログインを行なう際に、2FAを用いた認証を実現する方法を説明します。2FAを実現するための仕組みとしてはGoogle認証システムを用いるのがメジャーなようで、[FreeBSDへのSSHログインに対してGoogle認証システムを用いた2FAを実現する方法](http://april.fool.jp/blogs/2013/09/freebsd%E8%87%AA%E5%89%8D%E3%82%B5%E3%83%BC%E3%83%90%E3%81%AEssh%E3%83%AD%E3%82%B0%E3%82%A4%E3%83%B3%E3%81%ABgoogle%E3%81%AE2%E6%AE%B5%E9%9A%8E%E8%AA%8D%E8%A8%BC%E3%81%8C%E4%BD%BF%E3%81%88%E3%81%A6/)が、すでに紹介されています。

いっぽう、ここではGoogle認証システムの代わりに[Duo Security社](https://duo.com/)が提供する2FAサービスを用いた実現方法を紹介します。Google認証システムを使うかわりにDuo Security社のサービスを使おうと思った理由としては、

- 設定が簡単そう
- 認証のログが確認できる(↓のような感じ)

    [![認証ログイメージ](/img/duo-security-auth-log-thumbnail.png)](/img/duo-security-auth-log.png)

の2点があります。Duo Security社はエンタープライズ向けのサービスを主としているようで、有料サービスを選択すれば色々な機能が使えるようですが、個人的に利用するならば無料サービスでも十分そうです。

以下、[Configuring two-factor authentication on FreeBSD with Duo Push](https://www.teachnix.com/configuring-two-factor-authentication-on-freebsd-with-duo/)を参考にしながら、2FAの設定を行なっていきます。

1. Duoアカウントの取得

    まずは、Duo Security社の[Webサイト](https://duo.com/)にアクセスして、アカウントを取得します。トップページの右上にあるStart Trialをクリックしてください。"Trial"という言葉から、一定期間後に有料化されそうな印象を受けるかもしれませんが、支払い情報の入力は求められませんので安心してください。(30日間のDuo Access ($6/ユーザ/月)の無料体験となるようです。期間終了後は、Duo Freeになる模様。)

1. Duo Mobileアプリのダウンロード

    以下のいずれかのリンクから、スマホ向けのDuo Mobileアプリをダウンロード、インストールします。
    
    - [Google Play](https://play.google.com/store/apps/details?id=com.duosecurity.duomobile)
    - [App Store](https://itunes.apple.com/jp/app/duo-mobile/id422663827)
    - [Microsoft Store](https://www.microsoft.com/ja-jp/store/p/duo-mobile/9nblggh08m1g)

    そして、[Admin Panel](https://admin.duosecurity.com/)へログインして、まずはDuoアカウント自体の2FA設定を完了してください。

1. 保護対象アプリケーションの追加

    次に、Admin Panelの左側にあるApplicationsをクリックします。するとその下に、Protect an Applicationという項目が現れますので、ここをクリックします。するとアプリケーションの一覧が表示されますので、unixを検索し、UNIX Applicationの右側のProtect this Applicationをクリックします。
    [![UNIX Applicationの追加](/img/duo-security-select-unix-app-thumbnail.png)](/img/duo-security-select-unix-app.png)
    すると、ログインの対象となるサーバ側の設定に必要な情報が表示されます。
    [![UNIX Applicationの保護に必要な情報](/img/duo-security-unix-app-keys-thumbnail.png)](/img/duo-security-unix-app-keys.png)
    この情報を、次回[サーバ設定編](/post/freebsd-login-ssh-server/)で使用します。

### 参考文献
1. Two Factor Auth (2FA), https://twofactorauth.org/
1. 自前サーバのSSHログインにgoogleの2段階認証が使えて秋の夜長に感動した(追記あり)。, http://april.fool.jp/blogs/2013/09/freebsd%E8%87%AA%E5%89%8D%E3%82%B5%E3%83%BC%E3%83%90%E3%81%AEssh%E3%83%AD%E3%82%B0%E3%82%A4%E3%83%B3%E3%81%ABgoogle%E3%81%AE2%E6%AE%B5%E9%9A%8E%E8%AA%8D%E8%A8%BC%E3%81%8C%E4%BD%BF%E3%81%88%E3%81%A6/
1. Duo Security, Inc., https://duo.com/
1. Configuring two-factor authentication on FreeBSD with Duo Push, https://www.teachnix.com/configuring-two-factor-authentication-on-freebsd-with-duo/
