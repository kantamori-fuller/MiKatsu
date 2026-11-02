package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill

/**
 * 物理属性(スキル一覧の「物理属性」列)。物理型攻撃の性質を表す。空欄のスキルは`null`で表す。
 */
enum class PhysicalAttribute {
    /** 衝撃。 */
    IMPACT,

    /** 斬撃。 */
    SLASH,

    /** 刺突。 */
    PIERCE,
}
