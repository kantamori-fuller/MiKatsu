package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AilmentResistanceType
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleFieldPawn
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.StatusAilment

/**
 * 状態異常耐性の値の範囲と、成功率への掛け方を集約した唯一の実装箇所。
 *
 * 実際の成功率 = floor(成功率 × 耐性 / 100)。耐性は0〜100の整数で、既定は100(変化なし)。
 */
object AilmentResistanceFormula {

    /** 耐性値の下限。 */
    const val MIN = 0

    /** 耐性値の上限。 */
    const val MAX = 100

    /** 耐性の既定値(成功率が変わらない)。 */
    const val NEUTRAL = 100

    /**
     * 耐性値を[MIN]〜[MAX]に収める。
     *
     * @param value 収める前の値。
     * @return 収めた値。
     */
    fun clamp(value: Int): Int = value.coerceIn(MIN, MAX)

    /**
     * 耐性を掛けた実際の成功率を返す。
     *
     * @param successRate 効果が持つ成功率(0〜100)。
     * @param resistance 受ける個体の耐性(0〜100)。
     * @return 切り捨てた実際の成功率。
     */
    fun effectiveSuccessRate(successRate: Int, resistance: Int): Int = successRate * resistance / 100
}

/**
 * この個体の、指定した状態異常に対する耐性値を返す。
 *
 * @receiver 耐性を調べる個体。
 * @param ailment 状態異常。
 * @return 耐性値。耐性を持たない状態異常(スタン・烙印)は常に[AilmentResistanceFormula.NEUTRAL]。
 */
fun BattleFieldPawn.ailmentResistanceTo(ailment: StatusAilment): Int =
    AilmentResistanceType.of(ailment)?.let { ailmentResistances.of(it) } ?: AilmentResistanceFormula.NEUTRAL

/**
 * この個体の、即死に対する耐性値を返す。
 *
 * @receiver 耐性を調べる個体。
 * @return 即死耐性。
 */
fun BattleFieldPawn.instantDeathResistance(): Int = ailmentResistances.of(AilmentResistanceType.INSTANT_DEATH)
