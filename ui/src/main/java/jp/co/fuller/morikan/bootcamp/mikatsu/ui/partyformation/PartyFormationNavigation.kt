package jp.co.fuller.morikan.bootcamp.mikatsu.ui.partyformation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** 編成画面のルート。画面固有の入力を持たないため引数はない。 */
@Serializable
data object PartyFormationRoute : NavKey

/**
 * バックスタックのエントリープロバイダーへ編成画面を登録する。
 *
 * @param onBack 画面上部の戻るボタンが押されたときに呼ばれるコールバック。
 * @param onConfirm 「決定」ボタンが押されたときに呼ばれるコールバック。
 *   選択済みキャラクターIDの一覧を引数に、バトル画面への遷移をNavHost側に委ねる。
 */
fun EntryProviderScope<NavKey>.partyFormationEntry(
    onBack: () -> Unit,
    onConfirm: (List<Int>) -> Unit,
) {
    entry<PartyFormationRoute> {
        val viewModel: PartyFormationViewModel = hiltViewModel()
        PartyFormationScreen(
            viewModel = viewModel,
            onBack = onBack,
            onConfirm = onConfirm,
        )
    }
}
