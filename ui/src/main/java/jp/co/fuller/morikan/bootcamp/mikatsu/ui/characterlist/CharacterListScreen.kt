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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jp.co.fuller.morikan.bootcamp.mikatsu.core.theme.MiKatsuTheme
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Character
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.R
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.characterlist.components.CharacterListItem
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.characterlist.model.CharacterListUiState

/**
 * 端末内に保存されている全キャラクターを名前で一覧表示する画面。
 *
 * [CharacterListViewModel.uiState]の購読と、ViewModelのイベント関数をContentへ中継することだけを担い、
 * 描画は[CharacterListScreenContent]に任せる(MVVMにおけるViewの責務のみを担う)。
 *
 * @param viewModel この画面に対応する[CharacterListViewModel]。
 * @param onBack 画面上部の戻るボタンが押されたときに呼ばれるコールバック。
 * @param onSelectCharacter 一覧アイテムがタップされたときに呼ばれるコールバック。
 *   タップされたキャラクターのIDを引数に、編集画面への遷移をNavHost側に委ねる。
 * @param modifier このComposableに適用する[Modifier]。
 */
@Composable
fun CharacterListScreen(
    viewModel: CharacterListViewModel,
    onBack: () -> Unit,
    onSelectCharacter: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CharacterListScreenContent(
        uiState = uiState,
        onBack = onBack,
        onSelectCharacter = onSelectCharacter,
        onDeleteRequested = viewModel::onDeleteRequested,
        onDeleteConfirmed = viewModel::onDeleteConfirmed,
        onDeleteCancelled = viewModel::onDeleteCancelled,
        modifier = modifier,
    )
}

/**
 * キャラ一覧画面の描画本体。ViewModelに依存せず、[uiState]とコールバックだけで描画することで
 * Previewできるようにすることを目的とする。一覧アイテムをタップすると編集画面へ、
 * ゴミ箱アイコンをタップすると削除確認ダイアログを表示する。
 *
 * @param uiState 描画する画面状態。
 * @param onBack 戻るボタンが押されたときに呼ばれるコールバック。
 * @param onSelectCharacter 一覧アイテムがタップされたときに、キャラクターIDを引数に呼ばれる。
 * @param onDeleteRequested ゴミ箱アイコンが押されたときに、キャラクターIDを引数に呼ばれる。
 * @param onDeleteConfirmed 削除確認ダイアログで削除が確定されたときに呼ばれる。
 * @param onDeleteCancelled 削除確認ダイアログがキャンセルされたときに呼ばれる。
 * @param modifier このComposableに適用する[Modifier]。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CharacterListScreenContent(
    uiState: CharacterListUiState,
    onBack: () -> Unit,
    onSelectCharacter: (Int) -> Unit,
    onDeleteRequested: (Int) -> Unit,
    onDeleteConfirmed: () -> Unit,
    onDeleteCancelled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.character_list_title)) },
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
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            items(uiState.characters, key = { it.id }) { character ->
                CharacterListItem(
                    name = character.name,
                    onClick = { onSelectCharacter(character.id) },
                    onDeleteClick = { onDeleteRequested(character.id) },
                )
                HorizontalDivider()
            }
        }
    }

    val targetId = uiState.pendingDeleteId
    if (targetId != null) {
        AlertDialog(
            onDismissRequest = onDeleteCancelled,
            title = { Text(stringResource(R.string.delete_confirmation_title)) },
            text = { Text(stringResource(R.string.delete_confirmation_message)) },
            confirmButton = {
                TextButton(onClick = onDeleteConfirmed) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = onDeleteCancelled) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

/** Previewで表示するダミーのキャラクター。 */
private val previewCharacters = listOf("アリス", "ボブ", "キャロル").mapIndexed { index, name ->
    Character(
        id = index + 1, name = name, hp = 100, mana = 50, attack = 30, defense = 20,
        manaOutput = 10, manaResistance = 10, agility = 25, luck = 5,
    )
}

/** キャラクターが1人もいない状態を確認するためのPreview。 */
@Preview(showBackground = true, name = "キャラ一覧(空)")
@Composable
private fun CharacterListEmptyPreview() {
    MiKatsuTheme(darkTheme = false) {
        CharacterListScreenContent(
            uiState = CharacterListUiState(),
            onBack = {}, onSelectCharacter = {}, onDeleteRequested = {}, onDeleteConfirmed = {}, onDeleteCancelled = {},
        )
    }
}

/** キャラクターが複数いる状態を確認するためのPreview。 */
@Preview(showBackground = true, name = "キャラ一覧(あり)")
@Composable
private fun CharacterListPreview() {
    MiKatsuTheme(darkTheme = false) {
        CharacterListScreenContent(
            uiState = CharacterListUiState(characters = previewCharacters),
            onBack = {}, onSelectCharacter = {}, onDeleteRequested = {}, onDeleteConfirmed = {}, onDeleteCancelled = {},
        )
    }
}

/** 削除確認ダイアログを表示している状態を確認するためのPreview。 */
@Preview(showBackground = true, name = "キャラ一覧(削除確認)")
@Composable
private fun CharacterListDeleteDialogPreview() {
    MiKatsuTheme(darkTheme = false) {
        CharacterListScreenContent(
            uiState = CharacterListUiState(characters = previewCharacters, pendingDeleteId = 2),
            onBack = {}, onSelectCharacter = {}, onDeleteRequested = {}, onDeleteConfirmed = {}, onDeleteCancelled = {},
        )
    }
}
