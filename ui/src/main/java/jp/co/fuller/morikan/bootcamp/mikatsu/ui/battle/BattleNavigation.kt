package jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Formation
import kotlinx.serialization.Serializable

/**
 * バトル画面のルート。
 *
 * @property formationSlots 編成画面で編成された陣形の、スロット番号ごとの配置キャラクターID
 *   ([Formation.characterIdsBySlot]と同じ形式)。ルートはシリアライズ可能である必要があるため、
 *   ドメインモデルの[Formation]ではなく、その中身をそのまま保持する。
 */
@Serializable
data class BattleRoute(val formationSlots: List<Int?>) : NavKey {

    /**
     * 陣形のドメインモデルからルートを生成する。
     *
     * @param formation 編成画面で編成された陣形。
     */
    constructor(formation: Formation) : this(formation.characterIdsBySlot)
}

/**
 * バックスタックのエントリープロバイダーへバトル画面を登録する。
 *
 * ルートが保持する[BattleRoute.formationSlots]を陣形([Formation])に戻し、[BattleViewModel.Factory]経由で
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
            factory.create(Formation(route.formationSlots))
        }
        BattleScreen(
            viewModel = viewModel,
            onRetreat = onRetreat,
        )
    }
}
