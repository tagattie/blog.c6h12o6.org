+++
title = "FreeBSDでパッケージの更新がうまくいかないとき"
date = 2018-01-25T15:14:41+09:00
categories = ["FreeBSD"]
tags = ["freebsd", "package", "pkg", "nfs"]
+++

FreeBSDでパッケージを更新しようとしたときに、以下のようなエラーが出て、うまくいかないときがあります。
```sh
pkg: sqlite error while executing PRAGMA user_version; in file pkgdb.c:2372: database is locked
```

これは、パッケージのデータベースディレクトリに、ロックのための空ディレクトリが残ってしまっていることが原因です。なので、[このサイト](http://conocimiento.subteni.com/)に書かれているとおり、空ディレクトリを削除してやると更新がうまくいきます。
```sh
sudo rmdir /var/db/pkg/local.sqlite.lock
```

エラーがたまにしか起きないときは、いちいち手動で空ディレクトリを削除すればよいのですが、パッケージデータベースディレクトリがNFSマウントされているときには、このエラーが頻繁に起きてしまいます。(例えば、NFSルートでディスクレス運用しているマシンのパッケージを更新するときが該当しますね。)

このようなときには、`pkg`コマンドが「ちゃんとした」ロックを行なうように、`/usr/local/etc/pkg.conf`に以下の一行を追加するとうまくいきます。
```conf
NFS_WITH_PROPER_LOCKING = true;
```
