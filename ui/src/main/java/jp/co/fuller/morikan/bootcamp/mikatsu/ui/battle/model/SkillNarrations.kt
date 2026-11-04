package jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.EffectApplication
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.ResistanceOutcome
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.SkillResult

/**
 * スキルの結果を、1文ずつ順に表示するナレーションの列へ変換する。
 *
 * 全対象が命中し、ステータス変化も状態異常・即死もない(回復スキル)なら、従来どおり回復の1文にする(発動回数1のときだけ)。
 * そうでなければ、全対象に外れたときは「Aのスキル! しかし外れた!」の1文だけ、それ以外は「Aのスキル!」の後に、
 * 命中した対象ごと・かかったステータス変化ごとに「Bの俊敏が1段階下がった!」を1文ずつ並べる。
 * 状態異常は成功したものを「Bは毒になった!」、即死の成功を「Bは即死した!」と並べ、失敗(成功判定外れ・排他・石化で弾かれた)は、
 * すべて失敗なら「しかしBには効かなかった!」、一部だけなら成功分の文の後に「Bには毒が効かなかった!」を続ける。
 * 属性耐性で無効になった対象は、対象が1体なら「Aのスキル! しかしBには効かなかった!」、複数なら「Bには効かなかった!」を並べる。
 * 反射された対象は「Bは効果を跳ね返した!」の後に、使用者にかかったステータス変化・状態異常の文(使用者の耐性で無効なら「しかしAには効かなかった!」)を続ける。
 * 発動回数が2以上(結果が2回以上ある)ときは、攻撃と同じく回ごとに報告し、各文に「N回目: 」を付ける([BattleNarration.InRound])。
 * その回に外れた対象は「Bには外れた!」で伝える。
 * 文の組み立てをViewModelの演出の進行から切り離し、単体でテストできるようにすることを目的とする。
 *
 * @receiver スキルの結果。
 * @param nameOf FPIDから表示名を引く関数。
 * @return 順に表示するナレーション。
 */
fun SkillResult.toNarrations(nameOf: (Int) -> String): List<BattleNarration> {
    val actorName = nameOf(actorFpid)
    val isMultiRound = roundCount >= 2
    if (!isMultiRound &&
        targets.all { it.isHit && it.statChanges.isEmpty() && it.effectResults.isEmpty() && it.resistance == ResistanceOutcome.NONE }
    ) {
        return listOf(healNarration(actorName, nameOf))
    }
    if (targets.none { it.isHit }) return listOf(BattleNarration.SkillMissed(actorName, skillName))
    val single = targets.singleOrNull()
    if (single != null && single.resistance == ResistanceOutcome.NULLIFIED) {
        return listOf(BattleNarration.SkillNullified(actorName, skillName, nameOf(single.targetFpid)))
    }
    return buildList {
        add(BattleNarration.SkillUsed(actorName, skillName))
        for (round in 0 until roundCount) {
            val roundTargets = targets.filter { it.round == round }
            val sentences = buildList<BattleNarration> {
                if (isMultiRound) {
                    val missed = roundTargets.filter { !it.isHit }
                    if (missed.isNotEmpty()) add(BattleNarration.AreaMissed(missed.map { nameOf(it.targetFpid) }))
                }
                roundTargets.filter { it.isHit }.forEach { target ->
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
                                addAll(effectNarrations(actorName, reflected.effectResults))
                            }
                        }
                        ResistanceOutcome.NONE -> {
                            target.statChanges.forEach { change ->
                                add(BattleNarration.StatStageChanged(targetName, change.stat, change.stages))
                            }
                            addAll(effectNarrations(targetName, target.effectResults))
                        }
                    }
                }
            }
            sentences.forEach { add(if (isMultiRound) BattleNarration.InRound(round + 1, it) else it) }
        }
    }
}

/**
 * 状態異常の付与・即死の結果を、対象1体分の文へ変換する。
 *
 * 成功したものを効果の順に「Bは毒になった!」「Bは即死した!」と並べ、失敗がある場合は、すべて失敗なら「しかしBには効かなかった!」の1文、
 * 一部だけなら成功分の後に「Bには毒が効かなかった!」(即死は「即死」)を続ける。効果がなければ空。
 *
 * @param targetName 効果を受けた個体(反射では使用者)の表示名。
 * @param results 効果ごとの判定結果。
 * @return 順に表示する文。
 */
private fun effectNarrations(targetName: String, results: List<EffectApplication>): List<BattleNarration> {
    if (results.isEmpty()) return emptyList()
    if (results.none { it.succeeded }) return listOf(BattleNarration.AilmentAllFailed(targetName))
    return results.filter { it.succeeded }.map {
        when (it) {
            is EffectApplication.Ailment -> BattleNarration.AilmentInflicted(targetName, it.ailment)
            is EffectApplication.InstantDeath -> BattleNarration.InstantDeathSucceeded(targetName)
        }
    } + results.filter { !it.succeeded }.map {
        when (it) {
            is EffectApplication.Ailment -> BattleNarration.AilmentFailed(targetName, it.ailment)
            is EffectApplication.InstantDeath -> BattleNarration.AilmentFailed(targetName, null)
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
