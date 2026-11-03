package jp.co.fuller.morikan.bootcamp.mikatsu.ui.charactercreate

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jp.co.fuller.morikan.bootcamp.mikatsu.core.theme.MiKatsuTheme
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.R
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillLimits
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.charactercreate.components.SkillSection
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.charactercreate.components.StatField
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.charactercreate.model.CharacterCreateUiState
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.charactercreate.model.SkillPickerKind
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.charactercreate.model.SkillUiModel

/**
 * キャラクターの名前と8つのステータス、Aスキル・Pスキルを設定し、保存するための画面。
 *
 * [CharacterCreateViewModel.uiState]の購読、保存完了時の[onSaved]呼び出し(一度だけのイベント)、
 * ViewModelのイベント関数をContentへ中継することだけを担い、描画は[CharacterCreateScreenContent]に任せる。
 * 編集対象のキャラクターIDが指定されている場合は、[viewModel]側で既存データが復元された状態で表示される。
 *
 * @param viewModel この画面に対応する[CharacterCreateViewModel]。
 * @param onBack 画面上部の戻るボタンが押されたときに呼ばれるコールバック。
 * @param onSaved 保存が完了し、前の画面へ戻るべきタイミングで呼ばれるコールバック。
 * @param modifier このComposableに適用する[Modifier]。
 */
@Composable
fun CharacterCreateScreen(
    viewModel: CharacterCreateViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onSaved()
    }

    CharacterCreateScreenContent(
        uiState = uiState,
        onBack = onBack,
        onNameChange = viewModel::onNameChange,
        onHpChange = viewModel::onHpChange,
        onManaChange = viewModel::onManaChange,
        onAttackChange = viewModel::onAttackChange,
        onDefenseChange = viewModel::onDefenseChange,
        onManaOutputChange = viewModel::onManaOutputChange,
        onManaResistanceChange = viewModel::onManaResistanceChange,
        onAgilityChange = viewModel::onAgilityChange,
        onLuckChange = viewModel::onLuckChange,
        onAddActiveSkillClick = viewModel::onAddActiveSkillClick,
        onAddPassiveSkillClick = viewModel::onAddPassiveSkillClick,
        onRemoveActiveSkill = viewModel::onRemoveActiveSkill,
        onRemovePassiveSkill = viewModel::onRemovePassiveSkill,
        onSkillPicked = viewModel::onSkillPicked,
        onSkillPickerDismiss = viewModel::onSkillPickerDismiss,
        onSaveClick = viewModel::onSaveClick,
        modifier = modifier,
    )
}

/**
 * キャラ作成/編集画面の描画本体。ViewModelに依存せず、[uiState]とコールバックだけで描画することで
 * Previewできるようにすることを目的とする。入力検証や保存処理は一切持たない。
 *
 * @param uiState 描画する画面状態。
 * @param onBack 戻るボタンが押されたときに呼ばれるコールバック。
 * @param onNameChange 名前の入力が変化したときに呼ばれる。
 * @param onHpChange 体力の入力が変化したときに呼ばれる。
 * @param onManaChange マナ総量の入力が変化したときに呼ばれる。
 * @param onAttackChange 破壊力の入力が変化したときに呼ばれる。
 * @param onDefenseChange 耐久力の入力が変化したときに呼ばれる。
 * @param onManaOutputChange マナ出力の入力が変化したときに呼ばれる。
 * @param onManaResistanceChange マナ耐性の入力が変化したときに呼ばれる。
 * @param onAgilityChange 敏捷の入力が変化したときに呼ばれる。
 * @param onLuckChange 幸運の入力が変化したときに呼ばれる。
 * @param onAddActiveSkillClick Aスキルの「スキルを追加」が押されたときに呼ばれる。
 * @param onAddPassiveSkillClick Pスキルの「スキルを追加」が押されたときに呼ばれる。
 * @param onRemoveActiveSkill 設定済みAスキルの「削除」が押されたときに、スキルIDを引数に呼ばれる。
 * @param onRemovePassiveSkill 設定済みPスキルの「削除」が押されたときに、スキルIDを引数に呼ばれる。
 * @param onSkillPicked スキル追加ダイアログでスキルが選ばれたときに、スキルIDを引数に呼ばれる。
 * @param onSkillPickerDismiss スキル追加ダイアログが閉じられたときに呼ばれる。
 * @param onSaveClick 「保存」が押されたときに呼ばれる。
 * @param modifier このComposableに適用する[Modifier]。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CharacterCreateScreenContent(
    uiState: CharacterCreateUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onHpChange: (String) -> Unit,
    onManaChange: (String) -> Unit,
    onAttackChange: (String) -> Unit,
    onDefenseChange: (String) -> Unit,
    onManaOutputChange: (String) -> Unit,
    onManaResistanceChange: (String) -> Unit,
    onAgilityChange: (String) -> Unit,
    onLuckChange: (String) -> Unit,
    onAddActiveSkillClick: () -> Unit,
    onAddPassiveSkillClick: () -> Unit,
    onRemoveActiveSkill: (Int) -> Unit,
    onRemovePassiveSkill: (Int) -> Unit,
    onSkillPicked: (Int) -> Unit,
    onSkillPickerDismiss: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    uiState.skillPicker?.let { kind ->
        SkillPickerDialog(
            title = stringResource(
                if (kind == SkillPickerKind.ACTIVE) R.string.skill_picker_title_active else R.string.skill_picker_title_passive,
            ),
            candidates = uiState.skillPickerCandidates,
            onPick = onSkillPicked,
            onDismiss = onSkillPickerDismiss,
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (uiState.isEditing) R.string.character_edit_title else R.string.character_create_title,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_description_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState()),
        ) {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.label_name)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            StatField(label = stringResource(R.string.label_hp), value = uiState.hp, onValueChange = onHpChange)
            StatField(label = stringResource(R.string.label_mana), value = uiState.mana, onValueChange = onManaChange)
            StatField(label = stringResource(R.string.label_attack), value = uiState.attack, onValueChange = onAttackChange)
            StatField(label = stringResource(R.string.label_defense), value = uiState.defense, onValueChange = onDefenseChange)
            StatField(
                label = stringResource(R.string.label_mana_output),
                value = uiState.manaOutput,
                onValueChange = onManaOutputChange,
            )
            StatField(
                label = stringResource(R.string.label_mana_resistance),
                value = uiState.manaResistance,
                onValueChange = onManaResistanceChange,
            )
            StatField(label = stringResource(R.string.label_agility), value = uiState.agility, onValueChange = onAgilityChange)
            StatField(label = stringResource(R.string.label_luck), value = uiState.luck, onValueChange = onLuckChange)
            Spacer(Modifier.height(16.dp))
            SkillSection(
                title = stringResource(R.string.label_active_skills),
                skills = uiState.activeSkills,
                maxCount = SkillLimits.MAX_ACTIVE_SKILLS,
                canAdd = uiState.canAddActiveSkill,
                onAddClick = onAddActiveSkillClick,
                onRemoveClick = onRemoveActiveSkill,
            )
            Spacer(Modifier.height(16.dp))
            SkillSection(
                title = stringResource(R.string.label_passive_skills),
                skills = uiState.passiveSkills,
                maxCount = SkillLimits.MAX_PASSIVE_SKILLS,
                canAdd = uiState.canAddPassiveSkill,
                onAddClick = onAddPassiveSkillClick,
                onRemoveClick = onRemovePassiveSkill,
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onSaveClick,
                enabled = uiState.isSaveEnabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}

/**
 * まだ設定していないスキルの中から、追加するスキルを選ぶダイアログ。
 *
 * 追加できるスキルがない場合はその旨を表示する。選択肢の決定はViewModelが行い、ここでは描画のみを担う。
 *
 * @param title ダイアログの見出し。
 * @param candidates 選べるスキル(未設定のもの)。
 * @param onPick スキルが選ばれたときに呼ばれるコールバック(引数はスキルID)。
 * @param onDismiss ダイアログが閉じられたとき(キャンセル・外側のタップ)に呼ばれるコールバック。
 */
@Composable
private fun SkillPickerDialog(
    title: String,
    candidates: List<SkillUiModel>,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                if (candidates.isEmpty()) {
                    Text(stringResource(R.string.skill_picker_empty))
                }
                candidates.forEach { skill ->
                    TextButton(onClick = { onPick(skill.id) }, modifier = Modifier.fillMaxWidth()) {
                        Text(skill.name)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/** Previewで使うダミーのAスキルのマスターデータ。 */
private val previewActiveCatalog = listOf(SkillUiModel(2, "ディア"), SkillUiModel(3, "精神一到"), SkillUiModel(4, "覇気"))

/** Previewで使うダミーのPスキルのマスターデータ。 */
private val previewPassiveCatalog = listOf(SkillUiModel(1, "鋼の体"))

/** Previewで使う、既存キャラクターを編集している状態のUiState。 */
private val previewEditingState = CharacterCreateUiState(
    isEditing = true,
    name = "アリス",
    hp = "120",
    mana = "50",
    attack = "30",
    defense = "20",
    manaOutput = "10",
    manaResistance = "10",
    agility = "25",
    luck = "5",
    activeSkillIds = listOf(2),
    passiveSkillIds = listOf(1),
    activeSkillCatalog = previewActiveCatalog,
    passiveSkillCatalog = previewPassiveCatalog,
    isSkillCatalogLoaded = true,
)

/** Preview用に、すべてのイベントを無視する[CharacterCreateScreenContent]を表示する。 */
@Composable
private fun CharacterCreateContentPreview(uiState: CharacterCreateUiState) {
    MiKatsuTheme(darkTheme = false) {
        CharacterCreateScreenContent(
            uiState = uiState,
            onBack = {}, onNameChange = {}, onHpChange = {}, onManaChange = {}, onAttackChange = {},
            onDefenseChange = {}, onManaOutputChange = {}, onManaResistanceChange = {},
            onAgilityChange = {}, onLuckChange = {}, onAddActiveSkillClick = {},
            onAddPassiveSkillClick = {}, onRemoveActiveSkill = {}, onRemovePassiveSkill = {},
            onSkillPicked = {}, onSkillPickerDismiss = {}, onSaveClick = {},
        )
    }
}

/** 新規作成(未入力・保存不可)の状態を確認するためのPreview。 */
@Preview(showBackground = true, name = "キャラ作成(新規)")
@Composable
private fun CharacterCreateNewPreview() {
    CharacterCreateContentPreview(
        CharacterCreateUiState(
            activeSkillCatalog = previewActiveCatalog,
            passiveSkillCatalog = previewPassiveCatalog,
            isSkillCatalogLoaded = true,
        ),
    )
}

/** 既存キャラクターの編集中の状態を確認するためのPreview。 */
@Preview(showBackground = true, name = "キャラ作成(編集中)")
@Composable
private fun CharacterCreateEditingPreview() {
    CharacterCreateContentPreview(previewEditingState)
}

/** Aスキルの追加ダイアログを開いた状態を確認するためのPreview。 */
@Preview(showBackground = true, name = "キャラ作成(スキルピッカー)")
@Composable
private fun CharacterCreateSkillPickerPreview() {
    CharacterCreateContentPreview(previewEditingState.copy(skillPicker = SkillPickerKind.ACTIVE))
}
