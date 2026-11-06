---
paths:
  - "**/battle/**"
  - "**/domain/model/**"
  - "**/domain/usecase/**"
  - "**/data/repository/**"
  - "**/data/local/**"
  - "**/charactercreate/**"
---

# スキルシステム(Aスキル・Pスキル・CT・通常攻撃)

キャラクターには、戦闘中に自分で使うアクティブスキル(Aスキル)と、設定しているだけで常に効果を発揮するパッシブスキル(Pスキル)を設定できる。

- **モデル**: `domain/model/skill/`
- **効果の適用**: `domain/battle/`(使用の入口は`SkillUse.kt`、CTは`SkillCooldownFormula.kt`、Pスキルは`PassiveSkillEffects.kt`)
- **マスターデータ**: `SkillRepositoryImpl`(雛形はKDoc)。個々のスキルの内容は`skill-catalog.md`
- **関連**: 対象の決め方は`formation-and-targeting.md`、ダメージの計算順は`damage-pipeline.md`、TPコストの代替は`tp.md`

## スキルID

- `Skill.id`。マスターデータのID(敵のEIDと同じ位置づけ)で、AスキルとPスキルはそれぞれ独立して採番する(一覧は`skill-catalog.md`)。
- 同じ数値でもAスキル1とPスキル1は別のスキルであり、**IDは必ず種別(A/P)とセットで扱う**。種別を決めずにIDだけでスキルを探さないこと。
- 戦闘中の個体を識別するFPIDとも別物。
- 保存: Aスキルは`Character.activeSkillIds`、Pスキルは`passiveSkillIds`に分けて保存する(`characters.json`にキーがない旧データは空リスト、存在しないIDは解決時に無視)。

## スキル一覧(正本)とテンプレート

- スキルの正本は`data/スキル一覧.xlsx`(Aスキルは属性ごとのシート、Pスキル、項目の定義など)。
- `ActiveSkill`はシートの列と1対1で対応する:
  - ID、名前
  - 属性`SkillElement`(シートのタブ)
  - スキルタイプ`SkillCategory`(物理型攻撃/特殊型攻撃/回復/変化/バフ/デバフ/ポジティブエフェクト/ネガティブエフェクト/トラップ。複数可・空もあり)
  - 物理属性`PhysicalAttribute`(衝撃/斬撃/刺突)
  - コストタイプ+コスト消費`costs: List<SkillCost>`(MP/HP/TP。例: MP30&HP10)
  - CT`coolTime`
  - 接触`SkillContactType`
  - レンジ`SkillRange`、範囲`SkillArea`、対象選択`SkillTargetSelection`
  - 基礎威力`basePower`
  - 発動回数`activationCount`(旧称「攻撃回数」)
  - 命中率`accuracy`(0〜100。空欄=`null`は必中)
  - 効果`effects`
- `PassiveSkill`はID、名前、スキルタイプ、効果。
- **スキルを追加するときは、`SkillRepositoryImpl`のKDocにある「シートの1行をコードへ書き写す雛形」に従う。**

### シートに無い項目

- `ActiveSkill.targetSide`(`SkillTargetSide`: 敵/味方): シートに列が無いが、狙う陣営を表すために暫定で持つ。
- `ActiveSkill.preemptivePriority`(`Int?`): 列が無い。`null`が通常のスキル、値ありが先制行動スキル(数値が大きいほど優先。`preemptive.md`)。

### 戦闘で使われる項目

基礎威力・発動回数・命中率・コスト・CT・効果・レンジ・範囲・対象選択・スキルタイプ(特殊型攻撃かどうか)。

## スロットと戦闘での解決

- **スロット数**: Aスキル・Pスキルともに最大8個(暫定。`SkillLimits`)。同じスキルの重複設定は不可。キャラ編集画面(`CharacterCreateViewModel`)で追加・削除する。
- **戦闘での解決**: `GetBattleFieldUseCase`がスキルIDを解決して`BattleAlly.activeSkills` / `passiveSkills`に持たせる(FPIDの採番には影響しない)。`BattleFieldPawn.passiveSkills`は敵では空。

## 通常攻撃(Aスキル1)

- 全キャラクター・全敵が標準で持つ特別なスキルで、スキル枠を使わず、キャラ編集画面の一覧にもバトルの「スキル」一覧にも出ない(`SkillRepository.getNormalAttack()`で取得し、`getActiveSkills()`/`getActiveSkill(id)`には含まれない)。
- 味方の通常攻撃と自動の手番は`AttackUseCase`/`ExecuteAutoTurnUseCase`がこのデータを`performAttack`へ渡して実行する。
- **画面での使い方**: 専用のボタンは無い。「スキル」ボタンでスキル一覧を開き(選択状態`SkillList`)、スキルを選ばずに通常攻撃で選べる相手(`BattleUiState.attackTargetIds`。点滅する太い枠が付く)をタップすると発動する。一覧中のナレーションは「スキルを選択してください(対象をタップで通常攻撃)」。先制の手番のスキル一覧では使えない(`preemptive.md`)。
- 「スキル」ボタンは、Aスキルを1つ以上持つ(使えるかは問わない)か、先制の手番でなく通常攻撃できる相手がいれば押せる。Aスキルが0個でも押せて、一覧は空のまま表示する。
- 対象は`resolveTargets`で決め(範囲攻撃なら対象ごとに)、威力は基礎威力、発動回数の分だけ1回ずつ命中判定・ダメージ計算・軽減を行い(命中率は100)、対象が戦闘不能になったらその対象への攻撃を打ち切る。
- 処理順は回数優先(1回目を全対象 → 2回目を全対象 …)で、結果の`AttackHit`・TPの対象は`resolveTargets`の順に組み立てる。
- CT0なのでCTの影響を受けない。

## Aスキルの使用

入口は`BattleField.performActiveSkill`(`UseSkillUseCase`経由。結果は`SkillUseResult`の`Attack`/`Effect`)。

| 基礎威力 | 渡す先 | 内容 |
|---|---|---|
| あり | `performAttack` | 通常攻撃と同じ計算(`damage-pipeline.md`)。味方ならコストの確認と消費も行う |
| なし | `performSkill` | 下記 |

### `performSkill`

- **成立条件**(1つでも欠けたら`null`): 使用者が現在の行動者の味方 / 戦闘不能でない / スキルを設定済み / コストを払える / 選んだ対象が選べる相手(`selectableTargets`)。
  - コスト: MP・HP・TP。HPコストは支払い後にHPが1以上残る場合のみ。TPは足りない分をHPで肩代わりでき、HPコストとの合計でHPが1以上残る場合のみ(`tp.md`の「代替システム」)。
  - ブレイク中・行動不能の状態異常中も`null`(`break.md`・`status-ailments.md`)。挑発中も`null`(洗脳・発狂で自動で動いている間を除く。`mental-ailments.md`)。CT中も`null`(下記)。
- **処理**: コスト消費と、`resolveTargets`で決めた全対象への効果適用。
- **命中判定**: 命中率が`null`でないスキルは対象ごとに1回だけ命中判定をし(`HitChanceFormula`。使用者の命中率・対象の回避率、対象がブレイク中なら必中)、外れた対象にはそのスキルの効果が一切かからない。命中率`null`は乱数を引かず必ず成功する。コストは外れても消費する。
- **発動回数**: 発動回数の分だけ「命中判定 → 属性耐性 → 効果ごとの成功判定 → 効果」を繰り返す(回数優先。戦闘不能になった対象は以降打ち切り、反射で使用者が戦闘不能なら全て打ち切り)。
- クリティカル判定はしない(ダメージが無いため)。
- 手番の終了は通常攻撃と同じく呼び出し側が`EndTurnUseCase`で行う。
- 対象は必ずFPIDで指定する。

## CT(クールタイム)

全スキル共通の仕組み。**唯一の実装箇所**: `domain/battle/SkillCooldownFormula.kt`

- **値**: `BattleFieldPawn.skillCooldowns`(`SkillCooldown`: スキルID・残り・手番中に作られたか)に持つ戦闘中だけの値。`Character`・`characters.json`には持たず、次の戦闘へは持ち越さない(`createNextBattleField`で空から始まる)。
- **設定**: `coolTime > 0`のスキルを使ったら(外れても)設定する(`performAttack`/`performSkill`が`startCooldown`)。
- **減り方**: **本人の手番が終わるたびに1減り、使った手番の終わりでは減らない**。バフ・デバフの`grantedDuringOwnTurn`と同じ考え方で、使った手番に作られた記録は終了時にフラグを下ろすだけ(これがないとCT1が使った手番の終わりに明けてしまう)。よってCT1は「使った後、次の自分の手番が終わるまで使えない」。数えるのは`endTurn`(`progressCooldowns`)で、先制の手番も1手番として数える。
- **CT中**: `performAttack`/`performSkill`が`null`を返し、スキル一覧では無効(`isUsable=false`)。
- **表示**: スキル一覧では、CT中のスキルのコストの後ろに「CT残り1」を付ける(`BattleSkillUiModel.remainingCoolTime`に`remainingTurns`をそのまま入れ、Screenは整形するだけ。例: 「クイックシュート　MP20・TP10　CT残り1」)。

## Pスキルの適用

- ダメージ軽減は`domain/battle/PassiveSkillEffects.kt`の`reduceDamage`が唯一の適用箇所。
- `performAttack`が1ヒットごとに、対象のPスキルの軽減合計を適用する(計算順の位置は`damage-pipeline.md`)。軽減が1以上あるときは`max(ダメージ - 軽減合計, 1)`。

## MP

- 最大MPは`Character.mana`。戦闘開始時は満タンで、`BattleAlly.currentMp`がスキル使用で減る。
- 次の戦闘へは現在HPと同様に持ち越す(`createNextBattleField`)。
- 敵も`BattleEnemy.currentMp`(開始時`enemy.mana`)を持つが、スキルのコストには使わない(汚染・呪怨で減るだけ。`status-ailments.md`)。

## 特殊型攻撃

- スキルタイプに特殊型攻撃を含み、物理型攻撃を含まないスキル(`ActiveSkill.usesSpecialDamage`)は、`performAttack`が`DamageFormula.calculate`へ破壊力・耐久力の代わりに`effectiveStat(MANA_OUTPUT)`(C)・`effectiveStat(MANA_RESISTANCE)`(D)を渡す(式は同じ)。
- 物理型攻撃と併せ持つスキル、特殊型を含まないスキルは今までどおり破壊力・耐久力。
