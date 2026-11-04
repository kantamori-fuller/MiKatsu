package jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.ResistanceOutcome
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.SkillResult

/**
 * スキルの結果を、1文ずつ順に表示するナレーションの列へ変換する。
 *
 * 全対象が命中し、ステータス変化もない(回復スキル)なら、従来どおり回復の1文にする。
 * そうでなければ、全対象に外れたときは「Aのスキル! しかし外れた!」の1文だけ、それ以外は「Aのスキル!」の後に、
 * 命中した対象ごと・かかったステータス変化ごとに「Bの俊敏が1段階下がった!」を1文ずつ並べる。
 * 属性耐性で無効になった対象は、対象が1体なら「Aのスキル! しかしBには効かなかった!」、複数なら「Bには効かなかった!」を並べる。
 * 反射された対象は「Bは効果を跳ね返した!」の後に、使用者にかかったステータス変化の文(使用者の耐性で無効なら「しかしAには効かなかった!」)を続ける。
 * 文の組み立てをViewModelの演出の進行から切り離し、単体でテストできるようにすることを目的とする。
 *
 * @receiver スキルの結果。
 * @param nameOf FPIDから表示名を引く関数。
 * @return 順に表示するナレーション。
 */
fun SkillResult.toNarrations(nameOf: (Int) -> String): List<BattleNarration> {
    val actorName = nameOf(actorFpid)
    if (targets.all { it.isHit && it.statChanges.isEmpty() && it.resistance == ResistanceOutcome.NONE }) {
        return listOf(healNarration(actorName, nameOf))
    }
    if (targets.none { it.isHit }) return listOf(BattleNarration.SkillMissed(actorName, skillName))
    val single = targets.singleOrNull()
    if (single != null && single.resistance == ResistanceOutcome.NULLIFIED) {
        return listOf(BattleNarration.SkillNullified(actorName, skillName, nameOf(single.targetFpid)))
    }
    return buildList {
        add(BattleNarration.SkillUsed(actorName, skillName))
        targets.filter { it.isHit }.forEach { target ->
            val targetName = nameOf(target.targetFpid)
            when (target.resistance) {
                ResistanceOutcome.NULLIFIED, ResistanceOutcome.ABSORBED -> add(BattleNarration.EffectNullified(targetName))
                ResistanceOutcome.REFLECTED -> {
                    add(BattleNarration.EffectReflected(targetName))
                    val reflected = checkNotNull(target.reflected)
                    if (reflected.isNullified) {
                        add(BattleNarration.EffectReflectedNullified(actorName))
                    } else {
                        reflected.statChanges.forEach { change ->
                            add(BattleNarration.StatStageChanged(actorName, change.stat, change.stages))
                        }
                    }
                }
                ResistanceOutcome.NONE -> target.statChanges.forEach { change ->
                    add(BattleNarration.StatStageChanged(targetName, change.stat, change.stages))
                }
            }
        }
    }
}

/**
 * 回復スキルの結果の1文を作る。対象が1体なら誰に何を使いいくつ回復したか、複数なら何体を合計いくつ回復したかを伝える。
 *
 * @receiver スキルの結果。
 * @param actorName 使用者の表示名。
 * @param nameOf FPIDから表示名を引く関数。
 * @return 回復のナレーション。
 */
private fun SkillResult.healNarration(actorName: String, nameOf: (Int) -> String): BattleNarration {
    val single = targets.singleOrNull()
    return if (single != null) {
        BattleNarration.SkillResult(
            actorName = actorName,
            targetName = nameOf(single.targetFpid),
            skillName = skillName,
            healedAmount = single.healedAmount,
        )
    } else {
        BattleNarration.MultiSkillResult(
            actorName = actorName,
            skillName = skillName,
            targetCount = targets.size,
            totalHealed = totalHealed,
        )
    }
}
