package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** [HitChanceFormula]の最終命中率の計算と、命中判定の境界を検証するテスト。 */
class HitChanceFormulaTest {

    private fun chance(skill: Int?, accuracy: Int = 100, evasion: Int = 100) =
        HitChanceFormula.finalChance(skill, accuracy, evasion)

    /** 補正がなければスキルの命中率のままであること。 */
    @Test
    fun baseValues_keepSkillAccuracy() {
        assertEquals(90.0, chance(90), DELTA)
        assertEquals(100.0, chance(100), DELTA)
    }

    /** 命中率・回避率の補正が「差し引いてから掛ける」確認用の例どおりになること(スキル命中率90)。 */
    @Test
    fun modifiers_matchReferenceExamples() {
        assertEquals(99.0, chance(90, accuracy = 110), DELTA)
        assertEquals(81.0, chance(90, accuracy = 90), DELTA)
        assertEquals(81.0, chance(90, evasion = 110), DELTA)
        assertEquals(99.0, chance(90, evasion = 90), DELTA)
        assertEquals(90.0, chance(90, accuracy = 110, evasion = 110), DELTA)
    }

    /** 端数は丸めず、小数のまま返すこと。 */
    @Test
    fun fraction_isNotRounded() {
        assertEquals(89.1, chance(90, accuracy = 99), DELTA)
    }

    /** 結果は0〜100%に収められること。 */
    @Test
    fun result_isClampedTo0And100() {
        assertEquals(100.0, chance(95, accuracy = 110), DELTA)
        assertEquals(0.0, chance(90, accuracy = 0, evasion = 200), DELTA)
    }

    /** スキルの命中率がnullなら必中で、乱数を引かないこと。 */
    @Test
    fun nullAccuracy_alwaysHitsWithoutDrawing() {
        val random = object : Random() {
            override fun nextBits(bitCount: Int): Int = error("乱数を引いてはならない")
        }
        assertEquals(100.0, chance(null, accuracy = 0, evasion = 999), DELTA)
        assertTrue(HitChanceFormula.isHit(null, 0, 999, random))
    }

    /** 0%は必ず外れ、100%は必ず当たること(乱数の両端でも)。 */
    @Test
    fun boundaries_zeroNeverHits_hundredAlwaysHits() {
        val lowest = fixedRandom(0.0)
        val highest = fixedRandom(Math.nextDown(1.0))
        assertFalse(HitChanceFormula.isHit(0, 100, 100, lowest))
        assertFalse(HitChanceFormula.isHit(0, 100, 100, highest))
        assertTrue(HitChanceFormula.isHit(100, 100, 100, lowest))
        assertTrue(HitChanceFormula.isHit(100, 100, 100, highest))
    }

    /** 乱数が最終命中率(%)より小さければ当たり、以上なら外れること。 */
    @Test
    fun hit_whenRollBelowChance() {
        assertTrue(HitChanceFormula.isHit(90, 100, 100, fixedRandom(0.899)))
        assertFalse(HitChanceFormula.isHit(90, 100, 100, fixedRandom(0.9)))
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
