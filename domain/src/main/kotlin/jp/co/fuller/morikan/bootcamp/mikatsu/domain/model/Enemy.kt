package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model

/**
 * バトルに登場する敵を表すドメインモデル。
 *
 * @property id 敵を一意に識別するID。
 * @property name 敵の名前。
 * @property hp 現在のHP。
 * @property maxHp 最大HP。
 * @property mana 現在のMP。
 * @property maxMana 最大MP。
 */
data class Enemy(
    val id: Int,
    val name: String,
    val hp: Int,
    val maxHp: Int,
    val mana: Int,
    val maxMana: Int,
)
