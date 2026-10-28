package jp.co.fuller.morikan.bootcamp.mikatsu.data.repository

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.MapProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * [MapProgressRepository]のインメモリ実装。
 *
 * 進行度はアプリのプロセス内でのみ保持し、永続化しない。最初は1戦目から始まる。
 */
class MapProgressRepositoryImpl @Inject constructor() : MapProgressRepository {

    private val progress = MutableStateFlow(INITIAL_PROGRESS)

    override fun observeProgress(): Flow<Int> = progress

    override suspend fun reset() {
        progress.value = INITIAL_PROGRESS
    }

    override suspend fun advance() {
        progress.update { it + 1 }
    }

    private companion object {
        /** 最初の戦闘の進行度。 */
        const val INITIAL_PROGRESS = 1
    }
}
