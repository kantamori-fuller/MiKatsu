package jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * マップの進行度(今いる戦闘が何戦目か)へのアクセスを抽象化するRepositoryインターフェース。
 *
 * 進行度の正(SSOT)をViewModelではなくここに持たせ、ViewModelは購読した結果からUiStateを構築する
 * ことを目的とする。最初の戦闘は1戦目である。
 */
interface MapProgressRepository {

    /**
     * 現在の進行度を購読する。
     *
     * @return 今いる戦闘が何戦目かを流す[Flow]。
     */
    fun observeProgress(): Flow<Int>

    /** 進行度を最初の1に戻す。 */
    suspend fun reset()

    /** 進行度を1進める。 */
    suspend fun advance()
}
