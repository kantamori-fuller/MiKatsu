package jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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

/** 選択中のカードに表示する枠線の太さ。 */
private val SELECTED_BORDER_WIDTH = 3.dp

/**
 * バトル画面で敵・味方1体分の状態(名前・HP・MP)を表示するカード。
 *
 * 敵陣営・味方陣営の双方から共通で利用され、この画面における
 * 「四角いカード」という見た目を1箇所にまとめることを目的とする。
 *
 * カードの幅は並べ方によって異なるため、呼び出し側が[modifier]で指定する。
 * [onClick]を指定した場合はタップ可能なカードとなり、[isSelected]が`true`のときは
 * 選択中であることを示す枠線を表示する。[onClick]が`null`の場合はタップできない表示専用のカードとなる。
 *
 * @param unit 表示する敵またはキャラクターの状態。
 * @param modifier このComposableに適用する[Modifier]。
 * @param isSelected 選択中として強調表示するかどうか。
 * @param onClick カードがタップされたときに呼ばれるコールバック。タップ不可にする場合は`null`。
 */
@Composable
internal fun UnitStatCard(
    unit: BattleUnitUiModel,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val border = if (isSelected) {
        BorderStroke(SELECTED_BORDER_WIDTH, MaterialTheme.colorScheme.primary)
    } else {
        null
    }
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, border = border) {
            UnitStatContent(unit)
        }
    } else {
        Card(modifier = modifier, border = border) {
            UnitStatContent(unit)
        }
    }
}

/**
 * [UnitStatCard]の中身となる、名前・HP・MPの表示部分。
 *
 * タップ可能なカードと表示専用のカードで同じ中身を共有するために切り出している。
 *
 * @param unit 表示する敵またはキャラクターの状態。
 */
@Composable
private fun UnitStatContent(unit: BattleUnitUiModel) {
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
