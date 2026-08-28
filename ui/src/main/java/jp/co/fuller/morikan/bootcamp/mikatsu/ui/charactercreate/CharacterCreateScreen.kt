package jp.co.fuller.morikan.bootcamp.mikatsu.ui.charactercreate

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
                title = { Text(if (uiState.isEditing) "キャラを編集" else "キャラを作成") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("名前") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            StatField(label = "H. 体力", value = uiState.hp, onValueChange = viewModel::onHpChange)
            StatField(label = "M. マナ総量", value = uiState.mana, onValueChange = viewModel::onManaChange)
            StatField(label = "A. 破壊力", value = uiState.attack, onValueChange = viewModel::onAttackChange)
            StatField(label = "B. 耐久力", value = uiState.defense, onValueChange = viewModel::onDefenseChange)
            StatField(label = "C. マナ出力", value = uiState.manaOutput, onValueChange = viewModel::onManaOutputChange)
            StatField(label = "D. マナ耐性", value = uiState.manaResistance, onValueChange = viewModel::onManaResistanceChange)
            StatField(label = "S. 敏捷", value = uiState.agility, onValueChange = viewModel::onAgilityChange)
            StatField(label = "L. 幸運", value = uiState.luck, onValueChange = viewModel::onLuckChange)
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = viewModel::onSaveClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("保存")
            }
        }
    }
}
