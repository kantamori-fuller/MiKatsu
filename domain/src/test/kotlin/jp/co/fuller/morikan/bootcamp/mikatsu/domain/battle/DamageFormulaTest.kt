package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import org.junit.Assert.assertEquals
import org.junit.Test

/** [DamageFormula]のダメージ計算(丸め・境界値・0除算・オーバーフロー)を検証するテスト。 */
class DamageFormulaTest {

    /** 威力100・攻撃100・防御100のとき、24になること(浮動小数点誤差による23にならない)。 */
    @Test
    fun calculate_boundaryValues_returns24() {
        assertEquals(24, DamageFormula.calculate(100, 100, 100))
    }

    /** 実数値41.25が一度だけ切り捨てられ、43になること。 */
    @Test
    fun calculate_truncatesOnlyOnce() {
        assertEquals(43, DamageFormula.calculate(100, 150, 80))
    }

    /** 防御が0でも例外にならず、防御1として計算されること。 */
    @Test
    fun calculate_zeroDefense_treatedAsOne() {
        assertEquals(DamageFormula.calculate(100, 100, 1), DamageFormula.calculate(100, 100, 0))
    }

    /** 攻撃が負の場合は0として扱われ、固定値の2になること。 */
    @Test
    fun calculate_negativeAttack_returnsBaseDamage() {
        assertEquals(2, DamageFormula.calculate(100, -50, 100))
    }

    /** 極端に大きな値でもオーバーフローせず、Int最大値で頭打ちになること。 */
    @Test
    fun calculate_extremeValues_clampsToIntMax() {
        assertEquals(Int.MAX_VALUE, DamageFormula.calculate(Int.MAX_VALUE, Int.MAX_VALUE, 1))
    }
}
