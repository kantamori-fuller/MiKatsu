package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.CriticalType
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random

/**
 * クリティカル判定の[Random.nextInt]を[criticalRoll]で固定し、命中判定の[Random.nextDouble]を[hitRolls]で固定するテスト用の乱数。
 * クリティカル判定を引いた回数を[criticalDraws]に数える。
 *
 * @param criticalRoll `nextInt(until)`が返す値。省略時は23(基本の発生率ではクリティカルにならない)。
 * @param hitRolls `nextDouble()`が順に(尽きたら最後の値を)返す値。省略時は0.0。
 */
class CriticalRandom(private val criticalRoll: Int = 23, vararg hitRolls: Double) : Random() {
    private val rolls: DoubleArray = if (hitRolls.isEmpty()) doubleArrayOf(0.0) else hitRolls
    private var index = 0

    /** クリティカル判定として`nextInt`を引いた回数。 */
    var criticalDraws = 0
        private set

    override fun nextBits(bitCount: Int): Int = error("nextDouble・nextInt(until)以外は使わない")

    override fun nextInt(until: Int): Int {
        criticalDraws++
        return criticalRoll
    }

    override fun nextDouble(): Double = rolls[minOf(index++, rolls.lastIndex)]
}

/** [CriticalFormula]の発生率・倍率・判定・ダメージ適用を検証するテスト。 */
class CriticalFormulaTest {

    /** 発生率の分子の表(全13段階)。 */
    @Test
    fun rateNumerator_table() {
        val expected = mapOf(
            -6 to -22, -5 to -18, -4 to -14, -3 to -10, -2 to -6, -1 to -2,
            0 to 1,
            1 to 4, 2 to 8, 3 to 12, 4 to 16, 5 to 20, 6 to 24,
        )
        expected.forEach { (stage, numerator) -> assertEquals("stage=$stage", numerator, CriticalFormula.rateNumerator(stage)) }
    }

    /** 発生率の段階は±6で頭打ちになること。 */
    @Test
    fun rateNumerator_clampsToSix() {
        assertEquals(24, CriticalFormula.rateNumerator(7))
        assertEquals(24, CriticalFormula.rateNumerator(100))
        assertEquals(-22, CriticalFormula.rateNumerator(-7))
        assertEquals(-22, CriticalFormula.rateNumerator(-100))
    }

    /** 段階0は1/24: 乱数0だけがクリティカルになること。 */
    @Test
    fun judge_stageZero_onlyRollZeroIsCritical() {
        assertEquals(CriticalType.CRITICAL, CriticalFormula.judge(0, CriticalRandom(0)))
        assertEquals(CriticalType.NONE, CriticalFormula.judge(0, CriticalRandom(1)))
    }

    /** 正の段階はクリティカルのみ。+6(24/24)は必ずクリティカルになること。 */
    @Test
    fun judge_positiveStage_neverBad() {
        assertEquals(CriticalType.CRITICAL, CriticalFormula.judge(1, CriticalRandom(3)))
        assertEquals(CriticalType.NONE, CriticalFormula.judge(1, CriticalRandom(4)))
        for (roll in 0 until 24) assertEquals(CriticalType.CRITICAL, CriticalFormula.judge(6, CriticalRandom(roll)))
    }

    /** 負の段階はバッドクリティカルのみ(通常のクリティカルは起きない)。-1は2/24、-6は22/24。 */
    @Test
    fun judge_negativeStage_onlyBad() {
        assertEquals(CriticalType.BAD_CRITICAL, CriticalFormula.judge(-1, CriticalRandom(1)))
        assertEquals(CriticalType.NONE, CriticalFormula.judge(-1, CriticalRandom(2)))
        assertEquals(CriticalType.BAD_CRITICAL, CriticalFormula.judge(-6, CriticalRandom(21)))
        assertEquals(CriticalType.NONE, CriticalFormula.judge(-6, CriticalRandom(22)))
        for (roll in 0 until 24) {
            assertEquals(false, CriticalFormula.judge(-3, CriticalRandom(roll)) == CriticalType.CRITICAL)
        }
    }

    /** 判定は必ず乱数を1回引くこと。 */
    @Test
    fun judge_drawsOnce() {
        val random = CriticalRandom(0)
        CriticalFormula.judge(0, random)
        assertEquals(1, random.criticalDraws)
    }

    /** 倍率は150+25×段階。段階は±12で頭打ち、下限は0。 */
    @Test
    fun damageRate_table_clampAndFloor() {
        assertEquals(150, CriticalFormula.damageRate(150, 0, isBad = false))
        assertEquals(175, CriticalFormula.damageRate(150, 1, isBad = false))
        assertEquals(450, CriticalFormula.damageRate(150, 12, isBad = false))
        assertEquals(450, CriticalFormula.damageRate(150, 30, isBad = false))
        assertEquals(0, CriticalFormula.damageRate(150, -6, isBad = false))
        assertEquals(0, CriticalFormula.damageRate(150, -12, isBad = false))
        assertEquals(0, CriticalFormula.damageRate(150, -30, isBad = false))
    }

    /** バッドクリティカルは倍率からさらに75引き、下限は0。 */
    @Test
    fun damageRate_bad_subtracts75_floorZero() {
        assertEquals(75, CriticalFormula.damageRate(150, 0, isBad = true))
        assertEquals(100, CriticalFormula.damageRate(150, 1, isBad = true))
        assertEquals(0, CriticalFormula.damageRate(150, -4, isBad = true)) // 50 - 75 → 0
        assertEquals(0, CriticalFormula.damageRate(150, -12, isBad = true))
    }

    /** ダメージは切り捨て: 25×150/100=37.5 → 37。バッド: 25×75/100=18.75 → 18。 */
    @Test
    fun applyDamage_floors() {
        assertEquals(37, CriticalFormula.applyDamage(25, CriticalType.CRITICAL, 150, 0))
        assertEquals(18, CriticalFormula.applyDamage(25, CriticalType.BAD_CRITICAL, 150, 0))
    }

    /** クリティカル・バッドクリティカルの結果は最低1。倍率0でも1になる。通常ヒットはそのまま(0も0のまま)。 */
    @Test
    fun applyDamage_minimumOne_normalUntouched() {
        assertEquals(1, CriticalFormula.applyDamage(1, CriticalType.BAD_CRITICAL, 150, 0)) // 0.75 → 0 → 1
        assertEquals(1, CriticalFormula.applyDamage(50, CriticalType.CRITICAL, 150, -12)) // 倍率0
        assertEquals(24, CriticalFormula.applyDamage(24, CriticalType.NONE, 150, 12))
        assertEquals(0, CriticalFormula.applyDamage(0, CriticalType.NONE, 150, 0))
    }

    /** 倍率の分類: 99%まではBAD_CRITICAL、100%(ちょうど)以上はCRITICAL、0%もBAD_CRITICAL。 */
    @Test
    fun classify_boundaries() {
        assertEquals(CriticalType.BAD_CRITICAL, CriticalFormula.classify(0))
        assertEquals(CriticalType.BAD_CRITICAL, CriticalFormula.classify(99))
        assertEquals(CriticalType.CRITICAL, CriticalFormula.classify(100))
        assertEquals(CriticalType.CRITICAL, CriticalFormula.classify(450))
    }

    /** 最終種別は判定の種類ではなく最終倍率で決まる: 段階-3のクリティカル(75%)はBAD、段階+4のバッド(175%)はCRITICAL、段階-2のクリティカル(100%)はCRITICAL。 */
    @Test
    fun finalType_decidedByFinalRate() {
        assertEquals(CriticalType.BAD_CRITICAL, CriticalFormula.finalType(CriticalType.CRITICAL, 150, -3))
        assertEquals(CriticalType.CRITICAL, CriticalFormula.finalType(CriticalType.BAD_CRITICAL, 150, 4))
        assertEquals(CriticalType.CRITICAL, CriticalFormula.finalType(CriticalType.CRITICAL, 150, -2))
        assertEquals(CriticalType.BAD_CRITICAL, CriticalFormula.finalType(CriticalType.BAD_CRITICAL, 150, 0)) // 75
        assertEquals(CriticalType.CRITICAL, CriticalFormula.finalType(CriticalType.BAD_CRITICAL, 150, 1)) // 100
        assertEquals(CriticalType.BAD_CRITICAL, CriticalFormula.finalType(CriticalType.CRITICAL, 150, -12)) // 0
        assertEquals(CriticalType.NONE, CriticalFormula.finalType(CriticalType.NONE, 150, 12))
    }
}
