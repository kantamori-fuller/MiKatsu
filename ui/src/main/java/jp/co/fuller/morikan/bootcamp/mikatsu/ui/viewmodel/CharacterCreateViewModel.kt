package jp.co.fuller.morikan.bootcamp.mikatsu.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.CharacterDraft
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.GetCharacterUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.SaveCharacterUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.navigation.MiKatsuDestinations
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CharacterCreateViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getCharacterUseCase: GetCharacterUseCase,
    private val saveCharacterUseCase: SaveCharacterUseCase,
) : ViewModel() {

    private val characterId: Int? = savedStateHandle
        .get<Int>(MiKatsuDestinations.ARG_CHARACTER_ID)
        ?.takeIf { it != MiKatsuDestinations.NEW_CHARACTER_ID }

    private val _uiState = MutableStateFlow(CharacterCreateUiState(isEditing = characterId != null))
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

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value) }
    }

    fun onHpChange(value: String) = updateStat(value) { copy(hp = it) }
    fun onManaChange(value: String) = updateStat(value) { copy(mana = it) }
    fun onAttackChange(value: String) = updateStat(value) { copy(attack = it) }
    fun onDefenseChange(value: String) = updateStat(value) { copy(defense = it) }
    fun onManaOutputChange(value: String) = updateStat(value) { copy(manaOutput = it) }
    fun onManaResistanceChange(value: String) = updateStat(value) { copy(manaResistance = it) }
    fun onAgilityChange(value: String) = updateStat(value) { copy(agility = it) }
    fun onLuckChange(value: String) = updateStat(value) { copy(luck = it) }

    private inline fun updateStat(
        value: String,
        crossinline update: CharacterCreateUiState.(String) -> CharacterCreateUiState,
    ) {
        if (value.all { it in '0'..'9' }) {
            _uiState.update { it.update(value) }
        }
    }

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
}
