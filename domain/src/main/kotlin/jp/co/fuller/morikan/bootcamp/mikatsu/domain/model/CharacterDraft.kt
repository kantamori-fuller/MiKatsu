package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model

data class CharacterDraft(
    val id: Int?,
    val name: String,
    val hp: Int,
    val mana: Int,
    val attack: Int,
    val defense: Int,
    val manaOutput: Int,
    val manaResistance: Int,
    val agility: Int,
    val luck: Int,
)
