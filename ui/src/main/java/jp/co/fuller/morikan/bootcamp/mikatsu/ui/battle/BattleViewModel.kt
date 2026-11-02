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
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleOutcome
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AttackResult
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Formation
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.SkillResult
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillCostType
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.canAfford
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.AdvanceToNextBattleUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.AttackUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.EndTurnUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.ExecuteEnemyTurnUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.ObserveBattleFieldUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.ObserveMapProgressUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.StartBattleUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.UseSkillUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleNarration
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleSelection
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleSkillUiModel
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
 * 手番が来た味方は自動で行動者となり、「攻撃」→敵のタップで[attackUseCase]による通常攻撃を、
 * 「スキル」→スキルの選択→味方のタップで[useSkillUseCase]によるスキルの使用を行う。
 * 手番が敵のときは、間とナレーションを挟みながら[executeEnemyTurnUseCase]で自動的に攻撃させる。
 * 行動のたびに[endTurnUseCase]で手番を終える。
 * 勝利後は「次に進む」で[advanceToNextBattleUseCase]により同じ編成のまま次の戦闘へ進み、
 * 進行度は[observeMapProgressUseCase]で購読して表示する(正はRepositoryが保持する)。
 *
 * @property formation 編成画面で編成された、味方として参戦するキャラクターの陣形。
 * @property startBattleUseCase 陣形から戦闘を開始するUseCase。
 * @property observeBattleFieldUseCase 戦闘フィールドの最新状態を購読するUseCase。
 * @property attackUseCase 味方の通常攻撃を実行するUseCase。
 * @property useSkillUseCase 味方のアクティブスキルを使うUseCase。
 * @property executeEnemyTurnUseCase 敵の手番の攻撃を実行するUseCase。
 * @property endTurnUseCase 現在の行動者の手番を終えるUseCase。
 * @property observeMapProgressUseCase マップの進行度を購読するUseCase。
 * @property advanceToNextBattleUseCase 勝利後に次の戦闘へ進めるUseCase。
 */
@HiltViewModel(assistedFactory = BattleViewModel.Factory::class)
class BattleViewModel @AssistedInject constructor(
    @Assisted private val formation: Formation,
    private val startBattleUseCase: StartBattleUseCase,
    private val observeBattleFieldUseCase: ObserveBattleFieldUseCase,
    private val attackUseCase: AttackUseCase,
    private val useSkillUseCase: UseSkillUseCase,
    private val executeEnemyTurnUseCase: ExecuteEnemyTurnUseCase,
    private val endTurnUseCase: EndTurnUseCase,
    private val observeMapProgressUseCase: ObserveMapProgressUseCase,
    private val advanceToNextBattleUseCase: AdvanceToNextBattleUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BattleUiState())

    /** Screenが購読する画面状態。 */
    val uiState = _uiState.asStateFlow()

    /** 敵の手番の演出ジョブ。二重に開始しないための目印として保持する。 */
    private var enemyTurnJob: Job? = null

    /** 味方の攻撃・スキルの演出ジョブ。二重に開始しないための目印として保持する。 */
    private var playerActionJob: Job? = null

    /** 次の戦闘へ進める処理のジョブ。連打で二重に進まないための目印として保持する。 */
    private var nextBattleJob: Job? = null

    /** 演出中(敵の手番または味方の攻撃・スキルのジョブが動いている間)かどうか。 */
    private val isBusy: Boolean
        get() = enemyTurnJob?.isActive == true || playerActionJob?.isActive == true

    init {
        viewModelScope.launch {
            startBattleUseCase(formation)
            // 進行度の購読は戦闘開始(進行度のリセット)の完了後に始める。前回の進行度が一瞬表示されるのを防ぐため。
            launch {
                observeMapProgressUseCase().collect { progress ->
                    _uiState.update { it.copy(progress = progress) }
                }
            }
            observeBattleFieldUseCase().collect { battleField ->
                _uiState.update { battleField.reflectedIn(it) }
                startEnemyTurnIfNeeded(battleField)
            }
        }
    }

    /**
     * 「攻撃」ボタンが押されたときに呼ばれる。
     *
     * 攻撃できる状態(プレイヤーの手番・演出中でない・勝敗未決・スキルの選択中でない)であれば
     * 対象選択状態へ移行する。それ以外の場合は何も行わない。
     */
    fun onAttackClick() {
        _uiState.update {
            if (!it.isAttackEnabled) it else it.copy(selection = BattleSelection.AttackTarget)
        }
    }

    /**
     * 「スキル」ボタンが押されたときに呼ばれる。
     *
     * スキルを選べる状態(プレイヤーの手番・演出中でない・勝敗未決・使えるスキルがある・攻撃対象の
     * 選択中でない)であればスキル一覧の選択状態へ移行する。それ以外の場合は何も行わない。
     */
    fun onSkillClick() {
        _uiState.update {
            if (!it.isSkillEnabled) it else it.copy(selection = BattleSelection.SkillList)
        }
    }

    /**
     * スキル一覧でスキルが選ばれたときに呼ばれる。
     *
     * スキル一覧の選択中で、選ばれたスキルが手番の味方のもので、現在のMPで使えるときだけ、
     * 対象の味方を選ぶ状態へ移行する。それ以外の場合は何も行わない。
     *
     * @param skillId 選ばれたスキルのID([BattleSkillUiModel.id])。
     */
    fun onSkillSelected(skillId: Int) {
        _uiState.update { state ->
            val skill = state.skills.firstOrNull { it.id == skillId }
            if (state.selection != BattleSelection.SkillList || !state.isSkillEnabled || skill?.isUsable != true) {
                state
            } else {
                state.copy(selection = BattleSelection.SkillTarget(skillId))
            }
        }
    }

    /**
     * 「キャンセル」ボタンが押されたときに呼ばれる。
     *
     * 攻撃の対象選択・スキル一覧・スキルの対象選択のいずれの状態からも、選択状態を解除して
     * 行動選択に戻す。選択状態でなければ何も行わない。
     */
    fun onCancelClick() {
        _uiState.update { it.copy(selection = BattleSelection.None) }
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
        launchPlayerAction(attackerFpid) {
            attackUseCase(attackerFpid = attackerFpid, targetFpid = fpid)?.let(::attackNarration)
        }
    }

    /**
     * 味方のカードがタップされたときに呼ばれる。
     *
     * スキルの対象選択中・プレイヤーの手番・演出中でない・対象が戦闘不能でない、をすべて満たすときだけ、
     * 選んでおいたスキルを手番の味方からタップされた味方へ使う。結果をナレーションで表示して間を置いた
     * あと、手番を終える。条件を満たさなければ何も行わない。
     *
     * @param fpid タップされた味方のFPID([BattleUnitUiModel.id])。
     */
    fun onAllyClick(fpid: Int) {
        val state = _uiState.value
        val actorFpid = state.currentActorId ?: return
        val selection = state.selection as? BattleSelection.SkillTarget ?: return
        val target = state.partySlots.firstOrNull { it?.id == fpid } ?: return
        if (!state.isPlayerTurn || isBusy || target.isDefeated) return
        launchPlayerAction(actorFpid) {
            useSkillUseCase(actorFpid = actorFpid, skillId = selection.skillId, targetFpid = fpid)
                ?.let(::skillNarration)
        }
    }

    /**
     * 味方の行動(通常攻撃・スキル)の演出ジョブを開始する。
     *
     * 流れは「演出中にして選択状態を解除 → 行動の実行 → 結果のナレーション → 間 → 手番終了 → 演出終了」。
     * 演出中の状態は同期的に設定するため、連打しても二重に開始されない。行動が実行できなかった場合
     * ([execute]が`null`)は、手番を終えずに行動選択へ戻す。
     *
     * @param actorFpid 行動する味方のFPID。
     * @param execute 行動を実行し、結果のナレーションを返す処理。実行できなければ`null`を返す。
     */
    private fun launchPlayerAction(actorFpid: Int, execute: suspend () -> BattleNarration?) {
        _uiState.update { it.copy(isProcessing = true, selection = BattleSelection.None) }
        playerActionJob = viewModelScope.launch {
            val narration = execute()
            if (narration != null) {
                _uiState.update { it.copy(processingNarration = narration) }
                delay(RESULT_DISPLAY_MS)
                endTurnUseCase(actorFpid)
            }
            _uiState.update { it.copy(isProcessing = false, processingNarration = null) }
            playerActionJob = null
            startEnemyTurnIfNeeded(observeBattleFieldUseCase().first())
        }
    }

    /**
     * 勝利オーバーレイの「次に進む」ボタンが押されたときに呼ばれる。
     *
     * 勝利していて、演出中でなく、すでに次の戦闘へ進める処理が動いていないときだけ、
     * [advanceToNextBattleUseCase]で次の戦闘を始める。新しいフィールドは既存の購読経由で
     * UiStateへ反映され、敵の先手もそこから始まる。条件を満たさなければ何も行わない。
     */
    fun onNextBattleClick() {
        if (_uiState.value.outcome != BattleOutcome.Victory || isBusy || nextBattleJob?.isActive == true) return
        nextBattleJob = viewModelScope.launch {
            advanceToNextBattleUseCase()
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
                _uiState.update { it.copy(processingNarration = attackNarration(result)) }
                delay(RESULT_DISPLAY_MS)
            }
            endTurnUseCase(actorFpid)
            _uiState.update { it.copy(isProcessing = false, processingNarration = null) }
            enemyTurnJob = null
            startEnemyTurnIfNeeded(observeBattleFieldUseCase().first())
        }
    }

    /**
     * 攻撃結果から、表示するナレーションを作る。
     *
     * @param result 攻撃の結果。
     * @return 誰が誰にどれだけのダメージを与えたかを伝えるナレーション。
     */
    private fun attackNarration(result: AttackResult): BattleNarration {
        val state = _uiState.value
        return BattleNarration.AttackResult(
            attackerName = state.nameOf(result.attackerFpid),
            targetName = state.nameOf(result.targetFpid),
            damage = result.damage,
        )
    }

    /**
     * スキルの結果から、表示するナレーションを作る。
     *
     * @param result スキルの結果。
     * @return 誰が誰にどのスキルを使い、HPがどれだけ回復したかを伝えるナレーション。
     */
    private fun skillNarration(result: SkillResult): BattleNarration {
        val state = _uiState.value
        return BattleNarration.SkillResult(
            actorName = state.nameOf(result.actorFpid),
            targetName = state.nameOf(result.targetFpid),
            skillName = result.skillName,
            healedAmount = result.healedAmount,
        )
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
 * 選択状態は維持する。手番の個体が変わって味方の手番でなくなった場合は選択状態を解除する。
 * 手番の味方が使えるスキルと、その時点のMPで使えるかどうかも反映する。
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
    val actorAlly = allies.firstOrNull { it.fpid == actor }
    return current.copy(
        enemies = enemies.map { it.toUiModel(names.getValue(it.fpid), it.fpid == actor) },
        partySlots = List(Formation.SLOT_COUNT) { slot ->
            alliesBySlot[slot]?.let { it.toUiModel(names.getValue(it.fpid), it.fpid == actor) }
        },
        currentActorId = actor,
        isPlayerTurn = isPlayerTurn,
        selection = if (isPlayerTurn) current.selection else BattleSelection.None,
        skills = actorAlly?.let { ally ->
            ally.activeSkills.map { skill ->
                BattleSkillUiModel(
                    id = skill.id,
                    name = skill.name,
                    mpCost = skill.costOf(SkillCostType.MP),
                    hpCost = skill.costOf(SkillCostType.HP),
                    isUsable = ally.canAfford(skill),
                )
            }
        }.orEmpty(),
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
 * [BattleAlly.currentHp]を現在値、[BattleAlly.maxHp]を最大値として用いる。MPも同様に
 * [BattleAlly.currentMp]を現在値、[BattleAlly.maxMp]を最大値として用いる。
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
    mana = currentMp,
    maxMana = maxMp,
    isDefeated = isDefeated,
    isActive = isActive,
)
