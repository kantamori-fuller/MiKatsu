package jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.DamageFormula
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.applyDamage
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.BattleRepository

/**
 * 通常攻撃を実行し、対象の現在HPを減らすUseCase。
 *
 * 攻撃者・対象は、同じキャラクター・同じ敵種が複数体登場しても一意に特定できるよう、
 * 必ずFPID(フィールドポーンID)で指定する。
 *
 * @property battleRepository 戦闘状態を保持するRepository。
 */
class AttackUseCase(private val battleRepository: BattleRepository) {

    /**
     * 攻撃者から対象へ通常攻撃を行い、ダメージを適用する。
     * 攻撃者か対象がフィールド上に見つからない場合は何もしない。
     *
     * @param attackerFpid 攻撃者のFPID。
     * @param targetFpid 攻撃対象のFPID。
     */
    suspend operator fun invoke(attackerFpid: Int, targetFpid: Int) {
        battleRepository.updateBattleField { field ->
            val pawns = field.allPawns
            val attacker = pawns.firstOrNull { it.fpid == attackerFpid }
            val target = pawns.firstOrNull { it.fpid == targetFpid }
            if (attacker == null || target == null) return@updateBattleField field

            val damage = DamageFormula.calculate(NORMAL_ATTACK_POWER, attacker.attack, target.defense)
            field.applyDamage(targetFpid = targetFpid, damage = damage)
        }
    }

    companion object {
        /** 通常攻撃の技威力。将来は技データから渡す暫定値。 */
        private const val NORMAL_ATTACK_POWER = 100
    }
}
