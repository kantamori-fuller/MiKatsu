package jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.RandomEnemyTargetSelector
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AilmentState
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleOutcome
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BreakState
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Character
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.CharacterDraft
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleStat
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.ElementResistances
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Enemy
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.StatModifier
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.StatusAilment
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Formation
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.BattleRepository
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.CharacterRepository
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.EnemyRepository
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.MapProgressRepository
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.SkillRepository
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkill
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkillEffect
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.PassiveSkill
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillCost
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillArea
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillCostType
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillRange
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillTargetSelection
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillTargetSide
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.testActiveSkill
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.UseSkillUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleSelection
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleSkillUiModel
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleUnitPosition
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.StatChangeDetail
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.StatChangeEntry
import kotlinx.coroutines.test.runCurrent
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.AdvanceToNextBattleUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.ObserveMapProgressUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.AttackUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.DefendUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.EndTurnUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.ExecuteEnemyTurnUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.GetBattleFieldUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.GetNormalAttackUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.ObserveBattleFieldUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.ReservePreemptiveActionUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.StartBattleUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.AreaHitEntry
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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
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
     * @param enemyKindCount 登場させる敵種の数。敵は1種につき2体ずつ出現するため、2種なら4体になり4体目が後衛(スロット3)に並ぶ。
     * @param allySlots 味方(すべて同じキャラクター)を配置する陣形のスロット番号(0〜2が前衛、3〜5が後衛)。
     * @param normalAttack 通常攻撃として使うスキル。
     * @param hitRandom 命中判定に使う乱数(味方・敵の通常攻撃で共通)。
     * @return ViewModelと戦闘状態のRepository。
     */
    private fun createViewModel(
        enemyHp: Int = 1000,
        allyMana: Int = 0,
        activeSkillIds: List<Int> = emptyList(),
        enemyKindCount: Int = 1,
        allySlots: List<Int> = listOf(0),
        normalAttack: ActiveSkill = DEFAULT_NORMAL_ATTACK,
        hitRandom: Random = ScriptedRandom(),
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
        val getBattleFieldUseCase = GetBattleFieldUseCase(FakeCharacterRepository(character), FakeEnemyRepository(List(enemyKindCount) { enemy }), FakeSkillRepository(normalAttack))
        val viewModel = BattleViewModel(
            formation = Formation(List(Formation.SLOT_COUNT) { if (it in allySlots) 1 else null }),
            startBattleUseCase = StartBattleUseCase(getBattleFieldUseCase, battleRepository, mapProgressRepository, random),
            observeBattleFieldUseCase = ObserveBattleFieldUseCase(battleRepository),
            attackUseCase = AttackUseCase(battleRepository, FakeSkillRepository(normalAttack), hitRandom),
            getNormalAttackUseCase = GetNormalAttackUseCase(FakeSkillRepository(normalAttack)),
            useSkillUseCase = UseSkillUseCase(battleRepository, random),
            defendUseCase = DefendUseCase(battleRepository),
            executeEnemyTurnUseCase = ExecuteEnemyTurnUseCase(battleRepository, FakeSkillRepository(normalAttack), RandomEnemyTargetSelector(random), hitRandom),
            endTurnUseCase = EndTurnUseCase(battleRepository, random),
            observeMapProgressUseCase = ObserveMapProgressUseCase(mapProgressRepository),
            advanceToNextBattleUseCase = AdvanceToNextBattleUseCase(
                getBattleFieldUseCase,
                battleRepository,
                mapProgressRepository,
                random,
            ),
            reservePreemptiveActionUseCase = ReservePreemptiveActionUseCase(battleRepository),
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
        assertEquals(listOf("ゴブリンA", "ゴブリンB", null, null, null, null), state.enemySlots.map { it?.name })
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
            val target = viewModel.uiState.value.enemySlots.filterNotNull().first { !it.isDefeated }
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
            viewModel.onEnemyClick(viewModel.uiState.value.enemySlots.filterNotNull().first { !it.isDefeated }.id)
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
        assertTrue(state.enemySlots.filterNotNull().none { it.isDefeated })
    }

    /** 敵2体の攻撃(1回24ダメージ)を受けた直後の味方が、ディアで回復する一連の流れを検証する。 */
    @Test
    fun skillFlow_selectSkill_selectAlly_healsAndEndsTurn() = runTest(dispatcher) {
        val (viewModel, repository) = createViewModel(allyMana = 100, activeSkillIds = listOf(2))
        advanceUntilIdle()
        val allyFpid = repository.current.allies[0].fpid
        val state = viewModel.uiState.value
        assertEquals(listOf(BattleSkillUiModel(2, "ディア", 10, 0, setOf(allyFpid), true)), state.skills)
        assertTrue(state.isSkillEnabled)
        assertEquals(100, state.partySlots[0]?.mana)
        assertEquals(100, state.partySlots[0]?.maxMana)

        viewModel.onSkillClick()
        assertEquals(BattleSelection.SkillList, viewModel.uiState.value.selection)
        assertEquals(BattleNarration.SelectSkill, viewModel.uiState.value.narration)
        assertFalse(viewModel.uiState.value.isAttackEnabled)

        viewModel.onSkillSelected(2)
        assertEquals(BattleSelection.SkillTarget(2), viewModel.uiState.value.selection)
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
        val (viewModel, _) = createViewModel(allyMana = 5, activeSkillIds = listOf(2))
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.skills.single().isUsable)
        viewModel.onSkillClick()
        viewModel.onSkillSelected(2)

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
        val (viewModel, _) = createViewModel(allyMana = 100, activeSkillIds = listOf(2))
        advanceUntilIdle()

        viewModel.onSkillClick()
        viewModel.onCancelClick()
        assertEquals(BattleSelection.None, viewModel.uiState.value.selection)

        viewModel.onSkillClick()
        viewModel.onSkillSelected(2)
        viewModel.onCancelClick()
        assertEquals(BattleSelection.None, viewModel.uiState.value.selection)
        assertEquals(BattleNarration.SelectAction("味方"), viewModel.uiState.value.narration)
    }

    /** スキルの選択中は通常攻撃の対象選択へ移れず、攻撃の対象選択中はスキルを開けないこと。 */
    @Test
    fun attackAndSkillSelection_areMutuallyExclusive() = runTest(dispatcher) {
        val (viewModel, _) = createViewModel(allyMana = 100, activeSkillIds = listOf(2))
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
        val (viewModel, repository) = createViewModel(allyMana = 100, activeSkillIds = listOf(2))
        advanceUntilIdle()

        viewModel.onAllyClick(repository.current.allies[0].fpid)
        advanceUntilIdle()

        assertEquals(100, repository.current.allies[0].currentMp)
    }

    /** 攻撃の対象選択中は届く敵だけが選べる相手になり、敵のカードをタップして通常攻撃できること。 */
    @Test
    fun attackTargets_areReachableEnemies_andReflectedInSelectableIds() = runTest(dispatcher) {
        val (viewModel, repository) = createViewModel()
        advanceUntilIdle()
        val enemyFpids = repository.current.enemies.map { it.fpid }.toSet()

        assertEquals(enemyFpids, viewModel.uiState.value.attackTargetIds)
        assertEquals(emptySet<Int>(), viewModel.uiState.value.selectableTargetIds)

        viewModel.onAttackClick()
        assertEquals(enemyFpids, viewModel.uiState.value.selectableTargetIds)

        // 味方(選べない相手)をタップしても何も起きない。
        viewModel.onAllyClick(repository.current.allies[0].fpid)
        runCurrent()
        assertFalse(viewModel.uiState.value.isProcessing)

        viewModel.onEnemyClick(enemyFpids.first())
        runCurrent()
        assertTrue(viewModel.uiState.value.isProcessing)
        assertEquals(BattleNarration.AttackResult("味方", "ゴブリンA", 24), viewModel.uiState.value.narration)
    }

    /** 前衛が生きている間、後衛の味方はレンジ1の通常攻撃が届かず、攻撃ボタンが無効になること。 */
    @Test
    fun backRowAlly_cannotAttackWithRangeOne_whileFrontAlive() = runTest(dispatcher) {
        val (viewModel, _) = createViewModel(allySlots = listOf(0, 3))
        advanceUntilIdle()

        var guard = 0
        while (viewModel.uiState.value.partySlots[3]?.isActive != true && guard++ < 10) {
            // 前衛の味方の手番では通常攻撃が使えて、攻撃すると手番が進む。
            assertTrue(viewModel.uiState.value.isAttackEnabled)
            viewModel.onAttackClick()
            viewModel.onEnemyClick(viewModel.uiState.value.attackTargetIds.first())
            advanceUntilIdle()
        }

        val state = viewModel.uiState.value
        assertTrue(state.partySlots[3]?.isActive == true)
        assertTrue(state.isPlayerTurn)
        assertEquals(emptySet<Int>(), state.attackTargetIds)
        assertFalse(state.isAttackEnabled)
        viewModel.onAttackClick()
        assertEquals(BattleSelection.None, viewModel.uiState.value.selection)
    }

    /** 前衛がいない陣営の後衛は前衛として扱われ、後衛に1人だけでもレンジ1の通常攻撃が届くこと。 */
    @Test
    fun lonelyBackRowAlly_isPromotedToFront() = runTest(dispatcher) {
        val (viewModel, _) = createViewModel(allySlots = listOf(3))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.partySlots[0])
        assertEquals("味方", state.partySlots[3]?.name)
        assertEquals(2, state.attackTargetIds.size)
        assertTrue(state.isAttackEnabled)
    }

    /** スキルの対象選択中は、そのスキルで選べる相手(味方)だけがタップでき、敵をタップしても何も起きないこと。 */
    @Test
    fun skillTargets_selectableIdsFollowSelectedSkill() = runTest(dispatcher) {
        val (viewModel, repository) = createViewModel(allyMana = 100, activeSkillIds = listOf(2))
        advanceUntilIdle()
        val allyFpid = repository.current.allies[0].fpid

        viewModel.onSkillClick()
        assertEquals(emptySet<Int>(), viewModel.uiState.value.selectableTargetIds)
        viewModel.onSkillSelected(2)
        assertEquals(setOf(allyFpid), viewModel.uiState.value.selectableTargetIds)

        viewModel.onEnemyClick(repository.current.enemies[0].fpid)
        runCurrent()
        assertFalse(viewModel.uiState.value.isProcessing)
        assertEquals(BattleSelection.SkillTarget(2), viewModel.uiState.value.selection)
    }

    /** 選べる相手がいないスキルは、コストを払えても使用不可になること(前衛の味方からは届くが、後衛の味方からは届かない)。 */
    @Test
    fun skillWithoutTargets_isNotUsable() = runTest(dispatcher) {
        val (viewModel, _) = createViewModel(activeSkillIds = listOf(FAR_STRIKE_ID), allySlots = listOf(0, 3))
        advanceUntilIdle()

        var guard = 0
        while (viewModel.uiState.value.partySlots[3]?.isActive != true && guard++ < 10) {
            val skill = viewModel.uiState.value.skills.single()
            assertTrue(skill.targetIds.isNotEmpty())
            assertTrue(skill.isUsable)
            viewModel.onAttackClick()
            viewModel.onEnemyClick(viewModel.uiState.value.attackTargetIds.first())
            advanceUntilIdle()
        }

        val skill = viewModel.uiState.value.skills.single()
        assertEquals(emptySet<Int>(), skill.targetIds)
        assertFalse(skill.isUsable)
        viewModel.onSkillClick()
        viewModel.onSkillSelected(FAR_STRIKE_ID)
        assertNotEquals(BattleSelection.SkillTarget(FAR_STRIKE_ID), viewModel.uiState.value.selection)
    }

    /** 範囲(全て対象)の通常攻撃は「Aの攻撃!」から始まる複数対象のナレーションになり、全員にダメージが入ること。 */
    @Test
    fun areaAttack_showsMultiTargetNarration() = runTest(dispatcher) {
        val areaAttack = DEFAULT_NORMAL_ATTACK.copy(area = SkillArea.AREA, targetSelection = SkillTargetSelection.ALL)
        val (viewModel, repository) = createViewModel(normalAttack = areaAttack)
        advanceUntilIdle()

        viewModel.onAttackClick()
        viewModel.onEnemyClick(repository.current.enemies[0].fpid)
        runCurrent()

        assertEquals(BattleNarration.AreaAttackStart("味方"), viewModel.uiState.value.narration)
        advanceTimeBy(RESULT_MS)
        runCurrent()
        assertEquals(
            BattleNarration.AreaHit(listOf(AreaHitEntry("ゴブリンA", 24), AreaHitEntry("ゴブリンB", 24))),
            viewModel.uiState.value.narration,
        )
        assertEquals(listOf(976, 976), repository.current.enemies.map { it.currentHp })
    }

    /** 多段攻撃のナレーション中は、その文の時点のHPが表示され、次の文へ進むと次のヒットのHPになり、終了後に最終状態になること。 */
    @Test
    fun multiShot_displayFollowsNarration() = runTest(dispatcher) {
        val rolls = ScriptedRandom()
        val (viewModel, repository) = createViewModel(
            normalAttack = DEFAULT_NORMAL_ATTACK.copy(accuracy = 90, activationCount = 2),
            hitRandom = rolls,
        )
        advanceUntilIdle()
        rolls.queue(0.5, 0.5)

        viewModel.onAttackClick()
        viewModel.onEnemyClick(repository.current.enemies[0].fpid)
        runCurrent()

        // リポジトリは2発とも反映済みでも、1文目の表示は1発目の時点。
        assertEquals(BattleNarration.ShotHit(1, 24), viewModel.uiState.value.narration)
        assertEquals(1000 - 48, repository.current.enemies[0].currentHp)
        assertEquals(1000 - 24, viewModel.uiState.value.enemySlots[0]?.hp)

        viewModel.onNarrationClick()
        runCurrent()
        assertEquals(BattleNarration.ShotHit(2, 24), viewModel.uiState.value.narration)
        assertEquals(1000 - 48, viewModel.uiState.value.enemySlots[0]?.hp)

        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isProcessing)
        assertEquals(1000 - 48, viewModel.uiState.value.enemySlots[0]?.hp)
    }

    /** 対象1体・1回攻撃が外れると「しかし外れた!」の1文だけになり、HPは減らず、1文のあと手番が終わること。 */
    @Test
    fun singleTarget_oneShot_miss() = runTest(dispatcher) {
        val rolls = ScriptedRandom()
        val (viewModel, repository) = createViewModel(normalAttack = DEFAULT_NORMAL_ATTACK.copy(accuracy = 90), hitRandom = rolls)
        advanceUntilIdle()
        rolls.queue(0.95)

        viewModel.onAttackClick()
        viewModel.onEnemyClick(repository.current.enemies[0].fpid)
        runCurrent()

        assertEquals(BattleNarration.AttackMissed("味方", "ゴブリンA"), viewModel.uiState.value.narration)
        assertEquals(1000, repository.current.enemies[0].currentHp)
        assertEquals(0, repository.current.allies[0].turnCount)
        advanceTimeBy(RESULT_MS)
        runCurrent()
        assertEquals(1, repository.current.allies[0].turnCount)
    }

    /** 対象1体・2回攻撃で1発目が当たり2発目が外れると、1発ごとの文とまとめの文が順に並ぶこと。 */
    @Test
    fun singleTarget_twoShots_hitThenMiss_showsEachShotAndTotal() = runTest(dispatcher) {
        val rolls = ScriptedRandom()
        val (viewModel, repository) = createViewModel(
            normalAttack = DEFAULT_NORMAL_ATTACK.copy(accuracy = 90, activationCount = 2),
            hitRandom = rolls,
        )
        advanceUntilIdle()
        rolls.queue(0.5, 0.95)

        viewModel.onAttackClick()
        viewModel.onEnemyClick(repository.current.enemies[0].fpid)
        runCurrent()

        assertEquals(BattleNarration.ShotHit(1, 24), viewModel.uiState.value.narration)
        advanceTimeBy(RESULT_MS)
        runCurrent()
        assertEquals(BattleNarration.ShotMissed(2), viewModel.uiState.value.narration)
        advanceTimeBy(RESULT_MS)
        runCurrent()
        assertEquals(BattleNarration.AttackTotal("味方", "ゴブリンA", 24, 2, 1), viewModel.uiState.value.narration)
        assertEquals(976, repository.current.enemies[0].currentHp)
    }

    /** 対象1体・2回攻撃が全発外れると、1発ごとの表示をせず「しかし外れた!」の1文だけになること。 */
    @Test
    fun singleTarget_twoShots_allMiss_isSingleMissSentence() = runTest(dispatcher) {
        val rolls = ScriptedRandom()
        val (viewModel, repository) = createViewModel(
            normalAttack = DEFAULT_NORMAL_ATTACK.copy(accuracy = 90, activationCount = 2),
            hitRandom = rolls,
        )
        advanceUntilIdle()
        rolls.queue(0.95, 0.95)

        viewModel.onAttackClick()
        viewModel.onEnemyClick(repository.current.enemies[0].fpid)
        runCurrent()
        assertEquals(BattleNarration.AttackMissed("味方", "ゴブリンA"), viewModel.uiState.value.narration)
        advanceTimeBy(RESULT_MS)
        runCurrent()

        assertEquals(1, repository.current.allies[0].turnCount)
    }

    /** 範囲攻撃で一部が外れると、開始・外れた相手のまとめ・命中した相手のまとめの順に表示されること。 */
    @Test
    fun area_someMiss_showsStartMissedAndHit() = runTest(dispatcher) {
        val rolls = ScriptedRandom()
        val area = DEFAULT_NORMAL_ATTACK.copy(
            area = SkillArea.AREA, targetSelection = SkillTargetSelection.ALL, accuracy = 90,
        )
        val (viewModel, repository) = createViewModel(normalAttack = area, hitRandom = rolls)
        advanceUntilIdle()
        rolls.queue(0.95, 0.5)

        viewModel.onAttackClick()
        viewModel.onEnemyClick(repository.current.enemies[0].fpid)
        runCurrent()

        val seen = mutableListOf(viewModel.uiState.value.narration)
        repeat(2) {
            advanceTimeBy(RESULT_MS)
            runCurrent()
            seen += viewModel.uiState.value.narration
        }
        assertEquals(
            listOf(
                BattleNarration.AreaAttackStart("味方"),
                BattleNarration.AreaMissed(listOf("ゴブリンA")),
                BattleNarration.AreaHit(listOf(AreaHitEntry("ゴブリンB", 24))),
            ),
            seen,
        )
    }

    /** 範囲攻撃が全員に外れると「Aの攻撃! しかし外れた!」の1文だけになること。 */
    @Test
    fun area_allMiss_isSingleSentence() = runTest(dispatcher) {
        val rolls = ScriptedRandom()
        val area = DEFAULT_NORMAL_ATTACK.copy(
            area = SkillArea.AREA, targetSelection = SkillTargetSelection.ALL, accuracy = 90,
        )
        val (viewModel, repository) = createViewModel(normalAttack = area, hitRandom = rolls)
        advanceUntilIdle()
        rolls.queue(0.95, 0.95)

        viewModel.onAttackClick()
        viewModel.onEnemyClick(repository.current.enemies[0].fpid)
        runCurrent()
        assertEquals(BattleNarration.AreaAttackAllMissed("味方"), viewModel.uiState.value.narration)
        advanceTimeBy(RESULT_MS)
        runCurrent()

        assertEquals(1, repository.current.allies[0].turnCount)
    }

    /** ナレーション欄のタップで、待ちを打ち切って次の文へ進み、最後の文なら手番を終えること。 */
    @Test
    fun narrationTap_advancesToNextSentence_andEndsTurnAfterLast() = runTest(dispatcher) {
        val rolls = ScriptedRandom()
        val (viewModel, repository) = createViewModel(
            normalAttack = DEFAULT_NORMAL_ATTACK.copy(accuracy = 90, activationCount = 2),
            hitRandom = rolls,
        )
        advanceUntilIdle()
        rolls.queue(0.5, 0.95)
        viewModel.onAttackClick()
        viewModel.onEnemyClick(repository.current.enemies[0].fpid)
        runCurrent()
        assertEquals(BattleNarration.ShotHit(1, 24), viewModel.uiState.value.narration)

        viewModel.onNarrationClick()
        runCurrent()
        assertEquals(BattleNarration.ShotMissed(2), viewModel.uiState.value.narration)
        viewModel.onNarrationClick()
        runCurrent()
        assertEquals(BattleNarration.AttackTotal("味方", "ゴブリンA", 24, 2, 1), viewModel.uiState.value.narration)
        assertEquals(0, repository.current.allies[0].turnCount)

        viewModel.onNarrationClick()
        runCurrent()
        assertEquals(1, repository.current.allies[0].turnCount)
    }

    /** 待っていない間のタップは捨てられ、次に表示される文が飛ばされないこと。1回のタップで進むのは1文だけであること。 */
    @Test
    fun narrationTap_whenNotWaiting_isDiscarded_andOneTapAdvancesOneSentence() = runTest(dispatcher) {
        val rolls = ScriptedRandom()
        val (viewModel, repository) = createViewModel(
            normalAttack = DEFAULT_NORMAL_ATTACK.copy(accuracy = 90, activationCount = 2),
            hitRandom = rolls,
        )
        advanceUntilIdle()
        val selectAction = viewModel.uiState.value.narration
        viewModel.onNarrationClick() // 演出中でない: 何も起きない
        runCurrent()
        assertEquals(selectAction, viewModel.uiState.value.narration)

        rolls.queue(0.5, 0.95)
        viewModel.onAttackClick()
        viewModel.onEnemyClick(repository.current.enemies[0].fpid)
        viewModel.onNarrationClick() // 演出ジョブが文を表示する前のタップは捨てられる
        runCurrent()
        assertEquals(BattleNarration.ShotHit(1, 24), viewModel.uiState.value.narration)

        viewModel.onNarrationClick()
        viewModel.onNarrationClick() // 同時の2回目は同じ文の待ちに対するもので、無視される
        runCurrent()
        assertEquals(BattleNarration.ShotMissed(2), viewModel.uiState.value.narration)
    }

    /** 最後の文で何度タップしても、手番の終了は1回だけであること。 */
    @Test
    fun narrationTap_repeatedOnLastSentence_endsTurnOnce() = runTest(dispatcher) {
        val (viewModel, repository) = createViewModel()
        advanceUntilIdle()
        viewModel.onAttackClick()
        viewModel.onEnemyClick(repository.current.enemies[0].fpid)
        runCurrent()
        assertEquals(BattleNarration.AttackResult("味方", "ゴブリンA", 24), viewModel.uiState.value.narration)

        repeat(5) { viewModel.onNarrationClick() }
        runCurrent()

        assertEquals(1, repository.current.allies[0].turnCount)
        // 敵の手番が連鎖し、連打したタップは敵の演出を飛ばさない(敵の行動の文が表示されている)。
        // 同時に手番が来た敵の順番はTPの乱数で変わるため、いま手番の敵の名前で確かめる。
        val actorName = viewModel.uiState.value.nameOf(checkNotNull(repository.current.currentActorFpid))
        assertEquals(BattleNarration.EnemyTurn(actorName), viewModel.uiState.value.narration)
        advanceUntilIdle()
        assertEquals(1, repository.current.allies[0].turnCount)
        assertTrue(viewModel.uiState.value.isPlayerTurn) // 敵の手番を終えて、次は味方の手番に戻る
    }

    /** 敵の手番の「◯◯の行動」もタップで打ち切られ、すぐ攻撃が実行されること。 */
    @Test
    fun narrationTap_skipsEnemyTurnIntro() = runTest(dispatcher) {
        val (viewModel, repository) = createViewModel()
        runCurrent()
        assertEquals(BattleNarration.EnemyTurn("ゴブリンA"), viewModel.uiState.value.narration)
        assertEquals(1000, repository.current.allies[0].currentHp)

        viewModel.onNarrationClick()
        runCurrent()

        assertEquals(1000 - 24, repository.current.allies[0].currentHp)
        assertEquals(BattleNarration.AttackResult("ゴブリンA", "味方", 24), viewModel.uiState.value.narration)
    }

    /** ブレイク中の味方の手番は操作を受け付けず、ナレーションを順に表示して自動で終わり、立ち上がり状態になること。 */
    @Test
    fun brokenAlly_turnIsSkippedAutomatically() = runTest(dispatcher) {
        val (viewModel, repository) = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isAttackEnabled)

        repository.updateBattleField { f -> f.copy(allies = f.allies.map { it.copy(breakState = BreakState.BROKEN) }) }
        runCurrent()

        assertTrue(viewModel.uiState.value.isProcessing)
        assertFalse(viewModel.uiState.value.isAttackEnabled)
        assertEquals(BattleNarration.BreakStunned("味方"), viewModel.uiState.value.narration)
        viewModel.onAttackClick()
        assertEquals(BattleSelection.None, viewModel.uiState.value.selection)

        advanceTimeBy(1_200)
        runCurrent()
        assertEquals(BattleNarration.BreakRecovered("味方"), viewModel.uiState.value.narration)
        assertEquals(0, repository.current.allies[0].turnCount)

        advanceUntilIdle()

        val ally = repository.current.allies[0]
        assertEquals(1, ally.turnCount)
        assertEquals(BreakState.RECOVERING, ally.breakState)
        assertTrue(viewModel.uiState.value.isPlayerTurn)
        assertTrue(viewModel.uiState.value.isAttackEnabled)
    }

    /** ブレイク中の敵は攻撃せず、動けない旨のナレーションのあと手番を飛ばされ、立ち上がり状態になること。 */
    @Test
    fun brokenEnemy_doesNotAttack_andTurnIsSkipped() = runTest(dispatcher) {
        val (viewModel, repository) = createViewModel()
        advanceUntilIdle()
        val allyHp = repository.current.allies[0].currentHp
        repository.updateBattleField { f -> f.copy(enemies = f.enemies.map { it.copy(breakState = BreakState.BROKEN) }) }

        viewModel.onAttackClick()
        viewModel.onEnemyClick(repository.current.enemies[0].fpid)
        runCurrent()
        // 同時に手番が来た敵の順番はTPの乱数で変わるため、最初に手番を飛ばされる敵を名前とFPIDで確かめる。
        advanceTimeBy(1_200)
        runCurrent()
        val skippedFpid = checkNotNull(repository.current.currentActorFpid)
        val skippedName = viewModel.uiState.value.nameOf(skippedFpid)

        assertEquals(BattleNarration.BreakStunned(skippedName), viewModel.uiState.value.narration)
        assertEquals(allyHp, repository.current.allies[0].currentHp)
        advanceTimeBy(1_200)
        runCurrent()
        assertEquals(BattleNarration.BreakRecovered(skippedName), viewModel.uiState.value.narration)
        advanceTimeBy(1_200)
        runCurrent()
        val skipped = repository.current.enemies.first { it.fpid == skippedFpid }
        assertEquals(BreakState.RECOVERING, skipped.breakState)
        assertEquals(2, skipped.turnCount) // 味方の手番の前に1回行動済み
    }

    /** スタンの味方の手番は操作を受け付けず、「〇〇はスタンで動けない!」のあと手番を終え、その手番終了時の毒ダメージと解除が1文ずつ表示されること。 */
    @Test
    fun stunnedAlly_turnIsSkipped_andTurnEndEventsAreNarrated() = runTest(dispatcher) {
        val (viewModel, repository) = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isAttackEnabled)
        val hpBefore = repository.current.allies[0].currentHp

        repository.updateBattleField { f ->
            f.copy(
                allies = f.allies.map {
                    it.copy(ailments = listOf(AilmentState(StatusAilment.POISON, 3), AilmentState(StatusAilment.STUN, 1)))
                },
            )
        }
        runCurrent()

        assertTrue(viewModel.uiState.value.isProcessing)
        assertFalse(viewModel.uiState.value.isAttackEnabled)
        assertEquals(BattleNarration.AilmentIncapacitated("味方", StatusAilment.STUN), viewModel.uiState.value.narration)
        assertEquals(listOf(StatusAilment.POISON, StatusAilment.STUN), viewModel.uiState.value.partySlots[0]?.ailments)
        viewModel.onAttackClick()
        assertEquals(BattleSelection.None, viewModel.uiState.value.selection)

        advanceTimeBy(1_200)
        runCurrent()
        // 状態異常はenum順(スタン→毒)に処理される。スタンは残り1なので手番終了で治り、その後に毒のダメージ(最大HP1000の1/8)が
        // 直後のフィールドの表示と一緒に出る。
        assertEquals(BattleNarration.AilmentCured("味方", StatusAilment.STUN), viewModel.uiState.value.narration)
        assertEquals(hpBefore, viewModel.uiState.value.partySlots[0]?.hp)

        advanceTimeBy(1_200)
        runCurrent()
        assertEquals(BattleNarration.AilmentHpDamage("味方", StatusAilment.POISON, 125), viewModel.uiState.value.narration)
        assertEquals(hpBefore - 125, viewModel.uiState.value.partySlots[0]?.hp)

        advanceUntilIdle()
        val ally = repository.current.allies[0]
        assertEquals(1, ally.turnCount)
        assertEquals(listOf(StatusAilment.POISON), ally.ailments.map { it.ailment })
        assertEquals(2, ally.ailments.single().remainingTurns)
    }

    /** 睡眠の敵は攻撃せず、動けない旨のナレーションのあと手番を飛ばされること(ブレイクより状態異常の文は後順位)。 */
    @Test
    fun sleepingEnemy_doesNotAttack_andTurnIsSkipped() = runTest(dispatcher) {
        val (viewModel, repository) = createViewModel()
        advanceUntilIdle()
        val allyHp = repository.current.allies[0].currentHp
        repository.updateBattleField { f ->
            f.copy(enemies = f.enemies.map { it.copy(ailments = listOf(AilmentState(StatusAilment.SLEEP, 5))) })
        }

        // 防御して手番を終えると、敵の手番になる。眠っている敵は攻撃せず、動けない旨を表示する。
        viewModel.onDefendClick()
        runCurrent()
        advanceTimeBy(1_200)
        runCurrent()
        val skippedFpid = checkNotNull(repository.current.currentActorFpid)
        val skippedName = viewModel.uiState.value.nameOf(skippedFpid)

        assertEquals(BattleNarration.AilmentIncapacitated(skippedName, StatusAilment.SLEEP), viewModel.uiState.value.narration)
        assertEquals(allyHp, repository.current.allies[0].currentHp)
        advanceUntilIdle()
    }

    /** 全て対象の回復スキルは、味方全員に効き、複数対象のナレーションになること。 */
    @Test
    fun areaSkill_showsMultiTargetNarration() = runTest(dispatcher) {
        val (viewModel, repository) = createViewModel(activeSkillIds = listOf(HEAL_ALL_ID), allySlots = listOf(0, 3))
        advanceUntilIdle()
        val allyFpids = repository.current.allies.map { it.fpid }.toSet()

        viewModel.onSkillClick()
        viewModel.onSkillSelected(HEAL_ALL_ID)
        assertEquals(allyFpids, viewModel.uiState.value.selectableTargetIds)
        viewModel.onAllyClick(allyFpids.first())
        runCurrent()

        val narration = viewModel.uiState.value.narration
        assertTrue(narration is BattleNarration.MultiSkillResult)
        narration as BattleNarration.MultiSkillResult
        assertEquals("全体回復", narration.skillName)
        assertEquals(2, narration.targetCount)
    }

    /**
     * TPが足りないときは不足分をHPで肩代わりして選べ(代替HPは最大HPの1%/TP切り上げ)、肩代わりでHPが0以下になるときは使えないこと。
     * TPが足りれば代替HPは0で、攻撃として演出され、TPが消費・獲得され、カードのTPにも反映されること。
     */
    @Test
    fun tpSkill_withTp_usesAttackFlowWithSkillName() = runTest(dispatcher) {
        val (viewModel, repository) = createViewModel(activeSkillIds = listOf(SOBAT_ID))
        advanceUntilIdle()
        // 最大HP1000・TP20・コスト40 → 不足20 → 代替HPは200。
        repository.updateBattleField { f -> f.copy(allies = f.allies.map { it.copy(currentTp = 20) }) }
        runCurrent()
        viewModel.onSkillClick()
        val substitutable = viewModel.uiState.value.skills.single()
        assertEquals(40, substitutable.tpCost)
        assertEquals(200, substitutable.substituteHpCost)
        assertTrue(substitutable.isUsable)
        viewModel.onCancelClick()

        // 現在HPが代替HPと同じ200だと、使うとHPが0になるため使えない(代替HPの表示は残る)。
        repository.updateBattleField { f -> f.copy(allies = f.allies.map { it.copy(currentHp = 200) }) }
        runCurrent()
        viewModel.onSkillClick()
        val tooLowHp = viewModel.uiState.value.skills.single()
        assertEquals(200, tooLowHp.substituteHpCost)
        assertFalse(tooLowHp.isUsable)
        viewModel.onSkillSelected(SOBAT_ID)
        assertEquals(BattleSelection.SkillList, viewModel.uiState.value.selection)
        viewModel.onCancelClick()

        // HPが1残るなら使える。
        repository.updateBattleField { f -> f.copy(allies = f.allies.map { it.copy(currentHp = 201) }) }
        runCurrent()
        viewModel.onSkillClick()
        assertTrue(viewModel.uiState.value.skills.single().isUsable)
        viewModel.onCancelClick()

        // TPが足りていれば代替HPは0。
        repository.updateBattleField { f -> f.copy(allies = f.allies.map { it.copy(currentTp = 40, currentHp = 1000) }) }
        runCurrent()
        viewModel.onSkillClick()
        assertEquals(0, viewModel.uiState.value.skills.single().substituteHpCost)
        viewModel.onCancelClick()

        repository.updateBattleField { f -> f.copy(allies = f.allies.map { it.copy(currentTp = 100) }) }
        runCurrent()
        assertEquals(100, viewModel.uiState.value.partySlots[0]?.tp)
        viewModel.onSkillClick()
        assertTrue(viewModel.uiState.value.skills.single().isUsable)
        viewModel.onSkillSelected(SOBAT_ID)
        viewModel.onEnemyClick(repository.current.enemies[0].fpid)
        runCurrent()

        val narration = viewModel.uiState.value.narration
        assertTrue(narration is BattleNarration.AttackResult)
        narration as BattleNarration.AttackResult
        assertEquals("ローリング・ソバット", narration.skillName)
        assertEquals("味方", narration.attackerName)
        assertEquals("ゴブリンA", narration.targetName)
        val tp = repository.current.allies[0].currentTp
        assertTrue("TPは100-40に条件1の8〜12(クリティカルなら倍率つき)が加わる: $tp", tp in 68..78)
        assertEquals(tp, viewModel.uiState.value.partySlots[0]?.tp)
        advanceUntilIdle()
        assertEquals(1, repository.current.allies[0].turnCount)
    }

    /** 敵の手番で攻撃が届く相手がいないとき、「届かない」ナレーションを表示してから手番を終え、味方のHPは変わらないこと。 */
    @Test
    fun enemyTurn_noReachableTarget_showsCannotReachAndEndsTurn() = runTest(dispatcher) {
        // 敵4体のうち4体目は後衛(スロット3)で、前衛の味方(レンジ1)には届かない。
        val (viewModel, repository) = createViewModel(enemyKindCount = 2)
        val backEnemyFpid = 5 // 味方がFPID1、敵が2〜5の順に採番され、4体目の敵(スロット3)がFPID5
        var guard = 0
        while (viewModel.uiState.value.narration != BattleNarration.EnemyCannotReach("ゴブリンD") && guard++ < 200) {
            advanceTimeBy(50)
            runCurrent()
        }
        assertEquals(BattleNarration.EnemyCannotReach("ゴブリンD"), viewModel.uiState.value.narration)
        assertEquals(3, repository.current.enemies.first { it.fpid == backEnemyFpid }.slot)
        assertEquals(backEnemyFpid, repository.current.currentActorFpid)
        val hpBefore = repository.current.allies[0].currentHp

        advanceTimeBy(1200)
        runCurrent()

        assertEquals(1, repository.current.enemies.first { it.fpid == backEnemyFpid }.turnCount)
        assertEquals(hpBefore, repository.current.allies[0].currentHp)
        assertFalse(viewModel.uiState.value.narration is BattleNarration.EnemyCannotReach)
    }

    /** 対象選択中に選べない相手(レンジ外の敵・選べない味方)をタップしても、何も起きないこと。 */
    @Test
    fun tapUnselectableTargets_doesNothing() = runTest(dispatcher) {
        val (viewModel, repository) = createViewModel(enemyKindCount = 2)
        advanceUntilIdle()
        val allyFpid = repository.current.allies[0].fpid
        val backEnemyFpid = repository.current.enemies.first { it.slot == 3 }.fpid
        viewModel.onAttackClick()
        assertFalse(backEnemyFpid in viewModel.uiState.value.selectableTargetIds)
        val before = repository.current

        viewModel.onEnemyClick(backEnemyFpid)
        viewModel.onAllyClick(allyFpid)
        viewModel.onEnemyClick(999)
        runCurrent()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isProcessing)
        assertEquals(BattleSelection.AttackTarget, viewModel.uiState.value.selection)
        assertEquals(before, repository.current)
    }

    /** 長押しで味方・敵(戦闘不能も)の詳細が開き、選択状態は変わらず、閉じる操作で閉じること。存在しないFPIDは無視される。 */
    @Test
    fun longPress_opensAndDismissesDetail_withoutChangingSelection() = runTest(dispatcher) {
        val (viewModel, repository) = createViewModel()
        advanceUntilIdle()
        viewModel.onAttackClick()
        val allyFpid = repository.current.allies[0].fpid
        val enemyFpid = repository.current.enemies[0].fpid

        viewModel.onUnitLongPress(999)
        assertNull(viewModel.uiState.value.detailUnitId)
        viewModel.onUnitLongPress(allyFpid)
        assertEquals("味方", viewModel.uiState.value.unitDetail?.name)
        viewModel.onUnitLongPress(enemyFpid)
        assertEquals(enemyFpid, viewModel.uiState.value.unitDetail?.id)
        assertEquals(BattleSelection.AttackTarget, viewModel.uiState.value.selection)

        repository.updateBattleField { f -> f.copy(enemies = f.enemies.map { if (it.fpid == enemyFpid) it.copy(currentHp = 0) else it }) }
        runCurrent()
        viewModel.onUnitDetailDismiss()
        assertNull(viewModel.uiState.value.unitDetail)
        viewModel.onUnitLongPress(enemyFpid) // 戦闘不能の個体も開ける
        assertEquals(0, viewModel.uiState.value.unitDetail?.hp)
    }

    /** 勝敗が決まると開いていた詳細が閉じ、決着後は長押ししても開けないこと。 */
    @Test
    fun longPress_closesOnOutcome_andIgnoredAfter() = runTest(dispatcher) {
        val (viewModel, repository) = createViewModel()
        advanceUntilIdle()
        viewModel.onUnitLongPress(repository.current.allies[0].fpid)
        assertTrue(viewModel.uiState.value.detailUnitId != null)

        repository.updateBattleField { f -> f.copy(enemies = f.enemies.map { it.copy(currentHp = 0) }) }
        runCurrent()

        assertEquals(BattleOutcome.Victory, viewModel.uiState.value.outcome)
        assertNull(viewModel.uiState.value.detailUnitId)
        viewModel.onUnitLongPress(repository.current.allies[0].fpid)
        assertNull(viewModel.uiState.value.detailUnitId)
    }

    /** 詳細の値がフィールドの値と対応し、BG・残り距離は切り捨てられ、HPが戦闘の変化に追従すること。 */
    @Test
    fun detail_reflectsFieldValues_andFollowsChanges() = runTest(dispatcher) {
        val (viewModel, repository) = createViewModel(allyMana = 30)
        advanceUntilIdle()
        val allyFpid = repository.current.allies[0].fpid
        repository.updateBattleField { f ->
            f.copy(
                allies = f.allies.map {
                    it.copy(breakGauge = 75.9, remainingDistance = 6200.7, turnCount = 3, breakState = BreakState.RECOVERING)
                },
            )
        }
        runCurrent()
        viewModel.onUnitLongPress(allyFpid)

        val detail = checkNotNull(viewModel.uiState.value.unitDetail)
        assertEquals(75, detail.breakGauge)
        assertEquals(6200, detail.remainingDistance)
        assertEquals(3, detail.turnCount)
        assertEquals(BreakState.RECOVERING, detail.breakState)
        assertEquals(BattleUnitPosition.FRONT, detail.position)
        assertEquals(1000, detail.maxHp)
        assertEquals(30, detail.maxMp)
        assertEquals(100, detail.attack)
        assertEquals(100, detail.accuracy)

        repository.updateBattleField { f -> f.copy(allies = f.allies.map { it.copy(currentHp = 400, currentMp = 5) }) }
        runCurrent()
        assertEquals(400, viewModel.uiState.value.unitDetail?.hp)
        assertEquals(5, viewModel.uiState.value.unitDetail?.mp)
    }

    /** 詳細に12属性の耐性が表示順(炎・崩・水…)で入り、キャラクターの耐性がそのまま反映されること。 */
    @Test
    fun detail_elementResistances_followCharacter() = runTest(dispatcher) {
        val (viewModel, repository) = createViewModel()
        advanceUntilIdle()
        val ally = repository.current.allies.first()
        repository.updateBattleField { f ->
            f.copy(
                allies = f.allies.map {
                    if (it.fpid == ally.fpid) {
                        it.copy(character = it.character.copy(elementResistances = ElementResistances(fire = 0, collapse = -1, erosion = -999)))
                    } else {
                        it
                    }
                },
            )
        }
        runCurrent()

        val resistances = viewModel.uiState.value.unitDetails.getValue(ally.fpid).elementResistances
        assertEquals(ElementResistances.ORDER, resistances.map { it.element })
        assertEquals(0, resistances.first().value)
        assertEquals(-1, resistances[1].value)
        assertEquals(-999, resistances.last().value)
        assertEquals(100, resistances[2].value)
    }

    /** 後衛は「後衛」、前衛が全滅した陣営の後衛は「後衛(前衛扱い)」になること。 */
    @Test
    fun detail_position_backAndPromotedBack() = runTest(dispatcher) {
        val (viewModel, repository) = createViewModel(allySlots = listOf(0, 3))
        advanceUntilIdle()
        val front = repository.current.allies.first { it.slot == 0 }
        val back = repository.current.allies.first { it.slot == 3 }
        assertEquals(BattleUnitPosition.BACK, viewModel.uiState.value.unitDetails.getValue(back.fpid).position)

        repository.updateBattleField { f -> f.copy(allies = f.allies.map { if (it.fpid == front.fpid) it.copy(currentHp = 0) else it }) }
        runCurrent()

        assertEquals(BattleUnitPosition.FRONT, viewModel.uiState.value.unitDetails.getValue(front.fpid).position)
        assertEquals(BattleUnitPosition.BACK_AS_FRONT, viewModel.uiState.value.unitDetails.getValue(back.fpid).position)
    }

    /** Aスキル名には通常攻撃が入らず設定済みのスキルだけが並び、敵はスキルを持たないこと。 */
    @Test
    fun detail_skills_excludeNormalAttack_andEnemyHasNone() = runTest(dispatcher) {
        val (viewModel, repository) = createViewModel(allyMana = 30, activeSkillIds = listOf(2))
        advanceUntilIdle()
        val ally = viewModel.uiState.value.unitDetails.getValue(repository.current.allies[0].fpid)
        val enemy = viewModel.uiState.value.unitDetails.getValue(repository.current.enemies[0].fpid)

        assertEquals(listOf("ディア"), ally.activeSkillNames)
        assertTrue(ally.passiveSkillNames.isEmpty())
        assertTrue(enemy.activeSkillNames.isEmpty())
        assertTrue(enemy.passiveSkillNames.isEmpty())
    }

    /** レンジ0のスキルは、一覧で選んだ瞬間に対象選択を挟まず自分へ発動し、ナレーションが1文ずつ流れて手番が終わること。 */
    @Test
    fun selfTargetingSkill_castsImmediately_withStatNarrations() = runTest(dispatcher) {
        val (viewModel, repository) = createViewModel(allyMana = 100, activeSkillIds = listOf(SEISHIN_ID))
        advanceUntilIdle()
        val allyFpid = repository.current.allies[0].fpid
        assertTrue(viewModel.uiState.value.skills.single().isSelfTargeting)

        viewModel.onSkillClick()
        viewModel.onSkillSelected(SEISHIN_ID)
        viewModel.onSkillSelected(SEISHIN_ID) // 連打しても二重に発動しない
        runCurrent()

        assertEquals(BattleSelection.None, viewModel.uiState.value.selection)
        assertTrue(viewModel.uiState.value.isProcessing)
        assertEquals(BattleNarration.SkillUsed("味方", "精神一到"), viewModel.uiState.value.narration)
        assertEquals(allyFpid, repository.current.allies[0].fpid)
        assertEquals(1, repository.current.allies[0].statModifiers.size)

        viewModel.onNarrationClick()
        runCurrent()
        assertEquals(BattleNarration.StatStageChanged("味方", BattleStat.ATTACK, 1), viewModel.uiState.value.narration)

        advanceUntilIdle()

        val ally = repository.current.allies[0]
        assertEquals(80, ally.currentMp)
        assertEquals(1, ally.turnCount)
        // かけた手番は数えないので、残りは10のまま。
        assertEquals(10, ally.statModifiers.single().remainingTurns)
    }

    /** 詳細ポップアップ用に、ステータス変化が合計と内訳で組み立てられ、基礎のステータス値は変わらないこと。 */
    @Test
    fun detail_statChanges_totalAndEntries_baseStatUnchanged() = runTest(dispatcher) {
        val (viewModel, repository) = createViewModel(allyMana = 100, activeSkillIds = listOf(SEISHIN_ID))
        advanceUntilIdle()
        val allyFpid = repository.current.allies[0].fpid
        assertTrue(viewModel.uiState.value.unitDetails.getValue(allyFpid).statChanges.isEmpty())
        repository.updateBattleField { f ->
            f.copy(
                allies = f.allies.map {
                    it.copy(
                        statModifiers = listOf(
                            StatModifier(BattleStat.ATTACK, 1, 10),
                            StatModifier(BattleStat.ATTACK, 1, 3),
                            StatModifier(BattleStat.DEFENSE, 1, 2),
                            StatModifier(BattleStat.DEFENSE, -1, 5),
                        ),
                    )
                },
            )
        }
        runCurrent()

        val detail = viewModel.uiState.value.unitDetails.getValue(allyFpid)

        assertEquals(
            listOf(
                StatChangeDetail(BattleStat.ATTACK, 2, listOf(StatChangeEntry(1, 10), StatChangeEntry(1, 3))),
                StatChangeDetail(BattleStat.DEFENSE, 0, listOf(StatChangeEntry(1, 2), StatChangeEntry(-1, 5))),
            ),
            detail.statChanges,
        )
        assertEquals(100, detail.attack)
    }

    /** 命中率・回避率の段階は「ステータス変化」に出る一方、戦闘値の命中率・回避率は基本値100のままであること。 */
    @Test
    fun detail_accuracyEvasionStages_listedButBaseValuesStay100() = runTest(dispatcher) {
        val (viewModel, repository) = createViewModel()
        advanceUntilIdle()
        val allyFpid = repository.current.allies[0].fpid
        repository.updateBattleField { f ->
            f.copy(
                allies = f.allies.map {
                    it.copy(statModifiers = listOf(StatModifier(BattleStat.ACCURACY, 2, 4), StatModifier(BattleStat.EVASION, -1, 6)))
                },
            )
        }
        runCurrent()

        val detail = viewModel.uiState.value.unitDetails.getValue(allyFpid)

        assertEquals(100, detail.accuracy)
        assertEquals(100, detail.evasion)
        assertEquals(
            listOf(
                StatChangeDetail(BattleStat.ACCURACY, 2, listOf(StatChangeEntry(2, 4))),
                StatChangeDetail(BattleStat.EVASION, -1, listOf(StatChangeEntry(-1, 6))),
            ),
            detail.statChanges,
        )
    }

    /**
     * 戦闘値のクリティカル発生率・倍率は常に基本値(4%・150)で、段階の変化は「ステータス変化」に出ること。
     * 永続のバフは残りターンがnullのエントリになる。
     */
    @Test
    fun detail_criticalBaseValues_andPermanentEntry() = runTest(dispatcher) {
        val (viewModel, repository) = createViewModel()
        advanceUntilIdle()
        val allyFpid = repository.current.allies[0].fpid
        repository.updateBattleField { f ->
            f.copy(
                allies = f.allies.map {
                    it.copy(
                        statModifiers = listOf(
                            StatModifier(BattleStat.CRITICAL_RATE, 1, null),
                            StatModifier(BattleStat.CRITICAL_DAMAGE, 2, 5),
                        ),
                    )
                },
            )
        }
        runCurrent()

        val detail = viewModel.uiState.value.unitDetails.getValue(allyFpid)

        assertEquals(4, detail.criticalRate)
        assertEquals(150, detail.criticalDamage)
        assertEquals(
            listOf(
                StatChangeDetail(BattleStat.CRITICAL_RATE, 1, listOf(StatChangeEntry(1, null))),
                StatChangeDetail(BattleStat.CRITICAL_DAMAGE, 2, listOf(StatChangeEntry(2, 5))),
            ),
            detail.statChanges,
        )
    }

    /** 敵の手番中は「防御」を押せず、押しても何も起きないこと。 */
    @Test
    fun defend_disabledDuringEnemyTurn() = runTest(dispatcher) {
        val (viewModel, repository) = createViewModel()
        runCurrent()

        assertFalse(viewModel.uiState.value.isPlayerTurn)
        assertFalse(viewModel.uiState.value.isDefendEnabled)
        viewModel.onDefendClick()
        runCurrent()

        assertTrue(repository.current.allPawns.none { it.isDefending })
    }

    /** 攻撃の対象選択中は「防御」を押せず、キャンセルして選択状態がNoneに戻ると押せること。 */
    @Test
    fun defend_disabledWhileSelecting() = runTest(dispatcher) {
        val (viewModel, repository) = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isDefendEnabled)

        viewModel.onAttackClick()
        assertFalse(viewModel.uiState.value.isDefendEnabled)
        viewModel.onDefendClick()
        runCurrent()
        assertFalse(viewModel.uiState.value.isProcessing)
        assertFalse(repository.current.allies[0].isDefending)

        viewModel.onCancelClick()
        assertTrue(viewModel.uiState.value.isDefendEnabled)
    }

    /**
     * 「防御」を押すとナレーションを表示して手番が終わり、次の自分の手番まで防御状態が続いて(敵の攻撃が24→16に減る)、
     * 自分の手番が来たら解除されること。詳細ポップアップ用のモデルにも防御中が反映される。
     */
    @Test
    fun defendFlow_narratesEndsTurn_andReleasesOnOwnNextTurn() = runTest(dispatcher) {
        val (viewModel, repository) = createViewModel()
        advanceUntilIdle()
        val allyFpid = repository.current.allies[0].fpid
        assertEquals(1000 - 24 * 2, repository.current.allies[0].currentHp)

        viewModel.onDefendClick()
        viewModel.onDefendClick() // 連打しても二重に実行されない
        runCurrent()

        assertTrue(viewModel.uiState.value.isProcessing)
        assertFalse(viewModel.uiState.value.isDefendEnabled)
        assertEquals(BattleNarration.Defend("味方"), viewModel.uiState.value.narration)
        assertTrue(repository.current.allies[0].isDefending)
        assertTrue(viewModel.uiState.value.unitDetails.getValue(allyFpid).isDefending)

        advanceUntilIdle()

        val ally = repository.current.allies[0]
        assertEquals(1, ally.turnCount)
        // 次の自分の手番までに敵2体が計4回攻撃し、防御中なのでいずれも16ダメージ。
        assertEquals(1000 - 24 * 2 - 16 * 4, ally.currentHp)
        assertFalse(ally.isDefending)
        assertFalse(viewModel.uiState.value.unitDetails.getValue(allyFpid).isDefending)
        assertTrue(viewModel.uiState.value.isDefendEnabled)
    }

    // ---- 先制行動 ----

    /** 先制スキルを設定した味方(MP100)1体で始めたViewModelを返す。味方は素早さが低く、敵2体が先に行動する。 */
    private fun createPreemptiveViewModel(
        allySlots: List<Int> = listOf(0),
    ): Pair<BattleViewModel, FakeBattleRepository> =
        createViewModel(allyMana = 100, activeSkillIds = listOf(2, QUICK_ID), allySlots = allySlots)

    /** 敵の行動中に予約すると、その敵の行動の後に先制の手番が来て、「先制行動!」のあと先制スキルだけの一覧になること。 */
    @Test
    fun reserveDuringEnemyTurn_preemptiveTurnComesAfterThatAction() = runTest(dispatcher) {
        val (viewModel, repository) = createPreemptiveViewModel()
        runCurrent()
        val allyFpid = repository.current.allies[0].fpid
        assertFalse(viewModel.uiState.value.isPlayerTurn)
        assertEquals(setOf(allyFpid), viewModel.uiState.value.reservableAllyIds)
        assertTrue(viewModel.uiState.value.partySlots[0]?.canReservePreemptive == true)

        viewModel.onAllyClick(allyFpid)
        viewModel.onAllyClick(allyFpid) // 連打しても二重に予約されない
        runCurrent()

        assertEquals(listOf(allyFpid), repository.current.pendingPreemptive)
        val waiting = viewModel.uiState.value.partySlots[0]
        assertTrue(waiting?.isPreemptiveWaiting == true)
        assertFalse(waiting?.canReservePreemptive == true)
        // 予約したときのナレーションは出ない(敵の行動のナレーションのまま)。
        assertTrue(viewModel.uiState.value.narration is BattleNarration.EnemyTurn)

        // 敵の攻撃の結果を表示し終えた直後に、その敵の手番が終わって先制の手番が始まる。
        advanceTimeBy(2000)
        runCurrent()
        assertEquals(allyFpid, repository.current.currentActorFpid)
        assertTrue(viewModel.uiState.value.isPreemptiveTurn)
        assertTrue(viewModel.uiState.value.isProcessing)
        assertEquals(BattleNarration.PreemptiveAction("味方"), viewModel.uiState.value.narration)
        // 先制の手番を待っていた表示は、先制の手番に入ったら外れる(いまの行動者なので)。
        assertFalse(viewModel.uiState.value.partySlots[0]?.isPreemptiveWaiting == true)
        // 敵2体目は、先制の手番のあとまで行動しない。
        assertEquals(1, repository.current.enemies.count { it.turnCount == 1 })

        advanceTimeBy(RESULT_MS)
        runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isProcessing)
        assertEquals(BattleSelection.SkillList, state.selection)
        assertEquals(listOf(QUICK_ID), state.skills.map { it.id })
        assertTrue(state.skills.single().isUsable)
        assertEquals(BattleNarration.SelectSkill, state.narration)
    }

    /** 先制の手番では、攻撃・防御・撤退が無効で、スキル一覧からはキャンセルできず、対象選びからはスキル一覧へ戻れること。 */
    @Test
    fun preemptiveTurn_restrictsCommands_andCancelOnlyReturnsToSkillList() = runTest(dispatcher) {
        val (viewModel, repository) = createPreemptiveViewModel()
        runCurrent()
        viewModel.onAllyClick(repository.current.allies[0].fpid)
        advanceTimeBy(2000 + RESULT_MS)
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.isPreemptiveTurn)
        assertFalse(state.isAttackEnabled)
        assertFalse(state.isDefendEnabled)
        assertFalse(state.isRetreatEnabled)
        assertFalse(state.isCancelEnabled)

        viewModel.onAttackClick()
        viewModel.onDefendClick()
        viewModel.onCancelClick() // スキル一覧からはキャンセルできない
        assertEquals(BattleSelection.SkillList, viewModel.uiState.value.selection)
        assertFalse(viewModel.uiState.value.isProcessing)

        viewModel.onSkillSelected(QUICK_ID)
        assertEquals(BattleSelection.SkillTarget(QUICK_ID), viewModel.uiState.value.selection)
        assertTrue(viewModel.uiState.value.isCancelEnabled)
        viewModel.onCancelClick() // 対象選びからはスキル一覧へ戻る
        assertEquals(BattleSelection.SkillList, viewModel.uiState.value.selection)
    }

    /** 先制スキルを使うと通常のスキルと同じく手番が終わり、CT中は次の自分の通常の手番で一覧に出るが使えないこと。 */
    @Test
    fun preemptiveSkill_endsTurn_andIsOnCooldownInNextNormalTurn() = runTest(dispatcher) {
        val (viewModel, repository) = createPreemptiveViewModel()
        runCurrent()
        val allyFpid = repository.current.allies[0].fpid
        viewModel.onAllyClick(allyFpid)
        advanceTimeBy(2000 + RESULT_MS)
        runCurrent()
        val enemyFpid = repository.current.enemies[0].fpid
        val hpBefore = repository.current.enemies[0].currentHp

        viewModel.onSkillSelected(QUICK_ID)
        viewModel.onEnemyClick(enemyFpid)
        runCurrent()
        assertTrue(viewModel.uiState.value.isProcessing)
        assertEquals(10, 100 - repository.current.allies[0].currentMp)
        assertTrue(repository.current.enemies[0].currentHp < hpBefore)

        advanceUntilIdle()

        // 先制の手番は終わり(手番の回数+1)、先制の印も外れている。次の通常の手番では全スキルが並び、クイックシュートはCT中で使えない。
        val ally = repository.current.allies[0]
        assertEquals(1, ally.turnCount)
        assertFalse(repository.current.isPreemptiveTurn)
        val state = viewModel.uiState.value
        assertTrue(state.isPlayerTurn)
        assertEquals(allyFpid, state.currentActorId)
        assertEquals(listOf(2, QUICK_ID), state.skills.map { it.id })
        assertTrue(state.skills.first { it.id == 2 }.isUsable)
        assertFalse(state.skills.first { it.id == QUICK_ID }.isUsable)
        assertEquals(1, state.skills.first { it.id == QUICK_ID }.remainingCoolTime)
        assertEquals(0, state.skills.first { it.id == 2 }.remainingCoolTime)
        assertTrue(state.isAttackEnabled)
        assertTrue(state.isDefendEnabled)
    }

    /** CT中のスキルは使った次の自分の手番で残り1が入り、その手番を終えて明けたら0になって使えること。 */
    @Test
    fun remainingCoolTime_isOneInNextOwnTurn_andZeroAfterItExpires() = runTest(dispatcher) {
        val (viewModel, repository) = createPreemptiveViewModel()
        advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.skills.first { it.id == QUICK_ID }.remainingCoolTime)

        // 通常の手番でクイックシュートを使う。
        viewModel.onSkillClick()
        viewModel.onSkillSelected(QUICK_ID)
        viewModel.onEnemyClick(repository.current.enemies[0].fpid)
        advanceUntilIdle()

        // 次の自分の手番: CT残り1で使えない。
        var quick = viewModel.uiState.value.skills.first { it.id == QUICK_ID }
        assertEquals(1, quick.remainingCoolTime)
        assertFalse(quick.isUsable)

        // その手番を防御で終えると、CTが明けて0になり使える。
        viewModel.onDefendClick()
        advanceUntilIdle()
        quick = viewModel.uiState.value.skills.first { it.id == QUICK_ID }
        assertEquals(0, quick.remainingCoolTime)
        assertTrue(quick.isUsable)
    }

    /** 攻撃・スキルの対象選択中は、味方のカードのタップを予約に使わず、キャンセルで戻れば予約できること。 */
    @Test
    fun cannotReserveWhileSelectingTarget() = runTest(dispatcher) {
        val (viewModel, repository) = createPreemptiveViewModel(allySlots = listOf(0, 1))
        advanceUntilIdle()
        val actor = checkNotNull(viewModel.uiState.value.currentActorId)
        val other = repository.current.allies.first { it.fpid != actor }.fpid
        assertEquals(setOf(other), viewModel.uiState.value.reservableAllyIds)

        viewModel.onAttackClick()
        assertEquals(emptySet<Int>(), viewModel.uiState.value.reservableAllyIds)
        viewModel.onAllyClick(other)
        runCurrent()
        assertTrue(repository.current.pendingPreemptive.isEmpty())

        viewModel.onCancelClick()
        viewModel.onSkillClick()
        viewModel.onSkillSelected(QUICK_ID)
        assertEquals(BattleSelection.SkillTarget(QUICK_ID), viewModel.uiState.value.selection)
        assertEquals(emptySet<Int>(), viewModel.uiState.value.reservableAllyIds)
        viewModel.onAllyClick(other)
        runCurrent()
        assertTrue(repository.current.pendingPreemptive.isEmpty())

        viewModel.onCancelClick()
        viewModel.onCancelClick()
        viewModel.onAllyClick(other)
        runCurrent()
        assertEquals(listOf(other), repository.current.pendingPreemptive)
    }

    /** 先制の手番を待っている間に戦闘フィールドが更新されても、「先制行動!」が二重に出たり、スキル一覧が閉じたりしないこと。 */
    @Test
    fun preemptiveIntro_isNotRepeatedOnFieldUpdates() = runTest(dispatcher) {
        val (viewModel, repository) = createPreemptiveViewModel(allySlots = listOf(0, 1))
        runCurrent()
        val first = repository.current.allies[0].fpid
        val second = repository.current.allies[1].fpid
        viewModel.onAllyClick(first)
        advanceTimeBy(2000 + RESULT_MS)
        runCurrent()
        assertEquals(first, repository.current.currentActorFpid)
        assertEquals(BattleSelection.SkillList, viewModel.uiState.value.selection)

        // 先制の手番の最中に、もう1体が予約する(戦闘フィールドの更新が流れてくる)。
        viewModel.onAllyClick(second)
        runCurrent()
        advanceTimeBy(RESULT_MS * 3)
        runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isProcessing)
        assertNull(state.processingNarration)
        assertEquals(BattleSelection.SkillList, state.selection)
        assertEquals(BattleNarration.SelectSkill, state.narration)
        assertTrue(state.partySlots.filterNotNull().first { it.id == second }.isPreemptiveWaiting)

        // 先制の手番を終えると、予約していたもう1体が続けて先制の手番になり、改めて「先制行動!」が出る。
        viewModel.onSkillSelected(QUICK_ID)
        viewModel.onEnemyClick(repository.current.enemies[0].fpid)
        advanceTimeBy(RESULT_MS)
        runCurrent()
        assertEquals(second, repository.current.currentActorFpid)
        assertTrue(viewModel.uiState.value.isPreemptiveTurn)
        assertEquals(BattleNarration.PreemptiveAction(viewModel.uiState.value.nameOf(second)), viewModel.uiState.value.narration)
    }

    /** 予約後に先制スキルが使えなくなったら、次の手番へ進む前に「先制行動できなかった!」が出て、手番を使わないこと。 */
    @Test
    fun cancelledPreemptiveTurn_showsFailureNarrationBeforeNextTurn() = runTest(dispatcher) {
        val (viewModel, repository) = createPreemptiveViewModel()
        runCurrent()
        val allyFpid = repository.current.allies[0].fpid
        viewModel.onAllyClick(allyFpid)
        runCurrent()
        // 予約したあとでMPが尽きて、先制スキルを使えなくする。
        repository.updateBattleField { f -> f.copy(allies = f.allies.map { it.copy(currentMp = 0) }) }

        advanceTimeBy(2000)
        runCurrent()

        assertEquals(BattleNarration.PreemptiveFailed("味方"), viewModel.uiState.value.narration)
        assertTrue(viewModel.uiState.value.isProcessing)
        assertFalse(repository.current.isPreemptiveTurn)
        assertEquals(0, repository.current.allies[0].turnCount)
        assertFalse(viewModel.uiState.value.isPreemptiveTurn)

        advanceTimeBy(RESULT_MS)
        runCurrent()

        // 取り消しの文のあと、次の敵の手番へ進む。
        assertTrue(viewModel.uiState.value.narration is BattleNarration.EnemyTurn)
        assertEquals(0, repository.current.allies[0].turnCount)
    }

    /** 先制の手番でスキルを使えなかったとき(結果がnull)も、行動選択へ戻らずスキル一覧へ戻ること。 */
    @Test
    fun preemptiveTurn_failedUse_returnsToSkillList() = runTest(dispatcher) {
        val (viewModel, repository) = createPreemptiveViewModel()
        runCurrent()
        viewModel.onAllyClick(repository.current.allies[0].fpid)
        advanceTimeBy(2000 + RESULT_MS)
        runCurrent()
        viewModel.onSkillSelected(QUICK_ID)
        // 対象を選ぶ直前にMPが尽きる(UIの状態は古いまま、ドメインが使用を拒否する)。
        repository.updateBattleField { f -> f.copy(allies = f.allies.map { it.copy(currentMp = 0) }) }
        runCurrent()
        assertEquals(BattleSelection.SkillTarget(QUICK_ID), viewModel.uiState.value.selection)

        viewModel.onEnemyClick(repository.current.enemies[0].fpid)
        advanceUntilIdle()

        // 使えなかったので手番は終わらず、先制の手番のまま、スキル一覧(使えないと表示される)へ戻る。
        assertTrue(repository.current.isPreemptiveTurn)
        assertEquals(0, repository.current.allies[0].turnCount)
        val state = viewModel.uiState.value
        assertFalse(state.isProcessing)
        assertEquals(BattleSelection.SkillList, state.selection)
        assertFalse(state.skills.single().isUsable)
    }


    /**
     * テスト用の、通常攻撃・ディア(MP10・50回復・レンジ3・エリア全体から一体選択)・近接技・全体回復を返す[SkillRepository]。
     *
     * @property normalAttack 通常攻撃として返すスキル。
     */
    private class FakeSkillRepository(private val normalAttack: ActiveSkill) : SkillRepository {
        private val farStrike = testActiveSkill(id = FAR_STRIKE_ID, name = "近接技", targetSide = SkillTargetSide.ENEMY)
        private val healAll = testActiveSkill(
            id = HEAL_ALL_ID, name = "全体回復",
            range = SkillRange.RANGE_3, area = SkillArea.AREA, targetSelection = SkillTargetSelection.ALL,
            effects = listOf(ActiveSkillEffect.Heal(50)),
        )
        private val dia = testActiveSkill(
            id = 2, name = "ディア", costs = listOf(SkillCost(SkillCostType.MP, 10)),
            range = SkillRange.RANGE_3, area = SkillArea.AREA,
            effects = listOf(ActiveSkillEffect.Heal(50)),
        )
        private val seishin = testActiveSkill(
            id = SEISHIN_ID, name = "精神一到", costs = listOf(SkillCost(SkillCostType.MP, 20)),
            range = SkillRange.RANGE_0,
            effects = listOf(ActiveSkillEffect.StatChange(BattleStat.ATTACK, stages = 1, durationTurns = 10)),
        )
        override suspend fun getNormalAttack(): ActiveSkill = normalAttack
        private val sobat = testActiveSkill(
            id = SOBAT_ID, name = "ローリング・ソバット", costs = listOf(SkillCost(SkillCostType.TP, 40)),
            targetSide = SkillTargetSide.ENEMY, basePower = 150,
        )
        private val quick = testActiveSkill(
            id = QUICK_ID, name = "クイックシュート", costs = listOf(SkillCost(SkillCostType.MP, 10)),
            targetSide = SkillTargetSide.ENEMY, range = SkillRange.RANGE_3, basePower = 50,
            coolTime = 1, preemptivePriority = 1,
        )
        private val all = listOf(dia, farStrike, healAll, seishin, sobat, quick)
        override suspend fun getActiveSkills(): List<ActiveSkill> = all
        override suspend fun getPassiveSkills(): List<PassiveSkill> = emptyList()
        override suspend fun getActiveSkill(id: Int): ActiveSkill? = all.firstOrNull { it.id == id }
        override suspend fun getPassiveSkill(id: Int): PassiveSkill? = null
    }

    /** テスト用の定数をまとめたcompanion。 */
    private companion object {
        /** 結果の各文を表示する時間(ミリ秒。ViewModelの定数と同じ)。 */
        const val RESULT_MS = 1200L

        /** レンジ1・敵対象の、ダメージなしの近接スキル(後衛からは届かない)のID。 */
        const val FAR_STRIKE_ID = 3

        /** レンジ3・味方エリア全体・全て対象の回復スキルのID。 */
        const val HEAL_ALL_ID = 4

        /** TP40を消費する、基礎威力150の単体攻撃スキル(ローリング・ソバット)のID。 */
        const val SOBAT_ID = 6

        /** MP10・CT1・レンジ3の敵単体の攻撃で、先制行動の優先度1を持つスキル(クイックシュートの代わり)のID。 */
        const val QUICK_ID = 7

        /** レンジ0・自分の破壊力を+1段階10ターンにするスキル(精神一到)のID。 */
        const val SEISHIN_ID = 5

        /** 通常攻撃(基礎威力100・レンジ1・単体・一体選択・敵対象)。 */
        val DEFAULT_NORMAL_ATTACK = testActiveSkill(
            id = 1, name = "通常攻撃", targetSide = SkillTargetSide.ENEMY, basePower = 100,
        )
    }

    /**
     * 命中判定の乱数を、積んだ値の順に返すテスト用の乱数。積んでいない間は0.0(命中率が1以上なら必ず当たり)を返す。
     * 敵の手番など、結果を気にしない判定を必ず当たりにしておき、確認したい判定の直前に値を積むために使う。
     */
    private class ScriptedRandom : Random() {
        private val rolls = ArrayDeque<Double>()

        /**
         * 以降の判定で順に返す値を積む。
         *
         * @param values 返す値(0.0以上1.0未満)。
         */
        fun queue(vararg values: Double) {
            rolls.addAll(values.toList())
        }

        override fun nextBits(bitCount: Int): Int = error("nextDouble・nextInt(until)以外は使わない")

        /** クリティカル判定。基本の発生率ではクリティカルにならない`until - 1`を返す。 */
        override fun nextInt(until: Int): Int = until - 1

        override fun nextDouble(): Double = rolls.removeFirstOrNull() ?: 0.0
    }

    /** テスト用の、1体のキャラクターだけを返す[CharacterRepository]。 */
    private class FakeCharacterRepository(private val character: Character) : CharacterRepository {
        override fun observeCharacters(): Flow<List<Character>> = MutableStateFlow(listOf(character))
        override suspend fun getCharacter(id: Int): Character? = character.takeIf { it.id == id }
        override suspend fun saveCharacter(draft: CharacterDraft): Character = error("not used")
        override suspend fun deleteCharacter(id: Int) = error("not used")
    }

    /** テスト用の、指定した敵種を返す[EnemyRepository]。 */
    private class FakeEnemyRepository(private val enemies: List<Enemy>) : EnemyRepository {
        override suspend fun getEnemies(): List<Enemy> = enemies
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
