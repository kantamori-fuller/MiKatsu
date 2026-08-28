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

## データ層 / Repository

- データアクセス(ローカル永続化、DB、通信など)は必ずRepositoryを経由し、ViewModelやUseCaseがファイルI/O・SharedPreferences・DB等を直接操作しない。
- Repositoryはインターフェースと実装を分離する。ViewModel(またはUseCase)はRepositoryのインターフェースにのみ依存し、具象実装を知らない。

## 画面遷移

- 画面遷移はNavigation(Navigation Compose)を用いる。sealed classの手動切り替えなど、自前の画面状態管理による遷移は行わない。

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

## ドキュメンテーション(KDoc)

- クラス・インターフェース・関数(private関数を含む)には、その機能と目的を説明するKDocを丁寧に記述する。
- 引数を持つ関数には`@param`で各引数の意味を、戻り値がある場合は`@return`で戻り値の意味を記述する。
- 「何をしているか」だけでなく、「何のために存在するか(目的)」が伝わるように書く。
