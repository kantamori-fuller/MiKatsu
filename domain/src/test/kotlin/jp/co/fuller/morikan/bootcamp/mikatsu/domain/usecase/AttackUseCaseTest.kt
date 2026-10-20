package jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.testAlly
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.testEnemy
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AttackResult
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** [AttackUseCase]が手番の個体だけの攻撃を受け付け、指定したFPIDの個体にだけダメージを与えることを検証するテスト。 */
class AttackUseCaseTest {

    /** 味方FPID=1(手番)、同種の敵FPID=2,3を持つ戦闘フィールドを使うFake Repository。 */
    private val repository = FakeBattleRepository(
        BattleField(
            allies = listOf(testAlly(fpid = 1)),
            enemies = listOf(testEnemy(fpid = 2), testEnemy(fpid = 3)),
            turnQueue = listOf(1),
        ),
    )
    private val useCase = AttackUseCase(repository)

    /** 対象のHPだけが減り、味方のHPは変わらず、結果が返ること。 */
    @Test
    fun invoke_reducesOnlyTargetHp() = runBlocking {
        val result = useCase(attackerFpid = 1, targetFpid = 2)

        val field = repository.current
        assertEquals(AttackResult(attackerFpid = 1, targetFpid = 2, damage = 24), result)
        assertEquals(26, field.enemies[0].currentHp)
        assertEquals(50, field.enemies[1].currentHp)
        assertEquals(100, field.allies[0].currentHp)
    }

    /** 同じEIDの敵が2体いても、指定したFPIDの個体だけが減ること。 */
    @Test
    fun invoke_sameEid_reducesOnlySpecifiedFpid() = runBlocking {
        useCase(attackerFpid = 1, targetFpid = 3)

        val field = repository.current
        assertEquals(50, field.enemies[0].currentHp)
        assertEquals(26, field.enemies[1].currentHp)
    }

    /** 存在しないFPIDを指定するとnullを返し、何も変わらないこと。 */
    @Test
    fun invoke_unknownFpid_returnsNullAndChangesNothing() = runBlocking {
        val before = repository.current

        assertNull(useCase(attackerFpid = 1, targetFpid = 99))
        assertNull(useCase(attackerFpid = 99, targetFpid = 2))

        assertEquals(before, repository.current)
    }

    /** 手番ではない個体の攻撃はnullになり、何も変わらないこと。 */
    @Test
    fun invoke_notCurrentActor_returnsNullAndChangesNothing() = runBlocking {
        val before = repository.current

        assertNull(useCase(attackerFpid = 2, targetFpid = 1))

        assertEquals(before, repository.current)
    }

    /** 戦闘不能の個体への攻撃はnullになり、何も変わらないこと。 */
    @Test
    fun invoke_defeatedTarget_returnsNullAndChangesNothing() = runBlocking {
        val field = BattleField(
            allies = listOf(testAlly(fpid = 1)),
            enemies = listOf(testEnemy(fpid = 2).copy(currentHp = 0), testEnemy(fpid = 3)),
            turnQueue = listOf(1),
        )
        val repo = FakeBattleRepository(field)

        assertNull(AttackUseCase(repo)(attackerFpid = 1, targetFpid = 2))

        assertEquals(field, repo.current)
    }
}
