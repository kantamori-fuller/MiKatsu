package jp.co.fuller.morikan.bootcamp.mikatsu.ui.partyformation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jp.co.fuller.morikan.bootcamp.mikatsu.core.theme.SelectionGreen
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.R

/**
 * 編成画面のキャラ一覧の1行を表すUIコンポーネント。
 *
 * 行を長押しするとキャラクターをドラッグでき、陣形の表のマスへドロップすることで
 * 編成に加えられる。選択アイコンは、そのキャラクターが既に陣形に配置済みかどうかを示す
 * 表示専用の要素である。
 *
 * @param characterId この行が表すキャラクターのID。ドラッグ時に受け渡す。
 * @param name 表示するキャラクターの名前。
 * @param isPlaced 陣形に配置済みかどうか。配置済みの場合、アイコンが緑色になる。
 * @param modifier このComposableに適用する[Modifier]。
 */
@Composable
internal fun PartyFormationListItem(
    characterId: Int,
    name: String,
    isPlaced: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .characterDragSource(characterId, rememberCharacterDragShadow(name))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, modifier = Modifier.weight(1f))
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = stringResource(R.string.content_description_select),
            tint = if (isPlaced) SelectionGreen else LocalContentColor.current,
        )
    }
}
