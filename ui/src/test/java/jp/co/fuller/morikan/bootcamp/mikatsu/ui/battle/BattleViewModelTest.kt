package jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.RandomEnemyTargetSelector
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleOutcome
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Character
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.CharacterDraft
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Enemy
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Formation
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.BattleRepository
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.CharacterRepository
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.EnemyRepository
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.AttackUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.EndTurnUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.ExecuteEnemyTurnUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.GetBattleFieldUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.ObserveBattleFieldUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.StartBattleUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleNarration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

/**
 * [BattleViewModel]の手番進行(敵の連続行動・味方への手番の切り替わり・勝利)を検証するテスト。
 *
 * 本物のUseCaseとFakeのRepositoryで組み立て、仮想時間で演出の間を進める。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BattleViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * 味方1体(素早さ1)と同種の敵2体(素早さ4)で戦闘を始めたViewModelと、その戦闘状態を返す。
     *
     * 敵2体は味方より先に1回ずつ行動し、そのあと味方の手番になる。
     *
     * @param enemyHp 敵のHP。
     * @return ViewModelと戦闘状態のRepository。
     */
    private fun createViewModel(enemyHp: Int = 1000): Pair<BattleViewModel, FakeBattleRepository> {
        val battleRepository = FakeBattleRepository()
        val character = Character(
            id = 1, name = "味方", hp = 1000, mana = 0, attack = 100, defense = 100,
            manaOutput = 0, manaResistance = 0, agility = 1, luck = 0,
        )
        val enemy = Enemy(
            id = 1, name = "ゴブリン", hp = enemyHp, mana = 0, attack = 100, defense = 100,
            manaOutput = 0, manaResistance = 0, agility = 4, luck = 0,
        )
        val random = Random(0)
        val viewModel = BattleViewModel(
            formation = Formation(listOf(1, null, null, null, null, null)),
            startBattleUseCase = StartBattleUseCase(
                GetBattleFieldUseCase(FakeCharacterRepository(character), FakeEnemyRepository(enemy)),
                battleRepository,
                random,
            ),
            observeBattleFieldUseCase = ObserveBattleFieldUseCase(battleRepository),
            attackUseCase = AttackUseCase(battleRepository),
            executeEnemyTurnUseCase = ExecuteEnemyTurnUseCase(battleRepository, RandomEnemyTargetSelector(random)),
            endTurnUseCase = EndTurnUseCase(battleRepository, random),
        )
        return viewModel to battleRepository
    }

    /** 敵が味方の手番までにちょうど1回ずつ攻撃し、同じ敵が続けて2回攻撃しないこと。 */
    @Test
    fun enemiesActExactlyOnceEachBeforeAllyTurn() = runTest(dispatcher) {
        val (viewModel, repository) = createViewModel()

        advanceUntilIdle()

        val field = repository.current
        assertEquals(listOf(1, 1), field.enemies.map { it.turnCount })
        assertEquals(1000 - 24 * 2, field.allies[0].currentHp)
        assertEquals(0, field.allies[0].turnCount)
        assertEquals(field.allies[0].fpid, field.currentActorFpid)
        assertTrue(viewModel.uiState.value.isPlayerTurn)
    }

    /** 敵から味方の手番に切り替わったあと、演出が終わり、ナレーションが行動選択になること。 */
    @Test
    fun afterEnemyTurns_allyTurnIsInteractive() = runTest(dispatcher) {
        val (viewModel, _) = createViewModel()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isProcessing)
        assertEquals(BattleNarration.SelectAction("味方"), state.narration)
        assertTrue(state.isAttackEnabled)
        assertEquals(listOf("ゴブリンA", "ゴブリンB"), state.enemies.map { it.name })
    }

    /** 最後の敵を倒すと勝利になり、それ以降は敵のジョブが起動しないこと。 */
    @Test
    fun defeatingLastEnemy_victory_andNoMoreEnemyTurns() = runTest(dispatcher) {
        val (viewModel, repository) = createViewModel(enemyHp = 1)
        advanceUntilIdle()

        var guard = 0
        while (viewModel.uiState.value.outcome == null && guard++ < 10) {
            assertTrue(viewModel.uiState.value.isPlayerTurn)
            viewModel.onAttackClick()
            val target = viewModel.uiState.value.enemies.first { !it.isDefeated }
            viewModel.onEnemyClick(target.id)
            advanceUntilIdle()
        }

        assertEquals(BattleOutcome.Victory, viewModel.uiState.value.outcome)
        val before: BattleField = repository.current
        advanceTimeBy(100_000)
        advanceUntilIdle()
        assertEquals(before, repository.current)
        assertFalse(viewModel.uiState.value.isProcessing)
        assertTrue(before.turnQueue.isEmpty())
    }

    /** テスト用の、1体のキャラクターだけを返す[CharacterRepository]。 */
    private class FakeCharacterRepository(private val character: Character) : CharacterRepository {
        override fun observeCharacters(): Flow<List<Character>> = MutableStateFlow(listOf(character))
        override suspend fun getCharacter(id: Int): Character? = character.takeIf { it.id == id }
        override suspend fun saveCharacter(draft: CharacterDraft): Character = error("not used")
        override suspend fun deleteCharacter(id: Int) = error("not used")
    }

    /** テスト用の、1種類の敵だけを返す[EnemyRepository]。 */
    private class FakeEnemyRepository(private val enemy: Enemy) : EnemyRepository {
        override suspend fun getEnemies(): List<Enemy> = listOf(enemy)
    }

    /** テスト用の、戦闘フィールドをメモリ上に保持する[BattleRepository]。 */
    private class FakeBattleRepository : BattleRepository {
        private val state = MutableStateFlow<BattleField?>(null)

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
