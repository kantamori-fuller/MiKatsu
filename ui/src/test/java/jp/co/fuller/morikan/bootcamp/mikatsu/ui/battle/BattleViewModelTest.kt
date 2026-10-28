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
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.MapProgressRepository
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.AdvanceToNextBattleUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.ObserveMapProgressUseCase
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
        val mapProgressRepository = FakeMapProgressRepository()
        val getBattleFieldUseCase = GetBattleFieldUseCase(FakeCharacterRepository(character), FakeEnemyRepository(enemy))
        val viewModel = BattleViewModel(
            formation = Formation(listOf(1, null, null, null, null, null)),
            startBattleUseCase = StartBattleUseCase(getBattleFieldUseCase, battleRepository, mapProgressRepository, random),
            observeBattleFieldUseCase = ObserveBattleFieldUseCase(battleRepository),
            attackUseCase = AttackUseCase(battleRepository),
            executeEnemyTurnUseCase = ExecuteEnemyTurnUseCase(battleRepository, RandomEnemyTargetSelector(random)),
            endTurnUseCase = EndTurnUseCase(battleRepository, random),
            observeMapProgressUseCase = ObserveMapProgressUseCase(mapProgressRepository),
            advanceToNextBattleUseCase = AdvanceToNextBattleUseCase(
                getBattleFieldUseCase,
                battleRepository,
                mapProgressRepository,
                random,
            ),
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

    /** 勝利後に「次に進む」を押すと、勝敗が消えて敵が満タンで出し直され、進行度が2になること。 */
    @Test
    fun nextBattleClick_afterVictory_startsFreshBattle() = runTest(dispatcher) {
        val (viewModel, repository) = createViewModel(enemyHp = 1)
        advanceUntilIdle()
        var guard = 0
        while (viewModel.uiState.value.outcome == null && guard++ < 10) {
            viewModel.onAttackClick()
            viewModel.onEnemyClick(viewModel.uiState.value.enemies.first { !it.isDefeated }.id)
            advanceUntilIdle()
        }
        assertEquals(BattleOutcome.Victory, viewModel.uiState.value.outcome)
        assertEquals(1, viewModel.uiState.value.progress)

        viewModel.onNextBattleClick()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(null, state.outcome)
        assertEquals(2, state.progress)
        assertTrue(repository.current.enemies.all { it.currentHp == 1 })
        assertTrue(state.enemies.none { it.isDefeated })
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

    /** テスト用の、進行度をメモリ上に保持する[MapProgressRepository]。 */
    private class FakeMapProgressRepository : MapProgressRepository {
        private val state = MutableStateFlow(1)

        override fun observeProgress(): Flow<Int> = state

        override suspend fun reset() {
            state.value = 1
        }

        override suspend fun advance() {
            state.update { it + 1 }
        }
    }
}
