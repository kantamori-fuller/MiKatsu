package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.StatusAilment
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.SkillUseResult
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkill
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkillEffect
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillArea
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillRange
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillTargetSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 魅了(反対の陣営を対象に選べない)・挑発(攻撃技しか使えず防御もできない)・何もできない手番の判定を検証するテスト。 */
class CharmTauntTest {

    /** 全域に毒をかける、フィールド全体・全て対象のネガティブエフェクトスキル(範囲FIELDでも反対陣営を巻き込まないことの確認用)。 */
    private val fieldPoison: ActiveSkill = ailmentSkill(
        ActiveSkillEffect.InflictAilment(StatusAilment.POISON, 3, 100),
        area = SkillArea.FIELD,
        targetSelection = SkillTargetSelection.ALL,
    )

    /** 敵に使うレンジ3・単体の攻撃スキル(基礎威力50)。 */
    private val shot: ActiveSkill = TEST_QUICK_SHOT.copy(id = 22, preemptivePriority = null, range = SkillRange.RANGE_3, area = SkillArea.SINGLE, targetSelection = SkillTargetSelection.ONE)

    private fun field(vararg ailments: jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AilmentState, skills: List<ActiveSkill> = listOf(TEST_DIA, shot, fieldPoison)) = BattleField(
        allies = listOf(testAlly(1, mp = 100, activeSkills = skills).withAilments(*ailments).copy(currentTp = 100), testAlly(2, slot = 1)),
        enemies = listOf(testEnemy(10, hp = 1000), testEnemy(11, hp = 1000, slot = 1)),
        turnQueue = listOf(1),
    )

    private fun BattleField.targetIds(skill: ActiveSkill) = selectableTargets(1, skill).map { it.fpid }

    /** 魅了中は、反対の陣営の個体を選べず(範囲がFIELDでも)、自陣営(自分を含む)だけが候補になること。 */
    @Test
    fun charm_cannotSelectOppositeSide_evenForFieldSkill() {
        val charmed = field(ail(StatusAilment.CHARM))

        assertEquals(listOf(1, 2, 10, 11), field().targetIds(fieldPoison))
        assertEquals(listOf(1, 2), charmed.targetIds(fieldPoison))
        assertTrue(charmed.targetIds(TEST_NORMAL_ATTACK).isEmpty())
        assertTrue(charmed.targetIds(shot).isEmpty())
        assertEquals(listOf(1, 2), charmed.targetIds(TEST_DIA))
    }

    /** 魅了中は、フィールド全体・全て対象のスキルも反対の陣営を巻き込まないこと。 */
    @Test
    fun charm_fieldAreaSkill_doesNotInvolveOppositeSide() {
        val charmed = field(ail(StatusAilment.CHARM))

        val (after, result) = checkNotNull(charmed.performSkill(1, fieldPoison.id, 2, SequenceRandom()))

        assertEquals(listOf(1, 2), result.targets.map { it.targetFpid }.distinct())
        assertTrue(after.pawn(10).ailments.isEmpty() && after.pawn(11).ailments.isEmpty())
        assertEquals(StatusAilment.POISON, after.pawn(2).ailments.single().ailment)
    }

    /** 魅了中は通常攻撃・敵向けのスキルが使えず(performAttackもnull)、味方向けのスキルと防御は使えること。 */
    @Test
    fun charm_blocksEnemyActions_butAllowsAllySkillsAndDefense() {
        val charmed = field(ail(StatusAilment.CHARM))

        assertNull(charmed.performAttack(1, 10, TEST_NORMAL_ATTACK, SequenceRandom()))
        assertNull(charmed.performActiveSkill(1, shot.id, 10, SequenceRandom()))
        assertNotNull(charmed.performActiveSkill(1, TEST_DIA.id, 2, SequenceRandom()))
        assertNotNull(charmed.defend(1))
        assertFalse(charmed.isSkillUsable(1, TEST_NORMAL_ATTACK))
        assertFalse(charmed.isSkillUsable(1, shot))
        assertTrue(charmed.isSkillUsable(1, TEST_DIA))
    }

    /** 挑発中は、基礎威力のないスキル(performSkill)と防御が`null`で、攻撃技(通常攻撃を含む)は使えること。 */
    @Test
    fun taunt_blocksNonAttackSkillsAndDefense_butAllowsAttacks() {
        val taunted = field(ail(StatusAilment.TAUNT))

        assertNull(taunted.performSkill(1, TEST_DIA.id, 2, SequenceRandom()))
        assertNull(taunted.performActiveSkill(1, TEST_DIA.id, 2, SequenceRandom()))
        assertNull(taunted.defend(1))
        assertTrue(taunted.performAttack(1, 10, TEST_NORMAL_ATTACK, SequenceRandom()) != null)
        assertTrue(taunted.performActiveSkill(1, shot.id, 10, SequenceRandom()) is Pair<*, *>)
        assertTrue(taunted.performActiveSkill(1, shot.id, 10, SequenceRandom())!!.second is SkillUseResult.Attack)
        assertFalse(taunted.isSkillUsable(1, TEST_DIA))
        assertTrue(taunted.isSkillUsable(1, shot))
        assertTrue(taunted.isSkillUsable(1, TEST_NORMAL_ATTACK))
    }

    /** 発狂・洗脳で自動で動いている間は、挑発の制限が効かないこと(判定の優先順位)。 */
    @Test
    fun autoActing_ignoresTauntRestriction() {
        val madAndTaunted = field(ail(StatusAilment.MADNESS), ail(StatusAilment.TAUNT))

        assertNotNull(madAndTaunted.performSkill(1, TEST_DIA.id, 2, SequenceRandom()))
        assertTrue(madAndTaunted.isSkillUsable(1, TEST_DIA))
    }

    /** 通常攻撃・スキル・防御のどれかが選べれば、何もできない手番ではないこと(魅了でも防御は選べる)。 */
    @Test
    fun hasAnyManualAction_trueWhenSomethingIsAvailable() {
        assertTrue(field().hasAnyManualAction(1, TEST_NORMAL_ATTACK))
        assertTrue(field(ail(StatusAilment.CHARM), skills = emptyList()).hasAnyManualAction(1, TEST_NORMAL_ATTACK))
        assertTrue(field(ail(StatusAilment.TAUNT)).hasAnyManualAction(1, TEST_NORMAL_ATTACK))
    }

    /** 挑発で防御ができず、攻撃が届かず、スキルも攻撃技がなければ、何もできない手番になること。 */
    @Test
    fun hasAnyManualAction_falseWhenNothingCanBeChosen() {
        // 味方1は後衛(スロット3)・敵は後衛(スロット3)のみ → 通常攻撃(レンジ1)は届かない。前衛の味方2が生きていて繰り上がりもない。
        val unreachable = BattleField(
            allies = listOf(testAlly(2, slot = 0), testAlly(1, slot = 3, mp = 100, activeSkills = listOf(TEST_DIA)).withAilments(ail(StatusAilment.TAUNT))),
            enemies = listOf(testEnemy(10, slot = 0), testEnemy(11, slot = 3)),
            turnQueue = listOf(1),
        )
        // 魅了+挑発(同時にはかからないが判定として): 攻撃できず・防御できず・非攻撃スキルも使えない。
        val charmAndTaunt = field(ail(StatusAilment.CHARM), ail(StatusAilment.TAUNT))

        assertFalse(unreachable.hasAnyManualAction(1, TEST_NORMAL_ATTACK))
        assertFalse(charmAndTaunt.hasAnyManualAction(1, TEST_NORMAL_ATTACK))
    }

    /** 洗脳・発狂・行動不能の個体は、手動で選べる行動がない(false)こと。 */
    @Test
    fun hasAnyManualAction_falseForAutoActingOrIncapacitated() {
        assertFalse(field(ail(StatusAilment.BRAINWASH)).hasAnyManualAction(1, TEST_NORMAL_ATTACK))
        assertFalse(field(ail(StatusAilment.MADNESS)).hasAnyManualAction(1, TEST_NORMAL_ATTACK))
        assertFalse(field(ail(StatusAilment.SLEEP)).hasAnyManualAction(1, TEST_NORMAL_ATTACK))
    }
}
