package jp.co.fuller.morikan.bootcamp.mikatsu.ui.partyformation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.ObserveCharactersUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.partyformation.model.PartyFormationUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * 編成画面(PartyFormationScreen)のViewModel。
 *
 * 保存済みキャラクターの一覧表示と、バトルへ連れて行くキャラクターの選択状態管理を
 * 担当することを目的とする。Screen側は本ViewModelの[uiState]を購読して描画するのみで、
 * ロジックは持たない。
 *
 * @constructor Hiltがコンストラクタインジェクションで生成する。
 * @param observeCharactersUseCase 全キャラクター一覧を購読するUseCase。
 */
@HiltViewModel
class PartyFormationViewModel @Inject constructor(
    observeCharactersUseCase: ObserveCharactersUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PartyFormationUiState())

    /** Screenが購読する画面状態。 */
    val uiState = _uiState.asStateFlow()

    init {
        observeCharactersUseCase()
            .onEach { characters -> _uiState.update { it.copy(characters = characters) } }
            .launchIn(viewModelScope)
    }

    /**
     * 一覧アイテムがタップされたときに呼ばれる。
     *
     * 既に選択済みの場合は選択を解除し、未選択の場合は選択状態にする。ただし選択数が
     * 上限([MAX_SELECTABLE_COUNT])に達している状態で未選択のキャラクターがタップされた
     * 場合は何もしない。
     *
     * @param id タップされたキャラクターのID。
     */
    fun onToggleSelected(id: Int) {
        _uiState.update { state ->
            when {
                id in state.selectedIds -> state.copy(selectedIds = state.selectedIds - id)
                state.selectedIds.size >= MAX_SELECTABLE_COUNT -> state
                else -> state.copy(selectedIds = state.selectedIds + id)
            }
        }
    }

    companion object {
        /** 編成に選択できるキャラクターの最大数。 */
        private const val MAX_SELECTABLE_COUNT = 2
    }
}
