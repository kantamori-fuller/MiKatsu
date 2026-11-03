package jp.co.fuller.morikan.bootcamp.mikatsu.ui.charactercreate.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import jp.co.fuller.morikan.bootcamp.mikatsu.core.theme.MiKatsuTheme
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.R
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.charactercreate.model.SkillUiModel

/**
 * キャラ作成/編集画面で、設定済みのスキルの一覧と「スキルを追加」操作を表示する欄。
 *
 * Aスキル・Pスキルの両方で同じ見た目を共有するために切り出している。上限に達した場合は
 * 「スキルを追加」を押せなくする。表示のみを担い、追加・削除の判断はViewModelが行う。
 *
 * @param title 欄の見出し(「Aスキル」「Pスキル」)。
 * @param skills 設定済みのスキル。
 * @param maxCount 設定できるスキルの最大数。
 * @param canAdd スキルを追加できるかどうか。
 * @param onAddClick 「スキルを追加」が押されたときに呼ばれるコールバック。
 * @param onRemoveClick 設定済みスキルの「削除」が押されたときに呼ばれるコールバック(引数はスキルID)。
 * @param modifier このComposableに適用する[Modifier]。
 */
@Composable
internal fun SkillSection(
    title: String,
    skills: List<SkillUiModel>,
    maxCount: Int,
    canAdd: Boolean,
    onAddClick: () -> Unit,
    onRemoveClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.skill_section_title_format, title, skills.size, maxCount),
            style = MaterialTheme.typography.titleMedium,
        )
        if (skills.isEmpty()) {
            Text(text = stringResource(R.string.skill_none_set), style = MaterialTheme.typography.bodyMedium)
        }
        skills.forEach { skill ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = skill.name, style = MaterialTheme.typography.bodyLarge)
                TextButton(onClick = { onRemoveClick(skill.id) }) {
                    Text(stringResource(R.string.action_delete))
                }
            }
        }
        OutlinedButton(onClick = onAddClick, enabled = canAdd, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_add_skill))
        }
    }
}

/** スキルが未設定の状態を確認するためのPreview。 */
@Preview(showBackground = true, name = "スキル欄(未設定)")
@Composable
private fun SkillSectionEmptyPreview() {
    MiKatsuTheme(darkTheme = false) {
        SkillSection(
            title = "Aスキル", skills = emptyList(), maxCount = 8, canAdd = true,
            onAddClick = {}, onRemoveClick = {},
        )
    }
}

/** スキルが設定済みで、上限に達して追加できない状態を確認するためのPreview。 */
@Preview(showBackground = true, name = "スキル欄(上限)")
@Composable
private fun SkillSectionFullPreview() {
    MiKatsuTheme(darkTheme = false) {
        SkillSection(
            title = "Pスキル",
            skills = listOf(SkillUiModel(1, "鋼の体"), SkillUiModel(2, "ディア")),
            maxCount = 2, canAdd = false, onAddClick = {}, onRemoveClick = {},
        )
    }
}
