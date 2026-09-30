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
 * 保存済みキャラクターの一覧表示と、バトルへ連れて行くキャラクターの陣形(どのマスに誰を
 * 置くか)の編集を担当することを目的とする。配置のルール自体はドメインモデルである
 * [jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Formation]が持ち、本ViewModelは
 * 画面からのイベントをそれへ橋渡しする。Screen側は本ViewModelの[uiState]を購読して
 * 描画するのみで、ロジックは持たない。
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
     * キャラクターが陣形のマスへドロップされたときに呼ばれる。
     *
     * 一覧から新たに配置する場合と、既に配置済みのキャラクターを別のマスへ移動する場合の
     * 双方を扱う。配置の可否や入れ替えの挙動は
     * [Formation.place][jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Formation.place]に従う。
     *
     * @param characterId ドロップされたキャラクターのID。
     * @param slot ドロップ先のスロット番号。
     */
    fun onDropToSlot(characterId: Int, slot: Int) {
        _uiState.update { state -> state.copy(formation = state.formation.place(characterId, slot)) }
    }

    /**
     * 陣形のマスがタップされたときに呼ばれる。
     *
     * そのマスに配置されているキャラクターを陣形から外す。空きマスの場合は何も変わらない。
     *
     * @param slot タップされたマスのスロット番号。
     */
    fun onSlotClick(slot: Int) {
        _uiState.update { state -> state.copy(formation = state.formation.removeAt(slot)) }
    }
}
