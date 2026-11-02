package jp.co.fuller.morikan.bootcamp.mikatsu.ui.charactercreate

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Character
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.CharacterDraft
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkill
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkillEffect
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.PassiveSkill
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.PassiveSkillEffect
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillLimits
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.testActiveSkill
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.CharacterRepository
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.SkillRepository
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.GetCharacterUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.GetSkillsUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.SaveCharacterUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.charactercreate.model.SkillPickerKind
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.charactercreate.model.SkillUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** [CharacterCreateViewModel]のスキル設定(追加・削除・上限・重複・保存・復元)を検証するテスト。 */
@OptIn(ExperimentalCoroutinesApi::class)
class CharacterCreateViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** テスト用の架空のカタログ。アクティブスキルをIDの数だけ(ID 1〜)、パッシブスキルを1つ(ID 1。アクティブと同じ数値だが別のスキル)持つテスト用の[SkillRepository]。 */
    private class FakeSkillRepository(activeCount: Int = 10) : SkillRepository {
        private val actives = (1..activeCount).map {
            testActiveSkill(id = it, name = "A$it", effects = listOf(ActiveSkillEffect.Heal(1)))
        }
        private val passives = listOf(
            PassiveSkill(id = 1, name = "P", categories = emptySet(), effects = listOf(PassiveSkillEffect.DamageReduction(1))),
        )
        override suspend fun getNormalAttack(): ActiveSkill = testActiveSkill(id = 0, name = "通常攻撃")
        override suspend fun getActiveSkills(): List<ActiveSkill> = actives
        override suspend fun getPassiveSkills(): List<PassiveSkill> = passives
        override suspend fun getActiveSkill(id: Int): ActiveSkill? = actives.firstOrNull { it.id == id }
        override suspend fun getPassiveSkill(id: Int): PassiveSkill? = passives.firstOrNull { it.id == id }
    }

    /** 保存されたドラフトを記録し、既存キャラクターを1体だけ返すテスト用の[CharacterRepository]。 */
    private class FakeCharacterRepository(private val existing: Character? = null) : CharacterRepository {
        var saved: CharacterDraft? = null
        override fun observeCharacters(): Flow<List<Character>> = flowOf(listOfNotNull(existing))
        override suspend fun getCharacter(id: Int): Character? = existing?.takeIf { it.id == id }
        override suspend fun saveCharacter(draft: CharacterDraft): Character {
            saved = draft
            return Character(1, draft.name, 0, 0, 0, 0, 0, 0, 0, 0, draft.activeSkillIds, draft.passiveSkillIds)
        }
        override suspend fun deleteCharacter(id: Int) = error("not used")
    }

    private fun createViewModel(
        characterId: Int? = null,
        repository: FakeCharacterRepository = FakeCharacterRepository(),
        activeCount: Int = 10,
    ) = CharacterCreateViewModel(
        characterId = characterId,
        getCharacterUseCase = GetCharacterUseCase(repository),
        saveCharacterUseCase = SaveCharacterUseCase(repository),
        getSkillsUseCase = GetSkillsUseCase(FakeSkillRepository(activeCount)),
    )

    /** スキルを追加でき、重複した追加はできず、追加済みのスキルは選択肢から消えること。 */
    @Test
    fun addActiveSkill_addsOnce_andRemovedFromCandidates() = runTest(dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAddActiveSkillClick()
        assertEquals(SkillPickerKind.ACTIVE, viewModel.uiState.value.skillPicker)
        assertEquals(10, viewModel.uiState.value.skillPickerCandidates.size)
        viewModel.onSkillPicked(1)

        assertNull(viewModel.uiState.value.skillPicker)
        assertEquals(listOf(SkillUiModel(1, "A1")), viewModel.uiState.value.activeSkills)

        viewModel.onAddActiveSkillClick()
        assertTrue(viewModel.uiState.value.skillPickerCandidates.none { it.id == 1 })
        viewModel.onSkillPicked(1) // 選択肢にない(設定済みの)スキルは追加されない
        assertEquals(listOf(1), viewModel.uiState.value.activeSkillIds)
    }

    /** 上限(8個)に達すると追加できなくなり、削除すると再び追加できること。 */
    @Test
    fun activeSkills_limitIsEnforced() = runTest(dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()
        repeat(SkillLimits.MAX_ACTIVE_SKILLS) { index ->
            viewModel.onAddActiveSkillClick()
            viewModel.onSkillPicked(1 + index)
        }

        assertEquals(SkillLimits.MAX_ACTIVE_SKILLS, viewModel.uiState.value.activeSkillIds.size)
        assertFalse(viewModel.uiState.value.canAddActiveSkill)
        viewModel.onAddActiveSkillClick()
        assertNull(viewModel.uiState.value.skillPicker)

        viewModel.onRemoveActiveSkill(3)
        assertTrue(viewModel.uiState.value.canAddActiveSkill)
        assertFalse(3 in viewModel.uiState.value.activeSkillIds)
    }

    /** パッシブスキルの追加・削除ができ、アクティブスキルとは独立していること。 */
    @Test
    fun passiveSkills_addAndRemove() = runTest(dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAddPassiveSkillClick()
        assertEquals(SkillPickerKind.PASSIVE, viewModel.uiState.value.skillPicker)
        viewModel.onSkillPicked(1)
        assertEquals(listOf(1), viewModel.uiState.value.passiveSkillIds)
        assertTrue(viewModel.uiState.value.activeSkillIds.isEmpty())

        viewModel.onRemovePassiveSkill(1)
        assertTrue(viewModel.uiState.value.passiveSkillIds.isEmpty())
    }

    /** ダイアログを閉じると何も追加されないこと。 */
    @Test
    fun pickerDismiss_addsNothing() = runTest(dispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAddActiveSkillClick()
        viewModel.onSkillPickerDismiss()

        assertNull(viewModel.uiState.value.skillPicker)
        assertTrue(viewModel.uiState.value.activeSkillIds.isEmpty())
    }

    /** 保存時に、設定したスキルIDがドラフトへ含まれること。 */
    @Test
    fun save_includesSkillIds() = runTest(dispatcher) {
        val repository = FakeCharacterRepository()
        val viewModel = createViewModel(repository = repository)
        advanceUntilIdle()
        viewModel.onNameChange("勇者")
        viewModel.onAddActiveSkillClick()
        viewModel.onSkillPicked(2)
        viewModel.onAddActiveSkillClick()
        viewModel.onSkillPicked(1)
        viewModel.onAddPassiveSkillClick()
        viewModel.onSkillPicked(1)

        viewModel.onSaveClick()
        advanceUntilIdle()

        assertEquals(listOf(2, 1), repository.saved?.activeSkillIds)
        assertEquals(listOf(1), repository.saved?.passiveSkillIds)
        assertTrue(viewModel.uiState.value.isSaved)
    }

    /** カタログのスキルをすべて設定すると、追加ボタン(canAdd)が無効になること。 */
    @Test
    fun addDisabled_whenAllCatalogSkillsAreSet() = runTest(dispatcher) {
        val viewModel = createViewModel(activeCount = 2)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.canAddActiveSkill)

        viewModel.onAddActiveSkillClick()
        viewModel.onSkillPicked(1)
        viewModel.onAddActiveSkillClick()
        viewModel.onSkillPicked(2)

        assertFalse(viewModel.uiState.value.canAddActiveSkill)
        viewModel.onAddActiveSkillClick()
        assertNull(viewModel.uiState.value.skillPicker)
    }

    /** マスターデータに存在しないIDは上限の枠を消費せず、保存時に落とされること。 */
    @Test
    fun unknownIds_notCounted_andDroppedOnSave() = runTest(dispatcher) {
        val unknownIds = (900..907).toList()
        val existing = Character(
            5, "戦士", 1, 1, 1, 1, 1, 1, 1, 1,
            activeSkillIds = unknownIds + 3, passiveSkillIds = listOf(999, 1),
        )
        val repository = FakeCharacterRepository(existing)
        val viewModel = createViewModel(characterId = 5, repository = repository)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.activeSkills.size)
        assertTrue(viewModel.uiState.value.canAddActiveSkill)

        viewModel.onSaveClick()
        advanceUntilIdle()

        assertEquals(listOf(3), repository.saved?.activeSkillIds)
        assertEquals(listOf(1), repository.saved?.passiveSkillIds)
    }

    /** Aスキル1とPスキル1(同じ数値・別の種別)が互いに影響せず、追加・削除・保存が種別ごとに行われること。 */
    @Test
    fun sameIdAcrossKinds_areIndependent() = runTest(dispatcher) {
        val repository = FakeCharacterRepository()
        val viewModel = createViewModel(repository = repository)
        advanceUntilIdle()
        viewModel.onNameChange("勇者")

        viewModel.onAddActiveSkillClick()
        viewModel.onSkillPicked(1)
        viewModel.onAddPassiveSkillClick()
        // Aスキル1を設定済みでも、Pスキル1は選択肢に残る。
        assertEquals(listOf(SkillUiModel(1, "P")), viewModel.uiState.value.skillPickerCandidates)
        viewModel.onSkillPicked(1)
        assertEquals(listOf(1), viewModel.uiState.value.activeSkillIds)
        assertEquals(listOf(1), viewModel.uiState.value.passiveSkillIds)

        viewModel.onRemoveActiveSkill(1)
        assertTrue(viewModel.uiState.value.activeSkillIds.isEmpty())
        assertEquals(listOf(SkillUiModel(1, "P")), viewModel.uiState.value.passiveSkills)

        viewModel.onAddActiveSkillClick()
        viewModel.onSkillPicked(1)
        viewModel.onRemovePassiveSkill(1)
        assertEquals(listOf(SkillUiModel(1, "A1")), viewModel.uiState.value.activeSkills)
        assertTrue(viewModel.uiState.value.passiveSkills.isEmpty())

        viewModel.onAddPassiveSkillClick()
        viewModel.onSkillPicked(1)
        viewModel.onSaveClick()
        advanceUntilIdle()
        assertEquals(listOf(1), repository.saved?.activeSkillIds)
        assertEquals(listOf(1), repository.saved?.passiveSkillIds)
    }

    /** 既存キャラクターの編集では、保存済みのスキルIDが読み込まれること。 */
    @Test
    fun editing_loadsSavedSkillIds() = runTest(dispatcher) {
        val existing = Character(
            5, "戦士", 1, 1, 1, 1, 1, 1, 1, 1, activeSkillIds = listOf(3), passiveSkillIds = listOf(1),
        )
        val viewModel = createViewModel(characterId = 5, repository = FakeCharacterRepository(existing))

        advanceUntilIdle()

        assertEquals(listOf(3), viewModel.uiState.value.activeSkillIds)
        assertEquals(listOf(SkillUiModel(3, "A3")), viewModel.uiState.value.activeSkills)
        assertEquals(listOf(SkillUiModel(1, "P")), viewModel.uiState.value.passiveSkills)
    }
}
