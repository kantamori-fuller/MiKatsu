package jp.co.fuller.morikan.bootcamp.mikatsu.data.local

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.AilmentResistanceFormula
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AilmentResistanceType
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AilmentResistances

/**
 * [AilmentResistances]を保存用のキーと値の組([Map])へ相互変換する。
 *
 * JSONの読み書き([CharacterLocalDataSource])から、キー名の規則と旧データ(状態異常耐性の追加前)の互換処理を切り離し、
 * Android依存のJSONクラスなしで検証できるようにすることを目的とする。キーは[AilmentResistanceType]の列挙名(`POISON`など)。
 */
internal object AilmentResistancesStorageMapper {

    /**
     * 耐性を保存用のマップへ変換する。
     *
     * @param resistances 変換元の耐性。
     * @return 16項目ぶんの「列挙名 → 耐性値」。[AilmentResistanceType]の宣言順。
     */
    fun toMap(resistances: AilmentResistances): Map<String, Int> =
        AilmentResistanceType.entries.associate { it.name to resistances.of(it) }

    /**
     * 保存されていたマップから耐性を復元する。
     *
     * 旧データはマップ自体がなく、個々の項目のキーが欠けていることもあるため、読めない項目は100にする。
     * 範囲外の値は[AilmentResistanceFormula.clamp]で0〜100に収める。
     *
     * @param values 保存されていた「列挙名 → 耐性値」。なければ`null`。
     * @return 復元した耐性。
     */
    fun fromMap(values: Map<String, Int>?): AilmentResistances =
        AilmentResistanceType.entries.fold(AilmentResistances()) { resistances, type ->
            resistances.with(type, AilmentResistanceFormula.clamp(values?.get(type.name) ?: AilmentResistanceFormula.NEUTRAL))
        }
}
