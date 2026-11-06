package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AilmentResistanceType
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AilmentResistances
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.ElementResistances
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.EffectApplication
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.StatusAilment
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.TurnEndEvent
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkillEffect
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillCategory
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 状態異常耐性([AilmentResistanceFormula]と、付与・即死・反射・感染の伝染への効き方)を検証するテスト。 */
class AilmentResistanceTest {

    private fun res(type: AilmentResistanceType, value: Int) = AilmentResistances().with(type, value)

    private fun inflict(ailment: StatusAilment, rate: Int = 100) = ActiveSkillEffect.InflictAilment(ailment, 5, rate)

    /** 味方1(手番・スキル所持)と敵4(耐性[targetRes])の戦闘。 */
    private fun field(
        skill: jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkill,
        targetRes: AilmentResistances = AilmentResistances(),
        actorRes: AilmentResistances = AilmentResistances(),
        targetElement: ElementResistances = ElementResistances(),
    ) = BattleField(
        allies = listOf(testAlly(1, mp = 100, activeSkills = listOf(skill), ailmentResistances = actorRes)),
        enemies = listOf(testEnemy(4, hp = 1000, ailmentResistances = targetRes, elementResistances = targetElement)),
        turnQueue = listOf(1, 4),
    )

    /** 成功率×耐性の切り捨て。 */
    @Test
    fun effectiveSuccessRate_floors() {
        assertEquals(18, AilmentResistanceFormula.effectiveSuccessRate(90, 20))
        assertEquals(24, AilmentResistanceFormula.effectiveSuccessRate(75, 33))
        assertEquals(0, AilmentResistanceFormula.effectiveSuccessRate(80, 0))
        assertEquals(100, AilmentResistanceFormula.effectiveSuccessRate(100, 100))
    }

    /** clampは0〜100に収める。 */
    @Test
    fun clamp_limitsRange() {
        assertEquals(0, AilmentResistanceFormula.clamp(-5))
        assertEquals(100, AilmentResistanceFormula.clamp(101))
        assertEquals(40, AilmentResistanceFormula.clamp(40))
    }

    /** スキル封印・精神系は項目を共有し、スタン・烙印は耐性なし、他は同名。 */
    @Test
    fun typeOf_mapping() {
        assertEquals(AilmentResistanceType.SKILL_SEAL, AilmentResistanceType.of(StatusAilment.MP_SEAL))
        assertEquals(AilmentResistanceType.SKILL_SEAL, AilmentResistanceType.of(StatusAilment.TP_SEAL))
        listOf(StatusAilment.BRAINWASH, StatusAilment.MADNESS, StatusAilment.CHARM, StatusAilment.FEAR, StatusAilment.TAUNT)
            .forEach { assertEquals(AilmentResistanceType.MENTAL, AilmentResistanceType.of(it)) }
        assertNull(AilmentResistanceType.of(StatusAilment.STUN))
        assertNull(AilmentResistanceType.of(StatusAilment.BRAND))
        assertEquals(AilmentResistanceType.POISON, AilmentResistanceType.of(StatusAilment.POISON))
    }

    /** 全項目の既定は100で、withは他の項目を変えず、equalsが効く。 */
    @Test
    fun model_defaultsAndWith() {
        val base = AilmentResistances()
        assertTrue(AilmentResistanceType.entries.all { base.of(it) == 100 })
        val changed = base.with(AilmentResistanceType.BURN, 7)
        assertEquals(7, changed.of(AilmentResistanceType.BURN))
        assertEquals(100, changed.of(AilmentResistanceType.POISON))
        assertEquals(changed, AilmentResistances().with(AilmentResistanceType.BURN, 7))
        assertFalse(base == changed)
    }

    /** 成功率100×耐性100は乱数を引かず、成功判定の乱数は消費しない(TPの乱数だけ)。 */
    @Test
    fun neutral_neverDrawsForSuccess() {
        val skill = ailmentSkill(inflict(StatusAilment.POISON))
        val random = SequenceRandom()

        val (after, _) = checkNotNull(field(skill).performSkill(1, skill.id, 4, random))

        assertEquals(listOf(StatusAilment.POISON), after.pawn(4).ailmentKinds())
        assertEquals(listOf(5, 5), random.untils)
    }

    /** 耐性50: 成功率100でも実質50。49なら成功、50なら失敗。乱数は1回引く。 */
    @Test
    fun resistance_lowersSuccessRate() {
        val skill = ailmentSkill(inflict(StatusAilment.POISON))
        val res = res(AilmentResistanceType.POISON, 50)

        val ok = checkNotNull(field(skill, res).performSkill(1, skill.id, 4, SequenceRandom(ints = listOf(49))))
        assertEquals(listOf(StatusAilment.POISON), ok.first.pawn(4).ailmentKinds())

        val random = SequenceRandom(ints = listOf(50))
        val ng = checkNotNull(field(skill, res).performSkill(1, skill.id, 4, random))
        assertTrue(ng.first.pawn(4).ailments.isEmpty())
        assertEquals(100, random.untils.first())
        assertEquals(listOf(EffectApplication.Ailment(StatusAilment.POISON, false)), ng.second.targets.single().effectResults)
    }

    /** 耐性0は必ず失敗し、乱数は1回引く(どんな値でも失敗)。 */
    @Test
    fun resistanceZero_alwaysFails() {
        val skill = ailmentSkill(inflict(StatusAilment.POISON))
        val random = SequenceRandom(ints = listOf(0))

        val (after, _) = checkNotNull(field(skill, res(AilmentResistanceType.POISON, 0)).performSkill(1, skill.id, 4, random))

        assertTrue(after.pawn(4).ailments.isEmpty())
        assertEquals(100, random.untils.first())
    }

    /** MP封印・TP封印は SKILL_SEAL、精神系5種は MENTAL の耐性を共有する。 */
    @Test
    fun sharedResistances() {
        listOf(StatusAilment.MP_SEAL to AilmentResistanceType.SKILL_SEAL, StatusAilment.TP_SEAL to AilmentResistanceType.SKILL_SEAL,
            StatusAilment.BRAINWASH to AilmentResistanceType.MENTAL, StatusAilment.MADNESS to AilmentResistanceType.MENTAL,
            StatusAilment.CHARM to AilmentResistanceType.MENTAL, StatusAilment.FEAR to AilmentResistanceType.MENTAL,
            StatusAilment.TAUNT to AilmentResistanceType.MENTAL,
        ).forEach { (ailment, type) ->
            val skill = ailmentSkill(inflict(ailment))
            val (after, _) = checkNotNull(field(skill, res(type, 0)).performSkill(1, skill.id, 4, SequenceRandom(ints = listOf(0))))
            assertTrue("$ailment", after.pawn(4).ailments.isEmpty())
        }
    }

    /** スタン・烙印は耐性の影響を受けない(全項目0でも成功率100なら乱数なしでかかる)。 */
    @Test
    fun stunAndBrand_unaffected() {
        val zero = AilmentResistanceType.entries.fold(AilmentResistances()) { r, t -> r.with(t, 0) }
        listOf(StatusAilment.STUN, StatusAilment.BRAND).forEach { ailment ->
            val skill = ailmentSkill(inflict(ailment))
            val (after, _) = checkNotNull(field(skill, zero).performSkill(1, skill.id, 4, SequenceRandom()))
            assertEquals(listOf(ailment), after.pawn(4).ailmentKinds())
        }
    }

    /** 即死は INSTANT_DEATH の耐性で下がる。 */
    @Test
    fun instantDeath_usesInstantDeathResistance() {
        val skill = ailmentSkill(ActiveSkillEffect.InstantDeath(100))

        val dead = checkNotNull(field(skill).performSkill(1, skill.id, 4, SequenceRandom()))
        assertTrue(dead.first.pawn(4).isDefeated)

        val safe = checkNotNull(field(skill, res(AilmentResistanceType.INSTANT_DEATH, 0)).performSkill(1, skill.id, 4, SequenceRandom(ints = listOf(0))))
        assertFalse(safe.first.pawn(4).isDefeated)
        // 他の項目の耐性は即死に効かない。
        val other = checkNotNull(field(skill, res(AilmentResistanceType.POISON, 0)).performSkill(1, skill.id, 4, SequenceRandom()))
        assertTrue(other.first.pawn(4).isDefeated)
    }

    /** 属性耐性の反射で跳ね返った効果は、使用者の状態異常耐性で判定する(対象の耐性は見ない)。 */
    @Test
    fun reflected_usesActorResistance() {
        val base = ailmentSkill(inflict(StatusAilment.POISON)).copy(element = SkillElement.FIRE, categories = setOf(SkillCategory.NEGATIVE_EFFECT))
        val reflect = ElementResistances(fire = -1)

        // 使用者の毒耐性0 → 失敗(対象の耐性0でも関係なし)。
        val failed = checkNotNull(
            field(base, targetRes = AilmentResistances(), actorRes = res(AilmentResistanceType.POISON, 0), targetElement = reflect)
                .performSkill(1, base.id, 4, SequenceRandom(ints = listOf(0))),
        )
        assertTrue(failed.first.pawn(1).ailments.isEmpty())

        // 対象の耐性が0でも、使用者の耐性が100なら使用者にかかる。
        val applied = checkNotNull(
            field(base, targetRes = res(AilmentResistanceType.POISON, 0), targetElement = reflect)
                .performSkill(1, base.id, 4, SequenceRandom()),
        )
        assertEquals(listOf(StatusAilment.POISON), applied.first.pawn(1).ailmentKinds())
        assertTrue(applied.first.pawn(4).ailments.isEmpty())
    }

    private fun infectionField(allyRes: AilmentResistances) = BattleField(
        allies = listOf(
            testAlly(1, hp = 160).withAilments(ail(StatusAilment.INFECTION, turns = 4, denominator = 8)),
            testAlly(2, ailmentResistances = allyRes),
        ),
        enemies = listOf(testEnemy(9)),
        turnQueue = listOf(9, 1),
    )

    /** 伝染先の感染耐性が100なら今までどおり(追加の乱数なし)でうつる。 */
    @Test
    fun spread_neutralResistance_noExtraDraw() {
        val random = SequenceRandom(ints = listOf(29, 0))

        val (after, events) = infectionField(AilmentResistances()).processTurnEndAilments(1, random)

        assertEquals(1, events.filterIsInstance<TurnEndEvent.Spread>().size)
        assertEquals(listOf(StatusAilment.INFECTION), after.pawn(2).ailmentKinds())
        assertEquals(listOf(100, 1), random.untils)
    }

    /** 耐性<100なら追加で nextInt(100) を引き、失敗なら伝染せず出来事も出さない。 */
    @Test
    fun spread_lowResistance_failsWithoutEvent() {
        val random = SequenceRandom(ints = listOf(29, 0, 50))

        val (after, events) = infectionField(res(AilmentResistanceType.INFECTION, 50)).processTurnEndAilments(1, random)

        assertTrue(events.none { it is TurnEndEvent.Spread })
        assertTrue(after.pawn(2).ailments.isEmpty())
        assertEquals(listOf(100, 1, 100), random.untils)
    }

    /** 耐性<100でも判定に成功すれば伝染する。 */
    @Test
    fun spread_lowResistance_succeeds() {
        val random = SequenceRandom(ints = listOf(29, 0, 49))

        val (after, events) = infectionField(res(AilmentResistanceType.INFECTION, 50)).processTurnEndAilments(1, random)

        assertEquals(1, events.filterIsInstance<TurnEndEvent.Spread>().size)
        assertEquals(listOf(StatusAilment.INFECTION), after.pawn(2).ailmentKinds())
        assertEquals(listOf(100, 1, 100), random.untils)
    }
}
