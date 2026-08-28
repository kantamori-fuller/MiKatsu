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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import jp.co.fuller.morikan.bootcamp.mikatsu.data.CharacterRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterListScreen(
    repository: CharacterRepository,
    onBack: () -> Unit,
    onSelectCharacter: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var characters by remember { mutableStateOf(repository.getAll()) }
    var pendingDeleteId by remember { mutableStateOf<Int?>(null) }

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
            items(characters, key = { it.id }) { character ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectCharacter(character.id) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(character.name, modifier = Modifier.weight(1f))
                    IconButton(onClick = { pendingDeleteId = character.id }) {
                        Icon(Icons.Default.Delete, contentDescription = "削除")
                    }
                }
                HorizontalDivider()
            }
        }
    }

    val targetId = pendingDeleteId
    if (targetId != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("確認") },
            text = { Text("キャラを削除しますか？") },
            confirmButton = {
                TextButton(onClick = {
                    repository.delete(targetId)
                    characters = repository.getAll()
                    pendingDeleteId = null
                }) { Text("削除") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) { Text("キャンセル") }
            },
        )
    }
}
