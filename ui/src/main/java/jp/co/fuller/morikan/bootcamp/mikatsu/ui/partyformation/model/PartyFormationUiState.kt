package jp.co.fuller.morikan.bootcamp.mikatsu.ui.partyformation.model

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Character

/**
 * 編成画面(PartyFormationScreen)の画面状態を表すUiState。
 *
 * @property characters 選択対象となる、保存済み全キャラクターの一覧。
 * @property selectedIds 選択済みキャラクターのIDの一覧。選択順を保持し、最大2件までとなる。
 */
data class PartyFormationUiState(
    val characters: List<Character> = emptyList(),
    val selectedIds: List<Int> = emptyList(),
) {
    /**
     * 「決定」ボタンを押せる状態かどうか。
     *
     * 選択済みキャラクターが1体以上いる場合のみ押せるようにする。
     */
    val isConfirmEnabled: Boolean
        get() = selectedIds.isNotEmpty()
}
