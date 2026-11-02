package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill

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
}
