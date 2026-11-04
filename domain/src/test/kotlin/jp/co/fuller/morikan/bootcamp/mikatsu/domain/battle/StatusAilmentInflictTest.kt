package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.EffectApplication
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.ElementResistances
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.ResistanceOutcome
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.StatusAilment
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkill
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkillEffect
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillArea
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillCategory
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillElement
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillRange
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillTargetSelection
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillTargetSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 状態異常の付与・即死([performSkill])を検証するテスト。成功率と乱数の順、排他・石化・上書きのルール、
 * 手番中の付与、発動回数の繰り返しと打ち切り、烙印、TPの条件8を確かめる。
 */
class StatusAilmentInflictTest {

    private fun poison(rate: Int = 100, turns: Int = 5) = ActiveSkillEffect.InflictAilment(StatusAilment.POISON, turns, rate)

    /** 味方1(手番・[skill]所持)と、前衛の敵4・5がいる戦闘。 */
    private fun field(skill: ActiveSkill, enemy4: (jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleEnemy) -> jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleEnemy = { it }) = BattleField(
        allies = listOf(testAlly(1, mp = 100, activeSkills = listOf(skill))),
        enemies = listOf(enemy4(testEnemy(4, hp = 1000)), testEnemy(5, hp = 1000, slot = 1)),
        turnQueue = listOf(1, 4),
    )

    private fun BattleField.use(skill: ActiveSkill, target: Int, random: SequenceRandom) =
        checkNotNull(performSkill(1, skill.id, target, random))

    /** 成功率100は乱数を引かず必ずかかる。 */
    @Test
    fun rate100_neverDrawsRandom() {
        val skill = ailmentSkill(poison())
        val random = SequenceRandom()

        val (after, result) = field(skill).use(skill, 4, random)

        assertEquals(listOf(StatusAilment.POISON), after.pawn(4).ailmentKinds())
        assertEquals(listOf(EffectApplication.Ailment(StatusAilment.POISON, true)), result.targets.single().effectResults)
        // 引いた乱数はTPの獲得(使用者・対象の条件8)だけ。成功判定では引かない。
        assertEquals(listOf(5, 5), random.untils)
    }

    /** 成功判定は対象の順 × 効果の順に1回ずつ、`nextInt(100) < 成功率`で行う。 */
    @Test
    fun successRoll_orderIsTargetsThenEffects() {
        val skill = ailmentSkill(
            poison(rate = 50),
            ActiveSkillEffect.InflictAilment(StatusAilment.BLEED, 5, 50),
            area = SkillArea.AREA,
            targetSelection = SkillTargetSelection.ALL,
        )
        // 敵4: 毒10(成功)・流血90(失敗)、敵5: 毒90(失敗)・流血49(成功)。50は失敗の境界、49は成功の境界。
        val random = SequenceRandom(ints = listOf(10, 90, 90, 49))

        val (after, result) = field(skill).use(skill, 4, random)

        assertEquals(listOf(StatusAilment.POISON), after.pawn(4).ailmentKinds())
        assertEquals(listOf(StatusAilment.BLEED), after.pawn(5).ailmentKinds())
        assertEquals(listOf(100, 100, 100, 100), random.untils.take(4))
        assertEquals(
            listOf(true, false),
            result.targets.first { it.targetFpid == 4 }.effectResults.map { it.succeeded },
        )
    }

    /** 成功判定に外れた効果は命中扱いのままで、使用者のTP(条件4)が成立する。 */
    @Test
    fun failedRoll_stillCountsAsHit_userGetsTp() {
        val skill = ailmentSkill(poison(rate = 10))
        // 成功判定99(失敗)、使用者のTP(条件4)は0→8、対象のTP(条件8)は0→8。
        val random = SequenceRandom(ints = listOf(99, 0, 0))

        val (after, result) = field(skill).use(skill, 4, random)

        assertTrue(after.pawn(4).ailments.isEmpty())
        assertTrue(result.targets.single().isHit)
        assertEquals(8, after.pawn(1).currentTp)
        assertEquals(8, after.pawn(4).currentTp)
    }

    /** 火傷がある個体に凍傷・凍結は付かず、凍傷・凍結がある個体に火傷は付かない。凍傷と凍結は共存する。 */
    @Test
    fun burn_isExclusiveWithFrostbiteAndFreeze() {
        fun inflict(existing: StatusAilment, new: StatusAilment): List<StatusAilment> {
            val skill = ailmentSkill(ActiveSkillEffect.InflictAilment(new, 5))
            val f = field(skill) { it.withAilments(ail(existing)) }
            return f.use(skill, 4, SequenceRandom()).first.pawn(4).ailmentKinds()
        }

        assertEquals(listOf(StatusAilment.BURN), inflict(StatusAilment.BURN, StatusAilment.FROSTBITE))
        assertEquals(listOf(StatusAilment.BURN), inflict(StatusAilment.BURN, StatusAilment.FREEZE))
        assertEquals(listOf(StatusAilment.FROSTBITE), inflict(StatusAilment.FROSTBITE, StatusAilment.BURN))
        assertEquals(listOf(StatusAilment.FREEZE), inflict(StatusAilment.FREEZE, StatusAilment.BURN))
        assertEquals(
            listOf(StatusAilment.FROSTBITE, StatusAilment.FREEZE),
            inflict(StatusAilment.FROSTBITE, StatusAilment.FREEZE),
        )
    }

    /** 排他で弾かれた効果は失敗扱いとして記録される。 */
    @Test
    fun exclusion_isReportedAsFailure() {
        val skill = ailmentSkill(ActiveSkillEffect.InflictAilment(StatusAilment.FREEZE, 5))
        val f = field(skill) { it.withAilments(ail(StatusAilment.BURN)) }

        val (_, result) = f.use(skill, 4, SequenceRandom())

        assertEquals(listOf(EffectApplication.Ailment(StatusAilment.FREEZE, false)), result.targets.single().effectResults)
    }

    /** 石化は他の状態異常をすべて解除し、石化中は石化以外が付かない。石化の重ねがけは長い方で上書きする。 */
    @Test
    fun petrify_clearsOthers_andBlocksOthers() {
        val petrify = ailmentSkill(ActiveSkillEffect.InflictAilment(StatusAilment.PETRIFY, 4))
        val cleared = field(petrify) { it.withAilments(ail(StatusAilment.POISON), ail(StatusAilment.BURN)) }
            .use(petrify, 4, SequenceRandom()).first
        assertEquals(listOf(StatusAilment.PETRIFY), cleared.pawn(4).ailmentKinds())
        assertEquals(4, cleared.pawn(4).ailments.single().remainingTurns)

        val blocked = ailmentSkill(poison())
        val (after, result) = field(blocked) { it.withAilments(ail(StatusAilment.PETRIFY, turns = 2)) }
            .use(blocked, 4, SequenceRandom())
        assertEquals(listOf(StatusAilment.PETRIFY), after.pawn(4).ailmentKinds())
        assertFalse(result.targets.single().effectResults.single().succeeded)

        val longer = field(petrify) { it.withAilments(ail(StatusAilment.PETRIFY, turns = 2)) }
            .use(petrify, 4, SequenceRandom()).first
        assertEquals(4, longer.pawn(4).ailments.single().remainingTurns)
    }

    /** 同じ状態異常は残りターンの長い方で上書きし、感染の分母はそのまま残す。 */
    @Test
    fun sameAilment_keepsLongerTurns_andInfectionDenominator() {
        val poisonSkill = ailmentSkill(poison(turns = 5))
        val extended = field(poisonSkill) { it.withAilments(ail(StatusAilment.POISON, turns = 2)) }
            .use(poisonSkill, 4, SequenceRandom()).first
        assertEquals(5, extended.pawn(4).ailments.single().remainingTurns)

        val shorter = ailmentSkill(poison(turns = 2))
        val kept = field(shorter) { it.withAilments(ail(StatusAilment.POISON, turns = 5)) }
            .use(shorter, 4, SequenceRandom()).first
        assertEquals(5, kept.pawn(4).ailments.single().remainingTurns)

        val infect = ailmentSkill(ActiveSkillEffect.InflictAilment(StatusAilment.INFECTION, 5))
        val infection = field(infect) { it.withAilments(ail(StatusAilment.INFECTION, turns = 2, denominator = 10)) }
            .use(infect, 4, SequenceRandom()).first
        assertEquals(10, infection.pawn(4).ailments.single().infectionDenominator)
        assertEquals(5, infection.pawn(4).ailments.single().remainingTurns)
    }

    /** 本人の手番中に付与された状態異常だけ、手番中に付与されたしるしが付く。 */
    @Test
    fun grantedDuringOwnTurn_onlyWhenTargetIsActor() {
        val self = ailmentSkill(poison(), targetSide = SkillTargetSide.ALLY, range = SkillRange.RANGE_0)
        val onSelf = field(self).use(self, 1, SequenceRandom()).first
        assertTrue(onSelf.pawn(1).ailments.single().grantedDuringOwnTurn)

        val enemy = ailmentSkill(poison())
        val onEnemy = field(enemy).use(enemy, 4, SequenceRandom()).first
        assertFalse(onEnemy.pawn(4).ailments.single().grantedDuringOwnTurn)
    }

    /** 即死が成功したら対象のHPが0になり、状態異常が全解除される。石化中でも効く。 */
    @Test
    fun instantDeath_killsAndClearsAilments_evenWhenPetrified() {
        val skill = ailmentSkill(ActiveSkillEffect.InstantDeath(successRate = 30))
        val f = field(skill) { it.withAilments(ail(StatusAilment.PETRIFY)) }

        val (after, result) = f.use(skill, 4, SequenceRandom(ints = listOf(29)))

        assertEquals(0, after.pawn(4).currentHp)
        assertTrue(after.pawn(4).ailments.isEmpty())
        assertEquals(listOf(EffectApplication.InstantDeath(true)), result.targets.single().effectResults)
        // 30は失敗の境界。
        val (survived, _) = f.use(skill, 4, SequenceRandom(ints = listOf(30)))
        assertEquals(1000, survived.pawn(4).currentHp)
    }

    /** 発動回数の分だけ回数優先で繰り返し、戦闘不能になった対象はそれ以降の回を打ち切る(命頂戴と同じ構成)。 */
    @Test
    fun activationCount_repeatsRoundFirst_andSkipsDefeated() {
        val skill = ailmentSkill(
            ActiveSkillEffect.InstantDeath(successRate = 30),
            activationCount = 3,
            area = SkillArea.AREA,
            targetSelection = SkillTargetSelection.ALL,
            accuracy = 100,
        )
        // 1回目: 敵4失敗(50)・敵5成功(0)。2回目: 敵5は戦闘不能なので飛ばし、敵4失敗(50)。3回目: 敵4成功(0)。
        val random = SequenceRandom(ints = listOf(50, 0, 50, 0))

        val (after, result) = field(skill).use(skill, 4, random)

        assertEquals(0, after.pawn(4).currentHp)
        assertEquals(0, after.pawn(5).currentHp)
        assertEquals(listOf(0 to 4, 0 to 5, 1 to 4, 2 to 4), result.targets.map { it.round to it.targetFpid })
        assertEquals(3, result.roundCount)
        assertEquals(listOf(100, 100, 100, 100), random.untils.take(4))
    }

    /** 発動回数1のスキルは従来どおり回0・結果1件で、挙動が変わらない(回復)。 */
    @Test
    fun singleActivation_isUnchanged() {
        val f = BattleField(
            allies = listOf(testAlly(1, mp = 30, activeSkills = listOf(TEST_DIA)), testAlly(2).copy(currentHp = 50)),
            enemies = listOf(testEnemy(4)),
            turnQueue = listOf(1, 4),
        )

        val (_, result) = checkNotNull(f.performSkill(1, TEST_DIA.id, 2, SequenceRandom()))

        assertEquals(1, result.targets.size)
        assertEquals(0, result.targets.single().round)
    }

    /** 烙印中は、HPの回復量が1以上なら1になる。 */
    @Test
    fun brand_limitsHealToOne() {
        val f = BattleField(
            allies = listOf(
                testAlly(1, mp = 30, activeSkills = listOf(TEST_DIA)).withAilments(ail(StatusAilment.BRAND)).copy(currentHp = 10),
                testAlly(2).copy(currentHp = 50),
            ),
            enemies = listOf(testEnemy(4)),
            turnQueue = listOf(1, 4),
        )

        val (after, result) = checkNotNull(f.performSkill(1, TEST_DIA.id, 1, SequenceRandom()))

        assertEquals(11, after.pawn(1).currentHp)
        assertEquals(1, result.totalHealed)
        // 烙印がない味方は通常どおり50回復する。
        val (healed, _) = checkNotNull(f.performSkill(1, TEST_DIA.id, 2, SequenceRandom()))
        assertEquals(100, healed.pawn(2).currentHp)
    }

    /** 烙印の個体への回復量0は0のまま(1にしない)。 */
    @Test
    fun brand_doesNotRaiseZeroHeal() {
        assertEquals(0, StatusAilmentFormula.limitHealForBrand(0, isBranded = true))
        assertEquals(1, StatusAilmentFormula.limitHealForBrand(50, isBranded = true))
        assertEquals(50, StatusAilmentFormula.limitHealForBrand(50, isBranded = false))
    }

    /** 条件8: デバフ・ネガティブエフェクトを命中させられた対象が、1回の技につき1回だけ8〜12のTPを得る。 */
    @Test
    fun tpCondition8_targetGetsOncePerSkill() {
        val skill = ailmentSkill(
            ActiveSkillEffect.InstantDeath(successRate = 0),
            activationCount = 3,
            area = SkillArea.AREA,
            targetSelection = SkillTargetSelection.ALL,
        )
        // 成功率0(nextInt(100)<0は常に失敗)を3回×2体=6回引いてから、使用者(0→8)・敵4(1→9)・敵5(4→12)のTP。
        val random = SequenceRandom(ints = listOf(50, 50, 50, 50, 50, 50, 0, 1, 4))

        val (after, _) = field(skill).use(skill, 4, random)

        assertEquals(8, after.pawn(1).currentTp)
        assertEquals(9, after.pawn(4).currentTp)
        assertEquals(12, after.pawn(5).currentTp)
        assertEquals(listOf(5, 5, 5), random.untils.drop(6))
    }

    /** 条件8は外れた対象には与えられず(回避のTPだけ)、命中すれば属性耐性で無効・反射でも与えられる。反射された使用者は受け取らない。 */
    @Test
    fun tpCondition8_hitOnly_evenWhenNullifiedOrReflected() {
        val fire = ailmentSkill(poison(), accuracy = 50).copy(element = SkillElement.FIRE)
        fun fieldWith(resist: Int) = field(fire) { it.copy(enemy = it.enemy.copy(elementResistances = ElementResistances(fire = resist))) }

        // 外れ(nextDouble 0.9 → 90 ≥ 50)。回避のTP(18〜22。ここでは0→18)だけで、条件8はない。使用者はTPなし。
        val missed = fieldWith(100).performSkill(1, fire.id, 4, SequenceRandom(ints = listOf(0), doubles = listOf(0.9)))
        assertEquals(18, checkNotNull(missed).first.pawn(4).currentTp)
        assertEquals(0, missed.first.pawn(1).currentTp)

        // 無効(耐性0): 命中扱いで、使用者(0→8)と対象(2→10)がTPを得る。
        val nullified = checkNotNull(fieldWith(0).performSkill(1, fire.id, 4, SequenceRandom(ints = listOf(0, 2), doubles = listOf(0.1))))
        assertEquals(ResistanceOutcome.NULLIFIED, nullified.second.targets.single().resistance)
        assertEquals(10, nullified.first.pawn(4).currentTp)

        // 反射(耐性-1): 使用者に毒が跳ね返るが、使用者は受けた側のTPを得ない(使用者のTPは条件4の分だけ)。対象は条件8を得る。
        val reflected = checkNotNull(fieldWith(-1).performSkill(1, fire.id, 4, SequenceRandom(ints = listOf(0, 3), doubles = listOf(0.1))))
        assertEquals(ResistanceOutcome.REFLECTED, reflected.second.targets.single().resistance)
        assertEquals(listOf(StatusAilment.POISON), reflected.first.pawn(1).ailmentKinds())
        assertEquals(8, reflected.first.pawn(1).currentTp)
        assertEquals(11, reflected.first.pawn(4).currentTp)
    }

    /** 覇気(デバフ)にも条件8が適用される。 */
    @Test
    fun tpCondition8_appliesToDebuffSkills() {
        val haki = TEST_HAKI.copy(categories = setOf(SkillCategory.DEBUFF))
        val f = BattleField(
            allies = listOf(testAlly(1, mp = 100, activeSkills = listOf(haki))),
            enemies = listOf(testEnemy(4)),
            turnQueue = listOf(1, 4),
        )

        val (after, _) = checkNotNull(f.performSkill(1, haki.id, 4, SequenceRandom(ints = listOf(0, 4))))

        assertEquals(8, after.pawn(1).currentTp)
        assertEquals(12, after.pawn(4).currentTp)
    }

    /** バフ・回復など、デバフでもネガティブエフェクトでもないスキルには条件8はない。 */
    @Test
    fun tpCondition8_notForOtherSkills() {
        val f = BattleField(
            allies = listOf(testAlly(1, mp = 30, activeSkills = listOf(TEST_DIA)), testAlly(2).copy(currentHp = 50)),
            enemies = listOf(testEnemy(4)),
            turnQueue = listOf(1, 4),
        )

        val random = SequenceRandom(ints = listOf(0))
        val (after, _) = checkNotNull(f.performSkill(1, TEST_DIA.id, 2, random))

        assertEquals(0, after.pawn(2).currentTp)
        assertEquals(listOf(5), random.untils)
        assertNotNull(after)
    }
}
