+++
title = "FreeBSDに二要素認証(2FA)をセットアップする - ログイン編"
date = "2018-02-10T16:33:18+09:00"
draft = true
categories = ["FreeBSD"]
tags = ["freebsd", "login", "ssh", "2fa", "mfa", "duo"]
+++

[FreeBSDに二要素認証(2FA)をセットアップする - サーバ編](/post/freebsd-login-ssh-server/)では、2FAを実現するためのサーバ側での設定を説明しました。本ログイン編では、初回のSSHログイン時、あるいは初回のコンソールログイン時に必要な手続きについて説明します。

引き続き、[Configuring two-factor authentication on FreeBSD with Duo Push](https://www.teachnix.com/configuring-two-factor-authentication-on-freebsd-with-duo/)を参考に設定を進めていきます。

1. SSHクライアントからの初回アクセス

    サーバ側の設定が終わったら、次はSSHクライアントを用いてサーバにアクセスを行ないます。初回は認証が失敗し、下図に示すように、ユーザの追加が必要である旨のメッセージとともにURLが表示されますので、Webブラウザを用いて表示されたURLにアクセスします。
    ![SSHクライアントからの初回アクセス](/img/ssh-duo-first-login-attempt.png)
    注: Windowsの場合、SSHクライアントには[Putty](https://www.putty.org/)あるいは[RLogin](http://nanno.dip.jp/softlib/man/rlogin/)の使用をおすすめします。TeraTermは2FAに**対応していない**ので、ご注意ください。

1. ユーザアカウントの追加

    表示されたURLにWebブラウザでアクセスすると、以下のような画面が表示されます。
    ![Webブラウザを用いたユーザ追加](/img/ssh-duo-enrollment-1.png)
    SSHログインに使うアカウント名が表示されていますので、これを確認したうえで、"Start setup"をクリックします。あとは、画面の指示にしたがって、認証に使うデバイスの種別、電話番号、OS、…を入力していきます。最後に、ログインの都度認証方法を選択するか、自動的に認証デバイス(スマホ)にプッシュ通知を送信するか、好きな方を選択して"Finish Enrollment"をクリック(下図)すれば、アカウントの追加が完了です。
    ![Webブラウザを用いたユーザ追加](/img/ssh-duo-enrollment-2.png)
    注1: 「自動的にプッシュ通知を送信」を選択すると、ログインの際に、認証デバイスにてログインを承認するか否かを選択するプッシュ通知が表示されます。"TAP TO VIEW ACTIONS"をタップすると、"APPROVE"と"DENY"が表示されます。ここで、"APPROVE"(承認)をタップするとログインが完了しますので、認証アプリを開いてワンタイムパスワードを確認し、入力する手間が省けます。
        
    注2: わたしの場合、「プッシュ通知を送信」を選択しましたが、毎回認証手段を選択(Push or SMS)するよう促されるので、いまのところこの選択はうまく効いていないようです。

1. 再度SSHクライアントからアクセス

    再度、サーバにアクセスすると、今度は下図に示すように、認証手段を選択するよう促すメッセージが表示されます。
    ![SSHクライアントからの二回目以降アクセス](/img/ssh-duo-second-login-attempt.png)
    1を入力してエンターキーを押すと、認証デバイスにプッシュ通知が表示されますので、デバイス上で承認すればログインが完了します。あるいは、Duo Mobileアプリを開いて、該当アカウントのワンタイムパスワードを確認し、入力することでもログインが完了します。
    
    注: SMSでワンタイムパスワードを受信することも可能なようですが、できる限り**SMSの使用は控える**ことをおすすめします。その理由は、SMSについては「クレジット」が消費されてしまうためです(送信先が日本の場合は10クレジット)。(アカウント作成時に自動的に500クレジットが付与されています。) 万一のときのために、クレジットはできるだけ残しておきましょう。残クレジットについては、[Admin Panel](https://admin.duosecurity.com/)のDashboardで確認できます。もちろん、クレジットを購入することもできます([ここ](https://duo.com/docs/telephony_credits)によれば、1,000クレジットで$10の模様)。

以上、SSHを用いたリモートログインについて説明してきましたが、コンソールでのログインについてもほぼ同様です。コンソールの場合は、アカウントのパスワードを入力した後にDuo Mobileを用いた認証手続きを行ないます(下図)。
![コンソールからのログイン](/img/console-duo-second-login-attempt.png)

### 参考文献
1. Configuring two-factor authentication on FreeBSD with Duo Push, https://www.teachnix.com/configuring-two-factor-authentication-on-freebsd-with-duo/
1. Telephony Credits, https://duo.com/docs/telephony_credits
