package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.SkillResult
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
        assertEquals(SkillResult(1, 1, "ディア", 2, 50), result)
    }

    /** 回復は最大HPで頭打ちになり、実際の回復量が結果に入ること。 */
    @Test
    fun heal_isCappedAtMaxHp() {
        val f = field().let { it.copy(allies = it.allies.map { a -> if (a.fpid == 2) a.copy(currentHp = 80) else a }) }

        val (after, result) = checkNotNull(f.performSkill(1, TEST_DIA.id, 2))

        assertEquals(100, after.allies[1].currentHp)
        assertEquals(20, result.healedAmount)
    }

    /** 自分自身も対象にでき、MP消費と回復の両方が反映されること。 */
    @Test
    fun heal_canTargetSelf() {
        val f = field().let { it.copy(allies = it.allies.map { a -> if (a.fpid == 1) a.copy(currentHp = 10) else a }) }

        val (after, result) = checkNotNull(f.performSkill(1, TEST_DIA.id, 1))

        assertEquals(60, after.allies[0].currentHp)
        assertEquals(20, after.allies[0].currentMp)
        assertEquals(50, result.healedAmount)
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
}
