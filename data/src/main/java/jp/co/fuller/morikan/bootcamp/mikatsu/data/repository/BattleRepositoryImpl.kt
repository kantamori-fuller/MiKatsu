package jp.co.fuller.morikan.bootcamp.mikatsu.data.repository

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.BattleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * [BattleRepository]のインメモリ実装。
 *
 * 戦闘状態はアプリのプロセス内でのみ保持し、永続化しない。戦闘が開始されるまでは`null`を保持する。
 */
class BattleRepositoryImpl @Inject constructor() : BattleRepository {

    private val battleField = MutableStateFlow<BattleField?>(null)

    override fun observeBattleField(): Flow<BattleField?> = battleField

    override suspend fun setBattleField(battleField: BattleField) {
        this.battleField.value = battleField
    }

    override suspend fun updateBattleField(transform: (BattleField) -> BattleField) {
        battleField.update { current -> current?.let(transform) }
    }
}
