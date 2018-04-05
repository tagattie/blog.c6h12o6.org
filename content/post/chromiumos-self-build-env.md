+++
title = "Chromium OSでバッテリ駆動時間は伸びるのか? NEC LaVie Zで試す - ビルド環境設定編"
date = "2018-04-01T14:51:28+09:00"
draft = true
categories = ["ChromiumOS"]
tags = ["chromiumos", "chromeos", "nec", "lavie", "chromebook", "battery", "ubuntu", "googleapi"]
+++

[ビルド用VM構築編](/post/chromiumos-self-build-vm/)では、Chromium OSをビルドするための環境として、FreeBSDの仮想化機構である[bhyve](https://wiki.freebsd.org/bhyve)上にUbuntu 14.04が動作する仮想マシンを構築しました。さっそくChromium OSのソースコードを取得してビルド、といきたいところですが、残念ながらそう簡単にはいきません。その前にいくつか準備が必要です。

本記事では、Chromium OSのビルドに先立って行なう準備作業について説明していきます。

一部順序を入れ替えていますが、基本的には[Chromium OS Developer Guide](https://www.chromium.org/chromium-os/developer-guide)に沿って作業を進めます。素のChromium OSだけでなく、Googleが提供する各種APIへのアクセス(最も重要なのはGoogle Driveへのアクセスだろうと思います)も含めて試したい場合は、APIキーを取得する必要があります。これがかなり面倒ですが、本記事の内容を参考にして進めていただければだいじょうぶだと思います。

準備は大きく分けて三つのパートからなります。(後二者はオプションです。Googleの各種APIを利用する場合のみ行なってください。)

- [Ubuntuの環境設定](#ubuntuの環境設定)
- [Gerrit (ソースコードレビューツール)関連の設定(オプション)](#gerritの設定-オプション)
- [Google API関連の設定(オプション)](#google-apiの設定-オプション)

### Ubuntuの環境設定
#### デフォルトのファイルアクセス権(umask)の設定([Developer Guide対応部分](http://dev.chromium.org/chromium-os/developer-guide#TOC-Verify-that-your-default-file-permissions-umask-setting-is-correct))
Chromium OSのビルドは、Ubuntu上にさらにChromium OS SDK (cros_sdk)と呼ばれるchroot環境を作成して、この環境上で行ないます。cros_sdkから、チェックアウトしたChromium OSのソースコードにアクセスするために、ファイルがworld-readableである必要があります。つまり、オーナ、グループに加え、前記二者に属さないユーザがファイルの読み取りを行なえる必要があります。

最初に、デフォルトのファイルアクセス権(umask)設定を確認し、必要ならばその値を変更します。以下のコマンドを実行してumaskの値を確認しましょう。

``` shell-session
$ umask
0002
```

Ubuntu 14.04でのデフォルトは`0002`になっていますね。このままでOKです。

#### 必要なソフトウェアのインストール([Developer Guide対応部分](http://dev.chromium.org/chromium-os/developer-guide#TOC-Install-git-and-curl))
`git`, `curl`などのビルドに必要なコマンドをインストールします。Developer Guideではgit GUIとgitkもインストールしますが、ビルドを行なうだけなら不要ですので、以下のコマンド例では省いています。

``` shell
apt install git-core curl lvm2 thin-provisioning-tools python-pkg-resources python-virtualenv python-oauth2client
```

#### Gitの設定([Developer Guide対応部分](http://commondatastorage.googleapis.com/chrome-infra-docs/flat/depot_tools/docs/html/depot_tools_tutorial.html#_bootstrapping_configuration))
以下でdepot_toolsのソースコードをチェックアウトする前に、`git`コマンドの基本的な設定を済ませておきましょう。名前とメールアドレスは適宜読み替えてください。後ほど設定する、GerritやGoogle APIを設定するアカウントと同じものにしておくことをおすすめします。

``` shell
git config --global user.name "Your Name"
git config --global user.email "youraddress@example.com"
git config --global core.autocrlf false
git config --global core.filemode false
git config --global color.ui true
```

#### depot_toolsのチェックアウト([Developer Guide対応部分](http://commondatastorage.googleapis.com/chrome-infra-docs/flat/depot_tools/docs/html/depot_tools_tutorial.html#_get_depot_tools))
`git`を用いてdepot_toolsのリポジトリをチェックアウトします。ビルドを実行する際には、本リポジトリに含まれる`repo`コマンドを用いてChromium OSのソースコード一式を取得します。

``` shell
cd ~
git clone https://chromium.googlesource.com/chromium/tools/depot_tools.git
```

コマンドラインから`repo`コマンドを実行できるよう、チェックアウトしたディレクトリをパスに加えておきましょう。

- `${HOME}/.bashrc`

    ``` bashrc
export PATH=${PATH}:${HOME}/depot_tools
```

#### `sudo`の設定調整([Developer Guide対応部分](http://dev.chromium.org/chromium-os/tips-and-tricks-for-chromium-os-developers#TOC-Making-sudo-a-little-more-permissive))
cros_sdk上でのビルドが行なえるように、`sudo`コマンドの設定を一部調整します。下記のコマンドを実行してください。

``` shell
cd /tmp
cat > ./sudo_editor <<EOF
#! /bin/sh
echo Defaults \!tty_tickets > \$1          # Entering your password in one shell affects all shells 
echo Defaults timestamp_timeout=180 >> \$1 # Time between re-requesting your password, in minutes
EOF
chmod +x ./sudo_editor 
sudo EDITOR=./sudo_editor visudo -f /etc/sudoers.d/relax_requirements
```

以上でUbuntuの環境設定は終了です。

### Gerritの設定(オプション)
Ubuntuの環境設定がひととおり終わりましたので、次は[Gerrit](https://www.gerritcodereview.com/)の設定に進みます。Gerritとは、WebベースのGit向けリポジトリ管理およびソースコードレビュー用のソフトウェアです。ここからは、Developer Guideのドキュメント"[Gerrit credentials setup (for Chromium OS and Chrome OS)](http://www.chromium.org/chromium-os/developer-guide/gerrit-guide)"を参考にして設定を進めていきます。

まず、ブラウザで以下のURLにアクセスします。

- https://chromium-review.googlesource.com/new-password

Gerritで使用するGoogleアカウントを選択してログインします。

![Gerrit - ログイン](/img/chromiumos/chromiumos-gerrit-login.png)

ログインできたら、まずログインに使用したアカウント(メールアドレス)を再確認してください。OKであれば、表示されているシェルスクリプトをコピーして、ホームディレクトリで実行します。すると、`${HOME}/.gitcookies`というファイルが生成されます。

![Gerrit - Gitcookies](/img/chromiumos/chromiumos-gerrit-gitcookies.png)

正しく`.gitcookies`ファイルが生成されたかを、以下のコマンドを実行して確認します。パスワードの入力を求められることなく、以下のようにGitリポジトリへのリファレンスのリストが出力されればOKです。

``` shell-session
$ git ls-remote https://chromium.googlesource.com/a/chromiumos/manifest.git
4e75e9797d7c940da5dfb3bc8e8b9bfeda14e07f        HEAD
64a43418102660d49d1495cac84a45dc86ea5f55        refs/cache-automerge/b3/ff98a1dc9a5fbd02c3be8dc123e54ee4be8ae6
d8a75f0e8ae0d2e50af36b57f4fbb7a1ce0136a8        refs/changes/00/14300/1
a37063b2281451cdfe279058360175ffe2001002        refs/changes/00/14300/meta
2e5f4cbebda72439dd1b687dc3da28605f14b28c        refs/changes/00/202700/1
(snip)
```

次に、Gerritで表示されるフルネームが設定されているかを確認します。以下のURLにアクセスします。

- https://chromium-review.googlesource.com/#/settings/

"Full name"の欄を確認してください。ご自分のフルネームが表示されていればOKです。もし、表示されていない場合は、Google+のプロフィールを更新したあとで再度確認してみてください。

![Gerrit - ユーザプロフィール](/img/chromiumos/chromiumos-gerrit-fullname.png)

以上でGerritの設定は終了です。

### Google APIの設定(オプション)
次は、Google APIの設定に進みます。ここからは、Developer Guideのドキュメント"[API Keys](http://www.chromium.org/developers/how-tos/api-keys)"を参考にして設定を進めていきます。

Chromium OSが使用するGoogle APIですが、一部プライベートAPIが含まれているため、これらも含めて利用するにはChromium OSの開発者メーリングリストに参加する必要があります。まず、以下のURLにアクセスして参加登録をします。

- https://groups.google.com/a/chromium.org/forum/?fromgroups#!forum/chromium-dev

メーリングリストへのポスト一覧が表示されている上に、「投稿するにはグループに参加してください」というボタンがありますので、これをクリックします。

![Chromium-dev ML - メイン](/img/chromiumos/chromiumos-chromiumdevml-main.png)

まず、プロフィール名(フルネーム)とメールアドレスを確認してください。メーリングリストのメールを受け取りたくない場合は、メール配信設定を「更新情報をメールで送信しない」に設定します。内容を確認してOKであれば、「このグループに参加」ボタンをクリックします。

![Chromium-dev ML - 参加](/img/chromiumos/chromiumos-chromiumdevml-subscribe.png)

次に、**Chromium-devメーリングリストに登録したアカウントでログインしている**ことを確認した後、[Google Cloud Platformのコンソール](https://console.cloud.google.com/)にアクセスします。そして、ハンバーガーメニューから「IAMと管理→リソースの管理」を選択します。

![Google Cloud Platform Console - IAMと管理 - リソースの管理](/img/chromiumos/chromiumos-gcp-console-resource-management.png)

現在のプロジェクト一覧が表示されます。画面上部にある「プロジェクトを作成」をクリックします。

![Google Cloud Platform Console - IAMと管理 - リソースの管理 - プロジェクト一覧](/img/chromiumos/chromiumos-gcp-console-resource-management-main.png)

プロジェクト名(任意でかまいません)を入力して、「作成」ボタンをクリックします。

![Google Cloud Platform Console - IAMと管理 - リソースの管理 - 新規プロジェクト](/img/chromiumos/chromiumos-gcp-console-resource-management-new-project.png)

プロジェクト一覧表示に戻りますので、いま追加したプロジェクトが表示されていることを確認します。

![Google Cloud Platform Console - IAMと管理 - リソースの管理 - プロジェクト一覧](/img/chromiumos/chromiumos-gcp-console-resource-management-main-2.png)

プロジェクトが作成できました。次は、このプロジェクトでGoogle APIが使えるように設定します。ハンバーガーメニューから「APIとサービス→ライブラリ」を選択します。

![Google Cloud Platform Console - APIとサービス - ライブラリ](/img/chromiumos/chromiumos-gcp-console-api-library.png)

APIの検索画面になりますので、有効化したいAPIを検索します。どのAPIを有効化すべきかについては、[API Keys, Acquiring Keys](http://www.chromium.org/developers/how-tos/api-keys#TOC-Acquiring-Keys)の第6項を参照してください。

![Google Cloud Platform Console - APIとサービス - ライブラリ - 一覧](/img/chromiumos/chromiumos-gcp-console-api-library-search.png)

例えば、Google Now For Chrome APIを検索、クリックすると以下のような画面になりますので、「有効にする」ボタンをクリックします。APIごとに、個別に有効化を行なう必要があるので面倒ですが、一つ一つ進めていきます。

![Google Cloud Platform Console - APIとサービス - ライブラリ - 有効化](/img/chromiumos/chromiumos-gcp-console-api-library-enable.png)

有効化したいAPIをすべて有効化できたら「APIとサービス→ダッシュボード」に行きましょう。有効化されているAPIのリストを確認してください。もし、もれがあった場合は、再度APIを検索して有効化の手続きを行ないます。

![Google Cloud Platform Console - APIとサービス - ライブラリ - 有効化一覧](/img/chromiumos/chromiumos-gcp-console-api-library-list.png)

今回作成したプロジェクトでのGoogle APIの有効化ができました。次は、API利用時に用いる認証情報を設定します。ハンバーガーメニューから「APIとサービス→認証情報」を選択します。

![Google Cloud Platform Console - APIとサービス - 認証情報](/img/chromiumos/chromiumos-gcp-console-api-cred.png)

「認証情報を作成」ボタンをクリックして、「OAuthクライアントID」を選択します。

![Google Cloud Platform Console - APIとサービス - 認証情報 - クライアントID](/img/chromiumos/chromiumos-gcp-console-api-cred-new-oauth-client-id.png)

OAuthの同意画面を作成する必要がありますので、「同意画面を設定」ボタンをクリックします。

![Google Cloud Platform Console - APIとサービス - 認証情報 - 同意画面](/img/chromiumos/chromiumos-gcp-console-api-cred-new-oauth-consent.png)

メールアドレスを確認した後、ユーザに表示するサービス名(任意でかまいません)を入力します。その後、「保存」ボタンをクリックします。

![Google Cloud Platform Console - APIとサービス - 認証情報 - 同意画面設定](/img/chromiumos/chromiumos-gcp-console-api-cred-new-oauth-consent-edit.png)

クライアントIDの作成画面に戻ったら、アプリケーションの種類として「その他」を選択します。また、アプリケーションの名前(任意でかまいません)を入力します。その後、「作成」ボタンをクリックします。

![Google Cloud Platform Console - APIとサービス - 認証情報 - クライアントID設定](/img/chromiumos/chromiumos-gcp-console-api-cred-new-oauth-client-edit.png)

クライアントIDとクライアントシークレットが表示されますので、これらをコピーしてテキストファイルに保存しておいてください。

![Google Cloud Platform Console - APIとサービス - 認証情報 - クライアントID表示](/img/chromiumos/chromiumos-gcp-console-api-cred-new-oauth-client-id-save.png)

次に、もう一度「認証情報を作成」ボタンをクリックして、今度は「APIキー」を選択します。

![Google Cloud Platform Console - APIとサービス - 認証情報 - APIキー](/img/chromiumos/chromiumos-gcp-console-api-cred-new-api-key.png)

APIキーが表示されますので、これをコピーしてテキストファイルに保存しておいてください。

![Google Cloud Platform Console - APIとサービス - 認証情報 - APIキー表示](/img/chromiumos/chromiumos-gcp-console-api-cred-new-api-key-save.png)

以上で、Google APIを利用するのに必要な認証情報の生成は終了です。

Developer Guideの"[Chrome API keys in the Chromium OS SDK chroot](https://www.chromium.org/chromium-os/how-tos-and-troubleshooting/building-chromium-browser/chrome-api-keys-in-the-chroot)"を参考にして、`${HOME}/.googleapikeys`ファイルを作成します。さきほど保存しておいた、クライアントID、クライアントシークレット、およびAPIキーをファイルに記入してください。

- `${HOME}/.googleapikeys`

    ``` conf
'google_api_key': '<APIキー>',
'google_default_client_id':     '<クライアントID>',
'google_default_client_secret': '<クライアントシークレット>',
```

これでGoogle APIの設定は終了です。ようやく、ビルドを行なう準備が整いました。

### 参考文献
1. Chromium OS Developer Guide, https://www.chromium.org/chromium-os/developer-guide
1. depot_tools_tutorial - A tutorial introduction to the Chromium depot_tools git extensions, http://commondatastorage.googleapis.com/chrome-infra-docs/flat/depot_tools/docs/html/depot_tools_tutorial.html
1. Gerrit credentials setup (for Chromium OS and Chrome OS), http://www.chromium.org/chromium-os/developer-guide/gerrit-guide
1. API Keys, http://www.chromium.org/developers/how-tos/api-keys
1. Chrome API keys in the Chromium OS SDK chroot, https://www.chromium.org/chromium-os/how-tos-and-troubleshooting/building-chromium-browser/chrome-api-keys-in-the-chroot
