package jp.co.fuller.morikan.bootcamp.mikatsu.data.local

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AilmentResistanceType
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AilmentResistances
import org.junit.Assert.assertEquals
import org.junit.Test

/** [AilmentResistancesStorageMapper]の保存形式への変換、旧データ(キーなし)の互換、範囲外の補正を確かめるテスト。 */
class AilmentResistancesStorageMapperTest {

    /** 16項目が列挙名のキーで宣言順に出力される。 */
    @Test
    fun toMap_hasSixteenKeysInOrder() {
        val map = AilmentResistancesStorageMapper.toMap(AilmentResistances())
        assertEquals(AilmentResistanceType.entries.map { it.name }, map.keys.toList())
        assertEquals(16, map.size)
        assertEquals(true, map.values.all { it == 100 })
    }

    /** 書いた値を読み直すと同じ値に戻る。 */
    @Test
    fun roundTrip_keepsValues() {
        val original = AilmentResistances().with(AilmentResistanceType.POISON, 0).with(AilmentResistanceType.MENTAL, 35)
            .with(AilmentResistanceType.INSTANT_DEATH, 99)
        assertEquals(original, AilmentResistancesStorageMapper.fromMap(AilmentResistancesStorageMapper.toMap(original)))
    }

    /** 旧データ(マップなし)はすべて100。 */
    @Test
    fun fromMap_nullIsAllNeutral() {
        assertEquals(AilmentResistances(), AilmentResistancesStorageMapper.fromMap(null))
    }

    /** 欠けた項目だけ100になる。 */
    @Test
    fun fromMap_missingKeyIsNeutral() {
        val restored = AilmentResistancesStorageMapper.fromMap(mapOf("BURN" to 30))
        assertEquals(30, restored.of(AilmentResistanceType.BURN))
        assertEquals(100, restored.of(AilmentResistanceType.POISON))
    }

    /** 範囲外は0〜100に収める。 */
    @Test
    fun fromMap_clamps() {
        val restored = AilmentResistancesStorageMapper.fromMap(mapOf("BURN" to -10, "POISON" to 500))
        assertEquals(0, restored.of(AilmentResistanceType.BURN))
        assertEquals(100, restored.of(AilmentResistanceType.POISON))
    }
}
