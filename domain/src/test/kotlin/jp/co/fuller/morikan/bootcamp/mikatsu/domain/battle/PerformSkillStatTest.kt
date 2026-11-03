package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.StatModifier
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleStat
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BreakState
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.StatChangeResult
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.SkillTargetResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** ステータス変化スキル(レンジ0の精神一到・命中判定のある覇気)の[performSkill]を検証するテスト。 */
class PerformSkillStatTest {

    /** 味方1(手番・精神一到と覇気を所持・MP100)、味方2、敵3。 */
    private fun field(): BattleField = BattleField(
        allies = listOf(
            testAlly(1, mp = 100, activeSkills = listOf(TEST_SEISHIN, TEST_HAKI, TEST_DIA)),
            testAlly(2),
        ),
        enemies = listOf(testEnemy(3)),
        turnQueue = listOf(1, 3),
    )

    /** 精神一到は使用者自身にだけかかり、MPが消費され、破壊力+1が10ターンで記録され、結果にも入ること。 */
    @Test
    fun seishin_buffsSelfOnly() {
        val (after, result) = checkNotNull(field().performSkill(1, TEST_SEISHIN.id, 1, fixedRandom()))

        val modifier = after.allies[0].statModifiers.single()
        assertEquals(BattleStat.ATTACK, modifier.stat)
        assertEquals(1, modifier.stages)
        assertEquals(10, modifier.remainingTurns)
        assertTrue(modifier.grantedDuringOwnTurn)
        assertEquals(80, after.allies[0].currentMp)
        assertTrue(after.allies[1].statModifiers.isEmpty())
        assertEquals(
            listOf(SkillTargetResult(1, 0, true, listOf(StatChangeResult(BattleStat.ATTACK, 1)))),
            result.targets,
        )
    }

    /** レンジ0は他の個体を指定すると使えない(null)こと。 */
    @Test
    fun seishin_otherTarget_returnsNull() {
        assertNull(field().performSkill(1, TEST_SEISHIN.id, 2, Random(0)))
        assertNull(field().performSkill(1, TEST_SEISHIN.id, 3, Random(0)))
    }

    /** 命中率がnullの精神一到は乱数を引かないこと(引くとnextDouble以外のfixedRandomが尽きて失敗する)。 */
    @Test
    fun seishin_doesNotDrawRandom() {
        val random = object : Random() {
            override fun nextBits(bitCount: Int): Int = error("乱数を引いてはいけない")

            // 命中判定(nextDouble)は引かない。TPの獲得量(nextInt(until))だけは引いてよい。
            override fun nextDouble(): Double = error("命中判定の乱数を引いてはいけない")

            override fun nextInt(until: Int): Int = until - 1
        }

        assertNotNull(field().performSkill(1, TEST_SEISHIN.id, 1, random))
    }

    /** 覇気が命中すると、俊敏-1・耐久力-1が8ターンで同じ対象に記録され、結果に両方入ること。 */
    @Test
    fun haki_hit_appliesBothDebuffs() {
        val (after, result) = checkNotNull(field().performSkill(1, TEST_HAKI.id, 3, fixedRandom(0.5)))

        val target = after.enemies[0]
        assertEquals(setOf(BattleStat.AGILITY to -1, BattleStat.DEFENSE to -1), target.statModifiers.map { it.stat to it.stages }.toSet())
        assertTrue(target.statModifiers.all { it.remainingTurns == 8 && !it.grantedDuringOwnTurn })
        assertEquals(70, after.allies[0].currentMp)
        val targetResult = result.targets.single()
        assertTrue(targetResult.isHit)
        assertEquals(listOf(StatChangeResult(BattleStat.AGILITY, -1), StatChangeResult(BattleStat.DEFENSE, -1)), targetResult.statChanges)
    }

    /** 命中率100・命中率の段階-3(100×6/9≒66.7%)で外れた場合、どちらのデバフもかからず、コストは消費されること。 */
    @Test
    fun haki_miss_appliesNothing_butConsumesMp() {
        val f = field().let { it.copy(allies = it.allies.map { a -> if (a.fpid == 1) a.copy(statModifiers = listOf(StatModifier(BattleStat.ACCURACY, -3, 5))) else a }) }

        val (after, result) = checkNotNull(f.performSkill(1, TEST_HAKI.id, 3, fixedRandom(0.9)))

        assertTrue(after.enemies[0].statModifiers.isEmpty())
        assertEquals(70, after.allies[0].currentMp)
        assertEquals(listOf(SkillTargetResult(3, 0, false, emptyList())), result.targets)
    }

    /** 対象が回避率の高い場合も同じ判定式で外れること。 */
    @Test
    fun haki_usesTargetEvasion() {
        val f = field().let { it.copy(enemies = it.enemies.map { e -> e.copy(statModifiers = listOf(StatModifier(BattleStat.EVASION, 3, 5))) }) }

        val (_, result) = checkNotNull(f.performSkill(1, TEST_HAKI.id, 3, fixedRandom(0.7)))

        assertEquals(false, result.targets.single().isHit)
    }

    /** 対象がブレイク中なら、乱数を引かず必ず命中すること。 */
    @Test
    fun haki_brokenTarget_alwaysHits() {
        val f = field()
            .let { it.copy(allies = it.allies.map { a -> if (a.fpid == 1) a.copy(statModifiers = listOf(StatModifier(BattleStat.ACCURACY, -12, 5))) else a }) }
            .let { it.copy(enemies = it.enemies.map { e -> e.copy(breakState = BreakState.BROKEN) }) }
        val noDraw = object : Random() {
            override fun nextBits(bitCount: Int): Int = error("乱数を引いてはいけない")

            // 命中判定(nextDouble)は引かない。TPの獲得量(nextInt(until))だけは引いてよい。
            override fun nextDouble(): Double = error("命中判定の乱数を引いてはいけない")

            override fun nextInt(until: Int): Int = until - 1
        }

        val (after, result) = checkNotNull(f.performSkill(1, TEST_HAKI.id, 3, noDraw))

        assertTrue(result.targets.single().isHit)
        assertEquals(2, after.enemies[0].statModifiers.size)
    }

    /** 回復(ディア)は命中率nullなので従来どおり成功し、結果はisHit=trueでステータス変化なしであること。 */
    @Test
    fun dia_unchanged() {
        val f = field().let { it.copy(allies = it.allies.map { a -> if (a.fpid == 2) a.copy(currentHp = 10) else a }) }

        val (after, result) = checkNotNull(f.performSkill(1, TEST_DIA.id, 2, Random(0)))

        assertEquals(60, after.allies[1].currentHp)
        assertEquals(SkillTargetResult(2, 50), result.targets.single())
    }
}
