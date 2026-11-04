package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill

/**
 * スキルタイプ(スキル一覧の「スキルタイプ」列)。1つのスキルが複数のタイプを併せ持てる。
 *
 * 将来、タイプごとの強化・耐性・使用制限などを、スキル個別ではなく属性として一括して
 * 扱えるようにすることを目的とする。
 */
enum class SkillCategory {
    /** 物理型攻撃。 */
    PHYSICAL_ATTACK,

    /** 特殊型攻撃。破壊力・耐久力の代わりにマナ出力C・マナ耐性Dを使う(物理型攻撃を併せ持たない場合)。 */
    SPECIAL_ATTACK,

    /** 回復。 */
    HEAL,

    /** 変化。 */
    STATUS,

    /** バフ。 */
    BUFF,

    /** デバフ。 */
    DEBUFF,

    /** ポジティブエフェクト。 */
    POSITIVE_EFFECT,

    /** ネガティブエフェクト。 */
    NEGATIVE_EFFECT,

    /** トラップ。 */
    TRAP,
}
