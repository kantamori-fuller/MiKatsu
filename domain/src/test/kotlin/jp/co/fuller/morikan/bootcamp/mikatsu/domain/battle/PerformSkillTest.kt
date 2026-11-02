package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.SkillResult
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.SkillTargetResult
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillArea
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillCost
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillCostType
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillRange
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillTargetSelection
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillTargetSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** [performSkill]の成立条件(手番・所持・MP・対象)と、回復・MP消費の効果を検証するテスト。 */
class PerformSkillTest {

    /** 味方1(手番・ディア所持・MP30)、味方2(HP50/100)、味方3(戦闘不能)、敵4を持つフィールド。 */
    private fun field(): BattleField = BattleField(
        allies = listOf(
            testAlly(1, mp = 30, activeSkills = listOf(TEST_DIA)),
            testAlly(2).copy(currentHp = 50),
            testAlly(3).copy(currentHp = 0),
        ),
        enemies = listOf(testEnemy(4)),
        turnQueue = listOf(1, 4),
    )

    /** 味方単体を50回復し、使用者のMPが10減り、結果が返ること。 */
    @Test
    fun heal_restoresHpAndDeductsMp() {
        val (after, result) = checkNotNull(field().performSkill(1, TEST_DIA.id, 2))

        assertEquals(100, after.allies[1].currentHp)
        assertEquals(20, after.allies[0].currentMp)
        assertEquals(SkillResult(1, 2, "ディア", listOf(SkillTargetResult(2, 50))), result)
    }

    /** 回復は最大HPで頭打ちになり、実際の回復量が結果に入ること。 */
    @Test
    fun heal_isCappedAtMaxHp() {
        val f = field().let { it.copy(allies = it.allies.map { a -> if (a.fpid == 2) a.copy(currentHp = 80) else a }) }

        val (after, result) = checkNotNull(f.performSkill(1, TEST_DIA.id, 2))

        assertEquals(100, after.allies[1].currentHp)
        assertEquals(20, result.totalHealed)
    }

    /** 自分自身も対象にでき、MP消費と回復の両方が反映されること。 */
    @Test
    fun heal_canTargetSelf() {
        val f = field().let { it.copy(allies = it.allies.map { a -> if (a.fpid == 1) a.copy(currentHp = 10) else a }) }

        val (after, result) = checkNotNull(f.performSkill(1, TEST_DIA.id, 1))

        assertEquals(60, after.allies[0].currentHp)
        assertEquals(20, after.allies[0].currentMp)
        assertEquals(50, result.totalHealed)
    }

    /** MPがちょうどコストと同じなら使え、使うと0になること。 */
    @Test
    fun exactMp_isUsable() {
        val f = field().let { it.copy(allies = it.allies.map { a -> if (a.fpid == 1) a.copy(currentMp = 10) else a }) }

        val (after, _) = checkNotNull(f.performSkill(1, TEST_DIA.id, 2))

        assertEquals(0, after.allies[0].currentMp)
    }

    /** MPが足りないと使えないこと。 */
    @Test
    fun insufficientMp_returnsNull() {
        val f = field().let { it.copy(allies = it.allies.map { a -> if (a.fpid == 1) a.copy(currentMp = 9) else a }) }

        assertNull(f.performSkill(1, TEST_DIA.id, 2))
    }

    /** 戦闘不能の味方は対象にできない(蘇生しない)こと。 */
    @Test
    fun defeatedTarget_returnsNull() {
        assertNull(field().performSkill(1, TEST_DIA.id, 3))
    }

    /** 敵は味方単体スキルの対象にできないこと。 */
    @Test
    fun enemyTarget_returnsNull() {
        assertNull(field().performSkill(1, TEST_DIA.id, 4))
    }

    /** 存在しないFPIDは対象にできないこと。 */
    @Test
    fun unknownTarget_returnsNull() {
        assertNull(field().performSkill(1, TEST_DIA.id, 99))
    }

    /** 現在の行動者でない味方は使えないこと。 */
    @Test
    fun notCurrentActor_returnsNull() {
        val f = field().copy(turnQueue = listOf(2, 1))
            .let { it.copy(allies = it.allies.map { a -> if (a.fpid == 2) a.copy(activeSkills = listOf(TEST_DIA), currentMp = 30) else a }) }

        assertNull(f.performSkill(1, TEST_DIA.id, 2))
        assertNotNull(f.performSkill(2, TEST_DIA.id, 1))
    }

    /** 設定していないスキルは使えないこと。 */
    @Test
    fun skillNotSet_returnsNull() {
        val f = field().let { it.copy(allies = it.allies.map { a -> if (a.fpid == 1) a.copy(activeSkills = emptyList()) else a }) }

        assertNull(f.performSkill(1, TEST_DIA.id, 2))
        assertNull(field().performSkill(1, 999, 2))
    }

    /** 敵は味方のスキルを使えないこと。 */
    @Test
    fun enemyActor_returnsNull() {
        assertNull(field().copy(turnQueue = listOf(4)).performSkill(4, TEST_DIA.id, 1))
    }

    /** 使用者が戦闘不能なら使えないこと。 */
    @Test
    fun defeatedActor_returnsNull() {
        val f = field().let { it.copy(allies = it.allies.map { a -> if (a.fpid == 1) a.copy(currentHp = 0) else a }) }

        assertNull(f.performSkill(1, TEST_DIA.id, 2))
    }

    /** HPコストとMPコストの両方が消費され、すべて払えるときだけ使えること。 */
    @Test
    fun hpAndMpCosts_areDeductedTogether() {
        val skill = TEST_DIA.copy(costs = listOf(SkillCost(SkillCostType.MP, 30), SkillCost(SkillCostType.HP, 10)))
        val f = field().let { it.copy(allies = it.allies.map { a -> if (a.fpid == 1) a.copy(activeSkills = listOf(skill)) else a }) }

        val (after, _) = checkNotNull(f.performSkill(1, skill.id, 2))

        assertEquals(0, after.allies[0].currentMp)
        assertEquals(90, after.allies[0].currentHp)
    }

    /** MPが足りなければ、HPが足りていても使えないこと(逆も同様)。 */
    @Test
    fun hpAndMpCosts_allMustBeAffordable() {
        val skill = TEST_DIA.copy(costs = listOf(SkillCost(SkillCostType.MP, 31), SkillCost(SkillCostType.HP, 10)))
        val f = field().let { it.copy(allies = it.allies.map { a -> if (a.fpid == 1) a.copy(activeSkills = listOf(skill)) else a }) }
        assertNull(f.performSkill(1, skill.id, 2))

        val hpHeavy = TEST_DIA.copy(costs = listOf(SkillCost(SkillCostType.HP, 100)))
        val g = field().let { it.copy(allies = it.allies.map { a -> if (a.fpid == 1) a.copy(activeSkills = listOf(hpHeavy)) else a }) }
        assertNull(g.performSkill(1, hpHeavy.id, 2)) // 払うとHPが0になる
        val ok = TEST_DIA.copy(costs = listOf(SkillCost(SkillCostType.HP, 99)))
        val h = field().let { it.copy(allies = it.allies.map { a -> if (a.fpid == 1) a.copy(activeSkills = listOf(ok)) else a }) }
        assertEquals(1, checkNotNull(h.performSkill(1, ok.id, 2)).first.allies[0].currentHp)
    }

    /** 自分に回復スキルを使うとき、HPコスト消費後のHPを基準に回復すること。 */
    @Test
    fun selfHeal_afterHpCost() {
        val skill = TEST_DIA.copy(costs = listOf(SkillCost(SkillCostType.HP, 10)))
        val f = field().let { it.copy(allies = it.allies.map { a -> if (a.fpid == 1) a.copy(activeSkills = listOf(skill)) else a }) }

        val (after, result) = checkNotNull(f.performSkill(1, skill.id, 1))

        assertEquals(10, result.totalHealed)
        assertEquals(100, after.allies[0].currentHp)
    }

    /** 陣営が合わないスキル(味方を選ぶスキルで敵を選ぶ、敵を選ぶスキルで味方を選ぶ)・基礎威力を持つスキルは、performSkillでは使えないこと。 */
    @Test
    fun enemySideOrDamagingSkill_returnsNull() {
        val enemySide = TEST_DIA.copy(targetSide = SkillTargetSide.ENEMY)
        val damaging = TEST_DIA.copy(basePower = 100)
        val f = field().let { it.copy(allies = it.allies.map { a -> if (a.fpid == 1) a.copy(activeSkills = listOf(enemySide, damaging.copy(id = 9))) else a }) }

        assertNull(f.performSkill(1, enemySide.id, 2))
        assertNull(f.performSkill(1, 9, 2))
    }

    /** エリア全体の全て対象スキルが、味方の生存個体全員を回復し(最大HPで頭打ち)、戦闘不能者は蘇生しないこと。 */
    @Test
    fun areaAll_healsEveryLivingAlly() {
        val skill = TEST_DIA.copy(targetSelection = SkillTargetSelection.ALL)
        val f = field().let { it.copy(allies = it.allies.map { a -> if (a.fpid == 1) a.copy(activeSkills = listOf(skill), currentHp = 90) else a }) }

        val (after, result) = checkNotNull(f.performSkill(1, skill.id, 2))

        assertEquals(listOf(100, 100, 0), after.allies.map { it.currentHp })
        assertEquals(
            listOf(SkillTargetResult(1, 10), SkillTargetResult(2, 50)),
            result.targets,
        )
        assertEquals(60, result.totalHealed)
        assertEquals(20, after.allies[0].currentMp)
    }

    /** 一体選択のスキルは、アンカー以外の味方を回復しないこと。 */
    @Test
    fun oneSelection_healsOnlyAnchor() {
        val f = field().let { it.copy(allies = it.allies.map { a -> if (a.fpid == 1) a.copy(currentHp = 10) else a }) }

        val (after, result) = checkNotNull(f.performSkill(1, TEST_DIA.id, 2))

        assertEquals(listOf(10, 100, 0), after.allies.map { it.currentHp })
        assertEquals(listOf(2), result.targets.map { it.targetFpid })
    }

    /** 同じ陣営の距離は最大1なので、レンジ1の回復でも後衛の味方を対象にできるが、敵は対象にできないこと。 */
    @Test
    fun rangeOne_reachesOwnBackRow_butNotEnemy() {
        val skill = TEST_DIA.copy(range = SkillRange.RANGE_1, area = SkillArea.SINGLE)
        val f = BattleField(
            allies = listOf(testAlly(1, mp = 30, activeSkills = listOf(skill), slot = 0), testAlly(2, slot = 4).copy(currentHp = 10)),
            enemies = listOf(testEnemy(3)),
            turnQueue = listOf(1),
        )

        assertNotNull(f.performSkill(1, skill.id, 2))
        assertNull(f.performSkill(1, skill.id, 3))
    }
}
