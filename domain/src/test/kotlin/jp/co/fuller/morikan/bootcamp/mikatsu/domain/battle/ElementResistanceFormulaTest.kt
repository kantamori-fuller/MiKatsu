package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.ElementResistanceFormula.Kind
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.ElementResistances
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillElement
import org.junit.Assert.assertEquals
import org.junit.Test

/** [ElementResistanceFormula]の耐性値の分類・倍率・最低ダメージと、[ElementResistances]の値の引き方を検証するテスト。 */
class ElementResistanceFormulaTest {

    /** 1〜999は倍率、0は無効、-1は反射、-999は吸収、それ以外の負数は無効に分類されること。 */
    @Test
    fun classify_byValue() {
        assertEquals(Kind.MULTIPLIER, ElementResistanceFormula.classify(1))
        assertEquals(Kind.MULTIPLIER, ElementResistanceFormula.classify(100))
        assertEquals(Kind.MULTIPLIER, ElementResistanceFormula.classify(999))
        assertEquals(Kind.NULLIFY, ElementResistanceFormula.classify(0))
        assertEquals(Kind.REFLECT, ElementResistanceFormula.classify(-1))
        assertEquals(Kind.ABSORB, ElementResistanceFormula.classify(-999))
        assertEquals(Kind.NULLIFY, ElementResistanceFormula.classify(-2))
        assertEquals(Kind.NULLIFY, ElementResistanceFormula.classify(-500))
        assertEquals(Kind.NULLIFY, ElementResistanceFormula.classify(-998))
    }

    /** 倍率はダメージ×耐性÷100の切り捨てで、結果が0なら1になること。 */
    @Test
    fun scaleDamage_floorsAndHasMinimumOne() {
        assertEquals(24, ElementResistanceFormula.scaleDamage(24, 100))
        assertEquals(12, ElementResistanceFormula.scaleDamage(24, 50))
        assertEquals(36, ElementResistanceFormula.scaleDamage(24, 150))
        assertEquals(1, ElementResistanceFormula.scaleDamage(24, 1))
        assertEquals(1, ElementResistanceFormula.scaleDamage(1, 50))
        assertEquals(239,ElementResistanceFormula.scaleDamage(24, 999))
        assertEquals(10, ElementResistanceFormula.scaleDamage(7, 150)) // 10.5 → 10
    }

    /** 入力範囲への丸めが-999〜999になること。 */
    @Test
    fun clamp_keepsInRange() {
        assertEquals(999, ElementResistanceFormula.clamp(5000))
        assertEquals(-999, ElementResistanceFormula.clamp(-5000))
        assertEquals(30, ElementResistanceFormula.clamp(30))
    }

    /** 既定はすべて100で、無・龍は値を差し替えようとしても常に100を返すこと。 */
    @Test
    fun elementResistances_defaultAndNoneDragon() {
        val defaults = ElementResistances()
        assertEquals(12, ElementResistances.ORDER.size)
        assertEquals(true, ElementResistances.ORDER.all { defaults.of(it) == 100 })
        val changed = defaults.with(SkillElement.NONE, 0).with(SkillElement.DRAGON, 0)
        assertEquals(100, changed.of(SkillElement.NONE))
        assertEquals(100, changed.of(SkillElement.DRAGON))
    }

    /** 12属性それぞれが別の項目として読み書きされ、並びが炎・崩・水・氷・生・空・地・金・雷・神・死・侵であること。 */
    @Test
    fun elementResistances_eachElementIsIndependent_andOrdered() {
        assertEquals(
            listOf(
                SkillElement.FIRE, SkillElement.COLLAPSE, SkillElement.WATER, SkillElement.ICE, SkillElement.LIFE, SkillElement.AIR,
                SkillElement.EARTH, SkillElement.METAL, SkillElement.THUNDER, SkillElement.MYSTIC, SkillElement.DEATH, SkillElement.EROSION,
            ),
            ElementResistances.ORDER,
        )
        val all = ElementResistances.ORDER.foldIndexed(ElementResistances()) { index, acc, element -> acc.with(element, index + 1) }
        assertEquals((1..12).toList(), ElementResistances.ORDER.map { all.of(it) })
    }
}
