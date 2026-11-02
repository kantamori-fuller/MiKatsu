package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill

/**
 * スキルの対象選択(スキル一覧の「対象選択」列)。[SkillArea]の範囲内でどう対象を決めるかを表す。
 *
 * 範囲が[SkillArea.SINGLE]のときは、どちらでも常にアンカー1体だけが対象になる
 * (`SkillTargeting.kt`の`resolveTargets`を参照)。
 */
enum class SkillTargetSelection {
    /** 一体選択。範囲内で一体選択。 */
    ONE,

    /** 全て対象。範囲内全て対象。 */
    ALL,
}
