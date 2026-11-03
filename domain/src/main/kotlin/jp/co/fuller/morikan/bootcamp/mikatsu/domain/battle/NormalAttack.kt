package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AttackHit
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AttackResult
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AttackShot
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleStat
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.applyDamage
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkill
import kotlin.random.Random

/**
 * 現在の行動者による通常攻撃を実行し、対象にダメージを与えた[BattleField]と結果を返す。
 *
 * 威力と発動回数は、通常攻撃のスキルデータ([attackSkill]の基礎威力・発動回数)から読む。
 * 対象は、選んだ対象([targetFpid])を[resolveTargets]で範囲・対象選択に従って展開して決める。
 * ダメージ計算の破壊力・耐久力は、バフ・デバフを反映した値([jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleFieldPawn.effectiveStat])を使う。
 * 対象ごとに、1ヒットずつ[HitChanceFormula]で命中判定を行い、命中したヒットだけ[DamageFormula]の結果へ
 * ブレイク中の被ダメージ増加([BreakFormula.boostDamage])、続けて対象のパッシブスキルによるダメージ軽減
 * ([reduceDamage])を適用する。外れたヒットはダメージ0で軽減も適用せず、発動回数は1回消費する。
 * 発動回数だけ繰り返し、途中で対象が戦闘不能になったら、その対象への残りのヒットは行わない。
 * ダメージは1ヒットごとにフィールドへ適用してブレイクゲージを加算し([accumulateBreakGauge])、次のヒットは
 * 更新後の対象の状態で計算する。そのため多段攻撃の途中でブレイクすると、残りのヒットからすぐに被ダメージ増加と被必中が効く。
 * 味方・敵の攻撃が同じダメージ処理を共有し、重複させないために切り出している。
 * 攻撃者が現在の行動者ではない場合、攻撃者が見つからない・戦闘不能・ブレイク中(行動できない)の場合、[attackSkill]に基礎威力がない場合、
 * 選んだ対象が選べない個体(レンジ外・陣営違い・戦闘不能・存在しない)の場合は攻撃できないため`null`を返す。
 * 手番の終了はここでは行わない。
 *
 * なお特殊型攻撃(C・Dのステータスを使うダメージ式)は未実装で、現状は物理型と同じ式を使う(将来対応)。
 *
 * @receiver 攻撃前の戦闘フィールド。
 * @param attackerFpid 攻撃者のFPID。
 * @param targetFpid 選んだ攻撃対象(アンカー)のFPID。範囲攻撃では、この個体を基準に対象が広がる。
 * @param attackSkill 攻撃に用いるスキル(通常攻撃のデータ)。
 * @param random 命中判定の乱数。テストで結果を固定できるよう外から受け取る。
 * @return ダメージ適用後の戦闘フィールドと攻撃結果の組。攻撃結果は対象ごとのヒットごとの当たり外れとダメージを持つ。攻撃できない場合は`null`。
 */
fun BattleField.performAttack(
    attackerFpid: Int,
    targetFpid: Int,
    attackSkill: ActiveSkill,
    random: Random,
): Pair<BattleField, AttackResult>? {
    if (attackerFpid != currentActorFpid) return null
    val power = attackSkill.basePower ?: return null
    val attacker = allPawns.firstOrNull { it.fpid == attackerFpid } ?: return null
    if (attacker.isDefeated || attacker.isBroken) return null
    val targets = resolveTargets(attackerFpid, attackSkill, targetFpid) ?: return null

    var field = this
    val hits = targets.map { target ->
        val shots = mutableListOf<AttackShot>()
        while (shots.size < attackSkill.activationCount) {
            // 途中でブレイクした効果を残りのヒットに効かせるため、ヒットごとに最新の対象の状態を読む。
            val current = field.allPawns.firstOrNull { it.fpid == target.fpid } ?: break
            if (current.isDefeated) break
            val isHit = HitChanceFormula.isHit(
                skillAccuracy = attackSkill.accuracy,
                attackerAccuracy = attacker.accuracy,
                targetEvasion = current.evasion,
                random = random,
                isTargetGuaranteedHit = current.isBroken,
            )
            if (!isHit) {
                shots += AttackShot(isHit = false, damage = 0)
                continue
            }
            val base = DamageFormula.calculate(
                power,
                attacker.effectiveStat(BattleStat.ATTACK),
                current.effectiveStat(BattleStat.DEFENSE),
            )
            val damage = current.reduceDamage(BreakFormula.boostDamage(base, current.isBroken))
            val hpLoss = minOf(damage, current.currentHp)
            val (broken, causedBreak) = field
                .applyDamage(targetFpid = target.fpid, damage = damage)
                .accumulateBreakGauge(attacker.breakPointRate, target.fpid, hpLoss)
            field = broken
            shots += AttackShot(isHit = true, damage = damage, causedBreak = causedBreak)
        }
        AttackHit(targetFpid = target.fpid, shots = shots)
    }
    return field to AttackResult(attackerFpid = attackerFpid, hits = hits)
}
