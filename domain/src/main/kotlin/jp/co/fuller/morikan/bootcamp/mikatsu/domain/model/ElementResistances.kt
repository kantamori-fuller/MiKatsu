package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.ElementResistanceFormula
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillElement

/**
 * 12属性ぶんの耐性値をまとめたドメインモデル。キャラクター([Character])と敵([Enemy])が持つ。
 *
 * 値は[ElementResistanceFormula.MIN]〜[ElementResistanceFormula.MAX]の整数で、既定は等倍の[ElementResistanceFormula.NEUTRAL](100)。
 * 値の意味(倍率・無効・反射・吸収)の判定と計算は[ElementResistanceFormula]が担い、このクラスは値を持つだけである。
 * 無([SkillElement.NONE])と龍([SkillElement.DRAGON])には耐性項目がなく、[of]は常に等倍を返す。
 * 属性の並びは[ORDER](炎・崩・水・氷・生・空・地・金・雷・神・死・侵)で、画面の表示順もこれに従う。
 *
 * @property fire 炎の耐性。
 * @property collapse 崩の耐性。
 * @property water 水の耐性。
 * @property ice 氷の耐性。
 * @property life 生の耐性。
 * @property air 空の耐性。
 * @property earth 地の耐性。
 * @property metal 金の耐性。
 * @property thunder 雷の耐性。
 * @property mystic 神の耐性。
 * @property death 死の耐性。
 * @property erosion 侵の耐性。
 */
data class ElementResistances(
    val fire: Int = ElementResistanceFormula.NEUTRAL,
    val collapse: Int = ElementResistanceFormula.NEUTRAL,
    val water: Int = ElementResistanceFormula.NEUTRAL,
    val ice: Int = ElementResistanceFormula.NEUTRAL,
    val life: Int = ElementResistanceFormula.NEUTRAL,
    val air: Int = ElementResistanceFormula.NEUTRAL,
    val earth: Int = ElementResistanceFormula.NEUTRAL,
    val metal: Int = ElementResistanceFormula.NEUTRAL,
    val thunder: Int = ElementResistanceFormula.NEUTRAL,
    val mystic: Int = ElementResistanceFormula.NEUTRAL,
    val death: Int = ElementResistanceFormula.NEUTRAL,
    val erosion: Int = ElementResistanceFormula.NEUTRAL,
) {

    /**
     * 指定した属性の耐性値を返す。
     *
     * @param element 調べる属性。
     * @return 耐性値。耐性項目のない[SkillElement.NONE]・[SkillElement.DRAGON]は常に[ElementResistanceFormula.NEUTRAL]。
     */
    fun of(element: SkillElement): Int = when (element) {
        SkillElement.FIRE -> fire
        SkillElement.COLLAPSE -> collapse
        SkillElement.WATER -> water
        SkillElement.ICE -> ice
        SkillElement.LIFE -> life
        SkillElement.AIR -> air
        SkillElement.EARTH -> earth
        SkillElement.METAL -> metal
        SkillElement.THUNDER -> thunder
        SkillElement.MYSTIC -> mystic
        SkillElement.DEATH -> death
        SkillElement.EROSION -> erosion
        SkillElement.NONE, SkillElement.DRAGON -> ElementResistanceFormula.NEUTRAL
    }

    /**
     * 指定した属性の耐性値だけを差し替えたコピーを返す。
     *
     * @param element 差し替える属性。耐性項目のない[SkillElement.NONE]・[SkillElement.DRAGON]は何も変えない。
     * @param value 新しい耐性値。
     * @return 差し替え後の耐性。
     */
    fun with(element: SkillElement, value: Int): ElementResistances = when (element) {
        SkillElement.FIRE -> copy(fire = value)
        SkillElement.COLLAPSE -> copy(collapse = value)
        SkillElement.WATER -> copy(water = value)
        SkillElement.ICE -> copy(ice = value)
        SkillElement.LIFE -> copy(life = value)
        SkillElement.AIR -> copy(air = value)
        SkillElement.EARTH -> copy(earth = value)
        SkillElement.METAL -> copy(metal = value)
        SkillElement.THUNDER -> copy(thunder = value)
        SkillElement.MYSTIC -> copy(mystic = value)
        SkillElement.DEATH -> copy(death = value)
        SkillElement.EROSION -> copy(erosion = value)
        SkillElement.NONE, SkillElement.DRAGON -> this
    }

    companion object {
        /** 耐性項目を持つ12属性を、表示・保存で扱う順(炎・崩・水・氷・生・空・地・金・雷・神・死・侵)に並べたもの。 */
        val ORDER: List<SkillElement> = listOf(
            SkillElement.FIRE,
            SkillElement.COLLAPSE,
            SkillElement.WATER,
            SkillElement.ICE,
            SkillElement.LIFE,
            SkillElement.AIR,
            SkillElement.EARTH,
            SkillElement.METAL,
            SkillElement.THUNDER,
            SkillElement.MYSTIC,
            SkillElement.DEATH,
            SkillElement.EROSION,
        )
    }
}
