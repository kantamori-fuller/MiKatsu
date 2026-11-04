package jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model

import androidx.annotation.StringRes
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.StatusAilment
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.R

/**
 * 状態異常の表示名(文字列リソース)。ナレーションと詳細ポップアップが同じ表示名を共有するための対応表。
 *
 * @receiver 表示名を求める状態異常。
 * @return 表示名の文字列リソースID。
 */
@get:StringRes
val StatusAilment.labelRes: Int
    get() = when (this) {
        StatusAilment.STUN -> R.string.battle_ailment_stun
        StatusAilment.POISON -> R.string.battle_ailment_poison
        StatusAilment.BURN -> R.string.battle_ailment_burn
        StatusAilment.FROSTBITE -> R.string.battle_ailment_frostbite
        StatusAilment.FREEZE -> R.string.battle_ailment_freeze
        StatusAilment.INFECTION -> R.string.battle_ailment_infection
        StatusAilment.BLEED -> R.string.battle_ailment_bleed
        StatusAilment.PARALYSIS -> R.string.battle_ailment_paralysis
        StatusAilment.SHOCK -> R.string.battle_ailment_shock
        StatusAilment.PETRIFY -> R.string.battle_ailment_petrify
        StatusAilment.CONTAMINATION -> R.string.battle_ailment_contamination
        StatusAilment.CURSE -> R.string.battle_ailment_curse
        StatusAilment.SLEEP -> R.string.battle_ailment_sleep
        StatusAilment.MP_SEAL -> R.string.battle_ailment_mp_seal
        StatusAilment.TP_SEAL -> R.string.battle_ailment_tp_seal
        StatusAilment.DIZZINESS -> R.string.battle_ailment_dizziness
        StatusAilment.BRAND -> R.string.battle_ailment_brand
    }

/**
 * 状態異常の、カードに並べる短いラベル(文字列リソース)。名前が長いMP・TPスキル封印だけ短縮し、それ以外は[labelRes]と同じ。
 *
 * @receiver 表示名を求める状態異常。
 * @return 短いラベルの文字列リソースID。
 */
@get:StringRes
val StatusAilment.shortLabelRes: Int
    get() = when (this) {
        StatusAilment.MP_SEAL -> R.string.battle_ailment_short_mp_seal
        StatusAilment.TP_SEAL -> R.string.battle_ailment_short_tp_seal
        else -> labelRes
    }
