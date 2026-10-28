package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill

/**
 * スキルのマスターデータに共通のインターフェース。
 *
 * スキルIDはアクティブ・パッシブで独立して採番され、敵のEIDと同じく固定のマスターデータを
 * 識別する。同じ数値でも種別が違えば別のスキルであり、IDは必ず種別(A/P)とセットで意味を持つ。
 * 戦闘中の個体を識別するFPIDとは別物である。
 */
sealed interface Skill {

    /** 同じ種別(アクティブまたはパッシブ)の中でスキルを一意に識別するID(マスターデータのID)。 */
    val id: Int

    /** スキルの名前。 */
    val name: String

    /** スキルタイプ。1つのスキルが複数持てる。持たない場合は空。 */
    val categories: Set<SkillCategory>
}

/**
 * 戦闘中に自分の意思で使うアクティブスキル(Aスキル)。
 *
 * @property id スキルID。
 * @property name スキルの名前。
 * @property categories スキルタイプ。
 * @property contactType 接触タイプ。
 * @property costType コストタイプ。
 * @property cost 使用に必要なコスト量。[costType]がMPならMPの消費量。
 * @property target 対象の指定方法。
 * @property effect 発動時の効果。
 */
data class ActiveSkill(
    override val id: Int,
    override val name: String,
    override val categories: Set<SkillCategory>,
    val contactType: SkillContactType,
    val costType: SkillCostType,
    val cost: Int,
    val target: SkillTarget,
    val effect: ActiveSkillEffect,
) : Skill

/**
 * 設定しておくだけで常に効果を発揮するパッシブスキル(Pスキル)。
 *
 * @property id スキルID。
 * @property name スキルの名前。
 * @property categories スキルタイプ。
 * @property effect 常時発揮される効果。
 */
data class PassiveSkill(
    override val id: Int,
    override val name: String,
    override val categories: Set<SkillCategory>,
    val effect: PassiveSkillEffect,
) : Skill
