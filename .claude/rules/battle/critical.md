---
paths:
  - "**/battle/**"
  - "**/domain/model/**"
  - "**/domain/usecase/**"
  - "**/data/repository/**"
  - "**/data/local/**"
  - "**/charactercreate/**"
---

# クリティカルヒット

命中したヒットごとに、クリティカル(ダメージ増加)かバッドクリティカル(ダメージ減少)かを判定する。

- **唯一の実装箇所**: `domain/battle/CriticalFormula.kt`(計算と判定)
- **関連**: 計算順は`damage-pipeline.md`、防御による無効化は`defense.md`、睡眠中の確定判定は`status-ailments.md`

## 仕様

- **発生率**: クリティカル発生率の段階n(`BattleStat.CRITICAL_RATE`、±6)から、24分の`rateNumerator`で決まる。
  - n=0は1、n>0は4n(+6で24/24)、n<0は-(4|n|-2)(-1=-2/24 … -6=-22/24)。
  - 値が正ならその確率でクリティカル、負なら絶対値の確率でバッドクリティカルになる(このとき通常のクリティカルは起きない)。
  - 判定では`random.nextInt(24)`を1回引く。
- **倍率**: クリティカル倍率(%) = max(150 + 25 × 段階, 0)(`BattleStat.CRITICAL_DAMAGE`、±12)。バッドクリティカルでは、さらにmax(倍率 - 75, 0)とする。
  - 基本値150(`BattleFieldPawn.criticalDamageRate`)は、ブレイクポイント倍率と同じく戦闘中だけの固定値で、全個体(味方・敵)が持つ。`Character`・`Enemy`・`characters.json`には持たず、キャラ編集でも変えられない。
- **判定の単位**: `performAttack`が、命中したヒット1つごと・対象ごとに、命中判定の後で判定する。外れたヒットは判定せず、乱数も引かない。必中で当たったヒットは判定する。ダメージのない`performSkill`では判定しない。
- **適用**: `max(floor(ダメージ × 倍率 / 100), 1)`。計算順の位置は`damage-pipeline.md`。ブレイクゲージは、倍率込みで実際に減ったHPで貯まる。
- **記録と表記**: `AttackShot.critical`(`CriticalType`: NONE/CRITICAL/BAD_CRITICAL)。**どちらの判定が出たかではなく、最終的な倍率で分類する**(`CriticalFormula.finalType`/`classify`)。
  - 倍率が100未満ならBAD_CRITICAL、100以上(ちょうど100を含む)ならCRITICAL。
  - 例: 倍率-3段階でクリティカルの判定が出た場合は75%なのでバッドクリティカル、倍率+4段階でバッドクリティカルの判定が出た場合は175%なのでクリティカルと表記する。
  - 判定が出なかったヒットと外れたヒットはNONE。
- **段階**: 記録・持続・重ねがけは`stat-modifiers.md`と同じ仕組み。有効な段階の上限・下限は、`StatStageFormula.minStageOf`/`maxStageOf`でステータスごとに決まる(発生率だけ±6)。`effectiveStat`には渡せない(例外)。
- **ナレーション**:
  - 対象1体・1ヒット: 「AがBに攻撃! クリティカル! 45ダメージ」/「AがBに攻撃! バッドクリティカル… 30ダメージ」
  - 多段: 「1回目: クリティカル! 45ダメージ」
  - 範囲攻撃: ダメージの文の後に、クリティカルが出たヒットごとに「Bにクリティカル!」/「Cにバッドクリティカル…」を加え、その後に「〜はブレイクした!」が続く。
