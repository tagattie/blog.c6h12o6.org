+++
title = "FreeBSDのPAMの設定を理解する"
date = "2018-02-15T13:23:41+09:00"
draft = true
categories = ["FreeBSD"]
tags = ["freebsd", "pam"]
+++

[FreeBSDに二要素認証を導入する過程](/post/freebsd-login-ssh-server/)で、[Duo Security](https://duo.com/)社の認証モジュールをPAM (Pluggable Authentication Module)経由で使うための設定について述べました。基本的には、すでに存在しているPAMの設定ファイル`sshd`, `system`に一行書き加えるだけでOKでした。しかし、正直に言えば、なぜこれでやりたいことができるのかについて十分に理解していませんでした。

そこで、FreeBSDにおけるPAMの設定(特にauthenticationの設定)についてもう少し調べてみました。本記事では、その結果について報告します。

FreeBSDの`pam.conf(5)`によると、設定ファイルの書式は以下のようになります。

```conf
facility control-flag module-path [arguments ...]
facility include other-service-name
```

すなわち、`facility`(機能)、`control-flag`(制御フラグ)、および`module-path`(認証モジュールのパス)を一組で指定し、これを1つ以上記述するようになっています。同一機能について、複数の行が指定されている場合は、上にあるものから順に実行されます。また、別ファイルの設定内容を`include`を用いて読み込ませることも可能です。

facilityはPAMが取り扱う機能を指定するもので、`auth`(認証機能), `account`(アカウント管理機能), `session`(セッション処理機能), および`password`(パスワード管理機能)が指定できます。

次のcontrol-flagがPAMの設定の要点といえるもので、module-pathで指定されたモジュールの認証結果を、最終的な認証の成否にどのように結び付けるかを制御する項目です。control-flagには、`required`, `requisite`, `sufficient`, `binding`, および`optional`のいずれかが指定でき、それぞれの意味は以下のようになります。

- required: モジュールが認証成功のとき、後続のモジュールが認証失敗しない限り、最終的に認証成功となる。いっぽう、モジュールが認証失敗のとき、後続のモジュールが実行されるが、その成否に関わらず最終的に認証失敗となる。
- requisite: モジュールが認証成功のとき、後続のモジュールが認証失敗しない限り、最終的に認証成功となる。いっぽう、モジュールが認証失敗のとき、後続のモジュールは実行されず、即時認証失敗となる。
- sufficient: モジュールが認証成功のとき、後続のモジュールは実行されず、即時認証成功となる。いっぽう、モジュールが認証失敗のとき、後続のモジュールが実行され、後続のモジュールが認証成功となったら、最終的に認証成功となる。
- binding: モジュールが認証成功のとき、後続のモジュールは実行されず、即時認証成功となる。いっぽう、モジュールが認証失敗のとき、後続のモジュールが実行されるが、その成否に関わらず最終的に認証失敗となる。
- optional: モジュールが認証成功のとき、後続のモジュールが認証失敗しない限り、最終的に認証成功となる。いっぽう、モジュールが認証失敗のとき、後続のモジュールが実行され、後続のモジュールが認証成功となったら、最終的に認証成功となる。

`pam.conf(5)`の説明を日本語にして書き下してみましたが、言葉での説明ではややこしくてよくわかりませんね。[FreeBSDのドキュメント](https://www.freebsd.org/doc/en_US.ISO8859-1/articles/pam/pam-config.html)がこれらの内容をすっきりと表にまとめていますので、ここに再掲します。

|control-flag|PAM_SUCCESS (成功)|PAM_IGNORE|other (失敗)|
|:---|:---|:---|:---|
|binding|if (!fail) break;|-|fail = true;|
|required|-|-|fail = true;|
|requisite|-|-|fail = true; break;|
|sufficient|if (!fail) break;|-|-|
|optional|-|-|-|

最も左側の列にcontrol-flag、右側の列には該当の指定をされたモジュールの認証結果に応じた処理が書かれています。例えば、bindingの場合、モジュールが認証成功した場合、過去にすでに失敗したモジュールがなければ、すなわちfail = trueでなければ、認証成功で処理完了となります。いっぽう、モジュールが認証失敗した場合は、認証が失敗した旨のフラグを立てて(fail = true)、後続のモジュールの処理に進む、という具合になります。

次に、[二要素認証を導入する過程](/post/freebsd-login-ssh-server/)で編集したPAM設定を用いて、具体的な処理フローを確認していきたいと思います。今回は認証機能について興味があるので、コンソールログインに関する設定ファイル`login`の`auth`に関する記述を例にとってみましょう。`login`の`auth`の内容は以下のとおりです。(`include`で`system`の設定を読み込むようになっていますので、これを展開しています。また、コメントアウトされている行は省きます。)

```conf
auth  sufficient  pam_self.so                         no_warn
auth  sufficient  pam_opie.so                         no_warn no_fake_prompts
auth  requisite   pam_opieaccess.so                   no_warn allow_local
auth  required    pam_unix.so                         no_warn try_first_pass nullok
auth  required    /usr/local/lib/security/pam_duo.so
```

この認証に関する処理フローを図にしてみました(下図)。以下、図にそって認証処理の流れを見ていきます。(わたし自身はOPIE認証を有効にしていないので、以下ではOPIEに関する部分は省略して説明します。)
[![PAM login処理フロー](/img/pam-login-process-flow-thumbnail.png)](/img/pam-login-process-flow.png)

1. pam_self.so

    まず、pam_self.soを用いた認証処理が行なわれます。本モジュールは、ログイン先のユーザIDがログイン元のユーザIDと同一のとき、また同一のときにのみ成功します。(つまり、すでにログイン済みのユーザがコマンドラインから`login`コマンドを実行して、同一ユーザでログインする場合がこれにあたります。こんなことをするケースはあまりないと思いますが…。) 制御フラグが`sufficient`ですので、本モジュールによる認証が成功した場合は、即ログインが成功します。認証が失敗した場合にはそのまま次のモジュールに進みます。

1. pam_opie.so

    次は、pam_opie.soによる認証です。OPIE認証を用いていない場合は認証が失敗します。制御フラグが`sufficient`ですので、次のモジュールに進みます。
    
1. pam_opieaccess.so

    次は、pam_opieaccess.soによる認証です。OPIE認証を用いていない場合は認証が成功します。制御フラグが`requisite`ですので、次のモジュールに進みます。

1. pam_unix.so

    次は、pam_unix.soによる認証です。本モジュールでは通常のパスワードによる認証を行ないます。制御フラグが`required`ですので、認証が失敗した場合には失敗フラグを立てて次のモジュールに進みます。認証が成功した場合にはそのまま次のモジュールに進みます。
    
1. pam_duo.so

    最後に、pam_duo.soによる認証です。本モジュールではDuo Security社が提供するワンタイムパスワードによる認証を行ないます。制御フラグが`required`ですので、認証が失敗した場合には失敗フラグを立てます。本モジュールが最後なので、最終的に、失敗フラグの値を確認し、その値によってログインの可否が決定されます。(つまり、パスワード認証とワンタイムパスワード認証の両方ともが成功した場合に、ログイン成功となります。)

現在の設定では、パスワード認証が失敗した場合にも二要素目のワンタイムパスワード認証を実行してしまうため、やや不便ですね。パスワード認証が失敗したときに二要素目に進まないようにするには、`pam_unix.so`の制御フラグを`requisite`に変更してやるとよさそうです。

### 参考文献
1. PAM Configuration, https://www.freebsd.org/doc/en_US.ISO8859-1/articles/pam/pam-config.html
