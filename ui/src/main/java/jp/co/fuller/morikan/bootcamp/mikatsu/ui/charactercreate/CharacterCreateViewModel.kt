package jp.co.fuller.morikan.bootcamp.mikatsu.ui.charactercreate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.CharacterDraft
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.GetCharacterUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.SaveCharacterUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.charactercreate.model.CharacterCreateUiState
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
 * 既存データがあればフォームへ復元し、各ステータス入力の検証(半角数字のみ許可)と、
 * 保存処理の実行を担当することを目的とする。Screen側は本ViewModelの[uiState]を購読して
 * 描画するのみで、ロジックは持たない。
 *
 * @property characterId 編集対象のキャラクターID。新規作成の場合は`null`。
 * @property getCharacterUseCase 編集対象キャラクターの既存データを取得するUseCase。
 * @property saveCharacterUseCase 入力内容を保存するUseCase。
 */
@HiltViewModel(assistedFactory = CharacterCreateViewModel.Factory::class)
class CharacterCreateViewModel @AssistedInject constructor(
    @Assisted private val characterId: Int?,
    private val getCharacterUseCase: GetCharacterUseCase,
    private val saveCharacterUseCase: SaveCharacterUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CharacterCreateUiState(isEditing = characterId != null))

    /** Screenが購読する画面状態。 */
    val uiState = _uiState.asStateFlow()

    init {
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
     * 空文字のステータスは0として扱う。
     */
    fun onSaveClick() {
        val state = _uiState.value
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
