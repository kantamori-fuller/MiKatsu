package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleAlly
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.SkillResult
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkill
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkillEffect
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillCostType
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillTarget
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.updatePawn

/**
 * 現在の行動者によるアクティブスキルの使用を実行し、効果を適用した[BattleField]と結果を返す。
 *
 * 次のいずれかに当てはまる場合はスキルを使えないため`null`を返す。
 * - 使用者が現在の行動者ではない、味方ではない、または戦闘不能である。
 * - 使用者がそのスキルを設定していない。
 * - コスト(MP)が足りない。
 * - 対象がスキルの[SkillTarget]に合致しない(味方でない、戦闘不能である、見つからない)。
 *
 * 使えた場合は、コストを消費したうえで効果を適用する。手番の終了はここでは行わない。
 * 使用者・対象はいずれもFPIDで指定し、スキルはアクティブスキルのID(アクティブ採番のマスターデータのID)で指定する。
 *
 * @receiver スキル使用前の戦闘フィールド。
 * @param actorFpid スキルを使う個体のFPID。
 * @param skillId 使うスキルのID。
 * @param targetFpid 対象の個体のFPID。
 * @return 効果適用後の戦闘フィールドとスキルの結果の組。使えない場合は`null`。
 */
fun BattleField.performSkill(actorFpid: Int, skillId: Int, targetFpid: Int): Pair<BattleField, SkillResult>? {
    if (actorFpid != currentActorFpid) return null
    val actor = allies.firstOrNull { it.fpid == actorFpid } ?: return null
    if (actor.isDefeated) return null
    val skill = actor.activeSkills.firstOrNull { it.id == skillId } ?: return null
    if (!actor.canAfford(skill)) return null
    val target = resolveTarget(skill.target, targetFpid) ?: return null
    if (target.isDefeated) return null

    val paid = updatePawn(
        fpid = actorFpid,
        transformAlly = { it.copy(currentMp = it.currentMp - skill.cost) },
        transformEnemy = { it },
    )
    return when (val effect = skill.effect) {
        is ActiveSkillEffect.Heal -> {
            val healed = effect.amount.coerceAtLeast(0).coerceAtMost(target.maxHp - target.currentHp)
            val result = SkillResult(
                actorFpid = actorFpid,
                skillId = skill.id,
                skillName = skill.name,
                targetFpid = targetFpid,
                healedAmount = healed,
            )
            paid.updatePawn(
                fpid = targetFpid,
                transformAlly = { it.copy(currentHp = it.currentHp + healed) },
                transformEnemy = { it },
            ) to result
        }
    }
}

/**
 * 使用者が、スキルのコストを支払えるかどうかを返す。
 *
 * @receiver スキルを使おうとする味方。
 * @param skill 使おうとするスキル。
 * @return コストを支払えるなら`true`。
 */
private fun BattleAlly.canAfford(skill: ActiveSkill): Boolean = when (skill.costType) {
    SkillCostType.MP -> currentMp >= skill.cost
}

/**
 * スキルの対象指定に合致する個体を、FPIDから探す。
 *
 * @receiver 対象を探す戦闘フィールド。
 * @param target スキルの対象の指定方法。
 * @param targetFpid 対象として指定された個体のFPID。
 * @return 合致する個体。合致しなければ`null`。
 */
private fun BattleField.resolveTarget(target: SkillTarget, targetFpid: Int): BattleAlly? = when (target) {
    SkillTarget.SingleAlly -> allies.firstOrNull { it.fpid == targetFpid }
}
