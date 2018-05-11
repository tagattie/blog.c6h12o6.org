+++
title = "FreeBSDでChromium (Chrome)をできるだけ安定して使う"
date = "2018-05-11T15:01:08+09:00"
draft = true
categories = ["FreeBSD"]
tags = ["freebsd", "chromium", "chrome", "browser", "hang", "stability"]
+++

FreeBSDユーザのみなさん、Webブラウザには何をお使いですか? Chromium (Chrome)? Firefox? あるいはOperaをお使いですか? わたしはというと、メインブラウザにChromium、サブにFirefoxを使っているのですが、ここのところChromiumの動作が安定しない問題に悩まされています。

いつごろからだったのか、もうはっきりとした記憶がないのですが、あるときからChromiumの動作が安定しなくなりました。「安定しない」といっても、ブラウジングの最中にChromiumが突然落ちるわけではありません。おもな症状は以下の二つです。

- ページを開くときにローディングの状態のまま進まない
- マウスやキーボードでの操作に反応しない(例: スクロールさせている途中で反応がなくなる)

ひどい時には、ブラウザ全体がフリーズして操作にまったく反応せず、タブのクローズや切り替えもできなくなります。こうなってしまうと、もう「ムキーっ」となりながらコマンドラインで`killall chrome`するしかありません。

当然のことながら、Chromiumがハングアップしたりフリーズしたりという件に関しては、いくつかバグレポートが出ています。(代表的と思われるものを以下に二つ挙げます。)

- [Bug 211036  -  www/chromium hangs and becomes unresponsive just opening a new tab (FreeBSD)](https://bugs.freebsd.org/bugzilla/show_bug.cgi?id=211036)
- [Bug 212812 -  www/chromium: tabs "hang" 10% of the time (FreeBSD)](https://bugs.freebsd.org/bugzilla/show_bug.cgi?id=212812)

バグレポートを見てみると、これ、かなり息の長い問題なんですね。最初の日付は2016年の夏頃、Chromiumのバージョンでいうと52くらいから問題が報告されています。

もちろん、バグを前にして手をこまねいているわけではなく、上記レポートの中でいくつも解決方法が提案されています。ただ、いずれの提案も、効果がある、効果がない、という反応が混在していて、現時点で決定的といえるものはないようです。

そういうわけで、「できるだけ安定して使う」というなんとも中途半端なタイトルになってしまったわけですが、試してみて効果がある(と感じる)症状緩和策を四つ紹介したいと思います。(単なるプラセボ効果かもしれませんが…)

- [キャッシュディレクトリをメモリファイルシステムとしてマウント](#キャッシュディレクトリをメモリファイルシステムとしてマウント)
- [GPUハードウェアアクセラレーションの無効化](#gpuハードウェアアクセラレーションの無効化)
- [V8 JavaScriptエンジンのキャッシュを無効化](#v8-javascriptエンジンのキャッシュを無効化)
- [HTTPシンプルキャッシュを使用](#httpシンプルキャッシュを使用)

### キャッシュディレクトリをメモリファイルシステムとしてマウント
`chromium`パッケージをインストールしたときに、インストール後のメッセージとして表示される方法です。(公式パッケージのインストール時に表示される方法なので、おそらくFreeBSDコミュニティとして最も効果があると判断されているのだと思います。)

インストール時のメッセージを再度表示させるには、以下のコマンドを実行すればOKです。

``` shell
pkg info -D chromium
```

対応策は、Chromiumを使用するユーザで以下のコマンドを実行します。(以下はユーザ`exampleuser`、グループ`examplegroup`を想定したコマンドになっていますので、お使いの環境に合わせて読み替えをお願いします。)

``` shell
rm -rf ~/.cache/chromium
mkdir ~/.cache/chromium
sudo echo "md $(echo ~exampleuser)/.cache/chromium mfs rw,late,-wexampleuser:examplegroup,-s300m 2 0" >> /etc/fstab
sudo mount ~example/.cache/chromium
```

### GPUハードウェアアクセラレーションの無効化
`--disable-gpu`オプションをつけてChromiumを起動します。

``` shell
chrome --disable-gpu
```

デスクトップ環境を使用していて、ランチャ経由でChromiumを起動する場合は、ランチャに上記オプションを追加しておくと便利です。(下図)

![Chromium - ランチャ - オプション追加](/img/chromium/chromium-launcher-add-option.png)

Chromiumのコマンドラインオプションの詳細については、以下の二つのURLを参考にしてください。

- [Run Chromium with flags (The Chromium Project)](https://www.chromium.org/developers/how-tos/run-chromium-with-flags)
- [List of Chromium Command Line Switches (Peter Beverloo)](https://peter.sh/experiments/chromium-command-line-switches/)

### V8 JavaScriptエンジンのキャッシュを無効化
[Bug 212812](https://bugs.freebsd.org/bugzilla/show_bug.cgi?id=212812)で["Tomo Method"](https://bugs.freebsd.org/bugzilla/show_bug.cgi?id=212812#c24)と呼ばれている方法です。Chromiumを起動して、以下のURLにアクセスします。

- `chrome://flags/#v8-cache-options`

"Default"となっている設定値を"Disabled"に変更して、Chromiumを再起動してください。

![Chromium - フラグ - v8キャッシュ](/img/chromium/chromium-flags-v8-cache.png)

### HTTPシンプルキャッシュを使用
[Bug 212812](https://bugs.freebsd.org/bugzilla/show_bug.cgi?id=212812)の[コメント53](https://bugs.freebsd.org/bugzilla/show_bug.cgi?id=212812#c53)で提案されている方法です。Chromiumを起動して、以下のURLにアクセスします。

- `chrome://flags/#enable-simple-cache-backend`

"Default"となっている設定値を"Enabled"に変更して、Chromiumを再起動します。

![Chromium - フラグ - HTTPシンプルキャッシュ](/img/chromium/chomium-flags-http-simple-cache.png)

以上、Chromiumのハングアップに関する症状緩和策を四つ紹介しました。Chromiumユーザのお役に立てばさいわいです。

### 参考文献
1. Bug 211036  -  www/chromium hangs and becomes unresponsive just opening a new tab, https://bugs.freebsd.org/bugzilla/show_bug.cgi?id=211036
1. Bug 212812 -  www/chromium: tabs "hang" 10% of the time, https://bugs.freebsd.org/bugzilla/show_bug.cgi?id=212812
1. Run Chromium with flags, https://www.chromium.org/developers/how-tos/run-chromium-with-flags
1. List of Chromium Command Line Switches, https://peter.sh/experiments/chromium-command-line-switches/
