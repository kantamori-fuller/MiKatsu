package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill

/**
 * スキルの対象選択(スキル一覧の「対象選択」列)。[SkillArea]の範囲内でどう対象を決めるかを表す。
 *
 * 現段階では値を持つだけで、対象選択の挙動には使っていない。
 */
enum class SkillTargetSelection {
    /** 一体選択。範囲内で一体選択。 */
    ONE,

    /** 全て対象。範囲内全て対象。 */
    ALL,
}
