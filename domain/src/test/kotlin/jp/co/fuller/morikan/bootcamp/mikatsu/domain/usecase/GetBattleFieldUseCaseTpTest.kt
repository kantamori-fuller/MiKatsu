package jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.testAlly
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

/** [GetBattleFieldUseCase]が、戦闘開始のTP(+10)の付与と、次の戦闘へのTPの持ち越しを行うことを検証するテスト。 */
class GetBattleFieldUseCaseTpTest {

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

    /** 冒険の最初の戦闘は味方・敵とも0から始まり、組み立てた時点で全個体に+10が加わること。 */
    @Test
    fun invoke_everyoneStartsWithTen() = runBlocking {
        val field = useCase(Formation(List(Formation.SLOT_COUNT) { if (it == 0) 1 else null }))

        assertEquals(listOf(10), field.allies.map { it.currentTp })
        assertEquals(listOf(10, 10), field.enemies.map { it.currentTp })
    }

    /** 戦闘開始時は味方・敵とも、現在HP=最大HP=種族値+80(満タン)で始まること。 */
    @Test
    fun invoke_startsWithFullHpPlusBaseHp() = runBlocking {
        val field = useCase(Formation(List(Formation.SLOT_COUNT) { if (it == 0) 1 else null }))

        assertEquals(listOf(180), field.allies.map { it.currentHp })
        assertEquals(listOf(180), field.allies.map { it.maxHp })
        assertEquals(listOf(130, 130), field.enemies.map { it.currentHp })
        assertEquals(listOf(130, 130), field.enemies.map { it.maxHp })
    }

    /** 次の戦闘へは味方のTPを持ち越して+10(上限100)し、敵は新しく0から+10になること。 */
    @Test
    fun createNextBattleField_carriesOverTpPlusTen() = runBlocking {
        val previous = BattleField(
            allies = listOf(
                testAlly(1).copy(character = character, currentTp = 35, slot = 0),
                testAlly(2).copy(character = character.copy(id = 2), currentTp = 95, slot = 1),
            ),
            enemies = emptyList(),
        )

        val next = useCase.createNextBattleField(previous)

        assertEquals(listOf(45, 100), next.allies.map { it.currentTp })
        assertEquals(listOf(10, 10), next.enemies.map { it.currentTp })
    }
}
