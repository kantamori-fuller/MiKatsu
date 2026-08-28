package jp.co.fuller.morikan.bootcamp.mikatsu.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.charactercreate.characterCreateScreen
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.charactercreate.navigateToCharacterCreate
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.characterlist.characterListScreen
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.characterlist.navigateToCharacterList
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.mainmenu.MainMenuRoute
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.mainmenu.mainMenuScreen

/**
 * アプリ全体の画面遷移を組み立てるNavHost。
 *
 * ルートの定義・画面遷移コールバックの受け口は各画面の`<画面名>Navigation.kt`が
 * 個別に持つため、ここでは各画面が公開する`NavGraphBuilder`拡張関数を呼び出して
 * 画面グラフを組み立てることに専念する(特定の画面の引数の形などをここに書かない)。
 *
 * @param modifier このComposableに適用する[Modifier]。
 * @param navController 画面遷移を制御する[NavHostController]。テスト等で差し替え可能。
 */
@Composable
fun MiKatsuNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = MainMenuRoute,
        modifier = modifier,
    ) {
        mainMenuScreen(
            onNavigateToCharacterCreate = { navController.navigateToCharacterCreate() },
            onNavigateToCharacterList = { navController.navigateToCharacterList() },
        )
        characterListScreen(
            onBack = { navController.popBackStack() },
            onSelectCharacter = { id -> navController.navigateToCharacterCreate(characterId = id) },
        )
        characterCreateScreen(
            onBack = { navController.popBackStack() },
            onSaved = { navController.popBackStack() },
        )
    }
}
