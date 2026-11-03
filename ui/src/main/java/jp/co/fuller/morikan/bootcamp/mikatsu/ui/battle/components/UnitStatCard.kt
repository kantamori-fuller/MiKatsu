package jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import jp.co.fuller.morikan.bootcamp.mikatsu.core.theme.BreakGaugeYellow
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BreakState
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.R
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleUnitUiModel

/** 戦闘不能のカードに適用する不透明度(グレーアウト表現)。 */
private const val DEFEATED_ALPHA = 0.4f

/** 選択中のカードに表示する枠線の太さ。 */
private val SELECTED_BORDER_WIDTH = 3.dp

/** 選べる相手のカードに表示する枠線の太さ。選択中の枠線より太くして目立たせる。 */
private val SELECTABLE_BORDER_WIDTH = 5.dp

/** 選べる相手の枠線の色。選択中(primary)やMPゲージ(tertiary)と区別できるよう、固定の目立つ色にする。 */
private val SELECTABLE_BORDER_COLOR = Color(0xFFFFB300)

/** 選べる相手の枠線が点滅するときの、最も薄いときの不透明度。 */
private const val SELECTABLE_BLINK_MIN_ALPHA = 0.35f

/** 選べる相手の枠線が薄い状態から濃い状態へ変わるまでの時間(ミリ秒)。 */
private const val SELECTABLE_BLINK_DURATION_MS = 700

/**
 * バトル画面で敵・味方1体分の状態(名前・HP・MP・ブレイクゲージ)を表示するカード。
 *
 * 敵陣営・味方陣営の双方から共通で利用され、この画面における
 * 「四角いカード」という見た目を1箇所にまとめることを目的とする。
 *
 * カードの幅は並べ方によって異なるため、呼び出し側が[modifier]で指定する。
 * [onClick]を指定した場合はタップ可能なカードとなり、[isSelected]が`true`のときは
 * 選択中(手番中の個体)であることを示す枠線を表示する。[isSelectable]が`true`のとき(いま選べる相手)は、
 * それより太く目立つ色で点滅する枠線を表示し、[isSelected]の枠線よりも優先する。
 * [onClick]が`null`の場合はタップできない表示専用のカードとなる。
 * [BattleUnitUiModel.isDefeated]のときは半透明にしてグレーアウトし、[onClick]があってもタップできない。
 *
 * @param unit 表示する敵またはキャラクターの状態。
 * @param modifier このComposableに適用する[Modifier]。
 * @param isSelected 選択中(手番中の個体)として強調表示するかどうか。
 * @param isSelectable 行動の対象として選べる相手として、点滅する太い枠線で強調表示するかどうか。
 * @param onClick カードがタップされたときに呼ばれるコールバック。タップ不可にする場合は`null`。
 */
@Composable
internal fun UnitStatCard(
    unit: BattleUnitUiModel,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    isSelectable: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val border = when {
        isSelectable -> BorderStroke(SELECTABLE_BORDER_WIDTH, SELECTABLE_BORDER_COLOR.copy(alpha = rememberBlinkAlpha()))
        isSelected -> BorderStroke(SELECTED_BORDER_WIDTH, MaterialTheme.colorScheme.primary)
        else -> null
    }
    val cardModifier = if (unit.isDefeated) modifier.alpha(DEFEATED_ALPHA) else modifier
    if (onClick != null && !unit.isDefeated) {
        Card(onClick = onClick, modifier = cardModifier, border = border) {
            UnitStatContent(unit)
        }
    } else {
        Card(modifier = cardModifier, border = border) {
            UnitStatContent(unit)
        }
    }
}

/**
 * 選べる相手の枠線を点滅させるための不透明度を、無限に繰り返すアニメーションで返す。
 *
 * @return [SELECTABLE_BLINK_MIN_ALPHA]から1の間を往復する不透明度。
 */
@Composable
private fun rememberBlinkAlpha(): Float {
    val transition = rememberInfiniteTransition(label = "selectableBlink")
    val alpha by transition.animateFloat(
        initialValue = SELECTABLE_BLINK_MIN_ALPHA,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(SELECTABLE_BLINK_DURATION_MS), RepeatMode.Reverse),
        label = "selectableBlinkAlpha",
    )
    return alpha
}

/**
 * [UnitStatCard]の中身となる、名前・HP・MP・ブレイクゲージの表示部分。
 *
 * ブレイクゲージは数値を表示せず黄色のバーだけで示し、ブレイク中・立ち上がり中は状態のラベルを添える。
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
        LinearProgressIndicator(
            progress = { unit.breakGaugeFraction.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            color = BreakGaugeYellow,
        )
        when (unit.breakState) {
            BreakState.BROKEN -> Text(
                stringResource(R.string.battle_break_state_broken),
                style = MaterialTheme.typography.labelSmall,
            )
            BreakState.RECOVERING -> Text(
                stringResource(R.string.battle_break_state_recovering),
                style = MaterialTheme.typography.labelSmall,
            )
            BreakState.NONE -> Unit
        }
    }
}
