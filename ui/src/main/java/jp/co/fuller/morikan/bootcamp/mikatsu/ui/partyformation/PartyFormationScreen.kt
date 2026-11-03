package jp.co.fuller.morikan.bootcamp.mikatsu.ui.partyformation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jp.co.fuller.morikan.bootcamp.mikatsu.core.theme.MiKatsuTheme
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Character
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Formation
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.R
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.components.FormationGrid
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.partyformation.components.FormationSlot
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.partyformation.components.PartyFormationListItem
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.partyformation.model.PartyFormationUiState

/**
 * バトルに連れて行くキャラクターを選択する編成画面。
 *
 * [PartyFormationViewModel.uiState]の購読と、ViewModelのイベント関数をContentへ中継することだけを担い、
 * 描画は[PartyFormationScreenContent]に任せる(MVVMにおけるViewの責務のみを担う)。
 *
 * @param viewModel この画面に対応する[PartyFormationViewModel]。
 * @param onBack 画面上部の戻るボタンが押されたときに呼ばれるコールバック。
 * @param onConfirm 「決定」ボタンが押されたときに呼ばれるコールバック。
 *   編成した陣形を引数に、バトル画面への遷移をNavHost側に委ねる。
 * @param modifier このComposableに適用する[Modifier]。
 */
@Composable
fun PartyFormationScreen(
    viewModel: PartyFormationViewModel,
    onBack: () -> Unit,
    onConfirm: (Formation) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PartyFormationScreenContent(
        uiState = uiState,
        onBack = onBack,
        onConfirm = onConfirm,
        onDropToSlot = viewModel::onDropToSlot,
        onSlotClick = viewModel::onSlotClick,
        modifier = modifier,
    )
}

/**
 * 編成画面の描画本体。ViewModelに依存せず、[uiState]とコールバックだけで描画することで
 * Previewできるようにすることを目的とする。画面上部のキャラ一覧からキャラクターを長押しで
 * ドラッグし、その下の陣形の表(縦2マス×横3マス)のマスへドロップすることで編成する。
 * 「決定」ボタンは1体以上配置されている場合のみ押せる。
 *
 * @param uiState 描画する画面状態。
 * @param onBack 戻るボタンが押されたときに呼ばれるコールバック。
 * @param onConfirm 「決定」ボタンが押されたときに、編成した陣形を引数に呼ばれる。
 * @param onDropToSlot キャラクターがマスへドロップされたときに、キャラクターIDとスロット番号を引数に呼ばれる。
 * @param onSlotClick 配置済みのマスがタップされたときに、スロット番号を引数に呼ばれる。
 * @param modifier このComposableに適用する[Modifier]。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PartyFormationScreenContent(
    uiState: PartyFormationUiState,
    onBack: () -> Unit,
    onConfirm: (Formation) -> Unit,
    onDropToSlot: (characterId: Int, slot: Int) -> Unit,
    onSlotClick: (slot: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.party_formation_title)) },
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
                .fillMaxSize(),
        ) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(uiState.characters, key = { it.id }) { character ->
                    PartyFormationListItem(
                        characterId = character.id,
                        name = character.name,
                        isPlaced = character.id in uiState.formation.memberIds,
                    )
                    HorizontalDivider()
                }
            }
            HorizontalDivider()
            Text(
                stringResource(R.string.formation_hint),
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FormationGrid(
                slotContent = { slot, slotModifier ->
                    FormationSlot(
                        character = uiState.slotCharacters[slot],
                        onDrop = { characterId -> onDropToSlot(characterId, slot) },
                        onClick = { onSlotClick(slot) },
                        modifier = slotModifier,
                    )
                },
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp),
            )
            Button(
                onClick = { onConfirm(uiState.formation) },
                enabled = uiState.isConfirmEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(stringResource(R.string.action_confirm))
            }
        }
    }
}

/** Previewで表示するダミーのキャラクター。 */
private val previewCharacters = listOf("アリス", "ボブ", "キャロル").mapIndexed { index, name ->
    Character(
        id = index + 1, name = name, hp = 100, mana = 50, attack = 30, defense = 20,
        manaOutput = 10, manaResistance = 10, agility = 25, luck = 5,
    )
}

/** 誰も配置されていない状態(「決定」が押せない)を確認するためのPreview。 */
@Preview(showBackground = true, name = "編成(空)")
@Composable
private fun PartyFormationEmptyPreview() {
    MiKatsuTheme(darkTheme = false) {
        PartyFormationScreenContent(
            uiState = PartyFormationUiState(characters = previewCharacters),
            onBack = {}, onConfirm = {}, onDropToSlot = { _, _ -> }, onSlotClick = {},
        )
    }
}

/** 一部のキャラクターが配置済みの状態を確認するためのPreview。 */
@Preview(showBackground = true, name = "編成(一部配置済み)")
@Composable
private fun PartyFormationPlacedPreview() {
    MiKatsuTheme(darkTheme = false) {
        PartyFormationScreenContent(
            uiState = PartyFormationUiState(
                characters = previewCharacters,
                formation = Formation.EMPTY.place(1, 0).place(2, 4),
            ),
            onBack = {}, onConfirm = {}, onDropToSlot = { _, _ -> }, onSlotClick = {},
        )
    }
}
