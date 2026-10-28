package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill

/**
 * スキルタイプ(スキルの性質の分類)。1つのスキルが複数のタイプを併せ持てる。
 *
 * 将来、タイプごとの強化・耐性・使用制限などを、スキル個別ではなく属性として一括して
 * 扱えるようにすることを目的とする。
 */
enum class SkillCategory {
    /** 攻撃スキル。 */
    ATTACK,

    /** 回復スキル。 */
    HEAL,

    /** 変化スキル。能力や状態に変化を与える。 */
    STATUS,
}
