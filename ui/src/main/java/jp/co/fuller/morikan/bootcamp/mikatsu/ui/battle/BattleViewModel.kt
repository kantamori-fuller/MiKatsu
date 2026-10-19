package jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleAlly
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleEnemy
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Formation
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
 * 編成画面で編成された陣形([formation])をもとに、[getBattleFieldUseCase]
 * 経由でFPID(フィールドポーンID)を割り当てた味方・敵陣営のデータを取得し、味方は陣形の
 * マスごとに並べ直したうえで[uiState]として公開することを目的とする。戦闘の具体的なロジック(ダメージ計算やターン進行など)は
 * 本ViewModelの責務ではなく、今後別途実装される想定である。
 *
 * @property formation 編成画面で編成された、味方として参戦するキャラクターの陣形。
 * @property getBattleFieldUseCase 今回のバトルの味方・敵に、陣営を通じて一意なFPIDを
 *   割り当てたフィールドデータを取得するUseCase。
 */
@HiltViewModel(assistedFactory = BattleViewModel.Factory::class)
class BattleViewModel @AssistedInject constructor(
    @Assisted private val formation: Formation,
    private val getBattleFieldUseCase: GetBattleFieldUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BattleUiState())

    /** Screenが購読する画面状態。 */
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val battleField = getBattleFieldUseCase(formation)
            val alliesBySlot = battleField.allies.associateBy(BattleAlly::slot)
            _uiState.update {
                it.copy(
                    enemies = battleField.enemies.map(BattleEnemy::toUiModel),
                    partySlots = List(Formation.SLOT_COUNT) { slot -> alliesBySlot[slot]?.toUiModel() },
                )
            }
        }
    }

    /**
     * 味方のカードがタップされたときに呼ばれる。
     *
     * 選択できる味方は同時に1体のみで、未選択のカードをタップするとその味方を選択状態にし
     * (既に別の味方が選択されていた場合は選択を切り替える)、選択中のカードを再度タップすると選択を解除する。
     * 攻撃対象の指定などで味方を一意に特定できるよう、味方はFPIDで識別する。
     * 対象選択状態の間は味方の選択を変更できないため、何も行わない。
     *
     * @param fpid タップされた味方のFPID([BattleUnitUiModel.id])。
     */
    fun onAllyClick(fpid: Int) {
        _uiState.update {
            if (it.isSelectingTarget) return@update it
            it.copy(selectedAllyId = if (it.selectedAllyId == fpid) null else fpid)
        }
    }

    /**
     * 「攻撃」ボタンが押されたときに呼ばれる。
     *
     * 味方が選択されていれば対象選択状態へ移行する。味方が未選択の場合や、既に対象選択状態の場合は
     * 何も行わない。ダメージ計算等の戦闘ロジックは未実装であり、今後の対応で追加する想定である。
     */
    fun onAttackClick() {
        _uiState.update {
            if (!it.isAttackEnabled) it else it.copy(isSelectingTarget = true)
        }
    }

    /**
     * 対象選択状態で「キャンセル」ボタンが押されたときに呼ばれる。
     *
     * 対象選択状態を解除し、味方を選択した状態(行動選択)に戻す。味方の選択は維持される。
     * 対象選択状態でなければ何も行わない。
     */
    fun onCancelClick() {
        _uiState.update { it.copy(isSelectingTarget = false) }
    }

    /**
     * 敵のカードがタップされたときに呼ばれる。
     *
     * 対象選択状態で敵が選択された場合、味方の選択と対象選択状態をともに解除して
     * 戦闘開始時と同じ状態(キャラ選択待ち)に戻す。ダメージ計算等は未実装のため、
     * どの敵が選ばれたかはまだ保持しない。対象選択状態でなければ何も行わない。
     *
     * @param fpid タップされた敵のFPID([BattleUnitUiModel.id])。
     */
    @Suppress("UNUSED_PARAMETER")
    fun onEnemyClick(fpid: Int) {
        _uiState.update {
            if (!it.isSelectingTarget) it else it.copy(selectedAllyId = null, isSelectingTarget = false)
        }
    }

    /**
     * Hiltが[BattleViewModel]を生成するためのAssisted Factory。
     *
     * DIコンテナが解決できないルート由来の実行時の値([formation])を
     * 呼び出し側([BattleNavigation])から受け取るための橋渡し役を担う。
     */
    @AssistedFactory
    interface Factory {
        /**
         * @param formation 編成画面で編成された味方キャラクターの陣形。
         * @return 生成された[BattleViewModel]。
         */
        fun create(formation: Formation): BattleViewModel
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
