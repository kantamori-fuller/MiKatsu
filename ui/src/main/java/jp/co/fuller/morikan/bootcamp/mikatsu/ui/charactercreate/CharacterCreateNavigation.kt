package jp.co.fuller.morikan.bootcamp.mikatsu.ui.charactercreate

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * キャラ作成/編集画面のルート。
 *
 * @property characterId 編集対象のキャラクターID。新規作成の場合は`null`。
 *   `null`をそのまま表現できる型安全ルートを用いることで、新規作成を表す番兵値が不要になる。
 */
@Serializable
data class CharacterCreateRoute(val characterId: Int? = null) : NavKey

/**
 * バックスタックのエントリープロバイダーへキャラ作成/編集画面を登録する。
 *
 * ルートが保持する[CharacterCreateRoute.characterId]を、[CharacterCreateViewModel.Factory]
 * 経由でViewModelへAssisted Injectionする橋渡しをこの画面自身に持たせることで、
 * NavHost側がこの画面の内部事情(引数の形など)を知らずに済むようにすることを目的とする。
 *
 * @param onBack 画面上部の戻るボタンが押されたときに呼ばれるコールバック。
 * @param onSaved 保存が完了し、前の画面へ戻るべきタイミングで呼ばれるコールバック。
 */
fun EntryProviderScope<NavKey>.characterCreateEntry(
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    entry<CharacterCreateRoute> { route ->
        val viewModel = hiltViewModel<CharacterCreateViewModel, CharacterCreateViewModel.Factory> { factory ->
            factory.create(route.characterId)
        }
        CharacterCreateScreen(
            viewModel = viewModel,
            onBack = onBack,
            onSaved = onSaved,
        )
    }
}
