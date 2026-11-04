package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import kotlin.math.abs
import kotlin.random.Random

/**
 * 攻撃・スキルが当たるかどうかを決める命中判定の計算式。
 *
 * スキルの命中率に、攻撃者の命中率の段階と対象の回避率の段階の差から決まる倍率を掛けて最終的な命中率(%)を求め、
 * 乱数で当たり外れを決める。命中+1と回避+1のように、同じ段階どうしは打ち消し合う。
 * 倍率表は基礎ステータス6種([StatStageFormula])とは別で、段階nの倍率は n≥0 なら `(6+n)/6`、n<0 なら `6/(6+|n|)`。
 * 計算を1箇所に集約し、攻撃処理から切り離してテストできるようにすることを目的とする。
 */
object HitChanceFormula {

    /** 詳細ポップアップに表示する、命中率・回避率の基本値。計算には使わない(段階だけが計算に使われる)。 */
    const val BASE_RATE = 100

    /** 最終的な命中率(%)の上限。 */
    private const val MAX_CHANCE = 100.0

    /** 倍率の基準となる値(段階0で分子・分母がともにこの値になる)。 */
    private const val BASE = 6

    /**
     * 段階の倍率の分子を返す。
     *
     * @param stage 段階。範囲外は[StatStageFormula.MIN_STAGE]〜[StatStageFormula.MAX_STAGE]に収める。
     * @return 倍率の分子。
     */
    fun numerator(stage: Int): Int = clampStage(stage).let { if (it >= 0) BASE + it else BASE }

    /**
     * 段階の倍率の分母を返す。
     *
     * @param stage 段階。範囲外は[StatStageFormula.MIN_STAGE]〜[StatStageFormula.MAX_STAGE]に収める。
     * @return 倍率の分母。
     */
    fun denominator(stage: Int): Int = clampStage(stage).let { if (it >= 0) BASE else BASE + abs(it) }

    /**
     * 攻撃者の命中率の段階と対象の回避率の段階から、命中判定に使う段階を求める。
     *
     * @param accuracyStage 攻撃者の命中率の有効段階。
     * @param evasionStage 対象の回避率の有効段階。
     * @return `命中率の段階 − 回避率の段階`を±12に収めた値。
     */
    fun hitStage(accuracyStage: Int, evasionStage: Int): Int = clampStage(accuracyStage - evasionStage)

    /**
     * 最終的な命中率(%)を計算する。
     *
     * `スキルの命中率 × 倍率(hitStage)`を、0〜100に収めた値。端数は丸めず小数のまま返す。
     *
     * @param skillAccuracy スキルの命中率(0〜100)。`null`は必中を表し、判定しない。
     * @param accuracyStage 攻撃者の命中率の有効段階。
     * @param evasionStage 対象の回避率の有効段階。
     * @param isAttackerDizzy 攻撃者が目眩か。`true`なら最終命中率に[StatusAilmentFormula.DIZZINESS_HIT_RATE]を掛ける。
     * @return 最終的な命中率(0.0〜100.0)。スキルの命中率が`null`なら100.0(目眩でも必中は変わらない)。
     */
    fun finalChance(skillAccuracy: Int?, accuracyStage: Int, evasionStage: Int, isAttackerDizzy: Boolean = false): Double {
        if (skillAccuracy == null) return MAX_CHANCE
        val stage = hitStage(accuracyStage, evasionStage)
        val chance = (skillAccuracy.toDouble() * numerator(stage) / denominator(stage)).coerceIn(0.0, MAX_CHANCE)
        return if (isAttackerDizzy) chance * StatusAilmentFormula.DIZZINESS_HIT_RATE else chance
    }

    /**
     * 命中判定を行い、当たったかどうかを返す。
     *
     * スキルの命中率が`null`(必中)のときは乱数を引かずに命中とする。それ以外は`[0, 100)`の乱数が
     * 最終的な命中率より小さければ命中なので、0%は必ず外れ、100%は必ず当たる。
     * 対象がブレイク中([isTargetGuaranteedHit])のときは、段階に関係なく乱数を引かずに必ず命中する(被必中)。
     *
     * @param skillAccuracy スキルの命中率。`null`は必中。
     * @param accuracyStage 攻撃者の命中率の有効段階。
     * @param evasionStage 対象の回避率の有効段階。
     * @param random 当たり外れを決める乱数。テストで結果を固定できるよう外から受け取る。
     * @param isTargetGuaranteedHit 対象が被必中(ブレイク中)か。`true`なら必ず命中する。
     * @param isAttackerDizzy 攻撃者が目眩か(最終命中率が0.4倍になる。必中はそのまま必中)。
     * @return 命中したら`true`。
     */
    fun isHit(
        skillAccuracy: Int?,
        accuracyStage: Int,
        evasionStage: Int,
        random: Random,
        isTargetGuaranteedHit: Boolean = false,
        isAttackerDizzy: Boolean = false,
    ): Boolean {
        if (isTargetGuaranteedHit || skillAccuracy == null) return true
        return random.nextDouble() * MAX_CHANCE < finalChance(skillAccuracy, accuracyStage, evasionStage, isAttackerDizzy)
    }

    /** 段階を±12に収める。 */
    private fun clampStage(stage: Int): Int = stage.coerceIn(StatStageFormula.MIN_STAGE, StatStageFormula.MAX_STAGE)
}
