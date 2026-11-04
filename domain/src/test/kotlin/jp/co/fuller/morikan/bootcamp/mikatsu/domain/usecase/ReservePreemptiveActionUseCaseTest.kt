package jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.SequenceRandom
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.TEST_QUICK_SHOT
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.testAlly
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.testEnemy
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** [ReservePreemptiveActionUseCase]と[EndTurnUseCase]が、先制行動の予約・割り込み・取り消しの通知をRepository経由で行うことを検証するテスト。 */
class ReservePreemptiveActionUseCaseTest {

    /** 味方1(手番)・味方2(先制スキルあり)と、遅い敵3の戦闘フィールドを使うFake Repository。 */
    private val repository = FakeBattleRepository(
        BattleField(
            allies = listOf(
                testAlly(1, agility = 100),
                testAlly(2, agility = 100, mp = 100, activeSkills = listOf(TEST_QUICK_SHOT)).copy(currentTp = 50),
            ),
            enemies = listOf(testEnemy(3, agility = 1, hp = 100000)),
            turnQueue = listOf(1),
        ),
    )

    /** 予約できたら記録され、同じ個体の2回目・現在の行動者・敵の予約は受け付けられないこと。 */
    @Test
    fun reserve_recordsAndRejectsInvalid() = runBlocking {
        val useCase = ReservePreemptiveActionUseCase(repository)

        assertTrue(useCase(2))
        assertEquals(listOf(2), repository.current.pendingPreemptive)
        assertFalse(useCase(2))
        assertFalse(useCase(1))
        assertFalse(useCase(3))
        assertEquals(listOf(2), repository.current.pendingPreemptive)
    }

    /** 手番を終えると、予約した個体が先制の手番として割り込み、取り消しがなければ空の一覧が返ること。 */
    @Test
    fun endTurn_interrupts_andReturnsNoCancellation() = runBlocking {
        ReservePreemptiveActionUseCase(repository)(2)

        val cancelled = EndTurnUseCase(repository, SequenceRandom())(1).cancelledPreemptive

        assertTrue(cancelled.isEmpty())
        assertEquals(2, repository.current.currentActorFpid)
        assertTrue(repository.current.isPreemptiveTurn)
    }

    /** 予約後に先制スキルが使えなくなっていたら、手番を終えたときに取り消された個体のFPIDが返ること。 */
    @Test
    fun endTurn_returnsCancelledPreemptiveFpids() = runBlocking {
        ReservePreemptiveActionUseCase(repository)(2)
        repository.updateBattleField { f -> f.copy(allies = f.allies.map { if (it.fpid == 2) it.copy(currentMp = 0) else it }) }

        val cancelled = EndTurnUseCase(repository, SequenceRandom())(1).cancelledPreemptive

        assertEquals(listOf(2), cancelled)
        assertFalse(repository.current.isPreemptiveTurn)
    }

    /** 現在の行動者ではない個体の手番終了は、何も起こさず空の一覧を返すこと。 */
    @Test
    fun endTurn_nonActor_changesNothing() = runBlocking {
        val before = repository.current

        val cancelled = EndTurnUseCase(repository, SequenceRandom())(2).cancelledPreemptive

        assertTrue(cancelled.isEmpty())
        assertEquals(before, repository.current)
    }
}
