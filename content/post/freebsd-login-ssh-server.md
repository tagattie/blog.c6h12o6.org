+++
title = "FreeBSDに二要素認証(2FA)をセットアップする - サーバ編"
date = "2018-02-10T16:21:00+09:00"
draft = true
categories = ["FreeBSD"]
tags = ["freebsd", "login", "ssh", "2fa", "mfa", "duo"]
+++

[FreeBSDに二要素認証(2FA)をセットアップする - Duo Security編](/post/freebsd-login-ssh-duo/)では、Duo Securityのアカウントを取得し、保護対象のサーバに設定する情報を確認するところまでを説明しました。本サーバ編では、保護対象のサーバ上で行なう設定について説明します。

引き続き、[Configuring two-factor authentication on FreeBSD with Duo Push](https://www.teachnix.com/configuring-two-factor-authentication-on-freebsd-with-duo/)を参考に設定を進めていきます。

1. Duoパッケージのインストールと設定

    まず、duoパッケージをインストールします。
    ```shell
    pkg install duo
    ```
    すると、`/usr/local/etc`以下に、
	- `login_duo.conf`, `login_duo.conf.sample`
	- `pam_duo.conf`, `pam_duo.conf.sample`

    というファイルが作られます。`.conf`ファイルの方に必要な設定を記入します。どちらとも同じ内容になります。
    
    - `login_duo.conf`および`pam_duo.conf`
    
        ```conf
	[duo]
	; Duo integration key
	ikey = <Duo Securityサイトの情報のうち Integration key をコピペ>
	; Duo secret key
	skey = <Duo Securityサイトの情報のうち Secret key をコピペ>
	; Duo API host
	host = <Duo Securityサイトの情報のうち API hostname をコピペ>
	; Send command for Duo Push authentication
	pushinfo = yes
	```
	
    以上で、Duoパッケージの設定は完了です。

1. PAM (Pluggable Authentication Module)の設定

    次に、PAMの設定を変更します。`/etc/pam.d`以下にあるファイルを編集して、以下の内容にします。`system`はコンソールログインに2FAを適用するため、`sshd`はSSHによるリモートログインに2FAを適用するための変更です。
    - `/etc/pam/system` (`auth`の部分だけ抜き出しています)
    
        ```conf
	auth            sufficient      pam_opie.so             no_warn no_fake_prompts
	auth            requisite       pam_opieaccess.so       no_warn allow_local
	#auth           sufficient      pam_krb5.so             no_warn try_first_pass
	#auth           sufficient      pam_ssh.so              no_warn try_first_pass
	auth            required        pam_unix.so             no_warn try_first_pass nullok
	auth            required        /usr/local/lib/security/pam_duo.so                      # この行を追加
	```
    
    - `/etc/pam/sshd` (`auth`の部分だけ抜き出しています)
    
        ```conf
	auth            sufficient      pam_opie.so             no_warn no_fake_prompts
	auth            requisite       pam_opieaccess.so       no_warn allow_local
	#auth           sufficient      pam_krb5.so             no_warn try_first_pass
	#auth           sufficient      pam_ssh.so              no_warn try_first_pass
	#auth           required        pam_unix.so             no_warn try_first_pass   # この行をコメントアウト
	auth            required        /usr/local/lib/security/pam_duo.so               # この行を追加
	```

    以上で、PAMの設定変更は完了です。

1. sshdの設定

    最後に、sshdの設定を変更します。`/etc/ssh/sshd_config`を編集します。最低限、以下の設定を行なってください。(なお、公開鍵認証によるログインの設定は済んでいることを想定しています。)
    
    - `/etc/ssh/sshd_config`
    
        PAMを使用しますので`UsePAM`を`yes`にします。
        
	    また、公開鍵認証に加えてワンタイムパスワードを用いますので、`ChallengeResponseAuthentication`を`yes`に設定し、`AuthenticationMethods`に`keyboard-interactive`を加えます。
        ```conf
	PasswordAuthentication no
	ChallengeResponseAuthentication yes
	UsePAM yes
	AuthenticationMethods publickey,keyboard-interactive
	```

    sshdを再起動して、サーバ側での準備は完了です。
    ```shell
    service sshd restart
    ```

### 参考文献
1. Configuring two-factor authentication on FreeBSD with Duo Push, https://www.teachnix.com/configuring-two-factor-authentication-on-freebsd-with-duo/
