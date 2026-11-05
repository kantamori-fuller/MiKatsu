---
paths:
  - "**/battle/**"
  - "**/domain/model/**"
  - "**/domain/usecase/**"
  - "**/data/repository/**"
  - "**/data/local/**"
  - "**/charactercreate/**"
---

# ブレイク

ダメージを受けるとブレイクゲージが貯まり、満タンになると1ターン行動できなくなる。

- **唯一の実装箇所**: `domain/battle/BreakFormula.kt`(計算・状態の変化・定数)
- **関連**: 計算順は`damage-pipeline.md`、麻痺による耐性の補正は`status-ailments.md`

## 戦闘中だけの値

`BattleFieldPawn`の以下の値。キャラ編集では変えられず、`Character`・`Enemy`・`characters.json`には持たない。戦闘ごとに初期値から始まり、次の戦闘へは持ち越さない。

- `breakGauge`: 0〜100、Double
- `breakResistance`: ブレイク耐性。基本100、0〜999
- `breakPointRate`: ブレイクポイント倍率。基本150、攻撃する側の値で将来バフ/デバフで変わる
- `breakState`(`BreakState`): NONE / BROKEN=ブレイク / RECOVERING=立ち上がり

## ゲージの増え方

- 1ヒットごとに`実際に減ったHP ÷ 最大HP × 100 × 攻撃者の倍率/100 × 対象の耐性/100`(`BreakFormula.gaugeIncrease`)。
  - 実際に減ったHPは`min(ダメージ, ヒット前の現在HP)`で、軽減後の値。
  - 例: 最大HP120に60ダメージなら75、耐性80なら60。
  - 対象が麻痺なら耐性×0.7(保存値は変えない。`effectiveBreakResistance`)。
- 小数のまま持ち、画面のバーだけ切り捨てて表示する(数値・耐性は画面に出さない)。
- 攻撃のダメージだけで貯まり、HPコスト・TP代替・状態異常のダメージでは貯まらず、回復しても減らない。
- BROKEN・RECOVERING中は貯まらない。そのヒットで倒れた対象はブレイクしない。

## ブレイク

ゲージが100以上になったら0に戻し(超過分は捨てる)、BROKENにする(`accumulateBreakGauge`)。ブレイクした時点で防御も解除する(`defense.md`)。BROKENの間は次の3つが効く。多段攻撃の途中でブレイクしたら、残りのヒットからすぐに効く(`performAttack`はヒットごとにフィールドへ反映する)。

- **行動できない**: `performAttack`/`performSkill`/`defend`は`null`。手番が来たら`BattleViewModel`が自動で飛ばし、「〜はブレイクしていて動けない!」「〜は立ち上がった!」を表示して`EndTurnUseCase`で終える。予約もできない(`preemptive.md`)。
- **被ダメージが30%増える**: ×1.3を切り捨て(`boostDamage`)。位置は`damage-pipeline.md`。
- **被必中になる**: `HitChanceFormula.isHit`の`isTargetGuaranteedHit`で、乱数を引かずに必ず命中する。

## 状態の経過

本人の手番終了時(`endTurn`→`progressBreakState`)に、BROKEN→RECOVERING、RECOVERING→NONEになり、そのとき耐性を20下げる(数値が低いほど貯まりにくい。ブレイクのたびに下がり、0で止まる)。

## 表示

- ブレイクした対象ごとに、攻撃のナレーションの後へ「〜はブレイクした!」を加える(`AttackShot.causedBreak`)。
- カードにはゲージのバー(黄色)と、「ブレイク」「立ち上がり」のラベルを出す。
