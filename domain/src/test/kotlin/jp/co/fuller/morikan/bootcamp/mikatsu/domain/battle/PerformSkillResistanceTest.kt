package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleStat
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.ElementResistances
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.ResistanceOutcome
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillCategory
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [performSkill]の属性耐性(デバフ・ネガティブエフェクトの無効・反射・吸収)を検証するテスト。
 *
 * 炎属性にした覇気(デバフ。敵単体・命中率100・敏捷と耐久力を-1段階)を、味方1が敵3へ使う。
 * 回復・バフは耐性の影響を受けないことと、使用者のTP(条件4)が成立することも確かめる。
 */
class PerformSkillResistanceTest {

    private val fireHaki = TEST_HAKI.copy(element = SkillElement.FIRE, categories = setOf(SkillCategory.DEBUFF))

    private fun resist(fire: Int) = ElementResistances(fire = fire)

    /** 味方1(手番・MP100・炎の覇気を所持。耐性[actorFire])と敵3(耐性[targetFire])の戦闘。 */
    private fun field(targetFire: Int, actorFire: Int = 100, skill: jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkill = fireHaki) =
        BattleField(
            allies = listOf(testAlly(1, mp = 100, activeSkills = listOf(skill), elementResistances = resist(actorFire))),
            enemies = listOf(testEnemy(3, elementResistances = resist(targetFire))),
            turnQueue = listOf(1, 3),
        )

    /** 耐性100・50(倍率)は影響せず、通常どおり対象にデバフがかかること。 */
    @Test
    fun multiplier_appliesNormally() {
        for (value in listOf(100, 50, 999)) {
            val (after, result) = checkNotNull(field(value).performSkill(1, fireHaki.id, 3, fixedRandom(0.5)))

            assertEquals(2, after.enemies.single().statModifiers.size)
            assertEquals(ResistanceOutcome.NONE, result.targets.single().resistance)
        }
    }

    /** 無効(0・負数)と吸収(-999)の対象には効果がかからず、無効として記録されること。 */
    @Test
    fun nullifyAndAbsorb_noEffect() {
        for (value in listOf(0, -2, -999)) {
            val (after, result) = checkNotNull(field(value).performSkill(1, fireHaki.id, 3, fixedRandom(0.5)))

            assertTrue("耐性$value", after.enemies.single().statModifiers.isEmpty())
            assertTrue("耐性$value", after.allies.single().statModifiers.isEmpty())
            val target = result.targets.single()
            assertEquals("耐性$value", ResistanceOutcome.NULLIFIED, target.resistance)
            assertTrue("耐性$value", target.isHit)
            assertTrue("耐性$value", target.statChanges.isEmpty())
            assertEquals("耐性$value", 70, after.allies.single().currentMp)
        }
    }

    /** 無効・反射・吸収の対象でも命中した扱いで、使用者がサポート技のTP(最大12)を得ること。 */
    @Test
    fun resisted_stillGivesUserTp() {
        for (value in listOf(0, -1, -999)) {
            val (after, _) = checkNotNull(field(value).performSkill(1, fireHaki.id, 3, fixedRandom(0.5)))

            assertEquals("耐性$value", 12, after.allies.single().currentTp)
        }
    }

    /** 反射は対象にかからず、使用者に敏捷・耐久力のデバフがかかる(新たな命中判定なし)こと。 */
    @Test
    fun reflect_appliesToActor() {
        val (after, result) = checkNotNull(field(-1).performSkill(1, fireHaki.id, 3, fixedRandom(0.5)))

        assertTrue(after.enemies.single().statModifiers.isEmpty())
        assertEquals(listOf(BattleStat.AGILITY, BattleStat.DEFENSE), after.allies.single().statModifiers.map { it.stat })
        val target = result.targets.single()
        assertEquals(ResistanceOutcome.REFLECTED, target.resistance)
        assertTrue(target.isHit)
        val reflected = checkNotNull(target.reflected)
        assertEquals(false, reflected.isNullified)
        assertEquals(listOf(BattleStat.AGILITY, BattleStat.DEFENSE), reflected.statChanges.map { it.stat })
    }

    /** 反射でも、使用者の耐性が無効(0)・反射(-1)・吸収(-999)ならかからないこと(再反射なし)。 */
    @Test
    fun reflect_actorNullifies() {
        for (actorValue in listOf(0, -1, -999)) {
            val (after, result) = checkNotNull(field(-1, actorFire = actorValue).performSkill(1, fireHaki.id, 3, fixedRandom(0.5)))

            assertTrue("使用者$actorValue", after.allies.single().statModifiers.isEmpty())
            assertTrue("使用者$actorValue", after.enemies.single().statModifiers.isEmpty())
            assertEquals("使用者$actorValue", true, result.targets.single().reflected?.isNullified)
        }
    }

    /** 反射で、使用者の耐性が倍率(50)なら通常どおり使用者にかかること。 */
    @Test
    fun reflect_actorMultiplier_applies() {
        val (after, _) = checkNotNull(field(-1, actorFire = 50).performSkill(1, fireHaki.id, 3, fixedRandom(0.5)))

        assertEquals(2, after.allies.single().statModifiers.size)
    }

    /** 外れたら耐性に関係なく何も起きず、無効扱いにもならないこと。 */
    @Test
    fun miss_isPlainMiss() {
        val (after, result) = checkNotNull(field(0, skill = fireHaki.copy(accuracy = 50)).performSkill(1, fireHaki.id, 3, fixedRandom(0.99)))

        val target = result.targets.single()
        assertEquals(false, target.isHit)
        assertEquals(ResistanceOutcome.NONE, target.resistance)
        assertEquals(0, after.allies.single().currentTp)
    }

    /** 無属性のデバフは、炎の耐性が0でも影響を受けず通常どおりかかること。 */
    @Test
    fun noneElement_debuff_isUnaffected() {
        val noneHaki = TEST_HAKI.copy(categories = setOf(SkillCategory.DEBUFF))

        val (after, _) = checkNotNull(field(0, skill = noneHaki).performSkill(1, noneHaki.id, 3, fixedRandom(0.5)))

        assertEquals(2, after.enemies.single().statModifiers.size)
    }

    /** ネガティブエフェクトを含むスキルも耐性の対象になること。 */
    @Test
    fun negativeEffect_isAffected() {
        val negative = fireHaki.copy(categories = setOf(SkillCategory.NEGATIVE_EFFECT))

        val (after, result) = checkNotNull(field(0, skill = negative).performSkill(1, negative.id, 3, fixedRandom(0.5)))

        assertTrue(after.enemies.single().statModifiers.isEmpty())
        assertEquals(ResistanceOutcome.NULLIFIED, result.targets.single().resistance)
    }

    /** デバフでもネガティブエフェクトでもない(バフ)スキルは、使用者自身の耐性が0でも影響を受けないこと。 */
    @Test
    fun buff_isUnaffected() {
        val fireSeishin = TEST_SEISHIN.copy(element = SkillElement.FIRE)
        val f = BattleField(
            allies = listOf(testAlly(1, mp = 100, activeSkills = listOf(fireSeishin), elementResistances = resist(0))),
            enemies = listOf(testEnemy(3)),
            turnQueue = listOf(1, 3),
        )

        val (after, result) = checkNotNull(f.performSkill(1, fireSeishin.id, 1, fixedRandom(0.5)))

        assertEquals(1, after.allies.single().statModifiers.size)
        assertEquals(ResistanceOutcome.NONE, result.targets.single().resistance)
    }

    /** 回復スキルは、対象の耐性が0・反射・吸収でも通常どおり回復できること。 */
    @Test
    fun heal_isUnaffected() {
        val fireDia = TEST_DIA.copy(element = SkillElement.FIRE)
        for (value in listOf(0, -1, -999)) {
            val f = BattleField(
                allies = listOf(
                    testAlly(1, mp = 100, activeSkills = listOf(fireDia)),
                    testAlly(2, elementResistances = resist(value)).let { it.copy(currentHp = 10) },
                ),
                enemies = listOf(testEnemy(3)),
                turnQueue = listOf(1, 3),
            )

            val (after, result) = checkNotNull(f.performSkill(1, fireDia.id, 2, fixedRandom(0.5)))

            assertEquals("耐性$value", 60, after.allies[1].currentHp)
            assertEquals("耐性$value", ResistanceOutcome.NONE, result.targets.single().resistance)
        }
    }

    /** 範囲の使用で、対象ごとに耐性が判定されること(1体は無効、もう1体は通常)。 */
    @Test
    fun area_perTarget() {
        val area = fireHaki.copy(
            area = jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillArea.AREA,
            targetSelection = jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillTargetSelection.ALL,
        )
        val f = BattleField(
            allies = listOf(testAlly(1, mp = 100, activeSkills = listOf(area))),
            enemies = listOf(
                testEnemy(3, slot = 0, elementResistances = resist(0)),
                testEnemy(4, slot = 1, elementResistances = resist(100)),
            ),
            turnQueue = listOf(1),
        )

        val (after, result) = checkNotNull(f.performSkill(1, area.id, 3, fixedRandom(0.5)))

        assertEquals(listOf(ResistanceOutcome.NULLIFIED, ResistanceOutcome.NONE), result.targets.map { it.resistance })
        assertTrue(after.enemies[0].statModifiers.isEmpty())
        assertEquals(2, after.enemies[1].statModifiers.size)
        assertNull(result.targets[1].reflected)
    }
}
