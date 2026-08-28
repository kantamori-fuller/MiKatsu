package jp.co.fuller.morikan.bootcamp.mikatsu.ui.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.DeleteCharacterUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.GetCharacterUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.ObserveCharactersUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.SaveCharacterUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.viewmodel.CharacterCreateViewModel
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.viewmodel.CharacterListViewModel

class MiKatsuViewModelFactory(
    private val observeCharactersUseCase: ObserveCharactersUseCase,
    private val getCharacterUseCase: GetCharacterUseCase,
    private val saveCharacterUseCase: SaveCharacterUseCase,
    private val deleteCharacterUseCase: DeleteCharacterUseCase,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        return when {
            modelClass.isAssignableFrom(CharacterListViewModel::class.java) ->
                CharacterListViewModel(observeCharactersUseCase, deleteCharacterUseCase)

            modelClass.isAssignableFrom(CharacterCreateViewModel::class.java) ->
                CharacterCreateViewModel(extras.createSavedStateHandle(), getCharacterUseCase, saveCharacterUseCase)

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        } as T
    }
}
