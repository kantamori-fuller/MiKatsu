package jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.ActionTimeline
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.BreakFormula
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.TEST_DIA
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.TEST_STEEL_BODY
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.testAlly
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.testEnemy
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleStat
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BreakState
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Character
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.CharacterDraft
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Enemy
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.StatModifier
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.CharacterRepository
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.EnemyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** [AdvanceToNextBattleUseCase]が勝利時だけ、味方のHPを引き継いで次の戦闘へ進めることを検証するテスト。 */
class AdvanceToNextBattleUseCaseTest {

    private val characters = listOf(1, 2).map {
        Character(
            id = it, name = "味方$it", hp = 100, mana = 100, attack = 100, defense = 100,
            manaOutput = 0, manaResistance = 0, agility = 100, luck = 0,
            activeSkillIds = listOf(1, 2, 99), passiveSkillIds = listOf(1, 1),
        )
    }
    private val goblin = Enemy(
        id = 1, name = "ゴブリン", hp = 50, mana = 0, attack = 100, defense = 100,
        manaOutput = 0, manaResistance = 0, agility = 100, luck = 0,
    )

    private fun createUseCase(
        field: BattleField,
        progress: FakeMapProgressRepository,
        repo: FakeBattleRepository,
    ) = AdvanceToNextBattleUseCase(
        GetBattleFieldUseCase(FakeCharacters(characters), FakeEnemies(listOf(goblin)), FakeSkillRepository()),
        repo,
        progress,
        Random(0),
    )

    /** 勝利した戦闘のフィールド。味方2は戦闘不能で、味方1は一部ダメージを受けており、敵は全滅している。 */
    private fun victoryField() = BattleField(
        allies = listOf(
            testAlly(1).copy(character = characters[0], slot = 0, currentHp = 40, currentMp = 30, turnCount = 3, remainingDistance = 123.0),
            testAlly(2).copy(character = characters[1], slot = 3, currentHp = 0, currentMp = 0, turnCount = 2, remainingDistance = 456.0),
        ),
        enemies = listOf(testEnemy(3).copy(currentHp = 0), testEnemy(4).copy(currentHp = 0)),
    )

    /** 勝利時、HP引き継ぎ・FPID再採番・敵の再生成・リセット・進行度加算が行われること。 */
    @Test
    fun victory_carriesOverAlliesAndAdvances() = runBlocking {
        val repo = FakeBattleRepository(victoryField())
        val progress = FakeMapProgressRepository()

        assertTrue(createUseCase(victoryField(), progress, repo)())

        val field = repo.current
        assertEquals(listOf(40, 0), field.allies.map { it.currentHp })
        assertEquals(listOf(30, 0), field.allies.map { it.currentMp })
        assertEquals(listOf(0, 3), field.allies.map { it.slot })
        // 存在しないスキルIDは無視され、重複して設定されたIDは1つに解決される。
        assertEquals(listOf(TEST_DIA), field.allies[0].activeSkills)
        assertEquals(listOf(TEST_STEEL_BODY), field.allies[0].passiveSkills)
        assertEquals(listOf(1, 2, 3, 4), field.allPawns.map { it.fpid })
        assertEquals(listOf(goblin.hp, goblin.hp), field.enemies.map { it.currentHp })
        assertEquals(listOf(0, 1), field.enemies.map { it.slot })
        assertTrue(field.enemies.all { it.turnCount == 0 })
        assertTrue(field.allies.all { it.turnCount == 0 })
        assertTrue(field.allPawns.all { it.remainingDistance <= ActionTimeline.BASE_DISTANCE })
        assertEquals(2, progress.current)
        assertTrue(field.currentActorFpid != null)
    }

    /** ブレイクゲージ・ブレイク耐性・ブレイクポイント倍率・ブレイク状態は持ち越さず、初期値に戻ること。 */
    @Test
    fun victory_resetsBreakValues() = runBlocking {
        val broken = victoryField().let { f ->
            f.copy(
                allies = f.allies.map {
                    it.copy(breakGauge = 55.5, breakResistance = 40, breakPointRate = 200, breakState = BreakState.BROKEN)
                },
            )
        }
        val repo = FakeBattleRepository(broken)

        assertTrue(createUseCase(broken, FakeMapProgressRepository(), repo)())

        assertTrue(
            repo.current.allPawns.all {
                it.breakGauge == 0.0 && it.breakResistance == BreakFormula.BASE_RESISTANCE &&
                    it.breakPointRate == BreakFormula.BASE_BREAK_POINT_RATE && it.breakState == BreakState.NONE
            },
        )
    }

    /** バフ・デバフは持ち越さず、次の戦闘では空から始まること(HP・MPのみ持ち越す)。 */
    @Test
    fun victory_doesNotCarryOverStatModifiers() = runBlocking {
        val buffed = victoryField().let { f ->
            f.copy(allies = f.allies.map { it.copy(statModifiers = listOf(StatModifier(BattleStat.ATTACK, 3, 7), StatModifier(BattleStat.ACCURACY, 2, 7), StatModifier(BattleStat.EVASION, -2, 7))) })
        }
        val repo = FakeBattleRepository(buffed)

        assertTrue(createUseCase(buffed, FakeMapProgressRepository(), repo)())

        assertTrue(repo.current.allPawns.all { it.statModifiers.isEmpty() })
        assertEquals(listOf(40, 0), repo.current.allies.map { it.currentHp })
    }

    /** 防御状態は持ち越さず、次の戦闘では全員が防御していない状態で始まること。 */
    @Test
    fun victory_doesNotCarryOverDefending() = runBlocking {
        val defending = victoryField().let { f -> f.copy(allies = f.allies.map { it.copy(isDefending = true) }) }
        val repo = FakeBattleRepository(defending)

        assertTrue(createUseCase(defending, FakeMapProgressRepository(), repo)())

        assertTrue(repo.current.allPawns.none { it.isDefending })
    }

    /** 未決着・敗北時は何も変わらないこと。 */
    @Test
    fun notVictory_changesNothing() = runBlocking {
        val ongoing = victoryField().copy(enemies = listOf(testEnemy(3), testEnemy(4)))
        val defeat = victoryField().copy(
            allies = victoryField().allies.map { it.copy(currentHp = 0) },
            enemies = listOf(testEnemy(3), testEnemy(4)),
        )
        for (field in listOf(ongoing, defeat)) {
            val repo = FakeBattleRepository(field)
            val progress = FakeMapProgressRepository()

            assertFalse(createUseCase(field, progress, repo)())

            assertEquals(field, repo.current)
            assertEquals(1, progress.current)
        }
    }

    private class FakeCharacters(private val list: List<Character>) : CharacterRepository {
        override fun observeCharacters(): Flow<List<Character>> = flowOf(list)
        override suspend fun getCharacter(id: Int): Character? = list.firstOrNull { it.id == id }
        override suspend fun saveCharacter(draft: CharacterDraft): Character = error("not used")
        override suspend fun deleteCharacter(id: Int) = error("not used")
    }

    private class FakeEnemies(private val list: List<Enemy>) : EnemyRepository {
        override suspend fun getEnemies(): List<Enemy> = list
    }
}
