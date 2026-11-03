package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleStat

/**
 * アクティブスキルが発動したときの効果。
 *
 * スキル一覧の「効果」列に対応する。効果の種類をデータとして表し、効果の適用処理を戦闘ロジック側へ集約することを目的とする。
 */
sealed interface ActiveSkillEffect {

    /**
     * 対象のHPを固定値で回復する。能力値には依存しない。
     *
     * @property amount 回復量。対象の最大HPを超えては回復しない。
     */
    data class Heal(val amount: Int) : ActiveSkillEffect

    /**
     * 対象のステータスを段階式で上げ下げする(バフ・デバフ)。対象ごとに別のエントリとして記録され、別々に切れる。
     *
     * @property stat 補正するステータス。
     * @property stages 段階の増減。バフは正、デバフは負。
     * @property durationTurns 持続ターン数(対象本人の手番を数える。`null`は永続で、その戦闘の終わりまで切れない。詳細は[jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.StatModifier])。
     */
    data class StatChange(val stat: BattleStat, val stages: Int, val durationTurns: Int?) : ActiveSkillEffect
}
