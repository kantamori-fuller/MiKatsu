---
paths:
  - "**/battle/**"
  - "**/domain/model/**"
  - "**/domain/usecase/**"
  - "**/data/repository/**"
  - "**/data/local/**"
  - "**/charactercreate/**"
---

# バトルのターンシステム

崩壊スターレイル方式の「素早さで行動順が決まる個別ターン制」。フィールド全体のターン数はなく、各個体が自分の手番を持つ。

- **唯一の実装箇所**: `domain/battle/ActionTimeline.kt`・`ActionSpeedFormula.kt`
- **関連**: 先制の手番の割り込みは`preemptive.md`、手番終了時の状態異常は`status-ailments.md`、手番終了時に経過するものの一覧は下の「手番の開始・終了で起きること」

## 仕様

- **remainingDistance と速度の式**: 各個体は`remainingDistance`(次の手番までに進む残り距離。開始時・行動後は`BASE_DISTANCE = 10000.0`)を持つ。速度は`素早さ^0.387`(`ActionSpeedFormula.speedOf`、素早さは1〜9999に丸める)で、素早さ6倍なら行動回数は約2倍、3倍なら約1.53倍になる。素早さは`effectiveStat(AGILITY)`(バフ・デバフ・麻痺の補正込み)。
- **次の手番までの時間は保存しない**: `remainingDistance / speed`を毎回計算で求める。将来バフ/デバフで素早さが変わっても、進んだ距離は保たれたまま残り時間だけが即座に計算し直され、行動順に反映されるため。
- **同時に手番が来たとき**: 幸運値(`luck`)が高い順(敵味方の区別なし)、同値ならランダム。決めた順番は`BattleField.turnQueue`に保存して確定し、計算し直してもくじは引き直さない(幸運が変わっても引き直さない)。先頭が現在の行動者(`currentActorFpid`)。
- **turnCount**: 個体が終えた自分の手番の回数。ステータスのバフ・デバフの持続は、この手番終了のタイミングで数える(`stat-modifiers.md`)。
- 戦闘不能(HP 0)の個体は行動順から外れ、攻撃対象にも選べない。敵が全滅で勝利、味方が全滅で敗北(`BattleField.outcome`)。

## 手番の開始・終了で起きること(各節に散らばっている処理の早見表)

各処理の詳細は括弧内のファイルが正。

- **手番開始**(`advanceToNextTurn`が行動者を決めたとき)
  - 先制の手番なら、まず無効な先制の手番を取り消す(`cancelInvalidPreemptiveTurns`。`preemptive.md`)
  - 防御の解除(`defense.md`)
  - TP+6〜8(条件3。ブレイクで飛ばされる手番でも受け取る。同時手番のくじの後に引く。`tp.md`)
  - 勝敗が決まったら予約と先制の印を破棄(`preemptive.md`)
- **手番終了**(`endTurn`、呼び出し側は`EndTurnUseCase`)
  - 残り距離のリセット・`turnCount`+1・先制の印を外す
  - ブレイク状態の経過(`progressBreakState`。`break.md`)
  - バフ・デバフの残りターン減算(`progressStatModifiers`。`stat-modifiers.md`)
  - CTの減算(`progressCooldowns`。`skills.md`)
  - 状態異常のダメージ・減算(`processTurnEndAilments`。他の処理の後・先制の割り込みの前。`status-ailments.md`)
  - 予約の割り込み(`applyPreemptiveReservations`。`advanceToNextTurn`の前。`preemptive.md`)
  - いずれも「本人の手番中に作られた記録」(`grantedDuringOwnTurn`等)はその手番の終了では減らさない
- **戻り値**: `EndTurnUseCase`は`EndTurnResult`(`cancelledPreemptive`と`events`)を返す。
- **自動で飛ばされる手番**: ブレイク中・行動不能の状態異常中は`BattleViewModel`が自動で飛ばし、ナレーションを出して`EndTurnUseCase`で終える(飛ばされた手番も手番として数える)。
