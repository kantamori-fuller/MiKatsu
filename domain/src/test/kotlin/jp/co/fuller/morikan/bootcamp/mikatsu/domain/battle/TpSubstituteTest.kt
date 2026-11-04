package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleAlly
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.SkillUseResult
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkill
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillCost
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillCostType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * 乱数を引く値を固定するテスト用の乱数。
 *
 * `nextInt(until)`は積んだ値を順に返し(クリティカル判定の24、TPの獲得量の幅もこの1本の列を使う)、
 * 尽きたら`until - 1`を返す。`nextDouble`は命中判定用で[doubles]を順に(尽きたら最後の値を)返す。
 *
 * @param ints `nextInt(until)`が順に返す値。
 * @param doubles `nextDouble()`が順に返す値。省略時は0.0(必ず命中)。
 */
private class SubstituteRandom(ints: List<Int> = emptyList(), private val doubles: List<Double> = listOf(0.0)) : Random() {
    private val queue = ArrayDeque(ints)
    private var doubleIndex = 0

    override fun nextBits(bitCount: Int): Int = error("nextDouble・nextInt(until)以外は使わない")

    override fun nextInt(until: Int): Int = queue.removeFirstOrNull() ?: (until - 1)

    override fun nextDouble(): Double = doubles[minOf(doubleIndex++, doubles.lastIndex)]
}

/** TP代替システム(TPが足りない分をHPで肩代わりする)を検証するテスト。 */
class TpSubstituteTest {

    /** TPコスト40の、ダメージのない自分対象の変化スキル(マスターデータにはないのでテストで定義する)。 */
    private val tpSupport: ActiveSkill = TEST_SEISHIN.copy(
        id = 20,
        name = "TPサポート",
        costs = listOf(SkillCost(SkillCostType.TP, 40)),
    )

    /** 通常攻撃などで乱数が引かれても、クリティカルにならない値(`nextInt(24)`の最大)。 */
    private val noCrit = CriticalFormula.RATE_DENOMINATOR - 1

    /**
     * 味方1(手番)と敵2を並べたフィールドを作る。
     *
     * @param maxHp 味方1の最大HP(現在HPも同値)。
     * @param hp 味方1の現在HP。
     * @param tp 味方1の現在TP。
     * @param mp 味方1の現在MP。
     * @param skills 味方1が使えるスキル。
     * @return 生成したフィールド。
     */
    private fun field(
        maxHp: Int = 100,
        hp: Int = maxHp,
        tp: Int = 0,
        mp: Int = 0,
        skills: List<ActiveSkill> = emptyList(),
    ) = BattleField(
        allies = listOf(testAlly(1, hp = maxHp, mp = mp, activeSkills = skills).copy(currentHp = hp, currentTp = tp)),
        enemies = listOf(testEnemy(2, hp = 1000)),
        turnQueue = listOf(1),
    )

    /** 味方1を返す。 */
    private fun BattleField.ally(): BattleAlly = allies.single()

    // ---- 式 ----

    /** 仕様の例どおりに代替HPが求まること(切り上げ・不足0を含む)。 */
    @Test
    fun substituteHpCost_examples() {
        assertEquals(10, TacticalPointFormula.substituteHpCost(tpCost = 30, currentTp = 20, maxHp = 100))
        assertEquals(30, TacticalPointFormula.substituteHpCost(tpCost = 30, currentTp = 0, maxHp = 100))
        assertEquals(10, TacticalPointFormula.substituteHpCost(tpCost = 10, currentTp = 0, maxHp = 95)) // 9.5を切り上げ
        assertEquals(48, TacticalPointFormula.substituteHpCost(tpCost = 40, currentTp = 0, maxHp = 120))
    }

    /** TPが足りている(不足0)・ちょうど足りる・TPコストがないときは0であること。 */
    @Test
    fun substituteHpCost_noShortfall_isZero() {
        assertEquals(0, TacticalPointFormula.substituteHpCost(tpCost = 40, currentTp = 40, maxHp = 100))
        assertEquals(0, TacticalPointFormula.substituteHpCost(tpCost = 40, currentTp = 100, maxHp = 100))
        assertEquals(0, TacticalPointFormula.substituteHpCost(tpCost = 0, currentTp = 0, maxHp = 100))
    }

    /** 端数は常に切り上げられ、最大HPが小さくても不足があれば最低1は払うこと。 */
    @Test
    fun substituteHpCost_roundsUp() {
        assertEquals(1, TacticalPointFormula.substituteHpCost(tpCost = 1, currentTp = 0, maxHp = 1))
        assertEquals(1, TacticalPointFormula.substituteHpCost(tpCost = 1, currentTp = 0, maxHp = 50))
        assertEquals(2, TacticalPointFormula.substituteHpCost(tpCost = 3, currentTp = 0, maxHp = 50)) // 1.5
        assertEquals(100, TacticalPointFormula.substituteHpCost(tpCost = 100, currentTp = 0, maxHp = 100))
    }

    // ---- canAfford ----

    /** 代替後のHPがちょうど0になるときは使えず、1残るときは使えること。 */
    @Test
    fun canAfford_remainingHpBoundary() {
        // TP0・コスト40・最大HP100 → 代替HP40。
        assertFalse(field(hp = 40, skills = listOf(TEST_SOBAT)).ally().canAfford(TEST_SOBAT))
        assertTrue(field(hp = 41, skills = listOf(TEST_SOBAT)).ally().canAfford(TEST_SOBAT))
        assertEquals(40, field(hp = 40, skills = listOf(TEST_SOBAT)).ally().substituteHpCostOf(TEST_SOBAT))
    }

    /** HPコストと代替HPは合計で判定されること(HPコスト10+代替HP40=50)。 */
    @Test
    fun canAfford_combinesHpCostAndSubstitute() {
        val skill = TEST_SOBAT.copy(costs = listOf(SkillCost(SkillCostType.TP, 40), SkillCost(SkillCostType.HP, 10)))

        assertFalse(field(hp = 50, skills = listOf(skill)).ally().canAfford(skill))
        assertTrue(field(hp = 51, skills = listOf(skill)).ally().canAfford(skill))
        // TPが足りていれば代替HPは0なので、HPコスト10だけで判定され使える。
        assertTrue(field(hp = 50, tp = 40, skills = listOf(skill)).ally().canAfford(skill))
    }

    /** MPの不足は代替できず、TPが十分でもHPが十分でも使えないこと。 */
    @Test
    fun canAfford_mpShortfallIsNotSubstituted() {
        val skill = TEST_SOBAT.copy(costs = listOf(SkillCost(SkillCostType.TP, 40), SkillCost(SkillCostType.MP, 10)))

        assertFalse(field(mp = 9, tp = 100, skills = listOf(skill)).ally().canAfford(skill))
        assertFalse(field(mp = 9, tp = 0, skills = listOf(skill)).ally().canAfford(skill))
        assertTrue(field(mp = 10, tp = 0, skills = listOf(skill)).ally().canAfford(skill))
    }

    // ---- performAttack(攻撃スキル) ----

    /** TP20で使うと、TPが0になり(技のTP獲得は後から加算)、HPが代替HP(最大HP100の20%=20)だけ減ること。使用者のブレイクゲージは貯まらない。 */
    @Test
    fun attack_withPartialTp_paysShortfallWithHp() {
        val f = field(tp = 20, skills = listOf(TEST_SOBAT))

        val (after, use) = checkNotNull(f.performActiveSkill(1, TEST_SOBAT.id, 2, SubstituteRandom(listOf(noCrit, 2))))

        assertTrue(use is SkillUseResult.Attack)
        assertEquals(80, after.ally().currentHp)
        assertEquals(0 + 10, after.ally().currentTp) // TP0の状態へ条件1の8+2を加算
        assertEquals(0.0, after.ally().breakGauge, 0.0)
    }

    /** TP0で使うと代替HPは40で、HPが減る分で使用者のブレイクゲージは貯まらず、TPも(攻撃を受けた条件では)増えないこと。 */
    @Test
    fun attack_withNoTp_hpLossDoesNotFillBreakGaugeOrTp() {
        val f = field(tp = 0, skills = listOf(TEST_SOBAT.copy(accuracy = 0)))

        val (after, use) = checkNotNull(f.performActiveSkill(1, TEST_SOBAT.id, 2, SubstituteRandom(listOf(0), listOf(0.99))))

        assertTrue((use as SkillUseResult.Attack).result.hits.single().isMissed)
        assertEquals(60, after.ally().currentHp) // 外れてもHPは戻らない
        assertEquals(0, after.ally().currentTp) // 外れたので条件1のTPもなく、TPは0のまま
        assertEquals(0.0, after.ally().breakGauge, 0.0)
    }

    /** 代替でHPが0以下になるときは、performActiveSkillもperformAttackもnullを返し、何も変わらないこと。 */
    @Test
    fun attack_whenHpWouldReachZero_returnsNull() {
        val f = field(hp = 40, tp = 0, skills = listOf(TEST_SOBAT))

        assertNull(f.performActiveSkill(1, TEST_SOBAT.id, 2, SubstituteRandom()))
        assertNull(f.performAttack(1, 2, TEST_SOBAT, SubstituteRandom()))
        assertEquals(40, f.ally().currentHp)
    }

    /** TPが足りているときは従来どおりTPだけで払い、HPは減らないこと。 */
    @Test
    fun attack_withEnoughTp_doesNotLoseHp() {
        val f = field(tp = 50, skills = listOf(TEST_SOBAT))

        val (after, _) = checkNotNull(f.performActiveSkill(1, TEST_SOBAT.id, 2, SubstituteRandom(listOf(noCrit, 2))))

        assertEquals(100, after.ally().currentHp)
        assertEquals(50 - 40 + 10, after.ally().currentTp)
    }

    // ---- performSkill(ダメージのないスキル) ----

    /** ダメージのないTPスキルも、TP20ならHPが代替HP20だけ減り、TPは0になってから条件4のTPが加わること。 */
    @Test
    fun support_withPartialTp_paysShortfallWithHp() {
        val f = field(tp = 20, skills = listOf(tpSupport))

        val (after, _) = checkNotNull(f.performSkill(1, tpSupport.id, 1, SubstituteRandom(listOf(2))))

        assertEquals(80, after.ally().currentHp)
        assertEquals(10, after.ally().currentTp)
        assertEquals(0.0, after.ally().breakGauge, 0.0)
    }

    /** 命中判定のあるTPスキルが外れても、TPとHPは戻らないこと(TP0・HP代替40)。外れた自分対象は条件6のTPだけ受け取る。 */
    @Test
    fun support_miss_stillSpendsTpAndHp() {
        val skill = tpSupport.copy(accuracy = 0)
        val f = field(tp = 0, skills = listOf(skill))

        val (after, result) = checkNotNull(f.performSkill(1, skill.id, 1, SubstituteRandom(ints = listOf(0), doubles = listOf(0.99))))

        assertFalse(result.targets.single().isHit)
        assertEquals(60, after.ally().currentHp)
        // 自分を対象にして外れた(避けた扱い)ので、TP0の状態へ条件6の18が加わる(コストの40は戻らない)。
        assertEquals(18, after.ally().currentTp)
    }

    /** 代替でHPが0以下になるときはnullで、HPがちょうど1残るなら使えること。 */
    @Test
    fun support_hpBoundary() {
        assertNull(field(hp = 40, tp = 0, skills = listOf(tpSupport)).performSkill(1, tpSupport.id, 1, SubstituteRandom()))

        val (after, _) = checkNotNull(field(hp = 41, tp = 0, skills = listOf(tpSupport)).performSkill(1, tpSupport.id, 1, SubstituteRandom()))
        assertEquals(1, after.ally().currentHp)
    }

    /** TPが足りているときはHPが減らず、TPだけが減ること(回帰)。 */
    @Test
    fun support_withEnoughTp_doesNotLoseHp() {
        val f = field(tp = 100, skills = listOf(tpSupport))

        val (after, _) = checkNotNull(f.performSkill(1, tpSupport.id, 1, SubstituteRandom(listOf(0))))

        assertEquals(100, after.ally().currentHp)
        assertEquals(100 - 40 + 8, after.ally().currentTp)
    }

    /** 端数の切り上げが実際の消費にも反映されること(最大HP95・TP30・コスト40なら不足10で、9.5を切り上げた代替HP10)。 */
    @Test
    fun spend_roundsUpOnActualHp() {
        val f = field(maxHp = 95, tp = 30, skills = listOf(tpSupport)) // 不足10 → 9.5 → 10

        val (after, _) = checkNotNull(f.performSkill(1, tpSupport.id, 1, SubstituteRandom(listOf(0))))

        assertEquals(85, after.ally().currentHp)
    }
}
