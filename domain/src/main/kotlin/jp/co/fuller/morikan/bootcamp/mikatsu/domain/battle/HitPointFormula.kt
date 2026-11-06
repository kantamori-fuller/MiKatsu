package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

/**
 * 戦闘中の最大HPを求める計算式を提供する、状態を持たない純粋関数の置き場。
 *
 * 最大HPは「HPの種族値 + [BASE_HP]」とする。種族値([jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Character.hp]・
 * [jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Enemy.hp])や保存データは種族値のまま変えず、
 * 戦闘時の計算でだけ基礎HPを足すことで、既存のキャラクターにもそのまま新しい式が効くようにする。
 * 最大HPの式を1箇所に集約し、味方・敵・戦闘開始時の満タンの扱いが同じ式を共有できるようにすることを目的とする。
 */
object HitPointFormula {

    /** すべての個体に共通して最大HPへ加算する基礎HP。 */
    const val BASE_HP = 80

    /**
     * HPの種族値から戦闘中の最大HPを求める。
     *
     * @param hpBaseStat HPの種族値(キャラクター・敵のマスターデータが持つ値)。
     * @return 戦闘中の最大HP(種族値 + [BASE_HP])。
     */
    fun maxHpOf(hpBaseStat: Int): Int = hpBaseStat + BASE_HP
}
