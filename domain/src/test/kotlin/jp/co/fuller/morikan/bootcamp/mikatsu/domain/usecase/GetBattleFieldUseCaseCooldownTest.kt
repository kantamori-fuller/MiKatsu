package jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.testAlly
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Character
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.CharacterDraft
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Enemy
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.SkillCooldown
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.CharacterRepository
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.EnemyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 次の戦闘を作るとき、スキルのクールタイム・先制行動の予約が持ち越されずにリセットされることを検証するテスト。 */
class GetBattleFieldUseCaseCooldownTest {

    private val character = Character(
        id = 1, name = "味方", hp = 100, mana = 40, attack = 100, defense = 100,
        manaOutput = 0, manaResistance = 0, agility = 100, luck = 0,
    )
    private val enemy = Enemy(
        id = 1, name = "敵", hp = 50, mana = 0, attack = 100, defense = 100,
        manaOutput = 0, manaResistance = 0, agility = 100, luck = 0,
    )

    private val useCase = GetBattleFieldUseCase(
        object : CharacterRepository {
            override fun observeCharacters(): Flow<List<Character>> = flowOf(listOf(character))
            override suspend fun getCharacter(id: Int): Character? = character.takeIf { it.id == id }
            override suspend fun saveCharacter(draft: CharacterDraft): Character = error("not used")
            override suspend fun deleteCharacter(id: Int) = error("not used")
        },
        object : EnemyRepository {
            override suspend fun getEnemies(): List<Enemy> = listOf(enemy)
        },
        FakeSkillRepository(),
    )

    /** 前の戦闘でCT中だったスキルは、次の戦闘では使える状態に戻り、先制の予約・印も残らないこと。 */
    @Test
    fun createNextBattleField_resetsCooldownsAndReservations() = runBlocking {
        val previous = BattleField(
            allies = listOf(
                testAlly(1).copy(
                    character = character,
                    slot = 0,
                    skillCooldowns = listOf(SkillCooldown(skillId = 7, remainingTurns = 1)),
                ),
            ),
            enemies = emptyList(),
            turnQueue = listOf(1),
            pendingPreemptive = listOf(1),
            preemptiveTurns = setOf(1),
        )

        val next = useCase.createNextBattleField(previous)

        assertTrue(next.allies.single().skillCooldowns.isEmpty())
        assertTrue(next.enemies.all { it.skillCooldowns.isEmpty() })
        assertEquals(emptyList<Int>(), next.pendingPreemptive)
        assertEquals(emptySet<Int>(), next.preemptiveTurns)
    }
}
