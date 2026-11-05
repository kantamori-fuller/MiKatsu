---
paths:
  - "**/battle/**"
  - "**/domain/model/**"
  - "**/domain/usecase/**"
  - "**/data/repository/**"
  - "**/data/local/**"
  - "**/charactercreate/**"
---

# ステータスのバフ・デバフ(段階)

ステータスを段階で上げ下げする仕組み。

- **唯一の実装箇所**: `domain/battle/StatStageFormula.kt`
- **対象のステータス**(enum`BattleStat`):
  - 基礎ステータス6種: 破壊力`attack`・耐久力`defense`・マナ出力`manaOutput`・マナ耐性`manaResistance`・敏捷`agility`・幸運`luck` → このファイルの倍率表(4基準)
  - 命中率`ACCURACY`・回避率`EVASION` → 仕組みは同じ、倍率表は`hit-chance.md`(6基準)
  - クリティカル発生率`CRITICAL_RATE`(±6)・クリティカル倍率`CRITICAL_DAMAGE` → 仕組みは同じ、使い方は`critical.md`
- **関連**: 状態異常の凍傷・麻痺による補正(段階に数えない独立の補正)は`status-ailments.md`

## 仕様

- **段階と倍率**(基礎ステータス6種): -12〜+12。段階nの倍率は、n≥0なら`(4+n)/4`、n<0なら`4/(4+|n|)`(+1=5/4、+4=2、+12=4、-1=4/5、-2=2/3、-12=1/4)。補正後の値は`floor(基礎値 × 分子 / 分母)`を整数演算で求める。
- **上限・下限**: `StatStageFormula.minStageOf`/`maxStageOf`でステータスごとに決まる(クリティカル発生率だけ±6、他は±12)。
- **記録**: `BattleFieldPawn.statModifiers`(`StatModifier`: ステータス・段階・残りターン)に、かけた1回分ごとに別のエントリで持ち、別々に切れる。有効な段階はそのステータスの全エントリの合計を上限・下限に収めた値(`effectiveStage`)。記録は上限を超えても残る(+12中の+1も記録し、古い分が切れても残りの合計で求め直す)。
- **永続**: `StatModifier.remainingTurns`(`StatChange.durationTurns`)が`null`のエントリは手番終了で減らず、その戦闘の終わりまで切れない(次の戦闘へは持ち越さない)。
- **持続**: かけられた本人の手番終了時(`endTurn`→`progressStatModifiers`)に残りターンを1減らし、0で消える。本人の手番中にかけられた分(自分にかけた精神一到)はその手番の終了では減らさない(`grantedDuringOwnTurn`)。
- **戦闘中だけの値**: `Character`・`Enemy`・`characters.json`には持たない。次の戦闘へは持ち越さない。ブレイクゲージは貯めない。

## 使う箇所

- 基礎値のプロパティ(`attack`等)は基礎値のまま。**戦闘の計算は必ず`effectiveStat(BattleStat)`を使う**(ダメージ計算の破壊力・耐久力、特殊型攻撃のマナ出力・マナ耐性、行動速度の敏捷、同時手番の幸運比較、先制の並び順の敏捷・幸運)。
- `effectiveStat`は命中率・回避率・クリティカル発生率・クリティカル倍率には使えない(4基準の表で誤った値になるため例外を投げる)。段階は`effectiveStage`で取り、`HitChanceFormula`・`CriticalFormula`へ渡す。
- 確定済みの`turnQueue`は幸運が変わっても引き直さない。

## ナレーション

- `SkillResult.toNarrations`(`ui/battle/model/SkillNarrations.kt`)。
- 「Aの精神一到!」→「Aの破壊力が1段階上がった!」(命中率・回避率なども同じ形)。
- 外れたら「Aの覇気! しかし外れた!」の1文だけ。
- ±12で頭打ちでも文面は同じ。

## 表示

詳細ポップアップのステータス変化欄(`unit-detail-popup.md`)。
