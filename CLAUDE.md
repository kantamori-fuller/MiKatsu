# CLAUDE.md

このファイルは、本プロジェクト(MiKatsu)で開発を行う際にClaude Codeが従うべき方針を定義する。

## アーキテクチャ原則

本プロジェクトは以下の原則に厳密に従って実装する。

- **Clean Architecture**: UI層・ドメイン層・データ層を分離し、依存の方向は常に外側(UI)から内側(データ)への一方向のみとする。内側の層は外側の層を一切参照しない。
- **MVVM**: View(Screen) - ViewModel - Model(Repository/UseCase)の構成に従う。
- **SOLID**: 各クラス・関数は単一責任の原則(SRP)を満たし、具象ではなく抽象(インターフェース)に依存する(DIP)。
- **SSOT (Single Source of Truth)**: あるデータの正は必ず1箇所にのみ存在させる。データの正はRepositoryが保持し、ViewModelはRepositoryから取得したデータをもとにUiStateを構築して公開する。UI(Screen)側やViewModel側でデータを独自に複製・キャッシュしない。

## Screen / ViewModel

- 1つのScreen(Composable関数)に対して、原則として1つのViewModelを対応させる。
- ViewModelは画面の状態を`UiState`として`StateFlow`等で公開し、Screen(Composable)はそれを購読して描画に専念する。Screen側にビジネスロジック・データ加工処理を書かない。
- ユーザー操作(ボタン押下、入力変更など)はイベントとしてScreenからViewModelへ通知し、ViewModelがイベントを処理してUiStateを更新する。
- ViewModelはAndroidのUI要素(Context、Composable等)に直接依存しない。

## 文字列リソース

- 画面に表示する文言(ボタンのラベル、タイトル、`contentDescription`、ダイアログの本文など)をComposable内やKotlinコードに直接ハードコードしない。必ず`strings.xml`に定義し、`stringResource(R.string.xxx)`経由で参照する。
- 文字列リソースは、その文言を使うScreen(Composable)が属するモジュールの`res/values/strings.xml`に置く(現状は`ui`モジュール)。
- 複数の画面・コンポーネントで全く同じ文言を使う場合(例: 「戻る」「削除」)は、文言ごとに1つの文字列リソースを定義し、使い回す。同じ意味の文言を複数の名前で重複定義しない。

## データ層 / Repository

- データアクセス(ローカル永続化、DB、通信など)は必ずRepositoryを経由し、ViewModelやUseCaseがファイルI/O・SharedPreferences・DB等を直接操作しない。
- Repositoryはインターフェースと実装を分離する。ViewModel(またはUseCase)はRepositoryのインターフェースにのみ依存し、具象実装を知らない。

## 画面遷移

- 画面遷移にはNavigation 3(`androidx.navigation3`)を用いる。Navigation Compose(2系/`NavHost`+`NavController`)や、sealed classの手動切り替えなど、自前の画面状態管理による遷移は行わない。
- バックスタックは`rememberNavBackStack`で生成した単純な`NavKey`の`List`としてNavHost(集約点)側が直接所有し、画面遷移は`backStack.add(...)` / `backStack.removeLastOrNull()`で行う。
- ルートはKotlin Serialization(`@Serializable`)を付与し、`NavKey`インターフェースを実装したクラス/オブジェクトとして型安全に定義する。文字列ルートによる手動の引数定義は行わない。
- ナビゲーションを1つのファイルに一括管理しない。各画面が自分自身のルート定義・登録関数を`<画面名>Navigation.kt`として個別に持つ(詳細は「uiモジュールのディレクトリ構成」を参照)。登録関数は`EntryProviderScope<NavKey>`への拡張関数とし、内部で`entry<ルート型> { ... }`を呼び出す。
- アプリ全体のNavHostは、各画面の`<画面名>Navigation.kt`が公開する`EntryProviderScope`拡張関数を呼び出して`entryProvider`を組み立て、`NavDisplay`へ渡すだけの薄い集約点とする。ルートの定義や画面遷移先の詳細をNavHost自身に書かない。
- ルートが持つ値(例: 編集対象のID)をViewModelへ渡す場合は、Hiltの`@AssistedFactory`/`@AssistedInject`を用いる。Navigation 3はNav2と異なりバックスタックのエントリーにナビゲーション引数用の`SavedStateHandle`を自動で紐付けないため、`SavedStateHandle`経由での引数取得は行わない。

## uiモジュールのディレクトリ構成

`ui`モジュールは画面(Screen)単位のディレクトリで構成する。各画面ディレクトリは以下の形を基本とする。

```
ui/<screenName>/
    model/                       画面固有のUiStateなどのモデル
    components/                  画面固有の再利用可能なComposable部品
    <ScreenName>Screen.kt        画面本体のComposable
    <ScreenName>ViewModel.kt     画面に対応するViewModel
    <ScreenName>Navigation.kt    画面のルート定義・EntryProviderScope拡張(entry登録)
```

- 複数画面から共有されるNavHostなど、特定の画面に属さないものだけを`ui`直下に置く。

## モジュール構成

本プロジェクトは以下のマルチモジュール構成で実装する。

- **app**: アプリケーションのエントリポイント。Compositionルートとして、`domain`のRepositoryインターフェースに対する`data`の実装を組み立て、ViewModelへ注入する。`MainActivity`とNavHostの起動処理を持つ。
- **core**: 特定の機能に依存しない共通コード(デザインシステム/テーマ、共通拡張関数など)を置く。`domain`・`data`・`ui`には依存しない。
- **domain**: ドメインモデル、Repositoryのインターフェース、UseCaseを置く。Androidフレームワークに依存しない純粋なKotlinモジュールとする。他のどのモジュールにも依存しない。
- **data**: `domain`で定義したRepositoryインターフェースの実装、ローカル/リモートのデータソースを置く。`domain`にのみ依存する。
- **ui**: Screen(Composable)とViewModelを置く。`domain`(UseCase経由)と`core`にのみ依存し、`data`には直接依存しない(DIPを遵守し、具象実装を知らない状態を保つ)。

依存の方向は常に `app → ui → domain ← data` および `app → data`、`app/ui → core` の一方向とし、内側のモジュール(`domain`)が外側のモジュールを参照することはない。

## DI (Dependency Injection)

- 依存性注入にはDagger Hiltを用いる。ViewModelやRepository実装のインスタンス化・注入を手動のFactory/Compositionルートで行わない。
- `domain`モジュールはHiltを含むいかなるDIフレームワークにも依存しない(純粋なKotlinモジュールを維持するため)。UseCaseのコンストラクタにDIアノテーションを付与しない。
- Repositoryインターフェースへの実装の束縛(`@Binds`)、およびUseCaseの提供(`@Provides`)は`data`モジュールの`di`パッケージに置く。
- ViewModelは`@HiltViewModel`を付与し、コンストラクタインジェクションで必要なUseCaseを受け取る。
- `app`モジュールの`Application`クラスに`@HiltAndroidApp`、`MainActivity`に`@AndroidEntryPoint`を付与する。
- Compose画面でのViewModel取得には`hiltViewModel()`を用いる。

## バトルシステムのID体系(ID / EID / FPID)

バトル関連の実装には目的の異なる3種類のIDが登場する。今後バトルロジック(ダメージ計算・対象指定など)を実装する際は、これらを混同しないこと。

- **ID(キャラクターID)**: `Character.id`。端末内に保存された味方キャラクター自身の永続的な識別子。保存時に一度だけ採番され、削除されても再利用されない(`CharacterRepository`/`CharacterLocalDataSource`が管理)。バトルの有無に関わらずキャラクターそのものを指す、恒久的なID。

- **EID(Enemy ID)**: `Enemy.id`。敵の「種族(マスターデータ)」を識別するID。`EnemyRepositoryImpl`が保持する固定データに紐づく(現状: ゴブリン=EID1、オーク=EID2)。同じEIDの敵がフィールド上に複数体登場し得るため、EIDだけでは個体を区別できない。

- **FPID(Field Pawn ID / フィールドポーンID)**: `BattleFieldPawn.fpid`。ある1回の戦闘限りで、フィールド上に存在する個体(味方・敵の両方)を一意に識別するID。`GetBattleFieldUseCase`が味方→敵の順に単一のカウンターで採番し、味方と敵の間、および同じ敵種同士でも重複しない。戦闘が終われば失われる、永続化しないその場限りの識別子。

### 使い分けの指針

- キャラクターの永続データを扱う(一覧表示・編集・保存・削除など) → `Character.id`
- 敵の種族・ステータスのマスターデータを扱う → `Enemy.id`(EID)
- バトル中に「誰が」「誰に」効果を及ぼすかを扱う(攻撃対象の指定、ダメージの適用先、状態異常の対象など) → 必ずFPIDを使う。`Character.id`やEIDで対象を指定すると、同じキャラクター・同じ敵種がフィールド上に複数体登場した際に対象を一意に特定できず、意図しない相手に効果が及ぶ不具合の原因になる。

### 実装上の構造

- `BattleFieldPawn`: `fpid`プロパティを持つ、味方・敵共通のsealed interface。
- `BattleAlly(fpid, character)` / `BattleEnemy(fpid, enemy)`: それぞれ味方・敵の1個体を表すモデル。
- `BattleField(allies, enemies)`: 味方陣営・敵陣営をまとめた集約。`allPawns`で陣営を問わず全個体を横断的に扱える。
- `GetBattleFieldUseCase`: FPIDを割り当てて`BattleField`を組み立てる、採番ロジックの唯一の実装箇所。採番方法(採番順・敵の出現数など)を変える場合はここだけを変更すればよい。
- `BattleUnitUiModel.id`(UI層): 表示上のIDとして常にFPIDを使用する(`Character.id`やEIDをそのまま転用しない)。

例: ゴブリンx2・オークx2・味方2名の場合 → 味方がFPID=1,2、ゴブリンがFPID=3,4、オークがFPID=5,6となる(採番順は味方が先、敵は`EnemyRepositoryImpl`の登録順)。

## バトルのターンシステム

崩壊スターレイル方式の「素早さで行動順が決まる個別ターン制」。フィールド全体のターン数はなく、各個体が自分の手番を持つ。ロジックは`domain/battle/ActionTimeline.kt`・`ActionSpeedFormula.kt`に集約している。

- **remainingDistance と速度の式**: 各個体は`remainingDistance`(次の手番までに進む残り距離。開始時・行動後は`BASE_DISTANCE = 10000.0`)を持つ。速度は`素早さ^0.387`(`ActionSpeedFormula.speedOf`、素早さは1〜9999に丸める)で、素早さ6倍なら行動回数は約2倍、3倍なら約1.53倍になる。
- **次の手番までの時間は保存しない**: `remainingDistance / speed`を毎回計算で求める。将来バフ/デバフで素早さが変わっても、進んだ距離は保たれたまま残り時間だけが即座に計算し直され、行動順に反映されるため。
- **同時に手番が来たとき**: 幸運値(`luck`)が高い順(敵味方の区別なし)、同値ならランダム。決めた順番は`BattleField.turnQueue`に保存して確定し、計算し直してもくじは引き直さない。先頭が現在の行動者(`currentActorFpid`)。
- **turnCount**: 個体が終えた自分の手番の回数。ステータスのバフ・デバフの持続は、この手番終了のタイミングで数える(「ステータスのバフ・デバフ」を参照)。
- 戦闘不能(HP 0)の個体は行動順から外れ、攻撃対象にも選べない。敵が全滅で勝利、味方が全滅で敗北(`BattleField.outcome`)。

## スキルシステム

キャラクターには、戦闘中に自分で使うアクティブスキル(Aスキル)と、設定しているだけで常に効果を発揮するパッシブスキル(Pスキル)を設定できる。モデルは`domain/model/skill/`、効果の適用は`domain/battle/`に置く。

- **スキルID**: `Skill.id`。マスターデータのID(敵のEIDと同じ位置づけ)で、AスキルとPスキルはそれぞれ独立して採番する(通常攻撃=Aスキル1、ディア=Aスキル2、精神一到=Aスキル3、覇気=Aスキル4、研ぎ澄ます=Aスキル6、鋼の体=Pスキル1。Aスキル5(ローリング・ソバット、TPコスト)は未実装の欠番)。同じ数値でもAスキル1とPスキル1は別のスキルであり、IDは必ず種別(A/P)とセットで扱う。種別を決めずにIDだけでスキルを探さないこと。戦闘中の個体を識別するFPIDとも別物で、Aスキルは`Character.activeSkillIds`、Pスキルは`passiveSkillIds`に分けて保存する(`characters.json`にキーがない旧データは空リスト、存在しないIDは解決時に無視)。マスターデータは`SkillRepositoryImpl`が保持する。
- **スキル一覧とテンプレート**: スキルの正本は`data/スキル一覧.xlsx`(Aスキルは属性ごとのシート、Pスキル、項目の定義など)。`ActiveSkill`はシートの列と1対1で対応する(ID、名前、属性`SkillElement`(シートのタブ)、スキルタイプ`SkillCategory`(物理型攻撃/特殊型攻撃/回復/変化/バフ/デバフ/ポジティブエフェクト/ネガティブエフェクト/トラップ。複数可・空もあり)、物理属性`PhysicalAttribute`(衝撃/斬撃/刺突)、コストタイプ+コスト消費`costs: List<SkillCost>`(MP/HP。例: MP30&HP10)、CT`coolTime`、接触`SkillContactType`、レンジ`SkillRange`、範囲`SkillArea`、対象選択`SkillTargetSelection`、基礎威力`basePower`、発動回数`activationCount`(旧称「攻撃回数」)、命中率`accuracy`(0〜100。空欄=`null`は必中)、効果`effects`)。`PassiveSkill`はID、名前、スキルタイプ、効果。シートの1行をコードへ書き写す雛形は`SkillRepositoryImpl`のKDocにあるので、スキルを追加するときはそれに従う。
- **シートに無い項目**: `ActiveSkill.targetSide`(`SkillTargetSide`: 敵/味方)はシートに列が無いが、狙う陣営を表すために暫定で持つ。
- **データだけの項目**: CTは現状データとして持つだけで、クールタイムとしてはまだ効いていない。基礎威力・発動回数・命中率・コスト・効果・レンジ・範囲・対象選択は戦闘で使われる。
- **前衛/後衛と配置**: 味方・敵とも2行×3列(スロット0〜5)。`slot / 3`が0なら前衛、1なら後衛、列は`slot % 3`。味方は`Formation`のスロット(画面の上段=敵に近い側が前衛)、敵は`GetBattleFieldUseCase`が出現順にスロット0から詰めて割り当てる(画面では前衛を下側=味方側に表示)。陣営の前衛に生存個体がいなければ、その陣営の後衛は前衛として扱う(繰り上がり。`effectiveRowOf`)。
- **対象の決め方**: `domain/battle/SkillTargeting.kt`の`selectableTargets` / `resolveTargets`が唯一の判定箇所。
  - **レンジ0**: 使用者自身だけが対象(`SkillRange.RANGE_0`)。陣営・範囲・対象選択に関係なく、`selectableTargets`/`resolveTargets`は生存している使用者自身だけを返す。画面ではスキル一覧で押した瞬間に、対象選択を挟まず自分を対象に発動する。
  - **レンジ1〜3**: 味方後衛=0・味方前衛=1・敵前衛=2・敵後衛=3の一直線で、使用者と対象の有効な段の距離が`SkillRange.reach`以下なら選べる(反対陣営は`1+深さ+深さ`、同じ陣営は`|深さの差|`)。
  - **陣営**: `targetSide`(使用者から見た相対。ENEMY=反対陣営、ALLY=自分を含む自陣営)。範囲が`FIELD`のときだけ両陣営。
  - **範囲×対象選択**: タップした1体(アンカー)が選べる相手であること。一体選択(`ONE`)と`SINGLE`はアンカーのみ。全て対象(`ALL`)はアンカーの属するまとまりの生存個体全員(陣=同じ陣営の同じ有効な段、列=同じ陣営の同じ列、エリア全体=アンカーの陣営全員、フィールド全体=全員)で、まとまりの他のメンバーはレンジ外でも対象になる。
  - 選べる相手がいない行動は使えない(「攻撃」ボタン・スキル一覧が無効)。敵は届く相手がいなければ何もせず手番を終える。画面では選べる相手のカードに点滅する太い枠を付け、そのカードだけタップできる。
- **通常攻撃**: Aスキル1。全キャラクター・全敵が標準で持つ特別なスキルで、スキル枠を使わず、キャラ編集画面の一覧にもバトルの「スキル」一覧にも出ない(`SkillRepository.getNormalAttack()`で取得し、`getActiveSkills()`/`getActiveSkill(id)`には含まれない)。「攻撃」ボタンと敵の手番は`AttackUseCase`/`ExecuteEnemyTurnUseCase`がこのデータを`performAttack`へ渡して実行する。対象は`resolveTargets`で決め(範囲攻撃なら対象ごとに)、威力は基礎威力、発動回数の分だけ1回ずつ命中判定・ダメージ計算・軽減を行い(命中率は100)、対象が戦闘不能になったらその対象への攻撃を打ち切る。特殊型攻撃の計算式(C/Dを使う)は未実装。
- **スロット数**: Aスキル・Pスキルともに最大8個(暫定。`SkillLimits`)。同じスキルの重複設定は不可。キャラ編集画面(`CharacterCreateViewModel`)で追加・削除する。
- **戦闘での解決**: `GetBattleFieldUseCase`がスキルIDを解決して`BattleAlly.activeSkills` / `passiveSkills`に持たせる(FPIDの採番には影響しない)。`BattleFieldPawn.passiveSkills`は敵では空。
- **Aスキルの使用**: `BattleField.performSkill`(`UseSkillUseCase`経由)が唯一の適用箇所。使用者が現在の行動者の味方・戦闘不能でない・スキルを設定済み・コスト(MP・HP。HPコストは支払い後にHPが1以上残る場合のみ)を払える・選んだ対象が選べる相手(`selectableTargets`)のときだけ成立し、コスト消費と、`resolveTargets`で決めた全対象への効果適用を行う。命中率が`null`でないスキルは対象ごとに1回だけ命中判定をし(`HitChanceFormula`。使用者の命中率・対象の回避率、対象がブレイク中なら必中)、外れた対象にはそのスキルの効果が一切かからない。命中率`null`は乱数を引かず必ず成功する。コストは外れても消費する。発動回数が2以上でも効果の適用は1回(現状すべて1回)。現状はダメージなし(基礎威力なし)のスキルのみ使用できる。手番の終了は通常攻撃と同じく呼び出し側が`EndTurnUseCase`で行う。対象は必ずFPIDで指定する。
- **ディア**: 無属性(仮)/非接触/回復+変化/MP10/レンジ3・エリア全体・一体選択(仮)。自分を含む味方1体のHPを固定で50回復(最大HPで頭打ち、戦闘不能の味方は対象外で蘇生しない)。※スキル一覧ではレンジ1・基礎威力100・マナ出力で回復量が変動に更新されているが、回復量の式が未定のため、コードは未反映。
- **精神一到**: 無属性/バフ/非接触/MP20/レンジ0・単体・一体選択/命中率`null`。自分の破壊力を1段階上げる(10ターン)。
- **覇気**: 無属性/デバフ/非接触/MP30/レンジ3・単体・一体選択/命中率100。敵1体の敏捷と耐久力を1段階ずつ下げる(8ターン)。命中判定は1回で、両方かかるか両方外れるか。
- **研ぎ澄ます**: 無属性/バフ/非接触/HP30&MP50/レンジ0・単体・一体選択/命中率`null`。自分の命中率・クリティカル発生率・クリティカル倍率を1段階ずつ上げる(永続)。
- **Pスキルの適用**: ダメージ軽減は`domain/battle/PassiveSkillEffects.kt`の`reduceDamage`が唯一の適用箇所で、`performAttack`が`DamageFormula`の最終結果(+2の後)に、1ヒットごとに対象のPスキルの軽減合計を適用する。軽減が1以上あるときは`max(ダメージ - 軽減合計, 1)`。鋼の体は受けるダメージを20減らす(タイプは空)。
- **MP**: 最大MPは`Character.mana`。戦闘開始時は満タンで、`BattleAlly.currentMp`がスキル使用で減る。次の戦闘へは現在HPと同様に持ち越す(`createNextBattleField`)。敵のMPは現状使わない。

## 命中率・回避率

攻撃が当たるかどうかは、スキルの命中率と、攻撃者の命中率・対象の回避率の段階で決まる。計算と判定は`domain/battle/HitChanceFormula.kt`が唯一の実装箇所。

- **個体の命中率・回避率**: 数値は持たず、`BattleStat.ACCURACY` / `EVASION`の段階(-12〜+12)だけを持つ。仕組み(記録・持続・重ねがけ・±12・次の戦闘へ持ち越さない)は「ステータスのバフ・デバフ」と同じで、倍率表だけが違う。基本値`HitChanceFormula.BASE_RATE = 100`は詳細ポップアップの表示用で、計算には使わない。敏捷は命中・回避に影響しない。現状、命中率・回避率を変えるスキルは無い。
- **倍率表**: 段階nが n≥0 なら`(6+n)/6`、n<0 なら`6/(6+|n|)`(+1=7/6、+2=4/3、+3=3/2、+12=3、-1=6/7、-2=3/4、-3=2/3、-12=1/3)。基礎ステータス6種の表(4基準)とは別物で、`HitChanceFormula`に置く。
- **最終命中率**: 命中段階 = clamp(攻撃者の命中率の段階 − 対象の回避率の段階, -12, 12)。最終命中率 = `スキルの命中率 × 命中段階の倍率`。命中+1と回避+1は打ち消し合う。例えば命中90の技は、対象の回避率+1なら90×6/7≈77.1%、攻撃者の命中率+1なら100%(105の頭打ち)。結果は0〜100%に収め、端数は丸めずに小数のまま確率として使う。
- **必中**: スキルの命中率が`null`(シートで空欄)、または対象がブレイク中(被必中)なら、乱数を引かずに必ず命中する。味方へプラスの効果を与えるスキルなどは空欄にする(ディアは`null`)。
- **判定の単位**: `performAttack`が1ヒットごと・対象ごとに判定する。外れたヒットはダメージ0で、軽減(`reduceDamage`)も適用しないが、発動回数は1回消費する。結果は`AttackHit.shots`(`AttackShot`: 当たったかどうかとダメージ)に残る。全発外れた対象は`AttackHit.isMissed`。乱数は`Random`を引数で受け取る(DIは`UseCaseModule.provideRandom`)。
- **ダメージを与えないスキル**: `performSkill`も同じ式で、対象ごとに1回判定する(「Aスキルの使用」を参照)。
- **ナレーション**: `AttackResult.toNarrations`(`ui/battle/model/AttackNarrations.kt`)が、攻撃結果を1文ずつ表示するナレーションの列へ変換する。
  - 対象1体・1ヒット: 当たれば「AがBに攻撃! 30ダメージ」、外れれば「AがBに攻撃! しかし外れた!」。
  - 対象1体・2ヒット以上: 「1回目: 命中! 30ダメージ」「2回目: 外れた!」を1発ずつ表示し、最後に「AがBに合計30ダメージ(2回中1回命中)」。全発外れたら「しかし外れた!」の1文だけ。
  - 範囲攻撃: 1ヒットごとの表示はしない。「Aの攻撃!」→ 全発外れた相手がいれば「BとCには外れた!」→ 命中した相手は「Dに30、Eに25ダメージ!」。全員に外れたら「Aの攻撃! しかし外れた!」の1文だけ。
  - 演出中のナレーションは、どれもナレーション欄のタップ(`BattleViewModel.onNarrationClick`)で次の文へ進められる。1回のタップで進むのは1文だけで、待っていない間のタップは捨てる。HPは攻撃を実行した時点で反映され、ナレーションは後から流れる。

## クリティカルヒット

命中したヒットごとに、クリティカル(ダメージ増加)かバッドクリティカル(ダメージ減少)かを判定する。計算と判定は`domain/battle/CriticalFormula.kt`が唯一の実装箇所。

- **発生率**: クリティカル発生率の段階n(`BattleStat.CRITICAL_RATE`、±6)から、24分の`rateNumerator`で決まる。n=0は1、n>0は4n(+6で24/24)、n<0は-(4|n|-2)(-1=-2/24 … -6=-22/24)。値が正ならその確率でクリティカル、負なら絶対値の確率でバッドクリティカルになる(このとき通常のクリティカルは起きない)。判定では`random.nextInt(24)`を1回引く。
- **倍率**: クリティカル倍率(%) = max(150 + 25 × 段階, 0)(`BattleStat.CRITICAL_DAMAGE`、±12)。バッドクリティカルでは、さらにmax(倍率 - 75, 0)とする。基本値150(`BattleFieldPawn.criticalDamageRate`)は、ブレイクポイント倍率と同じく戦闘中だけの固定値で、全個体(味方・敵)が持つ。`Character`・`Enemy`・`characters.json`には持たず、キャラ編集でも変えられない。
- **判定の単位と適用順序**: `performAttack`が、命中したヒット1つごと・対象ごとに、命中判定の後で判定する。外れたヒットは判定せず、乱数も引かない。必中で当たったヒットは判定する。ダメージは、`DamageFormula`の結果(+2の後)→ クリティカル/バッドクリティカルなら`max(floor(ダメージ × 倍率 / 100), 1)` → ブレイク中×1.3(`boostDamage`)→ `reduceDamage`の順に求める。ブレイクゲージは、倍率込みで実際に減ったHPで貯まる。ダメージのない`performSkill`では判定しない。
- **記録と表記**: `AttackShot.critical`(`CriticalType`: NONE/CRITICAL/BAD_CRITICAL)。どちらの判定が出たかではなく、最終的な倍率で分類する(`CriticalFormula.finalType`/`classify`)。倍率が100未満ならBAD_CRITICAL、100以上(ちょうど100を含む)ならCRITICAL。例えば、倍率-3段階でクリティカルの判定が出た場合は75%なのでバッドクリティカル、倍率+4段階でバッドクリティカルの判定が出た場合は175%なのでクリティカルと表記する。判定が出なかったヒットと外れたヒットはNONE。
- **段階**: 記録・持続・重ねがけは「ステータスのバフ・デバフ」と同じ仕組み。有効な段階の上限・下限は、`StatStageFormula.minStageOf`/`maxStageOf`でステータスごとに決まる(発生率だけ±6)。`effectiveStat`には渡せない(例外)。
- **ナレーション**: 対象1体・1ヒットなら「AがBに攻撃! クリティカル! 45ダメージ」/「AがBに攻撃! バッドクリティカル… 30ダメージ」、多段なら「1回目: クリティカル! 45ダメージ」。範囲攻撃では、ダメージの文の後に、クリティカルが出たヒットごとに「Bにクリティカル!」/「Cにバッドクリティカル…」を加え、その後に「〜はブレイクした!」が続く。

## ブレイク

ダメージを受けるとブレイクゲージが貯まり、満タンになると1ターン行動できなくなる。計算・状態の変化は`domain/battle/BreakFormula.kt`に集約している(定数も`BreakFormula`)。

- **戦闘中だけの値**: `BattleFieldPawn`の`breakGauge`(0〜100、Double)・`breakResistance`(ブレイク耐性。基本100、0〜999)・`breakPointRate`(ブレイクポイント倍率。基本150、攻撃する側の値で将来バフ/デバフで変わる)・`breakState`(`BreakState`: NONE/BROKEN=ブレイク/RECOVERING=立ち上がり)。キャラ編集では変えられず、`Character`・`Enemy`・`characters.json`には持たない。戦闘ごとに初期値から始まり、次の戦闘へは持ち越さない。
- **ゲージの増え方**: 1ヒットごとに`実際に減ったHP ÷ 最大HP × 100 × 攻撃者の倍率/100 × 対象の耐性/100`(`BreakFormula.gaugeIncrease`)。実際に減ったHPは`min(ダメージ, ヒット前の現在HP)`で、軽減後の値。例: 最大HP120に60ダメージなら75、耐性80なら60。小数のまま持ち、画面のバーだけ切り捨てて表示する(数値・耐性は画面に出さない)。攻撃のダメージだけで貯まり、HPコストでは貯まらず、回復しても減らない。BROKEN・RECOVERING中は貯まらない。そのヒットで倒れた対象はブレイクしない。
- **ブレイク**: ゲージが100以上になったら0に戻し(超過分は捨てる)、BROKENにする(`accumulateBreakGauge`)。BROKENの間は次の3つが効く。多段攻撃の途中でブレイクしたら、残りのヒットからすぐに効く(`performAttack`はヒットごとにフィールドへ反映する)。
  - 行動できない(`performAttack`/`performSkill`は`null`)。手番が来たら`BattleViewModel`が自動で飛ばし、「〜はブレイクしていて動けない!」「〜は立ち上がった!」を表示して`EndTurnUseCase`で終える。
  - 被ダメージが30%増える。`DamageFormula`の最終結果×1.3を切り捨て(`boostDamage`)、その後に`reduceDamage`をかける。
  - 被必中になる。`HitChanceFormula.isHit`の`isTargetGuaranteedHit`で、乱数を引かずに必ず命中する。
- **状態の経過**: 本人の手番終了時(`endTurn`→`progressBreakState`)に、BROKEN→RECOVERING、RECOVERING→NONEになり、そのとき耐性を20下げる(数値が低いほど貯まりにくい。ブレイクのたびに下がり、0で止まる)。
- **ナレーション**: ブレイクした対象ごとに、攻撃のナレーションの後へ「〜はブレイクした!」を加える(`AttackShot.causedBreak`)。カードにはゲージのバー(黄色)と、「ブレイク」「立ち上がり」のラベルを出す。

## ステータスのバフ・デバフ

基礎ステータス6種と命中率・回避率・クリティカル発生率・クリティカル倍率(命中率・回避率は「命中率・回避率」、クリティカルは「クリティカルヒット」の節を参照)のうち、基礎ステータス6種(破壊力`attack`・耐久力`defense`・マナ出力`manaOutput`・マナ耐性`manaResistance`・敏捷`agility`・幸運`luck`、enum`BattleStat`)を段階で上げ下げする。計算は`domain/battle/StatStageFormula.kt`が唯一の実装箇所。

- **段階と倍率**: -12〜+12。段階nの倍率は、n≥0なら`(4+n)/4`、n<0なら`4/(4+|n|)`(+1=5/4、+4=2、+12=4、-1=4/5、-2=2/3、-12=1/4)。補正後の値は`floor(基礎値 × 分子 / 分母)`を整数演算で求める。
- **記録**: `BattleFieldPawn.statModifiers`(`StatModifier`: ステータス・段階・残りターン)に、かけた1回分ごとに別のエントリで持ち、別々に切れる。有効な段階はそのステータスの全エントリの合計を±12に収めた値(`effectiveStage`)。記録は上限を超えても残る(+12中の+1も記録し、古い分が切れても残りの合計で求め直す)。
- **永続**: `StatModifier.remainingTurns`(`StatChange.durationTurns`)が`null`のエントリは手番終了で減らず、その戦闘の終わりまで切れない(次の戦闘へは持ち越さない)。
- **持続**: かけられた本人の手番終了時(`endTurn`→`progressStatModifiers`)に残りターンを1減らし、0で消える。本人の手番中にかけられた分(自分にかけた精神一到)はその手番の終了では減らさない(`grantedDuringOwnTurn`)。
- **戦闘中だけの値**: `Character`・`Enemy`・`characters.json`には持たない。次の戦闘へは持ち越さない。ブレイクゲージは貯めない。
- **使う箇所**: 基礎値のプロパティ(`attack`等)は基礎値のまま。戦闘の計算は必ず`effectiveStat(BattleStat)`を使う。命中率・回避率には使えない(4基準の表で誤った値になるため例外を投げる。段階は`effectiveStage`で取り、`HitChanceFormula`へ渡す)(ダメージ計算の破壊力・耐久力、行動速度の敏捷、同時手番の幸運比較)。マナ出力・マナ耐性は現状戦闘で使わない。確定済みの`turnQueue`は幸運が変わっても引き直さない。
- **ナレーション**: `SkillResult.toNarrations`(`ui/battle/model/SkillNarrations.kt`)。「Aの精神一到!」→「Aの破壊力が1段階上がった!」(命中率・回避率も同じ形)。外れたら「Aの覇気! しかし外れた!」の1文だけ。±12で頭打ちでも文面は同じ。

## 個体の詳細ポップアップ

戦闘画面で敵・味方のカード(`UnitStatCard`)を長押しすると、その個体の詳細を戦闘画面の上に重ねて表示する(`ui/battle/components/UnitDetailPopup.kt`)。

- **開閉**: 敵・味方とも、戦闘不能の個体も開ける。敵の手番・演出中・対象選択中も開けるが、勝敗の表示中は開けず、勝敗が決まると自動で閉じる。開いている個体がフィールドからいなくなっても閉じる。外側のタップと端末の戻る操作で閉じ、内側のタップでは閉じない。裏の画面には黒いフェードをかけ、タップを届かせない(演出の時間経過による進行は裏で続く)。開閉で選択状態(`BattleSelection`)は変わらない。カードのタップの挙動は変わらない。
- **状態**: `BattleUiState.unitDetails`(FPID→`BattleUnitDetailUiModel`)を`reflectedIn`がフィールド更新のたびに全個体分組み立て、`detailUnitId`で開いている個体を持つ。詳細は戦闘の変化にそのまま追従する。イベントは`BattleViewModel.onUnitLongPress` / `onUnitDetailDismiss`。
- **表示内容**: 名前、位置(前衛/後衛。繰り上がり中は「後衛(前衛扱い)」)、HP・MP(現在/最大)、BG(ブレイクゲージを切り捨てた整数「75/100」)とブレイク状態、ステータスH〜L(Hは最大HP、Mは最大MP)、戦闘値(命中率・回避率(どちらも常に基本値100を表示)・クリティカル発生率(常に基本値「4%」。この項目だけ%付き)・クリティカル倍率(常に基本値150)・ブレイク耐性・ブレイクポイント倍率・行動回数`turnCount`・次の手番までの残り距離「切り捨て`remainingDistance` / `BASE_DISTANCE`」。どれも%なしの数値)、ステータス変化(変化のあるステータスだけ「破壊力 +2 (+1 残り10 / +1 残り3)」の形。合計は±12で頭打ちした有効段階、内訳は1回分ごとの段階と残りターン(永続の分は「+1 永続」)。無ければ「なし」。補正後の値は出さず、ステータス欄は基礎値のまま)、Aスキル・Pスキルの名前(通常攻撃は含めない。無ければ「なし」)。

## ドキュメンテーション(KDoc)

- クラス・インターフェース・関数(private関数を含む)には、その機能と目的を説明するKDocを丁寧に記述する。
- 引数を持つ関数には`@param`で各引数の意味を、戻り値がある場合は`@return`で戻り値の意味を記述する。
- 「何をしているか」だけでなく、「何のために存在するか(目的)」が伝わるように書く。
