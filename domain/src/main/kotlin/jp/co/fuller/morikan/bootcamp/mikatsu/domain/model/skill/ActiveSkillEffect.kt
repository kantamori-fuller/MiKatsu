package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleStat
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.StatusAilment

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

    /**
     * 対象に状態異常を付与する(ネガティブエフェクト)。対象ごと・効果ごとに成功判定を1回行い(成功率100なら乱数を引かない)、
     * 成功しても排他・石化のルールで弾かれることがある(付与のルールは`StatusAilmentFormula`)。
     *
     * @property ailment 付与する状態異常。
     * @property durationTurns 持続ターン数(対象本人の手番を数える)。
     * @property successRate 成功率(0〜100)。補正は受けず固定。
     */
    data class InflictAilment(
        val ailment: StatusAilment,
        val durationTurns: Int,
        val successRate: Int = 100,
    ) : ActiveSkillEffect

    /**
     * 対象を即死させる(HPを0にして戦闘不能にする)。対象ごと・効果ごとに成功判定を1回行う(成功率100なら乱数を引かない)。石化中の対象にも効く。
     *
     * @property successRate 成功率(0〜100)。補正は受けず固定。
     */
    data class InstantDeath(val successRate: Int = 100) : ActiveSkillEffect
}
