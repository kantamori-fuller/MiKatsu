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

## ドキュメンテーション(KDoc)

- クラス・インターフェース・関数(private関数を含む)には、その機能と目的を説明するKDocを丁寧に記述する。
- 引数を持つ関数には`@param`で各引数の意味を、戻り値がある場合は`@return`で戻り値の意味を記述する。
- 「何をしているか」だけでなく、「何のために存在するか(目的)」が伝わるように書く。
