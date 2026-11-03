package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.CriticalType
import kotlin.random.Random

/**
 * クリティカルヒット(バッドクリティカルを含む)の発生判定と、ダメージ倍率の計算式を集約した唯一の実装箇所。
 *
 * 発生判定([judge])でクリティカルかバッドクリティカルのどちらかが出たヒットだけ、倍率を求めてダメージに掛ける。
 * ヒットの最終的な種別([CriticalType])は、どちらの判定が出たかではなく、最終的な倍率で決まる([classify])。
 * 発生率は段階([MIN_RATE_STAGE]〜[MAX_RATE_STAGE])から、分母[RATE_DENOMINATOR]に対する分子[rateNumerator]を求めて乱数で判定する。
 * 分子が正ならクリティカル、負ならバッドクリティカル(通常のクリティカルは起きない)になる。
 * 倍率は段階([MIN_DAMAGE_STAGE]〜[MAX_DAMAGE_STAGE])から求め、整数演算で切り捨てる。
 * 計算を1箇所に集め、攻撃処理から切り離してテストできるようにすることを目的とする。
 */
object CriticalFormula {

    /** クリティカル発生率の段階の下限。 */
    const val MIN_RATE_STAGE = -6

    /** クリティカル発生率の段階の上限。 */
    const val MAX_RATE_STAGE = 6

    /** クリティカル倍率の段階の下限。 */
    const val MIN_DAMAGE_STAGE = -12

    /** クリティカル倍率の段階の上限。 */
    const val MAX_DAMAGE_STAGE = 12

    /** 発生率の分母。 */
    const val RATE_DENOMINATOR = 24

    /** 詳細ポップアップに表示する、クリティカル発生率の基本値の分子(段階0。分母は[RATE_DENOMINATOR])。計算には使わない。 */
    const val BASE_RATE_NUMERATOR = 1

    /** クリティカル倍率(%)の基本値。全個体が戦闘中だけ持つ固定値で、段階の増減はこれに加算される。 */
    const val BASE_DAMAGE_RATE = 150

    /** 倍率の段階1つあたりに増減する倍率(%)。 */
    private const val DAMAGE_RATE_PER_STAGE = 25

    /** バッドクリティカルのとき倍率から差し引く値(%)。 */
    private const val BAD_CRITICAL_PENALTY = 75

    /** この倍率(%)未満はバッドクリティカル、以上はクリティカルとして扱う境界。 */
    const val CRITICAL_THRESHOLD_RATE = 100

    /** 倍率(%)を割合へ直すための値。 */
    private const val PERCENT = 100

    /**
     * 発生率の段階から、発生率の分子(分母は[RATE_DENOMINATOR])を求める。
     *
     * n=0 → 1、n>0 → 4n、n<0 → -(4|n|-2)。正の値はクリティカル、負の値はバッドクリティカルの発生率(の符号付き分子)を表す。
     *
     * @param stage 発生率の有効段階。範囲外は[MIN_RATE_STAGE]〜[MAX_RATE_STAGE]に収める。
     * @return 符号付きの分子。
     */
    fun rateNumerator(stage: Int): Int {
        val n = stage.coerceIn(MIN_RATE_STAGE, MAX_RATE_STAGE)
        return when {
            n == 0 -> BASE_RATE_NUMERATOR
            n > 0 -> 4 * n
            else -> -(4 * -n - 2)
        }
    }

    /**
     * 命中したヒット1つについて、クリティカルの発生判定を行う。
     *
     * ここで返す[CriticalType]は「どちらの判定が出たか」で、ヒットの最終的な種別ではない(最終的な種別は[classify]が倍率で決める)。
     *
     * 分子が正なら`random.nextInt(24) < 分子`でクリティカル、負なら`random.nextInt(24) < |分子|`でバッドクリティカルとする。
     * 乱数は必ず1回引く。
     *
     * @param rateStage 攻撃者のクリティカル発生率の有効段階。
     * @param random 判定の乱数。テストで結果を固定できるよう外から受け取る。
     * @return 判定結果。クリティカルの判定、バッドクリティカルの判定、発生しない([CriticalType.NONE])のいずれか。
     */
    fun judge(rateStage: Int, random: Random): CriticalType {
        val numerator = rateNumerator(rateStage)
        val occurred = random.nextInt(RATE_DENOMINATOR) < kotlin.math.abs(numerator)
        return when {
            !occurred -> CriticalType.NONE
            numerator > 0 -> CriticalType.CRITICAL
            else -> CriticalType.BAD_CRITICAL
        }
    }

    /**
     * クリティカル倍率(%)を求める。
     *
     * `max(基本倍率 + 25 × 段階, 0)`。バッドクリティカルのときはさらに`max(倍率 - 75, 0)`。
     *
     * @param baseRate 基本倍率(%)。通常は[BASE_DAMAGE_RATE]。
     * @param damageStage 攻撃者のクリティカル倍率の有効段階。範囲外は[MIN_DAMAGE_STAGE]〜[MAX_DAMAGE_STAGE]に収める。
     * @param isBad バッドクリティカルか。
     * @return 倍率(%)。0以上。
     */
    fun damageRate(baseRate: Int, damageStage: Int, isBad: Boolean): Int {
        val stage = damageStage.coerceIn(MIN_DAMAGE_STAGE, MAX_DAMAGE_STAGE)
        val rate = (baseRate + DAMAGE_RATE_PER_STAGE * stage).coerceAtLeast(0)
        return if (isBad) (rate - BAD_CRITICAL_PENALTY).coerceAtLeast(0) else rate
    }

    /**
     * 最終的な倍率(%)から、ヒットの種別を決める。
     *
     * 倍率が[CRITICAL_THRESHOLD_RATE]未満なら[CriticalType.BAD_CRITICAL]、以上(ちょうど100も含む)なら[CriticalType.CRITICAL]。
     * どちらの発生判定が出たかは問わない。
     *
     * @param damageRate [damageRate]で求めた最終的な倍率(%)。
     * @return ヒットの種別。[CriticalType.NONE]にはならない。
     */
    fun classify(damageRate: Int): CriticalType =
        if (damageRate < CRITICAL_THRESHOLD_RATE) CriticalType.BAD_CRITICAL else CriticalType.CRITICAL

    /**
     * 発生判定の結果と段階から、ヒットの最終的な種別を決める。
     *
     * 判定が出なかった(NONE)ならNONE。出ていれば[damageRate]で最終的な倍率を求め、[classify]で分類する。
     *
     * @param judged [judge]の判定結果。
     * @param baseRate 基本倍率(%)。
     * @param damageStage 攻撃者のクリティカル倍率の有効段階。
     * @return ヒットの最終的な種別。
     */
    fun finalType(judged: CriticalType, baseRate: Int, damageStage: Int): CriticalType {
        if (judged == CriticalType.NONE) return CriticalType.NONE
        return classify(damageRate(baseRate, damageStage, isBad = judged == CriticalType.BAD_CRITICAL))
    }

    /**
     * ダメージにクリティカルの倍率を適用する。
     *
     * クリティカル・バッドクリティカルなら`max(floor(ダメージ × 倍率% / 100), 1)`(最低1)。通常ヒットはそのまま返す。
     *
     * @param damage [DamageFormula]の最終結果。
     * @param type [judge]の判定結果(倍率の計算にバッドクリティカルの-75を含めるかを決める)。
     * @param baseRate 基本倍率(%)。
     * @param damageStage 攻撃者のクリティカル倍率の有効段階。
     * @return 倍率適用後のダメージ。
     */
    fun applyDamage(damage: Int, type: CriticalType, baseRate: Int, damageStage: Int): Int {
        if (type == CriticalType.NONE) return damage
        val rate = damageRate(baseRate, damageStage, isBad = type == CriticalType.BAD_CRITICAL)
        return (damage.toLong() * rate / PERCENT).coerceIn(1L, Int.MAX_VALUE.toLong()).toInt()
    }
}
