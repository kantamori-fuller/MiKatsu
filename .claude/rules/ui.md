# UI層(Screen / ViewModel / Preview / 文字列 / ディレクトリ構成)

`ui`モジュールで画面や部品を作る・変えるときのルール。目次は`CLAUDE.md`。画面遷移は`navigation.md`。

## Screen / ViewModel

- 1つのScreen(Composable関数)に対して、原則として1つのViewModelを対応させる。
- ViewModelは画面の状態を`UiState`として`StateFlow`等で公開し、Screen(Composable)はそれを購読して描画に専念する。Screen側にビジネスロジック・データ加工処理を書かない。
- ユーザー操作(ボタン押下、入力変更など)はイベントとしてScreenからViewModelへ通知し、ViewModelがイベントを処理してUiStateを更新する。
- ViewModelはAndroidのUI要素(Context、Composable等)に直接依存しない。
- Screenは2つに分ける。
  - `XxxScreen(viewModel, 遷移コールバック, modifier)`: `uiState`の購読・一度だけのイベントの処理・ViewModelへの中継だけを行う。
  - `internal`な`XxxScreenContent(uiState, イベントのコールバック, modifier)`: 描画のすべて。ContentはViewModelに依存しない。
  - ViewModelを持たない画面は分けなくてよい。

## Preview

- **画面や部品を追加・変更したら、必ずPreviewも追加・更新する。** 対象は各画面(`XxxScreenContent`。ViewModelを持たない画面はScreen本体)と`components/`配下の部品。Preview自体が意味をなさない部品(仕組みだけのもの)は除いてよい。
- 主要な状態ごとに用意する(例: 一覧なら空/あり、戦闘画面なら通常/スキル一覧/対象選択中/詳細ポップアップ/勝利/敗北)。
- テーマはライトだけ。`MiKatsuTheme`で包み、`@Preview(showBackground = true, name = "...")`を付ける。
- Previewの関数とダミーデータは、対象のComposableと同じファイルの末尾に`private`で置く。関数名は確認したい状態が分かる名前(`XxxPreview`、`XxxSkillListPreview`など)にし、1〜2行のKDocを付ける。ダミーデータが多すぎるときだけ、同じ画面ディレクトリに`<ScreenName>PreviewData.kt`(`internal`)を作る(例: `ui/battle/BattlePreviewData.kt`)。
- Previewの中ではViewModel・Hilt・`hiltViewModel()`を使わない。ダミーの名前などはデータなので`strings.xml`に入れなくてよい。

## 文字列リソース

- 画面に表示する文言(ボタンのラベル、タイトル、`contentDescription`、ダイアログの本文など)をComposable内やKotlinコードに直接ハードコードしない。必ず`strings.xml`に定義し、`stringResource(R.string.xxx)`経由で参照する。
- 文字列リソースは、その文言を使うScreen(Composable)が属するモジュールの`res/values/strings.xml`に置く(現状は`ui`モジュール)。
- 複数の画面・コンポーネントで全く同じ文言を使う場合(例: 「戻る」「削除」)は、文言ごとに1つの文字列リソースを定義し、使い回す。同じ意味の文言を複数の名前で重複定義しない。

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

- 複数画面から共有されるNavHost(`MiKatsuNavHost.kt`)など、特定の画面に属さないものだけを`ui`直下に置く。
- 現在の画面ディレクトリ: `mainmenu` / `characterlist` / `charactercreate` / `partyformation` / `battle`。
