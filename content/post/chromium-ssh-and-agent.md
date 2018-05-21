+++
title = "Chromium OS (Chrome)のSecure ShellでSSH Agentを使う"
date = "2018-05-21T22:04:00+09:00"
categories = ["ChromiumOS"]
tags = ["chromiumos", "chromeos", "chromebook", "secure", "shell", "ssh", "agent", "extension"]
+++

NECのラップトップ[LaVie G タイプZ](http://nec-lavie.jp/navigate/products/pc/133q/10/lavie/lvz/spec/pc-gl186y3az.html)で、自前ビルドした[Chromium OS](https://www.chromium.org/chromium-os)を使い始めてからもう一か月半が経ちます。内蔵ドライブのWindowsを上書きするのはちょっとためらわれるので、USBメモリから起動して使っています。Windowsをいちおう残してはいますが、Chromium OSとWindowsのバッテリ駆動時間を比較する[ベンチマーク](/post/chromiumos-self-build-bench/)を行なって以来、起動していません。

文書(図表の編集を含む)作成とWebブラウジングくらいであれば、まったくWindowsの出る幕がないです。Chromium OS本当にいいですよ。バッテリベンチマークではWindowsに負けましたが、実際の使用ではChromium OSのほうがやや長く持つように感じますし。最近では、もっと本格的に使うためにChromebookを購入しようかと考えています。

さて、ラップトップの用途ですが一つ書き忘れました。文書作成とブラウジングに加えて、サーバ管理の際のSSHクライアントとしても使っています。ここでも、Chrome上で動作するSSHクライアントの[Secure Shell App](https://chrome.google.com/webstore/detail/secure-shell-app/pnhechapfaindjhompbnflcldabbghjo)を使えばこと足ります。(Secure Shell Appの使い方については、下記二つのURLをご参照ください。)

- [ChromeのSSHクライアント「Secure Shell」が公開鍵認証に対応！ブラウザだけでサーバー管理ができる時代に近づいた (Chrome Life)](http://www.chrome-life.com/chrome/5629/)
- [Chromebook Secure Shell 公開鍵認証でCentOSにSSH接続する (designetwork)](http://designetwork.hatenablog.com/entry/2017/02/02/public-ssh-on-chromebook)

しかし、怠惰なもので、接続のたびに秘密鍵のパスフレーズを入力するのが面倒になってきました。こんなときはssh-agentですよね。Secure Shell Appでもssh-agent(と同等の機能)を使えないかと調べてみました。すると、ありました。[SSH Agent for Google Chrome](https://chrome.google.com/webstore/detail/ssh-agent-for-google-chro/eechpbnaifiimgajnomdipfaamobdfha)という拡張機能です。Secure Shell Appと組み合わせて使うと、接続ごとのパスフレーズ入力を省くことができます。

本記事ではSSH Agent for Google Chrome (以下、単にSSH Agentと呼びます)とSecure Shell Appを組み合わせた使用法を紹介します。でも、その前に以下の注意書きを**必ずお読みください**。

**注**: Chromeの同期が有効な場合、エージェントに登録した**秘密鍵も同期の対象**になります。したがって、秘密鍵は必ず**十分な強度のパスフレーズで保護**してください。**秘密鍵が漏えい**する可能性がゼロとはいえませんので、インターネットから直接アクセス可能なサーバやその他重要なサーバへのアクセス鍵は**登録しない**ことをおすすめします。本拡張機能の使用は自己責任でお願いします。

納得いただけましたら、以下にお進みください。インストール、および設定の手順はSSH AgentのREADME (下記URL)に準じています。

- [SSH Agent for Google Chrome, README.md (GitHub)](https://github.com/google/chrome-ssh-agent/blob/master/README.md)

### インストール
まず、以下のURLにアクセスしてSecure Shell App、およびSSH Agentをインストールします。

- [Secure Shell App (Chrome Web Store)](https://chrome.google.com/webstore/detail/secure-shell-app/pnhechapfaindjhompbnflcldabbghjo)
- [SSH Agent for Google Chrome™ (Chrome Web Store)](https://chrome.google.com/webstore/detail/ssh-agent-for-google-chro/eechpbnaifiimgajnomdipfaamobdfha)

### 秘密鍵の登録
SSH Agentをインストールすると、ブラウザのツールバーにアイコンが追加されますので、これをクリックします。

![Chromium - SSH Agent - アイコン](/img/chromiumos/chromium-ssh-agent-icon.png)

"Add Key"ボタンをクリックして秘密鍵を登録します。Nameには識別しやすい任意の名前をつけます。Private Keyには秘密鍵ファイルの内容をまるごとコピペしてください(`-----BEGIN XXX PRIVATE KEY-----`から`-----END XXX PRIVATE KEY-----`まですべて)。本記事の例では、RSA形式の秘密鍵を登録しています。(ECDSA鍵やED25519鍵については**動作未確認**です。)

![Chromium - SSH Agent - 秘密鍵追加](/img/chromiumos/chromium-ssh-agent-add-private-key.png)

"Add"ボタンをクリックすると秘密鍵が追加されます。

### 秘密鍵のパスフレーズ入力
次に、秘密鍵のパスフレーズを入力して秘密鍵をエージェントに読み込ませます。読み込ませたい秘密鍵の"Load"ボタンをクリックしてください。すると、パスフレーズの入力ダイアログが表示されますので、秘密鍵のパスフレーズを入力します。

![Chromium - SSH Agent - パスフレーズ入力](/img/chromiumos/chromium-ssh-agent-load-private-key.png)

入力後"OK"ボタンをクリックすると、秘密鍵の種別および内容が表示され、エージェントに読み込み済みであることが示されます。

![Chromium - SSH Agent - 秘密鍵読み込み完了](/img/chromiumos/chromium-ssh-agent-private-key-loaded.png)

SSH Agentでの設定は以上で終了です。

### SSH接続
では、SSH接続を行なってみましょう。Chromium OSのランチャーボタン、あるいはURL `chrome://apps`からSecure Shell Appを起動します。SSH Agentを使用するためには、"SSH 中継サーバーのオプション"の欄に`--ssh-agent=eechpbnaifiimgajnomdipfaamobdfha`を追加してください。Secure Shell Appでの秘密鍵のインポートは不要です。

![Chromium - Secure Shell - 接続](/img/chromiumos/chromium-ssh-agent-connect.png)

接続先情報の入力が終わったら、右下部にある「接続」をクリックします。下図の例のように、パスフレーズの入力を求められることなくログインが可能です。

![Chromium - Secure Shell - 接続完了](/img/chromiumos/chromium-ssh-agent-connected.png)

ちなみに、「接続」の代わりに「SFTP マウント」をクリックすると、接続に使用したユーザのホームディレクトリをマウントできます(下図)。いったんマウントしてしまえば、ファイルアプリからローカルドライブと同様にアクセスできますので、これもとても便利ですよ。

![Chromium - Secure Shell - SFTPマウント](/img/chromiumos/chromium-ssh-agent-sftp-mount.png)

### 参考文献
1. Secure Shell App, https://chrome.google.com/webstore/detail/secure-shell-app/pnhechapfaindjhompbnflcldabbghjo
1. ChromeのSSHクライアント「Secure Shell」が公開鍵認証に対応！ブラウザだけでサーバー管理ができる時代に近づいた, http://www.chrome-life.com/chrome/5629/
1. Chromebook Secure Shell 公開鍵認証でCentOSにSSH接続する, http://designetwork.hatenablog.com/entry/2017/02/02/public-ssh-on-chromebook
1. SSH Agent for Google Chrome™, 
https://chrome.google.com/webstore/detail/ssh-agent-for-google-chro/eechpbnaifiimgajnomdipfaamobdfha
1. chrome-ssh-agent, https://github.com/google/chrome-ssh-agent
