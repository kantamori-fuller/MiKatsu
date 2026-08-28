package jp.co.fuller.morikan.bootcamp.mikatsu.ui.charactercreate.model

/**
 * キャラ作成/編集画面(CharacterCreateScreen)の画面状態を表すUiState。
 *
 * 各ステータスは入力フィールドの表示文字列として保持し、保存時に整数へ変換する。
 *
 * @property isEditing 既存キャラクターの編集かどうか。`false`の場合は新規作成。
 *   画面タイトルの出し分けに使用する。
 * @property name 名前フィールドの入力値。
 * @property hp H. 体力フィールドの入力値。
 * @property mana M. マナ総量フィールドの入力値。
 * @property attack A. 破壊力フィールドの入力値。
 * @property defense B. 耐久力フィールドの入力値。
 * @property manaOutput C. マナ出力フィールドの入力値。
 * @property manaResistance D. マナ耐性フィールドの入力値。
 * @property agility S. 敏捷フィールドの入力値。
 * @property luck L. 幸運フィールドの入力値。
 * @property isSaved 保存処理が完了したかどうか。`true`になったタイミングで
 *   Screen側が前の画面へ戻る。
 * @property isSaveEnabled 「保存」ボタンを押せる状態かどうか。[name]が空文字
 *   (または空白のみ)の場合は保存不可となる。
 */
data class CharacterCreateUiState(
    val isEditing: Boolean = false,
    val name: String = "",
    val hp: String = "",
    val mana: String = "",
    val attack: String = "",
    val defense: String = "",
    val manaOutput: String = "",
    val manaResistance: String = "",
    val agility: String = "",
    val luck: String = "",
    val isSaved: Boolean = false,
) {
    /**
     * 「保存」ボタンを押せる状態かどうか。
     *
     * 名前が空文字(または空白のみ)の場合は保存不可とする。
     */
    val isSaveEnabled: Boolean
        get() = name.isNotBlank()
}
