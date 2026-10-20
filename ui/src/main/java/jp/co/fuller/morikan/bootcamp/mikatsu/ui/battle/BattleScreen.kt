package jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleOutcome
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.R
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.components.FormationGrid
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.components.FormationSlotFrame
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.components.UnitStatCard
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleNarration
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleUnitUiModel

private const val ENEMY_GRID_COLUMNS = 3

/** 勝敗表示のオーバーレイ背景の不透明度。 */
private const val OVERLAY_ALPHA = 0.6f

/** 敵陣営のカード1枚あたりの幅。 */
private val ENEMY_CARD_WIDTH = 100.dp

/**
 * 敵陣営・味方陣営の状態表示とコマンド選択を行うバトル画面。
 *
 * [BattleViewModel.uiState]を購読して描画するのみに徹する(MVVMにおけるViewの責務のみを担う)。
 * 画面上部に敵陣営(最大6体)のカードを並べ、画面下部には編成画面と同じ形の陣形の表
 * (縦2マス×横3マス)に味方陣営のカードを配置し、その下にコマンド選択部分(攻撃・撤退)を
 * 配置する。手番が来た味方は自動で行動者となり、選択中と同じ見た目で強調表示される。
 * 攻撃ボタンはプレイヤーの手番で演出中でないときのみ押下できる。陣形の上にはバトルナレーターを表示する。
 * 攻撃ボタン押下後は対象選択状態となり、ボタン表示が「選択中」「キャンセル」に変わって
 * 敵のカードを選択できるようになる。戦闘不能のカードはグレーアウトされる。勝敗が決まると
 * 画面全体に結果を重ねて表示し、タップで[onBattleFinished]を呼ぶ。
 * 他の画面と異なりトップバーは表示しない。
 *
 * @param viewModel この画面に対応する[BattleViewModel]。
 * @param onRetreat 「撤退」ボタンが押されたときに呼ばれるコールバック。
 *   編成画面への遷移をNavHost側に委ねる。
 * @param onBattleFinished 勝敗表示がタップされたときに呼ばれるコールバック。
 *   編成画面への遷移をNavHost側に委ねる。
 * @param modifier このComposableに適用する[Modifier]。
 */
@Composable
fun BattleScreen(
    viewModel: BattleViewModel,
    onRetreat: () -> Unit,
    onBattleFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = modifier) {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(16.dp),
            ) {
                EnemyGrid(
                    enemies = uiState.enemies,
                    onEnemyClick = if (uiState.isSelectingTarget) viewModel::onEnemyClick else null,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = uiState.narration?.toText().orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                FormationGrid(
                    slotContent = { slot, slotModifier ->
                        val member = uiState.partySlots[slot]
                        if (member != null) {
                            UnitStatCard(
                                unit = member,
                                modifier = slotModifier,
                                isSelected = member.isActive,
                            )
                        } else {
                            FormationSlotFrame(modifier = slotModifier)
                        }
                    },
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Button(
                        onClick = viewModel::onAttackClick,
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
                        onClick = if (uiState.isSelectingTarget) viewModel::onCancelClick else onRetreat,
                        enabled = uiState.isSelectingTarget || (!uiState.isProcessing && uiState.outcome == null),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            stringResource(
                                if (uiState.isSelectingTarget) R.string.action_cancel else R.string.action_retreat,
                            ),
                        )
                    }
                }
            }
        }
        uiState.outcome?.let { outcome ->
            BattleOutcomeOverlay(outcome = outcome, onClick = onBattleFinished)
        }
    }
}

/**
 * 勝敗を画面全体に重ねて表示するオーバーレイ。
 *
 * 画面中央に勝敗と編成画面へ戻る案内を表示し、タップされると[onClick]を呼ぶ。
 *
 * @param outcome 表示する勝敗。
 * @param onClick オーバーレイがタップされたときに呼ばれるコールバック。
 */
@Composable
private fun BattleOutcomeOverlay(outcome: BattleOutcome, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = OVERLAY_ALPHA))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(
                    if (outcome == BattleOutcome.Victory) R.string.battle_outcome_victory else R.string.battle_outcome_defeat,
                ),
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.battle_outcome_tap_to_return),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
        }
    }
}

/**
 * 敵陣営のカードを、3列のグリッド状に並べて表示する。
 *
 * 敵は最大6体を想定するため3列×2行の枠に収まるが、実際に並ぶ行数は[enemies]の件数に従う。
 *
 * @param enemies 表示する敵の一覧。
 * @param onEnemyClick 敵のカードがタップされたときに呼ばれるコールバック(引数はタップされた敵のFPID)。
 *   敵を選択できない状態では`null`とし、カードをタップ不可にする。
 * @param modifier このComposableに適用する[Modifier]。
 */
@Composable
private fun EnemyGrid(
    enemies: List<BattleUnitUiModel>,
    onEnemyClick: ((Int) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        enemies.chunked(ENEMY_GRID_COLUMNS).forEach { rowEnemies ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                rowEnemies.forEach { enemy ->
                    UnitStatCard(
                        unit = enemy,
                        isSelected = enemy.isActive,
                        modifier = Modifier.width(ENEMY_CARD_WIDTH),
                        onClick = onEnemyClick?.let { onClick -> { onClick(enemy.id) } },
                    )
                }
            }
        }
    }
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
    is BattleNarration.AttackResult ->
        stringResource(R.string.battle_narration_attack_result, attackerName, targetName, damage)
}
