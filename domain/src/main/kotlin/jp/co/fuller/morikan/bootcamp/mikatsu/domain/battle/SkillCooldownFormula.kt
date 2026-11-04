package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.SkillCooldown
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkill
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.updatePawn

/*
 * スキルのクールタイム(CT)の設定と経過。全スキル共通の仕組みで、ここが唯一の実装箇所。
 * 先制の手番も通常の手番と同じく1手番として数える(経過は[endTurn]が行う)。
 */

/**
 * スキルを使った個体に、そのスキルのCTを設定した[BattleField]を返す。
 *
 * CTが0のスキルは何もしない。技の命中・外れに関わらず、使ったら設定する。
 * 使用者の手番の最中([BattleField.currentActorFpid]が使用者)なら、その手番の終了では数えないようにする
 * ([SkillCooldown.startedDuringOwnTurn])。
 *
 * @receiver スキルを使った後の戦闘フィールド。
 * @param fpid スキルを使った個体のFPID。
 * @param skill 使ったスキル。
 * @return CTを設定した戦闘フィールド。
 */
internal fun BattleField.startCooldown(fpid: Int, skill: ActiveSkill): BattleField {
    if (skill.coolTime <= 0) return this
    val cooldown = SkillCooldown(
        skillId = skill.id,
        remainingTurns = skill.coolTime,
        startedDuringOwnTurn = fpid == currentActorFpid,
    )
    return updatePawn(
        fpid = fpid,
        transformAlly = { it.copy(skillCooldowns = it.skillCooldowns.filterNot { c -> c.skillId == skill.id } + cooldown) },
        transformEnemy = { it.copy(skillCooldowns = it.skillCooldowns.filterNot { c -> c.skillId == skill.id } + cooldown) },
    )
}

/**
 * 手番を終えた個体のCTを1手番分進める。
 *
 * 手番の最中に作られた記録は、フラグを下ろすだけで残りを減らさない。それ以外は残りを1減らし、0になった記録を消す。
 *
 * @receiver 更新前の戦闘フィールド。
 * @param fpid 手番を終えた個体のFPID。
 * @return CTを進めた戦闘フィールド。
 */
internal fun BattleField.progressCooldowns(fpid: Int): BattleField = updatePawn(
    fpid = fpid,
    transformAlly = { it.copy(skillCooldowns = it.skillCooldowns.afterOwnTurn()) },
    transformEnemy = { it.copy(skillCooldowns = it.skillCooldowns.afterOwnTurn()) },
)

/** 本人の手番終了後のCTの記録の一覧。 */
private fun List<SkillCooldown>.afterOwnTurn(): List<SkillCooldown> = mapNotNull { cooldown ->
    if (cooldown.startedDuringOwnTurn) {
        cooldown.copy(startedDuringOwnTurn = false)
    } else {
        cooldown.copy(remainingTurns = cooldown.remainingTurns - 1).takeIf { it.remainingTurns > 0 }
    }
}
