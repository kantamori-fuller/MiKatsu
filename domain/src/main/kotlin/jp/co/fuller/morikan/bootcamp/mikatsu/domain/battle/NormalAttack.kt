package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AttackResult
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.applyDamage

/** 通常攻撃の技威力。将来は技データから渡す暫定値。 */
private const val NORMAL_ATTACK_POWER = 100

/**
 * 現在の行動者による通常攻撃を実行し、対象にダメージを与えた[BattleField]と結果を返す。
 *
 * 味方・敵の攻撃が同じダメージ処理を共有し、重複させないために切り出している。
 * 攻撃者が現在の行動者ではない場合、攻撃者か対象が見つからない場合、攻撃者か対象が
 * 戦闘不能の場合は攻撃できないため`null`を返す。手番の終了はここでは行わない。
 *
 * @receiver 攻撃前の戦闘フィールド。
 * @param attackerFpid 攻撃者のFPID。
 * @param targetFpid 攻撃対象のFPID。
 * @return ダメージ適用後の戦闘フィールドと攻撃結果の組。攻撃できない場合は`null`。
 */
fun BattleField.performAttack(attackerFpid: Int, targetFpid: Int): Pair<BattleField, AttackResult>? {
    if (attackerFpid != currentActorFpid) return null
    val pawns = allPawns
    val attacker = pawns.firstOrNull { it.fpid == attackerFpid } ?: return null
    val target = pawns.firstOrNull { it.fpid == targetFpid } ?: return null
    if (attacker.isDefeated || target.isDefeated) return null

    val damage = DamageFormula.calculate(NORMAL_ATTACK_POWER, attacker.attack, target.defense)
    return applyDamage(targetFpid = targetFpid, damage = damage) to
        AttackResult(attackerFpid = attackerFpid, targetFpid = targetFpid, damage = damage)
}
