package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model

/**
 * バトルに登場する敵を表すドメインモデル。
 *
 * [Character]と同じ8つのステータスを持つ。これにより、味方・敵の双方が同じ
 * ステータス構成に基づいてダメージ計算等の戦闘ロジックを行えるようにする。
 *
 * @property id 敵を一意に識別するID(EID)。
 * @property name 敵の名前。
 * @property hp H. 体力のステータス値。
 * @property mana M. マナ総量のステータス値。
 * @property attack A. 破壊力のステータス値。
 * @property defense B. 耐久力のステータス値。
 * @property manaOutput C. マナ出力のステータス値。
 * @property manaResistance D. マナ耐性のステータス値。
 * @property agility S. 敏捷のステータス値。
 * @property luck L. 幸運のステータス値。
 */
data class Enemy(
    val id: Int,
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
