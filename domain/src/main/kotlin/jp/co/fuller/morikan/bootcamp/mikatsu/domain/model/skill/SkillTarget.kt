package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill

/**
 * アクティブスキルの対象の指定方法。
 *
 * 対象はいずれもFPIDで指定する。スキルごとに対象の選び方(単体・全体・敵味方など)を
 * 型として表し、対象の妥当性判定を一箇所([jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.performSkill])で
 * 行えるようにすることを目的とする。
 */
sealed interface SkillTarget {

    /** 自分を含む味方全体から1体を選ぶ。戦闘不能の味方は選べない。 */
    data object SingleAlly : SkillTarget
}
