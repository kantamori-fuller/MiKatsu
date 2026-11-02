package jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.TEST_DIA
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.testAlly
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.testEnemy
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** [UseSkillUseCase]が効果をRepositoryへ反映し、使えない場合は何も変更しないことを検証するテスト。 */
class UseSkillUseCaseTest {

    private val initial = BattleField(
        allies = listOf(
            testAlly(1, mp = 10, activeSkills = listOf(TEST_DIA)),
            testAlly(2).copy(currentHp = 10),
        ),
        enemies = listOf(testEnemy(3)),
        turnQueue = listOf(1),
    )
    private val repository = FakeBattleRepository(initial)
    private val useCase = UseSkillUseCase(repository)

    /** 回復とMP消費がRepositoryに反映され、結果が返ること。 */
    @Test
    fun invoke_appliesEffect() = runBlocking {
        val result = useCase(actorFpid = 1, skillId = TEST_DIA.id, targetFpid = 2)

        assertEquals(50, result?.totalHealed)
        assertEquals(60, repository.current.allies[1].currentHp)
        assertEquals(0, repository.current.allies[0].currentMp)
    }

    /** 使えない場合はnullを返し、何も変わらないこと(2回目はMP不足)。 */
    @Test
    fun invoke_whenNotUsable_returnsNullAndChangesNothing() = runBlocking {
        useCase(actorFpid = 1, skillId = TEST_DIA.id, targetFpid = 2)
        val before = repository.current

        assertNull(useCase(actorFpid = 1, skillId = TEST_DIA.id, targetFpid = 2))

        assertEquals(before, repository.current)
    }
}
