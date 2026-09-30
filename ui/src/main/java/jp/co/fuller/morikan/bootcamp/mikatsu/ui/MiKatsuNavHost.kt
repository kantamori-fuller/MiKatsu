package jp.co.fuller.morikan.bootcamp.mikatsu.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.BattleRoute
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.battleEntry
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.charactercreate.CharacterCreateRoute
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.charactercreate.characterCreateEntry
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.characterlist.CharacterListRoute
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.characterlist.characterListEntry
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.mainmenu.MainMenuRoute
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.mainmenu.mainMenuEntry
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.partyformation.PartyFormationRoute
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.partyformation.partyFormationEntry

/**
 * アプリ全体の画面遷移を組み立てるNavHost(Navigation 3 / [NavDisplay]ベース)。
 *
 * バックスタックはこのComposableが所有する単純な[NavKey]のリストであり
 * ([rememberNavBackStack])、ルートの定義・画面遷移コールバックの受け口は各画面の
 * `<画面名>Navigation.kt`が個別に持つ。ここでは各画面が公開する
 * `EntryProviderScope`拡張関数を呼び出して画面グラフを組み立てることに専念し、
 * 特定の画面の引数の形などをここに書かない。
 *
 * @param modifier このComposableに適用する[Modifier]。
 */
@Composable
fun MiKatsuNavHost(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(MainMenuRoute)

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            mainMenuEntry(
                onNavigateToCharacterCreate = { backStack.add(CharacterCreateRoute()) },
                onNavigateToCharacterList = { backStack.add(CharacterListRoute) },
                onNavigateToPartyFormation = { backStack.add(PartyFormationRoute) },
            )
            characterListEntry(
                onBack = { backStack.removeLastOrNull() },
                onSelectCharacter = { id -> backStack.add(CharacterCreateRoute(characterId = id)) },
            )
            characterCreateEntry(
                onBack = { backStack.removeLastOrNull() },
                onSaved = { backStack.removeLastOrNull() },
            )
            partyFormationEntry(
                onBack = { backStack.removeLastOrNull() },
                onConfirm = { formation -> backStack.add(BattleRoute(formation)) },
            )
            battleEntry(
                onRetreat = { backStack.removeLastOrNull() },
            )
        },
    )
}
