+++
title = "FreeBSDのプライベート認証局(CA)で証明書を失効させる"
date = "2018-03-10T21:44:00+09:00"
categories = ["FreeBSD"]
tags = ["freebsd", "pki", "ssl", "openssl", "private", "ca", "certificate"]
+++

[証明書の発行](/post/freebsd-private-ca-cert/)では、FreeBSD上のプライベート認証局(Certificate Authority, CA)で証明書を発行する手続きを説明しました。サーバ―クライアント間の通信を行なう際に、証明書を用いることで、確かに通信相手が意図している相手(ドメイン名)であることを確認できます。

しかし、証明書を作成するもとになった、秘密鍵が紛失したり漏えいしてしまったらどうでしょうか? 悪意ある者が漏えいした秘密鍵を使って証明書を偽造し、なりすましを行なうかもしれません。そうすると、もはや証明書を使っても相手が正しい相手であることを保証できなくなります。このような場合は、すみやかに証明書を失効させるべきです。

本記事では、証明書の発行に加えて、CAのもう一つの役割である証明書の失効処理、および失効リストの作成について説明します。引き続き、以下のGitHubリポジトリにあるスクリプトを使用する前提で説明していきます。

- [FreeBSD-Private-CA](https://github.com/tagattie/FreeBSD-Private-CA) - Scripts for creating and managing a two-tier simple private CA for FreeBSD.

### 証明書の失効
失効処理を行なうに先立って、失効させたい証明書のシリアル番号がわかっている必要があります。証明書ファイルが手もとにある場合は、以下のコマンドでシリアル番号を含めた証明書のテキスト情報を確認できます。

``` shell
openssl x509 -in <証明書ファイル>.crt -noout -text
```

証明書が手元にない場合でも、発行日時、発行対象とした国名、都道府県名やコモンネーム(Common Name, CN)などがわかっていれば、CAのディレクトリにある`db/index`ファイルからシリアル番号を検索することもできます。(`db/index`はCAが発行した証明書の一覧を格納しています。)

では、失効の手順を説明します。以下の各ケースに応じて、それぞれのディレクトリに`cd`します。

- Signing CAの証明書を失効させたいとき: Root CAのディレクトリ
- サーバ、クライアントの証明書を失効させたいとき: Signing CAのディレクトリ

以下では、Signing CAが発行した証明書を失効させる例を示します。Singing CAのディレクトリに`cd`し、スクリプト`01_revoke_crt.sh`を実行します。

```shell
cd /<path>/<to>/<ca>/FreeBSD-Private-CA/signing-ca
./01_revoke_crt.sh newcerts/<証明書のシリアル番号>.pem
```

証明書の失効理由の入力を求められますので、適切な理由を選択してください。その後、CAの秘密鍵のパスフレーズを入力すれば、指定した証明書の失効処理は完了です。

出力例は以下のようになります。

```shell-session
$ ./01_revoke_crt.sh newcerts/4F48D09643300C499DA6F6F3707FAE94.pem

Choose reason of revocation... (0-7):                          # 失効理由の入力を求められますので、
  0 - unspecified                                              # いずれかを選択してください。
  1 - keyCompromise
  2 - CACompromise
  3 - affiliationChanged
  4 - superseded
  5 - cessationOfOperation
  6 - certificateHold
  7 - removeFromCRL
1

### Revoking specified certificate...
Using configuration from signing-ca.cnf
Enter pass phrase for ./private/signing-ca.key:<passphrase>    # 秘密鍵のパスフレーズを入力
Revoking Certificate 4F48D09643300C499DA6F6F3707FAE94.
Data Base Updated
```

### 証明書失効リスト(CRL, Certificate Revocation List)の作成
失効処理のときと同様、以下の各ケースに応じて、それぞれのディレクトリに`cd`します。

- Root CAのCRLを作成したいとき: Root CAのディレクトリ
- Signing CAのCRLを作成したいとき: Signing CAのディレクトリ

CAのディレクトリでスクリプト`01_generate_crl.sh`を実行します。以下の例では、Signing CAのCRLを生成しています。

```shell
cd FreeBSD-Private-CA/signing-ca
./01_generate_crl.sh
```

処理を行なったCAに応じて、`root-ca.crl`あるいは`signing-ca.crl`というファイルがディレクトリ`crl`に生成されます。

内容を確認してみると、失効処理の例で失効させた証明書がCRLに含まれていることがわかります。

``` shell-session
$ openssl crl -in crl/signing-ca.crl -noout -text
Certificate Revocation List (CRL):
        Version 2 (0x1)
    Signature Algorithm: ecdsa-with-SHA256
        Issuer: /C=JP/ST=Kanagawa/O=Example Org/CN=Example Org Signing CA
        Last Update: Mar  9 12:30:44 2018 GMT
        Next Update: Apr  8 12:30:44 2018 GMT
        CRL extensions:
            X509v3 Authority Key Identifier: 
                keyid:F7:B1:76:CB:CF:F3:32:6D:64:C9:8B:9B:F0:94:45:67:FA:0B:2A:DA

            X509v3 CRL Number: 
                4097
Revoked Certificates:
    Serial Number: 4F48D09643300C499DA6F6F3707FAE94
        Revocation Date: Mar  6 11:18:33 2018 GMT
        CRL entry extensions:
            X509v3 CRL Reason Code: 
                Key Compromise
    Signature Algorithm: ecdsa-with-SHA256
         30:64:02:30:56:3f:35:d9:bf:c8:04:48:de:8a:98:c1:fd:4a:
         (snip)
         b1:4d:1d:c6:67:66:bd:67:39:22:cc:49
```

### 参考文献
1. FreeBSD-Private-CA, https://github.com/tagattie/FreeBSD-Private-CA
1. OpenSSLクックブック, https://www.lambdanote.com/products/openssl
