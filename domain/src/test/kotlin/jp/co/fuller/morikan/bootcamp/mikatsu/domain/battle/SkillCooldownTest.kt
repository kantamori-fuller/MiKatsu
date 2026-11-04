package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.SkillCooldown
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillCost
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillCostType
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillRange
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillTargetSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** スキルのクールタイム(CT)の設定・経過・使用制限を検証するテスト。 */
class SkillCooldownTest {

    /** CT1の攻撃スキル(基礎威力50・命中率80・MPだけ消費)。 */
    private val attackSkill = testActiveSkill(
        id = 20, name = "CT1の攻撃", targetSide = SkillTargetSide.ENEMY, basePower = 50, accuracy = 80, coolTime = 1,
    )

    /** CT2の効果スキル(ダメージなし・自分にバフ)。 */
    private val buffSkill = testActiveSkill(
        id = 21, name = "CT2のバフ", coolTime = 2, range = SkillRange.RANGE_0,
    )

    /** 味方1(素早さが非常に高く、自分の手番が続けて来る・手番中)と、とても遅い敵2。 */
    private fun field() = BattleField(
        allies = listOf(testAlly(1, agility = 9999, activeSkills = listOf(attackSkill, buffSkill, TEST_NORMAL_ATTACK))),
        enemies = listOf(testEnemy(2, agility = 1, hp = 100000)),
        turnQueue = listOf(1),
    )

    private fun BattleField.cooldownsOf(fpid: Int): List<SkillCooldown> = allPawns.first { it.fpid == fpid }.skillCooldowns

    /** CT付きのスキルを使うと、命中しても外れても、使った手番の最中に作られたCTが設定されること。 */
    @Test
    fun usingSkill_setsCooldown_hitOrMiss() {
        val hit = checkNotNull(field().performAttack(1, 2, attackSkill, SequenceRandom())).first
        val miss = checkNotNull(field().performAttack(1, 2, attackSkill, SequenceRandom(doubles = listOf(0.99)))).first

        assertEquals(listOf(SkillCooldown(20, 1, startedDuringOwnTurn = true)), hit.cooldownsOf(1))
        assertEquals(listOf(SkillCooldown(20, 1, startedDuringOwnTurn = true)), miss.cooldownsOf(1))
    }

    /** CT0のスキル(通常攻撃を含む)はCTを設定しないこと。 */
    @Test
    fun zeroCooldownSkill_setsNothing() {
        val after = checkNotNull(field().performAttack(1, 2, TEST_NORMAL_ATTACK, SequenceRandom())).first

        assertTrue(after.cooldownsOf(1).isEmpty())
    }

    /** CT中のスキルは、攻撃・効果のどちらでも使えず(null)、CT中でないスキルや通常攻撃は使えること。 */
    @Test
    fun skillOnCooldown_cannotBeUsed() {
        val used = checkNotNull(field().performAttack(1, 2, attackSkill, SequenceRandom())).first

        assertNull(used.performAttack(1, 2, attackSkill, SequenceRandom()))
        assertNull(used.performActiveSkill(1, attackSkill.id, 2, SequenceRandom()))
        assertNotNull(used.performAttack(1, 2, TEST_NORMAL_ATTACK, SequenceRandom()))

        val buffed = checkNotNull(field().performSkill(1, buffSkill.id, 1, SequenceRandom())).first
        assertNull(buffed.performSkill(1, buffSkill.id, 1, SequenceRandom()))
    }

    /** CT1: 使った手番の終わりでは減らず、次の自分の手番の間は使えず、その手番が終わると使えること。 */
    @Test
    fun cooldownOne_blocksUntilNextOwnTurnEnds() {
        val used = checkNotNull(field().performAttack(1, 2, attackSkill, SequenceRandom())).first

        val nextTurn = used.endTurn(1, SequenceRandom())
        // 使った手番の終わりでは減らず、フラグを下ろすだけ。
        assertEquals(listOf(SkillCooldown(20, 1, startedDuringOwnTurn = false)), nextTurn.cooldownsOf(1))
        assertEquals(1, nextTurn.currentActorFpid)
        assertNull(nextTurn.performAttack(1, 2, attackSkill, SequenceRandom()))

        val afterNext = nextTurn.endTurn(1, SequenceRandom())
        assertTrue(afterNext.cooldownsOf(1).isEmpty())
        assertEquals(1, afterNext.currentActorFpid)
        assertNotNull(afterNext.performAttack(1, 2, attackSkill, SequenceRandom()))
    }

    /** CT2: 使った後の自分の手番が2回終わるまで使えず、3回目の手番(使った手番を数えない)で使えること。 */
    @Test
    fun cooldownTwo_needsTwoFullOwnTurns() {
        var f = checkNotNull(field().performSkill(1, buffSkill.id, 1, SequenceRandom())).first

        f = f.endTurn(1, SequenceRandom()) // 使った手番
        assertEquals(2, f.cooldownsOf(1).single().remainingTurns)
        f = f.endTurn(1, SequenceRandom()) // 1回目
        assertEquals(1, f.cooldownsOf(1).single().remainingTurns)
        assertNull(f.performSkill(1, buffSkill.id, 1, SequenceRandom()))
        f = f.endTurn(1, SequenceRandom()) // 2回目
        assertTrue(f.cooldownsOf(1).isEmpty())
        assertNotNull(f.performSkill(1, buffSkill.id, 1, SequenceRandom()))
    }

    /** 他の個体の手番が終わってもCTは減らないこと(本人の手番だけを数える)。 */
    @Test
    fun otherPawnsTurn_doesNotProgressCooldown() {
        val used = checkNotNull(field().performAttack(1, 2, attackSkill, SequenceRandom())).first
        // 手番を味方1から敵2に入れ替え、敵の手番を終える。
        val enemyTurn = used.copy(turnQueue = listOf(2))

        val after = enemyTurn.endTurn(2, SequenceRandom())

        assertEquals(listOf(SkillCooldown(20, 1, startedDuringOwnTurn = true)), after.cooldownsOf(1))
    }

    /** コストを払えない・対象がいないなど、使えなかったときはCTも設定しないこと。 */
    @Test
    fun failedUse_doesNotSetCooldown() {
        val poor = field().let { it.copy(allies = it.allies.map { a -> a.copy(currentMp = 0) }) }
        val costly = attackSkill.copy(costs = listOf(SkillCost(SkillCostType.MP, 10)))

        assertNull(poor.performAttack(1, 2, costly, SequenceRandom()))
        assertTrue(poor.cooldownsOf(1).isEmpty())
    }
}
