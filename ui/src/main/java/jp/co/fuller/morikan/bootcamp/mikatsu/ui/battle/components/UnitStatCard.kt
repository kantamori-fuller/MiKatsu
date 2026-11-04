package jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import jp.co.fuller.morikan.bootcamp.mikatsu.core.theme.BreakGaugeYellow
import jp.co.fuller.morikan.bootcamp.mikatsu.core.theme.MiKatsuTheme
import jp.co.fuller.morikan.bootcamp.mikatsu.core.theme.TacticalPointOrange
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BreakState
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.StatusAilment
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.shortLabelRes
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.R
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleUnitUiModel
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.previewAlly
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.previewEnemy

/** カード右上のTP円ゲージの大きさ。 */
private val TP_GAUGE_SIZE = 24.dp

/** 戦闘不能のカードに適用する不透明度(グレーアウト表現)。 */
private const val DEFEATED_ALPHA = 0.4f

/** 選択中のカードに表示する枠線の太さ。 */
private val SELECTED_BORDER_WIDTH = 3.dp

/** 選べる相手のカードに表示する枠線の太さ。選択中の枠線より太くして目立たせる。 */
private val SELECTABLE_BORDER_WIDTH = 5.dp

/** 選べる相手の枠線の色。選択中(primary)やMPゲージ(tertiary)と区別できるよう、固定の目立つ色にする。 */
private val SELECTABLE_BORDER_COLOR = Color(0xFFFFB300)

/** 先制行動を予約できるカードに表示する枠線の太さ。対象選びの点滅する太い枠線と見分けがつくよう細くする。 */
private val RESERVABLE_BORDER_WIDTH = 1.5.dp

/** 選べる相手の枠線が点滅するときの、最も薄いときの不透明度。 */
private const val SELECTABLE_BLINK_MIN_ALPHA = 0.35f

/** 選べる相手の枠線が薄い状態から濃い状態へ変わるまでの時間(ミリ秒)。 */
private const val SELECTABLE_BLINK_DURATION_MS = 700

/**
 * バトル画面で敵・味方1体分の状態(名前・HP・MP・TP・ブレイクゲージ)を表示するカード。
 *
 * 敵陣営・味方陣営の双方から共通で利用され、この画面における
 * 「四角いカード」という見た目を1箇所にまとめることを目的とする。
 *
 * カードの幅は並べ方によって異なるため、呼び出し側が[modifier]で指定する。
 * [onClick]を指定した場合はタップ可能なカードとなり、[isSelected]が`true`のときは
 * 選択中(手番中の個体)であることを示す枠線を表示する。[isSelectable]が`true`のとき(いま選べる相手)は、
 * それより太く目立つ色で点滅する枠線を表示し、[isSelected]の枠線よりも優先する。
 * [isReservable]が`true`のとき(いま先制行動を予約できる味方)は、[TacticalPointOrange]の細い枠線を表示する(対象選び・選択中の枠線がある間は出さない)。
 * [onClick]が`null`の場合はタップしても何も起きない。[BattleUnitUiModel.isDefeated]のときは半透明にしてグレーアウトし、
 * [onClick]があってもタップしても何も起きない。[onLongClick]は、戦闘不能や選べない状態に関わらず長押しで呼ばれる。
 *
 * @param unit 表示する敵またはキャラクターの状態。
 * @param modifier このComposableに適用する[Modifier]。
 * @param isSelected 選択中(手番中の個体)として強調表示するかどうか。
 * @param isSelectable 行動の対象として選べる相手として、点滅する太い枠線で強調表示するかどうか。
 * @param isReservable 先制行動を予約できるカードとして、細いオレンジの枠線で表示するかどうか。
 * @param onClick カードがタップされたときに呼ばれるコールバック。タップ不可にする場合は`null`。
 * @param onLongClick カードが長押しされたときに呼ばれるコールバック。長押しを受け付けない場合は`null`。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun UnitStatCard(
    unit: BattleUnitUiModel,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    isSelectable: Boolean = false,
    isReservable: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val border = when {
        isSelectable -> BorderStroke(SELECTABLE_BORDER_WIDTH, SELECTABLE_BORDER_COLOR.copy(alpha = rememberBlinkAlpha()))
        isSelected -> BorderStroke(SELECTED_BORDER_WIDTH, MaterialTheme.colorScheme.primary)
        isReservable -> BorderStroke(RESERVABLE_BORDER_WIDTH, TacticalPointOrange)
        else -> null
    }
    val cardModifier = if (unit.isDefeated) modifier.alpha(DEFEATED_ALPHA) else modifier
    val isTappable = onClick != null && !unit.isDefeated
    Card(
        modifier = cardModifier
            .clip(CardDefaults.shape)
            .combinedClickable(
                onClick = { if (isTappable) onClick?.invoke() },
                onLongClick = onLongClick,
            ),
        border = border,
    ) {
        UnitStatContent(unit)
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
 * [UnitStatCard]の中身となる、名前・HP・MP・ブレイクゲージと、右上のTP円ゲージの表示部分。
 *
 * ブレイクゲージは数値を表示せず黄色のバーだけで示し、ブレイク中・立ち上がり中は状態のラベルを添える。
 * かかっている状態異常は短いラベルを並べて表示する。
 * 先制行動を予約済み(または先制の手番待ち)の個体には、オレンジの「先制待ち」ラベルを添える。
 *
 * タップ可能なカードと表示専用のカードで同じ中身を共有するために切り出している。
 *
 * @param unit 表示する敵またはキャラクターの状態。
 */
@Composable
private fun UnitStatContent(unit: BattleUnitUiModel) {
    Box(modifier = Modifier.padding(8.dp)) {
        Column {
            // 右上のTPゲージと名前が重ならないよう、名前の行の右側にゲージの幅だけ余白を空ける。
            Text(
                unit.name,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = TP_GAUGE_SIZE + 4.dp),
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
            if (unit.ailments.isNotEmpty()) {
                Text(
                    text = unit.ailments.map { stringResource(it.shortLabelRes) }
                        .joinToString(stringResource(R.string.battle_ailment_label_separator)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (unit.isPreemptiveWaiting) {
                Text(
                    stringResource(R.string.battle_preemptive_waiting),
                    style = MaterialTheme.typography.labelSmall,
                    color = TacticalPointOrange,
                )
            }
        }
        TacticalPointGauge(unit = unit, modifier = Modifier.align(Alignment.TopEnd))
    }
}

/**
 * カードの右上に置く、TPの円ゲージ。円の進み具合がTP/最大TPで、真ん中に現在のTPの数値を表示する。
 *
 * @param unit 表示する敵またはキャラクターの状態。
 * @param modifier このComposableに適用する[Modifier]。
 */
@Composable
private fun TacticalPointGauge(unit: BattleUnitUiModel, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(TP_GAUGE_SIZE), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { unit.tp.toFloat() / unit.maxTp.coerceAtLeast(1) },
            modifier = Modifier.fillMaxSize(),
            color = TacticalPointOrange,
            strokeWidth = 3.dp,
        )
        Text(unit.tp.toString(), style = MaterialTheme.typography.labelSmall)
    }
}

/** Preview用に、カードを幅160dpで表示する。 */
@Composable
private fun UnitStatCardPreviewFrame(content: @Composable (Modifier) -> Unit) {
    MiKatsuTheme(darkTheme = false) { content(Modifier.width(160.dp).padding(8.dp)) }
}

/** 手番中(選択枠あり)の通常の味方カードを確認するためのPreview。 */
@Preview(showBackground = true, name = "カード(通常・手番中)")
@Composable
private fun UnitStatCardNormalPreview() {
    UnitStatCardPreviewFrame { UnitStatCard(unit = previewAlly(), modifier = it, isSelected = true) }
}

/** 行動の対象として選べる(点滅する太枠)状態のカードを確認するためのPreview。 */
@Preview(showBackground = true, name = "カード(選択可能)")
@Composable
private fun UnitStatCardSelectablePreview() {
    UnitStatCardPreviewFrame { UnitStatCard(unit = previewEnemy(), modifier = it, isSelectable = true, onClick = {}) }
}

/** 戦闘不能(グレーアウト)のカードを確認するためのPreview。 */
@Preview(showBackground = true, name = "カード(戦闘不能)")
@Composable
private fun UnitStatCardDefeatedPreview() {
    UnitStatCardPreviewFrame { UnitStatCard(unit = previewEnemy(hp = 0, isDefeated = true), modifier = it) }
}

/** ブレイク中(ラベルとゲージ満タン)のカードを確認するためのPreview。 */
@Preview(showBackground = true, name = "カード(ブレイク中)")
@Composable
private fun UnitStatCardBrokenPreview() {
    UnitStatCardPreviewFrame {
        UnitStatCard(unit = previewEnemy().copy(breakState = BreakState.BROKEN, breakGaugeFraction = 1f), modifier = it)
    }
}

/** TPがほぼ満タン(円ゲージが多い)のカードを確認するためのPreview。 */
@Preview(showBackground = true, name = "カード(TP多め)")
@Composable
private fun UnitStatCardHighTpPreview() {
    UnitStatCardPreviewFrame { UnitStatCard(unit = previewAlly().copy(tp = 95), modifier = it) }
}

/** 先制行動を予約できる(細いオレンジの枠)味方カードを確認するためのPreview。 */
@Preview(showBackground = true, name = "カード(先制予約できる)")
@Composable
private fun UnitStatCardReservablePreview() {
    UnitStatCardPreviewFrame {
        UnitStatCard(unit = previewAlly(isActive = false).copy(canReservePreemptive = true), modifier = it, isReservable = true, onClick = {})
    }
}

/** 先制行動を予約済み(「先制待ち」のラベル)の味方カードを確認するためのPreview。 */
@Preview(showBackground = true, name = "カード(先制待ち)")
@Composable
private fun UnitStatCardPreemptiveWaitingPreview() {
    UnitStatCardPreviewFrame { UnitStatCard(unit = previewAlly(isActive = false).copy(isPreemptiveWaiting = true), modifier = it) }
}

/** 状態異常が複数かかっている(短いラベルが並ぶ)味方カードを確認するためのPreview。 */
@Preview(showBackground = true, name = "カード(状態異常あり)")
@Composable
private fun UnitStatCardAilmentsPreview() {
    UnitStatCardPreviewFrame {
        UnitStatCard(
            unit = previewAlly().copy(ailments = listOf(StatusAilment.POISON, StatusAilment.PARALYSIS, StatusAilment.MP_SEAL)),
            modifier = it,
        )
    }
}

/** 行動不能の状態異常(睡眠)がかかっている敵カードを確認するためのPreview。 */
@Preview(showBackground = true, name = "カード(睡眠)")
@Composable
private fun UnitStatCardSleepPreview() {
    UnitStatCardPreviewFrame { UnitStatCard(unit = previewEnemy().copy(ailments = listOf(StatusAilment.SLEEP)), modifier = it) }
}
