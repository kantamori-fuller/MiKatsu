package jp.co.fuller.morikan.bootcamp.mikatsu.ui.characterlist.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * キャラ一覧の1行を表すUIコンポーネント。
 *
 * 名前の表示、行タップによる選択、ゴミ箱アイコンタップによる削除要求という
 * この画面固有の1行分の見た目と操作をまとめて提供することを目的とする。
 *
 * @param name 表示するキャラクターの名前。
 * @param onClick 行がタップされたときに呼ばれるコールバック。
 * @param onDeleteClick ゴミ箱アイコンがタップされたときに呼ばれるコールバック。
 * @param modifier このComposableに適用する[Modifier]。
 */
@Composable
internal fun CharacterListItem(
    name: String,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, modifier = Modifier.weight(1f))
        IconButton(onClick = onDeleteClick) {
            Icon(Icons.Default.Delete, contentDescription = "削除")
        }
    }
}
