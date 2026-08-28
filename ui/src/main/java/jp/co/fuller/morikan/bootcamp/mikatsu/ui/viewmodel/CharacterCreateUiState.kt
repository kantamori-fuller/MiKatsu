package jp.co.fuller.morikan.bootcamp.mikatsu.ui.viewmodel

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
)
