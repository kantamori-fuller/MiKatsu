package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import org.junit.Assert.assertEquals
import org.junit.Test

/** [ActionSpeedFormula]のべき乗則と素早さの丸めを検証するテスト。 */
class ActionSpeedFormulaTest {

    /** 素早さが10倍なら速度(=行動回数)はちょうど2倍になること。 */
    @Test
    fun speedOf_tenTimesAgility_isExactlyDouble() {
        assertEquals(2.0, ActionSpeedFormula.speedOf(1000) / ActionSpeedFormula.speedOf(100), 1e-9)
    }

    /** 素早さが5倍なら速度は約1.62倍になること。 */
    @Test
    fun speedOf_fiveTimesAgility_isAbout1Point62() {
        assertEquals(1.62, ActionSpeedFormula.speedOf(500) / ActionSpeedFormula.speedOf(100), 0.01)
    }

    /** 素早さが3倍なら速度は約1.39倍になること。 */
    @Test
    fun speedOf_threeTimesAgility_isAbout1Point39() {
        assertEquals(1.39, ActionSpeedFormula.speedOf(300) / ActionSpeedFormula.speedOf(100), 0.01)
    }

    /** 0は1に、上限超過は9999に丸められること。 */
    @Test
    fun speedOf_outOfRange_isClamped() {
        assertEquals(ActionSpeedFormula.speedOf(1), ActionSpeedFormula.speedOf(0), 0.0)
        assertEquals(ActionSpeedFormula.speedOf(9999), ActionSpeedFormula.speedOf(10000), 0.0)
    }
}
