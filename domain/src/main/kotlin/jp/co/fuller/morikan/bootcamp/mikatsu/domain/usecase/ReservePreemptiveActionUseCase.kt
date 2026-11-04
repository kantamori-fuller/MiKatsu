package jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.reservePreemptive
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.BattleRepository

/**
 * 味方の先制行動を予約するUseCase。
 *
 * 予約はカードをタップした時点の記録で、行動順への割り込みは現在の行動者の手番が終わるとき([EndTurnUseCase])に行われる。
 * 予約できる条件は`canReservePreemptive`が唯一の判定箇所で、UIもそれを使う。個体はFPIDで指定する。
 *
 * @property battleRepository 戦闘状態を保持するRepository。
 */
class ReservePreemptiveActionUseCase(
    private val battleRepository: BattleRepository,
) {

    /**
     * 指定した個体の先制行動を予約する。
     *
     * 予約できない場合(味方でない・戦闘不能・ブレイク中・予約済みまたは先制の手番待ち・勝敗が決まっている・現在の行動者・
     * 使える先制行動スキルが無い)は、何も変更せずに`false`を返す。
     *
     * @param fpid 予約する個体のFPID。
     * @return 予約できたら`true`。
     */
    suspend operator fun invoke(fpid: Int): Boolean {
        // updateBattleFieldのtransformは再実行され得るため、最後に計算した結果を外の変数に残す。
        var reserved = false
        battleRepository.updateBattleField { field ->
            val next = field.reservePreemptive(fpid)
            reserved = next != null
            next ?: field
        }
        return reserved
    }
}
