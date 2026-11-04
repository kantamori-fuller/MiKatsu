package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AilmentState
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleAlly
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleEnemy
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleFieldPawn
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.StatusAilment
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkill
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkillEffect
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillArea
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillCategory
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillRange
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillTargetSelection
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillTargetSide

/*
 * 状態異常のテストが共有する、状態異常の記録・スキル・個体の取り出しの補助。
 */

/**
 * テスト用に状態異常の記録を作る。
 *
 * @param ailment 状態異常の種類。
 * @param turns 残りターン。
 * @param granted 手番中にかけられたか。
 * @param denominator 感染の分母。
 * @return 状態異常の記録。
 */
fun ail(ailment: StatusAilment, turns: Int = 3, granted: Boolean = false, denominator: Int = 16) =
    AilmentState(ailment, turns, granted, denominator)

/**
 * 味方に状態異常を持たせたコピーを返す。
 *
 * @receiver 元の味方。
 * @param ailments かける状態異常。
 * @return 状態異常を持つ味方。
 */
fun BattleAlly.withAilments(vararg ailments: AilmentState): BattleAlly = copy(ailments = ailments.toList())

/**
 * 敵に状態異常を持たせたコピーを返す。
 *
 * @receiver 元の敵。
 * @param ailments かける状態異常。
 * @return 状態異常を持つ敵。
 */
fun BattleEnemy.withAilments(vararg ailments: AilmentState): BattleEnemy = copy(ailments = ailments.toList())

/**
 * フィールドから指定したFPIDの個体を取り出す。
 *
 * @receiver 調べる戦闘フィールド。
 * @param fpid 個体のFPID。
 * @return 個体。
 */
fun BattleField.pawn(fpid: Int): BattleFieldPawn = allPawns.first { it.fpid == fpid }

/**
 * 個体が持つ状態異常の種類だけを返す。
 *
 * @receiver 調べる個体。
 * @return 状態異常の種類(enum順)。
 */
fun BattleFieldPawn.ailmentKinds(): List<StatusAilment> = ailments.map { it.ailment }

/**
 * 状態異常の付与・即死を効果に持つ、敵に使うネガティブエフェクトのテスト用スキルを作る。
 *
 * @param effects 効果。
 * @param accuracy 命中率。`null`は必中。
 * @param activationCount 発動回数。
 * @param area 範囲。
 * @param targetSelection 対象選択。
 * @param targetSide 対象にする陣営。
 * @param range レンジ。
 * @return テスト用スキル(Aスキル8)。
 */
fun ailmentSkill(
    vararg effects: ActiveSkillEffect,
    accuracy: Int? = null,
    activationCount: Int = 1,
    area: SkillArea = SkillArea.SINGLE,
    targetSelection: SkillTargetSelection = SkillTargetSelection.ONE,
    targetSide: SkillTargetSide = SkillTargetSide.ENEMY,
    range: SkillRange = SkillRange.RANGE_3,
): ActiveSkill = testActiveSkill(
    id = 8,
    name = "テスト異常",
    targetSide = targetSide,
    range = range,
    area = area,
    targetSelection = targetSelection,
    accuracy = accuracy,
    activationCount = activationCount,
    effects = effects.toList(),
    categories = setOf(SkillCategory.NEGATIVE_EFFECT),
)
