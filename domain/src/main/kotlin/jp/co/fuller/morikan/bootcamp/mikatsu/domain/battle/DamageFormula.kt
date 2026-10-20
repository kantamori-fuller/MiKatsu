package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import java.math.BigInteger

/**
 * 通常攻撃などのダメージ量を求める計算式を提供する、状態を持たない純粋関数の置き場。
 *
 * 計算式は `((技威力 * A / B) / (50 / 11) + 2)` である。ここで A は攻撃者の破壊力、
 * B は被攻撃者の耐久力を指す。ダメージ計算式を1箇所に集約し、UseCaseや将来の技ごとの
 * 威力の違いが同じ式を共有できるようにすることを目的とする。
 */
object DamageFormula {

    /** 式中の `50 / 11` の分子。 */
    private const val DIVISOR_NUMERATOR = 50L

    /** 式中の `50 / 11` の分母。 */
    private const val DIVISOR_DENOMINATOR = 11L

    /** 最終ダメージに加算される固定値。 */
    private const val BASE_DAMAGE = 2L

    /**
     * 技の威力・攻撃者の破壊力・被攻撃者の耐久力からダメージ量を計算する。
     *
     * 浮動小数点数は用いず、式を通分した `power * attack * 11 / (defense * 50)` を
     * 整数同士の除算で求め、最後に固定値の2を加算する。途中の値は切り捨てず、実数としての
     * 最終値(固定値の加算前)を一度だけ切り捨てる丸め方針とする。Intで`50 / 11`と書くと4に
     * なって式が壊れること、およびDoubleでは境界値で誤差により1ずれ得ることを避けるための
     * 設計である。極端に大きな値でも溢れないよう、中間計算は[BigInteger]で行う。
     *
     * 耐久力は0(キャラ作成で未入力の項目)になり得るため、0除算を避けて1以上として扱う。
     * 威力・破壊力が負の場合は0として扱う。
     *
     * @param power 技の威力。
     * @param attack 攻撃者の破壊力(A)。
     * @param defense 被攻撃者の耐久力(B)。
     * @return 算出したダメージ量。[Int.MAX_VALUE]を超える場合は[Int.MAX_VALUE]に丸める。
     */
    fun calculate(power: Int, attack: Int, defense: Int): Int {
        val numerator = BigInteger.valueOf(power.coerceAtLeast(0).toLong())
            .multiply(BigInteger.valueOf(attack.coerceAtLeast(0).toLong()))
            .multiply(BigInteger.valueOf(DIVISOR_DENOMINATOR))
        val denominator = BigInteger.valueOf(defense.coerceAtLeast(1).toLong())
            .multiply(BigInteger.valueOf(DIVISOR_NUMERATOR))
        val damage = numerator.divide(denominator).add(BigInteger.valueOf(BASE_DAMAGE))
        return damage.min(BigInteger.valueOf(Int.MAX_VALUE.toLong())).toInt()
    }
}
