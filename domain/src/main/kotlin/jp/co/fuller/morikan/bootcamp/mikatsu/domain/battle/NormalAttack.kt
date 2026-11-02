package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AttackHit
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AttackResult
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.applyDamage
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkill

/**
 * 現在の行動者による通常攻撃を実行し、対象にダメージを与えた[BattleField]と結果を返す。
 *
 * 威力と攻撃回数は、通常攻撃のスキルデータ([attackSkill]の基礎威力・攻撃回数)から読む。
 * 対象は、選んだ対象([targetFpid])を[resolveTargets]で範囲・対象選択に従って展開して決める。
 * 対象ごとに、1ヒットずつ[DamageFormula]の結果へ対象のパッシブスキルによるダメージ軽減([reduceDamage])を適用し、
 * 攻撃回数だけ繰り返す。途中で対象が戦闘不能になったら、その対象への残りのヒットは行わない。
 * 味方・敵の攻撃が同じダメージ処理を共有し、重複させないために切り出している。
 * 攻撃者が現在の行動者ではない場合、攻撃者が見つからない・戦闘不能の場合、[attackSkill]に基礎威力がない場合、
 * 選んだ対象が選べない個体(レンジ外・陣営違い・戦闘不能・存在しない)の場合は攻撃できないため`null`を返す。
 * 手番の終了はここでは行わない。
 *
 * なお特殊型攻撃(C・Dのステータスを使うダメージ式)は未実装で、現状は物理型と同じ式を使う(将来対応)。
 *
 * @receiver 攻撃前の戦闘フィールド。
 * @param attackerFpid 攻撃者のFPID。
 * @param targetFpid 選んだ攻撃対象(アンカー)のFPID。範囲攻撃では、この個体を基準に対象が広がる。
 * @param attackSkill 攻撃に用いるスキル(通常攻撃のデータ)。
 * @return ダメージ適用後の戦闘フィールドと攻撃結果の組。攻撃結果は対象ごとのダメージ(全ヒットの合計)を持つ。攻撃できない場合は`null`。
 */
fun BattleField.performAttack(
    attackerFpid: Int,
    targetFpid: Int,
    attackSkill: ActiveSkill,
): Pair<BattleField, AttackResult>? {
    if (attackerFpid != currentActorFpid) return null
    val power = attackSkill.basePower ?: return null
    val attacker = allPawns.firstOrNull { it.fpid == attackerFpid } ?: return null
    if (attacker.isDefeated) return null
    val targets = resolveTargets(attackerFpid, attackSkill, targetFpid) ?: return null

    var field = this
    val hits = targets.map { target ->
        var remainingHp = target.currentHp
        var totalDamage = 0
        var count = 0
        while (count < attackSkill.hitCount && remainingHp > 0) {
            val damage = target.reduceDamage(DamageFormula.calculate(power, attacker.attack, target.defense))
            totalDamage += damage
            remainingHp -= damage
            count++
        }
        field = field.applyDamage(targetFpid = target.fpid, damage = totalDamage)
        AttackHit(targetFpid = target.fpid, damage = totalDamage)
    }
    return field to AttackResult(attackerFpid = attackerFpid, hits = hits)
}
