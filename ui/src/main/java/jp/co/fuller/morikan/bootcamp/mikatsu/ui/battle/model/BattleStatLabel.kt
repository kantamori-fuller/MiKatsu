package jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model

import androidx.annotation.StringRes
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleStat
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.R

/**
 * バフ・デバフの対象ステータスの表示名(文字列リソース)を返す。
 * ナレーションと詳細ポップアップが同じ表示名を共有するための対応表。
 *
 * @receiver 表示名を求めるステータス。
 * @return 表示名の文字列リソースID。
 */
@get:StringRes
val BattleStat.labelRes: Int
    get() = when (this) {
        BattleStat.ATTACK -> R.string.battle_stat_attack
        BattleStat.DEFENSE -> R.string.battle_stat_defense
        BattleStat.MANA_OUTPUT -> R.string.battle_stat_mana_output
        BattleStat.MANA_RESISTANCE -> R.string.battle_stat_mana_resistance
        BattleStat.AGILITY -> R.string.battle_stat_agility
        BattleStat.LUCK -> R.string.battle_stat_luck
        BattleStat.ACCURACY -> R.string.battle_detail_accuracy
        BattleStat.EVASION -> R.string.battle_detail_evasion
        BattleStat.CRITICAL_RATE -> R.string.battle_detail_critical_rate
        BattleStat.CRITICAL_DAMAGE -> R.string.battle_detail_critical_damage
    }
