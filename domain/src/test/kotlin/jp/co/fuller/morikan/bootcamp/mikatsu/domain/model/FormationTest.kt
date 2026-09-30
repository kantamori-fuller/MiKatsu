package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/** [Formation]の配置ルール(配置・移動・入れ替え・上限・取り外し)を検証するテスト。 */
class FormationTest {

    /** 空きマスへ未配置のキャラクターを置くと、そのマスに配置されること。 */
    @Test
    fun place_toEmptySlot_placesCharacter() {
        val formation = Formation.EMPTY.place(characterId = 1, slot = 4)

        assertEquals(listOf(null, null, null, null, 1, null), formation.characterIdsBySlot)
    }

    /** 配置済みのキャラクターを空きマスへ置くと、元のマスが空いて移動すること。 */
    @Test
    fun place_placedCharacterToEmptySlot_movesCharacter() {
        val formation = Formation.EMPTY.place(1, 0).place(1, 5)

        assertEquals(listOf(null, null, null, null, null, 1), formation.characterIdsBySlot)
    }

    /** 配置済みのキャラクターを他のキャラクターがいるマスへ置くと、互いの位置が入れ替わること。 */
    @Test
    fun place_placedCharacterToOccupiedSlot_swapsCharacters() {
        val formation = Formation.EMPTY.place(1, 0).place(2, 5).place(1, 5)

        assertEquals(listOf(2, null, null, null, null, 1), formation.characterIdsBySlot)
    }

    /** 未配置のキャラクターを埋まっているマスへ置くと、元いたキャラクターが外れること。 */
    @Test
    fun place_newCharacterToOccupiedSlot_replacesCharacter() {
        val formation = Formation.EMPTY.place(1, 0).place(2, 5).place(3, 0)

        assertEquals(listOf(3, null, null, null, null, 2), formation.characterIdsBySlot)
    }

    /** 上限人数に達しているとき、未配置のキャラクターは空きマスへ置けないこと。 */
    @Test
    fun place_newCharacterWhenFull_isIgnored() {
        val full = Formation.EMPTY.place(1, 0).place(2, 1)

        assertSame(full, full.place(3, 2))
    }

    /** マスを空けると、そのマスのキャラクターだけが外れること。 */
    @Test
    fun removeAt_removesOnlyThatSlot() {
        val formation = Formation.EMPTY.place(1, 0).place(2, 1).removeAt(0)

        assertEquals(listOf(2), formation.memberIds)
        assertEquals(1, formation.slotOf(2))
    }
}
