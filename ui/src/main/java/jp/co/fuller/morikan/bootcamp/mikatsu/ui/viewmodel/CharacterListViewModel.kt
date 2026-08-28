package jp.co.fuller.morikan.bootcamp.mikatsu.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.DeleteCharacterUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.ObserveCharactersUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CharacterListViewModel @Inject constructor(
    observeCharactersUseCase: ObserveCharactersUseCase,
    private val deleteCharacterUseCase: DeleteCharacterUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CharacterListUiState())
    val uiState = _uiState.asStateFlow()

    init {
        observeCharactersUseCase()
            .onEach { characters -> _uiState.update { it.copy(characters = characters) } }
            .launchIn(viewModelScope)
    }

    fun onDeleteRequested(id: Int) {
        _uiState.update { it.copy(pendingDeleteId = id) }
    }

    fun onDeleteCancelled() {
        _uiState.update { it.copy(pendingDeleteId = null) }
    }

    fun onDeleteConfirmed() {
        val id = _uiState.value.pendingDeleteId ?: return
        viewModelScope.launch {
            deleteCharacterUseCase(id)
            _uiState.update { it.copy(pendingDeleteId = null) }
        }
    }
}
