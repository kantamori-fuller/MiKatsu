# 画面遷移(Navigation 3)

画面の追加・遷移の変更をするときのルール。目次は`CLAUDE.md`。

- 画面遷移にはNavigation 3(`androidx.navigation3`)を用いる。Navigation Compose(2系/`NavHost`+`NavController`)や、sealed classの手動切り替えなど、自前の画面状態管理による遷移は行わない。
- バックスタックは`rememberNavBackStack`で生成した単純な`NavKey`の`List`としてNavHost(集約点)側が直接所有し、画面遷移は`backStack.add(...)` / `backStack.removeLastOrNull()`で行う。
- ルートはKotlin Serialization(`@Serializable`)を付与し、`NavKey`インターフェースを実装したクラス/オブジェクトとして型安全に定義する。文字列ルートによる手動の引数定義は行わない。
- ナビゲーションを1つのファイルに一括管理しない。各画面が自分自身のルート定義・登録関数を`<画面名>Navigation.kt`として個別に持つ(配置は`ui.md`の「uiモジュールのディレクトリ構成」)。登録関数は`EntryProviderScope<NavKey>`への拡張関数とし、内部で`entry<ルート型> { ... }`を呼び出す。
- アプリ全体のNavHost(`ui/MiKatsuNavHost.kt`)は、各画面の`<画面名>Navigation.kt`が公開する`EntryProviderScope`拡張関数を呼び出して`entryProvider`を組み立て、`NavDisplay`へ渡すだけの薄い集約点とする。ルートの定義や画面遷移先の詳細をNavHost自身に書かない。
- ルートが持つ値(例: 編集対象のID)をViewModelへ渡す場合は、Hiltの`@AssistedFactory`/`@AssistedInject`を用いる。Navigation 3はNav2と異なりバックスタックのエントリーにナビゲーション引数用の`SavedStateHandle`を自動で紐付けないため、`SavedStateHandle`経由での引数取得は行わない。
