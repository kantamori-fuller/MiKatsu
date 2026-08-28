package jp.co.fuller.morikan.bootcamp.mikatsu.ui.navigation

/**
 * アプリ内の画面遷移先(ルート)を一元管理するオブジェクト。
 *
 * ルート文字列や引数キーをこのオブジェクトに集約することで、[MiKatsuNavHost]と
 * 各ViewModelがナビゲーション引数を安全に共有できるようにすることを目的とする。
 */
object MiKatsuDestinations {

    /** メインメニュー画面のルート。 */
    const val MAIN_MENU = "main_menu"

    /** キャラ一覧画面のルート。 */
    const val CHARACTER_LIST = "character_list"

    /** キャラ作成/編集画面へ渡す、編集対象キャラクターIDの引数キー。 */
    const val ARG_CHARACTER_ID = "characterId"

    /**
     * [ARG_CHARACTER_ID]に指定する「新規作成」を表す番兵値。
     *
     * ナビゲーション引数は必須の[Int]としてしか渡せないため、`null`の代わりに
     * この値を用いて「編集対象なし(＝新規作成)」を表現する。
     */
    const val NEW_CHARACTER_ID = -1

    /** キャラ作成/編集画面のルート定義(オプショナル引数付き)。 */
    const val CHARACTER_CREATE = "character_create?$ARG_CHARACTER_ID={$ARG_CHARACTER_ID}"

    /**
     * キャラ作成/編集画面へ遷移するための実際のルート文字列を組み立てる。
     *
     * @param characterId 編集対象のキャラクターID。新規作成の場合は`null`(デフォルト)。
     * @return [characterId]に応じたクエリ引数を含む遷移先ルート文字列。
     */
    fun characterCreateRoute(characterId: Int? = null): String =
        "character_create?$ARG_CHARACTER_ID=${characterId ?: NEW_CHARACTER_ID}"
}
