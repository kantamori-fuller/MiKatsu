package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AttackResult
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.applyDamage
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkill

/**
 * 現在の行動者による通常攻撃を実行し、対象にダメージを与えた[BattleField]と結果を返す。
 *
 * 威力と攻撃回数は、通常攻撃のスキルデータ([attackSkill]の基礎威力・攻撃回数)から読む。
 * 1ヒットごとに[DamageFormula]の結果へ対象のパッシブスキルによるダメージ軽減([reduceDamage])を適用し、
 * 攻撃回数だけ繰り返す。途中で対象が戦闘不能になったら、残りのヒットは行わない。
 * 味方・敵の攻撃が同じダメージ処理を共有し、重複させないために切り出している。
 * 攻撃者が現在の行動者ではない場合、攻撃者か対象が見つからない場合、攻撃者か対象が戦闘不能の場合、
 * [attackSkill]に基礎威力がない場合は攻撃できないため`null`を返す。手番の終了はここでは行わない。
 *
 * なお特殊型攻撃(C・Dのステータスを使うダメージ式)は未実装で、現状は物理型と同じ式を使う(将来対応)。
 *
 * @receiver 攻撃前の戦闘フィールド。
 * @param attackerFpid 攻撃者のFPID。
 * @param targetFpid 攻撃対象のFPID。
 * @param attackSkill 攻撃に用いるスキル(通常攻撃のデータ)。
 * @return ダメージ適用後の戦闘フィールドと攻撃結果の組。攻撃結果のダメージは全ヒットの合計。攻撃できない場合は`null`。
 */
fun BattleField.performAttack(
    attackerFpid: Int,
    targetFpid: Int,
    attackSkill: ActiveSkill,
): Pair<BattleField, AttackResult>? {
    if (attackerFpid != currentActorFpid) return null
    val power = attackSkill.basePower ?: return null
    val pawns = allPawns
    val attacker = pawns.firstOrNull { it.fpid == attackerFpid } ?: return null
    val target = pawns.firstOrNull { it.fpid == targetFpid } ?: return null
    if (attacker.isDefeated || target.isDefeated) return null

    var remainingHp = target.currentHp
    var totalDamage = 0
    var hits = 0
    while (hits < attackSkill.hitCount && remainingHp > 0) {
        val damage = target.reduceDamage(DamageFormula.calculate(power, attacker.attack, target.defense))
        totalDamage += damage
        remainingHp -= damage
        hits++
    }
    return applyDamage(targetFpid = targetFpid, damage = totalDamage) to
        AttackResult(attackerFpid = attackerFpid, targetFpid = targetFpid, damage = totalDamage)
}
