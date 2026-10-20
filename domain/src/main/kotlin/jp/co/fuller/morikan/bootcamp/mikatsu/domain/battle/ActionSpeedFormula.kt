package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import kotlin.math.pow

/**
 * 個体の素早さ(agility)から、行動順を決めるための「速度」を求める計算式を提供する、
 * 状態を持たない純粋関数の置き場。
 *
 * 速度は `素早さ ^ [SPEED_EXPONENT]` で求める。この式は「べき乗則」であり、2体の行動回数の比は
 * 素早さの比だけで決まる(素早さが6倍なら行動回数は約2倍、3倍なら約1.53倍)。素早さの絶対値には
 * 依存しないため、ステータスのインフレに強い。また、[SPEED_EXPONENT]を変えるだけで、
 * 素早さの差がどれだけ行動回数の差に効くかというバランスを調整できる。
 * 行動順の計算式を1箇所に集約し、[ActionTimeline]が同じ式を共有できるようにすることを目的とする。
 */
object ActionSpeedFormula {

    /** 速度の式 `素早さ ^ P` の指数P。 */
    const val SPEED_EXPONENT = 0.387

    /** 速度の計算に用いる素早さの下限。0(キャラ作成で未入力の項目)は1として扱う。 */
    const val MIN_AGILITY = 1

    /** 速度の計算に用いる素早さの上限。 */
    const val MAX_AGILITY = 9999

    /**
     * 素早さから速度を求める。
     *
     * 素早さは[MIN_AGILITY]〜[MAX_AGILITY]の範囲に収めてから計算する。
     *
     * @param agility 個体の素早さ。
     * @return 速度(常に正の値)。大きいほど多くの手番を得る。
     */
    fun speedOf(agility: Int): Double =
        agility.coerceIn(MIN_AGILITY, MAX_AGILITY).toDouble().pow(SPEED_EXPONENT)
}
