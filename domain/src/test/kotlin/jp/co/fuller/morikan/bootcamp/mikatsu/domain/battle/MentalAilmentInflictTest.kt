package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.EffectApplication
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.StatusAilment
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkillEffect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 精神系の状態異常(洗脳・発狂・魅了・恐怖・挑発)どうしの、強さによる排他と上書きのルールを検証するテスト。 */
class MentalAilmentInflictTest {

    /** 味方1(手番)と、状態異常[ailments]がかかった敵4の戦闘。 */
    private fun field(vararg ailments: jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AilmentState) = BattleField(
        allies = listOf(testAlly(1, mp = 100)),
        enemies = listOf(testEnemy(4, hp = 1000).withAilments(*ailments)),
        turnQueue = listOf(1),
    )

    /** より強い精神系をかけると、弱い精神系は(文を出さずに)消えて付与されること。無関係な状態異常は残る。 */
    @Test
    fun stronger_replacesWeakerMentalAilments() {
        val (after, applied) = field(ail(StatusAilment.TAUNT), ail(StatusAilment.FEAR), ail(StatusAilment.POISON))
            .inflictAilment(4, StatusAilment.CHARM, 3)

        assertTrue(applied)
        assertEquals(listOf(StatusAilment.POISON, StatusAilment.CHARM), after.pawn(4).ailmentKinds())
    }

    /** 最強の洗脳は、他の精神系をすべて消して付与されること。 */
    @Test
    fun brainwash_replacesEveryOtherMental() {
        val (after, applied) = field(ail(StatusAilment.MADNESS), ail(StatusAilment.CHARM), ail(StatusAilment.FEAR), ail(StatusAilment.TAUNT))
            .inflictAilment(4, StatusAilment.BRAINWASH, 3)

        assertTrue(applied)
        assertEquals(listOf(StatusAilment.BRAINWASH), after.pawn(4).ailmentKinds())
    }

    /** 自分より強い精神系がかかっていると、弱い精神系は失敗してフィールドが変わらないこと。 */
    @Test
    fun weaker_failsAgainstStronger() {
        val before = field(ail(StatusAilment.MADNESS))

        StatusAilment.entries.filter { it.ordinal > StatusAilment.MADNESS.ordinal && it >= StatusAilment.CHARM }.forEach { weaker ->
            val (after, applied) = before.inflictAilment(4, weaker, 3)

            assertFalse("$weaker", applied)
            assertEquals(before, after)
        }
    }

    /** 同じ種類は、残りターンの長い方になること(短い方をかけても短くならない)。 */
    @Test
    fun sameKind_keepsLongerTurns() {
        val (longer, _) = field(ail(StatusAilment.CHARM, turns = 2)).inflictAilment(4, StatusAilment.CHARM, 4)
        val (shorter, _) = field(ail(StatusAilment.CHARM, turns = 5)).inflictAilment(4, StatusAilment.CHARM, 3)

        assertEquals(4, longer.pawn(4).ailments.single().remainingTurns)
        assertEquals(5, shorter.pawn(4).ailments.single().remainingTurns)
    }

    /** 精神系でない状態異常とは排他にならず共存すること。 */
    @Test
    fun mental_coexistsWithNonMental() {
        val (after, applied) = field(ail(StatusAilment.POISON), ail(StatusAilment.MP_SEAL)).inflictAilment(4, StatusAilment.TAUNT, 3)

        assertTrue(applied)
        assertEquals(listOf(StatusAilment.POISON, StatusAilment.MP_SEAL, StatusAilment.TAUNT), after.pawn(4).ailmentKinds())
    }

    /** 石化中は精神系もかからず、石化をかけると精神系は消えること(石化のルールがそのまま効く)。 */
    @Test
    fun petrify_rulesStillApply() {
        val (blocked, applied) = field(ail(StatusAilment.PETRIFY)).inflictAilment(4, StatusAilment.BRAINWASH, 3)
        val (cleared, _) = field(ail(StatusAilment.BRAINWASH), ail(StatusAilment.TAUNT)).inflictAilment(4, StatusAilment.PETRIFY, 3)

        assertFalse(applied)
        assertEquals(listOf(StatusAilment.PETRIFY), blocked.pawn(4).ailmentKinds())
        assertEquals(listOf(StatusAilment.PETRIFY), cleared.pawn(4).ailmentKinds())
    }

    /** 手番終了では精神系にダメージなどはなく、残りターンが減って0で解除されるだけであること。 */
    @Test
    fun turnEnd_onlyCountsDown() {
        val ended = field(ail(StatusAilment.CHARM, turns = 1), ail(StatusAilment.TAUNT, turns = 2))
            .copy(turnQueue = listOf(4))

        val (after, events) = ended.processTurnEndAilments(4, SequenceRandom())

        assertEquals(listOf(StatusAilment.TAUNT), after.pawn(4).ailmentKinds())
        assertEquals(1, after.pawn(4).ailments.single().remainingTurns)
        assertEquals(1000, after.pawn(4).currentHp)
        assertEquals(listOf(StatusAilment.CHARM), events.filterIsInstance<jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.TurnEndEvent.Cured>().map { it.ailment })
    }

    /** スキルで強い精神系がかかっている相手に弱い精神系をかけると、効果は「かからなかった」扱いになること(命中扱い)。 */
    @Test
    fun skill_againstStrongerMental_resultsInFailure() {
        val skill = ailmentSkill(ActiveSkillEffect.InflictAilment(StatusAilment.TAUNT, 3, 100))
        val field = BattleField(
            allies = listOf(testAlly(1, mp = 100, activeSkills = listOf(skill))),
            enemies = listOf(testEnemy(4, hp = 1000).withAilments(ail(StatusAilment.BRAINWASH))),
            turnQueue = listOf(1),
        )

        val (after, result) = checkNotNull(field.performSkill(1, skill.id, 4, SequenceRandom()))

        assertEquals(listOf(StatusAilment.BRAINWASH), after.pawn(4).ailmentKinds())
        assertEquals(listOf(EffectApplication.Ailment(StatusAilment.TAUNT, false)), result.targets.single().effectResults)
    }
}
