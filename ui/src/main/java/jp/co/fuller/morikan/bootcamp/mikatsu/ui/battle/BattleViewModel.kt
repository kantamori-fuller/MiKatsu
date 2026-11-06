package jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AutoTurnResult
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleAlly
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleEnemy
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleFieldPawn
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AttackResult
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleOutcome
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AilmentResistanceType
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.ElementResistances
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Formation
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleStat
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.SkillUseResult
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.StatusAilment
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillRange
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkill
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillCostType
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.BreakFormula
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.CriticalFormula
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.HitChanceFormula
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.BattleRow
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.canReservePreemptive
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.isSkillUsable
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.substituteHpCostOf
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.effectiveRowOf
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.defend
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.hasAnyManualAction
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.selectableTargets
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.AdvanceToNextBattleUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.AttackUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.DefendUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.EndTurnUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.ExecuteAutoTurnUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.GetNormalAttackUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.ObserveBattleFieldUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.ObserveMapProgressUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.ReservePreemptiveActionUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.StartBattleUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.UseSkillUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.EndTurnResult
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.AilmentDetail
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleNarration
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.toNarrationStep
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleSelection
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleSkillUiModel
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleUiState
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleUnitDetailUiModel
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.AilmentResistanceDetail
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.ElementResistanceDetail
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleUnitPosition
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleUnitUiModel
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.resolveDisplayNames
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.StatChangeDetail
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.StatChangeEntry
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.NarrationStep
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.toNarrationSteps
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.toNarrations
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.floor

/** CPUが動かす手番(敵・洗脳・発狂)を開始してから行動を実行するまでの間(ミリ秒)。 */
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
 * 「スキル」→スキルの選択→対象のタップで[useSkillUseCase]によるスキルの使用を、「防御」で[defendUseCase]による防御を行う。
 * タップできる対象は、通常攻撃のデータ([getNormalAttackUseCase])と手番の味方のスキルのレンジ・範囲・陣営の条件から
 * 求めた「選べる相手」だけで、敵・味方どちらのカードからでも選べる。
 * 手番が敵のとき、または洗脳・発狂した個体のときは、間とナレーションを挟みながら[executeAutoTurnUseCase]で自動的に行動させる
 * (洗脳は「〜は洗脳されている!」→通常攻撃、発狂は「〜は発狂している!」→スキル・攻撃または何もしない)。
 * 恐怖のスタンで手番を飛ばすときは専用の文(「〜は恐怖で体がすくんだ!」)、手動で操作する味方が何も選べない手番(魅了・挑発など)は
 * 「〜は何もできない!」を出して自動で飛ばす(判定は`domain`の`hasAnyManualAction`)。
 * 行動のたびに[endTurnUseCase]で手番を終える。
 * 味方のカードのタップ([onAllyClick])は、対象選択中でなければ[reservePreemptiveActionUseCase]で先制行動の予約になる。予約は行動中(敵・味方・演出中)でも
 * でき、現在の行動者の手番の終了([endTurnUseCase])で行動順に割り込む。先制の手番が始まったら「〇〇の先制行動!」を表示してから先制スキルだけの一覧を開き、
 * 取り消された先制の手番は、次の手番へ進む前に「〇〇は先制行動できなかった!」を表示する。
 * 演出中のナレーションは複数の文の列として1文ずつ順に表示し、ナレーション欄のタップ([onNarrationClick])で
 * 表示時間の待ちを打ち切って次の文へ進められる。
 * ダメージ技の演出中は、戦闘フィールドの購読による表示の更新を止め([isDisplayLocked])、ナレーションの1文ごとに、その時点の
 * 戦闘フィールドのスナップショット([NarrationStep.field])を表示する(「1回目の文なら1回目の時点のHP」)。ナレーションを流し終えたら
 * ロックを解除し、最新の戦闘フィールドを反映する。
 * 勝利後は「次に進む」で[advanceToNextBattleUseCase]により同じ編成のまま次の戦闘へ進み、
 * 進行度は[observeMapProgressUseCase]で購読して表示する(正はRepositoryが保持する)。
 *
 * @property formation 編成画面で編成された、味方として参戦するキャラクターの陣形。
 * @property startBattleUseCase 陣形から戦闘を開始するUseCase。
 * @property observeBattleFieldUseCase 戦闘フィールドの最新状態を購読するUseCase。
 * @property attackUseCase 味方の通常攻撃を実行するUseCase。
 * @property getNormalAttackUseCase 通常攻撃のスキルデータを取得するUseCase。通常攻撃で選べる相手の算出に使う。
 * @property useSkillUseCase 味方のアクティブスキルを使うUseCase。
 * @property defendUseCase 味方の防御を実行するUseCase。
 * @property executeAutoTurnUseCase CPUが動かす手番(敵・洗脳・発狂)の行動を実行するUseCase。
 * @property endTurnUseCase 現在の行動者の手番を終えるUseCase。
 * @property observeMapProgressUseCase マップの進行度を購読するUseCase。
 * @property advanceToNextBattleUseCase 勝利後に次の戦闘へ進めるUseCase。
 * @property reservePreemptiveActionUseCase 味方の先制行動を予約するUseCase。
 */
@HiltViewModel(assistedFactory = BattleViewModel.Factory::class)
class BattleViewModel @AssistedInject constructor(
    @Assisted private val formation: Formation,
    private val startBattleUseCase: StartBattleUseCase,
    private val observeBattleFieldUseCase: ObserveBattleFieldUseCase,
    private val attackUseCase: AttackUseCase,
    private val getNormalAttackUseCase: GetNormalAttackUseCase,
    private val useSkillUseCase: UseSkillUseCase,
    private val defendUseCase: DefendUseCase,
    private val executeAutoTurnUseCase: ExecuteAutoTurnUseCase,
    private val endTurnUseCase: EndTurnUseCase,
    private val observeMapProgressUseCase: ObserveMapProgressUseCase,
    private val advanceToNextBattleUseCase: AdvanceToNextBattleUseCase,
    private val reservePreemptiveActionUseCase: ReservePreemptiveActionUseCase,
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
     * 「先制行動!」を表示した、いまの先制の手番の個体のFPID。戦闘フィールドの更新は何度も流れてくるため、
     * 同じ先制の手番に対して二重に表示・開始しないための目印として保持する。先制の手番でなくなったら`null`に戻す。
     */
    private var announcedPreemptiveFpid: Int? = null

    /**
     * 演出中のナレーション1文の表示時間の待ちを打ち切るための合図。待っている間だけ非`null`で、
     * 待っていない間に来たタップを捨てるために使う。完了済みの合図への再度の完了は無視されるため、
     * 1回のタップで進むのは1文だけになる。
     */
    private var narrationSkipSignal: CompletableDeferred<Unit>? = null

    /**
     * 通常攻撃のスキルデータ。戦闘フィールドを表示へ反映する([reflectedIn])のに使う。取得するまでは`null`で、その間はスナップショットを表示できない。
     * 戦闘の状態そのものは持たない(SSOTはRepository)。
     */
    private var normalAttack: ActiveSkill? = null

    /**
     * ダメージ技の演出中に、購読した戦闘フィールドの表示への反映と自動の手番開始を止めるかどうか。
     * ロック中は[showStep]が出すスナップショットだけが表示になり、解除すると最新の戦闘フィールドが再び流れて反映される。
     */
    private val isDisplayLocked = MutableStateFlow(false)

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
            val normalAttack = getNormalAttackUseCase().also { this@BattleViewModel.normalAttack = it }
            combine(observeBattleFieldUseCase(), isDisplayLocked) { battleField, locked -> battleField to locked }
                .collect { (battleField, locked) ->
                    // ダメージ技の演出中は、ナレーションに連動したスナップショットだけを表示する。
                    if (locked) return@collect
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
     * スキル一覧の選択中で、選ばれたスキルが手番の味方のもので、現在のコスト(MP・HP・TP)を払え、選べる相手がいるときだけ、
     * 対象を選ぶ状態へ移行する。ただしレンジ0のスキル([BattleSkillUiModel.isSelfTargeting])は、対象選択を挟まず
     * 使用者自身を対象(FPID)として、その場で発動する。それ以外の場合は何も行わない。
     *
     * @param skillId 選ばれたスキルのID([BattleSkillUiModel.id])。
     */
    fun onSkillSelected(skillId: Int) {
        val state = _uiState.value
        val skill = state.skills.firstOrNull { it.id == skillId }
        val actorFpid = state.currentActorId
        if (state.selection != BattleSelection.SkillList || !state.isSkillEnabled || isBusy ||
            skill?.isUsable != true || actorFpid == null
        ) {
            return
        }
        if (skill.isSelfTargeting) {
            launchPlayerAction(actorFpid) {
                useSkillUseCase(actorFpid = actorFpid, skillId = skillId, targetFpid = actorFpid)
                    ?.let(::skillUseSteps)
            }
        } else {
            _uiState.update { it.copy(selection = BattleSelection.SkillTarget(skillId)) }
        }
    }

    /**
     * 「防御」ボタンが押されたときに呼ばれる。
     *
     * 防御できる状態(プレイヤーの手番・演出中でない・勝敗未決・選択状態がない)であれば、手番の味方を
     * [defendUseCase]で防御状態にし、「〇〇は防御している!」のナレーションを表示してから手番を終える。
     * それ以外の場合は何も行わない。
     */
    fun onDefendClick() {
        val state = _uiState.value
        val actorFpid = state.currentActorId ?: return
        if (!state.isDefendEnabled || isBusy) return
        val name = state.nameOf(actorFpid)
        launchPlayerAction(actorFpid) {
            if (defendUseCase(actorFpid)) listOf(NarrationStep(BattleNarration.Defend(name))) else null
        }
    }

    /**
     * 「キャンセル」ボタンが押されたときに呼ばれる。
     *
     * 攻撃の対象選択・スキル一覧・スキルの対象選択のいずれの状態からも、選択状態を解除して
     * 行動選択に戻す。選択状態でなければ何も行わない。
     * 先制の手番では、スキルの対象選択からスキル一覧に戻るだけで、スキル一覧からはキャンセルできない(先制はやめられない)。
     */
    fun onCancelClick() {
        _uiState.update {
            when {
                !it.isPreemptiveTurn -> it.copy(selection = BattleSelection.None)
                it.selection is BattleSelection.SkillTarget -> it.copy(selection = BattleSelection.SkillList)
                else -> it
            }
        }
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
     * 通常攻撃・スキルの対象選択中は、敵のカードのタップと共通の対象選びとして[onTargetClick]に委ねる(その間は予約できない)。
     * それ以外のときは、タップされた味方が先制行動を予約できる個体([BattleUiState.reservableAllyIds])なら、
     * [reservePreemptiveActionUseCase]で予約する(確認はなく、取り消しもできない)。敵・味方どちらの行動中でも、演出中でも予約できる。
     * 予約を受け付けるかの最終判断はドメインが行い、通らなければ何も起きない。
     *
     * @param fpid タップされた味方のFPID([BattleUnitUiModel.id])。
     */
    fun onAllyClick(fpid: Int) {
        val state = _uiState.value
        if (state.selection == BattleSelection.AttackTarget || state.selection is BattleSelection.SkillTarget) {
            onTargetClick(fpid)
            return
        }
        if (state.outcome != null || fpid !in state.reservableAllyIds) return
        viewModelScope.launch { reservePreemptiveActionUseCase(fpid) }
    }

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
                attackUseCase(attackerFpid = actorFpid, targetFpid = fpid)?.let(::attackSteps)
            }
            is BattleSelection.SkillTarget -> launchPlayerAction(actorFpid) {
                useSkillUseCase(actorFpid = actorFpid, skillId = selection.skillId, targetFpid = fpid)
                    ?.let(::skillUseSteps)
            }
            else -> Unit
        }
    }

    /**
     * 味方の行動(通常攻撃・スキル・防御)の演出ジョブを開始する。
     *
     * 流れは「演出中にして選択状態を解除 → 行動の実行 → 結果のナレーション → 間 → 手番終了 → 演出終了」。
     * 演出中の状態は同期的に設定するため、連打しても二重に開始されない。行動が実行できなかった場合
     * ([execute]が`null`)は、手番を終えずに行動選択へ戻す。
     * 実行の前に表示をロックし([isDisplayLocked])、ナレーションを流し終えたら手番の終了の前に解除する。
     * 実行が失敗したときやジョブがキャンセルされたときも、必ず解除する。表示するスナップショットを持たない結果
     * (ダメージのないスキル・防御)は、実行できた時点ですぐにロックを解除して最新の状態を反映してから、ナレーションを流す。
     *
     * @param actorFpid 行動する味方のFPID。
     * @param execute 行動を実行し、結果のナレーションの列を返す処理。実行できなければ`null`を返す。
     */
    private fun launchPlayerAction(actorFpid: Int, execute: suspend () -> List<NarrationStep>?) {
        _uiState.update { it.copy(isProcessing = true, selection = BattleSelection.None) }
        isDisplayLocked.value = true
        playerActionJob = viewModelScope.launch {
            val narrations = try {
                execute()?.also { steps -> if (steps.none { it.field != null }) isDisplayLocked.value = false }
                    ?.also { steps -> steps.forEach { showStep(it, RESULT_DISPLAY_MS) } }
            } finally {
                isDisplayLocked.value = false
            }
            if (narrations != null) {
                endTurnAndAnnounceCancellations(actorFpid)
            }
            _uiState.update {
                // 先制の手番は行動しないと終われないため、実行できなかったときは行動選択ではなくスキル一覧へ戻す。
                it.copy(
                    isProcessing = false,
                    processingNarration = null,
                    selection = if (narrations == null && it.isPreemptiveTurn) BattleSelection.SkillList else it.selection,
                )
            }
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
     * 現在の手番が自動で進む個体(ブレイク中・行動不能の個体、先制の手番の導入、発狂・洗脳した個体、敵、何も選べない味方)で、
     * 演出中でなく、勝敗も決まっていなければ、手番の演出ジョブを開始する。
     *
     * 判断には、古くなり得るUiStateではなく、Repositoryから流れてきた最新の[field]を用いる。判定の順は次のとおり。
     * 1. ブレイク中(味方・敵とも): 「動けない」→「立ち上がった」を1文ずつ表示して[endTurnUseCase]で手番を終える
     *    (ブレイクの状態遷移は手番の終了で`domain`が行う。[startBrokenTurnSkip])。
     * 2. 行動不能の状態異常: 「〇〇は〈名〉で動けない!」(恐怖によるスタンは専用の「恐怖で体がすくんだ!」)を表示して手番を終える([startIncapacitatedTurnSkip])。
     * 3. 先制の手番: 「先制行動!」の導入を出してスキル一覧を開く([startPreemptiveIntro])。
     * 4. 発狂・洗脳(味方・敵とも)、および敵: 自動で行動する([startCpuTurn])。
     *    流れは「演出中にする → 前置きのナレーション(発狂/洗脳/敵の行動) → 間 → 行動 → 結果のナレーション(1文ずつ、間を置く) → 手番終了 → 演出終了」。
     *    届く相手がいなくて攻撃できなかったときは「届かない」([BattleNarration.EnemyCannotReach])、発狂で何もしないときは
     *    「しかし〇〇は何もしなかった!」、何もできないときは「〇〇は何もできない!」を同じ間だけ表示してから手番を終える。
     * 5. 手動で操作する味方で、通常攻撃・スキル・防御のどれも選べないとき(`domain`の`hasAnyManualAction`): 「〇〇は何もできない!」を出して自動で飛ばす
     *    ([startNothingToDoSkip])。
     * 各ナレーションの間は、ナレーション欄のタップ([onNarrationClick])で打ち切れる。2つのジョブが動いている間は多重起動しない。
     *
     * @param field 判断に用いる、最新の戦闘フィールド。
     */
    private fun startAutoTurnIfNeeded(field: BattleField) {
        // 先制の手番でなくなった(または別の個体の手番になった)ら、次に同じ個体が先制の手番になったときにまた表示できるよう目印を戻す。
        if (!(field.isPreemptiveTurn && field.currentActorFpid == announcedPreemptiveFpid)) announcedPreemptiveFpid = null
        if (isBusy || field.outcome != null) return
        val actorFpid = field.currentActorFpid ?: return
        val actor = field.allPawns.firstOrNull { it.fpid == actorFpid } ?: return
        if (actor.isBroken) {
            startBrokenTurnSkip(actorFpid)
            return
        }
        actor.incapacitatingAilment?.let { ailment ->
            val causedByFear = ailment == StatusAilment.STUN &&
                actor.ailments.any { it.ailment == StatusAilment.STUN && it.causedByFear }
            startIncapacitatedTurnSkip(actorFpid, ailment, causedByFear)
            return
        }
        if (field.isPreemptiveTurn) {
            if (announcedPreemptiveFpid != actorFpid) startPreemptiveIntro(actorFpid)
            return
        }
        if (actor.isAutoActing || actor is BattleEnemy) {
            startCpuTurn(actorFpid, actor)
            return
        }
        val attack = normalAttack ?: return
        if (!field.hasAnyManualAction(actorFpid, attack)) startNothingToDoSkip(actorFpid)
    }

    /**
     * CPUが動かす手番(発狂・洗脳した個体、敵)の演出ジョブを開始する。
     *
     * 最初に前置きのナレーション(発狂は「〇〇は発狂している!」、洗脳は「〇〇は洗脳されている!」、それ以外の敵は「〇〇の行動」)を表示して間を置き、
     * [executeAutoTurnUseCase]で行動させ、結果の種類([AutoTurnResult])に応じた文を1文ずつ表示してから、手番を終える。
     * 発狂が洗脳より優先される(`domain`の優先順位と同じ)。
     *
     * @param actorFpid 行動させる個体のFPID。
     * @param actor 行動者(前置きの判定に使う)。
     */
    private fun startCpuTurn(actorFpid: Int, actor: BattleFieldPawn) {
        _uiState.update {
            val name = it.nameOf(actorFpid)
            val lead = when {
                actor.hasAilment(StatusAilment.MADNESS) -> BattleNarration.MadnessLead(name)
                actor.hasAilment(StatusAilment.BRAINWASH) -> BattleNarration.BrainwashLead(name)
                else -> BattleNarration.EnemyTurn(name)
            }
            it.copy(isProcessing = true, selection = BattleSelection.None, processingNarration = lead)
        }
        autoTurnJob = viewModelScope.launch {
            waitForNarration(ENEMY_TURN_DELAY_MS)
            isDisplayLocked.value = true
            try {
                val steps = autoTurnSteps(actorFpid, executeAutoTurnUseCase(actorFpid))
                if (steps.none { it.field != null }) isDisplayLocked.value = false
                steps.forEach { showStep(it, RESULT_DISPLAY_MS) }
            } finally {
                isDisplayLocked.value = false
            }
            endTurnAndAnnounceCancellations(actorFpid)
            _uiState.update { it.copy(isProcessing = false, processingNarration = null) }
            autoTurnJob = null
            startAutoTurnIfNeeded(observeBattleFieldUseCase().first())
        }
    }

    /**
     * CPUが動かした手番の結果から、表示するナレーションとスナップショットの組の列を作る。
     *
     * @param actorFpid 行動した個体のFPID。
     * @param result 行動の結果。実行できなかった(`null`)ときは「届かない」と同じ扱いにする。
     * @return 順に1文ずつ表示するナレーションと、その文を出すときに表示するフィールドの列。
     */
    private fun autoTurnSteps(actorFpid: Int, result: AutoTurnResult?): List<NarrationStep> {
        val name = _uiState.value.nameOf(actorFpid)
        return when (result) {
            is AutoTurnResult.Attacked -> attackSteps(result.result)
            is AutoTurnResult.UsedSkill -> result.result.toNarrations(_uiState.value::nameOf).map { NarrationStep(it) }
            AutoTurnResult.MadnessIdle -> listOf(NarrationStep(BattleNarration.MadnessIdle(name)))
            AutoTurnResult.NothingToDo -> listOf(NarrationStep(BattleNarration.NothingToDo(name)))
            AutoTurnResult.CannotReach, null -> listOf(NarrationStep(BattleNarration.EnemyCannotReach(name)))
        }
    }

    /**
     * 先制の手番が始まったときの演出ジョブを開始する。
     *
     * 演出中にして「〇〇の先制行動!」を表示し、そのあと選択状態を自動でスキル一覧([BattleSelection.SkillList])にする
     * (一覧には先制スキルだけが並ぶ)。同じ先制の手番に対して二重に始まらないよう、開始時に[announcedPreemptiveFpid]へ記録する。
     *
     * @param actorFpid 先制の手番に入った味方のFPID。
     */
    private fun startPreemptiveIntro(actorFpid: Int) {
        announcedPreemptiveFpid = actorFpid
        val name = _uiState.value.nameOf(actorFpid)
        _uiState.update { it.copy(isProcessing = true, selection = BattleSelection.None) }
        autoTurnJob = viewModelScope.launch {
            showNarration(BattleNarration.PreemptiveAction(name), RESULT_DISPLAY_MS)
            _uiState.update {
                it.copy(isProcessing = false, processingNarration = null, selection = BattleSelection.SkillList)
            }
            autoTurnJob = null
        }
    }

    /**
     * 現在の行動者の手番を[endTurnUseCase]で終え、手番終了時の状態異常の出来事と、取り消された先制の手番を1文ずつ表示する。
     *
     * 状態異常の出来事(ダメージ・伝染・解除・戦闘不能)は、各出来事の直後のフィールドと一緒に、攻撃と同じくナレーションと表示を連動させて流す
     * ([toNarrationStep])。その間は戦闘フィールドの購読による表示の更新を止め([isDisplayLocked])、流し終えたら解除して最新を反映する。
     * 続けて、取り消された先制の手番があれば「〇〇は先制行動できなかった!」を、次の手番へ進む前に表示する。
     * 呼び出し元は演出ジョブの中で呼ぶこと。
     *
     * @param actorFpid 手番を終える個体のFPID。
     */
    private suspend fun endTurnAndAnnounceCancellations(actorFpid: Int) {
        isDisplayLocked.value = true
        val result: EndTurnResult
        try {
            result = endTurnUseCase(actorFpid)
            if (result.events.isEmpty()) isDisplayLocked.value = false
            result.events.forEach { showStep(it.toNarrationStep(_uiState.value::nameOf), RESULT_DISPLAY_MS) }
        } finally {
            isDisplayLocked.value = false
        }
        result.cancelledPreemptive.forEach { cancelledFpid ->
            showNarration(BattleNarration.PreemptiveFailed(_uiState.value.nameOf(cancelledFpid)), RESULT_DISPLAY_MS)
        }
    }

    /**
     * 手番を飛ばす演出ジョブを開始する。ブレイク・行動不能・何もできない手番の共通の流れ。
     *
     * プレイヤーの操作を受け付けないよう演出中にし、[narrations]を順に表示してから[endTurnUseCase]で手番を終える。
     * 飛ばされた手番も1手番として数える(ブレイク・状態異常の経過は手番の終了の中で`domain`が行う)。
     *
     * @param actorFpid 手番を飛ばす個体のFPID。
     * @param narrations 順に表示するナレーション。
     */
    private fun startTurnSkip(actorFpid: Int, narrations: List<BattleNarration>) {
        _uiState.update { it.copy(isProcessing = true, selection = BattleSelection.None) }
        autoTurnJob = viewModelScope.launch {
            narrations.forEach { showNarration(it, RESULT_DISPLAY_MS) }
            endTurnAndAnnounceCancellations(actorFpid)
            _uiState.update { it.copy(isProcessing = false, processingNarration = null) }
            autoTurnJob = null
            startAutoTurnIfNeeded(observeBattleFieldUseCase().first())
        }
    }

    /**
     * ブレイク中の個体の手番を飛ばす演出ジョブを開始する。「動けない」「立ち上がった」のナレーションを順に表示してから手番を終える。
     * ブレイクの状態を進める処理は手番の終了([endTurnUseCase])の中で行われる。
     *
     * @param actorFpid 手番を飛ばす個体のFPID。
     */
    private fun startBrokenTurnSkip(actorFpid: Int) {
        val name = _uiState.value.nameOf(actorFpid)
        startTurnSkip(actorFpid, listOf(BattleNarration.BreakStunned(name), BattleNarration.BreakRecovered(name)))
    }

    /**
     * 状態異常(スタン・凍結・感電・石化・睡眠)で行動不能の個体の手番を飛ばす演出ジョブを開始する。
     *
     * 「〇〇は〈状態異常名〉で動けない!」を表示してから手番を終える。恐怖によるスタン([causedByFear])は、専用の
     * 「〇〇は恐怖で体がすくんだ!」を代わりに表示する。状態異常の経過(ダメージ・残りターンの減算)は手番の終了の中で行われ、
     * 飛ばされた手番も1手番として数える。ブレイク中の個体は[startBrokenTurnSkip]が優先して扱う。
     *
     * @param actorFpid 手番を飛ばす個体のFPID。
     * @param ailment 動けない原因の状態異常(複数該当ならenum順の先頭)。
     * @param causedByFear 動けない原因が恐怖によるスタンか(`domain`の[jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AilmentState.causedByFear])。
     */
    private fun startIncapacitatedTurnSkip(actorFpid: Int, ailment: StatusAilment, causedByFear: Boolean) {
        val name = _uiState.value.nameOf(actorFpid)
        val narration = if (causedByFear) BattleNarration.FearFrozen(name) else BattleNarration.AilmentIncapacitated(name, ailment)
        startTurnSkip(actorFpid, listOf(narration))
    }

    /**
     * 手動で操作する味方が、通常攻撃・スキル・防御のどれも選べない手番を、自動で飛ばす演出ジョブを開始する。
     * 「〇〇は何もできない!」を表示してから手番を終える(手番として数える)。判定は`domain`の`hasAnyManualAction`。
     *
     * @param actorFpid 手番を飛ばす味方のFPID。
     */
    private fun startNothingToDoSkip(actorFpid: Int) {
        startTurnSkip(actorFpid, listOf(BattleNarration.NothingToDo(_uiState.value.nameOf(actorFpid))))
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
     * 演出中のナレーションを1文表示し、その文に対応するスナップショットがあれば同時に画面へ反映して、表示時間が過ぎるかナレーション欄がタップされるまで待つ。
     *
     * @param step 表示するナレーションと、その文を出すときに表示する戦闘フィールド。
     * @param durationMs 表示する時間(ミリ秒)。
     */
    private suspend fun showStep(step: NarrationStep, durationMs: Long) {
        val skill = normalAttack
        _uiState.update {
            val reflected = if (step.field != null && skill != null) step.field.reflectedIn(it, skill) else it
            reflected.copy(processingNarration = step.narration)
        }
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
     * 攻撃結果から、表示するナレーションとスナップショットの組の列を作る。
     *
     * 文の組み立て自体は[toNarrationSteps]に委ね、ここでは表示名の解決だけを行う。
     *
     * @param result 攻撃の結果。
     * @return 順に1文ずつ表示するナレーションと、その文を出すときに表示するフィールドの列。
     */
    private fun attackSteps(result: AttackResult): List<NarrationStep> = result.toNarrationSteps(_uiState.value::nameOf)

    /**
     * スキル使用の結果から、表示するナレーションとスナップショットの組の列を作る。
     *
     * ダメージを与えるスキル([SkillUseResult.Attack])は通常攻撃と同じ[attackSteps]に乗せ、
     * 与えないスキル([SkillUseResult.Effect])はスキルの結果として[toNarrations]で組み立て、
     * スナップショットなし(`field = null`。表示は即時反映済み)のステップに包む。
     *
     * @param result スキル使用の結果。
     * @return 順に1文ずつ表示するナレーションと、その文を出すときに表示するフィールドの列。
     */
    private fun skillUseSteps(result: SkillUseResult): List<NarrationStep> = when (result) {
        is SkillUseResult.Attack -> attackSteps(result.result)
        is SkillUseResult.Effect -> result.result.toNarrations(_uiState.value::nameOf).map { NarrationStep(it) }
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
 * 手番の味方が使えるスキルと、その時点のコスト・CT・選べる相手(魅了・挑発の制限を含む)で使えるかどうか([isSkillUsable])、通常攻撃・各スキルで選べる相手
 * ([selectableTargets])、防御できるか([defend]が成立するか。挑発中は不可)も反映する。先制の手番では先制スキルだけを一覧にする。味方のカードには、先制行動を予約できるか([canReservePreemptive])と
 * 予約済み・先制の手番待ちかも反映する。全個体の詳細([BattleUnitDetailUiModel])も組み立て直し、
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
            alliesBySlot[slot]?.let {
                it.toUiModel(
                    displayName = names.getValue(it.fpid),
                    isActive = it.fpid == actor,
                    canReservePreemptive = canReservePreemptive(it.fpid),
                    isPreemptiveWaiting = it.fpid in pendingPreemptive || (it.fpid in preemptiveTurns && it.fpid != actor),
                )
            }
        },
        currentActorId = actor,
        isPlayerTurn = isPlayerTurn,
        isPreemptiveTurn = isPreemptiveTurn,
        selection = if (isPlayerTurn) current.selection else BattleSelection.None,
        skills = actorAlly?.let { ally ->
            // 先制の手番では、先制スキルだけを一覧に出す。
            ally.activeSkills.filter { !isPreemptiveTurn || it.isPreemptive }.map { skill ->
                val targetIds = selectableTargetIds(ally.fpid, skill)
                BattleSkillUiModel(
                    id = skill.id,
                    name = skill.name,
                    mpCost = skill.costOf(SkillCostType.MP),
                    hpCost = skill.costOf(SkillCostType.HP),
                    tpCost = skill.costOf(SkillCostType.TP),
                    substituteHpCost = ally.substituteHpCostOf(skill),
                    remainingCoolTime = ally.skillCooldowns.firstOrNull { it.skillId == skill.id }?.remainingTurns ?: 0,
                    targetIds = targetIds,
                    isUsable = isSkillUsable(ally.fpid, skill),
                    isSelfTargeting = skill.range == SkillRange.RANGE_0,
                )
            }
        }.orEmpty(),
        attackTargetIds = actorAlly?.let { selectableTargetIds(it.fpid, normalAttack) }.orEmpty(),
        canDefend = actorAlly?.let { defend(it.fpid) != null } ?: true,
        outcome = outcome,
        unitDetails = details,
        detailUnitId = current.detailUnitId?.takeIf { outcome == null && it in details },
    )
}

/**
 * 個体1体分の詳細ポップアップ用UIモデルを組み立てる。
 *
 * 値は[BattleFieldPawn]に該当プロパティがあればそれを(ステータスは基礎値のまま。補正後の値は出さない)、無ければ味方は[BattleAlly.character]、
 * 敵は[BattleEnemy.enemy]から取る。位置は配置上の段と[effectiveRowOf]の有効な段から決める。ステータス変化は[BattleFieldPawn.statModifiers]をステータスごとにまとめて組み立てる。
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
        mp = pawn.currentMp,
        maxMp = pawn.maxMp,
        tp = pawn.currentTp,
        maxTp = pawn.maxTp,
        breakGauge = floor(pawn.breakGauge).toInt(),
        breakState = pawn.breakState,
        isDefending = pawn.isDefending,
        position = position,
        attack = pawn.attack,
        defense = pawn.defense,
        manaOutput = ally?.character?.manaOutput ?: enemy?.enemy?.manaOutput ?: 0,
        manaResistance = ally?.character?.manaResistance ?: enemy?.enemy?.manaResistance ?: 0,
        agility = pawn.agility,
        luck = pawn.luck,
        accuracy = HitChanceFormula.BASE_RATE,
        evasion = HitChanceFormula.BASE_RATE,
        criticalRate = CriticalFormula.BASE_RATE_NUMERATOR * 100 / CriticalFormula.RATE_DENOMINATOR,
        criticalDamage = pawn.criticalDamageRate,
        breakResistance = pawn.breakResistance,
        breakPointRate = pawn.breakPointRate,
        turnCount = pawn.turnCount,
        remainingDistance = floor(pawn.remainingDistance).toInt(),
        activeSkillNames = ally?.activeSkills?.map { it.name }.orEmpty(),
        passiveSkillNames = pawn.passiveSkills.map { it.name },
        statChanges = BattleStat.entries.mapNotNull { stat ->
            val entries = pawn.statModifiers.filter { it.stat == stat }
            if (entries.isEmpty()) {
                null
            } else {
                StatChangeDetail(
                    stat = stat,
                    totalStage = pawn.effectiveStage(stat),
                    entries = entries.map { StatChangeEntry(it.stages, it.remainingTurns) },
                )
            }
        },
        ailments = pawn.ailments.map { AilmentDetail(it.ailment, it.remainingTurns) },
        elementResistances = ElementResistances.ORDER.map { ElementResistanceDetail(it, pawn.elementResistances.of(it)) },
        ailmentResistances = AilmentResistanceType.entries.map { AilmentResistanceDetail(it, pawn.ailmentResistances.of(it)) },
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
    mana = currentMp,
    maxMana = maxMp,
    isDefeated = isDefeated,
    isActive = isActive,
    breakGaugeFraction = floor(breakGauge).toFloat() / BreakFormula.MAX_GAUGE.toFloat(),
    breakState = breakState,
    tp = currentTp,
    maxTp = maxTp,
    ailments = ailments.map { it.ailment },
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
 * @param canReservePreemptive いま先制行動を予約できるかどうか(`domain`の`canReservePreemptive`の結果)。
 * @param isPreemptiveWaiting 先制行動を予約済み、または先制の手番を待っているかどうか。
 * @return 変換後の[BattleUnitUiModel]。
 */
private fun BattleAlly.toUiModel(
    displayName: String,
    isActive: Boolean,
    canReservePreemptive: Boolean,
    isPreemptiveWaiting: Boolean,
) = BattleUnitUiModel(
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
    tp = currentTp,
    maxTp = maxTp,
    canReservePreemptive = canReservePreemptive,
    isPreemptiveWaiting = isPreemptiveWaiting,
    ailments = ailments.map { it.ailment },
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
