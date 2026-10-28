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
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.SkillRepository
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkill
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkillEffect
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.PassiveSkill
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillCategory
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillContactType
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillCostType
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillTarget
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.UseSkillUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleSelection
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleSkillUiModel
import kotlinx.coroutines.test.runCurrent
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
     * @param allyMana 味方のマナ総量(最大MP)。
     * @param activeSkillIds 味方が設定しているアクティブスキルのID。
     * @return ViewModelと戦闘状態のRepository。
     */
    private fun createViewModel(
        enemyHp: Int = 1000,
        allyMana: Int = 0,
        activeSkillIds: List<Int> = emptyList(),
    ): Pair<BattleViewModel, FakeBattleRepository> {
        val battleRepository = FakeBattleRepository()
        val character = Character(
            id = 1, name = "味方", hp = 1000, mana = allyMana, attack = 100, defense = 100,
            manaOutput = 0, manaResistance = 0, agility = 1, luck = 0,
            activeSkillIds = activeSkillIds,
        )
        val enemy = Enemy(
            id = 1, name = "ゴブリン", hp = enemyHp, mana = 0, attack = 100, defense = 100,
            manaOutput = 0, manaResistance = 0, agility = 4, luck = 0,
        )
        val random = Random(0)
        val mapProgressRepository = FakeMapProgressRepository()
        val getBattleFieldUseCase = GetBattleFieldUseCase(FakeCharacterRepository(character), FakeEnemyRepository(enemy), FakeSkillRepository())
        val viewModel = BattleViewModel(
            formation = Formation(listOf(1, null, null, null, null, null)),
            startBattleUseCase = StartBattleUseCase(getBattleFieldUseCase, battleRepository, mapProgressRepository, random),
            observeBattleFieldUseCase = ObserveBattleFieldUseCase(battleRepository),
            attackUseCase = AttackUseCase(battleRepository),
            useSkillUseCase = UseSkillUseCase(battleRepository),
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

    /** 敵2体の攻撃(1回24ダメージ)を受けた直後の味方が、ディアで回復する一連の流れを検証する。 */
    @Test
    fun skillFlow_selectSkill_selectAlly_healsAndEndsTurn() = runTest(dispatcher) {
        val (viewModel, repository) = createViewModel(allyMana = 100, activeSkillIds = listOf(1))
        advanceUntilIdle()
        val allyFpid = repository.current.allies[0].fpid
        val state = viewModel.uiState.value
        assertEquals(listOf(BattleSkillUiModel(1, "ディア", 10, true)), state.skills)
        assertTrue(state.isSkillEnabled)
        assertEquals(100, state.partySlots[0]?.mana)
        assertEquals(100, state.partySlots[0]?.maxMana)

        viewModel.onSkillClick()
        assertEquals(BattleSelection.SkillList, viewModel.uiState.value.selection)
        assertEquals(BattleNarration.SelectSkill, viewModel.uiState.value.narration)
        assertFalse(viewModel.uiState.value.isAttackEnabled)

        viewModel.onSkillSelected(1)
        assertEquals(BattleSelection.SkillTarget(1), viewModel.uiState.value.selection)
        assertEquals(BattleNarration.SelectAllyTarget, viewModel.uiState.value.narration)

        viewModel.onAllyClick(allyFpid)
        viewModel.onAllyClick(allyFpid) // 連打しても二重に発動しない
        runCurrent()
        assertTrue(viewModel.uiState.value.isProcessing)
        assertEquals(BattleNarration.SkillResult("味方", "味方", "ディア", 48), viewModel.uiState.value.narration)

        advanceUntilIdle()

        val ally = repository.current.allies[0]
        // 回復で満タンに戻ったあと、味方の次の手番までに敵2体が素早さの差で2回ずつ(計4回)24ダメージを与える。
        assertEquals(1000 - 24 * 4, ally.currentHp)
        assertEquals(90, ally.currentMp)
        assertEquals(1, ally.turnCount)
        assertEquals(90, viewModel.uiState.value.partySlots[0]?.mana)
        assertEquals(BattleSelection.None, viewModel.uiState.value.selection)
    }

    /** MPが足りないスキルは一覧で使用不可になり、選んでも対象選択へ進まないこと。 */
    @Test
    fun skillSelection_insufficientMp_isIgnored() = runTest(dispatcher) {
        val (viewModel, _) = createViewModel(allyMana = 5, activeSkillIds = listOf(1))
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.skills.single().isUsable)
        viewModel.onSkillClick()
        viewModel.onSkillSelected(1)

        assertEquals(BattleSelection.SkillList, viewModel.uiState.value.selection)
    }

    /** スキルを持たない味方では「スキル」が無効で、押しても何も起きないこと。 */
    @Test
    fun noSkills_skillCommandDisabled() = runTest(dispatcher) {
        val (viewModel, _) = createViewModel(allyMana = 100)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSkillEnabled)
        viewModel.onSkillClick()
        assertEquals(BattleSelection.None, viewModel.uiState.value.selection)
    }

    /** スキル一覧・味方の対象選択のどちらからでも、キャンセルで行動選択に戻ること。 */
    @Test
    fun cancel_fromSkillListAndAllyTarget_returnsToActionSelection() = runTest(dispatcher) {
        val (viewModel, _) = createViewModel(allyMana = 100, activeSkillIds = listOf(1))
        advanceUntilIdle()

        viewModel.onSkillClick()
        viewModel.onCancelClick()
        assertEquals(BattleSelection.None, viewModel.uiState.value.selection)

        viewModel.onSkillClick()
        viewModel.onSkillSelected(1)
        viewModel.onCancelClick()
        assertEquals(BattleSelection.None, viewModel.uiState.value.selection)
        assertEquals(BattleNarration.SelectAction("味方"), viewModel.uiState.value.narration)
    }

    /** スキルの選択中は通常攻撃の対象選択へ移れず、攻撃の対象選択中はスキルを開けないこと。 */
    @Test
    fun attackAndSkillSelection_areMutuallyExclusive() = runTest(dispatcher) {
        val (viewModel, _) = createViewModel(allyMana = 100, activeSkillIds = listOf(1))
        advanceUntilIdle()

        viewModel.onSkillClick()
        viewModel.onAttackClick()
        assertEquals(BattleSelection.SkillList, viewModel.uiState.value.selection)

        viewModel.onCancelClick()
        viewModel.onAttackClick()
        viewModel.onSkillClick()
        assertEquals(BattleSelection.AttackTarget, viewModel.uiState.value.selection)
    }

    /** 対象選択でない状態で味方をタップしても、スキルは発動しないこと。 */
    @Test
    fun allyClick_withoutSelection_doesNothing() = runTest(dispatcher) {
        val (viewModel, repository) = createViewModel(allyMana = 100, activeSkillIds = listOf(1))
        advanceUntilIdle()

        viewModel.onAllyClick(repository.current.allies[0].fpid)
        advanceUntilIdle()

        assertEquals(100, repository.current.allies[0].currentMp)
    }

    /** テスト用の、ディア(MP10・50回復)だけを返す[SkillRepository]。 */
    private class FakeSkillRepository : SkillRepository {
        private val dia = ActiveSkill(
            id = 1, name = "ディア", categories = setOf(SkillCategory.HEAL, SkillCategory.STATUS),
            contactType = SkillContactType.NON_CONTACT, costType = SkillCostType.MP, cost = 10,
            target = SkillTarget.SingleAlly, effect = ActiveSkillEffect.Heal(50),
        )
        override suspend fun getActiveSkills(): List<ActiveSkill> = listOf(dia)
        override suspend fun getPassiveSkills(): List<PassiveSkill> = emptyList()
        override suspend fun getActiveSkill(id: Int): ActiveSkill? = dia.takeIf { it.id == id }
        override suspend fun getPassiveSkill(id: Int): PassiveSkill? = null
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
