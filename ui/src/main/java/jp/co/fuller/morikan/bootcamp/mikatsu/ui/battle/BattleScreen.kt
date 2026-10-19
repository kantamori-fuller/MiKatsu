package jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.R
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.components.FormationGrid
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.components.FormationSlotFrame
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.components.UnitStatCard
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleNarration
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleUnitUiModel

private const val ENEMY_GRID_COLUMNS = 3

/** 敵陣営のカード1枚あたりの幅。 */
private val ENEMY_CARD_WIDTH = 100.dp

/**
 * 敵陣営・味方陣営の状態表示とコマンド選択を行うバトル画面。
 *
 * [BattleViewModel.uiState]を購読して描画するのみに徹する(MVVMにおけるViewの責務のみを担う)。
 * 画面上部に敵陣営(最大6体)のカードを並べ、画面下部には編成画面と同じ形の陣形の表
 * (縦2マス×横3マス)に味方陣営のカードを配置し、その下にコマンド選択部分(攻撃・撤退)を
 * 配置する。味方のカードはタップで1体のみ選択でき、攻撃ボタンは味方を選択している間のみ押下できる。
 * 陣形の上にはバトルナレーター(次の操作の指示)を表示する。攻撃ボタン押下後は対象選択状態となり、
 * ボタン表示が「選択中」「キャンセル」に変わって敵のカードを選択できるようになる。
 * 他の画面と異なりトップバーは表示しない。
 *
 * @param viewModel この画面に対応する[BattleViewModel]。
 * @param onRetreat 「撤退」ボタンが押されたときに呼ばれるコールバック。
 *   編成画面への遷移をNavHost側に委ねる。
 * @param modifier このComposableに適用する[Modifier]。
 */
@Composable
fun BattleScreen(
    viewModel: BattleViewModel,
    onRetreat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(modifier = modifier) { innerPadding ->
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
                text = stringResource(uiState.narration.toStringRes()),
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
                            isSelected = member.id == uiState.selectedAllyId,
                            onClick = if (uiState.isSelectingTarget) {
                                null
                            } else {
                                { viewModel.onAllyClick(member.id) }
                            },
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
                        modifier = Modifier.width(ENEMY_CARD_WIDTH),
                        onClick = onEnemyClick?.let { onClick -> { onClick(enemy.id) } },
                    )
                }
            }
        }
    }
}

/**
 * [BattleNarration]を、画面に表示する文言の文字列リソースIDへ変換する。
 *
 * ViewModelやUiStateに文言を持ち込まず、文字列リソース経由で表示するための対応表となる。
 *
 * @receiver 変換元のバトルナレーターの種類。
 * @return 対応する文言の文字列リソースID。
 */
@StringRes
private fun BattleNarration.toStringRes(): Int = when (this) {
    BattleNarration.SelectCharacter -> R.string.battle_narration_select_character
    BattleNarration.SelectAction -> R.string.battle_narration_select_action
    BattleNarration.SelectTarget -> R.string.battle_narration_select_target
}
