package jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.BattleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * 戦闘フィールドをメモリ上に保持するテスト用の[BattleRepository]。
 *
 * @param initial 初期状態の戦闘フィールド。
 */
class FakeBattleRepository(initial: BattleField) : BattleRepository {
    private val state = MutableStateFlow<BattleField?>(initial)

    /** 現在の戦闘フィールド。 */
    val current: BattleField get() = checkNotNull(state.value)

    override fun observeBattleField(): Flow<BattleField?> = state

    override suspend fun setBattleField(battleField: BattleField) {
        state.value = battleField
    }

    override suspend fun updateBattleField(transform: (BattleField) -> BattleField) {
        state.update { it?.let(transform) }
    }
}
