package jp.co.fuller.morikan.bootcamp.mikatsu.ui.characterlist

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.characterlist.components.CharacterListItem

/**
 * 端末内に保存されている全キャラクターを名前で一覧表示する画面。
 *
 * [CharacterListViewModel.uiState]を購読して描画するのみに徹し、削除確認や実際の削除処理は
 * 一切持たない(MVVMにおけるViewの責務のみを担う)。一覧アイテムをタップすると編集画面へ、
 * ゴミ箱アイコンをタップすると削除確認ダイアログを表示する。
 *
 * @param viewModel この画面に対応する[CharacterListViewModel]。
 * @param onBack 画面上部の戻るボタンが押されたときに呼ばれるコールバック。
 * @param onSelectCharacter 一覧アイテムがタップされたときに呼ばれるコールバック。
 *   タップされたキャラクターのIDを引数に、編集画面への遷移をNavHost側に委ねる。
 * @param modifier このComposableに適用する[Modifier]。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterListScreen(
    viewModel: CharacterListViewModel,
    onBack: () -> Unit,
    onSelectCharacter: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("キャラ一覧") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            items(uiState.characters, key = { it.id }) { character ->
                CharacterListItem(
                    name = character.name,
                    onClick = { onSelectCharacter(character.id) },
                    onDeleteClick = { viewModel.onDeleteRequested(character.id) },
                )
                HorizontalDivider()
            }
        }
    }

    val targetId = uiState.pendingDeleteId
    if (targetId != null) {
        AlertDialog(
            onDismissRequest = viewModel::onDeleteCancelled,
            title = { Text("確認") },
            text = { Text("キャラを削除しますか？") },
            confirmButton = {
                TextButton(onClick = viewModel::onDeleteConfirmed) { Text("削除") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDeleteCancelled) { Text("キャンセル") }
            },
        )
    }
}
