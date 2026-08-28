package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model

/**
 * 端末内に保存済みのキャラクターを表すドメインモデル。
 *
 * [id]は保存時に一度だけ採番され、以降変更されない永続的な識別子である。
 * このモデルは「保存が完了した状態」のキャラクターのみを表し、
 * まだ保存されていない入力途中のキャラクターは[CharacterDraft]で表現する。
 *
 * @property id キャラクターを一意に識別するID。作成順に採番され、削除されても再利用されない。
 * @property name キャラクターの名前。
 * @property hp H. 体力のステータス値。
 * @property mana M. マナ総量のステータス値。
 * @property attack A. 破壊力のステータス値。
 * @property defense B. 耐久力のステータス値。
 * @property manaOutput C. マナ出力のステータス値。
 * @property manaResistance D. マナ耐性のステータス値。
 * @property agility S. 敏捷のステータス値。
 * @property luck L. 幸運のステータス値。
 */
data class Character(
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
