package jp.co.fuller.morikan.bootcamp.mikatsu.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.viewmodel.CharacterListViewModel

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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectCharacter(character.id) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(character.name, modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.onDeleteRequested(character.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "削除")
                    }
                }
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
