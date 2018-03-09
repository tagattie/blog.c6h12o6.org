+++
title = "FreeBSDのプライベート認証局(CA)で証明書を発行する"
date = "2018-03-09T20:09:00+09:00"
categories = ["FreeBSD"]
tags = ["freebsd", "pki", "ssl", "openssl", "private", "ca", "certificate"]
+++

[プライベート認証局(CA)構築](/post/freebsd-private-ca-setup/)では、FreeBSD上にプライベートCAを構築する手順について説明しました。本記事では、実際にサーバ、クライアントで用いる証明書を発行する手順を説明します。

引き続き、以下のGitHubリポジトリにあるスクリプトを使用する前提で説明していきます。

- [FreeBSD-Private-CA](https://github.com/tagattie/FreeBSD-Private-CA) - Scripts for creating and managing a two-tier simple private CA for FreeBSD.

### サーバあるいはクライアント(証明書の発行を依頼する側)での作業
#### 準備
まず、リポジトリをチェックアウトしてください。

``` shell
git clone https://github.com/tagattie/FreeBSD-Private-CA.git
cd FreeBSD-Private-CA
```

FreeBSDでは、デフォルトで`/etc/ssl`以下にOpenSSLの設定ファイルや秘密鍵、証明書を格納するようになっています。ここでは異なるディレクトリを使う理由がありませんので、チェックアウトしたリポジトリのトップにあるスクリプト(3つ)と設定ファイル(1つ)を、デフォルトのディレクトリにコピー(あるいはシンボリックリンクを作成)します。

``` shell
cp *.sh *.cnf /etc/ssl
cd /etc/ssl
./00_setup.sh
```

その後、セットアップスクリプト`00_setup.sh`を実行します。(中身を見ていただければわかりますが、`certs`と`private`というディレクトリを作成するだけのものです。) これらのディレクトリは、それぞれ証明書および秘密鍵を格納するために用いられます。

#### 証明書署名要求(CSR)の作成
デフォルトディレクトリ(`/etc/ssl`)でスクリプト`01_create_csr.sh`を実行します。この際、CSRにサブジェクト代替名(Subject Alternative Name, SAN)拡張を含めたい場合は`-a`オプションを指定してください。

- サブジェクト代替名(Subject Alternative Name, SAN)拡張とは?
    
    通常、証明書の発行対象は単一のコモンネーム(Common Name, CN)になります。(サーバやクライアント向け証明書の場合はドメイン名(Fully Qualified Domain Name, FQDN)だと思ってください。)
    
    例えば、`www.example.com`というドメイン名に対して発行された証明書を提示するWebサーバがあったとします。このサーバは`web.example.com`とか、単に`example.com`とかでもアクセス可能かもしれません。しかし、証明書は`www.example.com`向けに発行されていますので、後二者のドメイン名でアクセスすると証明書の検証でエラーとなります。
    
    SAN拡張はこのようなケースに用いるもので、`www.example.com`の代替の名前として`web.example.com`や`example.com`も許される証明書の発行を依頼できます。

以下の例では`-a`オプションを使用して、SAN拡張のドメイン名を入力する過程が含まれています。

``` shell-session
$ ./01_create_csr.sh -a example

Please specifiy subject alternative name(s) separated by space.         # -a オプションでSAN拡張のドメイン名の入力を求められます
example.com openvpn.example.com vpn.example.com                         # ドメイン名(複数可)を空白で区切って入力してください

Environment variable OPENSSL_SAN=DNS:example.com;DNS:openvpn.example.com;DNS:vpn.example.com

### Generating RSA private key...
Generating RSA private key, 2048 bit long modulus
..........................................................+++
............+++
e is 65537 (0x10001)

### Encrypting RSA private key...
writing RSA key
Enter PEM pass phrase:<passphrase>                                      # 秘密鍵のパスフレーズを入力
Verifying - Enter PEM pass phrase:<passphrase>                          # 秘密鍵のパスフレーズを再入力

### Creatting example's certificate signing request...
Enter pass phrase for /etc/ssl/private/example.key:<passphrase>         # 秘密鍵のパスフレーズを入力
You are about to be asked to enter information that will be incorporated
into your certificate request.
What you are about to enter is what is called a Distinguished Name or a DN.
There are quite a few fields but you can leave some blank
For some fields there will be a default value,
If you enter '.', the field will be left blank.
-----
Country Name (2 letter code) [AU]:JP                                    # 国名を入力
State or Province Name (full name) [Some-State]:Kanagawa                # 都道府県名を入力
Locality Name (eg, city) []:                                            # 市町村名を入力
Organization Name (eg, company) [Internet Widgits Pty Ltd]:Example Org  # 組織名を入力
Organizational Unit Name (eg, section) []:                              # 部門名を入力
Common Name (e.g. server FQDN or YOUR name) []:example.com              # コモンネーム(ドメイン名)を入力
Email Address []:                                                       # Eメールアドレスを入力

Please enter the following 'extra' attributes
to be sent with your certificate request
A challenge password []:                                                # 入力せずエンター
An optional company name []:                                            # 入力せずエンター
```

以上でCSRの生成は終了です。`certs`ディレクトリに`example.csr`というファイルができていますので、これを何らかの手段でSigning CAに送付します。(例えば、Signing CAと同じマシンで実行した場合は、単純にファイルをSigning CAのディレクトリにコピーすればOKです。)

### Signing CA (証明書を発行する側)
#### CSRへの署名
CSRを入手したら、これをSigning CAのディレクトリにコピーします。

``` shell
cp example.csr /<path>/<to>/<ca>/FreeBSD-Private-CA/signing-ca
```

Signing CAのディレクトリに`cd`して、以下のいずれかのスクリプトを実行してください。

- `01_sign_csr_server.sh`: サーバ証明書の場合
- `01_sign_csr_client.sh`: クライアント証明書の場合

スクリプトを実行すると3つの証明書ファイルができます。

- `example.crt`: サーバ or クライアント証明書
- `example.crt.notext`: 証明書からテキスト形式の部分を削除したもの
- `example.crt.full`: フルチェーン証明書(サーバ/クライアント証明書とSigning CAの証明書を連結したもの)

生成した証明書ファイルを要求者に返送してください。ちなみに、本記事以降で構築を予定しているOpenVPNサーバおよびクライアント向けにはフルチェーン証明書を用います。

### 参考文献
1. FreeBSD-Private-CA, https://github.com/tagattie/FreeBSD-Private-CA
1. OpenSSLクックブック, https://www.lambdanote.com/products/openssl
