package jp.co.fuller.morikan.bootcamp.mikatsu.ui.characterlist

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * キャラ一覧画面のルート。
 *
 * 画面固有の引数を持たないため、シングルトンである`data object`として型安全に定義する。
 */
@Serializable
data object CharacterListRoute : NavKey

/**
 * バックスタックのエントリープロバイダーへキャラ一覧画面を登録する。
 *
 * ルートの定義とViewModelの取得(`hiltViewModel()`)をこの画面自身に持たせることで、
 * NavHost側がこの画面の内部事情を知らずに済むようにすることを目的とする。
 *
 * @param onBack 画面上部の戻るボタンが押されたときに呼ばれるコールバック。
 * @param onSelectCharacter 一覧アイテムが選択されたときに呼ばれるコールバック。
 *   選択されたキャラクターのIDを引数に受け取る。
 */
fun EntryProviderScope<NavKey>.characterListEntry(
    onBack: () -> Unit,
    onSelectCharacter: (Int) -> Unit,
) {
    entry<CharacterListRoute> {
        val viewModel: CharacterListViewModel = hiltViewModel()
        CharacterListScreen(
            viewModel = viewModel,
            onBack = onBack,
            onSelectCharacter = onSelectCharacter,
        )
    }
}
