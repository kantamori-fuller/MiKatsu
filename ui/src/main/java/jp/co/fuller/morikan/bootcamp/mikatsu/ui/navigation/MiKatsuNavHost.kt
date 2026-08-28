package jp.co.fuller.morikan.bootcamp.mikatsu.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.screens.CharacterCreateScreen
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.screens.CharacterListScreen
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.screens.MainMenuScreen
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.viewmodel.CharacterCreateViewModel
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.viewmodel.CharacterListViewModel

@Composable
fun MiKatsuNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = MiKatsuDestinations.MAIN_MENU,
        modifier = modifier,
    ) {
        composable(MiKatsuDestinations.MAIN_MENU) {
            MainMenuScreen(
                onCreateCharacter = { navController.navigate(MiKatsuDestinations.characterCreateRoute()) },
                onShowCharacterList = { navController.navigate(MiKatsuDestinations.CHARACTER_LIST) },
            )
        }
        composable(MiKatsuDestinations.CHARACTER_LIST) {
            val viewModel: CharacterListViewModel = hiltViewModel()
            CharacterListScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onSelectCharacter = { id -> navController.navigate(MiKatsuDestinations.characterCreateRoute(id)) },
            )
        }
        composable(
            route = MiKatsuDestinations.CHARACTER_CREATE,
            arguments = listOf(
                navArgument(MiKatsuDestinations.ARG_CHARACTER_ID) {
                    type = NavType.IntType
                    defaultValue = MiKatsuDestinations.NEW_CHARACTER_ID
                },
            ),
        ) {
            val viewModel: CharacterCreateViewModel = hiltViewModel()
            CharacterCreateScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }
    }
}
