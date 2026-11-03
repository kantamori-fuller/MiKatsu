package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleStat
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.SkillTargetResult
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.StatChangeResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** 永続のバフ(残りターンnull)と、永続バフを付けるスキル「研ぎ澄ます」の[performSkill]を検証するテスト。 */
class PermanentBuffTest {

    private fun field(hp: Int = 100, mp: Int = 100): BattleField = BattleField(
        allies = listOf(testAlly(1, agility = 9999, hp = hp, mp = mp, activeSkills = listOf(TEST_TOGISUMASU))),
        enemies = listOf(testEnemy(2, agility = 1, hp = 10000)),
        turnQueue = listOf(1),
    )

    /** 永続のバフは、手番を何度終えても減らず消えないこと(有限のバフは同時に切れる)。 */
    @Test
    fun permanent_neverExpires() {
        var f = field().addStatModifier(1, BattleStat.CRITICAL_RATE, 1, null).addStatModifier(1, BattleStat.ATTACK, 1, 2)

        repeat(20) { f = f.endTurn(1, Random(0)) }

        val modifier = f.allies[0].statModifiers.single()
        assertEquals(BattleStat.CRITICAL_RATE, modifier.stat)
        assertNull(modifier.remainingTurns)
        assertEquals(false, modifier.grantedDuringOwnTurn)
    }

    /** 発生率の有効段階は±6、倍率は±12で頭打ち。記録は上限を超えても残ること。 */
    @Test
    fun effectiveStage_perStatCap() {
        var f = field()
        repeat(8) { f = f.addStatModifier(1, BattleStat.CRITICAL_RATE, 1, null) }
        repeat(14) { f = f.addStatModifier(1, BattleStat.CRITICAL_DAMAGE, 1, null) }
        repeat(8) { f = f.addStatModifier(1, BattleStat.ACCURACY, 2, null) }

        assertEquals(6, f.allies[0].effectiveStage(BattleStat.CRITICAL_RATE))
        assertEquals(12, f.allies[0].effectiveStage(BattleStat.CRITICAL_DAMAGE))
        assertEquals(12, f.allies[0].effectiveStage(BattleStat.ACCURACY))
        assertEquals(8, f.allies[0].statModifiers.count { it.stat == BattleStat.CRITICAL_RATE })

        var g = field()
        repeat(9) { g = g.addStatModifier(1, BattleStat.CRITICAL_RATE, -1, null) }
        assertEquals(-6, g.allies[0].effectiveStage(BattleStat.CRITICAL_RATE))
    }

    /** 研ぎ澄ますで、3つの永続エントリが付き、HP30・MP50が消費され、乱数は引かないこと。 */
    @Test
    fun togisumasu_addsThreePermanentEntries_andPaysCosts() {
        val noDraw = object : Random() {
            override fun nextBits(bitCount: Int): Int = error("乱数を引いてはいけない")

            // 命中判定(nextDouble)は引かない。TPの獲得量(nextInt(until))だけは引いてよい。
            override fun nextDouble(): Double = error("命中判定の乱数を引いてはいけない")

            override fun nextInt(until: Int): Int = until - 1
        }

        val (after, result) = checkNotNull(field().performSkill(1, TEST_TOGISUMASU.id, 1, noDraw))

        val modifiers = after.allies[0].statModifiers
        assertEquals(
            listOf(BattleStat.ACCURACY, BattleStat.CRITICAL_RATE, BattleStat.CRITICAL_DAMAGE),
            modifiers.map { it.stat },
        )
        assertTrue(modifiers.all { it.stages == 1 && it.remainingTurns == null })
        assertEquals(70, after.allies[0].currentHp)
        assertEquals(50, after.allies[0].currentMp)
        assertEquals(
            listOf(
                SkillTargetResult(
                    1, 0, true,
                    listOf(
                        StatChangeResult(BattleStat.ACCURACY, 1),
                        StatChangeResult(BattleStat.CRITICAL_RATE, 1),
                        StatChangeResult(BattleStat.CRITICAL_DAMAGE, 1),
                    ),
                ),
            ),
            result.targets,
        )
    }

    /** HPコストは支払い後にHPが1以上残る場合のみ: HP30では使えず、HP31なら使える。 */
    @Test
    fun togisumasu_hpCostCondition() {
        assertNull(field(hp = 30).performSkill(1, TEST_TOGISUMASU.id, 1, Random(0)))
        assertEquals(1, checkNotNull(field(hp = 31).performSkill(1, TEST_TOGISUMASU.id, 1, Random(0))).first.allies[0].currentHp)
    }

    /** MPが足りない(49)と使えず、50なら使えること。 */
    @Test
    fun togisumasu_mpCostCondition() {
        assertNull(field(mp = 49).performSkill(1, TEST_TOGISUMASU.id, 1, Random(0)))
        assertEquals(0, checkNotNull(field(mp = 50).performSkill(1, TEST_TOGISUMASU.id, 1, Random(0))).first.allies[0].currentMp)
    }

    /** レンジ0なので、他の個体は対象にできないこと。 */
    @Test
    fun togisumasu_otherTarget_returnsNull() {
        assertNull(field().performSkill(1, TEST_TOGISUMASU.id, 2, Random(0)))
    }
}
