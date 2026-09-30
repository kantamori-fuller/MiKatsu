package jp.co.fuller.morikan.bootcamp.mikatsu.ui.partyformation.model

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Character
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Formation

/**
 * 編成画面(PartyFormationScreen)の画面状態を表すUiState。
 *
 * @property characters 配置対象となる、保存済み全キャラクターの一覧。
 * @property formation 現在編集中の陣形。
 */
data class PartyFormationUiState(
    val characters: List<Character> = emptyList(),
    val formation: Formation = Formation.EMPTY,
) {
    /**
     * スロット番号ごとの、陣形に配置されているキャラクター。
     *
     * 陣形の表の各マスにキャラクター名を表示するために用いる。配置されていないマスは`null`となる。
     */
    val slotCharacters: List<Character?>
        get() = formation.characterIdsBySlot.map { id -> characters.find { it.id == id } }

    /**
     * 「決定」ボタンを押せる状態かどうか。
     *
     * 陣形にキャラクターが1体以上配置されている場合のみ押せるようにする。
     */
    val isConfirmEnabled: Boolean
        get() = formation.memberIds.isNotEmpty()
}
