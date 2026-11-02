package jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.TEST_DIA
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.TEST_OTHER_PASSIVE
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.TEST_STEEL_BODY
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
import org.junit.Assert.assertTrue
import org.junit.Test

/** [GetBattleFieldUseCase]が戦闘開始時にMPを満タンにし、設定されたスキルIDをマスターデータから解決することを検証するテスト。 */
class GetBattleFieldUseCaseSkillTest {

    private val skilled = Character(
        id = 1, name = "味方", hp = 100, mana = 40, attack = 100, defense = 100,
        manaOutput = 0, manaResistance = 0, agility = 100, luck = 0,
        activeSkillIds = listOf(1, 2, 77), passiveSkillIds = listOf(1),
    )
    private val plain = skilled.copy(id = 2, name = "素手", activeSkillIds = emptyList(), passiveSkillIds = emptyList())
    private val characters = listOf(skilled, plain)

    private val useCase = GetBattleFieldUseCase(
        object : CharacterRepository {
            override fun observeCharacters(): Flow<List<Character>> = flowOf(characters)
            override suspend fun getCharacter(id: Int): Character? = characters.firstOrNull { it.id == id }
            override suspend fun saveCharacter(draft: CharacterDraft): Character = error("not used")
            override suspend fun deleteCharacter(id: Int) = error("not used")
        },
        object : EnemyRepository {
            override suspend fun getEnemies(): List<Enemy> = emptyList()
        },
        FakeSkillRepository(),
    )

    /** アクティブのID1(ディア)とパッシブのID1(鋼の体)は別の種別として、それぞれ独立に解決されること。 */
    @Test
    fun invoke_sameIdInBothKinds_resolvedSeparately() = runBlocking {
        val onlyPassive = skilled.copy(id = 2, activeSkillIds = emptyList(), passiveSkillIds = listOf(2))
        val onlyActive = skilled.copy(id = 3, activeSkillIds = listOf(2), passiveSkillIds = emptyList())
        val all = listOf(onlyPassive, onlyActive)
        val useCase = GetBattleFieldUseCase(
            object : CharacterRepository {
                override fun observeCharacters(): Flow<List<Character>> = flowOf(all)
                override suspend fun getCharacter(id: Int): Character? = all.firstOrNull { it.id == id }
                override suspend fun saveCharacter(draft: CharacterDraft): Character = error("not used")
                override suspend fun deleteCharacter(id: Int) = error("not used")
            },
            object : EnemyRepository {
                override suspend fun getEnemies(): List<Enemy> = emptyList()
            },
            FakeSkillRepository(),
        )

        val field = useCase(Formation(listOf(2, 3, null, null, null, null)))

        assertEquals(TEST_DIA.id, TEST_OTHER_PASSIVE.id)
        assertTrue(field.allies[0].activeSkills.isEmpty())
        assertEquals(listOf(TEST_OTHER_PASSIVE), field.allies[0].passiveSkills)
        assertEquals(listOf(TEST_DIA), field.allies[1].activeSkills)
        assertTrue(field.allies[1].passiveSkills.isEmpty())
    }

    /** 旧データのAスキル1(以前はディア)は通常攻撃として解決されず、無視されること(上の設定にA1を含めている)。 */
    @Test
    fun invoke_staleActiveId1_isIgnored() = runBlocking {
        val field = useCase(Formation(listOf(1, null, null, null, null, null)))

        assertEquals(listOf(TEST_DIA), field.allies[0].activeSkills)
    }

    /** MPは最大MP(マナ総量)で始まり、スキルは解決され、未知のIDは無視され、FPIDの採番は変わらないこと。 */
    @Test
    fun invoke_resolvesSkillsAndStartsWithFullMp() = runBlocking {
        val field = useCase(Formation(listOf(1, 2, null, null, null, null)))

        assertEquals(listOf(40, 40), field.allies.map { it.currentMp })
        assertEquals(listOf(TEST_DIA), field.allies[0].activeSkills)
        assertEquals(listOf(TEST_STEEL_BODY), field.allies[0].passiveSkills)
        assertTrue(field.allies[1].activeSkills.isEmpty())
        assertTrue(field.allies[1].passiveSkills.isEmpty())
        assertEquals(listOf(1, 2), field.allies.map { it.fpid })
    }
}
