package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill

/**
 * スキルの範囲(スキル一覧の「範囲」列)。1回の使用で選べる対象の広がりを表す。
 *
 * [SkillTargetSelection]と組み合わせて、プレイヤーが選んだ対象(アンカー)から実際の対象を決める
 * (`SkillTargeting.kt`の`resolveTargets`を参照)。[SkillTargetSelection.ONE]ではアンカー1体のみが対象となり、
 * [SkillTargetSelection.ALL]では各値が示す「まとまり」の生存個体全員が対象となる。
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
