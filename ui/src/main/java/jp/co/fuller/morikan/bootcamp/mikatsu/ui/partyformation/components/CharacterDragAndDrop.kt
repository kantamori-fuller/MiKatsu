package jp.co.fuller.morikan.bootcamp.mikatsu.ui.partyformation.components

import android.content.ClipData
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp

/** ドラッグ中のデータを表す[ClipData]のラベル。画面には表示されない識別用の値。 */
private const val CHARACTER_CLIP_LABEL = "mikatsu_character"

/**
 * 要素を長押しすると、指定したキャラクターをドラッグできるようにする[Modifier]。
 *
 * キャラ一覧の行と陣形のマスの双方から同じ形式でキャラクターをドラッグさせることで、
 * ドロップ先(陣形のマス)がドラッグ元を区別せずに扱えるようにすることを目的とする。
 * キャラクターIDはアプリ内のドラッグに限り参照できる`localState`に載せて受け渡す。
 *
 * ドラッグ中の影には[drawDragShadow]を用いる。標準の影(要素の見た目の複製)は要素の描画内容を
 * 一度だけ記録して使い回すため、配置状態に応じた見た目の変化が画面に反映されなくなってしまう。
 * それを避けるために、影は別途描画する。
 *
 * @param characterId ドラッグさせるキャラクターのID。
 * @param drawDragShadow ドラッグ中の影を描画する処理。[rememberCharacterDragShadow]で生成する。
 * @return ドラッグ元としての振る舞いを追加した[Modifier]。
 */
internal fun Modifier.characterDragSource(
    characterId: Int,
    drawDragShadow: DrawScope.() -> Unit,
): Modifier =
    dragAndDropSource(drawDragDecoration = drawDragShadow) { _ ->
        DragAndDropTransferData(
            clipData = ClipData.newPlainText(CHARACTER_CLIP_LABEL, characterId.toString()),
            localState = characterId,
        )
    }

/**
 * 陣形のマスにおいて、タップ(配置解除)と長押しによるドラッグ開始の双方を1つの
 * ジェスチャー検出にまとめた[Modifier]。
 *
 * [Modifier.clickable]と[characterDragSource]を単純に併用すると、両者が別々に
 * ポインタイベントを検出しようとするため、内側のドラッグ検出器が押下イベントを
 * 消費してしまい、外側のタップ検出が押下イベントを受け取れずタップが機能しなくなる
 * (Composeのポインタイベント処理の制約による)。これを避けるため、1つの検出器の中で
 * タップと長押しを判定し、タップは[onClick]、長押しはドラッグ開始として扱う。
 *
 * @param characterId ドラッグさせるキャラクターのID。
 * @param onClick タップされたときに呼ばれるコールバック。
 * @param drawDragShadow ドラッグ中の影を描画する処理。[rememberCharacterDragShadow]で生成する。
 * @return タップとドラッグ元としての振る舞いを追加した[Modifier]。
 */
@OptIn(ExperimentalFoundationApi::class)
@Suppress("DEPRECATION")
@Composable
internal fun Modifier.characterDragSourceWithClick(
    characterId: Int,
    onClick: () -> Unit,
    drawDragShadow: DrawScope.() -> Unit,
): Modifier {
    val currentCharacterId by rememberUpdatedState(characterId)
    val currentOnClick by rememberUpdatedState(onClick)
    return dragAndDropSource(
        drawDragDecoration = drawDragShadow,
        block = {
            detectTapGestures(
                onTap = { currentOnClick() },
                onLongPress = {
                    startTransfer(
                        DragAndDropTransferData(
                            clipData = ClipData.newPlainText(CHARACTER_CLIP_LABEL, currentCharacterId.toString()),
                            localState = currentCharacterId,
                        ),
                    )
                },
            )
        },
    )
}

/**
 * キャラクターをドラッグしている間に指の下へ表示する影(角丸の札にキャラクター名を載せたもの)の
 * 描画処理を生成する。
 *
 * 影の大きさはドラッグ元の要素の大きさに合わせられる。
 *
 * @param name 影に表示するキャラクターの名前。
 * @return [characterDragSource]へ渡す、影の描画処理。
 */
@Composable
internal fun rememberCharacterDragShadow(name: String): DrawScope.() -> Unit {
    val textMeasurer = rememberTextMeasurer()
    val containerColor = MaterialTheme.colorScheme.primaryContainer
    val textStyle = MaterialTheme.typography.titleMedium.copy(
        color = MaterialTheme.colorScheme.onPrimaryContainer,
    )
    return {
        val horizontalPadding = 16.dp.toPx()
        drawRoundRect(color = containerColor, cornerRadius = CornerRadius(8.dp.toPx()))
        val layout = textMeasurer.measure(
            text = name,
            style = textStyle,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            constraints = Constraints(
                maxWidth = (size.width - horizontalPadding * 2).toInt().coerceAtLeast(0),
            ),
        )
        drawText(
            textLayoutResult = layout,
            topLeft = Offset(horizontalPadding, (size.height - layout.size.height) / 2),
        )
    }
}

/**
 * ドラッグ&ドロップのイベントから、[characterDragSource]で渡されたキャラクターIDを取り出す。
 *
 * @receiver ドロップ先が受け取ったドラッグ&ドロップのイベント。
 * @return ドラッグされているキャラクターのID。キャラクター以外のドラッグの場合は`null`。
 */
internal fun DragAndDropEvent.draggedCharacterId(): Int? =
    toAndroidDragEvent().localState as? Int
