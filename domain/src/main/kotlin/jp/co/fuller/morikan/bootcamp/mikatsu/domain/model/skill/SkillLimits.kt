package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill

/**
 * キャラクターに設定できるスキル数の上限。
 *
 * 画面(設定可否の判定)と将来の検証で同じ値を共有するため、定数として1箇所に集約する。
 * 現在の値は暫定。
 */
object SkillLimits {

    /** 1キャラクターに設定できるアクティブスキルの最大数。 */
    const val MAX_ACTIVE_SKILLS = 8

    /** 1キャラクターに設定できるパッシブスキルの最大数。 */
    const val MAX_PASSIVE_SKILLS = 8
}
