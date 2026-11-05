# アーキテクチャ・モジュール・DI・データ層

全作業で常に守る設計の原則。目次は`CLAUDE.md`。

## アーキテクチャ原則

本プロジェクトは以下の原則に厳密に従って実装する。

- **Clean Architecture**: UI層・ドメイン層・データ層を分離し、依存の方向は常に外側(UI)から内側(データ)への一方向のみとする。内側の層は外側の層を一切参照しない。
- **MVVM**: View(Screen) - ViewModel - Model(Repository/UseCase)の構成に従う。
- **SOLID**: 各クラス・関数は単一責任の原則(SRP)を満たし、具象ではなく抽象(インターフェース)に依存する(DIP)。
- **SSOT (Single Source of Truth)**: あるデータの正は必ず1箇所にのみ存在させる。データの正はRepositoryが保持し、ViewModelはRepositoryから取得したデータをもとにUiStateを構築して公開する。UI(Screen)側やViewModel側でデータを独自に複製・キャッシュしない。

## モジュール構成

マルチモジュール構成。依存の方向は常に `app → ui → domain ← data` および `app → data`、`app/ui → core` の一方向とし、内側のモジュール(`domain`)が外側のモジュールを参照することはない。

| モジュール | 置くもの | 依存してよい先 |
|---|---|---|
| **app** | アプリのエントリポイント。Compositionルートとして`domain`のRepositoryインターフェースに対する`data`の実装を組み立て、ViewModelへ注入する。`MainActivity`とNavHostの起動処理 | ui, data, core |
| **core** | 特定の機能に依存しない共通コード(デザインシステム/テーマ、共通拡張関数など) | なし(`domain`・`data`・`ui`に依存しない) |
| **domain** | ドメインモデル、Repositoryのインターフェース、UseCase。Androidフレームワークに依存しない純粋なKotlinモジュール | なし |
| **data** | `domain`のRepositoryインターフェースの実装、ローカル/リモートのデータソース | domain のみ |
| **ui** | Screen(Composable)とViewModel | domain(UseCase経由)・core のみ。`data`には直接依存しない(DIPを遵守し、具象実装を知らない状態を保つ) |

## DI (Dependency Injection)

- 依存性注入にはDagger Hiltを用いる。ViewModelやRepository実装のインスタンス化・注入を手動のFactory/Compositionルートで行わない。
- `domain`モジュールはHiltを含むいかなるDIフレームワークにも依存しない(純粋なKotlinモジュールを維持するため)。UseCaseのコンストラクタにDIアノテーションを付与しない。
- Repositoryインターフェースへの実装の束縛(`@Binds`)、およびUseCaseの提供(`@Provides`)は`data`モジュールの`di`パッケージに置く(`RepositoryModule` / `UseCaseModule`)。
- ViewModelは`@HiltViewModel`を付与し、コンストラクタインジェクションで必要なUseCaseを受け取る。
- `app`モジュールの`Application`クラスに`@HiltAndroidApp`、`MainActivity`に`@AndroidEntryPoint`を付与する。
- Compose画面でのViewModel取得には`hiltViewModel()`を用いる。

## データ層 / Repository

- データアクセス(ローカル永続化、DB、通信など)は必ずRepositoryを経由し、ViewModelやUseCaseがファイルI/O・SharedPreferences・DB等を直接操作しない。
- Repositoryはインターフェースと実装を分離する。ViewModel(またはUseCase)はRepositoryのインターフェースにのみ依存し、具象実装を知らない。
