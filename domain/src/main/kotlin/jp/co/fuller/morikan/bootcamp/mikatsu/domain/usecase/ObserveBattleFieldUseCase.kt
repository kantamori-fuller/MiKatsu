package jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.BattleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull

/**
 * 戦闘フィールドの最新状態を購読するUseCase。
 *
 * バトル画面が現在HPの変化などをリアルタイムに表示できるようにすることを目的とする。
 *
 * @property battleRepository 戦闘状態を保持するRepository。
 */
class ObserveBattleFieldUseCase(private val battleRepository: BattleRepository) {

    /**
     * 戦闘フィールドを流し続ける[Flow]を返す。
     *
     * @return 戦闘開始後、状態が更新される度に最新の[BattleField]を流す[Flow]。
     */
    operator fun invoke(): Flow<BattleField> = battleRepository.observeBattleField().filterNotNull()
}
