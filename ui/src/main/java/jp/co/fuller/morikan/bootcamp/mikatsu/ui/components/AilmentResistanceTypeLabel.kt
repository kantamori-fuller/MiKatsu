package jp.co.fuller.morikan.bootcamp.mikatsu.ui.components

import androidx.annotation.StringRes
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AilmentResistanceType
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.R

/**
 * 状態異常耐性の項目名(文字列リソース)を返す。キャラ編集画面と戦闘の詳細ポップアップが同じ項目名を共有するための対応表。
 * 文言はドメインに持たせないため、ここ(ui)で対応づける。
 *
 * @receiver 項目名を求める耐性の項目。
 * @return 項目名の文字列リソースID。
 */
@get:StringRes
val AilmentResistanceType.labelRes: Int
    get() = when (this) {
        AilmentResistanceType.POISON -> R.string.battle_ailment_poison
        AilmentResistanceType.BURN -> R.string.battle_ailment_burn
        AilmentResistanceType.FROSTBITE -> R.string.battle_ailment_frostbite
        AilmentResistanceType.FREEZE -> R.string.battle_ailment_freeze
        AilmentResistanceType.INFECTION -> R.string.battle_ailment_infection
        AilmentResistanceType.BLEED -> R.string.battle_ailment_bleed
        AilmentResistanceType.PARALYSIS -> R.string.battle_ailment_paralysis
        AilmentResistanceType.SHOCK -> R.string.battle_ailment_shock
        AilmentResistanceType.INSTANT_DEATH -> R.string.battle_ailment_instant_death
        AilmentResistanceType.PETRIFY -> R.string.battle_ailment_petrify
        AilmentResistanceType.CONTAMINATION -> R.string.battle_ailment_contamination
        AilmentResistanceType.CURSE -> R.string.battle_ailment_curse
        AilmentResistanceType.SLEEP -> R.string.battle_ailment_sleep
        AilmentResistanceType.SKILL_SEAL -> R.string.ailment_resistance_skill_seal
        AilmentResistanceType.DIZZINESS -> R.string.battle_ailment_dizziness
        AilmentResistanceType.MENTAL -> R.string.ailment_resistance_mental
    }
