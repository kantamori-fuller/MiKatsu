---
paths:
  - "**/battle/**"
  - "**/domain/model/**"
  - "**/domain/usecase/**"
  - "**/data/repository/**"
  - "**/data/local/**"
  - "**/charactercreate/**"
---

# 最大HP(基礎HP)

戦闘中の最大HPの決め方。

- **唯一の実装箇所**: `domain/battle/HitPointFormula.kt`(`BASE_HP = 80`・`maxHpOf`)
- **使う箇所**: `BattleAlly.maxHp`・`BattleEnemy.maxHp`・`GetBattleFieldUseCase`の戦闘開始時の現在HP(満タン)

## 仕様

- **最大HP = HPの種族値 + 基礎HP80**。味方・敵の区別なく全個体に加算する。
- **足すのは戦闘時の計算だけ**: `Character.hp`・`Enemy.hp`・`characters.json`・キャラ作成画面の入力と表示は種族値のまま。既存のキャラにもそのまま効く。
- 戦闘画面のカードと詳細ポップアップ(ステータスH)は加算後の最大HPを表示する。
- HPの割合を使う処理(TP・ブレイクゲージ・状態異常の割合ダメージ・TP不足の代替HPなど)は`maxHp`経由なので、加算後の値を使う。
- 冒険の最初の戦闘は加算後の最大HPで満タンから始まる。次の戦闘へは現在HPをそのまま持ち越す。
- テストの`testAlly`/`testEnemy`の`hp`は「最大HP」の意味で、種族値には`hp - BASE_HP`を入れている。
