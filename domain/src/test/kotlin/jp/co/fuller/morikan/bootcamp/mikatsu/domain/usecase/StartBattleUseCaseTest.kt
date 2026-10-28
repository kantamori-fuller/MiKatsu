package jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.testAlly
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.testEnemy
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
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
import kotlin.random.Random

/** [StartBattleUseCase]が戦闘開始時に進行度を1へ戻すことを検証するテスト。 */
class StartBattleUseCaseTest {

    /** 進行度が進んでいても、戦闘を始めると1に戻ること。 */
    @Test
    fun invoke_resetsProgressToOne() = runBlocking {
        val character = Character(
            id = 1, name = "味方", hp = 100, mana = 0, attack = 100, defense = 100,
            manaOutput = 0, manaResistance = 0, agility = 100, luck = 0,
        )
        val enemy = Enemy(
            id = 1, name = "敵", hp = 50, mana = 0, attack = 100, defense = 100,
            manaOutput = 0, manaResistance = 0, agility = 100, luck = 0,
        )
        val progress = FakeMapProgressRepository(initial = 5)
        val repo = FakeBattleRepository(BattleField(listOf(testAlly(1)), listOf(testEnemy(2))))
        val useCase = StartBattleUseCase(
            GetBattleFieldUseCase(
                object : CharacterRepository {
                    override fun observeCharacters(): Flow<List<Character>> = flowOf(listOf(character))
                    override suspend fun getCharacter(id: Int): Character? = character
                    override suspend fun saveCharacter(draft: CharacterDraft): Character = error("not used")
                    override suspend fun deleteCharacter(id: Int) = error("not used")
                },
                object : EnemyRepository {
                    override suspend fun getEnemies(): List<Enemy> = listOf(enemy)
                },
                FakeSkillRepository(),
            ),
            repo,
            progress,
            Random(0),
        )

        useCase(Formation(listOf(1, null, null, null, null, null)))

        assertEquals(1, progress.current)
    }
}
