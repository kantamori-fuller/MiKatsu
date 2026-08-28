package jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * バトル画面のルート。
 *
 * @property partyCharacterIds 編成画面で選択された、味方として参戦するキャラクターのID一覧。
 */
@Serializable
data class BattleRoute(val partyCharacterIds: List<Int>) : NavKey

/**
 * バックスタックのエントリープロバイダーへバトル画面を登録する。
 *
 * ルートが保持する[BattleRoute.partyCharacterIds]を、[BattleViewModel.Factory]経由で
 * ViewModelへAssisted Injectionする橋渡しをこの画面自身に持たせることで、NavHost側が
 * この画面の内部事情(引数の形など)を知らずに済むようにすることを目的とする。
 *
 * @param onRetreat 「撤退」ボタンが押されたときに呼ばれるコールバック。
 *   編成画面への遷移をNavHost側に委ねる。
 */
fun EntryProviderScope<NavKey>.battleEntry(
    onRetreat: () -> Unit,
) {
    entry<BattleRoute> { route ->
        val viewModel = hiltViewModel<BattleViewModel, BattleViewModel.Factory> { factory ->
            factory.create(route.partyCharacterIds)
        }
        BattleScreen(
            viewModel = viewModel,
            onRetreat = onRetreat,
        )
    }
}
