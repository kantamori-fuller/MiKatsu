package jp.co.fuller.morikan.bootcamp.mikatsu.ui.viewmodel

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Character

/**
 * キャラ一覧画面(CharacterListScreen)の画面状態を表すUiState。
 *
 * @property characters 表示対象の全キャラクター一覧。
 * @property pendingDeleteId 削除確認ダイアログを表示中のキャラクターID。
 *   ダイアログを表示していない場合は`null`。
 */
data class CharacterListUiState(
    val characters: List<Character> = emptyList(),
    val pendingDeleteId: Int? = null,
)
