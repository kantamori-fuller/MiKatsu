package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import org.junit.Assert.assertEquals
import org.junit.Test

/** [HitPointFormula]が種族値に基礎HP80を足した最大HPを返すことを検証するテスト。 */
class HitPointFormulaTest {

    /** 最大HPは種族値+80になること。 */
    @Test
    fun maxHpOf_addsBaseHp() {
        assertEquals(180, HitPointFormula.maxHpOf(100))
        assertEquals(80, HitPointFormula.maxHpOf(0))
    }
}
