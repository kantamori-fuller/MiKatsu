package jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.RandomEnemyTargetSelector
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.TEST_NORMAL_ATTACK
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.testAlly
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.testEnemy
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AttackResult
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AutoTurnResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** [ExecuteAutoTurnUseCase]が、敵の手番では生きている味方を攻撃し、洗脳・発狂・魅了の手番も正しく扱うことを検証するテスト。 */
class ExecuteAutoTurnUseCaseTest {

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
            val result = ExecuteAutoTurnUseCase(repo, FakeSkillRepository(), RandomEnemyTargetSelector(Random(seed)), Random(0))(actorFpid = 4)

            assertEquals(4, attacked(result).attackerFpid)
            assertEquals(listOf(2), attacked(result).hits.map { it.targetFpid })
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

        assertNull(ExecuteAutoTurnUseCase(repo, FakeSkillRepository(), RandomEnemyTargetSelector(Random(0)), Random(0))(actorFpid = 2))

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

        assertNull(ExecuteAutoTurnUseCase(repo, FakeSkillRepository(), RandomEnemyTargetSelector(Random(0)), Random(0))(actorFpid = 3))

        assertEquals(field, repo.current)
    }

    /** 敵の攻撃が、通常攻撃のスキルデータ(基礎威力・発動回数)を使うこと。 */
    @Test
    fun invoke_usesNormalAttackData() = runBlocking {
        val repo = FakeBattleRepository(
            BattleField(allies = listOf(testAlly(1)), enemies = listOf(testEnemy(2)), turnQueue = listOf(2)),
        )
        val skills = FakeSkillRepository(TEST_NORMAL_ATTACK.copy(basePower = 200, activationCount = 2))

        val result = ExecuteAutoTurnUseCase(repo, skills, RandomEnemyTargetSelector(Random(0)), Random(0))(actorFpid = 2)

        assertEquals(92, attacked(result).totalDamage) // 威力200の1ヒットは46、2回で92
        assertEquals(100 - 92, repo.current.allies[0].currentHp)
    }

    /** 敵が前衛なら、届く前衛の味方だけを狙い、後衛の味方は狙わないこと。 */
    @Test
    fun invoke_targetsOnlyReachableAllies() = runBlocking {
        repeat(30) { seed ->
            val repo = FakeBattleRepository(
                BattleField(
                    allies = listOf(testAlly(1, slot = 0), testAlly(2, slot = 3), testAlly(3, slot = 1)),
                    enemies = listOf(testEnemy(4, slot = 0)),
                    turnQueue = listOf(4),
                ),
            )

            val result = ExecuteAutoTurnUseCase(repo, FakeSkillRepository(), RandomEnemyTargetSelector(Random(seed)), Random(0))(actorFpid = 4)

            assertTrue(attacked(result).hits.single().targetFpid in listOf(1, 3))
            assertEquals(100, repo.current.allies[1].currentHp)
        }
    }

    /** 届く相手がいない(後衛の敵からレンジ1)とき、何もせず「届かない」を返し、フィールドも変わらないこと。 */
    @Test
    fun invoke_noReachableTarget_doesNothing() = runBlocking {
        val field = BattleField(
            allies = listOf(testAlly(1, slot = 0)),
            enemies = listOf(testEnemy(2, slot = 0), testEnemy(3, slot = 3)),
            turnQueue = listOf(3),
        )
        val repo = FakeBattleRepository(field)

        assertEquals(AutoTurnResult.CannotReach, ExecuteAutoTurnUseCase(repo, FakeSkillRepository(), RandomEnemyTargetSelector(Random(0)), Random(0))(actorFpid = 3))

        assertEquals(field, repo.current)
    }

    /** 前衛の敵が全滅して後衛が繰り上がると、後衛の敵も攻撃できること。 */
    @Test
    fun invoke_promotedBackEnemyAttacks() = runBlocking {
        val repo = FakeBattleRepository(
            BattleField(
                allies = listOf(testAlly(1, slot = 0)),
                enemies = listOf(testEnemy(2, slot = 0).copy(currentHp = 0), testEnemy(3, slot = 3)),
                turnQueue = listOf(3),
            ),
        )

        val result = ExecuteAutoTurnUseCase(repo, FakeSkillRepository(), RandomEnemyTargetSelector(Random(0)), Random(0))(actorFpid = 3)

        assertEquals(listOf(1), attacked(result).hits.map { it.targetFpid })
    }

    /**
     * 結果が通常攻撃(攻撃した)であることを確かめて、攻撃の結果を取り出す。
     *
     * @param result 自動行動の結果。
     * @return 攻撃の結果。
     */
    private fun attacked(result: AutoTurnResult?): AttackResult = (result as AutoTurnResult.Attacked).result
}
