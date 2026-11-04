package jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.StatusAilment
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.TurnEndEvent

/**
 * 手番終了時の状態異常の出来事を、ナレーションと表示するフィールドの組([NarrationStep])へ変換する。
 *
 * 各出来事は直後のフィールドを持つので、1文ごとにそのフィールドを表示させ、ナレーションとHPなどの表示を連動させられる。
 * 文面は、ダメージが「Bは毒で12ダメージ」、MP・TPが「BはMPが5減った」「BはTPが12減った」、伝染が「Bの感染がCにうつった!」、
 * 解除が「Bの毒が治った」、戦闘不能が「Bは戦闘不能になった!」。
 *
 * @receiver 手番終了時の状態異常の出来事。
 * @param nameOf FPIDから表示名を引く関数。
 * @return 表示するナレーションと、その文を出すときに表示するフィールド。
 */
fun TurnEndEvent.toNarrationStep(nameOf: (Int) -> String): NarrationStep = when (this) {
    is TurnEndEvent.HpDamage -> NarrationStep(BattleNarration.AilmentHpDamage(nameOf(fpid), ailment, damage), field)
    is TurnEndEvent.MpLoss -> NarrationStep(BattleNarration.AilmentMpLoss(nameOf(fpid), amount), field)
    is TurnEndEvent.TpLoss -> NarrationStep(BattleNarration.AilmentTpLoss(nameOf(fpid), amount), field)
    is TurnEndEvent.Spread -> NarrationStep(BattleNarration.AilmentSpread(nameOf(fromFpid), nameOf(toFpid)), field)
    is TurnEndEvent.Cured -> NarrationStep(BattleNarration.AilmentCured(nameOf(fpid), ailment), field)
    is TurnEndEvent.Defeated -> NarrationStep(BattleNarration.UnitDefeated(nameOf(fpid)), field)
}

/**
 * 攻撃のヒットで解除された睡眠・凍結を、「Bは目を覚ました!」「Bの凍結が解けた!」の文へ変換する。
 *
 * @param name 解除された個体(反射では攻撃者)の表示名。
 * @param cured 解除された状態異常。
 * @return 該当する文。なければ空。
 */
internal fun curedNarrations(name: String, cured: List<StatusAilment>): List<BattleNarration> =
    cured.map { BattleNarration.AilmentReleasedByDamage(name, it) }
