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

/**
 * キャラ一覧画面(CharacterListScreen)のViewModel。
 *
 * 保存済みキャラクターの一覧表示と、削除確認ダイアログの表示制御・削除実行を
 * 担当することを目的とする。Screen側は本ViewModelの[uiState]を購読して描画するのみで、
 * ロジックは持たない。
 *
 * @property deleteCharacterUseCase キャラクターを削除するUseCase。
 * @constructor Hiltがコンストラクタインジェクションで生成する。
 * @param observeCharactersUseCase 全キャラクター一覧を購読するUseCase。
 */
@HiltViewModel
class CharacterListViewModel @Inject constructor(
    observeCharactersUseCase: ObserveCharactersUseCase,
    private val deleteCharacterUseCase: DeleteCharacterUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CharacterListUiState())

    /** Screenが購読する画面状態。 */
    val uiState = _uiState.asStateFlow()

    init {
        observeCharactersUseCase()
            .onEach { characters -> _uiState.update { it.copy(characters = characters) } }
            .launchIn(viewModelScope)
    }

    /**
     * 一覧アイテムのゴミ箱アイコンが押されたときに呼ばれる。
     *
     * 削除確認ダイアログを表示するため、[CharacterListUiState.pendingDeleteId]に
     * 対象のIDを設定する。
     *
     * @param id 削除対象として選択されたキャラクターのID。
     */
    fun onDeleteRequested(id: Int) {
        _uiState.update { it.copy(pendingDeleteId = id) }
    }

    /**
     * 削除確認ダイアログの「キャンセル」または枠外タップで呼ばれる。
     *
     * ダイアログを閉じるため、[CharacterListUiState.pendingDeleteId]を`null`に戻す。
     */
    fun onDeleteCancelled() {
        _uiState.update { it.copy(pendingDeleteId = null) }
    }

    /**
     * 削除確認ダイアログの「削除」が押されたときに呼ばれる。
     *
     * [CharacterListUiState.pendingDeleteId]が示すキャラクターを実際に削除し、
     * 完了後にダイアログを閉じる。一覧の再表示は[ObserveCharactersUseCase]経由で
     * 自動的に反映される。
     */
    fun onDeleteConfirmed() {
        val id = _uiState.value.pendingDeleteId ?: return
        viewModelScope.launch {
            deleteCharacterUseCase(id)
            _uiState.update { it.copy(pendingDeleteId = null) }
        }
    }
}
