package jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.endTurnWithCancellations
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.BattleRepository
import kotlin.random.Random

/**
 * 現在の行動者の手番を終え、次の手番を進めるUseCase。
 *
 * 手番の終了では、予約された先制行動の割り込みと、先制の手番の取り消しも行われる(`endTurn`)。
 *
 * @property battleRepository 戦闘状態を保持するRepository。
 * @property random 先制行動の予約の並びと、同時に手番が来て幸運値も同じ個体の順番を決める乱数。
 */
class EndTurnUseCase(
    private val battleRepository: BattleRepository,
    private val random: Random,
) {

    /**
     * 指定した個体の手番を終える。現在の行動者でなければ何もしない。
     *
     * 次の手番に進む際、先頭に来た先制の手番が行動できなくなっていれば(戦闘不能・ブレイク中・使える先制行動スキルが無い)、
     * その手番は使われずに取り消される。取り消された個体は、呼び出し側が「先制行動できなかった」と伝えられるよう戻り値で返す
     * (戦闘フィールドの状態にはイベントを残さない)。
     *
     * @param fpid 手番を終える個体のFPID。
     * @return 取り消された先制の手番の個体のFPID(取り消した順)。取り消しがなければ空。
     */
    suspend operator fun invoke(fpid: Int): List<Int> {
        // updateBattleFieldのtransformは再実行され得るため、最後に計算した結果を外の変数に残す。
        var cancelled: List<Int> = emptyList()
        battleRepository.updateBattleField { field ->
            val (next, cancelledInThisRun) = field.endTurnWithCancellations(fpid, random)
            cancelled = cancelledInThisRun
            next
        }
        return cancelled
    }
}
