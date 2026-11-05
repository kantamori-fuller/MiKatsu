# CLAUDE.md

本プロジェクト(MiKatsu)で開発を行う際にClaude Codeが従う方針の**目次**。本文は`.claude/rules/`にトピックごとに分けて置いている。

## 読み方

- `.claude/rules/`直下の全般ルール(アーキテクチャ・UI・遷移・KDoc)は、常に自動で読み込まれる。
- `.claude/rules/battle/`のバトル仕様は、バトル関連のファイル(`**/battle/**`・`domain/model`・`domain/usecase`・`data/repository`・`data/local`・`charactercreate`)を読んだときに自動で読み込まれる。
- **ファイルを開く前にバトルの設計・相談・指示書の作成をするときは、下の目次から該当するファイルを自分で`Read`してから答える。** 複数の仕組みにまたがる変更(ダメージ・手番・TPなど)は、関連欄に出てくるファイルもあわせて読む。
- 仕様を変えたら、このファイルではなく該当するルールファイルを更新する。新しい仕組みはファイルを新設し、目次に1行足す。各ルールファイルの冒頭にある`paths`は、バトル仕様なら他のファイルと同じものを付ける。

## 目次: 全般ルール(常に読み込み)

| ファイル | 内容 |
|---|---|
| [architecture.md](.claude/rules/architecture.md) | Clean Architecture・MVVM・SOLID・SSOT、モジュール構成と依存方向(`app → ui → domain ← data`)、Hilt、Repository |
| [ui.md](.claude/rules/ui.md) | Screen/ScreenContentの分け方、ViewModel、Previewの作り方、`strings.xml`、`ui/<screenName>/`の構成 |
| [navigation.md](.claude/rules/navigation.md) | Navigation 3、`NavKey`ルート、`<画面名>Navigation.kt`、`@AssistedInject`での引数渡し |
| [kdoc.md](.claude/rules/kdoc.md) | KDocの書き方(private関数も、`@param`/`@return`、目的まで書く) |

## 目次: バトル仕様(`.claude/rules/battle/`)

| ファイル | 内容 | 主な実装箇所 |
|---|---|---|
| [ids.md](.claude/rules/battle/ids.md) | ID / EID / FPIDの違いと使い分け。**対象指定は必ずFPID** | `GetBattleFieldUseCase` |
| [turn-system.md](.claude/rules/battle/turn-system.md) | 素早さによる個別ターン制、同時手番、**手番の開始・終了で起きることの早見表** | `ActionTimeline.kt`・`ActionSpeedFormula.kt` |
| [formation-and-targeting.md](.claude/rules/battle/formation-and-targeting.md) | 前衛/後衛・繰り上がり、レンジ・陣営・範囲×対象選択 | `SkillTargeting.kt` |
| [skills.md](.claude/rules/battle/skills.md) | スキルID(A/P別採番)、スキル一覧xlsxとの対応、通常攻撃、`performSkill`、CT、Pスキル、MP、特殊型攻撃 | `SkillUse.kt`・`SkillCooldownFormula.kt`・`PassiveSkillEffects.kt`・`SkillRepositoryImpl` |
| [skill-catalog.md](.claude/rules/battle/skill-catalog.md) | 実装済みスキルのID一覧と各スキルの仕様 | `SkillRepositoryImpl` |
| [damage-pipeline.md](.claude/rules/battle/damage-pipeline.md) | **ダメージ技の1ヒットの処理順・ダメージ補正の順序の正本** | `performAttack`(`SkillUse.kt`) |
| [hit-chance.md](.claude/rules/battle/hit-chance.md) | 命中率・回避率の段階と倍率表、必中 | `HitChanceFormula.kt` |
| [critical.md](.claude/rules/battle/critical.md) | クリティカル/バッドクリティカルの発生率・倍率・最終倍率での分類 | `CriticalFormula.kt` |
| [tp.md](.claude/rules/battle/tp.md) | TPの獲得条件1〜8・乱数の順序、TP不足をHPで払う代替システム | `TacticalPointFormula.kt` |
| [break.md](.claude/rules/battle/break.md) | ブレイクゲージ、ブレイク中の効果、立ち上がり | `BreakFormula.kt` |
| [defense.md](.claude/rules/battle/defense.md) | 防御の成立条件・効果・解除 | `DefenseFormula.kt` |
| [element-resistance.md](.claude/rules/battle/element-resistance.md) | 12属性の耐性、倍率/無効/吸収/反射 | `ElementResistanceFormula.kt` |
| [stat-modifiers.md](.claude/rules/battle/stat-modifiers.md) | ステータスのバフ・デバフ(段階・持続・永続)、`effectiveStat`の使い方 | `StatStageFormula.kt` |
| [preemptive.md](.claude/rules/battle/preemptive.md) | 先制行動の予約・割り込み・取り消し・UI | `PreemptiveAction.kt`・`ActionTimeline.kt` |
| [status-ailments.md](.claude/rules/battle/status-ailments.md) | 状態異常の付与・成功率・手番終了時のダメージ・行動不能・各効果 | `StatusAilmentFormula.kt` |
| [mental-ailments.md](.claude/rules/battle/mental-ailments.md) | 精神系の状態異常(洗脳・発狂・魅了・恐怖・挑発)。強さによる排他、自動行動、対象の変化、恐怖のスタン、挑発の制限、何もできない手番 | `StatusAilmentFormula.kt`・`AutoTurn.kt`・`SkillTargeting.kt`・`ManualAction.kt` |
| [narration.md](.claude/rules/battle/narration.md) | 攻撃のナレーションの形、タップで進める、ナレーションと表示の連動 | `AttackNarrations.kt`・`BattleViewModel` |
| [unit-detail-popup.md](.claude/rules/battle/unit-detail-popup.md) | カード長押しの詳細ポップアップの開閉・表示内容 | `UnitDetailPopup.kt` |

## 迷ったときの索引

| やりたいこと | 読むファイル |
|---|---|
| 新しいスキルを追加する | `skills.md` → `skill-catalog.md`(+効果に応じて`stat-modifiers.md`/`status-ailments.md`など) |
| ダメージに補正を足す | `damage-pipeline.md` → 該当する仕組みのファイル |
| 手番の開始・終了に処理を足す | `turn-system.md`(早見表) → `preemptive.md` |
| 戦闘中だけの値を足す | `break.md`・`stat-modifiers.md`の「戦闘中だけの値」(`Character`・`characters.json`に持たない、`createNextBattleField`で持ち越すかを決める) |
| 戦闘画面の表示・文言を変える | `ui.md` → `narration.md`・`unit-detail-popup.md` |
| 乱数を引く順番が絡む | `tp.md`の「乱数の順序」・`status-ailments.md`の「成功率」・`preemptive.md`の「並び順」・`mental-ailments.md`(発狂・恐怖) |

## 次の戦闘への持ち越し早見表(`createNextBattleField`)

| 持ち越す | 持ち越さない(戦闘ごとに初期値) |
|---|---|
| 現在HP・現在MP・TP | CT・ブレイク(ゲージ・耐性・状態)・バフ・デバフ(永続を含む)・防御・先制の予約・状態異常 |

冒険の最初の戦闘(`GetBattleFieldUseCase.invoke`)ではTPは0、MPは満タンから始まる。敵のTPは常に0から。
