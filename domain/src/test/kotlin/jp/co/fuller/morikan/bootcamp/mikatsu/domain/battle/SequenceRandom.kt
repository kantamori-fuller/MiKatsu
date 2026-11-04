package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import kotlin.random.Random

/**
 * 乱数を引く値と順序を固定するテスト用の乱数。
 *
 * `nextInt(until)`は積んだ値を順に返し(クリティカル判定の24、TPの獲得量の幅3・4、同順位のくじ`shuffled`の幅も、この1本の列を使う)、
 * 尽きたら`until - 1`(クリティカルにならず、獲得量は範囲の最大、くじは並びが変わらない)を返す。
 * `nextDouble`は命中判定用で[doubles]を順に(尽きたら0.0=必ず命中)返す。
 *
 * @param ints `nextInt(until)`が順に返す値。
 * @param doubles `nextDouble()`が順に返す値。
 */
class SequenceRandom(ints: List<Int> = emptyList(), doubles: List<Double> = emptyList()) : Random() {
    private val intQueue = ArrayDeque(ints)
    private val doubleQueue = ArrayDeque(doubles)

    /** `nextInt(until)`を引いたときの`until`の記録(引いた順)。引く順序の確認に使う。 */
    val untils = mutableListOf<Int>()

    override fun nextBits(bitCount: Int): Int = error("nextDouble・nextInt(until)以外は使わない")

    override fun nextInt(until: Int): Int {
        untils += until
        return intQueue.removeFirstOrNull() ?: (until - 1)
    }

    override fun nextDouble(): Double = doubleQueue.removeFirstOrNull() ?: 0.0
}
