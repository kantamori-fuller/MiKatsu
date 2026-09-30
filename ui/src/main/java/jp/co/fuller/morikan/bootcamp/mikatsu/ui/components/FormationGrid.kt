package jp.co.fuller.morikan.bootcamp.mikatsu.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Formation

/** 陣形の1マスの角丸形状。マスの枠線とクリップの双方で同じ形状を用いる。 */
private val SlotShape = RoundedCornerShape(8.dp)

/**
 * 陣形([Formation])のマス目を、縦[Formation.ROWS]マス×横[Formation.COLUMNS]マスの表として
 * 並べるレイアウト。
 *
 * 編成画面(配置の編集)とバトル画面(配置の表示)で同じ形の表を用いるため、
 * マス目の並べ方をこの1箇所にまとめることを目的とする。各マスの中身は呼び出し側が
 * [slotContent]で描画する。同じ行のマスは、最も背の高いマスに高さが揃う。
 *
 * @param slotContent スロット番号ごとのマスを描画するComposable。第2引数の[Modifier]には
 *   マスの幅・高さを表に合わせるための指定が入っているため、マスのルート要素に必ず適用する。
 * @param modifier このComposableに適用する[Modifier]。
 */
@Composable
internal fun FormationGrid(
    slotContent: @Composable (slot: Int, modifier: Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(Formation.ROWS) { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(Formation.COLUMNS) { column ->
                    slotContent(
                        row * Formation.COLUMNS + column,
                        Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            }
        }
    }
}

/**
 * 陣形の1マス分の枠。
 *
 * 空きマスや、編成画面で配置したキャラクターを表示するマスの見た目(枠線・最小の高さ)を
 * 統一することを目的とする。
 *
 * @param modifier このComposableに適用する[Modifier]。
 * @param highlighted 枠を強調表示するかどうか。ドラッグ中のキャラクターがこのマスの上に
 *   あるときなど、ドロップ先であることを示すために用いる。
 * @param content 枠の中央に表示する内容。
 */
@Composable
internal fun FormationSlotFrame(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 72.dp)
            .clip(SlotShape)
            .border(
                width = if (highlighted) 2.dp else 1.dp,
                color = if (highlighted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                shape = SlotShape,
            ),
        contentAlignment = Alignment.Center,
        content = content,
    )
}
