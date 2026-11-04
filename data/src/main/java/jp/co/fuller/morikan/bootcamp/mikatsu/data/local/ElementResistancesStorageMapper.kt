package jp.co.fuller.morikan.bootcamp.mikatsu.data.local

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.ElementResistanceFormula
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.ElementResistances

/**
 * [ElementResistances]を保存用のキーと値の組([Map])へ相互変換する。
 *
 * JSONの読み書き([CharacterLocalDataSource])から、キー名の規則と旧データ(属性耐性の追加前)の互換処理を切り離し、
 * Android依存のJSONクラスなしで検証できるようにすることを目的とする。キーは属性の列挙名(`FIRE`など)。
 */
internal object ElementResistancesStorageMapper {

    /**
     * 耐性を保存用のマップへ変換する。
     *
     * @param resistances 変換元の耐性。
     * @return 12属性ぶんの「列挙名 → 耐性値」。[ElementResistances.ORDER]の順。
     */
    fun toMap(resistances: ElementResistances): Map<String, Int> =
        ElementResistances.ORDER.associate { it.name to resistances.of(it) }

    /**
     * 保存されていたマップから耐性を復元する。
     *
     * 属性耐性の追加前に保存された旧データはマップ自体がなく、個々の属性のキーが欠けていることもあるため、
     * 読めない属性は等倍([ElementResistanceFormula.NEUTRAL])にする。
     *
     * @param values 保存されていた「列挙名 → 耐性値」。なければ`null`。
     * @return 復元した耐性。
     */
    fun fromMap(values: Map<String, Int>?): ElementResistances =
        ElementResistances.ORDER.fold(ElementResistances()) { resistances, element ->
            resistances.with(element, values?.get(element.name) ?: ElementResistanceFormula.NEUTRAL)
        }
}
