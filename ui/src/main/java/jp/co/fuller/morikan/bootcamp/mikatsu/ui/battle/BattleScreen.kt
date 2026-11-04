package jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jp.co.fuller.morikan.bootcamp.mikatsu.core.theme.MiKatsuTheme
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleOutcome
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.CriticalType
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Formation
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.R
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.components.FormationGrid
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.components.FormationSlotFrame
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.components.UnitDetailPopup
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.components.UnitStatCard
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleNarration
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.labelRes
import kotlin.math.abs
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleSelection
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleSkillUiModel
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleUiState
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleUnitUiModel

/** 勝敗表示のオーバーレイ背景の不透明度。 */
private const val OVERLAY_ALPHA = 0.6f

/** スキル一覧の最大の高さ。スキルが多い場合はこの高さの中でスクロールする。 */
private val SKILL_LIST_MAX_HEIGHT = 160.dp

/**
 * 敵陣営・味方陣営の状態表示とコマンド選択を行うバトル画面。
 *
 * [BattleViewModel.uiState]の購読と、ViewModelのイベント関数をContentへ中継することだけを担い、
 * 描画は[BattleScreenContent]に任せる(MVVMにおけるViewの責務のみを担う)。
 *
 * @param viewModel この画面に対応する[BattleViewModel]。
 * @param onRetreat 「撤退」ボタンが押されたときに呼ばれるコールバック。
 *   編成画面への遷移をNavHost側に委ねる。
 * @param onDefeat 敗北表示がタップされたときに呼ばれるコールバック。
 *   メインメニューへの遷移をNavHost側に委ねる。
 * @param modifier このComposableに適用する[Modifier]。
 */
@Composable
fun BattleScreen(
    viewModel: BattleViewModel,
    onRetreat: () -> Unit,
    onDefeat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BattleScreenContent(
        uiState = uiState,
        onRetreat = onRetreat,
        onDefeat = onDefeat,
        onEnemyClick = viewModel::onEnemyClick,
        onAllyClick = viewModel::onAllyClick,
        onUnitLongPress = viewModel::onUnitLongPress,
        onNarrationClick = viewModel::onNarrationClick,
        onAttackClick = viewModel::onAttackClick,
        onSkillClick = viewModel::onSkillClick,
        onSkillSelected = viewModel::onSkillSelected,
        onDefendClick = viewModel::onDefendClick,
        onCancelClick = viewModel::onCancelClick,
        onUnitDetailDismiss = viewModel::onUnitDetailDismiss,
        onNextBattleClick = viewModel::onNextBattleClick,
        modifier = modifier,
    )
}

/**
 * 敵陣営・味方陣営の状態表示とコマンド選択を行うバトル画面。
 *
 * ViewModelに依存せず、[uiState]とコールバックだけで描画することでPreviewできるようにする(戦闘画面の描画本体)。
 * 画面上部に敵陣営(最大6体)のカードを2行×3列で並べ(前衛が味方に近い下側、後衛が上側)、画面下部には編成画面と同じ形の陣形の表
 * (縦2マス×横3マス)に味方陣営のカードを配置し、その下にコマンド選択部分(攻撃・スキル・撤退)を
 * 配置する。手番が来た味方は自動で行動者となり、選択中と同じ見た目で強調表示される。
 * 攻撃ボタンはプレイヤーの手番で演出中でなく、攻撃が届く相手がいるときのみ押下できる。陣形の上にはバトルナレーターを表示する。
 * 攻撃ボタン押下後は対象選択状態となり、ボタン表示が「選択中」「キャンセル」に変わって
 * 攻撃が届く相手のカードを選択できるようになる。スキルボタン押下後はスキル一覧(名前とMP消費量)が表示され、
 * スキルを選ぶとそのスキルが届く相手のカードを選択できるようになる。選べる相手のカードは、
 * 点滅する太い枠線で強調され、敵・味方どちらのカードでもタップできる(それ以外のカードはタップできない)。戦闘不能のカードはグレーアウトされる。勝敗が決まると
 * 画面全体に結果を重ねて表示する。勝利時は「次に進む」ボタンで[onNextBattleClick]を呼び、
 * 敗北時はオーバーレイのタップで[onDefeat]を呼ぶ。敵カードの下の空きには進行度を表示する。
 * 先制行動を予約できる味方のカードは細いオレンジの枠で示し、タップで予約する(予約済みのカードには「先制待ち」を表示する)。
 * 先制の手番では、スキル一覧に先制スキルだけを出し、攻撃・防御・撤退を無効にする。
 * カードを長押しすると[UnitDetailPopup]で個体の詳細を重ねて表示する。
 * 他の画面と異なりトップバーは表示しない。
 *
 * @param uiState 描画する画面状態。
 * @param onRetreat 「撤退」ボタンが押されたときに呼ばれるコールバック。
 *   編成画面への遷移をNavHost側に委ねる。
 * @param onDefeat 敗北表示がタップされたときに呼ばれるコールバック。
 *   メインメニューへの遷移をNavHost側に委ねる。
 * @param onEnemyClick 選べる敵のカードがタップされたときに、敵のFPIDを引数に呼ばれる。
 * @param onAllyClick 選べる味方(対象選び中)または先制行動を予約できる味方のカードがタップされたときに、味方のFPIDを引数に呼ばれる。
 * @param onUnitLongPress カードが長押しされたときに、個体のFPIDを引数に呼ばれる。
 * @param onNarrationClick ナレーション欄がタップされたときに呼ばれる。
 * @param onAttackClick 「攻撃」ボタンが押されたときに呼ばれる。
 * @param onSkillClick 「スキル」ボタンが押されたときに呼ばれる。
 * @param onSkillSelected スキル一覧でスキルが選ばれたときに、スキルIDを引数に呼ばれる。
 * @param onDefendClick 「防御」ボタンが押されたときに呼ばれる。
 * @param onCancelClick 選択中に「キャンセル」ボタンが押されたときに呼ばれる。
 * @param onUnitDetailDismiss 詳細ポップアップが閉じられたときに呼ばれる。
 * @param onNextBattleClick 勝利時の「次に進む」ボタンが押されたときに呼ばれる。
 * @param modifier このComposableに適用する[Modifier]。
 */
@Composable
internal fun BattleScreenContent(
    uiState: BattleUiState,
    onRetreat: () -> Unit,
    onDefeat: () -> Unit,
    onEnemyClick: (Int) -> Unit,
    onAllyClick: (Int) -> Unit,
    onUnitLongPress: (Int) -> Unit,
    onNarrationClick: () -> Unit,
    onAttackClick: () -> Unit,
    onSkillClick: () -> Unit,
    onSkillSelected: (Int) -> Unit,
    onDefendClick: () -> Unit,
    onCancelClick: () -> Unit,
    onUnitDetailDismiss: () -> Unit,
    onNextBattleClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(16.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    EnemyGrid(
                        enemySlots = uiState.enemySlots,
                        selectableTargetIds = uiState.selectableTargetIds,
                        onEnemyClick = onEnemyClick,
                        onEnemyLongClick = onUnitLongPress,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = stringResource(R.string.map_progress_format, uiState.progress),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = uiState.narration?.toText().orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onNarrationClick,
                        ),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                FormationGrid(
                    slotContent = { slot, slotModifier ->
                        val member = uiState.partySlots[slot]
                        if (member != null) {
                            val isSelectable = member.id in uiState.selectableTargetIds
                            val isReservable = member.id in uiState.reservableAllyIds
                            UnitStatCard(
                                unit = member,
                                modifier = slotModifier,
                                isSelected = member.isActive,
                                isSelectable = isSelectable,
                                isReservable = isReservable,
                                onClick = if (isSelectable || isReservable) {
                                    { onAllyClick(member.id) }
                                } else {
                                    null
                                },
                                onLongClick = { onUnitLongPress(member.id) },
                            )
                        } else {
                            FormationSlotFrame(modifier = slotModifier)
                        }
                    },
                )
                if (uiState.selection == BattleSelection.SkillList) {
                    Spacer(Modifier.height(8.dp))
                    SkillList(skills = uiState.skills, onSkillClick = onSkillSelected)
                }
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Button(
                        onClick = onAttackClick,
                        enabled = uiState.isAttackEnabled,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            stringResource(
                                if (uiState.isSelectingTarget) R.string.action_selecting else R.string.action_attack,
                            ),
                        )
                    }
                    Button(
                        onClick = onSkillClick,
                        enabled = uiState.isSkillEnabled,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            stringResource(
                                if (uiState.isSelectingSkill) R.string.action_selecting else R.string.action_skill,
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Button(
                        onClick = onDefendClick,
                        enabled = uiState.isDefendEnabled,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.action_defend))
                    }
                    val isSelecting = uiState.selection != BattleSelection.None
                    Button(
                        onClick = if (isSelecting) onCancelClick else onRetreat,
                        enabled = if (isSelecting) uiState.isCancelEnabled else uiState.isRetreatEnabled,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            stringResource(if (isSelecting) R.string.action_cancel else R.string.action_retreat),
                        )
                    }
                }
            }
        }
        uiState.unitDetail?.let { detail ->
            UnitDetailPopup(detail = detail, onDismiss = onUnitDetailDismiss)
        }
        uiState.outcome?.let { outcome ->
            BattleOutcomeOverlay(
                outcome = outcome,
                isNextEnabled = !uiState.isProcessing,
                onNextClick = onNextBattleClick,
                onDefeatClick = onDefeat,
            )
        }
    }
}

/**
 * 手番の味方が使えるアクティブスキルを、名前とコスト(MP、HPコストがあればHPも)つきで縦に並べて表示する。
 *
 * 現在のMP・HPでコストを支払えないスキルは選択できない(無効表示になる)。TPが足りない分はHPで肩代わりでき、
 * 肩代わりするHPがあるとき(使えない場合も)、TPの表示は「TP40(HP-12)」の形になる。CT中のスキルは、コストの後ろに「CT残り1」を付ける(無効のまま)。数が多い場合は
 * [SKILL_LIST_MAX_HEIGHT]の中でスクロールする。
 *
 * @param skills 表示するスキルの一覧。
 * @param onSkillClick スキルが選ばれたときに呼ばれるコールバック(引数はスキルID)。
 * @param modifier このComposableに適用する[Modifier]。
 */
@Composable
private fun SkillList(
    skills: List<BattleSkillUiModel>,
    onSkillClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = SKILL_LIST_MAX_HEIGHT)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        skills.forEach { skill ->
            OutlinedButton(
                onClick = { onSkillClick(skill.id) },
                enabled = skill.isUsable,
                modifier = Modifier.fillMaxWidth(),
            ) {
                val costs = listOfNotNull(
                    skill.mpCost.takeIf { it > 0 }?.let { stringResource(R.string.battle_skill_cost_mp_format, it) },
                    skill.hpCost.takeIf { it > 0 }?.let { stringResource(R.string.battle_skill_cost_hp_format, it) },
                    skill.tpCost.takeIf { it > 0 }?.let {
                        if (skill.substituteHpCost > 0) {
                            stringResource(R.string.battle_skill_cost_tp_substitute_format, it, skill.substituteHpCost)
                        } else {
                            stringResource(R.string.battle_skill_cost_tp_format, it)
                        }
                    },
                )
                val nameWithCosts = if (costs.isEmpty()) {
                    skill.name
                } else {
                    stringResource(
                        R.string.battle_skill_item_format,
                        skill.name,
                        costs.joinToString(stringResource(R.string.battle_skill_cost_separator)),
                    )
                }
                Text(
                    if (skill.remainingCoolTime > 0) {
                        stringResource(
                            R.string.battle_skill_item_format,
                            nameWithCosts,
                            stringResource(R.string.battle_skill_cooldown_format, skill.remainingCoolTime),
                        )
                    } else {
                        nameWithCosts
                    },
                )
            }
        }
    }
}

/**
 * 勝敗を画面全体に重ねて表示するオーバーレイ。
 *
 * 勝利時は「勝利!」の下に「次に進む」ボタンを表示し、オーバーレイ自体のタップでは何も起きない。
 * 敗北時は「敗北…」とメインメニューへ戻る案内を表示し、オーバーレイのタップで[onDefeatClick]を呼ぶ。
 *
 * @param outcome 表示する勝敗。
 * @param isNextEnabled 「次に進む」ボタンを押下できるかどうか。
 * @param onNextClick 「次に進む」ボタンが押されたときに呼ばれるコールバック。
 * @param onDefeatClick 敗北時にオーバーレイがタップされたときに呼ばれるコールバック。
 */
@Composable
private fun BattleOutcomeOverlay(
    outcome: BattleOutcome,
    isNextEnabled: Boolean,
    onNextClick: () -> Unit,
    onDefeatClick: () -> Unit,
) {
    val isVictory = outcome == BattleOutcome.Victory
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = OVERLAY_ALPHA))
            .then(if (isVictory) Modifier.pointerInput(Unit) {} else Modifier.clickable(onClick = onDefeatClick)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(if (isVictory) R.string.battle_outcome_victory else R.string.battle_outcome_defeat),
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
            )
            Spacer(Modifier.height(16.dp))
            if (isVictory) {
                Button(onClick = onNextClick, enabled = isNextEnabled) {
                    Text(stringResource(R.string.action_next))
                }
            } else {
                Text(
                    text = stringResource(R.string.battle_outcome_tap_to_return_main_menu),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
            }
        }
    }
}

/**
 * 敵陣営のカードを、2行×3列のグリッド状に並べて表示する。
 *
 * 味方に近い側(画面の下側)に前衛(スロット0〜2)、遠い側(上側)に後衛(スロット3〜5)を置き、
 * 列はスロット番号の`% 3`に従う。敵のいないマスは、同じ幅の空白にして列を揃える。
 * いま選べる相手([selectableTargetIds]に含まれる敵)だけが、目立つ枠線つきでタップできる。
 *
 * @param enemySlots スロット番号ごとの敵。敵のいないマスは`null`。要素数は[Formation.SLOT_COUNT]。
 * @param selectableTargetIds いま選べる対象のFPID。
 * @param onEnemyClick 敵のカードがタップされたときに呼ばれるコールバック(引数はタップされた敵のFPID)。
 * @param onEnemyLongClick 敵のカードが長押しされたときに呼ばれるコールバック(引数は長押しされた敵のFPID)。
 * @param modifier このComposableに適用する[Modifier]。
 */
@Composable
private fun EnemyGrid(
    enemySlots: List<BattleUnitUiModel?>,
    selectableTargetIds: Set<Int>,
    onEnemyClick: (Int) -> Unit,
    onEnemyLongClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 画面の上から順に後衛(最後の行)、前衛(最初の行)とする。味方側と同じ表で並べて幅・間隔を揃える。
    FormationGrid(
        modifier = modifier,
        slotContent = { slot, slotModifier ->
            val displayRow = slot / Formation.COLUMNS
            val column = slot % Formation.COLUMNS
            val enemy = enemySlots.getOrNull((Formation.ROWS - 1 - displayRow) * Formation.COLUMNS + column)
            if (enemy != null) {
                val isSelectable = enemy.id in selectableTargetIds
                UnitStatCard(
                    unit = enemy,
                    isSelected = enemy.isActive,
                    isSelectable = isSelectable,
                    modifier = slotModifier,
                    onClick = if (isSelectable) {
                        { onEnemyClick(enemy.id) }
                    } else {
                        null
                    },
                    onLongClick = { onEnemyLongClick(enemy.id) },
                )
            } else {
                Spacer(slotModifier)
            }
        },
    )
}

/**
 * [BattleNarration]を、画面に表示する文言へ変換する。
 *
 * ViewModelやUiStateに文言を持ち込まず、文字列リソース(書式付き)経由で表示するための対応表となる。
 *
 * @receiver 変換元のバトルナレーターの種類。
 * @return 引数を埋め込んだ表示文言。
 */
@Composable
private fun BattleNarration.toText(): String = when (this) {
    is BattleNarration.SelectAction -> stringResource(R.string.battle_narration_select_action, actorName)
    BattleNarration.SelectTarget -> stringResource(R.string.battle_narration_select_target)
    is BattleNarration.EnemyTurn -> stringResource(R.string.battle_narration_enemy_turn, actorName)
    is BattleNarration.EnemyCannotReach -> stringResource(R.string.battle_narration_enemy_cannot_reach, actorName)
    BattleNarration.SelectSkill -> stringResource(R.string.battle_narration_select_skill)
    BattleNarration.SelectAllyTarget -> stringResource(R.string.battle_narration_select_ally_target)
    is BattleNarration.AttackResult -> stringResource(
        when (critical) {
            CriticalType.NONE ->
                if (isCriticalBlocked) R.string.battle_narration_attack_result_critical_blocked else R.string.battle_narration_attack_result
            CriticalType.CRITICAL -> R.string.battle_narration_attack_result_critical
            CriticalType.BAD_CRITICAL -> R.string.battle_narration_attack_result_bad_critical
        },
        attackerName,
        targetName,
        damage,
        skillName ?: stringResource(R.string.action_attack),
    )
    is BattleNarration.SkillResult ->
        stringResource(R.string.battle_narration_skill_result, actorName, targetName, skillName, healedAmount)
    is BattleNarration.AttackMissed -> stringResource(R.string.battle_narration_attack_missed, attackerName, targetName, skillName ?: stringResource(R.string.action_attack))
    is BattleNarration.ShotHit -> stringResource(
        when (critical) {
            CriticalType.NONE ->
                if (isCriticalBlocked) R.string.battle_narration_shot_critical_blocked else R.string.battle_narration_shot_hit
            CriticalType.CRITICAL -> R.string.battle_narration_shot_critical
            CriticalType.BAD_CRITICAL -> R.string.battle_narration_shot_bad_critical
        },
        shotNumber,
        damage,
    )
    is BattleNarration.ShotMissed -> stringResource(R.string.battle_narration_shot_missed, shotNumber)
    is BattleNarration.AttackTotal ->
        stringResource(R.string.battle_narration_attack_total, attackerName, targetName, totalDamage, shotCount, hitCount)
    is BattleNarration.AreaAttackStart -> stringResource(R.string.battle_narration_area_attack_start, attackerName, skillName ?: stringResource(R.string.action_attack))
    is BattleNarration.AreaAttackAllMissed -> stringResource(R.string.battle_narration_area_attack_all_missed, attackerName, skillName ?: stringResource(R.string.action_attack))
    is BattleNarration.AreaMissed -> stringResource(
        R.string.battle_narration_area_missed,
        targetNames.joinToString(stringResource(R.string.battle_narration_area_missed_separator)),
    )
    is BattleNarration.AreaHit -> stringResource(
        R.string.battle_narration_area_hit,
        entries
            .map { stringResource(R.string.battle_narration_area_hit_entry, it.targetName, it.damage) }
            .joinToString(stringResource(R.string.battle_narration_area_hit_separator)),
    )
    is BattleNarration.AreaCritical -> stringResource(R.string.battle_narration_area_critical, targetName)
    is BattleNarration.AreaBadCritical -> stringResource(R.string.battle_narration_area_bad_critical, targetName)
    is BattleNarration.AreaCriticalBlocked -> stringResource(R.string.battle_narration_area_critical_blocked, targetName)
    is BattleNarration.Defend -> stringResource(R.string.battle_narration_defend, actorName)
    is BattleNarration.PreemptiveAction -> stringResource(R.string.battle_narration_preemptive_action, actorName)
    is BattleNarration.PreemptiveFailed -> stringResource(R.string.battle_narration_preemptive_failed, actorName)
    is BattleNarration.Broke -> stringResource(R.string.battle_narration_broke, targetName)
    is BattleNarration.BreakStunned -> stringResource(R.string.battle_narration_break_stunned, actorName)
    is BattleNarration.BreakRecovered -> stringResource(R.string.battle_narration_break_recovered, actorName)
    is BattleNarration.SkillUsed -> stringResource(R.string.battle_narration_skill_used, actorName, skillName)
    is BattleNarration.SkillMissed -> stringResource(R.string.battle_narration_skill_missed, actorName, skillName)
    is BattleNarration.StatStageChanged -> stringResource(
        if (stages >= 0) R.string.battle_narration_stat_up else R.string.battle_narration_stat_down,
        targetName,
        stringResource(stat.labelRes),
        abs(stages),
    )
    is BattleNarration.AttackNullified -> stringResource(R.string.battle_narration_attack_nullified, attackerName, targetName, skillName ?: stringResource(R.string.action_attack))
    is BattleNarration.AttackLead -> stringResource(R.string.battle_narration_attack_lead, attackerName, targetName, skillName ?: stringResource(R.string.action_attack))
    is BattleNarration.ShotLanded -> stringResource(R.string.battle_narration_shot_landed, shotNumber)
    is BattleNarration.ShotNullified -> stringResource(R.string.battle_narration_shot_nullified, shotNumber)
    is BattleNarration.AreaNullified -> stringResource(
        R.string.battle_narration_area_nullified,
        targetNames.joinToString(stringResource(R.string.battle_narration_area_missed_separator)),
    )
    is BattleNarration.AttackAbsorbed -> stringResource(R.string.battle_narration_attack_absorbed, targetName, healedAmount)
    is BattleNarration.AttackReflected -> stringResource(R.string.battle_narration_attack_reflected, targetName, attackerName, damage)
    is BattleNarration.AttackReflectedNullified -> stringResource(R.string.battle_narration_attack_reflected_nullified, targetName, attackerName)
    is BattleNarration.AttackReflectedAbsorbed ->
        stringResource(R.string.battle_narration_attack_reflected_absorbed, targetName, attackerName, healedAmount)
    is BattleNarration.SkillNullified -> stringResource(R.string.battle_narration_skill_nullified, actorName, skillName, targetName)
    is BattleNarration.EffectNullified -> stringResource(R.string.battle_narration_effect_nullified, targetName)
    is BattleNarration.EffectReflected -> stringResource(R.string.battle_narration_effect_reflected, targetName)
    is BattleNarration.EffectReflectedNullified -> stringResource(R.string.battle_narration_effect_reflected_nullified, actorName)
    is BattleNarration.MultiSkillResult ->
        stringResource(R.string.battle_narration_multi_skill_result, actorName, skillName, targetCount, totalHealed)
}

/** Preview用に、すべてのイベントを無視する[BattleScreenContent]を表示する。 */
@Composable
private fun BattleContentPreview(uiState: BattleUiState) {
    MiKatsuTheme(darkTheme = false) {
        BattleScreenContent(
            uiState = uiState,
            onRetreat = {}, onDefeat = {}, onEnemyClick = {}, onAllyClick = {}, onUnitLongPress = {},
            onNarrationClick = {}, onAttackClick = {}, onSkillClick = {}, onSkillSelected = {},
            onDefendClick = {}, onCancelClick = {}, onUnitDetailDismiss = {}, onNextBattleClick = {},
        )
    }
}

/** 味方の手番で、行動を選ぶ前の通常の状態を確認するためのPreview。 */
@Preview(showBackground = true, name = "戦闘(味方の手番)")
@Composable
private fun BattleScreenPlayerTurnPreview() {
    BattleContentPreview(previewBattleUiState())
}

/** スキル一覧を開いた状態(TP代替で使えるスキルと、HPが足りず使えないスキルの「TP40(HP-12)」表示と、CT中で使えないスキルの「CT残り1」表示を含む)を確認するためのPreview。 */
@Preview(showBackground = true, name = "戦闘(スキル一覧)")
@Composable
private fun BattleScreenSkillListPreview() {
    BattleContentPreview(previewBattleUiState().copy(selection = BattleSelection.SkillList))
}

/** 通常攻撃の対象選択中(選べる敵に点滅枠)の状態を確認するためのPreview。 */
@Preview(showBackground = true, name = "戦闘(対象選択)")
@Composable
private fun BattleScreenTargetSelectionPreview() {
    val base = previewBattleUiState()
    BattleContentPreview(
        base.copy(
            selection = BattleSelection.AttackTarget,
            attackTargetIds = base.enemySlots.filterNotNull().filter { !it.isDefeated }.map { it.id }.toSet(),
        ),
    )
}

/** 先制の手番(先制スキルだけの一覧。攻撃・防御・撤退は無効)を確認するためのPreview。 */
@Preview(showBackground = true, name = "戦闘(先制の手番のスキル一覧)")
@Composable
private fun BattleScreenPreemptiveSkillListPreview() {
    val base = previewBattleUiState()
    BattleContentPreview(
        base.copy(
            isPreemptiveTurn = true,
            selection = BattleSelection.SkillList,
            skills = listOf(previewPreemptiveSkill()),
        ),
    )
}

/** 手番ではない味方が先制行動を予約できる(細いオレンジの枠)状態を確認するためのPreview。 */
@Preview(showBackground = true, name = "戦闘(先制の予約)")
@Composable
private fun BattleScreenPreemptiveReservationPreview() {
    val base = previewBattleUiState()
    val party = base.partySlots.map { unit ->
        when (unit?.id) {
            1 -> unit
            2 -> unit.copy(canReservePreemptive = true)
            else -> unit
        }
    }
    BattleContentPreview(base.copy(partySlots = party))
}

/** 個体の詳細ポップアップを表示している状態を確認するためのPreview。 */
@Preview(showBackground = true, name = "戦闘(詳細ポップアップ)")
@Composable
private fun BattleScreenDetailPopupPreview() {
    val detail = previewUnitDetail()
    BattleContentPreview(
        previewBattleUiState().copy(unitDetails = mapOf(detail.id to detail), detailUnitId = detail.id),
    )
}

/** 勝利のオーバーレイを表示している状態を確認するためのPreview。 */
@Preview(showBackground = true, name = "戦闘(勝利)")
@Composable
private fun BattleScreenVictoryPreview() {
    BattleContentPreview(previewBattleUiState().copy(outcome = BattleOutcome.Victory))
}

/** 敗北のオーバーレイを表示している状態を確認するためのPreview。 */
@Preview(showBackground = true, name = "戦闘(敗北)")
@Composable
private fun BattleScreenDefeatPreview() {
    BattleContentPreview(previewBattleUiState().copy(outcome = BattleOutcome.Defeat))
}
