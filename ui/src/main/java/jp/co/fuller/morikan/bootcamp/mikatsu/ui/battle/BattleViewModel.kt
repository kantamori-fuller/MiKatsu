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
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.AttackUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.ObserveBattleFieldUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.StartBattleUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleUiState
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleUnitUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * バトル画面(BattleScreen)のViewModel。
 *
 * 編成画面で編成された陣形([formation])で[startBattleUseCase]により戦闘を開始し、
 * [observeBattleFieldUseCase]で購読した戦闘フィールド(現在HPを含む。正はRepositoryが保持する)
 * をもとに、味方は陣形のマスごとに並べ直したうえで[uiState]として公開することを目的とする。
 * 敵がタップされたときは[attackUseCase]で通常攻撃を実行する。ターン進行などは本ViewModelの
 * 責務ではなく、今後別途実装される想定である。
 *
 * @property formation 編成画面で編成された、味方として参戦するキャラクターの陣形。
 * @property startBattleUseCase 陣形から戦闘を開始するUseCase。
 * @property observeBattleFieldUseCase 戦闘フィールドの最新状態を購読するUseCase。
 * @property attackUseCase 通常攻撃を実行するUseCase。
 */
@HiltViewModel(assistedFactory = BattleViewModel.Factory::class)
class BattleViewModel @AssistedInject constructor(
    @Assisted private val formation: Formation,
    private val startBattleUseCase: StartBattleUseCase,
    private val observeBattleFieldUseCase: ObserveBattleFieldUseCase,
    private val attackUseCase: AttackUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BattleUiState())

    /** Screenが購読する画面状態。 */
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            startBattleUseCase(formation)
            observeBattleFieldUseCase().collect { battleField ->
                val alliesBySlot = battleField.allies.associateBy(BattleAlly::slot)
                _uiState.update {
                    it.copy(
                        enemies = battleField.enemies.map(BattleEnemy::toUiModel),
                        partySlots = List(Formation.SLOT_COUNT) { slot -> alliesBySlot[slot]?.toUiModel() },
                    )
                }
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
     * 何も行わない。
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
     * 対象選択状態で味方が選択されていれば、その味方からタップされた敵へ通常攻撃を行って
     * ダメージを与え、味方の選択と対象選択状態をともに解除して戦闘開始時と同じ状態
     * (キャラ選択待ち)に戻す。対象選択状態でなければ何も行わない。
     *
     * @param fpid タップされた敵のFPID([BattleUnitUiModel.id])。
     */
    fun onEnemyClick(fpid: Int) {
        val state = _uiState.value
        val attackerFpid = state.selectedAllyId
        if (!state.isSelectingTarget || attackerFpid == null) return
        _uiState.update { it.copy(selectedAllyId = null, isSelectingTarget = false) }
        viewModelScope.launch {
            attackUseCase(attackerFpid = attackerFpid, targetFpid = fpid)
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
 * HPは戦闘中に変化する[BattleEnemy.currentHp]を現在値、[BattleEnemy.maxHp]を最大値として用いる。
 *
 * @receiver 変換元のフィールド上の敵個体。
 * @return 変換後の[BattleUnitUiModel]。
 */
private fun BattleEnemy.toUiModel() = BattleUnitUiModel(
    id = fpid,
    name = enemy.name,
    hp = currentHp,
    maxHp = maxHp,
    mana = enemy.mana,
    maxMana = enemy.mana,
)

/**
 * [BattleAlly]をバトル画面表示用の[BattleUnitUiModel]へ変換する。
 *
 * [id][BattleUnitUiModel.id]にはキャラクター自身のIDではなく、[BattleEnemy]を含めた
 * フィールド全体で一意な[BattleAlly.fpid]を用いる。HPは戦闘中に変化する
 * [BattleAlly.currentHp]を現在値、[BattleAlly.maxHp]を最大値として用いる。
 *
 * @receiver 変換元のフィールド上の味方個体。
 * @return 変換後の[BattleUnitUiModel]。
 */
private fun BattleAlly.toUiModel() = BattleUnitUiModel(
    id = fpid,
    name = character.name,
    hp = currentHp,
    maxHp = maxHp,
    mana = character.mana,
    maxMana = character.mana,
)
