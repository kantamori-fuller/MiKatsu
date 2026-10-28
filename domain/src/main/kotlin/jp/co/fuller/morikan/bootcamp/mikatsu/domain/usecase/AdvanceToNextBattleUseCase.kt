package jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.advanceToNextTurn
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleOutcome
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.BattleRepository
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.MapProgressRepository
import kotlinx.coroutines.flow.first
import kotlin.random.Random

/**
 * 勝利した戦闘のあと、同じ編成のまま次の戦闘へ進めるUseCase。
 *
 * 味方の現在HPは引き継ぎ、敵は新しく出し直す。次の戦闘へ進めたら進行度を1進める。
 * FPIDの採番は[GetBattleFieldUseCase]が唯一の実装箇所であり、本UseCaseはその結果を
 * 戦闘状態の正(SSOT)であるRepositoryへ保存することを目的とする。
 *
 * @property getBattleFieldUseCase 次戦のフィールドを組み立てるUseCase。
 * @property battleRepository 戦闘状態を保持するRepository。
 * @property mapProgressRepository 進行度を保持するRepository。
 * @property random 最初の手番で素早さ・幸運値が同じ個体の順番を決める乱数。
 */
class AdvanceToNextBattleUseCase(
    private val getBattleFieldUseCase: GetBattleFieldUseCase,
    private val battleRepository: BattleRepository,
    private val mapProgressRepository: MapProgressRepository,
    private val random: Random,
) {

    /**
     * 現在の戦闘が勝利で終わっていれば、次の戦闘を始めて進行度を進める。
     * 戦闘が未開始・未決着・敗北の場合は何もしない。
     *
     * @return 次の戦闘へ進めた場合は`true`、何もしなかった場合は`false`。
     */
    suspend operator fun invoke(): Boolean {
        val current = battleRepository.observeBattleField().first() ?: return false
        if (current.outcome != BattleOutcome.Victory) return false
        val next = getBattleFieldUseCase.createNextBattleField(current).advanceToNextTurn(random)
        // 新しい戦闘が更新前の進行度で描画されないよう、進行度を先に進めてから戦闘状態を設定する。
        mapProgressRepository.advance()
        battleRepository.setBattleField(next)
        return true
    }
}
