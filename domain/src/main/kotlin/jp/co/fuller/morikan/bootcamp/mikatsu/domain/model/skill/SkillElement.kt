package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill

/**
 * スキルの属性(スキル一覧の「Aスキル」シートがタブごとに分かれている単位)。
 *
 * 属性名は正式名称で持つ(崩=崩壊、神=神秘、侵=侵食)。Aスキルだけが持ち、Pスキルには属性はない。
 */
enum class SkillElement {
    /** 無(無属性)。 */
    NONE,

    /** 炎。 */
    FIRE,

    /** 崩(崩壊)。 */
    COLLAPSE,

    /** 水。 */
    WATER,

    /** 氷。 */
    ICE,

    /** 生。 */
    LIFE,

    /** 空。 */
    AIR,

    /** 雷。 */
    THUNDER,

    /** 神(神秘)。 */
    MYSTIC,

    /** 地。 */
    EARTH,

    /** 金。 */
    METAL,

    /** 死。 */
    DEATH,

    /** 侵(侵食)。 */
    EROSION,

    /** 龍。 */
    DRAGON,
}
