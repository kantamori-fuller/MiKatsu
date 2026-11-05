---
paths:
  - "**/battle/**"
  - "**/domain/model/**"
  - "**/domain/usecase/**"
  - "**/data/repository/**"
  - "**/data/local/**"
  - "**/charactercreate/**"
---

# 精神系の状態異常(洗脳・発狂・魅了・恐怖・挑発)

行動を変える5種類の状態異常。仕組みは状態異常(`status-ailments.md`)の一部で、モデル・付与・解除はそちらに従い、このファイルは精神系だけの仕様をまとめる。

- **モデル**: `StatusAilment`の末尾に強さの順で`BRAINWASH`(洗脳)・`MADNESS`(発狂)・`CHARM`(魅了)・`FEAR`(恐怖)・`TAUNT`(挑発)。enumの宣言順が強さ・表示・手番終了時の処理の順
- **実装箇所**: 付与の排他・恐怖の判定は`StatusAilmentFormula.kt`、対象の変化は`SkillTargeting.kt`の`selectableTargets`/`resolveTargets`、自動行動は`AutoTurn.kt`(`performAutoTurn`)と`ExecuteAutoTurnUseCase`、何もできない手番の判定は`ManualAction.kt`(`hasAnyManualAction`)
- **関連**: 状態異常の共通ルールは`status-ailments.md`、手番の進行は`turn-system.md`、先制行動は`preemptive.md`、対象は`formation-and-targeting.md`、防御は`defense.md`、スキルの使用条件は`skills.md`、文は`narration.md`

## 共通

- 手番終了時のダメージなどはなく、手番終了で残りターンが減って0で解除されるだけ(解除の文は「Bの洗脳が治った」の形)。
- ダメージを受けても解けない。石化中はかからず、石化をかけると消える。戦闘不能で全解除(`status-ailments.md`のまま)。
- 陣営・勝敗の判定・TPの獲得条件・他の個体から狙えるかどうかは変わらない。
- 付与の文は「Bは洗脳された!」「Bは発狂した!」「Bは魅了された!」「Bは恐怖に陥った!」「Bは挑発された!」。失敗は今の失敗の文。
- カードのラベル・詳細ポップアップの一覧は「洗脳」「発狂」「魅了」「恐怖」「挑発」。

## 精神系どうしの排他(`inflictAilment`)

強さは 洗脳 > 発狂 > 魅了 > 恐怖 > 挑発。

- **同じ種類**: 残りターンの長い方で上書き(今のルール)。
- **自分より強い精神系がかかっている**: 失敗(効かなかった扱い。命中扱いなのでTP条件は変わらない)。
- **自分より弱い精神系がかかっている**: 弱い方を消して付与する(消えたことの文は出さない)。
- 精神系でない状態異常とは共存する。

## 行動を決める優先順位

行動不能・ブレイク(今の仕組み) > 発狂 > 洗脳 > (敵) > 手動操作。発狂・洗脳で自動で動いている間は、魅了・挑発の制限は効かない(`BattleFieldPawn.isAutoActing`・`isRestrictedToAttackSkills`)。

`BattleViewModel.startAutoTurnIfNeeded`の判定順は「ブレイク → 行動不能(恐怖のスタンは専用の文) → 先制の手番 → 発狂 → 洗脳 → 敵 → 味方の手動(何もできなければ自動で飛ばす)」。

## 洗脳(BRAINWASH)

- 味方がかかったら操作できなくなり、敵のCPUと同じく**通常攻撃だけ**を自動で使う。敵がかかったら、ほかの敵を同じように通常攻撃する。
- 狙うのは**自分の陣営のうち自分以外**で、通常攻撃のレンジが届く生存個体(同じ陣営どうしの距離は`distanceBetween`の式=深さの差)。候補から選ぶのは`EnemyTargetSelector`(敵のCPUと同じ)。
- 届く相手がいなければ何もせず、「届かない」(`EnemyCannotReach`)を出して手番を終える。
- 手番の文: 前置き「Aは洗脳されている!」→ 攻撃の結果(または届かない文)。

## 発狂(MADNESS)

- 操作できなくなり、自動で動く。手番で乱数を次の順に引く(`performAutoTurn`)。
  1. `nextInt(100) < 60`なら何もしない。
  2. スキルを選ぶ: 通常攻撃 + 設定済みのAスキルのうち**今使えるもの**(`isSkillUsable`: CTが明けている・`canAfford`(封印を含む)・下の発狂用の対象選びで選べる相手が1体以上)。等確率で`nextInt(候補数)`。敵は通常攻撃だけ。コストは普通に払う。候補が空なら何もしない(乱数は引かない)。
  3. 相手を選ぶ: **陣営を無視**して、そのスキルのレンジが届く生存個体から等確率で`nextInt(候補数)`。**自分も含む**。レンジ0は自分だけ。範囲スキルは選んだ1体をアンカーにして`resolveTargets`のルールで広げる。
- 実行は基礎威力ありなら`performAttack`、なしなら`performSkill`(命中・耐性・TPなどは今までどおり)。
- 手番の文: 前置き「Aは発狂している!」→ スキル/攻撃の結果。何もしないときは「しかしAは何もしなかった!」。敵も同じ。

## 魅了(CHARM)

- 自分から見て**反対の陣営の個体を対象に選べない**(範囲が`FIELD`のスキルでも反対陣営は選べない・巻き込まない)。
- その結果、通常攻撃・敵向けのスキルは使えなくなる(攻撃ボタン・スキル一覧が無効)。味方向けのスキル・防御は使える。
- 敵がかかったら通常攻撃しかないので何もできず、「Aは何もできない!」を出して手番を終える。

## 恐怖(FEAR)

- **自分の手番の開始時**(`advanceToNextTurnWithCancellations`の両方の分岐で、防御の解除・手番開始のTPの**後**)に、`nextInt(100) < 30`ならスタンする(`rollFearAtTurnStart`)。乱数はTPの乱数の後に1回。
- 戦闘不能・ブレイク中・すでに行動不能の状態異常のときは判定しない(乱数も引かない)。先制の手番でも判定する(先制の取り消しの判定はその前に済んでいるので取り消さず、スタンとして手番を飛ばす)。
- スタンは**その手番だけ**: `AilmentState(STUN, remainingTurns = 1, grantedDuringOwnTurn = false, causedByFear = true)`。その手番の終了で減って解除される(`grantedDuringOwnTurn = true`にすると2手番動けなくなる)。このスタンの解除の文は出さない。
- 恐怖由来かは`AilmentState.causedByFear`で持つ(ViewModelは推測しない)。手番を飛ばす文は「Aは恐怖で体がすくんだ!」(通常のスタンの「Aはスタンで動けない!」の代わり)。
- 恐怖そのものの残りターンは、普通に手番終了で減る。

## 挑発(TAUNT)

- **攻撃技(基礎威力が`null`でないスキル。通常攻撃を含む)しか使えない**: `performSkill`は`null`、スキル一覧でも無効(`isSkillUsable`)。
- **防御もできない**: `defend`は`null`、防御ボタンも無効(`BattleUiState.canDefend`)。
- 狙う相手の強制はない。敵がかかっても通常攻撃しかないので影響はない。

## 何もできない手番(汎用の仕組み)

- 手動で操作する味方の手番で、通常攻撃・使えるスキル・防御のどれも選べない(撤退は数えない)ときは、`BattleViewModel`が自動で「Aは何もできない!」を出して手番を飛ばす(手番として数える)。
- 判定は`BattleField.hasAnyManualAction(fpid, normalAttack)`の1か所。ViewModelは呼ぶだけ。

## 先制行動

- 洗脳・発狂の間は予約できない(`canActPreemptively`→`canReservePreemptive`)。すでに入っている先制の手番も、先頭に来たときに取り消す(`cancelInvalidPreemptiveTurns`)。
- 魅了・挑発は、使える先制スキル(`isSkillUsable`。魅了の対象の制限・挑発の攻撃技の制限を反映)が残っていれば予約できる。恐怖は予約できる。

## 操作の入口

- 洗脳・発狂の味方は手動で操作できない。プレイヤー向けのUseCase(`AttackUseCase`・`UseSkillUseCase`・`DefendUseCase`)は、行動者が洗脳・発狂なら何もしない(`null`/`false`)。
- 自動行動は`ExecuteAutoTurnUseCase`(旧`ExecuteEnemyTurnUseCase`)。敵・洗脳・発狂の手番を扱い、結果は`AutoTurnResult`(`Attacked`/`UsedSkill`/`MadnessIdle`/`CannotReach`/`NothingToDo`。手動の味方や行動不能では`null`)。乱数は注入された`Random`から引く。
- 対象の変化(洗脳=自分以外の自陣営、発狂=陣営無視で自分を含む、魅了=反対陣営を除く)は`selectableTargets`/`resolveTargets`が行動者の状態から自動で決める。`performAttack`/`performSkill`・UIの点滅枠はそのまま正しくなる。

## テスト用スキル

A10「テスト洗脳」〜A14「テスト挑発」(`skill-catalog.md`)で、それぞれ1種類だけをかけて動作を確認できる。
