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
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleFieldPawn
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AttackResult
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleOutcome
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Formation
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.SkillResult
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkill
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillCostType
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.BreakFormula
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.BattleRow
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.canAfford
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.effectiveRowOf
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.selectableTargets
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.AdvanceToNextBattleUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.AttackUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.EndTurnUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.ExecuteEnemyTurnUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.GetNormalAttackUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.ObserveBattleFieldUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.ObserveMapProgressUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.StartBattleUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.UseSkillUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleNarration
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleSelection
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleSkillUiModel
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleUiState
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleUnitDetailUiModel
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleUnitPosition
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleUnitUiModel
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.resolveDisplayNames
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.toNarrations
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.floor

/** 敵の行動を開始してから攻撃を実行するまでの間(ミリ秒)。 */
private const val ENEMY_TURN_DELAY_MS = 800L

/** 攻撃結果などの演出ナレーション1文あたりの表示時間(ミリ秒)。最後の文の表示後に手番を終える。 */
private const val RESULT_DISPLAY_MS = 1200L

/**
 * バトル画面(BattleScreen)のViewModel。
 *
 * 編成画面で編成された陣形([formation])で[startBattleUseCase]により戦闘を開始し、
 * [observeBattleFieldUseCase]で購読した戦闘フィールド(現在HP・手番を含む。正はRepositoryが保持する)
 * をもとに、味方は陣形のマスごとに並べ直したうえで[uiState]として公開することを目的とする。
 * 手番が来た味方は自動で行動者となり、「攻撃」→対象のタップで[attackUseCase]による通常攻撃を、
 * 「スキル」→スキルの選択→対象のタップで[useSkillUseCase]によるスキルの使用を行う。
 * タップできる対象は、通常攻撃のデータ([getNormalAttackUseCase])と手番の味方のスキルのレンジ・範囲・陣営の条件から
 * 求めた「選べる相手」だけで、敵・味方どちらのカードからでも選べる。
 * 手番が敵のときは、間とナレーションを挟みながら[executeEnemyTurnUseCase]で自動的に攻撃させる。
 * 行動のたびに[endTurnUseCase]で手番を終える。
 * 演出中のナレーションは複数の文の列として1文ずつ順に表示し、ナレーション欄のタップ([onNarrationClick])で
 * 表示時間の待ちを打ち切って次の文へ進められる。
 * 勝利後は「次に進む」で[advanceToNextBattleUseCase]により同じ編成のまま次の戦闘へ進み、
 * 進行度は[observeMapProgressUseCase]で購読して表示する(正はRepositoryが保持する)。
 *
 * @property formation 編成画面で編成された、味方として参戦するキャラクターの陣形。
 * @property startBattleUseCase 陣形から戦闘を開始するUseCase。
 * @property observeBattleFieldUseCase 戦闘フィールドの最新状態を購読するUseCase。
 * @property attackUseCase 味方の通常攻撃を実行するUseCase。
 * @property getNormalAttackUseCase 通常攻撃のスキルデータを取得するUseCase。通常攻撃で選べる相手の算出に使う。
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
    private val getNormalAttackUseCase: GetNormalAttackUseCase,
    private val useSkillUseCase: UseSkillUseCase,
    private val executeEnemyTurnUseCase: ExecuteEnemyTurnUseCase,
    private val endTurnUseCase: EndTurnUseCase,
    private val observeMapProgressUseCase: ObserveMapProgressUseCase,
    private val advanceToNextBattleUseCase: AdvanceToNextBattleUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BattleUiState())

    /** Screenが購読する画面状態。 */
    val uiState = _uiState.asStateFlow()

    /** 敵の手番、またはブレイク中の個体の手番飛ばしの演出ジョブ。二重に開始しないための目印として保持する。 */
    private var autoTurnJob: Job? = null

    /** 味方の攻撃・スキルの演出ジョブ。二重に開始しないための目印として保持する。 */
    private var playerActionJob: Job? = null

    /** 次の戦闘へ進める処理のジョブ。連打で二重に進まないための目印として保持する。 */
    private var nextBattleJob: Job? = null

    /**
     * 演出中のナレーション1文の表示時間の待ちを打ち切るための合図。待っている間だけ非`null`で、
     * 待っていない間に来たタップを捨てるために使う。完了済みの合図への再度の完了は無視されるため、
     * 1回のタップで進むのは1文だけになる。
     */
    private var narrationSkipSignal: CompletableDeferred<Unit>? = null

    /** 演出中(敵の手番・ブレイク中の手番飛ばしまたは味方の攻撃・スキルのジョブが動いている間)かどうか。 */
    private val isBusy: Boolean
        get() = autoTurnJob?.isActive == true || playerActionJob?.isActive == true

    init {
        viewModelScope.launch {
            startBattleUseCase(formation)
            // 進行度の購読は戦闘開始(進行度のリセット)の完了後に始める。前回の進行度が一瞬表示されるのを防ぐため。
            launch {
                observeMapProgressUseCase().collect { progress ->
                    _uiState.update { it.copy(progress = progress) }
                }
            }
            val normalAttack = getNormalAttackUseCase()
            observeBattleFieldUseCase().collect { battleField ->
                _uiState.update { battleField.reflectedIn(it, normalAttack) }
                startAutoTurnIfNeeded(battleField)
            }
        }
    }

    /**
     * ナレーション欄がタップされたときに呼ばれる。
     *
     * 演出中で、ナレーション1文の表示時間を待っている間だけ、その待ちを打ち切って次の文へ進める
     * (最後の文なら手番の終了へ進む)。待っていない間のタップは溜め込まずに捨てる。
     */
    fun onNarrationClick() {
        narrationSkipSignal?.complete(Unit)
    }

    /**
     * カードが長押しされたときに呼ばれ、その個体の詳細ポップアップを開く。
     *
     * 味方・敵のどちらも、戦闘不能の個体も開ける。勝敗が決まった後と、存在しないFPIDは無視する。
     * 選択状態は変えない。
     *
     * @param fpid 長押しされた個体のFPID([BattleUnitUiModel.id])。
     */
    fun onUnitLongPress(fpid: Int) {
        _uiState.update {
            if (it.outcome != null || fpid !in it.unitDetails) it else it.copy(detailUnitId = fpid)
        }
    }

    /** 詳細ポップアップの外側のタップや戻る操作で呼ばれ、詳細ポップアップを閉じる。 */
    fun onUnitDetailDismiss() {
        _uiState.update { it.copy(detailUnitId = null) }
    }

    /**
     * 「攻撃」ボタンが押されたときに呼ばれる。
     *
     * 攻撃できる状態(プレイヤーの手番・演出中でない・勝敗未決・スキルの選択中でない・攻撃が届く相手がいる)であれば
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
     * スキル一覧の選択中で、選ばれたスキルが手番の味方のもので、現在のMPで使え、選べる相手がいるときだけ、
     * 対象を選ぶ状態へ移行する。それ以外の場合は何も行わない。
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
     * 対象選択の処理は味方のカードのタップと共通で、[onTargetClick]に委ねる。
     *
     * @param fpid タップされた敵のFPID([BattleUnitUiModel.id])。
     */
    fun onEnemyClick(fpid: Int) = onTargetClick(fpid)

    /**
     * 味方のカードがタップされたときに呼ばれる。
     *
     * 対象選択の処理は敵のカードのタップと共通で、[onTargetClick]に委ねる。
     *
     * @param fpid タップされた味方のFPID([BattleUnitUiModel.id])。
     */
    fun onAllyClick(fpid: Int) = onTargetClick(fpid)

    /**
     * 敵・味方どちらのカードがタップされたときにも共通の、対象選択の処理。
     *
     * プレイヤーの手番・演出中でない・タップされた個体が選べる相手([BattleUiState.selectableTargetIds])である、
     * をすべて満たすときだけ、選択状態に応じて行動する。通常攻撃の対象選択中なら手番の味方がタップされた個体へ
     * 通常攻撃を、スキルの対象選択中なら選んでおいたスキルをタップされた個体へ使う。結果(範囲の場合は複数の対象
     * をまとめて)をナレーションで表示して間を置いたあと、手番を終える。条件を満たさなければ何も行わない。
     *
     * @param fpid タップされた個体のFPID([BattleUnitUiModel.id])。
     */
    private fun onTargetClick(fpid: Int) {
        val state = _uiState.value
        val actorFpid = state.currentActorId ?: return
        if (!state.isPlayerTurn || isBusy || fpid !in state.selectableTargetIds) return
        when (val selection = state.selection) {
            BattleSelection.AttackTarget -> launchPlayerAction(actorFpid) {
                attackUseCase(attackerFpid = actorFpid, targetFpid = fpid)?.let(::attackNarrations)
            }
            is BattleSelection.SkillTarget -> launchPlayerAction(actorFpid) {
                useSkillUseCase(actorFpid = actorFpid, skillId = selection.skillId, targetFpid = fpid)
                    ?.let { listOf(skillNarration(it)) }
            }
            else -> Unit
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
     * @param execute 行動を実行し、結果のナレーションの列を返す処理。実行できなければ`null`を返す。
     */
    private fun launchPlayerAction(actorFpid: Int, execute: suspend () -> List<BattleNarration>?) {
        _uiState.update { it.copy(isProcessing = true, selection = BattleSelection.None) }
        playerActionJob = viewModelScope.launch {
            val narrations = execute()
            if (narrations != null) {
                narrations.forEach { showNarration(it, RESULT_DISPLAY_MS) }
                endTurnUseCase(actorFpid)
            }
            _uiState.update { it.copy(isProcessing = false, processingNarration = null) }
            playerActionJob = null
            startAutoTurnIfNeeded(observeBattleFieldUseCase().first())
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
     * 現在の手番が自動で進む個体(ブレイク中の個体、または敵)で、演出中でなく、勝敗も決まっていなければ、
     * 手番の演出ジョブを開始する。
     *
     * 判断には、古くなり得るUiStateではなく、Repositoryから流れてきた最新の[field]を用いる。
     * ブレイク中の個体(味方・敵とも)は行動できないため、「動けない」→「立ち上がった」のナレーションを
     * 1文ずつ表示してから[endTurnUseCase]で手番を終える(ブレイクの状態遷移は手番の終了で`domain`が行う)。
     * 敵の流れは「演出中にする → 敵の行動のナレーション → 間 → 攻撃 → 攻撃結果のナレーションの列(1文ずつ、間を置く) →
     * 手番終了 → 演出終了」。届く相手がいなくて攻撃できなかったときは、攻撃結果の代わりに
     * 「届かない」ナレーション([BattleNarration.EnemyCannotReach])を同じ間だけ表示してから手番を終える。
     * 各ナレーションの間は、ナレーション欄のタップ([onNarrationClick])で打ち切れる。
     * 2つのジョブが動いている間は多重起動しない。
     *
     * @param field 判断に用いる、最新の戦闘フィールド。
     */
    private fun startAutoTurnIfNeeded(field: BattleField) {
        if (isBusy || field.outcome != null) return
        val actorFpid = field.currentActorFpid ?: return
        val actor = field.allPawns.firstOrNull { it.fpid == actorFpid } ?: return
        if (actor.isBroken) {
            startBrokenTurnSkip(actorFpid)
            return
        }
        if (field.enemies.none { it.fpid == actorFpid }) return
        _uiState.update {
            it.copy(isProcessing = true, processingNarration = BattleNarration.EnemyTurn(it.nameOf(actorFpid)))
        }
        autoTurnJob = viewModelScope.launch {
            waitForNarration(ENEMY_TURN_DELAY_MS)
            val result = executeEnemyTurnUseCase(actorFpid)
            val narrations = result?.let(::attackNarrations)
                ?: listOf(BattleNarration.EnemyCannotReach(_uiState.value.nameOf(actorFpid)))
            narrations.forEach { showNarration(it, RESULT_DISPLAY_MS) }
            endTurnUseCase(actorFpid)
            _uiState.update { it.copy(isProcessing = false, processingNarration = null) }
            autoTurnJob = null
            startAutoTurnIfNeeded(observeBattleFieldUseCase().first())
        }
    }

    /**
     * ブレイク中の個体の手番を飛ばす演出ジョブを開始する。
     *
     * プレイヤーの操作を受け付けないよう演出中にし、「動けない」「立ち上がった」のナレーションを順に表示して
     * から手番を終える。ブレイクの状態を進める処理は手番の終了([endTurnUseCase])の中で行われる。
     *
     * @param actorFpid 手番を飛ばす個体のFPID。
     */
    private fun startBrokenTurnSkip(actorFpid: Int) {
        val name = _uiState.value.nameOf(actorFpid)
        _uiState.update { it.copy(isProcessing = true, selection = BattleSelection.None) }
        autoTurnJob = viewModelScope.launch {
            showNarration(BattleNarration.BreakStunned(name), RESULT_DISPLAY_MS)
            showNarration(BattleNarration.BreakRecovered(name), RESULT_DISPLAY_MS)
            endTurnUseCase(actorFpid)
            _uiState.update { it.copy(isProcessing = false, processingNarration = null) }
            autoTurnJob = null
            startAutoTurnIfNeeded(observeBattleFieldUseCase().first())
        }
    }

    /**
     * 演出中のナレーションを1文表示し、表示時間が過ぎるかナレーション欄がタップされるまで待つ。
     *
     * @param narration 表示するナレーション。
     * @param durationMs 表示する時間(ミリ秒)。
     */
    private suspend fun showNarration(narration: BattleNarration, durationMs: Long) {
        _uiState.update { it.copy(processingNarration = narration) }
        waitForNarration(durationMs)
    }

    /**
     * [durationMs]が過ぎるか[onNarrationClick]で打ち切られるまで待つ。待っている間だけタップを受け付ける。
     *
     * @param durationMs 待つ時間(ミリ秒)。
     */
    private suspend fun waitForNarration(durationMs: Long) {
        val signal = CompletableDeferred<Unit>()
        narrationSkipSignal = signal
        try {
            withTimeoutOrNull(durationMs) { signal.await() }
        } finally {
            narrationSkipSignal = null
        }
    }

    /**
     * 攻撃結果から、表示するナレーションの列を作る。
     *
     * 文の組み立て自体は[toNarrations]に委ね、ここでは表示名の解決だけを行う。
     *
     * @param result 攻撃の結果。
     * @return 順に1文ずつ表示するナレーション。
     */
    private fun attackNarrations(result: AttackResult): List<BattleNarration> {
        val state = _uiState.value
        return result.toNarrations(state::nameOf)
    }

    /**
     * スキルの結果から、表示するナレーションを作る。
     *
     * @param result スキルの結果。
     * @return 対象が1体なら誰が誰にどのスキルを使いHPがどれだけ回復したか、複数なら何体のHPを合計どれだけ回復したかを伝えるナレーション。
     */
    private fun skillNarration(result: SkillResult): BattleNarration {
        val state = _uiState.value
        val single = result.targets.singleOrNull()
        return if (single != null) {
            BattleNarration.SkillResult(
                actorName = state.nameOf(result.actorFpid),
                targetName = state.nameOf(single.targetFpid),
                skillName = result.skillName,
                healedAmount = single.healedAmount,
            )
        } else {
            BattleNarration.MultiSkillResult(
                actorName = state.nameOf(result.actorFpid),
                skillName = result.skillName,
                targetCount = result.targets.size,
                totalHealed = result.totalHealed,
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
 * 選択状態は維持する。手番の個体が変わって味方の手番でなくなった場合は選択状態を解除する。
 * 手番の味方が使えるスキルと、その時点のMPで使えるかどうか、通常攻撃・各スキルで選べる相手
 * ([selectableTargets])も反映する。全個体の詳細([BattleUnitDetailUiModel])も組み立て直し、
 * 勝敗が決まったか、開いていた詳細の個体がいなくなったときは詳細ポップアップを閉じる。
 *
 * @receiver 最新の戦闘フィールド。
 * @param current 反映先の現在のUiState。
 * @param normalAttack 通常攻撃のスキルデータ。通常攻撃で選べる相手の算出に使う。
 * @return 反映後のUiState。
 */
private fun BattleField.reflectedIn(current: BattleUiState, normalAttack: ActiveSkill): BattleUiState {
    val names = resolveDisplayNames(
        allies.map { it.fpid to it.character.name } + enemies.map { it.fpid to it.enemy.name },
    )
    val alliesBySlot = allies.associateBy(BattleAlly::slot)
    val enemiesBySlot = enemies.associateBy(BattleEnemy::slot)
    val actor = currentActorFpid
    val isPlayerTurn = allies.any { it.fpid == actor }
    val actorAlly = allies.firstOrNull { it.fpid == actor }
    val details = allPawns.associate { it.fpid to detailOf(it, names.getValue(it.fpid)) }
    return current.copy(
        enemySlots = List(Formation.SLOT_COUNT) { slot ->
            enemiesBySlot[slot]?.let { it.toUiModel(names.getValue(it.fpid), it.fpid == actor) }
        },
        partySlots = List(Formation.SLOT_COUNT) { slot ->
            alliesBySlot[slot]?.let { it.toUiModel(names.getValue(it.fpid), it.fpid == actor) }
        },
        currentActorId = actor,
        isPlayerTurn = isPlayerTurn,
        selection = if (isPlayerTurn) current.selection else BattleSelection.None,
        skills = actorAlly?.let { ally ->
            ally.activeSkills.map { skill ->
                val targetIds = selectableTargetIds(ally.fpid, skill)
                BattleSkillUiModel(
                    id = skill.id,
                    name = skill.name,
                    mpCost = skill.costOf(SkillCostType.MP),
                    hpCost = skill.costOf(SkillCostType.HP),
                    targetIds = targetIds,
                    isUsable = ally.canAfford(skill) && targetIds.isNotEmpty(),
                )
            }
        }.orEmpty(),
        attackTargetIds = actorAlly?.let { selectableTargetIds(it.fpid, normalAttack) }.orEmpty(),
        outcome = outcome,
        unitDetails = details,
        detailUnitId = current.detailUnitId?.takeIf { outcome == null && it in details },
    )
}

/**
 * 個体1体分の詳細ポップアップ用UIモデルを組み立てる。
 *
 * 値は[BattleFieldPawn]に該当プロパティがあればそれを、無ければ味方は[BattleAlly.character]、
 * 敵は[BattleEnemy.enemy]から取る。位置は配置上の段と[effectiveRowOf]の有効な段から決める。
 *
 * @receiver 判定の基になる戦闘フィールド。
 * @param pawn 対象の個体。
 * @param displayName 同名個体の区別を付けた表示名。
 * @return 詳細ポップアップ用のUIモデル。
 */
private fun BattleField.detailOf(pawn: BattleFieldPawn, displayName: String): BattleUnitDetailUiModel {
    val position = when {
        pawn.slot / Formation.COLUMNS == 0 -> BattleUnitPosition.FRONT
        effectiveRowOf(pawn) == BattleRow.FRONT -> BattleUnitPosition.BACK_AS_FRONT
        else -> BattleUnitPosition.BACK
    }
    val ally = pawn as? BattleAlly
    val enemy = pawn as? BattleEnemy
    return BattleUnitDetailUiModel(
        id = pawn.fpid,
        name = displayName,
        hp = pawn.currentHp,
        maxHp = pawn.maxHp,
        mp = ally?.currentMp ?: enemy?.enemy?.mana ?: 0,
        maxMp = ally?.maxMp ?: enemy?.enemy?.mana ?: 0,
        breakGauge = floor(pawn.breakGauge).toInt(),
        breakState = pawn.breakState,
        position = position,
        attack = pawn.attack,
        defense = pawn.defense,
        manaOutput = ally?.character?.manaOutput ?: enemy?.enemy?.manaOutput ?: 0,
        manaResistance = ally?.character?.manaResistance ?: enemy?.enemy?.manaResistance ?: 0,
        agility = pawn.agility,
        luck = pawn.luck,
        accuracy = pawn.accuracy,
        evasion = pawn.evasion,
        breakResistance = pawn.breakResistance,
        breakPointRate = pawn.breakPointRate,
        turnCount = pawn.turnCount,
        remainingDistance = floor(pawn.remainingDistance).toInt(),
        activeSkillNames = ally?.activeSkills?.map { it.name }.orEmpty(),
        passiveSkillNames = pawn.passiveSkills.map { it.name },
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
    breakGaugeFraction = floor(breakGauge).toFloat() / BreakFormula.MAX_GAUGE.toFloat(),
    breakState = breakState,
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
    breakGaugeFraction = floor(breakGauge).toFloat() / BreakFormula.MAX_GAUGE.toFloat(),
    breakState = breakState,
)

/**
 * 使用者がそのスキルで選べる対象のFPIDの集合を求める。
 *
 * 選べる相手の判定そのものは`domain`の[selectableTargets]が唯一の実装箇所で、ここでは画面状態向けにFPIDへ変換するだけである。
 *
 * @receiver 判定の基になる戦闘フィールド。
 * @param actorFpid スキルを使う個体のFPID。
 * @param skill 使うスキル。
 * @return 選べる対象のFPIDの集合。
 */
private fun BattleField.selectableTargetIds(actorFpid: Int, skill: ActiveSkill): Set<Int> =
    selectableTargets(actorFpid, skill).map { it.fpid }.toSet()
