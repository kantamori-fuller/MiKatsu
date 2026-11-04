package jp.co.fuller.morikan.bootcamp.mikatsu.data.local

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.ElementResistances
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillElement
import org.junit.Assert.assertEquals
import org.junit.Test

/** [ElementResistancesStorageMapper]の保存形式への変換と、旧データ(キーなし)の互換を確かめるテスト。 */
class ElementResistancesStorageMapperTest {

    /** 12属性のキーが列挙名で、シート順に出力される。 */
    @Test
    fun toMap_hasTwelveKeysInOrder() {
        val map = ElementResistancesStorageMapper.toMap(ElementResistances())
        assertEquals(
            listOf("FIRE", "COLLAPSE", "WATER", "ICE", "LIFE", "AIR", "EARTH", "METAL", "THUNDER", "MYSTIC", "DEATH", "EROSION"),
            map.keys.toList(),
        )
        assertEquals(true, map.values.all { it == 100 })
    }

    /** 書いた値を読み直すと同じ値に戻る(負数・999も含む)。 */
    @Test
    fun roundTrip_keepsValues() {
        val original = ElementResistances(fire = 0, collapse = -1, water = -999, ice = 999, thunder = 50, erosion = 150)
        val restored = ElementResistancesStorageMapper.fromMap(ElementResistancesStorageMapper.toMap(original))
        assertEquals(original, restored)
    }

    /** 旧データ(マップなし)はすべて100として読む。 */
    @Test
    fun fromMap_nullIsAllNeutral() {
        assertEquals(ElementResistances(), ElementResistancesStorageMapper.fromMap(null))
    }

    /** 一部のキーが欠けていても、欠けた属性だけ100になる。 */
    @Test
    fun fromMap_missingKeyIsNeutral() {
        val restored = ElementResistancesStorageMapper.fromMap(mapOf("ICE" to 30))
        assertEquals(30, restored.of(SkillElement.ICE))
        assertEquals(100, restored.of(SkillElement.FIRE))
    }
}
