+++
title = "FreeBSDでパッケージデータベースが破損した、あるいは削除してしまったときの復旧手順"
date = "2018-04-25T19:46:00+09:00"
lastmod = "2018-04-26T20:15:00+09:00"
categories = ["FreeBSD"]
tags = ["freebsd", "package", "database", "delete", "corrupt", "pkg", "sqlite", "backup", "restore", "recovery"]
+++

**追記: 2018/4/26**  
FreeBSDとpkgのバージョンを記載し忘れていました。下記事象が発生したのは、以下のバージョンの組み合わせです。

- FreeBSD - 11.1-RELEASE-p9
- pkg - 1.10.5

___

FreeBSDには、さまざまなソフトウェアを手軽に使えるよう、ビルド済みのバイナリパッケージが用意されています。バイナリパッケージのインストール、更新、および削除には`pkg(8)`コマンドを使用します。

また、ソフトウェアをソースコードからビルドしたいユーザ向けには、Portsという仕組みが用意されています。こちらを使う場合は、ビルドするための時間は必要になりますが、ビルドのオプションを自分好みにカスタマイズできるという柔軟性が得られます。

バイナリパッケージを使う場合、インストールされたソフトウェアのカタログは[SQLite](https://www.sqlite.org/)データベースとして管理されており、以下のファイルに格納されています。

- `/var/db/pkg/local.sqlite`

上記ファイルはたいへん重要ですので、万が一の破損や操作ミスによる削除などに備えて、一日に一度バックアップが自動で取られるようになっています。

先日、このファイルをあやまって削除してしまいました。少しあせりましたが、バックアップがあることはわかっていましたので、これを使ってさっそく復旧を試みました。以下のコマンドを実行します。

``` shell-session
# cd <適当なディレクトリ>
# cp /var/backups/pkg.sql.xz .
# unxz pkg.sql.xz
# pkg backup -r pkg.sql
Restoring database:
Restoring: 100%
pkg: sqlite error while executing backup step in file backup.c:97: not an error
pkg: sqlite error -- (null)
```

えっ!? これでうまく行くはずなのに、エラーが出てしまいます。エラーメッセージの中に、`error`と`not an error`が混在していて、エラーなのかエラーでないのかわからない…。ここに至って、かなりあせりました。急いで、エラーメッセージをGoogle検索に放り込んでみると、関係ありそうなURLが二つ見つかりました。

- [How to recreate local.sqlite? (The FreeBSD Forums)](https://forums.freebsd.org/threads/how-to-recreate-local-sqlite.49057/)
- [pkg backup broken #1183 (GitHub)](https://github.com/freebsd/pkg/issues/1183)

URLに記載されている手順にしたがって、再度復旧を試みます。

``` shell-session
$ cd <適当なディレクトリ>
$ xzcat /var/backup/pkg.sql.xz | sqlite3 tmp.sqlite
$ sudo cp tmp.sqlite /var/db/pkg/local.sqlite
```

ほーっ…。なんとか復旧出来ました。参考URLでは、`pkg shell`を起動して、`PRAGMA user_version=30;`を実行していますが、この手順は不要でした。

以上、メモとして復旧手順を記録しておきます。

### 参考文献
1. SQLite, https://www.sqlite.org/
1. How to recreate local.sqlite?, https://forums.freebsd.org/threads/how-to-recreate-local-sqlite.49057/
1. pkg backup broken #1183, https://github.com/freebsd/pkg/issues/1183
