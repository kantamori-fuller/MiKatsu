package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model

/**
 * キャラクター作成/編集画面で入力中の内容を表す、保存前のドラフトモデル。
 *
 * 新規作成時は[id]を`null`とし、保存時に新しいIDが採番される。
 * 既存キャラクターの編集時は[id]に対象のIDを設定し、保存時に同じIDへ上書き保存される。
 *
 * @property id 保存対象のキャラクターID。新規作成の場合は`null`。
 * @property name 入力された名前。
 * @property hp 入力されたH. 体力の値。
 * @property mana 入力されたM. マナ総量の値。
 * @property attack 入力されたA. 破壊力の値。
 * @property defense 入力されたB. 耐久力の値。
 * @property manaOutput 入力されたC. マナ出力の値。
 * @property manaResistance 入力されたD. マナ耐性の値。
 * @property agility 入力されたS. 敏捷の値。
 * @property luck 入力されたL. 幸運の値。
 */
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
