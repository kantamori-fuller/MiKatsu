package jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.R
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.components.FormationGrid
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.components.FormationSlotFrame
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.components.UnitStatCard
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
 * 配置する。他の画面と異なりトップバーは表示しない。
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
            EnemyGrid(enemies = uiState.enemies, modifier = Modifier.weight(1f))
            Spacer(Modifier.height(16.dp))
            FormationGrid(
                slotContent = { slot, slotModifier ->
                    val member = uiState.partySlots[slot]
                    if (member != null) {
                        UnitStatCard(unit = member, modifier = slotModifier)
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
                Button(onClick = viewModel::onAttackClick, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.action_attack))
                }
                Button(onClick = onRetreat, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.action_retreat))
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
 * @param modifier このComposableに適用する[Modifier]。
 */
@Composable
private fun EnemyGrid(
    enemies: List<BattleUnitUiModel>,
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
                    UnitStatCard(unit = enemy, modifier = Modifier.width(ENEMY_CARD_WIDTH))
                }
            }
        }
    }
}
