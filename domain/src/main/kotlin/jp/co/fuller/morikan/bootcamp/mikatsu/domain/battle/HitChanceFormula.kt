package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import kotlin.random.Random

/**
 * 攻撃が当たるかどうかを決める命中判定の計算式。
 *
 * スキルの命中率に、攻撃者の命中率と対象の回避率による補正を掛けて最終的な命中率(%)を求め、
 * 乱数で当たり外れを決める。補正は「差し引きしてから掛ける」ため、命中+10と回避+10は打ち消し合う。
 * 計算を1箇所に集約し、攻撃処理から切り離してテストできるようにすることを目的とする。
 */
object HitChanceFormula {

    /** 命中率・回避率の基本値。全個体が戦闘開始時にこの値から始まる。 */
    const val BASE_RATE = 100

    /** 最終的な命中率(%)の上限。 */
    private const val MAX_CHANCE = 100.0

    /**
     * 最終的な命中率(%)を計算する。
     *
     * `スキルの命中率 × (攻撃者の命中率 − (対象の回避率 − 100)) / 100`を、0〜100に収めた値。
     * 端数は丸めず小数のまま返す。
     *
     * @param skillAccuracy スキルの命中率(0〜100)。`null`は必中を表し、判定しない。
     * @param attackerAccuracy 攻撃者の命中率。
     * @param targetEvasion 対象の回避率。
     * @return 最終的な命中率(0.0〜100.0)。スキルの命中率が`null`なら100.0。
     */
    fun finalChance(skillAccuracy: Int?, attackerAccuracy: Int, targetEvasion: Int): Double {
        if (skillAccuracy == null) return MAX_CHANCE
        val modifier = attackerAccuracy - (targetEvasion - BASE_RATE)
        return (skillAccuracy.toDouble() * modifier / 100.0).coerceIn(0.0, MAX_CHANCE)
    }

    /**
     * 命中判定を行い、当たったかどうかを返す。
     *
     * スキルの命中率が`null`(必中)のときは乱数を引かずに命中とする。それ以外は`[0, 100)`の乱数が
     * 最終的な命中率より小さければ命中なので、0%は必ず外れ、100%は必ず当たる。
     *
     * @param skillAccuracy スキルの命中率。`null`は必中。
     * @param attackerAccuracy 攻撃者の命中率。
     * @param targetEvasion 対象の回避率。
     * @param random 当たり外れを決める乱数。テストで結果を固定できるよう外から受け取る。
     * @return 命中したら`true`。
     */
    fun isHit(skillAccuracy: Int?, attackerAccuracy: Int, targetEvasion: Int, random: Random): Boolean {
        if (skillAccuracy == null) return true
        return random.nextDouble() * MAX_CHANCE < finalChance(skillAccuracy, attackerAccuracy, targetEvasion)
    }
}
