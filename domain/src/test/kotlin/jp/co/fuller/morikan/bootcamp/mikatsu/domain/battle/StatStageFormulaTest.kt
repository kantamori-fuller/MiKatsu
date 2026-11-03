package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleStat
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.StatModifier
import org.junit.Assert.assertEquals
import org.junit.Test

/** [StatStageFormula]の段階ごとの倍率・切り捨て・有効段階の±12の頭打ちを検証するテスト。 */
class StatStageFormulaTest {

    /** 全段階(-12〜+12)の倍率の分子/分母が仕様どおりであること。 */
    @Test
    fun multiplier_allStages() {
        for (stage in -12..12) {
            val expected = if (stage >= 0) (4 + stage) to 4 else 4 to (4 + -stage)
            assertEquals("stage=$stage", expected, StatStageFormula.numerator(stage) to StatStageFormula.denominator(stage))
        }
    }

    /** 代表的な段階の倍率が、基礎値240に対する期待値と一致すること。 */
    @Test
    fun apply_representativeStages() {
        val expected = mapOf(
            1 to 300, 4 to 480, 7 to 660, 12 to 960,
            -1 to 192, -2 to 160, -6 to 96, -12 to 60, 0 to 240,
        )
        expected.forEach { (stage, value) -> assertEquals("stage=$stage", value, StatStageFormula.apply(240, stage)) }
    }

    /** 全段階で `floor(基礎値 × 分子 / 分母)` と一致し、端数が切り捨てられること。 */
    @Test
    fun apply_floorsForAllStages() {
        for (base in listOf(0, 1, 7, 100, 123, 9999)) {
            for (stage in -12..12) {
                val n = if (stage >= 0) 4 + stage else 4
                val d = if (stage >= 0) 4 else 4 - stage
                assertEquals("base=$base stage=$stage", base * n / d, StatStageFormula.apply(base, stage))
            }
        }
        assertEquals(2, StatStageFormula.apply(3, -1)) // 3 × 4/5 = 2.4 → 2
        assertEquals(3, StatStageFormula.apply(3, 1)) // 3 × 5/4 = 3.75 → 3
    }

    /** 範囲外の段階は±12に収めて計算されること。 */
    @Test
    fun apply_clampsStage() {
        assertEquals(StatStageFormula.apply(100, 12), StatStageFormula.apply(100, 13))
        assertEquals(StatStageFormula.apply(100, -12), StatStageFormula.apply(100, -20))
    }

    /** 有効段階は、同じステータスの全エントリの合計を±12に収めた値で、他のステータスは含まないこと。 */
    @Test
    fun effectiveStage_sumsAndClamps() {
        val modifiers = listOf(
            StatModifier(BattleStat.ATTACK, 10, 5),
            StatModifier(BattleStat.ATTACK, 5, 5),
            StatModifier(BattleStat.DEFENSE, -3, 5),
            StatModifier(BattleStat.DEFENSE, -12, 5),
        )

        assertEquals(12, StatStageFormula.effectiveStage(modifiers, BattleStat.ATTACK))
        assertEquals(-12, StatStageFormula.effectiveStage(modifiers, BattleStat.DEFENSE))
        assertEquals(0, StatStageFormula.effectiveStage(modifiers, BattleStat.LUCK))
    }
}
