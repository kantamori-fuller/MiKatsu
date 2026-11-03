package jp.co.fuller.morikan.bootcamp.mikatsu.ui

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkill
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkillEffect
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillArea
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillCategory
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillContactType
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillCost
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillElement
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillRange
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillTargetSelection
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillTargetSide

/**
 * テスト用のアクティブスキルを、必要な項目だけ指定して生成する。
 *
 * @param id スキルID。
 * @param name スキル名。
 * @param costs コスト。
 * @param targetSide 対象にする陣営。
 * @param range レンジ。
 * @param area 範囲。
 * @param targetSelection 対象選択。
 * @param basePower 基礎威力。
 * @param activationCount 発動回数。
 * @param accuracy 命中率。`null`は必中。
 * @param effects 効果。
 * @return 生成したアクティブスキル。
 */
fun testActiveSkill(
    id: Int,
    name: String,
    costs: List<SkillCost> = emptyList(),
    targetSide: SkillTargetSide = SkillTargetSide.ALLY,
    range: SkillRange = SkillRange.RANGE_1,
    area: SkillArea = SkillArea.SINGLE,
    targetSelection: SkillTargetSelection = SkillTargetSelection.ONE,
    basePower: Int? = null,
    activationCount: Int = 1,
    accuracy: Int? = null,
    effects: List<ActiveSkillEffect> = emptyList(),
) = ActiveSkill(
    id = id,
    name = name,
    element = SkillElement.NONE,
    categories = setOf(SkillCategory.HEAL),
    physicalAttribute = null,
    costs = costs,
    coolTime = 0,
    contactType = SkillContactType.NON_CONTACT,
    range = range,
    area = area,
    targetSelection = targetSelection,
    basePower = basePower,
    activationCount = activationCount,
    accuracy = accuracy,
    effects = effects,
    targetSide = targetSide,
)
