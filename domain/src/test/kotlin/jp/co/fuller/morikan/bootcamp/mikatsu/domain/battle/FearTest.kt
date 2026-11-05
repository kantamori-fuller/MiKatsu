package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BreakState
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.StatusAilment
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.TurnEndEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 恐怖が、自分の手番の開始時に30%でその手番だけスタンさせること(乱数の順・判定しない条件・先制の手番を含む)を検証するテスト。 */
class FearTest {

    /** 恐怖(残り3)の味方1が手番待ちの先頭にいる戦闘。敵は遅く、味方2もいる。 */
    private fun field(
        fear: Boolean = true,
        modify: (jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleAlly) -> jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleAlly = { it },
    ) = BattleField(
        allies = listOf(
            modify(testAlly(1).let { if (fear) it.withAilments(ail(StatusAilment.FEAR)) else it }),
            testAlly(2),
        ),
        enemies = listOf(testEnemy(10, agility = 1, hp = 100000)),
        turnQueue = listOf(1),
    )

    /** 判定の乱数が29ならスタンし、30ならスタンしないこと(30%の境界)。 */
    @Test
    fun boundary_29stuns_30doesNot() {
        val stunned = field().advanceToNextTurn(SequenceRandom(ints = listOf(0, 29)))
        val free = field().advanceToNextTurn(SequenceRandom(ints = listOf(0, 30)))

        assertTrue(stunned.pawn(1).isIncapacitated)
        assertFalse(free.pawn(1).isIncapacitated)
    }

    /** 判定は手番開始のTPの乱数(幅3)の後に1回(幅100)だけ引くこと。 */
    @Test
    fun roll_isDrawnAfterTurnStartTp() {
        val random = SequenceRandom(ints = listOf(0, 29))

        field().advanceToNextTurn(random)

        assertEquals(listOf(3, 100), random.untils)
    }

    /** 付くスタンは残り1ターン・手番中の付与ではなく・恐怖由来の印があること。 */
    @Test
    fun stun_isOneTurnNotGrantedAndMarkedByFear() {
        val stun = field().advanceToNextTurn(SequenceRandom(ints = listOf(0, 0))).pawn(1).ailments.first { it.ailment == StatusAilment.STUN }

        assertEquals(1, stun.remainingTurns)
        assertFalse(stun.grantedDuringOwnTurn)
        assertTrue(stun.causedByFear)
    }

    /** ブレイク中・すでに行動不能・恐怖でない個体には判定せず、乱数も引かないこと。 */
    @Test
    fun noRoll_whenBrokenIncapacitatedOrNotFearful() {
        val broken = SequenceRandom(ints = listOf(0, 0))
        val sleeping = SequenceRandom(ints = listOf(0, 0))
        val calm = SequenceRandom(ints = listOf(0, 0))

        field { it.copy(breakState = BreakState.BROKEN) }.advanceToNextTurn(broken)
        field { it.withAilments(ail(StatusAilment.FEAR), ail(StatusAilment.SLEEP)) }.advanceToNextTurn(sleeping)
        field(fear = false).advanceToNextTurn(calm)

        assertEquals(listOf(3), broken.untils)
        assertEquals(listOf(3), sleeping.untils)
        assertEquals(listOf(3), calm.untils)
    }

    /** 同時手番のくじで先頭になった個体(キューが空からの分岐)でも判定すること。 */
    @Test
    fun roll_alsoHappensWhenQueueIsBuiltFromScratch() {
        val random = SequenceRandom(ints = listOf(0, 0))
        val start = field().copy(turnQueue = emptyList(), allies = field().allies.map { if (it.fpid == 1) it.copy(remainingDistance = 0.0) else it })

        val next = start.advanceToNextTurn(random)

        assertEquals(1, next.currentActorFpid)
        assertTrue(next.pawn(1).isIncapacitated)
        assertEquals(100, random.untils.last())
    }

    /** スタンはその手番の終了で消え(恐怖の残りは1減る)、次の手番では30でスタンせず動けること。解除の文は出さない。 */
    @Test
    fun stun_endsWithTheTurn_andNextTurnIsFree() {
        val stunned = field().advanceToNextTurn(SequenceRandom(ints = listOf(0, 0)))

        val (ended, result) = stunned.endTurnWithEvents(1, SequenceRandom())

        assertFalse(ended.pawn(1).hasAilment(StatusAilment.STUN))
        assertEquals(2, ended.pawn(1).ailments.single { it.ailment == StatusAilment.FEAR }.remainingTurns)
        assertTrue(result.events.none { it is TurnEndEvent.Cured })
        val next = ended.copy(turnQueue = listOf(1)).advanceToNextTurn(SequenceRandom(ints = listOf(0, 30)))
        assertFalse(next.pawn(1).isIncapacitated)
    }

    /** 恐怖そのものは普通に残りターンが減り、0で解除されること。 */
    @Test
    fun fear_itselfCountsDownAndIsCured() {
        val f = field { it.withAilments(ail(StatusAilment.FEAR, turns = 1)) }

        val (ended, result) = f.endTurnWithEvents(1, SequenceRandom())

        assertFalse(ended.pawn(1).hasAilment(StatusAilment.FEAR))
        assertTrue(result.events.any { it is TurnEndEvent.Cured && it.ailment == StatusAilment.FEAR })
    }

    /** 先制の手番でも判定し、取り消さずにスタンとして手番が飛ばされること。 */
    @Test
    fun roll_alsoHappensOnPreemptiveTurn() {
        val shooter = testAlly(1, mp = 100, activeSkills = listOf(TEST_QUICK_SHOT)).copy(currentTp = 50).withAilments(ail(StatusAilment.FEAR))
        val f = BattleField(
            allies = listOf(shooter, testAlly(2)),
            enemies = listOf(testEnemy(10, agility = 1, hp = 100000)),
            turnQueue = listOf(2),
            pendingPreemptive = listOf(1),
        )
        val random = SequenceRandom(ints = listOf(0, 0))

        val (next, result) = f.endTurnWithEvents(2, random)

        assertEquals(1, next.currentActorFpid)
        assertTrue(next.isPreemptiveTurn)
        assertTrue(result.cancelledPreemptive.isEmpty())
        assertTrue(next.pawn(1).ailments.any { it.ailment == StatusAilment.STUN && it.causedByFear })
        assertEquals(100, random.untils.last())
    }

    /** 恐怖でスタンしたかどうかは、スタンの印([causedByFear])で分かり、通常のスタンとは区別できること。 */
    @Test
    fun ordinaryStun_isNotMarkedByFear() {
        val (after, _) = field(fear = false).inflictAilment(1, StatusAilment.STUN, 1)

        assertNull(after.pawn(1).ailments.firstOrNull { it.causedByFear })
    }
}
