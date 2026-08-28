package jp.co.fuller.morikan.bootcamp.mikatsu.ui.partyformation.components

import androidx.compose.foundation.clickable
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
 * 行のどこをタップしても選択状態が反転する。選択アイコンはあくまで現在の選択状態を
 * 示す表示専用の要素であり、単独のタップ対象ではない。
 *
 * @param name 表示するキャラクターの名前。
 * @param isSelected 選択済みかどうか。選択済みの場合、アイコンが緑色になる。
 * @param onToggleSelected 行がタップされたときに呼ばれるコールバック。
 * @param modifier このComposableに適用する[Modifier]。
 */
@Composable
internal fun PartyFormationListItem(
    name: String,
    isSelected: Boolean,
    onToggleSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleSelected)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, modifier = Modifier.weight(1f))
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = stringResource(R.string.content_description_select),
            tint = if (isSelected) SelectionGreen else LocalContentColor.current,
        )
    }
}
