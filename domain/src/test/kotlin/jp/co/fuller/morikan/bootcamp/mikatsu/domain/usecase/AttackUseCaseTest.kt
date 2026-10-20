package jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleAlly
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleEnemy
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Character
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Enemy
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.BattleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/** [AttackUseCase]が指定したFPIDの個体にだけダメージを与えることを検証するテスト。 */
class AttackUseCaseTest {

    private val character = Character(
        id = 1, name = "味方", hp = 100, mana = 10, attack = 100, defense = 100,
        manaOutput = 0, manaResistance = 0, agility = 0, luck = 0,
    )
    private val enemy = Enemy(
        id = 1, name = "敵", hp = 50, mana = 0, attack = 10, defense = 100,
        manaOutput = 0, manaResistance = 0, agility = 0, luck = 0,
    )

    /** 味方FPID=1、同じEIDの敵FPID=2,3を持つ戦闘フィールドを使うFake Repository。 */
    private val repository = FakeBattleRepository(
        BattleField(
            allies = listOf(BattleAlly(fpid = 1, character = character, currentHp = 100, slot = 0)),
            enemies = listOf(
                BattleEnemy(fpid = 2, enemy = enemy, currentHp = 50),
                BattleEnemy(fpid = 3, enemy = enemy, currentHp = 50),
            ),
        ),
    )
    private val useCase = AttackUseCase(repository)

    /** 対象のHPだけが減り、味方のHPは変わらないこと。 */
    @Test
    fun invoke_reducesOnlyTargetHp() = runBlocking {
        useCase(attackerFpid = 1, targetFpid = 2)

        val field = repository.current
        assertEquals(26, field.enemies[0].currentHp)
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

    /** HPは0未満にならないこと。 */
    @Test
    fun invoke_repeatedAttacks_hpNeverBelowZero() = runBlocking {
        repeat(5) { useCase(attackerFpid = 1, targetFpid = 2) }

        assertEquals(0, repository.current.enemies[0].currentHp)
    }

    /** 存在しないFPIDを指定しても何も変わらないこと。 */
    @Test
    fun invoke_unknownFpid_changesNothing() = runBlocking {
        val before = repository.current

        useCase(attackerFpid = 1, targetFpid = 99)
        useCase(attackerFpid = 99, targetFpid = 2)

        assertEquals(before, repository.current)
    }

    /**
     * 戦闘フィールドをメモリ上に保持するテスト用の[BattleRepository]。
     *
     * @param initial 初期状態の戦闘フィールド。
     */
    private class FakeBattleRepository(initial: BattleField) : BattleRepository {
        private val state = MutableStateFlow<BattleField?>(initial)

        /** 現在の戦闘フィールド。 */
        val current: BattleField get() = checkNotNull(state.value)

        override fun observeBattleField(): Flow<BattleField?> = state

        override suspend fun setBattleField(battleField: BattleField) {
            state.value = battleField
        }

        override suspend fun updateBattleField(transform: (BattleField) -> BattleField) {
            state.update { it?.let(transform) }
        }
    }
}
