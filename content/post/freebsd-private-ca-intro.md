+++
title = "FreeBSDでプライベート認証局(CA)を構築する - まえがき"
date = "2018-03-05T22:34:00+09:00"
categories = ["FreeBSD"]
tags = ["freebsd", "pki", "ssl", "openssl", "private", "ca"]
+++

Let's Encryptが発行したサーバ証明書の数は、2007年12月現在で6100万にものぼるそうです。
{{< tweet 940280276210499584 >}}

以前はサーバ証明書(DV証明書)の取得費用が高額だったため、個人運営のサーバでは自己署名証明書(いわゆるオレオレ証明書)がよく用いられてきました。Let's EncryptによってDV (Domain Verification)証明書が無料で取得できるようになったため、インターネットに公開するサーバでオレオレ証明書を使う理由はなくなったといえそうです。

しかし、Let's Encryptが発行するのはサーバ向けのDV証明書のみであり、クライアント証明書は扱っていません。クライアント証明書の用途って何でしょうか? ビジネスならば、電子メールの署名・暗号化がもっともポピュラーだと思われます。パーソナルユースだと? そうです、VPN (Virtual Private Network, 仮想私設網)です。VPNサーバがVPNクライアントを認証する際に用いられるケースが、わりとポピュラーなのではないでしょうか。

VPNも、もともとは企業の拠点間を経済的かつ安全にネットワーク接続するためのビジネス向けの技術でした。しかしながら、携帯電話網に加えて公衆無線LANが個人のモバイルネット接続で用いられるようになり、その安全性を確保するための技術としても注目されています。

そこで、次回からのいくつかの記事では、プライベート認証局(CA, Certificate Authority)の構築、OpenVPNサーバ・クライアント向け証明書の発行、OpenVPNサーバの構築、およびクライアントからOpenVPNサーバへの接続まで、順に説明していきたいと思います。

公開鍵証明書を含む公開鍵インフラストラクチャ(PKI, Public Key Infrastructure)は複雑で理解が困難です。しかし、さいわいなことに、ラムダノート社から発売されている、「[プロフェッショナルSSL/TLS](https://www.lambdanote.com/collections/ssl-tls)」のOpenSSLに関連する第11, 12章を抜き出した「[OpenSSLクックブック](https://www.lambdanote.com/collections/frontpage/products/openssl)」が、なんと無料で入手できます。

次回の記事では、OpenSSLクックブックを参考にしながら、まず、プライベートCAを構築していくことにします。

### 参考文献
1. プロフェッショナルSSL/TLS, https://www.lambdanote.com/collections/ssl-tls
1. OpenSSLクックブック, https://www.lambdanote.com/collections/frontpage/products/openssl
