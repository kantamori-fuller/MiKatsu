package jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.MapProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * 進行度をメモリ上に保持するテスト用の[MapProgressRepository]。
 *
 * @param initial 初期の進行度。
 */
class FakeMapProgressRepository(initial: Int = 1) : MapProgressRepository {
    private val state = MutableStateFlow(initial)

    /** 現在の進行度。 */
    val current: Int get() = state.value

    override fun observeProgress(): Flow<Int> = state

    override suspend fun reset() {
        state.value = 1
    }

    override suspend fun advance() {
        state.update { it + 1 }
    }
}
