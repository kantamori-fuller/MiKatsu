package jp.co.fuller.morikan.bootcamp.mikatsu.ui.mainmenu

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * メインメニュー画面のルート。
 *
 * 画面固有の引数を持たないため、シングルトンである`data object`として型安全に定義する。
 */
@Serializable
data object MainMenuRoute : NavKey

/**
 * バックスタックのエントリープロバイダーへメインメニュー画面を登録する。
 *
 * ルートの定義と画面遷移コールバックの受け口をこの画面自身に持たせることで、
 * NavHost側がこの画面の内部事情(引数の形など)を知らずに済むようにすることを目的とする。
 *
 * @param onNavigateToCharacterCreate 「キャラを作成」導線が選ばれたときに呼ばれるコールバック。
 * @param onNavigateToCharacterList 「キャラ一覧」導線が選ばれたときに呼ばれるコールバック。
 */
fun EntryProviderScope<NavKey>.mainMenuEntry(
    onNavigateToCharacterCreate: () -> Unit,
    onNavigateToCharacterList: () -> Unit,
) {
    entry<MainMenuRoute> {
        MainMenuScreen(
            onCreateCharacter = onNavigateToCharacterCreate,
            onShowCharacterList = onNavigateToCharacterList,
        )
    }
}
