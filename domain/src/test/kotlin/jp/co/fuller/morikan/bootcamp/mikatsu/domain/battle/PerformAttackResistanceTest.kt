package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleAlly
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleEnemy
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BreakState
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.CriticalType
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.ElementResistances
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.ResistanceOutcome
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillArea
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillElement
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillTargetSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * [performAttack]の属性耐性(倍率・無効・吸収・反射)を検証するテスト。
 *
 * 攻撃100・耐久100・基礎威力100の1ヒットは24ダメージ。乱数は[fixedRandom]を使い、クリティカルは起きない
 * (クリティカル判定の`nextInt(24)`は23を返す)。TPは攻撃を当てた使用者が最大の12(`nextInt(5)`が4)を得る。
 */
class PerformAttackResistanceTest {

    private val fire = TEST_NORMAL_ATTACK.copy(element = SkillElement.FIRE)

    private fun resist(fire: Int) = ElementResistances(fire = fire)

    /** 味方1(攻撃者)と敵2(対象。耐性[targetFire]。現在HP[enemyHp]/最大1000)の戦闘。 */
    private fun field(
        targetFire: Int,
        enemyHp: Int = 1000,
        attacker: BattleAlly = testAlly(1),
    ): BattleField {
        val enemy: BattleEnemy = testEnemy(2, hp = 1000, elementResistances = resist(targetFire))
        return BattleField(
            allies = listOf(attacker),
            enemies = listOf(enemy.copy(currentHp = enemyHp)),
            turnQueue = listOf(1),
        )
    }

    /** 乱数の引かれた回数を種類ごとに数える乱数(値はfixedRandomと同じで、クリティカル判定だけは[criticalRoll]を返す)。 */
    private class CountingRandom(private val criticalRoll: Int = 23) : Random() {
        var doubles = 0
        var ints = 0
        override fun nextBits(bitCount: Int): Int = error("nextDouble・nextInt(until)以外は使わない")
        override fun nextInt(until: Int): Int {
            ints++
            return if (until == 24) criticalRoll else until - 1
        }
        override fun nextDouble(): Double {
            doubles++
            return 0.5
        }
    }

    // ---- 倍率 ----

    /** 耐性50は12、150は36、100は24ダメージになること(防御・ブレイクなし)。 */
    @Test
    fun multiplier_scalesDamage() {
        assertEquals(12, checkNotNull(field(50).performAttack(1, 2, fire, fixedRandom(0.5))).second.totalDamage)
        assertEquals(36, checkNotNull(field(150).performAttack(1, 2, fire, fixedRandom(0.5))).second.totalDamage)
        assertEquals(24, checkNotNull(field(100).performAttack(1, 2, fire, fixedRandom(0.5))).second.totalDamage)
    }

    /** 耐性1で結果が0になるときは最低1ダメージになること。 */
    @Test
    fun multiplier_minimumOne() {
        val (after, result) = checkNotNull(field(1).performAttack(1, 2, fire, fixedRandom(0.5)))

        assertEquals(1, result.totalDamage)
        assertEquals(999, after.enemies.single().currentHp)
    }

    /** 倍率のあとに鋼の体の軽減がかかること(24×200%=48、鋼の体で28)。 */
    @Test
    fun multiplier_thenPassiveReduction() {
        val f = BattleField(
            allies = listOf(testAlly(1, passiveSkills = listOf(TEST_STEEL_BODY), elementResistances = resist(200))),
            enemies = listOf(testEnemy(2)),
            turnQueue = listOf(2),
        )

        val (_, result) = checkNotNull(f.performAttack(2, 1, fire, fixedRandom(0.5)))

        assertEquals(28, result.totalDamage)
    }

    /** 無属性のスキルは、炎の耐性があっても影響を受けないこと。 */
    @Test
    fun noneElement_ignoresResistances() {
        val (_, result) = checkNotNull(field(0).performAttack(1, 2, TEST_NORMAL_ATTACK, fixedRandom(0.5)))

        assertEquals(24, result.totalDamage)
        assertEquals(ResistanceOutcome.NONE, result.hits.single().shots.single().resistance)
    }

    /** 防御中の対象は、防御の30%カットの後に耐性がかかること(24→16→耐性50で8)。 */
    @Test
    fun resistance_appliesAfterDefense() {
        val f = field(50).let { it.copy(enemies = listOf(it.enemies.single().copy(isDefending = true))) }

        val (_, result) = checkNotNull(f.performAttack(1, 2, fire, fixedRandom(0.5)))

        assertEquals(8, result.totalDamage)
    }

    // ---- 無効 ----

    /** 耐性0・負数(-2など)はダメージ0で、HP・ブレイクゲージ・対象のTPが変わらず、攻撃者はTPを得ること。 */
    @Test
    fun nullify_noDamage_noBreak_noTargetTp_attackerGainsTp() {
        for (value in listOf(0, -2, -500)) {
            val (after, result) = checkNotNull(field(value).performAttack(1, 2, fire, fixedRandom(0.5)))

            val shot = result.hits.single().shots.single()
            assertTrue(shot.isHit)
            assertEquals(ResistanceOutcome.NULLIFIED, shot.resistance)
            assertEquals(0, shot.damage)
            assertEquals(1000, after.enemies.single().currentHp)
            assertEquals(0.0, after.enemies.single().breakGauge, 0.0)
            assertEquals(0, after.enemies.single().currentTp)
            assertEquals(12, after.allies.single().currentTp)
        }
    }

    /** 無効は鋼の体があってもダメージ0(最低1にならない)こと。 */
    @Test
    fun nullify_withSteelBody_staysZero() {
        val f = BattleField(
            allies = listOf(testAlly(1, passiveSkills = listOf(TEST_STEEL_BODY), elementResistances = resist(0))),
            enemies = listOf(testEnemy(2, attack = 300)),
            turnQueue = listOf(2),
        )

        val (after, result) = checkNotNull(f.performAttack(2, 1, fire, fixedRandom(0.5)))

        assertEquals(0, result.totalDamage)
        assertEquals(100, after.allies.single().currentHp)
    }

    /** 外れたヒットは耐性に関係なく何も起きず(無効扱いにもならず)、命中扱いにもならないこと。 */
    @Test
    fun miss_withNullify_isPlainMiss() {
        val (after, result) = checkNotNull(field(0).performAttack(1, 2, fire.copy(accuracy = 90), fixedRandom(0.95)))

        val shot = result.hits.single().shots.single()
        assertFalse(shot.isHit)
        assertEquals(ResistanceOutcome.NONE, shot.resistance)
        assertEquals(0, after.allies.single().currentTp)
    }

    // ---- 吸収 ----

    /** 吸収は対象のダメージが0で、耐性100なら受けたはずのダメージ(24)だけHPが回復すること。 */
    @Test
    fun absorb_healsByWouldBeDamage() {
        val (after, result) = checkNotNull(field(-999, enemyHp = 500).performAttack(1, 2, fire, fixedRandom(0.5)))

        val shot = result.hits.single().shots.single()
        assertEquals(ResistanceOutcome.ABSORBED, shot.resistance)
        assertEquals(0, shot.damage)
        assertEquals(24, shot.absorbedAmount)
        assertEquals(524, after.enemies.single().currentHp)
        assertEquals(0.0, after.enemies.single().breakGauge, 0.0)
        assertEquals(0, after.enemies.single().currentTp)
    }

    /** 吸収の回復は最大HPで頭打ちになり、実際に回復した量が記録されること。 */
    @Test
    fun absorb_cappedByMaxHp() {
        val (after, result) = checkNotNull(field(-999, enemyHp = 990).performAttack(1, 2, fire, fixedRandom(0.5)))

        assertEquals(10, result.hits.single().shots.single().absorbedAmount)
        assertEquals(1000, after.enemies.single().currentHp)
    }

    /** 吸収量には防御(24→16)・鋼の体(20軽減で最低1)が込みで反映されること。 */
    @Test
    fun absorb_includesDefenseAndReduction() {
        val defending = field(-999, enemyHp = 500).let { it.copy(enemies = listOf(it.enemies.single().copy(isDefending = true))) }
        assertEquals(16, checkNotNull(defending.performAttack(1, 2, fire, fixedRandom(0.5))).second.hits.single().shots.single().absorbedAmount)

        val steel = BattleField(
            allies = listOf(testAlly(1, hp = 100, passiveSkills = listOf(TEST_STEEL_BODY), elementResistances = resist(-999))),
            enemies = listOf(testEnemy(2)),
            turnQueue = listOf(2),
        ).let { it.copy(allies = listOf(it.allies.single().copy(currentHp = 50))) }
        // 24 - 20 = 4 (軽減が1以上あるのでmax(…, 1))
        assertEquals(4, checkNotNull(steel.performAttack(2, 1, fire, fixedRandom(0.5))).second.hits.single().shots.single().absorbedAmount)
    }

    // ---- 反射 ----

    /** 攻撃100・耐久200の攻撃者に、対象(耐久100)から反射する。攻撃者は自分の耐久で計算した24を受ける。 */
    private fun reflectAttacker() = testAlly(1, hp = 100, attack = 200, defense = 200)

    /** 反射は対象に無傷で、攻撃者が自分の破壊力・耐久力で計算したダメージ(24。通常なら46)を受けること。 */
    @Test
    fun reflect_attackerTakesSelfDamage_targetUnharmed() {
        val (after, result) = checkNotNull(field(-1, attacker = reflectAttacker()).performAttack(1, 2, fire, fixedRandom(0.5)))

        val shot = result.hits.single().shots.single()
        assertEquals(ResistanceOutcome.REFLECTED, shot.resistance)
        assertEquals(0, shot.damage)
        assertEquals(24, shot.reflected?.damage)
        assertEquals(1000, after.enemies.single().currentHp)
        assertEquals(100 - 24, after.allies.single().currentHp)
    }

    /** 反射で、対象のTP・ブレイクは増えず、攻撃者はTP(攻撃を当てた12+受けたダメージ24%→24)とブレイクゲージ(24×1.5=36)を得ること。 */
    @Test
    fun reflect_tpAndBreak() {
        val (after, _) = checkNotNull(field(-1, attacker = reflectAttacker()).performAttack(1, 2, fire, fixedRandom(0.5)))

        assertEquals(0, after.enemies.single().currentTp)
        assertEquals(0.0, after.enemies.single().breakGauge, 0.0)
        assertEquals(12 + 24, after.allies.single().currentTp)
        assertEquals(36.0, after.allies.single().breakGauge, 0.0001)
    }

    /** 攻撃者が防御中なら反射ダメージは30%カットされ(24→16)、攻撃者のTPは防御中の1.5倍(16%→24)で貯まること。 */
    @Test
    fun reflect_attackerDefending() {
        val attacker = reflectAttacker().copy(isDefending = true)

        val (after, result) = checkNotNull(field(-1, attacker = attacker).performAttack(1, 2, fire, fixedRandom(0.5)))

        assertEquals(16, result.hits.single().shots.single().reflected?.damage)
        assertEquals(12 + 24, after.allies.single().currentTp)
    }

    /** クリティカルは反射されたヒットの判定結果をそのまま使い(24×150%=36)、乱数は引き直さないこと。 */
    @Test
    fun reflect_usesSameCriticalJudgement_noExtraRandom() {
        val plain = CountingRandom(criticalRoll = 0)
        val reflected = CountingRandom(criticalRoll = 0)
        checkNotNull(field(100, attacker = reflectAttacker()).performAttack(1, 2, fire, plain))

        val (_, result) = checkNotNull(field(-1, attacker = reflectAttacker()).performAttack(1, 2, fire, reflected))

        val shot = result.hits.single().shots.single()
        assertEquals(CriticalType.CRITICAL, shot.reflected?.critical)
        assertEquals(36, shot.reflected?.damage)
        assertEquals(plain.ints, reflected.ints)
        assertEquals(plain.doubles, reflected.doubles)
    }

    /** 攻撃者が防御中なら、反射されたクリティカルは無効化されて通常ヒット扱い(16)になること。 */
    @Test
    fun reflect_criticalBlockedByAttackerDefense() {
        val attacker = reflectAttacker().copy(isDefending = true)

        val (_, result) = checkNotNull(field(-1, attacker = attacker).performAttack(1, 2, fire, CountingRandom(criticalRoll = 0)))

        val reflected = checkNotNull(result.hits.single().shots.single().reflected)
        assertEquals(CriticalType.NONE, reflected.critical)
        assertTrue(reflected.isCriticalBlocked)
        assertEquals(16, reflected.damage)
    }

    /** 攻撃者の鋼の体(20軽減)がかかる(24→4)こと。 */
    @Test
    fun reflect_attackerSteelBody() {
        val attacker = reflectAttacker().copy(passiveSkills = listOf(TEST_STEEL_BODY))

        val (_, result) = checkNotNull(field(-1, attacker = attacker).performAttack(1, 2, fire, fixedRandom(0.5)))

        assertEquals(4, result.hits.single().shots.single().reflected?.damage)
    }

    /** 攻撃者自身の属性耐性(50)が反射ダメージにも適用されること(24→12)。 */
    @Test
    fun reflect_attackerResistanceMultiplier() {
        val attacker = testAlly(1, attack = 200, defense = 200, elementResistances = resist(50))

        val (_, result) = checkNotNull(field(-1, attacker = attacker).performAttack(1, 2, fire, fixedRandom(0.5)))

        assertEquals(12, result.hits.single().shots.single().reflected?.damage)
    }

    /** 攻撃者の耐性が無効(0)なら反射ダメージは0で、攻撃者のHPは減らず無効扱いになること。 */
    @Test
    fun reflect_attackerNullifies() {
        val attacker = testAlly(1, attack = 200, defense = 200, elementResistances = resist(0))

        val (after, result) = checkNotNull(field(-1, attacker = attacker).performAttack(1, 2, fire, fixedRandom(0.5)))

        val reflected = checkNotNull(result.hits.single().shots.single().reflected)
        assertEquals(ResistanceOutcome.NULLIFIED, reflected.outcome)
        assertEquals(0, reflected.damage)
        assertEquals(100, after.allies.single().currentHp)
        assertEquals(12, after.allies.single().currentTp)
    }

    /** 攻撃者も反射(-1)なら再反射せず無効扱いになり、対象にも攻撃者にもダメージがないこと。 */
    @Test
    fun reflect_bothReflect_noReReflect() {
        val attacker = testAlly(1, attack = 200, defense = 200, elementResistances = resist(-1))

        val (after, result) = checkNotNull(field(-1, attacker = attacker).performAttack(1, 2, fire, fixedRandom(0.5)))

        assertEquals(ResistanceOutcome.NULLIFIED, result.hits.single().shots.single().reflected?.outcome)
        assertEquals(100, after.allies.single().currentHp)
        assertEquals(1000, after.enemies.single().currentHp)
    }

    /** 攻撃者が吸収(-999)なら、反射ダメージ分のHPを攻撃者が回復する(最大HPで頭打ち)こと。 */
    @Test
    fun reflect_attackerAbsorbs() {
        val attacker = testAlly(1, attack = 200, defense = 200, elementResistances = resist(-999)).let { it.copy(currentHp = 90) }

        val (after, result) = checkNotNull(field(-1, attacker = attacker).performAttack(1, 2, fire, fixedRandom(0.5)))

        val reflected = checkNotNull(result.hits.single().shots.single().reflected)
        assertEquals(ResistanceOutcome.ABSORBED, reflected.outcome)
        assertEquals(10, reflected.absorbedAmount)
        assertEquals(100, after.allies.single().currentHp)
    }

    /** 多段で攻撃者が反射ダメージでブレイクしたら、その後の反射ヒットはブレイク中×1.3(24→31)で計算されること。 */
    @Test
    fun reflect_attackerBreaks_thenBrokenBoost() {
        val attacker = reflectAttacker().copy(breakGauge = 90.0)

        val (after, result) = checkNotNull(
            field(-1, attacker = attacker).performAttack(1, 2, fire.copy(activationCount = 2), fixedRandom(0.5)),
        )

        val shots = result.hits.single().shots
        assertTrue(checkNotNull(shots[0].reflected).causedBreak)
        assertEquals(24, shots[0].reflected?.damage)
        assertFalse(checkNotNull(shots[1].reflected).causedBreak)
        assertEquals(31, shots[1].reflected?.damage)
        assertEquals(BreakState.BROKEN, after.allies.single().breakState)
    }

    /** 反射ダメージで攻撃者が戦闘不能になったら、多段の残りのヒットが打ち切られること。 */
    @Test
    fun reflect_attackerDefeated_cutsOffRemainingShots() {
        val attacker = reflectAttacker().copy(currentHp = 20)

        val (after, result) = checkNotNull(
            field(-1, attacker = attacker).performAttack(1, 2, fire.copy(activationCount = 3), fixedRandom(0.5)),
        )

        assertEquals(1, result.hits.single().shots.size)
        assertEquals(0, after.allies.single().currentHp)
    }

    /** 反射ダメージで攻撃者が戦闘不能になったら、範囲攻撃の残りの対象が打ち切られること。倒れなければ続くこと。 */
    @Test
    fun reflect_attackerDefeated_cutsOffRemainingTargets() {
        val area = fire.copy(area = SkillArea.AREA, targetSelection = SkillTargetSelection.ALL)
        fun areaField(attackerHp: Int) = BattleField(
            allies = listOf(reflectAttacker().copy(currentHp = attackerHp)),
            enemies = listOf(
                testEnemy(2, hp = 1000, slot = 0, elementResistances = resist(-1)),
                testEnemy(3, hp = 1000, slot = 1, elementResistances = resist(-1)),
            ),
            turnQueue = listOf(1),
        )

        val defeated = checkNotNull(areaField(20).performAttack(1, 2, area, fixedRandom(0.5))).second
        val survived = checkNotNull(areaField(100).performAttack(1, 2, area, fixedRandom(0.5))).second

        assertEquals(listOf(2), defeated.hits.map { it.targetFpid })
        assertEquals(listOf(2, 3), survived.hits.map { it.targetFpid })
    }

    /** 反射を受けた攻撃者が倒れても、手番を終えられ戦闘が次へ進むこと(勝敗は敗北)。 */
    @Test
    fun reflect_attackerDefeated_battleOutcomeIsDefeat() {
        val attacker = reflectAttacker().copy(currentHp = 20)

        val (after, _) = checkNotNull(field(-1, attacker = attacker).performAttack(1, 2, fire, fixedRandom(0.5)))

        assertNotNull(after.outcome)
        assertEquals(jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleOutcome.Defeat, after.outcome)
    }

    // ---- 乱数の順序 ----

    /** 無効・吸収・反射でも、乱数の引かれる回数は耐性100のときと同じ(新規乱数なし)であること。 */
    @Test
    fun randomConsumption_unchangedByResistance() {
        val baseline = CountingRandom().also { checkNotNull(field(100, attacker = reflectAttacker()).performAttack(1, 2, fire, it)) }

        for (value in listOf(0, -999, -1)) {
            val random = CountingRandom()
            checkNotNull(field(value, attacker = reflectAttacker()).performAttack(1, 2, fire, random))
            assertEquals("耐性$value", baseline.ints, random.ints)
            assertEquals("耐性$value", baseline.doubles, random.doubles)
        }
    }

    /** 無効・吸収・反射のヒットも結果上は命中扱い(isHit=true)であること。 */
    @Test
    fun resistedHits_areCountedAsHits() {
        for (value in listOf(0, -999, -1)) {
            val result = checkNotNull(field(value, attacker = reflectAttacker()).performAttack(1, 2, fire, fixedRandom(0.5))).second
            assertTrue("耐性$value", result.hits.single().shots.single().isHit)
            assertFalse("耐性$value", result.hits.single().isMissed)
        }
    }

    /** 倍率の耐性では反射情報が付かないこと。 */
    @Test
    fun multiplier_hasNoReflectedInfo() {
        val shot = checkNotNull(field(150).performAttack(1, 2, fire, fixedRandom(0.5))).second.hits.single().shots.single()

        assertNull(shot.reflected)
        assertEquals(0, shot.absorbedAmount)
    }
}
