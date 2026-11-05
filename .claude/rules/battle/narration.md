---
paths:
  - "**/battle/**"
  - "**/domain/model/**"
  - "**/domain/usecase/**"
  - "**/data/repository/**"
  - "**/data/local/**"
  - "**/charactercreate/**"
---

# 戦闘のナレーションと表示の連動

攻撃結果を1文ずつナレーションにする仕組みと、文に合わせて画面のフィールド表示を進める仕組み。

- **攻撃の文の組み立て**: `AttackResult.toNarrations` / `toNarrationSteps`(`ui/battle/model/AttackNarrations.kt`)
- **他の文**: スキル(バフ・デバフ)は`SkillNarrations.kt`(`stat-modifiers.md`)、状態異常は`AilmentNarrations.kt`(`status-ailments.md`)、文の型は`BattleNarration.kt`、文と表示の組は`NarrationStep.kt`
- **各仕組み固有の文**: クリティカル(`critical.md`)・ブレイク(`break.md`)・防御(`defense.md`)・属性耐性(`element-resistance.md`)・先制(`preemptive.md`)・技名(`skill-catalog.md`のローリング・ソバット)。それぞれのファイルが正。

## 攻撃のナレーションの形

| ケース | 文 |
|---|---|
| 対象1体・1ヒット | 当たれば「AがBに攻撃! 30ダメージ」、外れれば「AがBに攻撃! しかし外れた!」 |
| 対象1体・2ヒット以上 | 「1回目: 命中! 30ダメージ」「2回目: 外れた!」を1発ずつ表示し、最後に「AがBに合計30ダメージ(2回中1回命中)」。全発外れたら「しかし外れた!」の1文だけ |
| 範囲攻撃(発動回数1) | 1ヒットごとの表示はしない。「Aの攻撃!」→ 全発外れた相手がいれば「BとCには外れた!」→ 命中した相手は「Dに30、Eに25ダメージ!」。全員に外れたら「Aの攻撃! しかし外れた!」の1文だけ |
| 範囲の多段攻撃(対象2体以上かつ2ヒット以上) | 回ごとに報告する(下記) |

**範囲の多段攻撃**:

1. 「Aのクイックシュート!」
2. 回ごとに(外れた相手 → ダメージを与えた相手 → 無効 → クリティカル・吸収・反射の文)を「1回目: Bに30、Cに25ダメージ!」「1回目: Cにクリティカル!」のように「N回目: 」付きで(`BattleNarration.InRound`)
3. その回でブレイクした対象の「Cはブレイクした!」(接頭辞なし)
4. 最後に「AがBに合計30、Cに合計53ダメージ」(`AreaAttackTotal`。ダメージを与えたヒットがある対象だけ。無ければ出さない)
5. 全回・全員に外れたら「Aの攻撃! しかし外れた!」の1文だけ

技名のある攻撃は「攻撃!」の部分が技名になる(`AttackResult.skillId`/`skillName`)。

## ナレーションの進め方

- 演出中のナレーションは、どれもナレーション欄のタップ(`BattleViewModel.onNarrationClick`)で次の文へ進められる。
- 1回のタップで進むのは1文だけで、待っていない間のタップは捨てる。

## 表示との連動(ダメージ技)

ダメージ技(`performAttack`)では、表示がナレーションに連動する。

- **スナップショット**(`performAttack`が残す):
  - `AttackResult.fieldBeforeShots` = コスト消費直後・1発目の前
  - `AttackShot.fieldAfter` = 各ヒットの反映直後
  - `AttackResult.fieldAfter` = TP加算後の最終
- **文ごとの対応**(`AttackResult.toNarrationSteps`。`NarrationStep` = 文+表示するフィールド):
  - 単体: 各ヒットの文 = そのヒットの後
  - 範囲の開始の文 = `fieldBeforeShots`
  - 範囲1発の以降の文 = 最後のヒットの後
  - 範囲多段のN回目の文 = N回目の最後のヒットの後
  - 技全体の最後の文 = `fieldAfter`
- **`BattleViewModel`の流れ**: 実行前に`isDisplayLocked`で購読の反映と自動の手番開始を止める → 1文ごとにそのスナップショットを`reflectedIn`で表示 → 流し終えたら`EndTurnUseCase`の前に解除して最新を反映する。
- 手番終了時の状態異常の出来事(`TurnEndEvent`)も同じく表示をロックして1文ずつ流す(`status-ailments.md`)。
- ダメージのないスキル・防御は従来どおり即時反映。
