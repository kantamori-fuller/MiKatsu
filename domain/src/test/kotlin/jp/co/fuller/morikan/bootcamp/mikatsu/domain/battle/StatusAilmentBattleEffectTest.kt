package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AilmentState
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleAlly
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleEnemy
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleStat
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BreakState
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.CriticalType
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.ElementResistances
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.StatModifier
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.StatusAilment
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillCost
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillCostType
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 戦闘中の状態異常の効果を検証するテスト。行動不能(攻撃・スキル・防御・先制)、睡眠・凍結の解除、ダメージ計算の順序
 * (凍結×2・流血×1.2・石化1固定)、睡眠の必ずクリティカル、凍傷・麻痺・目眩の補正、MP・TPスキル封印を確かめる。
 *
 * 通常攻撃(威力100)の素のダメージは、破壊力100・耐久力100のとき24(クリティカル・補正なし)。
 */
class StatusAilmentBattleEffectTest {

    /** 補正のない素のダメージ。 */
    private val base = 24

    private fun field(
        ally: (BattleAlly) -> BattleAlly = { it },
        enemy: (BattleEnemy) -> BattleEnemy = { it },
    ): BattleField = BattleField(
        allies = listOf(
            ally(testAlly(1, mp = 100, activeSkills = listOf(TEST_NORMAL_ATTACK, TEST_DIA, TEST_SOBAT, TEST_SEISHIN))),
            testAlly(2),
        ),
        enemies = listOf(enemy(testEnemy(4, hp = 1000))),
        turnQueue = listOf(1, 4),
    )

    private fun BattleField.attack(random: SequenceRandom = SequenceRandom()) = performAttack(1, 4, TEST_NORMAL_ATTACK, random)

    // ---- 行動不能 ----

    private val incapacitating = listOf(
        StatusAilment.STUN, StatusAilment.FREEZE, StatusAilment.SHOCK, StatusAilment.PETRIFY, StatusAilment.SLEEP,
    )

    /** 行動不能の状態異常にかかった個体は、攻撃・スキル・防御ができない(ブレイクと違い、カードの手番は飛ばされる)。 */
    @Test
    fun incapacitated_cannotAttackUseSkillOrDefend() {
        incapacitating.forEach { ailment ->
            val f = field(ally = { it.withAilments(ail(ailment)) })

            assertNull("$ailment 攻撃", f.attack())
            assertNull("$ailment スキル", f.performSkill(1, TEST_SEISHIN.id, 1, SequenceRandom()))
            assertNull("$ailment 防御", f.defend(1))
            assertTrue("$ailment", f.pawn(1).isIncapacitated)
        }
    }

    /** 行動不能にならない状態異常では、通常どおり行動できる。 */
    @Test
    fun otherAilments_canAct() {
        val f = field(ally = { it.withAilments(ail(StatusAilment.POISON), ail(StatusAilment.MP_SEAL)) })

        assertNotNull(f.attack())
        assertNotNull(f.defend(1))
        assertNull(f.pawn(1).incapacitatingAilment)
    }

    /** 複数が行動不能にする場合は、enum順の先頭が理由になる。 */
    @Test
    fun incapacitatingAilment_isFirstInEnumOrder() {
        val pawn = field(ally = { it.withAilments(ail(StatusAilment.SLEEP), ail(StatusAilment.SHOCK), ail(StatusAilment.STUN)) }).pawn(1)

        assertEquals(StatusAilment.STUN, pawn.incapacitatingAilment)
    }

    /** ブレイクと違い、行動不能の対象は被ダメージが増えず、必中にもならない。 */
    @Test
    fun incapacitatedTarget_noBreakBonus() {
        val stunned = field(enemy = { it.withAilments(ail(StatusAilment.STUN)) })

        val (_, result) = checkNotNull(stunned.attack())
        assertEquals(base, result.hits.single().damage)

        val weak = TEST_NORMAL_ATTACK.copy(accuracy = 50)
        val missed = checkNotNull(stunned.performAttack(1, 4, weak, SequenceRandom(doubles = listOf(0.9)))).second
        assertTrue(missed.hits.single().isMissed)
    }

    /** 行動不能になった味方は先制行動を予約できず、予約済みでも先制の手番は取り消される。 */
    @Test
    fun incapacitated_cannotReservePreemptive_andCancelsPreemptiveTurn() {
        fun withShooter(vararg ailments: AilmentState) = BattleField(
            allies = listOf(
                testAlly(1),
                testAlly(2, mp = 100, activeSkills = listOf(TEST_QUICK_SHOT)).copy(currentTp = 50).withAilments(*ailments),
            ),
            enemies = listOf(testEnemy(4, hp = 1000)),
            turnQueue = listOf(1, 4),
        )

        assertTrue(withShooter().canReservePreemptive(2))
        assertFalse(withShooter(ail(StatusAilment.SLEEP)).canReservePreemptive(2))

        val reserved = checkNotNull(withShooter().reservePreemptive(2))
        val asleep = reserved.copy(allies = reserved.allies.map { if (it.fpid == 2) it.withAilments(ail(StatusAilment.SLEEP)) else it })
        val (_, cancelled) = asleep.endTurnWithCancellations(1, SequenceRandom())
        assertEquals(listOf(2), cancelled)
    }

    // ---- 睡眠・凍結の解除 ----

    /** HPが実際に減ったヒットの直後に睡眠・凍結が解除される。 */
    @Test
    fun damage_releasesSleepAndFreeze() {
        val (after, result) = checkNotNull(
            field(enemy = { it.withAilments(ail(StatusAilment.FREEZE), ail(StatusAilment.SLEEP), ail(StatusAilment.POISON)) }).attack(),
        )

        assertEquals(listOf(StatusAilment.POISON), after.pawn(4).ailmentKinds())
        assertEquals(listOf(StatusAilment.FREEZE, StatusAilment.SLEEP), result.hits.single().shots.single().curedAilments)
    }

    /** 外れ・無効ではHPが減らないので解除されない。 */
    @Test
    fun missAndNullify_doNotRelease() {
        val sleeping = field(enemy = { it.withAilments(ail(StatusAilment.SLEEP)) })
        val missed = checkNotNull(
            sleeping.performAttack(1, 4, TEST_NORMAL_ATTACK.copy(accuracy = 10), SequenceRandom(doubles = listOf(0.9))),
        ).first
        assertEquals(listOf(StatusAilment.SLEEP), missed.pawn(4).ailmentKinds())

        val fire = TEST_NORMAL_ATTACK.copy(element = SkillElement.FIRE)
        val immune = field(enemy = { it.copy(enemy = it.enemy.copy(elementResistances = ElementResistances(fire = 0))).withAilments(ail(StatusAilment.SLEEP)) })
        val nullified = checkNotNull(immune.performAttack(1, 4, fire, SequenceRandom())).first
        assertEquals(listOf(StatusAilment.SLEEP), nullified.pawn(4).ailmentKinds())
    }

    // ---- ダメージ計算の順序 ----

    /** 凍結中の対象へのヒットはダメージ2倍で、そのヒットで凍結が解けるため、多段の2発目以降は2倍にならない。 */
    @Test
    fun freeze_doublesOnlyFirstHit() {
        val twice = TEST_NORMAL_ATTACK.copy(activationCount = 2)
        val f = field(enemy = { it.withAilments(ail(StatusAilment.FREEZE)) })

        val (after, result) = checkNotNull(f.performAttack(1, 4, twice, SequenceRandom()))

        assertEquals(listOf(base * 2, base), result.hits.single().shots.map { it.damage })
        assertTrue(after.pawn(4).ailments.isEmpty())
    }

    /** 流血中は1.2倍(切り捨て)で、凍結と重なると凍結×2の後に流血×1.2を掛ける。 */
    @Test
    fun bleed_boostsByTwentyPercent_afterFreeze() {
        val bleed = checkNotNull(field(enemy = { it.withAilments(ail(StatusAilment.BLEED)) }).attack()).second
        assertEquals(base * 12 / 10, bleed.hits.single().damage)

        val both = checkNotNull(
            field(enemy = { it.withAilments(ail(StatusAilment.FREEZE), ail(StatusAilment.BLEED)) }).attack(),
        ).second
        assertEquals(base * 2 * 12 / 10, both.hits.single().damage)
    }

    /** 凍結・流血の補正は、防御の30%カットより前に掛かる(計算順: ブレイク → 凍結 → 流血 → 防御)。 */
    @Test
    fun freezeAndBleed_applyBeforeDefense() {
        val f = field(enemy = { it.copy(isDefending = true).withAilments(ail(StatusAilment.FREEZE), ail(StatusAilment.BLEED)) })

        val damage = checkNotNull(f.attack()).second.hits.single().damage

        assertEquals((base * 2 * 12 / 10) * 70 / 100, damage)
    }

    /** 石化中は無効・吸収・反射の耐性があっても常にダメージ1で、HPが1減る。 */
    @Test
    fun petrify_fixesDamageToOne_ignoringResistance() {
        val fire = TEST_NORMAL_ATTACK.copy(element = SkillElement.FIRE)
        listOf(0, -999, -1, 100).forEach { value ->
            val f = field(enemy = {
                it.copy(enemy = it.enemy.copy(elementResistances = ElementResistances(fire = value)))
                    .withAilments(ail(StatusAilment.PETRIFY))
            })

            val (after, result) = checkNotNull(f.performAttack(1, 4, fire, SequenceRandom()))

            val shot = result.hits.single().shots.single()
            assertEquals("耐性$value", 1, shot.damage)
            assertEquals("耐性$value", 999, after.pawn(4).currentHp)
            assertNull("耐性$value", shot.reflected)
            assertEquals("耐性$value", 100, after.pawn(1).currentHp)
        }
    }

    /** 反射の再計算にも流血×1.2が掛かる。石化した味方は行動不能なので攻撃できない。 */
    @Test
    fun reflect_appliesBleedToAttacker_andPetrifiedCannotAct() {
        val fire = TEST_NORMAL_ATTACK.copy(element = SkillElement.FIRE)
        val f = field(
            ally = { it.withAilments(ail(StatusAilment.PETRIFY, turns = 3)).copy(character = it.character) },
            enemy = { it.copy(enemy = it.enemy.copy(elementResistances = ElementResistances(fire = -1))) },
        )
        // 石化した味方は行動不能なので、石化していない味方で、攻撃者自身が流血中の反射を確かめる。
        val bleeding = field(
            ally = { it.withAilments(ail(StatusAilment.BLEED)) },
            enemy = { it.copy(enemy = it.enemy.copy(elementResistances = ElementResistances(fire = -1))) },
        )
        val (after, result) = checkNotNull(bleeding.performAttack(1, 4, fire, SequenceRandom()))

        assertEquals(base * 12 / 10, result.hits.single().shots.single().reflected?.damage)
        assertEquals(100 - base * 12 / 10, after.pawn(1).currentHp)
        assertNull(f.attack())
    }

    // ---- 睡眠の必ずクリティカル ----

    /** 睡眠中の対象へのヒットは、クリティカル判定の乱数を引かずにクリティカル扱いになり、攻撃者の倍率(150%)が掛かる。 */
    @Test
    fun sleep_alwaysCritical_withoutDrawingRandom() {
        val random = SequenceRandom()

        val (after, result) = checkNotNull(field(enemy = { it.withAilments(ail(StatusAilment.SLEEP)) }).attack(random))

        val shot = result.hits.single().shots.single()
        assertEquals(CriticalType.CRITICAL, shot.critical)
        assertEquals(base * 150 / 100, shot.damage)
        assertFalse(24 in random.untils)
        assertEquals(listOf(StatusAilment.SLEEP), shot.curedAilments)
        assertTrue(after.pawn(4).ailments.isEmpty())
    }

    /** 睡眠での必ずクリティカルは、目覚める前の1発目だけ。2発目は通常どおり判定を引く。 */
    @Test
    fun sleep_criticalOnlyBeforeWakingUp() {
        val random = SequenceRandom()
        val twice = TEST_NORMAL_ATTACK.copy(activationCount = 2)

        val (_, result) = checkNotNull(
            field(enemy = { it.withAilments(ail(StatusAilment.SLEEP)) }).performAttack(1, 4, twice, random),
        )

        val shots = result.hits.single().shots
        assertEquals(listOf(CriticalType.CRITICAL, CriticalType.NONE), shots.map { it.critical })
        assertEquals(base, shots[1].damage)
        assertEquals(1, random.untils.count { it == 24 })
    }

    /** 防御中の睡眠の対象には、必ずクリティカルでもCRITICALが無効になる。 */
    @Test
    fun sleep_criticalIsBlockedByDefense() {
        val f = field(enemy = { it.copy(isDefending = true).withAilments(ail(StatusAilment.SLEEP)) })

        val shot = checkNotNull(f.attack()).second.hits.single().shots.single()

        assertEquals(CriticalType.NONE, shot.critical)
        assertTrue(shot.isCriticalBlocked)
        assertEquals(base * 70 / 100, shot.damage)
    }

    /** 攻撃者のクリティカル倍率が100%未満になる段階なら、睡眠の必ずクリティカルでもバッドクリティカルと表記する。 */
    @Test
    fun sleep_classifiedByFinalRate() {
        val lowRate = StatModifier(BattleStat.CRITICAL_DAMAGE, stages = -3, remainingTurns = null)
        val f = field(ally = { it.copy(statModifiers = listOf(lowRate)) }, enemy = { it.withAilments(ail(StatusAilment.SLEEP)) })

        val shot = checkNotNull(f.attack()).second.hits.single().shots.single()

        assertEquals(CriticalType.BAD_CRITICAL, shot.critical)
        assertEquals(base * 75 / 100, shot.damage)
    }

    // ---- 凍傷・麻痺・目眩 ----

    /** 凍傷は、段階補正後の破壊力に4/6を掛けて切り捨てる。段階の上限(±12)には数えない。 */
    @Test
    fun frostbite_scalesAttackAfterStage() {
        assertEquals(66, field(ally = { it.withAilments(ail(StatusAilment.FROSTBITE)) }).pawn(1).effectiveStat(BattleStat.ATTACK))
        val boosted = field(ally = {
            it.copy(statModifiers = listOf(StatModifier(BattleStat.ATTACK, 4, null))).withAilments(ail(StatusAilment.FROSTBITE))
        }).pawn(1)
        assertEquals(133, boosted.effectiveStat(BattleStat.ATTACK))
        assertEquals(4, boosted.effectiveStage(BattleStat.ATTACK))
        assertEquals(100, field().pawn(1).effectiveStat(BattleStat.ATTACK))
    }

    /** 麻痺は素早さを0.7倍(切り捨て)にし、ブレイクゲージの計算に使う耐性も0.7倍にする。保存している耐性は変えない。 */
    @Test
    fun paralysis_scalesAgilityAndBreakResistance() {
        val pawn = field(enemy = { it.withAilments(ail(StatusAilment.PARALYSIS)) }).pawn(4)

        assertEquals(70, pawn.effectiveStat(BattleStat.AGILITY))
        assertEquals(100, pawn.breakResistance)
        assertEquals(70, pawn.effectiveBreakResistance())

        val normal = field().accumulateBreakGauge(150, 4, 100).first.pawn(4).breakGauge
        val paralyzed = field(enemy = { it.withAilments(ail(StatusAilment.PARALYSIS)) }).accumulateBreakGauge(150, 4, 100).first.pawn(4).breakGauge
        assertEquals(1000 / 1000.0 * 15.0, normal, 1e-9)
        assertEquals(10.5, paralyzed, 1e-9)
        assertEquals(BreakState.NONE, field().pawn(4).breakState)
    }

    /** 目眩は攻撃者の最終命中率を0.4倍にする。必中(命中率null・ブレイク中の対象)はそのまま必中。 */
    @Test
    fun dizziness_scalesFinalHitChance() {
        assertEquals(40.0, HitChanceFormula.finalChance(100, 0, 0, isAttackerDizzy = true), 1e-9)
        assertEquals(100.0, HitChanceFormula.finalChance(null, 0, 0, isAttackerDizzy = true), 1e-9)

        val dizzy = field(ally = { it.withAilments(ail(StatusAilment.DIZZINESS)) })
        // 命中率100の通常攻撃でも、乱数45(≥40)は外れ、乱数35(<40)は当たる。
        assertTrue(checkNotNull(dizzy.attack(SequenceRandom(doubles = listOf(0.45)))).second.hits.single().isMissed)
        assertFalse(checkNotNull(dizzy.attack(SequenceRandom(doubles = listOf(0.35)))).second.hits.single().isMissed)
        assertFalse(checkNotNull(field().attack(SequenceRandom(doubles = listOf(0.45)))).second.hits.single().isMissed)
    }

    /** 目眩はスキル(performSkill)の命中判定にも効く。 */
    @Test
    fun dizziness_affectsSkillHit() {
        val skill = ailmentSkill(accuracy = 100)
        val f = BattleField(
            allies = listOf(testAlly(1, activeSkills = listOf(skill)).withAilments(ail(StatusAilment.DIZZINESS))),
            enemies = listOf(testEnemy(4)),
            turnQueue = listOf(1, 4),
        )

        val (_, missed) = checkNotNull(f.performSkill(1, skill.id, 4, SequenceRandom(doubles = listOf(0.45))))
        val (_, hit) = checkNotNull(f.performSkill(1, skill.id, 4, SequenceRandom(doubles = listOf(0.35))))

        assertFalse(missed.targets.single().isHit)
        assertTrue(hit.targets.single().isHit)
    }

    // ---- 封印 ----

    /** MPスキル封印はコストにMPを含むスキルを使えなくし、TPスキル封印はコストにTPを含むスキルを使えなくする。通常攻撃は使える。 */
    @Test
    fun seals_blockSkillsByCostType() {
        val mpSealed = field(ally = { it.withAilments(ail(StatusAilment.MP_SEAL)).copy(currentTp = 100) })
        assertFalse(mpSealed.pawn(1).let { (it as BattleAlly).canAfford(TEST_DIA) })
        assertNull(mpSealed.performSkill(1, TEST_DIA.id, 2, SequenceRandom()))
        assertFalse(mpSealed.isSkillUsable(1, TEST_DIA))
        assertTrue(mpSealed.isSkillUsable(1, TEST_SOBAT))
        assertNotNull(mpSealed.attack())

        val tpSealed = field(ally = { it.withAilments(ail(StatusAilment.TP_SEAL)).copy(currentTp = 100) })
        assertNull(tpSealed.performAttack(1, 4, TEST_SOBAT, SequenceRandom()))
        assertFalse(tpSealed.isSkillUsable(1, TEST_SOBAT))
        assertTrue(tpSealed.isSkillUsable(1, TEST_DIA))
        assertNotNull(tpSealed.attack())
    }

    /** コストにMPとTPの両方を含むスキルは、どちらの封印でも使えない。コストのないスキル(精神一到はMP20)以外は影響しない。 */
    @Test
    fun seals_mixedCost() {
        val mixed = TEST_DIA.copy(costs = listOf(SkillCost(SkillCostType.MP, 10), SkillCost(SkillCostType.TP, 10)))
        listOf(StatusAilment.MP_SEAL, StatusAilment.TP_SEAL).forEach { seal ->
            val ally = testAlly(1, mp = 100, activeSkills = listOf(mixed)).copy(currentTp = 100).withAilments(ail(seal))
            assertFalse("$seal", ally.canAfford(mixed))
        }
        val free = TEST_DIA.copy(costs = emptyList())
        assertTrue(testAlly(1, activeSkills = listOf(free)).withAilments(ail(StatusAilment.MP_SEAL)).canAfford(free))
    }
}
