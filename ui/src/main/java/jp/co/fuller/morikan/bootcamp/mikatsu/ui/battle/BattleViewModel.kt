package jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleAlly
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleEnemy
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Formation
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.AttackUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.EndTurnUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.ExecuteEnemyTurnUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.ObserveBattleFieldUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.StartBattleUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleNarration
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleUiState
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleUnitUiModel
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.resolveDisplayNames
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 敵の行動を開始してから攻撃を実行するまでの間(ミリ秒)。 */
private const val ENEMY_TURN_DELAY_MS = 800L

/** 攻撃結果のナレーションを表示してから手番を終えるまでの間(ミリ秒)。 */
private const val RESULT_DISPLAY_MS = 1200L

/**
 * バトル画面(BattleScreen)のViewModel。
 *
 * 編成画面で編成された陣形([formation])で[startBattleUseCase]により戦闘を開始し、
 * [observeBattleFieldUseCase]で購読した戦闘フィールド(現在HP・手番を含む。正はRepositoryが保持する)
 * をもとに、味方は陣形のマスごとに並べ直したうえで[uiState]として公開することを目的とする。
 * 手番が来た味方は自動で行動者となり、「攻撃」→敵のタップで[attackUseCase]による通常攻撃を行う。
 * 手番が敵のときは、間とナレーションを挟みながら[executeEnemyTurnUseCase]で自動的に攻撃させる。
 * 行動のたびに[endTurnUseCase]で手番を終える。
 *
 * @property formation 編成画面で編成された、味方として参戦するキャラクターの陣形。
 * @property startBattleUseCase 陣形から戦闘を開始するUseCase。
 * @property observeBattleFieldUseCase 戦闘フィールドの最新状態を購読するUseCase。
 * @property attackUseCase 味方の通常攻撃を実行するUseCase。
 * @property executeEnemyTurnUseCase 敵の手番の攻撃を実行するUseCase。
 * @property endTurnUseCase 現在の行動者の手番を終えるUseCase。
 */
@HiltViewModel(assistedFactory = BattleViewModel.Factory::class)
class BattleViewModel @AssistedInject constructor(
    @Assisted private val formation: Formation,
    private val startBattleUseCase: StartBattleUseCase,
    private val observeBattleFieldUseCase: ObserveBattleFieldUseCase,
    private val attackUseCase: AttackUseCase,
    private val executeEnemyTurnUseCase: ExecuteEnemyTurnUseCase,
    private val endTurnUseCase: EndTurnUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BattleUiState())

    /** Screenが購読する画面状態。 */
    val uiState = _uiState.asStateFlow()

    /** 敵の手番の演出ジョブ。二重に開始しないための目印として保持する。 */
    private var enemyTurnJob: Job? = null

    /** 味方の攻撃の演出ジョブ。二重に開始しないための目印として保持する。 */
    private var playerActionJob: Job? = null

    /** 演出中(敵の手番または味方の攻撃のジョブが動いている間)かどうか。 */
    private val isBusy: Boolean
        get() = enemyTurnJob?.isActive == true || playerActionJob?.isActive == true

    init {
        viewModelScope.launch {
            startBattleUseCase(formation)
            observeBattleFieldUseCase().collect { battleField ->
                _uiState.update { battleField.reflectedIn(it) }
                startEnemyTurnIfNeeded(battleField)
            }
        }
    }

    /**
     * 「攻撃」ボタンが押されたときに呼ばれる。
     *
     * 攻撃できる状態(プレイヤーの手番・演出中でない・勝敗未決)であれば対象選択状態へ移行する。
     * それ以外の場合は何も行わない。
     */
    fun onAttackClick() {
        _uiState.update {
            if (!it.isAttackEnabled) it else it.copy(isSelectingTarget = true)
        }
    }

    /**
     * 対象選択状態で「キャンセル」ボタンが押されたときに呼ばれる。
     *
     * 対象選択状態を解除し、行動選択に戻す。対象選択状態でなければ何も行わない。
     */
    fun onCancelClick() {
        _uiState.update { it.copy(isSelectingTarget = false) }
    }

    /**
     * 敵のカードがタップされたときに呼ばれる。
     *
     * 対象選択中・プレイヤーの手番・演出中でない・対象が戦闘不能でない、をすべて満たすときだけ、
     * 手番の味方からタップされた敵へ通常攻撃を行う。攻撃結果をナレーションで表示して間を置いたあと、
     * 手番を終える。条件を満たさなければ何も行わない。
     *
     * @param fpid タップされた敵のFPID([BattleUnitUiModel.id])。
     */
    fun onEnemyClick(fpid: Int) {
        val state = _uiState.value
        val attackerFpid = state.currentActorId ?: return
        val target = state.enemies.firstOrNull { it.id == fpid } ?: return
        if (!state.isSelectingTarget || !state.isPlayerTurn || isBusy || target.isDefeated) return
        _uiState.update { it.copy(isProcessing = true, isSelectingTarget = false) }
        playerActionJob = viewModelScope.launch {
            val result = attackUseCase(attackerFpid = attackerFpid, targetFpid = fpid)
            if (result != null) {
                showAttackResult(result.attackerFpid, result.targetFpid, result.damage)
                delay(RESULT_DISPLAY_MS)
                endTurnUseCase(attackerFpid)
            }
            _uiState.update { it.copy(isProcessing = false, processingNarration = null) }
            playerActionJob = null
            startEnemyTurnIfNeeded(observeBattleFieldUseCase().first())
        }
    }

    /**
     * 現在の手番が敵で、演出中でなく、勝敗も決まっていなければ、敵の手番の演出ジョブを開始する。
     *
     * 判断には、古くなり得るUiStateではなく、Repositoryから流れてきた最新の[field]を用いる。
     * 流れは「演出中にする → 敵の行動のナレーション → 間 → 攻撃 → 攻撃結果のナレーション → 間 →
     * 手番終了 → 演出終了」。2つのジョブが動いている間は多重起動しない。
     *
     * @param field 判断に用いる、最新の戦闘フィールド。
     */
    private fun startEnemyTurnIfNeeded(field: BattleField) {
        if (isBusy || field.outcome != null) return
        val actorFpid = field.currentActorFpid ?: return
        if (field.enemies.none { it.fpid == actorFpid }) return
        _uiState.update {
            it.copy(isProcessing = true, processingNarration = BattleNarration.EnemyTurn(it.nameOf(actorFpid)))
        }
        enemyTurnJob = viewModelScope.launch {
            delay(ENEMY_TURN_DELAY_MS)
            val result = executeEnemyTurnUseCase(actorFpid)
            if (result != null) {
                showAttackResult(result.attackerFpid, result.targetFpid, result.damage)
                delay(RESULT_DISPLAY_MS)
            }
            endTurnUseCase(actorFpid)
            _uiState.update { it.copy(isProcessing = false, processingNarration = null) }
            enemyTurnJob = null
            startEnemyTurnIfNeeded(observeBattleFieldUseCase().first())
        }
    }

    /**
     * 攻撃結果のナレーションを表示状態にする。
     *
     * @param attackerFpid 攻撃した個体のFPID。
     * @param targetFpid 攻撃を受けた個体のFPID。
     * @param damage 与えたダメージ量。
     */
    private fun showAttackResult(attackerFpid: Int, targetFpid: Int, damage: Int) {
        _uiState.update {
            it.copy(
                processingNarration = BattleNarration.AttackResult(
                    attackerName = it.nameOf(attackerFpid),
                    targetName = it.nameOf(targetFpid),
                    damage = damage,
                ),
            )
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
 * 戦闘フィールドの最新状態を、現在のUiStateへ反映した新しいUiStateを返す。
 *
 * 同名の個体には[resolveDisplayNames]で表示名を付け、演出中の状態(演出フラグ・一時ナレーション)や
 * 対象選択状態は維持する。手番の個体が変わって味方の手番でなくなった場合は対象選択状態を解除する。
 *
 * @receiver 最新の戦闘フィールド。
 * @param current 反映先の現在のUiState。
 * @return 反映後のUiState。
 */
private fun BattleField.reflectedIn(current: BattleUiState): BattleUiState {
    val names = resolveDisplayNames(
        allies.map { it.fpid to it.character.name } + enemies.map { it.fpid to it.enemy.name },
    )
    val alliesBySlot = allies.associateBy(BattleAlly::slot)
    val actor = currentActorFpid
    val isPlayerTurn = allies.any { it.fpid == actor }
    return current.copy(
        enemies = enemies.map { it.toUiModel(names.getValue(it.fpid), it.fpid == actor) },
        partySlots = List(Formation.SLOT_COUNT) { slot ->
            alliesBySlot[slot]?.let { it.toUiModel(names.getValue(it.fpid), it.fpid == actor) }
        },
        currentActorId = actor,
        isPlayerTurn = isPlayerTurn,
        isSelectingTarget = current.isSelectingTarget && isPlayerTurn,
        outcome = outcome,
    )
}

/**
 * [BattleEnemy]をバトル画面表示用の[BattleUnitUiModel]へ変換する。
 *
 * 同じ敵種がフィールドに複数体登場してもUI上で区別できるよう、[id][BattleUnitUiModel.id]には
 * EID(敵種のID)ではなく、[BattleAlly]を含めたフィールド全体で一意な[BattleEnemy.fpid]を用いる。
 * HPは戦闘中に変化する[BattleEnemy.currentHp]を現在値、[BattleEnemy.maxHp]を最大値として用いる。
 *
 * @receiver 変換元のフィールド上の敵個体。
 * @param displayName 同名個体の区別を付けた表示名。
 * @param isActive 現在の手番の個体かどうか。
 * @return 変換後の[BattleUnitUiModel]。
 */
private fun BattleEnemy.toUiModel(displayName: String, isActive: Boolean) = BattleUnitUiModel(
    id = fpid,
    name = displayName,
    hp = currentHp,
    maxHp = maxHp,
    mana = enemy.mana,
    maxMana = enemy.mana,
    isDefeated = isDefeated,
    isActive = isActive,
)

/**
 * [BattleAlly]をバトル画面表示用の[BattleUnitUiModel]へ変換する。
 *
 * [id][BattleUnitUiModel.id]にはキャラクター自身のIDではなく、[BattleEnemy]を含めた
 * フィールド全体で一意な[BattleAlly.fpid]を用いる。HPは戦闘中に変化する
 * [BattleAlly.currentHp]を現在値、[BattleAlly.maxHp]を最大値として用いる。
 *
 * @receiver 変換元のフィールド上の味方個体。
 * @param displayName 同名個体の区別を付けた表示名。
 * @param isActive 現在の手番の個体かどうか。
 * @return 変換後の[BattleUnitUiModel]。
 */
private fun BattleAlly.toUiModel(displayName: String, isActive: Boolean) = BattleUnitUiModel(
    id = fpid,
    name = displayName,
    hp = currentHp,
    maxHp = maxHp,
    mana = character.mana,
    maxMana = character.mana,
    isDefeated = isDefeated,
    isActive = isActive,
)
