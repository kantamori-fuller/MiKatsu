package jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Character
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.CharacterDraft
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Enemy
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Formation
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.CharacterRepository
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.EnemyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/** [GetBattleFieldUseCase]が敵に出現順のスロットを割り当て、FPIDの採番ルールを変えないことを検証するテスト。 */
class GetBattleFieldUseCaseEnemySlotTest {

    private val character = Character(
        id = 1, name = "味方", hp = 100, mana = 0, attack = 100, defense = 100,
        manaOutput = 0, manaResistance = 0, agility = 100, luck = 0,
    )

    private fun enemy(id: Int) = Enemy(
        id = id, name = "敵$id", hp = 50, mana = 0, attack = 100, defense = 100,
        manaOutput = 0, manaResistance = 0, agility = 100, luck = 0,
    )

    private fun useCase(enemies: List<Enemy>) = GetBattleFieldUseCase(
        object : CharacterRepository {
            override fun observeCharacters(): Flow<List<Character>> = flowOf(listOf(character))
            override suspend fun getCharacter(id: Int): Character? = character.takeIf { it.id == id }
            override suspend fun saveCharacter(draft: CharacterDraft): Character = error("not used")
            override suspend fun deleteCharacter(id: Int) = error("not used")
        },
        object : EnemyRepository {
            override suspend fun getEnemies(): List<Enemy> = enemies
        },
        FakeSkillRepository(),
    )

    private val formation = Formation(listOf(1, null, null, null, null, null))

    /** 敵種2×2体(計4体)なら、スロット0〜3が出現順(前衛3体+後衛1体)で割り当てられ、FPIDは味方の次から連番であること。 */
    @Test
    fun invoke_assignsSlotsInSpawnOrder() = runBlocking {
        val field = useCase(listOf(enemy(1), enemy(2))).invoke(formation)

        assertEquals(listOf(0, 1, 2, 3), field.enemies.map { it.slot })
        assertEquals(listOf(2, 3, 4, 5), field.enemies.map { it.fpid })
        assertEquals(listOf(1, 1, 2, 2), field.enemies.map { it.enemy.id })
    }

    /** 次の戦闘でも同じ規則でスロットが割り当てられること。 */
    @Test
    fun createNextBattleField_assignsSlotsInSpawnOrder() = runBlocking {
        val useCase = useCase(listOf(enemy(1), enemy(2)))

        val next = useCase.createNextBattleField(useCase.invoke(formation))

        assertEquals(listOf(0, 1, 2, 3), next.enemies.map { it.slot })
    }

    /** 出現数がスロット数(6)を超える場合は6体で打ち切られること。 */
    @Test
    fun invoke_capsEnemiesAtSlotCount() = runBlocking {
        val field = useCase(listOf(enemy(1), enemy(2), enemy(3), enemy(4))).invoke(formation)

        assertEquals(listOf(0, 1, 2, 3, 4, 5), field.enemies.map { it.slot })
        assertEquals(listOf(1, 1, 2, 2, 3, 3), field.enemies.map { it.enemy.id })
    }
}
