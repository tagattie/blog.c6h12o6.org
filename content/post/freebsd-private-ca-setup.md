+++
title = "FreeBSDでプライベート認証局(CA)を構築する - 本編"
date = "2018-03-08T08:10:00+09:00"
categories = ["FreeBSD"]
tags = ["freebsd", "pki", "ssl", "openssl", "private", "ca"]
+++

[まえがき](/post/freebsd-private-ca-intro/)では、無料の証明書サービスが提供されている現在、プライベート認証局(CA, Certificate Authority)を構築するケースについて簡単に説明しました。プライベート証明書を使う一つの理由がVPNサーバによるVPNクライアントの認証です。

本記事では、プライベートCAを構築する手順について説明します。いろいろと試行錯誤をしているうちに、シェルスクリプトと設定ファイルがひと通りできましたので、GitHubのリポジトリを作成しました。

- [FreeBSD-Private-CA](https://github.com/tagattie/FreeBSD-Private-CA) - Scripts for creating and managing a two-tier simple private CA for FreeBSD.

非常につたないものではありますが、本記事ではこのスクリプトを使用する前提で説明していきます。

複雑で難しいという印象がある証明書システムですが、本当に難しいです。本記事を書くにあたり、いろいろと調査しましたが全貌を把握するのはそう簡単ではないです。そういうわけで、本記事では二層構造のプライベート認証局(Root CA, Signing CA)と証明書を利用するサーバ、クライアントを含むシステムの概略構成と処理の流れを簡単に説明したあと、CAを構築する具体的な手順を説明します。

ちゃんと勉強するならば、

- [プロフェッショナルSSL/TLS](https://www.lambdanote.com/products/tls)
- [PKI関連技術情報](https://www.ipa.go.jp/security/pki/)

などをあたるのがよさそうです。

なんとか構成を理解しようということで、Signing CAの証明書に署名するRoot CA、サーバ、クライアントの証明書に署名するSigning CA、そして実際に証明書を利用するサーバ、クライアントを含めたブロック図を書いてみました。(下図)

![二層構成のCA概略](/img/two-tier-ca-overview.png)

おのおのが発行、利用する証明書の用途は異なりますが、証明書発行までの流れは大まかにいうと以下のようになります。

- 証明書の発行を受ける側
    - 秘密鍵を作成
    - 秘密鍵から証明書署名要求(CSR, Certificate Signing Request)を作成  
      (必要に応じてX.509v3拡張を含める)
    - CSRをCAに送付
- 証明書を発行する側
    - CSRに署名(証明書完成)  
      証明書の用途に合わせたX.509v3拡張を証明書に含める  
      さらに、Signing CAの場合、CSRに含まれる拡張が上記拡張に含まれていなければ、これを証明書にコピーする
    - 証明書を依頼者に送付

CSRと証明書が主体者間をやり取りされるわけですが、上に述べたように、それぞれにX.509v3拡張を含めることができます。拡張にはさまざまなものがありますが、図中には証明書の用途を規定する拡張を例示しました。実際に使用している拡張については、OpenSSLの設定ファイル内のセクション名を図中に示しましたので、詳しく知りたい場合は設定ファイルの該当セクションを参照してください。

では、以下CAを構築する具体的な手順を説明します。

### 準備
まず、リポジトリをチェックアウトしてください。

```shell
git clone https://github.com/tagattie/FreeBSD-Private-CA.git
cd FreeBSD-Private-CA
```

必要に応じて以下の設定ファイルの内容を変更します。特に`req_distinguished_name`セクションの内容(国、県、市、組織の名前など)を変更したくなると思います。

- `root-ca/root-ca.cnf`
- `signing-ca/signing-ca.cnf`

### ルート認証局(Root CA)の作成
以下のコマンドを実行します。

```shell
cd root-ca
./00_setup_root-ca.sh
```

カレントディレクトリにRoot CAの証明書(`root-ca.crt`)が生成されます。

### サイニング認証局(Signing CA)の作成 
以下のコマンドを実行します。

```shell
cd ../signing-ca
./00_setup_signing-ca.sh
```

カレントディレクトリにSigning CAのCSR(`signing-ca.csr`)が生成されます。これをRoot CAのディレクトリにコピーしてください。その後、Root CAのディレクトリに移動し、Root CAとしてSigning CAのCSRに署名します。

```shell
cp signing-ca.csr ../root-ca 
cd ../root-ca
./01_sign_csr.sh
```

カレントディレクトリにSigning CAの証明書(`signing-ca.crt`)が生成されます。これをSigning CAのディレクトリにコピーしてください。

```shell
cp signing-ca.crt ../signing-ca
```

Signing CAのディレクトリに移動し、最後に証明書内の説明的テキストを削除します。

```shell
cd ../signing-ca
./01_strip_crt_text.sh
```

削除されたテキストは`openssl x509 -in signing-ca.crt -noout -text`でいつでも確認できます。

以上でプライベートCAの構築は完了です。サーバあるいはクライアントの証明書に署名する準備ができました。


### 参考文献
1. FreeBSD-Private-CA, https://github.com/tagattie/FreeBSD-Private-CA
1. プロフェッショナルSSL/TLS, https://www.lambdanote.com/products/tls
1. PKI関連技術情報, https://www.ipa.go.jp/security/pki/
