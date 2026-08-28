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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.R
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.partyformation.components.PartyFormationListItem

/**
 * バトルに連れて行くキャラクターを選択する編成画面。
 *
 * [PartyFormationViewModel.uiState]を購読して描画するのみに徹し、選択状態の管理は
 * 一切持たない(MVVMにおけるViewの責務のみを担う)。「決定」ボタンは画面下部、
 * かつ一覧の下に常に表示され、1体以上選択されている場合のみ押せるようになる。
 *
 * @param viewModel この画面に対応する[PartyFormationViewModel]。
 * @param onBack 画面上部の戻るボタンが押されたときに呼ばれるコールバック。
 * @param onConfirm 「決定」ボタンが押されたときに呼ばれるコールバック。
 *   選択済みキャラクターIDの一覧を引数に、バトル画面への遷移をNavHost側に委ねる。
 * @param modifier このComposableに適用する[Modifier]。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartyFormationScreen(
    viewModel: PartyFormationViewModel,
    onBack: () -> Unit,
    onConfirm: (List<Int>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
                        name = character.name,
                        isSelected = character.id in uiState.selectedIds,
                        onSelect = { viewModel.onSelect(character.id) },
                        onDeselect = { viewModel.onDeselect(character.id) },
                    )
                    HorizontalDivider()
                }
            }
            Button(
                onClick = { onConfirm(uiState.selectedIds) },
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
