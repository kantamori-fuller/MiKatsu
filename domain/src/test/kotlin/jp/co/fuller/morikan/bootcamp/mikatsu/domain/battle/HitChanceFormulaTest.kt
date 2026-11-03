package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** [HitChanceFormula]の倍率表・段階の差し引き・最終命中率の計算と、命中判定の境界を検証するテスト。 */
class HitChanceFormulaTest {

    private fun chance(skill: Int?, accuracyStage: Int = 0, evasionStage: Int = 0) =
        HitChanceFormula.finalChance(skill, accuracyStage, evasionStage)

    /** 全段階(-12〜+12)の倍率の分子/分母が、n≥0なら(6+n)/6、n<0なら6/(6+|n|)であること。 */
    @Test
    fun multiplier_allStages() {
        for (stage in -12..12) {
            val expected = if (stage >= 0) (6 + stage) to 6 else 6 to (6 - stage)
            assertEquals("stage=$stage", expected, HitChanceFormula.numerator(stage) to HitChanceFormula.denominator(stage))
        }
        assertEquals(7 to 6, HitChanceFormula.numerator(1) to HitChanceFormula.denominator(1))
        assertEquals(18 to 6, HitChanceFormula.numerator(12) to HitChanceFormula.denominator(12)) // ×3
        assertEquals(6 to 18, HitChanceFormula.numerator(-12) to HitChanceFormula.denominator(-12))
    }

    /** 補正がなければスキルの命中率のままであること。 */
    @Test
    fun baseValues_keepSkillAccuracy() {
        assertEquals(90.0, chance(90), DELTA)
        assertEquals(100.0, chance(100), DELTA)
    }

    /** 回避+1なら90×6/7≒77.14%、命中+1と回避+1は打ち消し合って90%、命中-2は90×3/4になること。 */
    @Test
    fun stages_matchReferenceExamples() {
        assertEquals(90.0 * 6 / 7, chance(90, evasionStage = 1), DELTA)
        assertEquals(77.142857, chance(90, evasionStage = 1), 1e-5)
        assertEquals(90.0, chance(90, accuracyStage = 1, evasionStage = 1), DELTA)
        assertEquals(67.5, chance(90, accuracyStage = -2), DELTA)
        assertEquals(60.0, chance(40, accuracyStage = 3), DELTA)
    }

    /** 段階の差は±12に収められ、命中+12・回避-12でも+12扱い(×3)であること。 */
    @Test
    fun stageDifference_isClampedTo12() {
        assertEquals(12, HitChanceFormula.hitStage(12, -12))
        assertEquals(-12, HitChanceFormula.hitStage(-12, 12))
        assertEquals(30.0, chance(10, accuracyStage = 12, evasionStage = -12), DELTA)
        assertEquals(10.0 / 3, chance(10, accuracyStage = -12, evasionStage = 12), DELTA)
    }

    /** 端数は丸めず、小数のまま返すこと。 */
    @Test
    fun fraction_isNotRounded() {
        assertEquals(90.0 * 6 / 8, chance(90, evasionStage = 2), DELTA)
    }

    /** 結果は0〜100%に収められること。 */
    @Test
    fun result_isClampedTo0And100() {
        assertEquals(100.0, chance(95, accuracyStage = 6), DELTA)
        assertEquals(0.0, chance(0, evasionStage = 12), DELTA)
    }

    /** スキルの命中率がnullなら必中で、乱数を引かないこと。 */
    @Test
    fun nullAccuracy_alwaysHitsWithoutDrawing() {
        val random = object : Random() {
            override fun nextBits(bitCount: Int): Int = error("乱数を引いてはならない")
        }
        assertEquals(100.0, chance(null, accuracyStage = -12, evasionStage = 12), DELTA)
        assertTrue(HitChanceFormula.isHit(null, -12, 12, random))
    }

    /** 0%は必ず外れ、100%は必ず当たること(乱数の両端でも)。 */
    @Test
    fun boundaries_zeroNeverHits_hundredAlwaysHits() {
        val lowest = fixedRandom(0.0)
        val highest = fixedRandom(Math.nextDown(1.0))
        assertFalse(HitChanceFormula.isHit(0, 0, 0, lowest))
        assertFalse(HitChanceFormula.isHit(0, 0, 0, highest))
        assertTrue(HitChanceFormula.isHit(100, 0, 0, lowest))
        assertTrue(HitChanceFormula.isHit(100, 0, 0, highest))
    }

    /** 乱数が最終命中率(%)より小さければ当たり、以上なら外れること。 */
    @Test
    fun hit_whenRollBelowChance() {
        assertTrue(HitChanceFormula.isHit(90, 0, 0, fixedRandom(0.899)))
        assertFalse(HitChanceFormula.isHit(90, 0, 0, fixedRandom(0.9)))
    }

    private companion object {
        const val DELTA = 1e-9
    }
}

/**
 * [Random.nextDouble]が、与えた値を順に(尽きたら最後の値を)返すテスト用の乱数。命中判定の結果を固定するために使う。
 *
 * @param values 順に返す値(0.0以上1.0未満)。
 * @return 生成した乱数。
 */
fun fixedRandom(vararg values: Double): Random = object : Random() {
    private var index = 0
    override fun nextBits(bitCount: Int): Int = error("nextDouble以外は使わない")
    override fun nextDouble(): Double = values[minOf(index++, values.lastIndex)]
}
