package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AttackHit
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AttackResult
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AttackShot
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleAlly
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleStat
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.CriticalType
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.ReflectedDamage
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.ResistanceOutcome
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.updatePawn
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.applyDamage
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkill
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillElement
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
 *
 * 属性耐性([ElementResistanceFormula])は、防御の30%カットの後・パッシブスキルの軽減の前に、対象の耐性値(スキルの属性に対する値)で適用する。
 * 倍率(1〜999)は`floor(ダメージ × 耐性 / 100)`(最低1)の後に軽減をかける。無効(0など)はダメージ0で軽減も通さない。
 * 吸収(-999)は対象のダメージを0にし、耐性が100だった場合のダメージ(軽減込み)を最大HPで頭打ちにして対象のHPを回復する。
 * 反射(-1)は対象のダメージを0にし、攻撃者が自分自身を攻撃したものとしてダメージを求め直して攻撃者に与える([reflectOntoAttacker]。新たな命中判定はなく、
 * クリティカルは反射されたヒットの判定結果をそのまま使う)。反射ダメージで攻撃者が戦闘不能になったら、その技の残りのヒット・対象をすべて打ち切る。
 * 無効・反射・吸収のヒットも、クリティカル判定(乱数)を含めて命中した扱いで、使用者のTP(条件1)は成立する。
 * 受けた側のHPは減らないので、ブレイクゲージもTP(条件2)も増えない(反射された攻撃者自身は、受けたダメージに応じて増える)。
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
    // 反射で攻撃者自身が受けたダメージによるTP(条件2)。技全体で合計してから切り捨てる。
    var attackerTpFromReflect = 0.0
    // 反射ダメージで攻撃者が戦闘不能になったら、残りのヒット・対象をすべて打ち切る。
    var aborted = false
    val tpTargets = mutableListOf<TpTargetReport>()
    val hits = mutableListOf<AttackHit>()
    for (target in targets) {
        if (aborted) break
        val shots = mutableListOf<AttackShot>()
        var tpFromDamage = 0.0
        while (shots.size < attackSkill.activationCount && !aborted) {
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
            // 属性耐性とパッシブスキルの軽減の前までのダメージ(吸収の回復量・反射の元にもなる)。
            val defended = DefenseFormula.reduceDamage(boosted, current.isDefending)
            val resistanceValue = current.resistanceTo(attackSkill.element)
            val kind = ElementResistanceFormula.classify(resistanceValue)
            val damage = when (kind) {
                ElementResistanceFormula.Kind.MULTIPLIER ->
                    current.reduceDamage(ElementResistanceFormula.scaleDamage(defended, resistanceValue))
                else -> 0
            }
            val hpLoss = minOf(damage, current.currentHp)
            // TPのために、ヒットの時点の防御と最終的なクリティカルの倍率を控える(防がれたクリティカルはNONEなので倍率に含めない)。
            val criticalRate = if (critical == CriticalType.CRITICAL) actualCriticalRate else null
            anyHit = true
            if (!current.isDefending) allHitsOnDefending = false
            if (criticalRate != null) maxCriticalRate = maxOf(maxCriticalRate ?: 0, criticalRate)
            tpFromDamage += TacticalPointFormula.damageTaken(hpLoss, current.maxHp, current.isDefending, criticalRate)
            var causedBreak = false
            var resistance = ResistanceOutcome.NONE
            var absorbedAmount = 0
            var reflected: ReflectedDamage? = null
            when (kind) {
                ElementResistanceFormula.Kind.MULTIPLIER -> {
                    val (broken, broke) = field
                        .applyDamage(targetFpid = target.fpid, damage = damage)
                        .accumulateBreakGauge(attacker.breakPointRate, target.fpid, hpLoss)
                    field = broken
                    causedBreak = broke
                }
                ElementResistanceFormula.Kind.NULLIFY -> resistance = ResistanceOutcome.NULLIFIED
                ElementResistanceFormula.Kind.ABSORB -> {
                    resistance = ResistanceOutcome.ABSORBED
                    // 耐性が100だった場合に受けるはずのダメージ(軽減込み)を、最大HPで頭打ちにして回復する。
                    absorbedAmount = current.reduceDamage(defended).coerceAtMost(current.maxHp - current.currentHp)
                    field = field.healPawn(target.fpid, absorbedAmount)
                }
                ElementResistanceFormula.Kind.REFLECT -> {
                    resistance = ResistanceOutcome.REFLECTED
                    val outcome = field.reflectOntoAttacker(
                        attackerFpid = attackerFpid,
                        element = attackSkill.element,
                        power = power,
                        offenseStat = offenseStat,
                        defenseStat = defenseStat,
                        judged = judged,
                        finalType = finalType,
                        damageStage = damageStage,
                        actualCriticalRate = actualCriticalRate,
                    )
                    field = outcome.field
                    reflected = outcome.reflected
                    attackerTpFromReflect += outcome.tpFromDamage
                    if (outcome.attackerDefeated) aborted = true
                }
            }
            shots += AttackShot(
                isHit = true,
                damage = damage,
                causedBreak = causedBreak,
                critical = critical,
                isCriticalBlocked = isCriticalBlocked,
                resistance = resistance,
                absorbedAmount = absorbedAmount,
                reflected = reflected,
            )
        }
        tpTargets += TpTargetReport(
            fpid = target.fpid,
            tpFromDamage = tpFromDamage,
            evaded = shots.any { !it.isHit },
        )
        hits += AttackHit(targetFpid = target.fpid, shots = shots)
    }
    // 反射で攻撃者自身が受けたダメージのTPは、乱数を引かない対象として最後に加える(乱数の順序を変えない)。
    if (attackerTpFromReflect > 0.0) {
        tpTargets += TpTargetReport(fpid = attackerFpid, tpFromDamage = attackerTpFromReflect)
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
        hits = hits.toList(),
        skillId = attackSkill.id,
        skillName = attackSkill.name,
    )
}

/**
 * 反射の処理結果。
 *
 * @property field 反射ダメージ(または吸収の回復)を攻撃者に反映した戦闘フィールド。
 * @property reflected 攻撃者が受けた反射ダメージの結果。
 * @property tpFromDamage 攻撃者が反射ダメージを受けたことによるTP(条件2)の元になる値(切り捨て前)。
 * @property attackerDefeated 反射ダメージで攻撃者が戦闘不能になったか。
 */
private data class ReflectOutcome(
    val field: BattleField,
    val reflected: ReflectedDamage,
    val tpFromDamage: Double,
    val attackerDefeated: Boolean,
)

/**
 * 反射された攻撃を、攻撃者が自分自身を攻撃したものとして計算し直して攻撃者に与える。
 *
 * ダメージ計算は攻撃者の破壊力→攻撃者の耐久力(特殊型ならマナ出力→マナ耐性)で、基礎威力は同じ。クリティカルは反射されたヒットの判定結果
 * ([judged]・[finalType])をそのまま使い(引き直さない)、攻撃者が防御中なら最終的なクリティカルを無効化する。
 * 続けて攻撃者のブレイク中の被ダメージ増加・防御中の30%カット・攻撃者の属性耐性・パッシブスキルの軽減を順に適用する。
 * 攻撃者の耐性が反射のときは再反射せず無効扱い、吸収なら攻撃者がHPを回復する。ダメージを受けた攻撃者のブレイクゲージは
 * 攻撃者自身のブレイクポイント倍率・ブレイク耐性で貯まる。反射された側の対象は呼び出し側で何も変化させない。
 *
 * @receiver 反射が起きた時点の戦闘フィールド。
 * @param attackerFpid 攻撃者のFPID。
 * @param element 攻撃の属性。
 * @param power 攻撃の基礎威力。
 * @param offenseStat ダメージ計算で攻撃側として使うステータス。
 * @param defenseStat ダメージ計算で防御側として使うステータス。
 * @param judged 反射されたヒットのクリティカル判定結果。
 * @param finalType 反射されたヒットの最終的なクリティカル種別(防御による無効化の前)。
 * @param damageStage 攻撃者のクリティカル倍率の段階。
 * @param actualCriticalRate 反射されたヒットの実際のクリティカル倍率(%)。TPの計算に使う。
 * @return 反射を反映した結果。
 */
private fun BattleField.reflectOntoAttacker(
    attackerFpid: Int,
    element: SkillElement,
    power: Int,
    offenseStat: BattleStat,
    defenseStat: BattleStat,
    judged: CriticalType,
    finalType: CriticalType,
    damageStage: Int,
    actualCriticalRate: Int,
): ReflectOutcome {
    val self = allPawns.first { it.fpid == attackerFpid }
    val base = DamageFormula.calculate(power, self.effectiveStat(offenseStat), self.effectiveStat(defenseStat))
    val isBlocked = DefenseFormula.blocksCritical(self.isDefending, finalType)
    val critical = if (isBlocked) CriticalType.NONE else finalType
    val appliedJudge = if (isBlocked) CriticalType.NONE else judged
    val criticalDamage = CriticalFormula.applyDamage(base, appliedJudge, self.criticalDamageRate, damageStage)
    val boosted = BreakFormula.boostDamage(criticalDamage, self.isBroken)
    val defended = DefenseFormula.reduceDamage(boosted, self.isDefending)
    val value = self.resistanceTo(element)
    var field = this
    var damage = 0
    var outcome = ResistanceOutcome.NONE
    var absorbed = 0
    var causedBreak = false
    when (ElementResistanceFormula.classify(value)) {
        ElementResistanceFormula.Kind.MULTIPLIER -> {
            damage = self.reduceDamage(ElementResistanceFormula.scaleDamage(defended, value))
            val (broken, broke) = field
                .applyDamage(targetFpid = attackerFpid, damage = damage)
                .accumulateBreakGauge(self.breakPointRate, attackerFpid, minOf(damage, self.currentHp))
            field = broken
            causedBreak = broke
        }
        // 攻撃者も反射の耐性なら、再反射せず無効扱いにする。
        ElementResistanceFormula.Kind.NULLIFY, ElementResistanceFormula.Kind.REFLECT -> outcome = ResistanceOutcome.NULLIFIED
        ElementResistanceFormula.Kind.ABSORB -> {
            outcome = ResistanceOutcome.ABSORBED
            absorbed = self.reduceDamage(defended).coerceAtMost(self.maxHp - self.currentHp)
            field = field.healPawn(attackerFpid, absorbed)
        }
    }
    val criticalRate = if (critical == CriticalType.CRITICAL) actualCriticalRate else null
    val hpLoss = minOf(damage, self.currentHp)
    val tp = TacticalPointFormula.damageTaken(hpLoss, self.maxHp, self.isDefending, criticalRate)
    val defeated = field.allPawns.first { it.fpid == attackerFpid }.isDefeated
    return ReflectOutcome(
        field = field,
        reflected = ReflectedDamage(
            damage = damage,
            outcome = outcome,
            absorbedAmount = absorbed,
            critical = critical,
            isCriticalBlocked = isBlocked,
            causedBreak = causedBreak,
        ),
        tpFromDamage = tp,
        attackerDefeated = defeated,
    )
}

/**
 * 指定した個体のHPを回復した戦闘フィールドを返す。吸収の回復を対象・攻撃者のどちらにも同じ処理で反映するために切り出している。
 *
 * @receiver 回復前の戦闘フィールド。
 * @param fpid 回復する個体のFPID。
 * @param amount 回復量。呼び出し側が最大HPで頭打ちにした値を渡す。
 * @return 回復後の戦闘フィールド。
 */
internal fun BattleField.healPawn(fpid: Int, amount: Int): BattleField = updatePawn(
    fpid = fpid,
    transformAlly = { it.copy(currentHp = it.currentHp + amount) },
    transformEnemy = { it.copy(currentHp = it.currentHp + amount) },
)
