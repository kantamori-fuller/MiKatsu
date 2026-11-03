package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleAlly
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleStat
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
 * - コスト(MP・HP・TP)を支払えない([canAfford])。
 * - 選んだ対象が選べない個体である(レンジ外・陣営違い・戦闘不能・存在しない。[selectableTargets]を参照)。
 * - 基礎威力を持つ(ダメージを与える)スキル。この適用は未対応で、
 *   通常攻撃は[performAttack]で行う(将来、攻撃スキルもここへ統合する)。
 *
 * 使えた場合は、すべてのコストを消費したうえで、対象([resolveTargets]で範囲・対象選択に従って展開した個体)
 * 全員に効果を順に適用する。スキルの命中率が`null`でないときは、対象ごとに1回だけ[HitChanceFormula]で命中判定を行い
 * (使用者の命中率・対象の回避率を使い、対象がブレイク中なら必中)、外れた対象にはそのスキルの効果が一切かからない。
 * 命中率が`null`のスキルは乱数を引かず必ず成功する。コストは外れても消費する。
 * 回復は最大HPで頭打ちになり、戦闘不能の個体は対象に含まれないため蘇生しない。
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
    if (!actor.canAfford(skill)) return null
    val targets = resolveTargets(actorFpid, skill, targetFpid) ?: return null

    var field = spendCosts(actorFpid, skill)
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
        var healedTotal = 0
        val statChanges = mutableListOf<StatChangeResult>()
        skill.effects.forEach { effect ->
            when (effect) {
                is ActiveSkillEffect.Heal -> {
                    // 使用者自身が対象のとき、HPコスト消費後のHPを基準に回復量を求める。
                    val latest = checkNotNull(field.allPawns.firstOrNull { it.fpid == target.fpid })
                    val healed = effect.amount.coerceAtLeast(0).coerceAtMost(latest.maxHp - latest.currentHp)
                    healedTotal += healed
                    field = field.updatePawn(
                        fpid = target.fpid,
                        transformAlly = { it.copy(currentHp = it.currentHp + healed) },
                        transformEnemy = { it.copy(currentHp = it.currentHp + healed) },
                    )
                }
                is ActiveSkillEffect.StatChange -> {
                    field = field.addStatModifier(target.fpid, effect.stat, effect.stages, effect.durationTurns)
                    statChanges += StatChangeResult(stat = effect.stat, stages = effect.stages)
                }
            }
        }
        SkillTargetResult(targetFpid = target.fpid, healedAmount = healedTotal, statChanges = statChanges)
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
        transformAlly = { it.copy(currentMp = it.currentMp - mpCost, currentHp = it.currentHp - hpCost, currentTp = it.currentTp - tpCost) },
        transformEnemy = { it },
    )
}

/**
 * 味方が、スキルのすべてのコストを支払えるかどうかを返す。
 *
 * MPは現在MPがコスト以上、TPは現在TPがコスト以上であること、HPは消費後も現在HPが1以上残る(HPが0以下になるコストは払えない)ことを条件とする。
 * 戦闘画面のスキル一覧の使用可否表示と、[performSkill]の判定が同じ基準になるよう公開している。
 *
 * @receiver スキルを使おうとする味方。
 * @param skill 使おうとするスキル。
 * @return すべてのコストを支払えるなら`true`。
 */
fun BattleAlly.canAfford(skill: ActiveSkill): Boolean =
    currentMp >= skill.costOf(SkillCostType.MP) &&
        currentTp >= skill.costOf(SkillCostType.TP) &&
        currentHp - skill.costOf(SkillCostType.HP) > 0
