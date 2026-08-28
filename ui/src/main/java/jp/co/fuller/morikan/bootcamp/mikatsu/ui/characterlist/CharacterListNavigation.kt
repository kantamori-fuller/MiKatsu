package jp.co.fuller.morikan.bootcamp.mikatsu.ui.characterlist

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/**
 * キャラ一覧画面のルート。
 *
 * 画面固有の引数を持たないため、シングルトンである`data object`として型安全に定義する。
 */
@Serializable
data object CharacterListRoute

/**
 * NavHostのグラフへキャラ一覧画面を登録する。
 *
 * ルートの定義とViewModelの取得(`hiltViewModel()`)をこの画面自身に持たせることで、
 * NavHost側がこの画面の内部事情を知らずに済むようにすることを目的とする。
 *
 * @param onBack 画面上部の戻るボタンが押されたときに呼ばれるコールバック。
 * @param onSelectCharacter 一覧アイテムが選択されたときに呼ばれるコールバック。
 *   選択されたキャラクターのIDを引数に受け取る。
 */
fun NavGraphBuilder.characterListScreen(
    onBack: () -> Unit,
    onSelectCharacter: (Int) -> Unit,
) {
    composable<CharacterListRoute> {
        val viewModel: CharacterListViewModel = hiltViewModel()
        CharacterListScreen(
            viewModel = viewModel,
            onBack = onBack,
            onSelectCharacter = onSelectCharacter,
        )
    }
}

/**
 * キャラ一覧画面へ遷移する。
 *
 * @param navOptions 遷移時の追加オプション(バックスタックのポップ挙動など)。
 */
fun NavController.navigateToCharacterList(navOptions: NavOptions? = null) {
    navigate(CharacterListRoute, navOptions)
}
