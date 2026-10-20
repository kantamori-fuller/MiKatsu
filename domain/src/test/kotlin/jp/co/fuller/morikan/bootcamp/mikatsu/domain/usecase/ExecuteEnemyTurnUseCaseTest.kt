package jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.RandomEnemyTargetSelector
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.testAlly
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.testEnemy
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** [ExecuteEnemyTurnUseCase]が敵の手番のときだけ、生きている味方を攻撃することを検証するテスト。 */
class ExecuteEnemyTurnUseCaseTest {

    /** 戦闘不能の味方を狙わず、生きている味方だけを攻撃すること。 */
    @Test
    fun invoke_targetsOnlyLivingAllies() = runBlocking {
        repeat(30) { seed ->
            val repo = FakeBattleRepository(
                BattleField(
                    allies = listOf(testAlly(1).copy(currentHp = 0), testAlly(2), testAlly(3).copy(currentHp = 0)),
                    enemies = listOf(testEnemy(4)),
                    turnQueue = listOf(4),
                ),
            )
            val result = ExecuteEnemyTurnUseCase(repo, RandomEnemyTargetSelector(Random(seed)))(actorFpid = 4)

            assertNotNull(result)
            assertEquals(4, result!!.attackerFpid)
            assertEquals(2, result.targetFpid)
            assertTrue(repo.current.allies[1].currentHp < 100)
        }
    }

    /** 現在の手番が味方ならnullを返し、何も変わらないこと。 */
    @Test
    fun invoke_allyTurn_returnsNullAndChangesNothing() = runBlocking {
        val field = BattleField(
            allies = listOf(testAlly(1)),
            enemies = listOf(testEnemy(2)),
            turnQueue = listOf(1),
        )
        val repo = FakeBattleRepository(field)

        assertNull(ExecuteEnemyTurnUseCase(repo, RandomEnemyTargetSelector(Random(0)))(actorFpid = 2))

        assertEquals(field, repo.current)
    }

    /** 現在の手番ではない敵のFPIDを渡すとnullを返し、何も変わらないこと。 */
    @Test
    fun invoke_notCurrentEnemy_returnsNullAndChangesNothing() = runBlocking {
        val field = BattleField(
            allies = listOf(testAlly(1)),
            enemies = listOf(testEnemy(2), testEnemy(3)),
            turnQueue = listOf(2),
        )
        val repo = FakeBattleRepository(field)

        assertNull(ExecuteEnemyTurnUseCase(repo, RandomEnemyTargetSelector(Random(0)))(actorFpid = 3))

        assertEquals(field, repo.current)
    }
}
