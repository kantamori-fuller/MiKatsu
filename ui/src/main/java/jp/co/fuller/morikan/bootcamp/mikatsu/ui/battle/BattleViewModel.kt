package jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleAlly
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleEnemy
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.GetBattleFieldUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleUiState
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleUnitUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * バトル画面(BattleScreen)のViewModel。
 *
 * 編成画面で選択されたキャラクターID([partyCharacterIds])をもとに、[getBattleFieldUseCase]
 * 経由でFPID(フィールドポーンID)を割り当てた味方・敵陣営のデータを取得し、[uiState]として
 * 公開することを目的とする。戦闘の具体的なロジック(ダメージ計算やターン進行など)は
 * 本ViewModelの責務ではなく、今後別途実装される想定である。
 *
 * @property partyCharacterIds 編成画面で選択された、味方として参戦するキャラクターのID一覧。
 * @property getBattleFieldUseCase 今回のバトルの味方・敵に、陣営を通じて一意なFPIDを
 *   割り当てたフィールドデータを取得するUseCase。
 */
@HiltViewModel(assistedFactory = BattleViewModel.Factory::class)
class BattleViewModel @AssistedInject constructor(
    @Assisted private val partyCharacterIds: List<Int>,
    private val getBattleFieldUseCase: GetBattleFieldUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BattleUiState())

    /** Screenが購読する画面状態。 */
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val battleField = getBattleFieldUseCase(partyCharacterIds)
            _uiState.update {
                it.copy(
                    enemies = battleField.enemies.map(BattleEnemy::toUiModel),
                    party = battleField.allies.map(BattleAlly::toUiModel),
                )
            }
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
 * [BattleEnemy]をバトル画面表示用の[BattleUnitUiModel]へ変換する。
 *
 * 同じ敵種がフィールドに複数体登場してもUI上で区別できるよう、[id][BattleUnitUiModel.id]には
 * EID(敵種のID)ではなく、[BattleAlly]を含めたフィールド全体で一意な[BattleEnemy.fpid]を用いる。
 * また敵は被ダメージ等の状態を持たないため、現在値・最大値ともにステータス値をそのまま用いる。
 *
 * @receiver 変換元のフィールド上の敵個体。
 * @return 変換後の[BattleUnitUiModel]。
 */
private fun BattleEnemy.toUiModel() = BattleUnitUiModel(
    id = fpid,
    name = enemy.name,
    hp = enemy.hp,
    maxHp = enemy.hp,
    mana = enemy.mana,
    maxMana = enemy.mana,
)

/**
 * [BattleAlly]をバトル画面表示用の[BattleUnitUiModel]へ変換する。
 *
 * [id][BattleUnitUiModel.id]にはキャラクター自身のIDではなく、[BattleEnemy]を含めた
 * フィールド全体で一意な[BattleAlly.fpid]を用いる。また味方は被ダメージ等の状態を
 * 持たないため、現在値・最大値ともにステータス値をそのまま用いる。
 *
 * @receiver 変換元のフィールド上の味方個体。
 * @return 変換後の[BattleUnitUiModel]。
 */
private fun BattleAlly.toUiModel() = BattleUnitUiModel(
    id = fpid,
    name = character.name,
    hp = character.hp,
    maxHp = character.hp,
    mana = character.mana,
    maxMana = character.mana,
)
