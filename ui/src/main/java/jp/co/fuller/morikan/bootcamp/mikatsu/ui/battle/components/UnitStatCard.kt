package jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.R
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleUnitUiModel

/**
 * バトル画面で敵・味方1体分の状態(名前・HP・MP)を表示するカード。
 *
 * 敵陣営・味方陣営の双方から共通で利用され、この画面における
 * 「四角いカード」という見た目を1箇所にまとめることを目的とする。
 *
 * @param unit 表示する敵またはキャラクターの状態。
 * @param modifier このComposableに適用する[Modifier]。
 */
@Composable
internal fun UnitStatCard(
    unit: BattleUnitUiModel,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.width(100.dp)) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                unit.name,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(R.string.battle_hp_format, unit.hp, unit.maxHp),
                style = MaterialTheme.typography.labelSmall,
            )
            LinearProgressIndicator(
                progress = { unit.hp.toFloat() / unit.maxHp.coerceAtLeast(1) },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                stringResource(R.string.battle_mp_format, unit.mana, unit.maxMana),
                style = MaterialTheme.typography.labelSmall,
            )
            LinearProgressIndicator(
                progress = { unit.mana.toFloat() / unit.maxMana.coerceAtLeast(1) },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}
