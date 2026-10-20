package jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.performAttack
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AttackResult
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.BattleRepository

/**
 * 味方の通常攻撃を実行し、対象の現在HPを減らすUseCase。
 *
 * 攻撃者・対象は、同じキャラクター・同じ敵種が複数体登場しても一意に特定できるよう、
 * 必ずFPID(フィールドポーンID)で指定する。手番の終了は本UseCaseでは行わず、
 * 呼び出し側が[EndTurnUseCase]で行う。
 *
 * @property battleRepository 戦闘状態を保持するRepository。
 */
class AttackUseCase(private val battleRepository: BattleRepository) {

    /**
     * 攻撃者から対象へ通常攻撃を行い、ダメージを適用する。
     *
     * 攻撃者が現在の行動者ではない場合、攻撃者か対象が見つからない場合、攻撃者か対象が
     * 戦闘不能の場合は、何も変更せずに`null`を返す。
     *
     * @param attackerFpid 攻撃者のFPID。
     * @param targetFpid 攻撃対象のFPID。
     * @return 攻撃の結果。攻撃できなかった場合は`null`。
     */
    suspend operator fun invoke(attackerFpid: Int, targetFpid: Int): AttackResult? {
        // updateBattleFieldのtransformは再実行され得るため、最後に計算した結果を外の変数に残す。
        var result: AttackResult? = null
        battleRepository.updateBattleField { field ->
            val attack = field.performAttack(attackerFpid, targetFpid)
            result = attack?.second
            attack?.first ?: field
        }
        return result
    }
}
