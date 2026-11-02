package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleFieldPawn
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.PassiveSkillEffect

/** パッシブスキルによる軽減を適用した後でも、ダメージが下回らない最小値。 */
private const val MIN_DAMAGE_AFTER_REDUCTION = 1

/**
 * この個体が持つパッシブスキルによる、ダメージ軽減量の合計を求める。
 *
 * ダメージ軽減の適用を[reduceDamage]の1箇所に集約するため、合計の算出を切り出している。
 *
 * @receiver 軽減量を求める個体(ダメージを受ける側)。
 * @return [PassiveSkillEffect.DamageReduction]の軽減量の合計。持たなければ0。
 */
fun BattleFieldPawn.totalDamageReduction(): Int =
    passiveSkills
        .flatMap { it.effects }
        .sumOf { (it as? PassiveSkillEffect.DamageReduction)?.amount ?: 0 }

/**
 * ダメージ計算式の最終結果に、この個体のパッシブスキルによる軽減を適用する。
 *
 * 軽減が1以上ある場合は `max(damage - 軽減量の合計, 1)` とし、軽減によってダメージが0以下に
 * ならないようにする。軽減がなければダメージは変わらない(式の結果が0でも引き上げない)。
 * 通常攻撃・スキルを問わず「受けるダメージ」の軽減を共通の処理にすることを目的とする。
 *
 * @receiver ダメージを受ける個体。
 * @param damage [DamageFormula]で求めた、軽減前の最終ダメージ。
 * @return 軽減後のダメージ。
 */
fun BattleFieldPawn.reduceDamage(damage: Int): Int {
    val reduction = totalDamageReduction()
    return if (reduction > 0) (damage - reduction).coerceAtLeast(MIN_DAMAGE_AFTER_REDUCTION) else damage
}
