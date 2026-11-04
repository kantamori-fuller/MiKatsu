package jp.co.fuller.morikan.bootcamp.mikatsu.ui.components

import androidx.annotation.StringRes
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillElement
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.R

/**
 * 属性の表示名(文字列リソース)を返す。キャラ編集画面と戦闘の詳細ポップアップが同じ表示名を共有するための対応表。
 *
 * @receiver 表示名を求める属性。
 * @return 表示名の文字列リソースID。
 */
@get:StringRes
val SkillElement.labelRes: Int
    get() = when (this) {
        SkillElement.NONE -> R.string.element_none
        SkillElement.FIRE -> R.string.element_fire
        SkillElement.COLLAPSE -> R.string.element_collapse
        SkillElement.WATER -> R.string.element_water
        SkillElement.ICE -> R.string.element_ice
        SkillElement.LIFE -> R.string.element_life
        SkillElement.AIR -> R.string.element_air
        SkillElement.EARTH -> R.string.element_earth
        SkillElement.METAL -> R.string.element_metal
        SkillElement.THUNDER -> R.string.element_thunder
        SkillElement.MYSTIC -> R.string.element_mystic
        SkillElement.DEATH -> R.string.element_death
        SkillElement.EROSION -> R.string.element_erosion
        SkillElement.DRAGON -> R.string.element_dragon
    }
