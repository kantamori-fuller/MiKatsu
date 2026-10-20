package jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model

import org.junit.Assert.assertEquals
import org.junit.Test

/** [resolveDisplayNames]が同名の個体にだけアルファベットを付けることを検証するテスト。 */
class BattleDisplayNamesTest {

    /** ゴブリン2体とオーク1体なら、ゴブリンにだけA・Bが付くこと。 */
    @Test
    fun resolveDisplayNames_onlyDuplicatedNamesGetSuffix() {
        val names = resolveDisplayNames(listOf(1 to "ゴブリン", 2 to "ゴブリン", 3 to "オーク"))

        assertEquals(mapOf(1 to "ゴブリンA", 2 to "ゴブリンB", 3 to "オーク"), names)
    }

    /** 味方と敵が同名でも、FPIDの順にA, Bが振られること。 */
    @Test
    fun resolveDisplayNames_sameNameAcrossSides_assignedByFpidOrder() {
        val names = resolveDisplayNames(listOf(3 to "勇者", 1 to "勇者"))

        assertEquals(mapOf(1 to "勇者A", 3 to "勇者B"), names)
    }
}
