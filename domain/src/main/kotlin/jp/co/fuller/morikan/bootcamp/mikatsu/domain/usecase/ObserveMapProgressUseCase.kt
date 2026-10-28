package jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.MapProgressRepository
import kotlinx.coroutines.flow.Flow

/**
 * マップの進行度を購読するUseCase。
 *
 * バトル画面が「今いる戦闘が何戦目か」を表示できるようにすることを目的とする。
 *
 * @property mapProgressRepository 進行度を保持するRepository。
 */
class ObserveMapProgressUseCase(private val mapProgressRepository: MapProgressRepository) {

    /**
     * 進行度を流し続ける[Flow]を返す。
     *
     * @return 進行度が変わる度に最新の値を流す[Flow]。
     */
    operator fun invoke(): Flow<Int> = mapProgressRepository.observeProgress()
}
