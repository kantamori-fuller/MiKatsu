---
paths:
  - "**/battle/**"
  - "**/domain/model/**"
  - "**/domain/usecase/**"
  - "**/data/repository/**"
  - "**/data/local/**"
  - "**/charactercreate/**"
---

# 先制行動

カードをタップして「予約」しておくと、現在の行動者の手番が終わったところで行動順に割り込み、先制スキル(`ActiveSkill.preemptivePriority`がある=クイックシュートなど)を使える。

- **用語**: **予約** = カードをタップした時点の記録 / **先制の手番** = 行動順に割り込んだ手番
- **唯一の実装箇所**: 判定と割り込みは`domain/battle/PreemptiveAction.kt`、手番の進行への組み込みは`ActionTimeline.kt`(`endTurnWithCancellations` / `advanceToNextTurnWithCancellations`)
- **関連**: 手番の進行は`turn-system.md`、スキルの使用可否は`skills.md`

## 状態

`BattleField`の以下の値。戦闘中だけの値で、次の戦闘へは持ち越さない。先制の手番は常に`turnQueue`の先頭側にまとまる。

- `pendingPreemptive`: まだ割り込んでいない予約のFPIDの列
- `preemptiveTurns`: `turnQueue`の中で先制の手番になっているFPIDの集合
- `isPreemptiveTurn`: 現在の手番が先制の手番か

## 予約できる条件

`canReservePreemptive`が唯一の判定箇所で、UIも使う。`ReservePreemptiveActionUseCase`経由で保存。すべて満たすこと:

- 味方
- 戦闘不能でない / ブレイク中でない / 行動不能の状態異常でない
- まだ予約しておらず、先制の手番として行動順にも入っていない
- 勝敗が決まっていない
- 現在の行動者ではない
- 先制スキルのうち1つ以上が「CTが明けている・`canAfford`・`selectableTargets`が空でない」(`isSkillUsable`。スキル一覧の使用可否と同じ基準)

## 割り込み

- `endTurn`が、手番を進める`advanceToNextTurn`の**前**に、その時点の予約をすべて取り出して並べ(`applyPreemptiveReservations`)、`turnQueue`の先頭(残っている同時手番より前)に入れて先制の印を付け、予約を空にする。
- **並び順**: 優先度(その個体の先制スキルの中で一番高い値)の高い順 → `effectiveStat(AGILITY)`の高い順 → `effectiveStat(LUCK)`の高い順 → それでも同じなら`random`でくじ(手番開始のTPの乱数より前に引く)。
- 先制の手番の最中に入った予約は、その手番の`endTurn`で同じように先頭に入るので、残っている先制の手番より前に来る。
- 予約した個体がすでに通常の手番として`turnQueue`に並んでいたら、その通常の手番は取り除く(前借り)。

## 先制の手番の扱い

- 通常の手番とまったく同じ(`advanceToNextTurn`による防御の解除・手番開始のTP、`endTurn`による残り距離のリセット・`turnCount`+1・ブレイク状態/バフ・デバフ/CT/状態異常の経過)。`endTurn`では先制の印も外す。
- **取り消し**: 先制の手番が先頭に来た時点(手番開始のTP・防御の解除をする**前**。`cancelInvalidPreemptiveTurns`)で、その個体が戦闘不能・ブレイク中・行動不能の状態異常・使える先制スキルが無い(CT/コスト/対象)のどれかなら、先制の手番を行動順から外す。
  - 手番は使わない(TP・防御・距離・CTはそのまま)。
  - 外れた後の行動順は普通に決まる(行動ゲージが0のままなら、普通に手番が来る)。
  - 取り消したFPIDは`EndTurnUseCase`の戻り値(`EndTurnResult.cancelledPreemptive`)で呼び出し側に返す(戦闘フィールドにイベントは残さない)。
- **勝敗が決まったら**、予約と先制の印を破棄する(`advanceToNextTurn`)。
- 敵は先制を使わない。

## UI

- **味方カードのタップ**: 選択状態が`AttackTarget`/`SkillTarget`のときは対象選びだけに使い(その間は予約できない。`BattleUiState.reservableAllyIds`が空になる)、それ以外のときは予約できる個体なら予約する(確認も取り消しもない)。
- 敵・味方どちらの行動中でも、演出中でも予約できる。勝敗の表示中や詳細ポップアップを開いている間はできない。
- 予約できるカードには細いオレンジ(`TacticalPointOrange`)の枠(`BattleUnitUiModel.canReservePreemptive`)、予約済み・先制の手番待ちのカードには「先制待ち」(`isPreemptiveWaiting`)を出す。
- 予約したときのナレーションは出さない。

## 先制の手番の流れ(`BattleViewModel`)

1. 先制の手番が始まったら「Aの先制行動!」(`BattleNarration.PreemptiveAction`。演出中扱いで、ナレーション欄のタップで進められる)を出す。
2. 選択状態を自動で`SkillList`にして**先制スキルだけ**を一覧に出す(有効/無効の判定は通常と同じ)。
3. 攻撃・防御・撤退は無効、スキル一覧からはキャンセルできない(対象選びのキャンセルはスキル一覧に戻るだけ)。スキルを使えなかったとき(`null`)もスキル一覧へ戻る。
4. 同じ先制の手番に二重に出ないよう`announcedPreemptiveFpid`で目印を持つ(戦闘フィールドの更新は何度も流れてくるため)。
5. 取り消し(`EndTurnUseCase`の戻り値)のときは、次の手番に進む前に「〜は先制行動できなかった!」(`PreemptiveFailed`)を出す。

普段の手番のスキル一覧には、先制スキルも通常のスキルとして出る。
