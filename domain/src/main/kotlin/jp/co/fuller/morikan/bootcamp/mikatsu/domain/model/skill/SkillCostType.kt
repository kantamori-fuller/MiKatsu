package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill

/**
 * アクティブスキルを使うために支払うコストの種類(スキル一覧の「コストタイプ」列)。
 */
enum class SkillCostType {
    /** MPスキル。MPを消費する。 */
    MP,

    /** HPスキル。HPを消費する(消費によってHPが0以下になる使い方はできない)。 */
    HP,

    /**
     * TPスキル。TP(タクティカルポイント)を消費する。
     * 味方はTPが足りなくても使え、足りない分は最大HPの1%/TP(切り上げ)のHPで肩代わりする(TP代替システム。HPが1以上残る場合のみ)。
     */
    TP,
}
