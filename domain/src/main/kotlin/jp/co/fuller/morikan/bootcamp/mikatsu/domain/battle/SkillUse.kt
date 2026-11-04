package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleAlly
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleStat
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.ReflectedEffect
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.ResistanceOutcome
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.SkillResult
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.SkillUseResult
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.SkillTargetResult
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.StatChangeResult
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkill
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkillEffect
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillCostType
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.updatePawn
import kotlin.random.Random

/**
 * 現在の行動者によるアクティブスキルの使用を実行し、効果を適用した[BattleField]と結果を返す。
 *
 * 次のいずれかに当てはまる場合はスキルを使えないため`null`を返す。
 * - 使用者が現在の行動者ではない、味方ではない、戦闘不能である、またはブレイク中で行動できない。
 * - 使用者がそのスキルを設定していない。
 * - そのスキルがクールタイム(CT)中である。使えたら、外れてもCTを設定する([startCooldown])。
 * - コスト(MP・HP・TP。TPは不足分をHPで肩代わりできる)を支払えない([canAfford])。
 * - 選んだ対象が選べない個体である(レンジ外・陣営違い・戦闘不能・存在しない。[selectableTargets]を参照)。
 * - 基礎威力を持つ(ダメージを与える)スキル。この適用は未対応で、
 *   通常攻撃は[performAttack]で行う(将来、攻撃スキルもここへ統合する)。
 *
 * 使えた場合は、すべてのコストを消費したうえで、対象([resolveTargets]で範囲・対象選択に従って展開した個体)
 * 全員に効果を順に適用する。スキルの命中率が`null`でないときは、対象ごとに1回だけ[HitChanceFormula]で命中判定を行い
 * (使用者の命中率・対象の回避率を使い、対象がブレイク中なら必中)、外れた対象にはそのスキルの効果が一切かからない。
 * 命中率が`null`のスキルは乱数を引かず必ず成功する。コストは外れても消費する。
 * 回復は最大HPで頭打ちになり、戦闘不能の個体は対象に含まれないため蘇生しない。
 * スキルタイプにデバフまたはネガティブエフェクトを含むスキルだけ、命中した対象の属性耐性([ElementResistanceFormula])で扱いが変わる。
 * 無効(0など)・吸収(-999)の対象には効果がかからず、反射(-1)の対象にはかからずに効果が使用者へ跳ね返る
 * (使用者の耐性が無効・反射・吸収なら使用者にもかからない)。いずれも命中した扱いで、使用者のTP(条件4)は成立する。
 * 倍率(1〜999)・回復・バフ・ポジティブエフェクトには影響しない。
 * ステータス変化は対象に新しいエントリとして記録し、段階の上限で頭打ちでも記録する。
 * 発動回数が2以上でも効果の適用は1回のままである。
 * 最後に、全対象の処理が終わった後で、使用者・対象のTPをまとめて加算する([settleTp])。使用者は1体以上に命中したときだけ
 * 1回受け取り(攻撃技ではないので倍率なし)、避けた対象は18〜22を受け取る。乱数は命中判定がすべて終わった後で、使用者→対象の順に引く。
 * 手番の終了はここでは行わない。使用者・対象はいずれもFPIDで指定し、スキルはアクティブスキルのID
 * (アクティブ採番のマスターデータのID)で指定する。
 *
 * @receiver スキル使用前の戦闘フィールド。
 * @param actorFpid スキルを使う個体のFPID。
 * @param skillId 使うスキルのID。
 * @param targetFpid 選んだ対象(アンカー)のFPID。範囲スキルでは、この個体を基準に対象が広がる。
 * @param random 命中判定の乱数。テストで結果を固定できるよう外から受け取る。
 * @return 効果適用後の戦闘フィールドとスキルの結果の組。使えない場合は`null`。
 */
fun BattleField.performSkill(
    actorFpid: Int,
    skillId: Int,
    targetFpid: Int,
    random: Random,
): Pair<BattleField, SkillResult>? {
    if (actorFpid != currentActorFpid) return null
    val actor = allies.firstOrNull { it.fpid == actorFpid } ?: return null
    if (actor.isDefeated || actor.isBroken) return null
    val skill = actor.activeSkills.firstOrNull { it.id == skillId } ?: return null
    if (skill.basePower != null) return null
    if (actor.isOnCooldown(skill.id)) return null
    if (!actor.canAfford(skill)) return null
    val targets = resolveTargets(actorFpid, skill, targetFpid) ?: return null

    var field = spendCosts(actorFpid, skill).startCooldown(actorFpid, skill)
    val tpTargets = mutableListOf<TpTargetReport>()
    val results = targets.map { target ->
        // 途中の効果で状態が変わり得るため、命中判定には対象の最新の状態を使う。
        val current = checkNotNull(field.allPawns.firstOrNull { it.fpid == target.fpid })
        val isHit = HitChanceFormula.isHit(
            skillAccuracy = skill.accuracy,
            accuracyStage = actor.effectiveStage(BattleStat.ACCURACY),
            evasionStage = current.effectiveStage(BattleStat.EVASION),
            random = random,
            isTargetGuaranteedHit = current.isBroken,
        )
        if (!isHit) {
            tpTargets += TpTargetReport(fpid = target.fpid, evaded = true)
            return@map SkillTargetResult(targetFpid = target.fpid, healedAmount = 0, isHit = false)
        }
        tpTargets += TpTargetReport(fpid = target.fpid)
        // デバフ・ネガティブエフェクトのスキルだけ、対象の属性耐性で扱いが変わる(無効・反射・吸収も命中した扱い)。
        val kind = if (ElementResistanceFormula.affectsEffects(skill)) current.resistanceKindTo(skill.element) else null
        when (kind) {
            ElementResistanceFormula.Kind.NULLIFY, ElementResistanceFormula.Kind.ABSORB ->
                // 吸収はダメージがないので、実質無効として扱う。
                return@map SkillTargetResult(
                    targetFpid = target.fpid,
                    healedAmount = 0,
                    resistance = ResistanceOutcome.NULLIFIED,
                )
            ElementResistanceFormula.Kind.REFLECT -> {
                // 効果を使用者に跳ね返す。使用者の耐性が無効・反射・吸収なら使用者にもかからない(新たな命中判定なし、再反射なし)。
                val latestActor = checkNotNull(field.allPawns.firstOrNull { it.fpid == actorFpid })
                val actorKind = latestActor.resistanceKindTo(skill.element)
                val reflected = if (actorKind != ElementResistanceFormula.Kind.MULTIPLIER) {
                    ReflectedEffect(isNullified = true)
                } else {
                    val applied = field.applyEffects(skill, actorFpid)
                    field = applied.field
                    ReflectedEffect(isNullified = false, statChanges = applied.statChanges)
                }
                return@map SkillTargetResult(
                    targetFpid = target.fpid,
                    healedAmount = 0,
                    resistance = ResistanceOutcome.REFLECTED,
                    reflected = reflected,
                )
            }
            ElementResistanceFormula.Kind.MULTIPLIER, null -> Unit
        }
        val applied = field.applyEffects(skill, target.fpid)
        field = applied.field
        SkillTargetResult(
            targetFpid = target.fpid,
            healedAmount = applied.healed,
            statChanges = applied.statChanges,
        )
    }
    field = field.settleTp(
        actorFpid = actorFpid,
        skill = skill,
        user = TpUserReport(isHit = results.any { it.isHit }),
        targets = tpTargets,
        random = random,
    )
    return field to SkillResult(
        actorFpid = actorFpid,
        skillId = skill.id,
        skillName = skill.name,
        targets = results,
    )
}

/**
 * 現在の行動者の味方によるアクティブスキルの使用を、ダメージの有無に関わらず1つの入口として実行する。
 *
 * 基礎威力を持つ(ダメージを与える)スキルは[performAttack]の流れ(命中・ダメージ・クリティカル・ブレイク・防御・軽減とコスト消費)で、
 * それ以外は[performSkill]で実行する。使用者が味方であること・スキルを設定済みであることをここで確認する
 * (使えない条件は[performSkill]と同じ)。手番の終了は行わない。
 *
 * @receiver スキル使用前の戦闘フィールド。
 * @param actorFpid スキルを使う個体のFPID。
 * @param skillId 使うスキルのID。
 * @param targetFpid 選んだ対象(アンカー)のFPID。
 * @param random 判定の乱数。
 * @return 使用後の戦闘フィールドと結果の組。使えない場合は`null`。
 */
fun BattleField.performActiveSkill(
    actorFpid: Int,
    skillId: Int,
    targetFpid: Int,
    random: Random,
): Pair<BattleField, SkillUseResult>? {
    val actor = allies.firstOrNull { it.fpid == actorFpid } ?: return null
    val skill = actor.activeSkills.firstOrNull { it.id == skillId } ?: return null
    if (skill.basePower == null) {
        return performSkill(actorFpid, skillId, targetFpid, random)?.let { (field, result) ->
            field to SkillUseResult.Effect(result)
        }
    }
    return performAttack(actorFpid, targetFpid, skill, random)?.let { (field, result) ->
        field to SkillUseResult.Attack(result)
    }
}

/**
 * 使用者のスキルのコスト(MP・HP・TP)を消費した[BattleField]を返す。味方でなければ何も消費しない。
 *
 * TPが足りないときは、持っているTPをすべて消費して(TPは0になる)、不足分を[substituteHpCostOf]のHPで肩代わりし、
 * スキルのHPコストと合わせて現在HPから引く。代替のHPは消費の経路(ここ)だけで引くため、
 * ブレイクゲージにもTP(攻撃を受けた条件)にも影響しない。肩代わりの量は消費前の使用者の状態から求める。
 * 支払えるかの確認は呼び出し側([canAfford])が行う。外れても戻さない。
 *
 * @receiver 消費前の戦闘フィールド。
 * @param actorFpid 使用者のFPID。
 * @param skill 使うスキル。
 * @return コストを消費した戦闘フィールド。
 */
internal fun BattleField.spendCosts(actorFpid: Int, skill: ActiveSkill): BattleField {
    val mpCost = skill.costOf(SkillCostType.MP)
    val hpCost = skill.costOf(SkillCostType.HP)
    val tpCost = skill.costOf(SkillCostType.TP)
    return updatePawn(
        fpid = actorFpid,
        transformAlly = {
            val substituteHp = it.substituteHpCostOf(skill)
            it.copy(
                currentMp = it.currentMp - mpCost,
                currentHp = it.currentHp - hpCost - substituteHp,
                currentTp = (it.currentTp - tpCost).coerceAtLeast(0),
            )
        },
        transformEnemy = { it },
    )
}

/**
 * 味方が、スキルのTPコストの不足分を肩代わりするHPの量(TP代替システム)を返す。
 *
 * 使用可否の判定([canAfford])・コスト消費([spendCosts])・戦闘画面のスキル一覧の表示が同じ計算を使うよう公開している。
 * 計算の本体は[TacticalPointFormula.substituteHpCost]。TPが足りているなら0。
 *
 * @receiver スキルを使おうとする味方。
 * @param skill 使おうとするスキル。
 * @return 代替で消費するHP。TPが足りている、またはTPコストがなければ0。
 */
fun BattleAlly.substituteHpCostOf(skill: ActiveSkill): Int =
    TacticalPointFormula.substituteHpCost(
        tpCost = skill.costOf(SkillCostType.TP),
        currentTp = currentTp,
        maxHp = maxHp,
    )

/**
 * 味方が、スキルのすべてのコストを支払えるかどうかを返す。
 *
 * MPは現在MPがコスト以上であること。HPは、HPコストとTP代替のHP([substituteHpCostOf])の合計を引いた後も現在HPが1以上残ることを条件とする
 * (HPが0以下になる使い方はできない。HPを1に丸めて払わせることはしない)。
 * TPが足りなくても、不足分をHPで肩代わりできれば使える。MPの不足は代替できない。
 * 戦闘画面のスキル一覧の使用可否表示と、[performSkill]の判定が同じ基準になるよう公開している。
 *
 * @receiver スキルを使おうとする味方。
 * @param skill 使おうとするスキル。
 * @return すべてのコストを支払えるなら`true`。
 */
fun BattleAlly.canAfford(skill: ActiveSkill): Boolean =
    currentMp >= skill.costOf(SkillCostType.MP) &&
        currentHp - skill.costOf(SkillCostType.HP) - substituteHpCostOf(skill) > 0

/**
 * [BattleField.applyEffects]の結果。
 *
 * @property field 効果を適用した戦闘フィールド。
 * @property healed 実際に回復したHP量の合計。
 * @property statChanges かかったステータス変化。
 */
private data class AppliedEffects(
    val field: BattleField,
    val healed: Int,
    val statChanges: List<StatChangeResult>,
)

/**
 * スキルの効果(回復・ステータス変化)を指定した個体に適用する。
 *
 * 対象に効く場合も、反射で使用者に跳ね返る場合も同じ処理を共有するために切り出している。
 * 回復は最大HPで頭打ちになる。ステータス変化は新しいエントリとして記録し、段階の上限で頭打ちでも記録する。
 *
 * @receiver 適用前の戦闘フィールド。
 * @param skill 効果を持つスキル。
 * @param fpid 効果を受ける個体のFPID。
 * @return 適用後のフィールドと、回復量・かかったステータス変化。
 */
private fun BattleField.applyEffects(skill: ActiveSkill, fpid: Int): AppliedEffects {
    var field = this
    var healedTotal = 0
    val statChanges = mutableListOf<StatChangeResult>()
    skill.effects.forEach { effect ->
        when (effect) {
            is ActiveSkillEffect.Heal -> {
                // 使用者自身が対象のとき、HPコスト消費後のHPを基準に回復量を求める。
                val latest = checkNotNull(field.allPawns.firstOrNull { it.fpid == fpid })
                val healed = effect.amount.coerceAtLeast(0).coerceAtMost(latest.maxHp - latest.currentHp)
                healedTotal += healed
                field = field.healPawn(fpid, healed)
            }
            is ActiveSkillEffect.StatChange -> {
                field = field.addStatModifier(fpid, effect.stat, effect.stages, effect.durationTurns)
                statChanges += StatChangeResult(stat = effect.stat, stages = effect.stages)
            }
        }
    }
    return AppliedEffects(field, healedTotal, statChanges)
}
