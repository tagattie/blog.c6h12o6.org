+++
title = "フレッツ光におけるPPPoEとIPoE+DS-LiteでのIPv4 ping応答時間の違い"
date = "2018-01-26T18:06:25+09:00"
categories = ["Network"]
tags = ["flets", "ipv4", "pppoe", "ipoe", "dslite"]
+++

わが家では、フレッツ光+IIJmioでインターネットに接続していますが、だいぶん前から夕方から深夜にかけての速度低下が顕著になってきました。[Munin](http://munin-monitoring.org/)を使って計測した、ここ1か月間のPPPoE経由、およびIPoE+DS-Lite経由での、`www.iijmio.jp`に対するpingの応答時間のグラフを比較してみます。

![PPPoE](/img/ping_www_iijmio_jp-month-pppoe.png)
![IPoE+DSLite](/img/ping_www_iijmio_jp-month-dslite.png)

一目瞭然ですね。PPPoE接続のほうは、応答時間が夜間に最大40~50ms程度まで増加しているのに対し、IPoE+DS-Liteのほうは5~6ms程度で安定しています。(PPPoEでも、お正月3が日くらいはあまり遅くなっていないのがちょっと面白いかも。)

### 参考文献
- Munin, http://munin-monitoring.org/
