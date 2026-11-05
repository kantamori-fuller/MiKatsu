package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.StatusAilment
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkill
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkillEffect
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillTargetSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 精神系の状態異常による、先制行動の予約・取り消しの条件を検証するテスト。 */
class PreemptiveMentalAilmentTest {

    /** 味方向けの先制スキル(ディアと同じ回復に先制優先度を付けたもの)。 */
    private val allyPreemptive: ActiveSkill = TEST_DIA.copy(id = 40, name = "先制回復", preemptivePriority = 1, effects = listOf(ActiveSkillEffect.Heal(10)), targetSide = SkillTargetSide.ALLY)

    /** 先制スキルを持つ味方2([ailment]がかかっている)と、手番の味方1の戦闘。 */
    private fun field(ailment: StatusAilment?, skill: ActiveSkill = TEST_QUICK_SHOT): BattleField {
        val reserved = testAlly(2, mp = 100, activeSkills = listOf(skill)).copy(currentTp = 50)
        return BattleField(
            allies = listOf(testAlly(1), if (ailment == null) reserved else reserved.withAilments(ail(ailment))),
            enemies = listOf(testEnemy(10, agility = 1, hp = 100000)),
            turnQueue = listOf(1),
        )
    }

    /** 洗脳・発狂の間は予約できず、恐怖・(攻撃の先制スキルがある)挑発は予約できること。 */
    @Test
    fun canReserve_dependsOnMentalAilment() {
        assertTrue(field(null).canReservePreemptive(2))
        assertFalse(field(StatusAilment.BRAINWASH).canReservePreemptive(2))
        assertFalse(field(StatusAilment.MADNESS).canReservePreemptive(2))
        assertTrue(field(StatusAilment.FEAR).canReservePreemptive(2))
        assertTrue(field(StatusAilment.TAUNT).canReservePreemptive(2))
    }

    /** 魅了は反対の陣営を選べないので、敵向けの先制スキルしかなければ予約できず、味方向けなら予約できること。 */
    @Test
    fun charm_reservableOnlyWithAllyTargetingSkill() {
        assertFalse(field(StatusAilment.CHARM).canReservePreemptive(2))
        assertTrue(field(StatusAilment.CHARM, allyPreemptive).canReservePreemptive(2))
    }

    /** 挑発は基礎威力のない先制スキルしかなければ予約できないこと。 */
    @Test
    fun taunt_notReservableWithNonAttackPreemptiveSkill() {
        assertFalse(field(StatusAilment.TAUNT, allyPreemptive).canReservePreemptive(2))
    }

    /** すでに入っている先制の手番も、洗脳・発狂が先頭に来た時点で取り消されること。 */
    @Test
    fun preemptiveTurn_isCancelledForBrainwashAndMadness() {
        listOf(StatusAilment.BRAINWASH, StatusAilment.MADNESS).forEach { ailment ->
            val f = field(ailment).copy(turnQueue = listOf(2), preemptiveTurns = setOf(2))

            val (next, cancelled) = f.advanceToNextTurnWithCancellations(SequenceRandom())

            assertEquals("$ailment", listOf(2), cancelled)
            assertFalse("$ailment", next.isPreemptiveTurn)
        }
    }

    /** 恐怖の先制の手番は取り消されないこと。 */
    @Test
    fun preemptiveTurn_isNotCancelledForFear() {
        val f = field(StatusAilment.FEAR).copy(turnQueue = listOf(2), preemptiveTurns = setOf(2))

        val (next, cancelled) = f.advanceToNextTurnWithCancellations(SequenceRandom(ints = listOf(0, 99)))

        assertTrue(cancelled.isEmpty())
        assertTrue(next.isPreemptiveTurn)
    }
}
