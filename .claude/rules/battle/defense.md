---
paths:
  - "**/battle/**"
  - "**/domain/model/**"
  - "**/domain/usecase/**"
  - "**/data/repository/**"
  - "**/data/local/**"
  - "**/charactercreate/**"
---

# 防御

戦闘画面の基本行動(スキル・防御・撤退)の1つ。ボタンは横1列で「スキル | 防御 | 撤退」、選択中は「選択中 | キャンセル」。通常攻撃はスキル一覧から行う(`skills.md`)。

- **唯一の実装箇所**: `domain/battle/DefenseFormula.kt`(計算と状態の変化)
- **関連**: 計算順は`damage-pipeline.md`

## 使用

- `BattleField.defend`(`DefendUseCase`経由)。使用者が現在の行動者の味方・戦闘不能でない・ブレイク中でない・行動不能の状態異常でない・勝敗未確定のときだけ成立し、`BattleFieldPawn.isDefending`を`true`にする。成立した瞬間にTP+5(`tp.md`)。
- 手番の終了は呼び出し側が`EndTurnUseCase`で行う。
- ボタンは味方の手番で、演出中でなく、選択状態が`None`のときだけ有効(先制の手番では無効。`preemptive.md`)。
- 挑発中は防御できない(`defend`が`null`。洗脳・発狂で自動で動いている間は挑発の制限は効かない)。ボタンは`BattleUiState.canDefend`で無効になる。`DefendUseCase`は洗脳・発狂の個体も弾く(`mental-ailments.md`)。
- 今回は味方だけが使い、敵AIは使わない(状態は敵味方共通で持つ)。

## 効果(防御中の個体が攻撃を受けるとき)

- **被ダメージ30%カット**: `max(floor(ダメージ × 70 / 100), 1)`。位置は`damage-pipeline.md`。
- **クリティカル無効**: 判定(乱数1回)の結果、最終種別が`CRITICAL`(最終倍率100%以上)になるヒットは通常ヒット扱い(`AttackShot.critical = NONE`、`isCriticalBlocked = true`)。`BAD_CRITICAL`はそのまま発生する。反射で攻撃者が防御中のとき・睡眠中の確定判定でも同じ。

## 解除

- 次の自分の手番が始まったとき(`advanceToNextTurn`がキューの先頭の防御を解除)、またはブレイクした時点(`accumulateBreakGauge`)。
- 多段攻撃では、ブレイクさせたヒットまでは防御ありで、残りのヒットは防御なし。
- 戦闘中だけの値で、次の戦闘へは持ち越さない。

## 表示

- 防御時に「Aは防御している!」。
- クリティカルを防いだヒットは「クリティカル!」と同じ位置に「防御でクリティカルを防いだ!」(範囲攻撃では「Bは防御でクリティカルを防いだ!」)。
- 詳細ポップアップでは防御中のときだけ、ブレイク状態の近くに「防御中」を表示する(カードには出さない)。
- 解除の文は出さない。
