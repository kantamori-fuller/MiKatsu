package jp.co.fuller.morikan.bootcamp.mikatsu.ui.charactercreate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.AilmentResistanceFormula
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.ElementResistanceFormula
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AilmentResistanceType
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AilmentResistances
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.CharacterDraft
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.ElementResistances
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillElement
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillLimits
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.GetCharacterUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.GetSkillsUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.SaveCharacterUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.charactercreate.model.CharacterCreateUiState
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.charactercreate.model.SkillPickerKind
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.charactercreate.model.SkillUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * キャラ作成/編集画面(CharacterCreateScreen)のViewModel。
 *
 * 編集対象のキャラクターID([characterId])は、Nav3のルート([CharacterCreateRoute])が
 * 直接保持する値をAssisted Injectionで受け取る。Nav3はNav2と異なりバックスタックの
 * エントリーにナビゲーション引数用のSavedStateHandleを自動で紐付けないため、
 * DI(Hilt)で解決できる依存(UseCase)とルートが持つ実行時の値(characterId)を
 * `@AssistedFactory`で組み合わせて生成する。
 *
 * 既存データがあればフォームへ復元し、各ステータス入力の検証(半角数字のみ許可。属性耐性は先頭の「-」と-999〜999の範囲も)と、
 * 保存処理の実行、およびAスキル・Pスキルの追加・削除(それぞれ上限は[SkillLimits]、重複なし)を担当することを目的とする。Screen側は本ViewModelの[uiState]を購読して
 * 描画するのみで、ロジックは持たない。
 *
 * @property characterId 編集対象のキャラクターID。新規作成の場合は`null`。
 * @property getCharacterUseCase 編集対象キャラクターの既存データを取得するUseCase。
 * @property saveCharacterUseCase 入力内容を保存するUseCase。
 * @property getSkillsUseCase 設定可能なスキルの一覧を取得するUseCase。
 */
@HiltViewModel(assistedFactory = CharacterCreateViewModel.Factory::class)
class CharacterCreateViewModel @AssistedInject constructor(
    @Assisted private val characterId: Int?,
    private val getCharacterUseCase: GetCharacterUseCase,
    private val saveCharacterUseCase: SaveCharacterUseCase,
    private val getSkillsUseCase: GetSkillsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CharacterCreateUiState(isEditing = characterId != null))

    /** Screenが購読する画面状態。 */
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val catalog = getSkillsUseCase()
            _uiState.update {
                it.copy(
                    activeSkillCatalog = catalog.activeSkills.map { skill -> SkillUiModel(skill.id, skill.name) },
                    passiveSkillCatalog = catalog.passiveSkills.map { skill -> SkillUiModel(skill.id, skill.name) },
                    isSkillCatalogLoaded = true,
                )
            }
        }
        val id = characterId
        if (id != null) {
            viewModelScope.launch {
                getCharacterUseCase(id)?.let { character ->
                    _uiState.update {
                        it.copy(
                            name = character.name,
                            hp = character.hp.toString(),
                            mana = character.mana.toString(),
                            attack = character.attack.toString(),
                            defense = character.defense.toString(),
                            manaOutput = character.manaOutput.toString(),
                            manaResistance = character.manaResistance.toString(),
                            agility = character.agility.toString(),
                            luck = character.luck.toString(),
                            elementResistances = ElementResistances.ORDER.associateWith { element ->
                                character.elementResistances.of(element).toString()
                            },
                            ailmentResistances = AilmentResistanceType.entries.associateWith { type ->
                                character.ailmentResistances.of(type).toString()
                            },
                            activeSkillIds = character.activeSkillIds,
                            passiveSkillIds = character.passiveSkillIds,
                        )
                    }
                }
            }
        }
    }

    /**
     * 名前フィールドの入力が変化したときに呼ばれる。
     *
     * @param value 入力後の名前。
     */
    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value) }
    }

    /**
     * H. 体力フィールドの入力が変化したときに呼ばれる。
     *
     * @param value 入力後の文字列。半角数字のみ許可される。
     */
    fun onHpChange(value: String) = updateStat(value) { copy(hp = it) }

    /**
     * M. マナ総量フィールドの入力が変化したときに呼ばれる。
     *
     * @param value 入力後の文字列。半角数字のみ許可される。
     */
    fun onManaChange(value: String) = updateStat(value) { copy(mana = it) }

    /**
     * A. 破壊力フィールドの入力が変化したときに呼ばれる。
     *
     * @param value 入力後の文字列。半角数字のみ許可される。
     */
    fun onAttackChange(value: String) = updateStat(value) { copy(attack = it) }

    /**
     * B. 耐久力フィールドの入力が変化したときに呼ばれる。
     *
     * @param value 入力後の文字列。半角数字のみ許可される。
     */
    fun onDefenseChange(value: String) = updateStat(value) { copy(defense = it) }

    /**
     * C. マナ出力フィールドの入力が変化したときに呼ばれる。
     *
     * @param value 入力後の文字列。半角数字のみ許可される。
     */
    fun onManaOutputChange(value: String) = updateStat(value) { copy(manaOutput = it) }

    /**
     * D. マナ耐性フィールドの入力が変化したときに呼ばれる。
     *
     * @param value 入力後の文字列。半角数字のみ許可される。
     */
    fun onManaResistanceChange(value: String) = updateStat(value) { copy(manaResistance = it) }

    /**
     * S. 敏捷フィールドの入力が変化したときに呼ばれる。
     *
     * @param value 入力後の文字列。半角数字のみ許可される。
     */
    fun onAgilityChange(value: String) = updateStat(value) { copy(agility = it) }

    /**
     * L. 幸運フィールドの入力が変化したときに呼ばれる。
     *
     * @param value 入力後の文字列。半角数字のみ許可される。
     */
    fun onLuckChange(value: String) = updateStat(value) { copy(luck = it) }

    /**
     * 属性耐性フィールドの入力が変化したときに呼ばれる。
     *
     * 入力できるのは、空欄、先頭の「-」だけ、または「-」(任意)に続く半角数字で、整数として
     * [ElementResistanceFormula.MIN]〜[ElementResistanceFormula.MAX]に収まるものだけ。それ以外の入力は弾いて状態を変えない。
     * 耐性項目のない属性(無・龍)の入力は無視する。
     *
     * @param element 入力が変化した属性。
     * @param value 入力後の文字列。
     */
    fun onElementResistanceChange(element: SkillElement, value: String) {
        if (element !in ElementResistances.ORDER || !isValidResistanceInput(value)) return
        _uiState.update { it.copy(elementResistances = it.elementResistances + (element to value)) }
    }

    /**
     * 状態異常耐性フィールドの入力が変化したときに呼ばれる。
     *
     * 入力できるのは、空欄、または半角数字のみで、整数として
     * [AilmentResistanceFormula.MIN]〜[AilmentResistanceFormula.MAX]に収まるものだけ(先頭の「-」は不可)。
     * それ以外の入力は弾いて状態を変えない。
     *
     * @param type 入力が変化した項目。
     * @param value 入力後の文字列。
     */
    fun onAilmentResistanceChange(type: AilmentResistanceType, value: String) {
        if (!isValidAilmentResistanceInput(value)) return
        _uiState.update { it.copy(ailmentResistances = it.ailmentResistances + (type to value)) }
    }

    /**
     * 状態異常耐性の入力途中の文字列として許せるかを判定する。
     *
     * @param value 検証対象の入力値。
     * @return 空欄、または半角数字のみで、整数として0〜100に収まるなら`true`。
     */
    private fun isValidAilmentResistanceInput(value: String): Boolean {
        if (value.isEmpty()) return true
        if (!value.all { it in '0'..'9' }) return false
        val parsed = value.toIntOrNull() ?: return false
        return parsed in AilmentResistanceFormula.MIN..AilmentResistanceFormula.MAX
    }

    /**
     * 属性耐性の入力途中の文字列として許せるかを判定する。
     *
     * @param value 検証対象の入力値。
     * @return 空欄、「-」だけ、または「-」(任意)+半角数字で、整数として範囲内に収まるなら`true`。
     */
    private fun isValidResistanceInput(value: String): Boolean {
        if (value.isEmpty() || value == "-") return true
        val digits = value.removePrefix("-")
        if (digits.isEmpty() || !digits.all { it in '0'..'9' }) return false
        val parsed = value.toIntOrNull() ?: return false
        return parsed in ElementResistanceFormula.MIN..ElementResistanceFormula.MAX
    }

    /**
     * アクティブスキルの「追加」が押されたときに呼ばれる。
     *
     * 上限([SkillLimits.MAX_ACTIVE_SKILLS])に達していなければ、追加するスキルを選ぶダイアログを開く。
     */
    fun onAddActiveSkillClick() {
        _uiState.update { if (it.canAddActiveSkill) it.copy(skillPicker = SkillPickerKind.ACTIVE) else it }
    }

    /**
     * パッシブスキルの「追加」が押されたときに呼ばれる。
     *
     * 上限([SkillLimits.MAX_PASSIVE_SKILLS])に達していなければ、追加するスキルを選ぶダイアログを開く。
     */
    fun onAddPassiveSkillClick() {
        _uiState.update { if (it.canAddPassiveSkill) it.copy(skillPicker = SkillPickerKind.PASSIVE) else it }
    }

    /** スキル追加ダイアログが閉じられた(キャンセルされた)ときに呼ばれる。 */
    fun onSkillPickerDismiss() {
        _uiState.update { it.copy(skillPicker = null) }
    }

    /**
     * スキル追加ダイアログでスキルが選ばれたときに呼ばれる。
     *
     * ダイアログの種別に応じたスキルの設定リストの末尾へ追加してダイアログを閉じる。
     * すでに設定済み・上限に達している・選択肢にないスキルの場合は追加しない。
     *
     * @param skillId 選ばれたスキルのID。
     */
    fun onSkillPicked(skillId: Int) {
        _uiState.update { state ->
            if (state.skillPicker == null || state.skillPickerCandidates.none { it.id == skillId }) {
                return@update state.copy(skillPicker = null)
            }
            when (state.skillPicker) {
                SkillPickerKind.ACTIVE -> state.copy(
                    activeSkillIds = if (state.canAddActiveSkill) state.activeSkillIds + skillId else state.activeSkillIds,
                    skillPicker = null,
                )
                SkillPickerKind.PASSIVE -> state.copy(
                    passiveSkillIds = if (state.canAddPassiveSkill) state.passiveSkillIds + skillId else state.passiveSkillIds,
                    skillPicker = null,
                )
            }
        }
    }

    /**
     * 設定済みアクティブスキルの「削除」が押されたときに呼ばれる。
     *
     * @param skillId 外すスキルのID。
     */
    fun onRemoveActiveSkill(skillId: Int) {
        _uiState.update { it.copy(activeSkillIds = it.activeSkillIds - skillId) }
    }

    /**
     * 設定済みパッシブスキルの「削除」が押されたときに呼ばれる。
     *
     * @param skillId 外すスキルのID。
     */
    fun onRemovePassiveSkill(skillId: Int) {
        _uiState.update { it.copy(passiveSkillIds = it.passiveSkillIds - skillId) }
    }

    /**
     * ステータス入力フィールド共通の更新処理。
     *
     * 入力値が半角数字のみで構成されている場合のみ状態を更新することで、
     * 「半角数字のみ入力可能」という入力制約をViewModel側で一元的に守る。
     *
     * @param value 検証対象の入力値。
     * @param update 検証を通過した場合に適用する、UiStateの更新処理。
     */
    private inline fun updateStat(
        value: String,
        crossinline update: CharacterCreateUiState.(String) -> CharacterCreateUiState,
    ) {
        if (value.all { it in '0'..'9' }) {
            _uiState.update { it.update(value) }
        }
    }

    /**
     * 「保存」ボタンが押されたときに呼ばれる。
     *
     * 現在の入力内容から[CharacterDraft]を組み立てて保存し、完了後に
     * [CharacterCreateUiState.isSaved]を`true`にすることでScreen側へ画面遷移を促す。
     * 空文字のステータスは0として扱い、空欄や「-」だけの属性耐性は100として扱う。名前が未入力の場合は何もしない。
     * スキルIDは、マスターデータの読み込み済みなら存在するもの(設定順)だけを保存し、存在しないIDは落とす。
     * 未読み込みの場合は、設定済みのIDを失わないようそのまま保存する。
     */
    fun onSaveClick() {
        val state = _uiState.value
        if (!state.isSaveEnabled) return
        viewModelScope.launch {
            saveCharacterUseCase(
                CharacterDraft(
                    id = characterId,
                    name = state.name,
                    hp = state.hp.toIntOrNull() ?: 0,
                    mana = state.mana.toIntOrNull() ?: 0,
                    attack = state.attack.toIntOrNull() ?: 0,
                    defense = state.defense.toIntOrNull() ?: 0,
                    manaOutput = state.manaOutput.toIntOrNull() ?: 0,
                    manaResistance = state.manaResistance.toIntOrNull() ?: 0,
                    agility = state.agility.toIntOrNull() ?: 0,
                    luck = state.luck.toIntOrNull() ?: 0,
                    elementResistances = ElementResistances.ORDER.fold(ElementResistances()) { resistances, element ->
                        // 空欄や「-」だけの欄は等倍(100)にする。
                        val value = state.elementResistances[element]?.toIntOrNull() ?: ElementResistanceFormula.NEUTRAL
                        resistances.with(element, ElementResistanceFormula.clamp(value))
                    },
                    ailmentResistances = AilmentResistanceType.entries.fold(AilmentResistances()) { resistances, type ->
                        // 空欄は100にする。
                        val value = state.ailmentResistances[type]?.toIntOrNull() ?: AilmentResistanceFormula.NEUTRAL
                        resistances.with(type, AilmentResistanceFormula.clamp(value))
                    },
                    activeSkillIds = if (state.isSkillCatalogLoaded) state.activeSkills.map { it.id } else state.activeSkillIds,
                    passiveSkillIds = if (state.isSkillCatalogLoaded) state.passiveSkills.map { it.id } else state.passiveSkillIds,
                )
            )
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    /**
     * Hiltが[CharacterCreateViewModel]を生成するためのAssisted Factory。
     *
     * DIコンテナが解決できないルート由来の実行時の値([characterId])を
     * 呼び出し側([CharacterCreateNavigation])から受け取るための橋渡し役を担う。
     */
    @AssistedFactory
    interface Factory {
        /**
         * @param characterId 編集対象のキャラクターID。新規作成の場合は`null`。
         * @return 生成された[CharacterCreateViewModel]。
         */
        fun create(characterId: Int?): CharacterCreateViewModel
    }
}
