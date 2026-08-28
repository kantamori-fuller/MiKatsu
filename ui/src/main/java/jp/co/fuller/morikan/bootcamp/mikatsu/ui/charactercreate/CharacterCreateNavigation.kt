package jp.co.fuller.morikan.bootcamp.mikatsu.ui.charactercreate

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/**
 * キャラ作成/編集画面のルート。
 *
 * @property characterId 編集対象のキャラクターID。新規作成の場合は`null`。
 *   `null`をそのまま表現できる型安全ルートを用いることで、旧来の文字列ルートで
 *   必要だった「新規作成を表す番兵値」が不要になる。
 */
@Serializable
data class CharacterCreateRoute(val characterId: Int? = null)

/**
 * NavHostのグラフへキャラ作成/編集画面を登録する。
 *
 * ルートの定義とViewModelの取得(`hiltViewModel()`)をこの画面自身に持たせることで、
 * NavHost側がこの画面の内部事情(引数の形など)を知らずに済むようにすることを目的とする。
 *
 * @param onBack 画面上部の戻るボタンが押されたときに呼ばれるコールバック。
 * @param onSaved 保存が完了し、前の画面へ戻るべきタイミングで呼ばれるコールバック。
 */
fun NavGraphBuilder.characterCreateScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    composable<CharacterCreateRoute> {
        val viewModel: CharacterCreateViewModel = hiltViewModel()
        CharacterCreateScreen(
            viewModel = viewModel,
            onBack = onBack,
            onSaved = onSaved,
        )
    }
}

/**
 * キャラ作成/編集画面へ遷移する。
 *
 * @param characterId 編集対象のキャラクターID。新規作成の場合は`null`(デフォルト)。
 * @param navOptions 遷移時の追加オプション(バックスタックのポップ挙動など)。
 */
fun NavController.navigateToCharacterCreate(characterId: Int? = null, navOptions: NavOptions? = null) {
    navigate(CharacterCreateRoute(characterId), navOptions)
}
