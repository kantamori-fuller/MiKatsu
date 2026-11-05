package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AutoTurnResult
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleAlly
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleEnemy
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleFieldPawn
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.StatusAilment
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkill
import kotlin.random.Random

/**
 * 現在の手番の個体が、CPUが動かす個体(敵・洗脳または発狂の個体)のとき、その手番の行動を実行する。
 *
 * 行動を決める優先順位は 行動不能・ブレイク > 発狂 > 洗脳 > 敵 > 手動操作(`mental-ailments.md`)。行動不能・ブレイク中の個体や、
 * 現在の行動者でない個体、手動で操作する味方(洗脳・発狂でない味方)では何もせず`null`を返す。
 * - 発狂: 乱数を ① `nextInt(100) < 60`なら何もしない → ② スキルを選ぶ → ③ 相手を選ぶ の順で引く([performMadnessTurn])。
 * - 洗脳・敵: 通常攻撃を、届く相手から[targetSelector]で選んで行う([performCpuAttackTurn])。
 *
 * @receiver 行動前の戦闘フィールド。
 * @param actorFpid 行動させる個体のFPID。
 * @param normalAttack 通常攻撃のスキルデータ。
 * @param targetSelector 洗脳・敵の攻撃対象を候補から選ぶ方法。
 * @param random 判定の乱数。
 * @return 行動後の戦闘フィールドと結果の組。CPUが動かす手番でなければ`null`(何も変更しない)。
 */
fun BattleField.performAutoTurn(
    actorFpid: Int,
    normalAttack: ActiveSkill,
    targetSelector: EnemyTargetSelector,
    random: Random,
): Pair<BattleField, AutoTurnResult>? {
    val actor = allPawns.firstOrNull { it.fpid == actorFpid } ?: return null
    if (actorFpid != currentActorFpid || actor.isDefeated || actor.isBroken || actor.isIncapacitated) return null
    return when {
        actor.hasAilment(StatusAilment.MADNESS) -> performMadnessTurn(actor, normalAttack, random)
        actor.hasAilment(StatusAilment.BRAINWASH) || actor is BattleEnemy ->
            performCpuAttackTurn(actor, normalAttack, targetSelector, random)
        else -> null
    }
}

/**
 * 発狂した個体の手番を実行する。
 *
 * 乱数は次の順に引く。
 * 1. 行動判定: `nextInt(100) < 60`なら何もしない(以降は引かない)。
 * 2. スキルの選択: 通常攻撃と、設定済みのAスキル(敵は通常攻撃だけ)のうち今使えるもの([isSkillUsable]。CTが明けていて、
 *    封印を含めコストを払え、発狂用の対象選びで選べる相手が1体以上いる)から等確率で`nextInt(候補数)`。候補が空なら何もしない(乱数は引かない)。
 * 3. 相手の選択: そのスキルの[selectableTargets](陣営を無視。使用者自身を含む。レンジ0は自分だけ)から等確率で`nextInt(候補数)`。
 *    範囲スキルは選んだ1体をアンカーにして[resolveTargets]のルールで広げる。
 * 実行は基礎威力があれば[performAttack]、なければ[performSkill]で、コストは普通に払う。
 *
 * @receiver 行動前の戦闘フィールド。
 * @param actor 発狂している行動者。
 * @param normalAttack 通常攻撃のスキルデータ。
 * @param random 判定の乱数。
 * @return 行動後の戦闘フィールドと結果の組。何もしなければ元のフィールドと[AutoTurnResult.MadnessIdle]。
 */
private fun BattleField.performMadnessTurn(
    actor: BattleFieldPawn,
    normalAttack: ActiveSkill,
    random: Random,
): Pair<BattleField, AutoTurnResult> {
    val idle = this to AutoTurnResult.MadnessIdle
    if (random.nextInt(100) < StatusAilmentFormula.MADNESS_IDLE_PERCENT) return idle
    val skills = listOf(normalAttack) + (actor as? BattleAlly)?.activeSkills.orEmpty()
    val usable = skills.filter { isSkillUsable(actor.fpid, it) }
    if (usable.isEmpty()) return idle
    val skill = usable[random.nextInt(usable.size)]
    val candidates = selectableTargets(actor.fpid, skill)
    val anchor = candidates[random.nextInt(candidates.size)]
    return if (skill.basePower != null) {
        performAttack(actor.fpid, anchor.fpid, skill, random)?.let { (field, result) -> field to AutoTurnResult.Attacked(result) } ?: idle
    } else {
        performSkill(actor.fpid, skill.id, anchor.fpid, random)?.let { (field, result) -> field to AutoTurnResult.UsedSkill(result) } ?: idle
    }
}

/**
 * 敵または洗脳された個体の手番として、通常攻撃を実行する。
 *
 * 通常攻撃が届く相手を[selectableTargets]で求め(洗脳は自分の陣営のうち自分以外、敵は反対の陣営)、その中から[targetSelector]が選んだ相手を攻撃する。
 * 届く相手がいなければ何もしない。魅了している敵は反対の陣営を選べず候補が空になるので、「届かない」ではなく「何もできない」とする。
 *
 * @receiver 行動前の戦闘フィールド。
 * @param actor 行動者(敵または洗脳された個体)。
 * @param normalAttack 通常攻撃のスキルデータ。
 * @param targetSelector 攻撃対象を候補から選ぶ方法。
 * @param random 攻撃の乱数。
 * @return 行動後の戦闘フィールドと結果の組。
 */
private fun BattleField.performCpuAttackTurn(
    actor: BattleFieldPawn,
    normalAttack: ActiveSkill,
    targetSelector: EnemyTargetSelector,
    random: Random,
): Pair<BattleField, AutoTurnResult> {
    val candidates = selectableTargets(actor.fpid, normalAttack)
    if (candidates.isEmpty()) {
        val charmed = actor.hasAilment(StatusAilment.CHARM) && !actor.isAutoActing
        return this to if (charmed) AutoTurnResult.NothingToDo else AutoTurnResult.CannotReach
    }
    val target = targetSelector.select(this, actor, candidates) ?: return this to AutoTurnResult.CannotReach
    return performAttack(actor.fpid, target.fpid, normalAttack, random)
        ?.let { (field, result) -> field to AutoTurnResult.Attacked(result) }
        ?: (this to AutoTurnResult.CannotReach)
}
