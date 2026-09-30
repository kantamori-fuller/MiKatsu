package jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Character
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Enemy
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.GetCharacterUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.GetEnemiesUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleUiState
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleUnitUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * バトル画面(BattleScreen)のViewModel。
 *
 * 編成画面で選択されたキャラクターID([partyCharacterIds])をもとに味方陣営のデータを、
 * [getEnemiesUseCase]経由で敵陣営のデータを取得し、[uiState]として公開することを目的とする。
 * 戦闘の具体的なロジック(ダメージ計算やターン進行など)は本ViewModelの責務ではなく、
 * 今後別途実装される想定である。
 *
 * @property partyCharacterIds 編成画面で選択された、味方として参戦するキャラクターのID一覧。
 * @property getEnemiesUseCase 今回のバトルに登場する敵の一覧を取得するUseCase。
 * @property getCharacterUseCase 指定IDのキャラクターを取得するUseCase。
 */
@HiltViewModel(assistedFactory = BattleViewModel.Factory::class)
class BattleViewModel @AssistedInject constructor(
    @Assisted private val partyCharacterIds: List<Int>,
    private val getEnemiesUseCase: GetEnemiesUseCase,
    private val getCharacterUseCase: GetCharacterUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BattleUiState())

    /** Screenが購読する画面状態。 */
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val enemies = getEnemiesUseCase().map(Enemy::toUiModel)
            val party = partyCharacterIds
                .mapNotNull { id -> getCharacterUseCase(id) }
                .map(Character::toUiModel)
            _uiState.update { it.copy(enemies = enemies, party = party) }
        }
    }

    /**
     * 「攻撃」ボタンが押されたときに呼ばれる。
     *
     * 現段階では戦闘ロジックが未実装のため何も行わない。今後の対応でダメージ計算等の
     * 処理をここに追加する想定である。
     */
    fun onAttackClick() = Unit

    /**
     * Hiltが[BattleViewModel]を生成するためのAssisted Factory。
     *
     * DIコンテナが解決できないルート由来の実行時の値([partyCharacterIds])を
     * 呼び出し側([BattleNavigation])から受け取るための橋渡し役を担う。
     */
    @AssistedFactory
    interface Factory {
        /**
         * @param partyCharacterIds 編成画面で選択された味方キャラクターのID一覧。
         * @return 生成された[BattleViewModel]。
         */
        fun create(partyCharacterIds: List<Int>): BattleViewModel
    }
}

/**
 * [Enemy]をバトル画面表示用の[BattleUnitUiModel]へ変換する。
 *
 * @receiver 変換元の敵。
 * @return 変換後の[BattleUnitUiModel]。
 */
private fun Enemy.toUiModel() = BattleUnitUiModel(
    id = id,
    name = name,
    hp = hp,
    maxHp = maxHp,
    mana = mana,
    maxMana = maxMana,
)

/**
 * [Character]をバトル画面表示用の[BattleUnitUiModel]へ変換する。
 *
 * [Character]は被ダメージ等の状態を持たないため、現在値・最大値ともに
 * ステータス値をそのまま用いる。
 *
 * @receiver 変換元のキャラクター。
 * @return 変換後の[BattleUnitUiModel]。
 */
private fun Character.toUiModel() = BattleUnitUiModel(
    id = id,
    name = name,
    hp = hp,
    maxHp = hp,
    mana = mana,
    maxMana = mana,
)
