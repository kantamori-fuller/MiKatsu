package jp.co.fuller.morikan.bootcamp.mikatsu.ui.viewmodel

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Character

data class CharacterListUiState(
    val characters: List<Character> = emptyList(),
    val pendingDeleteId: Int? = null,
)
