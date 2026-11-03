package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model

/**
 * 味方のアクティブスキル1回分の結果。ダメージを与えるスキルと与えないスキルで結果の形が違うため、
 * 呼び出し側(画面)が両方を同じ入口で受け取り、結果の種類に応じた演出に振り分けられるようにすることを目的とする。
 */
sealed interface SkillUseResult {

    /**
     * ダメージを与えるスキルの結果。通常攻撃と同じ[AttackResult]で、攻撃と同じ演出・勝敗判定の流れに乗せる。
     *
     * @property result 攻撃の結果。
     */
    data class Attack(val result: AttackResult) : SkillUseResult

    /**
     * ダメージを与えないスキル(回復・バフ・デバフなど)の結果。
     *
     * @property result スキルの結果。
     */
    data class Effect(val result: SkillResult) : SkillUseResult
}
