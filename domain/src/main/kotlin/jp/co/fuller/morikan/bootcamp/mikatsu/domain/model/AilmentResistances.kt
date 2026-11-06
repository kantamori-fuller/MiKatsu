package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.AilmentResistanceFormula

/**
 * 状態異常耐性([AilmentResistanceType]の16項目)の値をまとめたドメインモデル。キャラクターと敵が持つ。
 *
 * 値は[AilmentResistanceFormula.MIN]〜[AilmentResistanceFormula.MAX]の整数で、既定は[AilmentResistanceFormula.NEUTRAL](100)。
 * 計算は[AilmentResistanceFormula]が担い、このクラスは値を持つだけである。
 * 全項目が必ず値を持つよう、内部は[AilmentResistanceType]の全項目を埋めたMapで持つ。
 */
class AilmentResistances private constructor(private val values: Map<AilmentResistanceType, Int>) {

    /** 既定(全項目が[AilmentResistanceFormula.NEUTRAL])の耐性を作る。 */
    constructor() : this(AilmentResistanceType.entries.associateWith { AilmentResistanceFormula.NEUTRAL })

    /**
     * 指定した項目の耐性値を返す。
     *
     * @param type 調べる項目。
     * @return 耐性値。
     */
    fun of(type: AilmentResistanceType): Int = values.getValue(type)

    /**
     * 指定した項目の耐性値だけを差し替えたコピーを返す。
     *
     * @param type 差し替える項目。
     * @param value 新しい耐性値。
     * @return 差し替え後の耐性。
     */
    fun with(type: AilmentResistanceType, value: Int): AilmentResistances = AilmentResistances(values + (type to value))

    override fun equals(other: Any?): Boolean = other is AilmentResistances && values == other.values

    override fun hashCode(): Int = values.hashCode()

    override fun toString(): String = "AilmentResistances($values)"
}
