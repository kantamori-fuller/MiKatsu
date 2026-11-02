package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill

/**
 * スキルの範囲(スキル一覧の「範囲」列)。1回の使用で選べる対象の広がりを表す。
 *
 * 現段階では値を持つだけで、対象選択の制限には使っていない。
 */
enum class SkillArea {
    /** 単体。単体選択。 */
    SINGLE,

    /** 陣。前衛3マスもしくは後衛3マスの陣選択。 */
    FORMATION,

    /** 列。1つの前衛と後衛を突き通す列選択。 */
    COLUMN,

    /** エリア全体。エリア全体選択(敵全体・味方全体のどちらも選べる)。 */
    AREA,

    /** フィールド全体。フィールド全体選択(敵味方の両方をまとめて選ぶ)。 */
    FIELD,
}
