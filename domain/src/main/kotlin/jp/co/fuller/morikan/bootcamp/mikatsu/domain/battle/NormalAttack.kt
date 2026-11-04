package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AttackHit
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AttackResult
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AttackShot
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleAlly
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleStat
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.CriticalType
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
 * クリティカル判定([CriticalFormula]。命中後に1回、乱数を引く)と倍率の適用(ヒットの種別は最終的な倍率で決める)、続けてブレイク中の被ダメージ増加([BreakFormula.boostDamage])、対象が防御中なら30%カット([DefenseFormula.reduceDamage])、さらに対象のパッシブスキルによるダメージ軽減
 * ([reduceDamage])を適用する。外れたヒットはダメージ0で軽減も適用せず、発動回数は1回消費する。
 * 防御中の対象では、最終的にクリティカルになるヒットを通常ヒット扱いにする([DefenseFormula.blocksCritical]。乱数は通常どおり引く)。
 * 発動回数だけ繰り返し、途中で対象が戦闘不能になったら、その対象への残りのヒットは行わない。
 * ダメージは1ヒットごとにフィールドへ適用してブレイクゲージを加算し([accumulateBreakGauge])、次のヒットは
 * 更新後の対象の状態で計算する。そのため多段攻撃の途中でブレイクすると、残りのヒットからすぐに被ダメージ増加と被必中が効く。
 * 味方・敵の攻撃が同じダメージ処理を共有し、重複させないために切り出している。
 * 攻撃者が現在の行動者ではない場合、攻撃者が見つからない・戦闘不能・ブレイク中(行動できない)の場合、[attackSkill]に基礎威力がない場合、
 * 選んだ対象が選べない個体(レンジ外・陣営違い・戦闘不能・存在しない)の場合は攻撃できないため`null`を返す。
 * 手番の終了はここでは行わない。
 *
 * 攻撃者が味方のときは、スキルのコスト(MP・HP・TP。TPが足りない分はHPで肩代わりする)を技の最初に消費し(外れても戻さない)、払えなければ`null`を返す([canAfford])。
 * 通常攻撃はコストを持たないため消費しない。技の最後には、全対象・全ヒットの処理が終わった後で、使用者・対象のTPをまとめて加算する
 * ([settleTp]。使用者は1回の技で1回だけ、1発以上命中したときに条件1の分を受け取り、対象はヒットごとの被ダメージ割合と回避の分を受け取る)。
 * 乱数は、命中・クリティカルの判定がすべて終わった後で、使用者→対象の順にTPのために引く。
 * 結果には、使った技のIDと名前を持たせる。ダメージのあるスキルを使う場合も、この関数が命中・ダメージ・クリティカル・ブレイク・防御・軽減を担う。
 * スキルを設定済みかどうかの確認は呼び出し側([performActiveSkill])が行う。
 *
 * 特殊型攻撃(スキルタイプに特殊型攻撃を含み、物理型攻撃を含まない。[ActiveSkill.usesSpecialDamage])は、
 * ダメージ計算の破壊力・耐久力の代わりにマナ出力C・マナ耐性D(どちらもバフ・デバフ反映後)を渡す。式は同じ。
 * 攻撃者がそのスキルのクールタイム(CT)中なら攻撃できず`null`を返す。使えたら、外れてもCTを設定する([startCooldown])。
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
    if (attacker.isOnCooldown(attackSkill.id)) return null
    if (attacker is BattleAlly && !attacker.canAfford(attackSkill)) return null
    val targets = resolveTargets(attackerFpid, attackSkill, targetFpid) ?: return null

    // 特殊型攻撃(特殊型を含み物理型を含まないスキル)は、破壊力・耐久力の代わりにマナ出力C・マナ耐性Dを使う(式は同じ)。
    val offenseStat = if (attackSkill.usesSpecialDamage) BattleStat.MANA_OUTPUT else BattleStat.ATTACK
    val defenseStat = if (attackSkill.usesSpecialDamage) BattleStat.MANA_RESISTANCE else BattleStat.DEFENSE
    var field = spendCosts(attackerFpid, attackSkill).startCooldown(attackerFpid, attackSkill)
    // 使用者のTP(条件1)の材料。命中したヒットごとに集める。
    var anyHit = false
    var allHitsOnDefending = true
    var maxCriticalRate: Int? = null
    val tpTargets = mutableListOf<TpTargetReport>()
    val hits = targets.map { target ->
        val shots = mutableListOf<AttackShot>()
        var tpFromDamage = 0.0
        while (shots.size < attackSkill.activationCount) {
            // 途中でブレイクした効果を残りのヒットに効かせるため、ヒットごとに最新の対象の状態を読む。
            val current = field.allPawns.firstOrNull { it.fpid == target.fpid } ?: break
            if (current.isDefeated) break
            val isHit = HitChanceFormula.isHit(
                skillAccuracy = attackSkill.accuracy,
                accuracyStage = attacker.effectiveStage(BattleStat.ACCURACY),
                evasionStage = current.effectiveStage(BattleStat.EVASION),
                random = random,
                isTargetGuaranteedHit = current.isBroken,
            )
            if (!isHit) {
                shots += AttackShot(isHit = false, damage = 0)
                continue
            }
            val base = DamageFormula.calculate(
                power,
                attacker.effectiveStat(offenseStat),
                current.effectiveStat(defenseStat),
            )
            val judged = CriticalFormula.judge(attacker.effectiveStage(BattleStat.CRITICAL_RATE), random)
            val damageStage = attacker.effectiveStage(BattleStat.CRITICAL_DAMAGE)
            val actualCriticalRate = CriticalFormula.damageRate(
                attacker.criticalDamageRate,
                damageStage,
                isBad = judged == CriticalType.BAD_CRITICAL,
            )
            val finalType = CriticalFormula.finalType(judged, attacker.criticalDamageRate, damageStage)
            // 防御中の対象には、最終的にクリティカルになるヒットを通常ヒット扱いにする(乱数は判定のために引いたまま)。
            val isCriticalBlocked = DefenseFormula.blocksCritical(current.isDefending, finalType)
            val critical = if (isCriticalBlocked) CriticalType.NONE else finalType
            val appliedJudge = if (isCriticalBlocked) CriticalType.NONE else judged
            val criticalDamage = CriticalFormula.applyDamage(base, appliedJudge, attacker.criticalDamageRate, damageStage)
            val boosted = BreakFormula.boostDamage(criticalDamage, current.isBroken)
            val damage = current.reduceDamage(DefenseFormula.reduceDamage(boosted, current.isDefending))
            val hpLoss = minOf(damage, current.currentHp)
            // TPのために、ヒットの時点の防御と最終的なクリティカルの倍率を控える(防がれたクリティカルはNONEなので倍率に含めない)。
            val criticalRate = if (critical == CriticalType.CRITICAL) actualCriticalRate else null
            anyHit = true
            if (!current.isDefending) allHitsOnDefending = false
            if (criticalRate != null) maxCriticalRate = maxOf(maxCriticalRate ?: 0, criticalRate)
            tpFromDamage += TacticalPointFormula.damageTaken(hpLoss, current.maxHp, current.isDefending, criticalRate)
            val (broken, causedBreak) = field
                .applyDamage(targetFpid = target.fpid, damage = damage)
                .accumulateBreakGauge(attacker.breakPointRate, target.fpid, hpLoss)
            field = broken
            shots += AttackShot(
                isHit = true,
                damage = damage,
                causedBreak = causedBreak,
                critical = critical,
                isCriticalBlocked = isCriticalBlocked,
            )
        }
        tpTargets += TpTargetReport(
            fpid = target.fpid,
            tpFromDamage = tpFromDamage,
            evaded = shots.any { !it.isHit },
        )
        AttackHit(targetFpid = target.fpid, shots = shots)
    }
    field = field.settleTp(
        actorFpid = attackerFpid,
        skill = attackSkill,
        user = TpUserReport(isHit = anyHit, allHitsOnDefending = allHitsOnDefending, maxCriticalRate = maxCriticalRate),
        targets = tpTargets,
        random = random,
    )
    return field to AttackResult(
        attackerFpid = attackerFpid,
        hits = hits,
        skillId = attackSkill.id,
        skillName = attackSkill.name,
    )
}
