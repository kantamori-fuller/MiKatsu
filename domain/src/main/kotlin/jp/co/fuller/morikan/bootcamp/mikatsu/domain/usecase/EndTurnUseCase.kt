package jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.endTurn
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.BattleRepository
import kotlin.random.Random

/**
 * 現在の行動者の手番を終え、次の手番を進めるUseCase。
 *
 * @property battleRepository 戦闘状態を保持するRepository。
 * @property random 同時に手番が来て幸運値も同じ個体の順番を決める乱数。
 */
class EndTurnUseCase(
    private val battleRepository: BattleRepository,
    private val random: Random,
) {

    /**
     * 指定した個体の手番を終える。現在の行動者でなければ何もしない。
     *
     * @param fpid 手番を終える個体のFPID。
     */
    suspend operator fun invoke(fpid: Int) {
        battleRepository.updateBattleField { it.endTurn(fpid, random) }
    }
}
