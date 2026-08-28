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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.R
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.charactercreate.components.StatField

/**
 * キャラクターの名前と8つのステータスを入力し、保存するための画面。
 *
 * [CharacterCreateViewModel.uiState]を購読して描画するのみに徹し、入力検証や保存処理は
 * 一切持たない(MVVMにおけるViewの責務のみを担う)。編集対象のキャラクターIDが指定されている
 * 場合は、[viewModel]側で既存データが復元された状態で表示される。
 *
 * @param viewModel この画面に対応する[CharacterCreateViewModel]。
 * @param onBack 画面上部の戻るボタンが押されたときに呼ばれるコールバック。
 * @param onSaved 保存が完了し、前の画面へ戻るべきタイミングで呼ばれるコールバック。
 * @param modifier このComposableに適用する[Modifier]。
 */
@OptIn(ExperimentalMaterial3Api::class)
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
                onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(R.string.label_name)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            StatField(label = stringResource(R.string.label_hp), value = uiState.hp, onValueChange = viewModel::onHpChange)
            StatField(label = stringResource(R.string.label_mana), value = uiState.mana, onValueChange = viewModel::onManaChange)
            StatField(label = stringResource(R.string.label_attack), value = uiState.attack, onValueChange = viewModel::onAttackChange)
            StatField(label = stringResource(R.string.label_defense), value = uiState.defense, onValueChange = viewModel::onDefenseChange)
            StatField(
                label = stringResource(R.string.label_mana_output),
                value = uiState.manaOutput,
                onValueChange = viewModel::onManaOutputChange,
            )
            StatField(
                label = stringResource(R.string.label_mana_resistance),
                value = uiState.manaResistance,
                onValueChange = viewModel::onManaResistanceChange,
            )
            StatField(label = stringResource(R.string.label_agility), value = uiState.agility, onValueChange = viewModel::onAgilityChange)
            StatField(label = stringResource(R.string.label_luck), value = uiState.luck, onValueChange = viewModel::onLuckChange)
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = viewModel::onSaveClick,
                enabled = uiState.isSaveEnabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}
