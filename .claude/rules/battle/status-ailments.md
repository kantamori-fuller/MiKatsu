---
paths:
  - "**/battle/**"
  - "**/domain/model/**"
  - "**/domain/usecase/**"
  - "**/data/repository/**"
  - "**/data/local/**"
  - "**/charactercreate/**"
---

# 状態異常(ネガティブエフェクト)

個体にかかる、手番をまたいで残るマイナスの状態。ステータスのバフ・デバフ(段階式、`stat-modifiers.md`)とは別の仕組み。

- **唯一の実装箇所**: `domain/battle/StatusAilmentFormula.kt`(計算・付与・解除・手番終了時の処理)
- **モデル**: `StatusAilment`(enum。**宣言順が扱い・表示・手番終了時の処理の順**)と`AilmentState`(種類・残りターン・`grantedDuringOwnTurn`・感染の分母)
- **精神系**(洗脳・発狂・魅了・恐怖・挑発。enum末尾の5種)は強さによる排他・自動行動・対象の変化などを持つので、`mental-ailments.md`にまとめている
- **関連**: ダメージ計算での位置は`damage-pipeline.md`、耐性との順序は`element-resistance.md`、付与するスキルは`skill-catalog.md`(カオスリズム・命頂戴)

## 状態

- `BattleFieldPawn.ailments`(味方・敵共通、同じ種類は1つだけ)。戦闘中だけの値で、`Character`・`Enemy`・`characters.json`に持たず、次の戦闘へは持ち越さない。
- 戦闘不能になると全解除(`applyDamage`)。
- 敵も`BattleEnemy.currentMp`(開始時`enemy.mana`)を持ち、汚染・呪怨で減る。

## 付与

- **効果**: `ActiveSkillEffect.InflictAilment(ailment, durationTurns, successRate=100)`と`InstantDeath(successRate=100)`。
- **成功率**: 命中とは別に、効果が実際にかかる確率(0〜100、補正なし)。
  - 判定の順: 命中判定 → 属性耐性(無効/吸収/反射。`affectsEffects`のルール) → 効果ごとに成功判定(`random.nextInt(100) < 成功率`、100なら乱数を引かない。乱数は対象の順 × 効果の順)。
  - 反射で跳ね返った効果も使用者に対して成功判定をする。
  - 失敗は効果がかからないだけで**命中扱い**(使用者のTP条件4・受けた側の条件8は成立)。
- **付与のルール**(`inflictAilment`):
  - 同じ種類が既にあれば残りターンを長い方で上書き(感染の分母はそのまま)。
  - 火傷と凍傷・火傷と凍結は排他で、後からかけた方が失敗(凍傷と凍結は共存可)。
  - 石化をかけると他の状態異常をすべて解除し、石化中は石化以外かからない(失敗)。即死は石化中でも効く。
  - 精神系どうしは強さで排他(強い方がかかっていれば失敗、弱い方は消えて付与。`mental-ailments.md`)。
  - 本人の手番中にかかった分は`grantedDuringOwnTurn`で、その手番の終了ではダメージも減算もしない。
- **即死**: 成功したらHPを0にして戦闘不能。

## 手番終了時の処理(`processTurnEndAilments`)

`endTurn`の他の処理の後・先制の割り込みの前。本人の手番終了時にenum順で、状態異常ごとに「ダメージ等 → (感染なら伝染判定) → 残りターンを1減らし0で解除」。行動不能で飛ばされた手番も数える。割合は`floor(最大値/分母)`で最低1。

| 状態異常 | 手番終了時 |
|---|---|
| 毒 | HP 1/8 |
| 火傷 | HP 30 |
| 凍傷 | HP 10 |
| 流血・感電 | HP 1/16 |
| 呪怨 | HP 1/16・MP 1/8 |
| 汚染 | MP 1/8・TP 12(0未満にしない) |
| 感染 | HP 1/分母。分母は16から始まり、ダメージのたびに2ずつ減って最小2 |

- **感染の伝染**: ダメージ後に30%で、同じ陣営の生存・未感染・石化していない個体からランダムに1体へ伝染(候補がいなければ乱数なし。`nextInt(100)` → `nextInt(候補数)`)。伝染先は分母16、残りターンは伝染元の減算前の値。命中・耐性・成功判定はない。
- このダメージはブレイクゲージ・TP条件2を貯めず、睡眠・凍結も解除しない。戦闘不能になったら以降を打ち切る。
- **出来事の返し方**: `TurnEndEvent`(HpDamage/MpLoss/TpLoss/Spread/Cured/Defeated。それぞれ直後のフィールドを持つ)として`EndTurnUseCase`の戻り値`EndTurnResult`(`cancelledPreemptive`と`events`)で返し、`BattleViewModel`が表示をロックして1文ずつナレーションとフィールドを連動させて流す。

## 行動不能

- スタン・凍結・感電・石化・睡眠(`isIncapacitated`)。恐怖のスタンは`AilmentState.causedByFear`で区別し、文は「Aは恐怖で体がすくんだ!」(`mental-ailments.md`)。
- `performAttack`/`performSkill`/`defend`は`null`。
- 手番が来たら`BattleViewModel`が自動で飛ばし「Aは〈名〉で動けない!」(ブレイク中ならブレイクの文が優先、複数ならenum順の先頭)。敵も同じ。
- 予約不可・先制の手番は取り消し(`preemptive.md`)。
- ブレイクと違い被ダメ増・被必中は付かない。

## 戦闘中の効果

- **ダメージ順**: `damage-pipeline.md`の表(凍結×2 → 流血×1.2(切り捨て) → … → 石化なら常に1(無効・吸収・反射も起きない))。反射の再計算も同じ。
- **睡眠・凍結**: 攻撃のヒットでHPが実際に減ったら、そのヒットの直後に解除(「Bは目を覚ました!」「Bの凍結が解けた!」)。凍結の2倍は解けたそのヒットだけ。睡眠中の対象へのヒットは乱数を引かずにクリティカル判定(倍率は攻撃者、最終倍率で分類、防御中は無効化)。
- **凍傷**: `effectiveStat(ATTACK)`の段階補正後の値に×4/6(切り捨て)。±12の段階に数えない独立の補正。
- **麻痺**: `effectiveStat(AGILITY)`に×0.7、ブレイクゲージ計算の耐性に×0.7(保存値は変えない)。
- 凍傷・麻痺はいずれも`StatusAilmentFormula.adjustStat`/`effectiveBreakResistance`。
- **目眩**: 攻撃者の最終命中率×0.4(必中はそのまま)。
- **MP/TPスキル封印**: コストにMP/TPを含むスキルが使えない(`canAfford`)。
- **烙印**: HP/MPの回復量が1以上なら1(吸収による回復・TPの獲得は対象外)。

## ナレーション

- 付与: 「Bは毒になった!」(精神系だけ「Bは洗脳された!」などの専用の文)、全効果が失敗なら「しかしBには効かなかった!」、一部だけなら「Bには〈名〉が効かなかった!」、「Bは即死した!」。発動回数2以上は「N回目: 」付き。
- 手番終了: 「Bは毒で12ダメージ」「BはMPが5減った」「BはTPが12減った」「Bの感染がCにうつった!」「Bの毒が治った」「Bは戦闘不能になった!」。
- 実装: `ui/battle/model/AilmentNarrations.kt`。

## 表示

- カードに短いラベル(精神系は「洗脳」「発狂」「魅了」「恐怖」「挑発」)(MP/TPスキル封印は「MP封」「TP封」。`StatusAilmentLabel`)。
- 詳細ポップアップに「毒 残り3」の一覧(無ければ「なし」)。敵のMPも現在/最大で表示。
