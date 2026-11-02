package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill

/**
 * スキルのレンジ(スキル一覧の「レンジ」列)。味方と敵のどの位置関係まで届くかを表す。
 *
 * 現段階では値を持つだけで、対象選択の制限には使っていない。
 */
enum class SkillRange {
    /** レンジ1。味方前衛から敵前衛など、目の前を対象にできる。 */
    RANGE_1,

    /** レンジ2。味方前衛から敵後衛まで、または味方後衛から敵前衛までを選択できる。 */
    RANGE_2,

    /** レンジ3。味方後衛から敵後衛まで選択できる。 */
    RANGE_3,
}
