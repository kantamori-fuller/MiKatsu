package jp.co.fuller.morikan.bootcamp.mikatsu.ui.mainmenu

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/**
 * メインメニュー画面のルート。
 *
 * 画面固有の引数を持たないため、シングルトンである`data object`として型安全に定義する。
 */
@Serializable
data object MainMenuRoute

/**
 * NavHostのグラフへメインメニュー画面を登録する。
 *
 * ルートの定義と画面遷移コールバックの受け口をこの画面自身に持たせることで、
 * NavHost側がこの画面の内部事情(引数の形など)を知らずに済むようにすることを目的とする。
 *
 * @param onNavigateToCharacterCreate 「キャラを作成」導線が選ばれたときに呼ばれるコールバック。
 * @param onNavigateToCharacterList 「キャラ一覧」導線が選ばれたときに呼ばれるコールバック。
 */
fun NavGraphBuilder.mainMenuScreen(
    onNavigateToCharacterCreate: () -> Unit,
    onNavigateToCharacterList: () -> Unit,
) {
    composable<MainMenuRoute> {
        MainMenuScreen(
            onCreateCharacter = onNavigateToCharacterCreate,
            onShowCharacterList = onNavigateToCharacterList,
        )
    }
}

/**
 * メインメニュー画面へ遷移する。
 *
 * @param navOptions 遷移時の追加オプション(バックスタックのポップ挙動など)。
 */
fun NavController.navigateToMainMenu(navOptions: NavOptions? = null) {
    navigate(MainMenuRoute, navOptions)
}
