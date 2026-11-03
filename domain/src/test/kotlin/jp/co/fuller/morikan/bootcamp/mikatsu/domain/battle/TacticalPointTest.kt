package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleEnemy
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleStat
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BreakState
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.CriticalType
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.SkillUseResult
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.StatModifier
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkill
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillArea
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillTargetSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * 乱数を引く値と順序を固定するテスト用の乱数。
 *
 * `nextInt(until)`は積んだ値を順に返し(クリティカル判定の24、TPの獲得量の幅3・4・5もこの1本の列を使う)、
 * 尽きたら`until - 1`(クリティカルにならず、獲得量は範囲の最大)を返す。`nextDouble`は命中判定用で[doubles]を順に(尽きたら最後の値を)返す。
 *
 * @param ints `nextInt(until)`が順に返す値。
 * @param doubles `nextDouble()`が順に返す値。省略時は0.0(必ず命中)。
 */
private class TpRandom(ints: List<Int> = emptyList(), private val doubles: List<Double> = listOf(0.0)) : Random() {
    private val queue = ArrayDeque(ints)
    private var doubleIndex = 0

    /** `nextInt(until)`を引いたときの`until`の記録(引いた順)。 */
    val untils = mutableListOf<Int>()

    override fun nextBits(bitCount: Int): Int = error("nextDouble・nextInt(until)以外は使わない")

    override fun nextInt(until: Int): Int {
        untils += until
        return queue.removeFirstOrNull() ?: (until - 1)
    }

    override fun nextDouble(): Double = doubles[minOf(doubleIndex++, doubles.lastIndex)]
}

/** TP(タクティカルポイント)の獲得・消費・持ち越しを検証するテスト。 */
class TacticalPointTest {

    /** クリティカル判定の分母。この幅の`nextInt`はクリティカル判定。 */
    private val critical = CriticalFormula.RATE_DENOMINATOR

    /** 判定でクリティカルにならない値(`nextInt(24)`の最大)。 */
    private val noCrit = critical - 1

    /**
     * 味方1(手番・前衛)と敵2・3(前衛)を並べたフィールドを作る。
     *
     * @param allyTp 味方1の現在TP。
     * @param skills 味方1が使えるスキル。
     * @param mp 味方1の現在MP。
     * @param enemyHp 敵の最大HP。
     * @return 生成したフィールド。
     */
    private fun field(
        allyTp: Int = 0,
        skills: List<ActiveSkill> = emptyList(),
        mp: Int = 0,
        enemyHp: Int = 1000,
    ) = BattleField(
        allies = listOf(testAlly(1, mp = mp, activeSkills = skills).copy(currentTp = allyTp)),
        enemies = listOf(testEnemy(2, hp = enemyHp, slot = 0), testEnemy(3, hp = enemyHp, slot = 1)),
        turnQueue = listOf(1),
    )

    /** 指定したFPIDの個体のTPを返す。 */
    private fun BattleField.tpOf(fpid: Int): Int = allPawns.first { it.fpid == fpid }.currentTp

    /** 指定したFPIDの個体を書き換えたフィールドを返す。 */
    private fun BattleField.withEnemy(fpid: Int, transform: (BattleEnemy) -> BattleEnemy) =
        copy(enemies = enemies.map { if (it.fpid == fpid) transform(it) else it })

    // ---- 基本 ----

    /** 加算は上限100で頭打ちになり、超えた分は捨てること。 */
    @Test
    fun gainTp_capsAt100() {
        assertEquals(100, field(allyTp = 95).gainTp(1, 10).tpOf(1))
        assertEquals(100, field(allyTp = 100).gainTp(1, 10).tpOf(1))
        assertEquals(55, field(allyTp = 50).gainTp(1, 5).tpOf(1))
    }

    /** 戦闘開始の+10が、味方・敵の全個体に加わり、上限100で頭打ちになること。 */
    @Test
    fun grantBattleStartTp_addsTenToEveryone_capped() {
        val f = field(allyTp = 95).grantBattleStartTp()

        assertEquals(100, f.tpOf(1))
        assertEquals(10, f.tpOf(2))
        assertEquals(10, f.tpOf(3))
    }

    // ---- 条件3: 手番開始 ----

    /** 手番が決まった個体は、同時手番のくじの後に6〜8を受け取ること(範囲の両端を確かめる)。 */
    @Test
    fun turnStart_gainsSixToEight() {
        val f = BattleField(allies = listOf(testAlly(1, agility = 500)), enemies = listOf(testEnemy(2, agility = 1)))

        assertEquals(6, f.advanceToNextTurn(TpRandom(listOf(0))).tpOf(1))
        assertEquals(7, f.advanceToNextTurn(TpRandom(listOf(1))).tpOf(1))
        assertEquals(8, f.advanceToNextTurn(TpRandom(listOf(2))).tpOf(1))
        assertEquals(0, f.advanceToNextTurn(TpRandom(listOf(2))).tpOf(2))
    }

    /** ブレイク中の個体も、自分の手番が来れば手番開始のTPを受け取ること。 */
    @Test
    fun turnStart_brokenActor_stillGains() {
        val f = BattleField(
            allies = listOf(testAlly(1, agility = 500).copy(breakState = BreakState.BROKEN)),
            enemies = listOf(testEnemy(2, agility = 1)),
        )

        assertEquals(8, f.advanceToNextTurn(TpRandom(listOf(2))).tpOf(1))
    }

    /** 勝敗が決まっているときは、手番開始のTPを加えないこと。 */
    @Test
    fun turnStart_afterOutcome_doesNotGain() {
        val f = field().withEnemy(2) { it.copy(currentHp = 0) }.withEnemy(3) { it.copy(currentHp = 0) }

        assertEquals(0, f.advanceToNextTurn(TpRandom()).tpOf(1))
    }

    /** 手番の終了で次の個体が決まると、その個体だけが受け取ること(キューの残りを使う分岐も含む)。 */
    @Test
    fun endTurn_nextActorGains_onlyOnce() {
        val f = BattleField(
            allies = listOf(testAlly(1, agility = 500)),
            enemies = listOf(testEnemy(2, agility = 1)),
            turnQueue = listOf(1),
        )

        val next = f.endTurn(1, TpRandom(listOf(1)))

        // 敵の手番が来るまでに味方の距離が進むため、先に手番が来るのは味方(素早さ500)。味方だけが受け取る。
        assertEquals(7, next.tpOf(1))
        assertEquals(0, next.tpOf(2))
    }

    // ---- 条件5: 防御 ----

    /** 防御が成立した瞬間に使用者が+5を受け取ること。 */
    @Test
    fun defend_gainsFive() {
        assertEquals(5, checkNotNull(field().defend(1)).tpOf(1))
        assertEquals(100, checkNotNull(field(allyTp = 98).defend(1)).tpOf(1))
    }

    /** 防御できなければTPは増えない(nullが返る)こと。 */
    @Test
    fun defend_notCurrentActor_returnsNull() {
        assertNull(field().defend(2))
    }

    // ---- 条件1: 攻撃技を当てた ----

    /** 通常攻撃が命中すると使用者が8〜12を受け取ること(両端)。 */
    @Test
    fun attackHit_gainsEightToTwelve() {
        val low = checkNotNull(field().performAttack(1, 2, TEST_NORMAL_ATTACK, TpRandom(listOf(noCrit, 0)))).first
        val high = checkNotNull(field().performAttack(1, 2, TEST_NORMAL_ATTACK, TpRandom(listOf(noCrit, 4)))).first

        assertEquals(8, low.tpOf(1))
        assertEquals(12, high.tpOf(1))
    }

    /** 命中した対象が防御中だけなら、6〜9になること(両端)。 */
    @Test
    fun attackHit_allDefending_gainsSixToNine() {
        val defending = field().withEnemy(2) { it.copy(isDefending = true) }

        val low = checkNotNull(defending.performAttack(1, 2, TEST_NORMAL_ATTACK, TpRandom(listOf(noCrit, 0)))).first
        val high = checkNotNull(defending.performAttack(1, 2, TEST_NORMAL_ATTACK, TpRandom(listOf(noCrit, 3)))).first

        assertEquals(6, low.tpOf(1))
        assertEquals(9, high.tpOf(1))
    }

    /** 範囲攻撃で、防御中でない対象が1人でも命中していれば8〜12になること。 */
    @Test
    fun area_oneNotDefending_usesNormalRange() {
        val skill = TEST_NORMAL_ATTACK.copy(area = SkillArea.AREA, targetSelection = SkillTargetSelection.ALL)
        val f = field().withEnemy(2) { it.copy(isDefending = true) }

        val after = checkNotNull(f.performAttack(1, 2, skill, TpRandom(listOf(noCrit, noCrit, 4)))).first

        assertEquals(12, after.tpOf(1))
    }

    /** クリティカルが出ると、引いた値に倍率(150%)をかけて切り捨てること(12/13/15/16/18)。 */
    @Test
    fun attackHit_critical_multipliesByRate() {
        val results = (0..4).map { roll ->
            checkNotNull(field().performAttack(1, 2, TEST_NORMAL_ATTACK, TpRandom(listOf(0, roll)))).first.tpOf(1)
        }

        assertEquals(listOf(12, 13, 15, 16, 18), results)
    }

    /** クリティカル倍率の段階が上がると、その実際の倍率(175%)でかかること。 */
    @Test
    fun attackHit_critical_usesActualRate() {
        val boosted = field().let {
            it.copy(allies = it.allies.map { a -> a.copy(statModifiers = listOf(StatModifier(BattleStat.CRITICAL_DAMAGE, 1, null))) })
        }

        val after = checkNotNull(boosted.performAttack(1, 2, TEST_NORMAL_ATTACK, TpRandom(listOf(0, 4)))).first

        assertEquals(21, after.tpOf(1)) // 12 × 175 / 100 = 21
    }

    /** バッドクリティカルでは倍率をかけず、TPを減らしもしないこと。 */
    @Test
    fun attackHit_badCritical_noMultiplier() {
        val bad = field().let {
            it.copy(allies = it.allies.map { a -> a.copy(statModifiers = listOf(StatModifier(BattleStat.CRITICAL_RATE, -1, null))) })
        }

        val (after, result) = checkNotNull(bad.performAttack(1, 2, TEST_NORMAL_ATTACK, TpRandom(listOf(0, 4))))

        assertEquals(CriticalType.BAD_CRITICAL, result.hits.single().shots.single().critical)
        assertEquals(12, after.tpOf(1))
    }

    /** 防御で防がれたクリティカルは倍率をかけないこと(防御中の6〜9のまま)。 */
    @Test
    fun attackHit_blockedCritical_noMultiplier() {
        val defending = field().withEnemy(2) { it.copy(isDefending = true) }

        val (after, result) = checkNotNull(defending.performAttack(1, 2, TEST_NORMAL_ATTACK, TpRandom(listOf(0, 3))))

        assertTrue(result.hits.single().shots.single().isCriticalBlocked)
        assertEquals(9, after.tpOf(1))
    }

    /** 多段攻撃でも、使用者が受け取るのは1回だけであること。 */
    @Test
    fun multiHit_userGainsOnce() {
        val skill = TEST_NORMAL_ATTACK.copy(activationCount = 3)
        val random = TpRandom(listOf(noCrit, noCrit, noCrit, 4))

        val after = checkNotNull(field().performAttack(1, 2, skill, random)).first

        assertEquals(12, after.tpOf(1))
        // クリティカル判定3回の後、使用者の獲得量の1回だけ(幅5)。回避はないので他に引かない。
        assertEquals(listOf(critical, critical, critical, 5), random.untils)
    }

    /** 範囲攻撃でも、使用者が受け取るのは1回だけで、乱数は使用者→対象の順に引くこと。 */
    @Test
    fun area_userGainsOnce_beforeTargets() {
        val skill = TEST_NORMAL_ATTACK.copy(area = SkillArea.AREA, targetSelection = SkillTargetSelection.ALL)
        val random = TpRandom(listOf(noCrit, noCrit, 3))

        val after = checkNotNull(field().performAttack(1, 2, skill, random)).first

        assertEquals(11, after.tpOf(1))
        assertEquals(listOf(critical, critical, 5), random.untils)
    }

    /** 全発が外れたら使用者は0で、避けた対象は18〜22を受け取ること(使用者の獲得量は引かない)。 */
    @Test
    fun allMissed_userGainsNothing_targetEvades() {
        val miss = TEST_NORMAL_ATTACK.copy(accuracy = 0)
        val random = TpRandom(listOf(2))

        val after = checkNotNull(field().performAttack(1, 2, miss, random)).first

        assertEquals(0, after.tpOf(1))
        assertEquals(20, after.tpOf(2))
        assertEquals(listOf(5), random.untils)
    }

    // ---- 条件6: 回避 ----

    /** 多段攻撃で複数回避けても、回避のTPは1回だけであること。 */
    @Test
    fun multiHit_evadedManyTimes_gainsOnce() {
        val skill = TEST_NORMAL_ATTACK.copy(accuracy = 0, activationCount = 3)
        val random = TpRandom(listOf(0))

        val after = checkNotNull(field().performAttack(1, 2, skill, random)).first

        assertEquals(18, after.tpOf(2))
        assertEquals(listOf(5), random.untils)
    }

    /** 同じ攻撃で命中も回避もあった対象は、条件2と条件6の両方を受け取ること。 */
    @Test
    fun hitAndEvade_inOneAttack_gainsBoth() {
        val skill = TEST_NORMAL_ATTACK.copy(activationCount = 2, accuracy = 50)
        // 1発目: 0.0 → 命中 / 2発目: 0.9 → 外れ。
        val random = TpRandom(listOf(noCrit, 1, 4), doubles = listOf(0.0, 0.9))

        val (after, result) = checkNotNull(field(enemyHp = 120).performAttack(1, 2, skill, random))

        val loss = result.hits.single().damage
        // 使用者は1回(8+1=9)。対象は被ダメージ割合の分(24/120×100=20)と回避の分(18+4=22)。
        assertEquals(9, after.tpOf(1))
        assertEquals(loss * 100 / 120 + 22, after.tpOf(2))
    }

    // ---- 条件2: 攻撃を受けた ----

    /**
     * 被ダメージ割合の式: 最大HP120で36(30%)を受けたら30、防御中なら45、クリティカル倍率150なら×1.5。
     * (仕様書の例「40を受けたら30」は40/120=33.3…となり式と合わないため、30%になる36で確かめる。)
     */
    @Test
    fun damageTaken_formula() {
        assertEquals(30.0, TacticalPointFormula.damageTaken(36, 120, isDefending = false, criticalRate = null), 1e-9)
        assertEquals(45.0, TacticalPointFormula.damageTaken(36, 120, isDefending = true, criticalRate = null), 1e-9)
        assertEquals(45.0, TacticalPointFormula.damageTaken(36, 120, isDefending = false, criticalRate = 150), 1e-9)
        assertEquals(60.0, TacticalPointFormula.damageTaken(48, 120, isDefending = false, criticalRate = 150), 1e-9)
    }

    /** 攻撃を受けた個体が、実際に減ったHPの割合の分を受け取ること(最大HP120でダメージ24なら20)。 */
    @Test
    fun targetGainsFromDamage() {
        val after = checkNotNull(field(enemyHp = 120).performAttack(1, 2, TEST_NORMAL_ATTACK, TpRandom(listOf(noCrit)))).first

        assertEquals(20, after.tpOf(2))
        assertEquals(0, after.tpOf(3))
    }

    /** 防御中に受けると×1.5(ダメージ16 → 13.33×1.5=20)になること。 */
    @Test
    fun targetGainsFromDamage_defending() {
        val defending = field(enemyHp = 120).withEnemy(2) { it.copy(isDefending = true) }

        val (after, result) = checkNotNull(defending.performAttack(1, 2, TEST_NORMAL_ATTACK, TpRandom(listOf(noCrit))))

        val loss = result.hits.single().damage
        assertEquals((loss * 100.0 / 120 * 1.5).toInt(), after.tpOf(2))
    }

    /** クリティカルを受けると×(倍率/100)になること。 */
    @Test
    fun targetGainsFromDamage_critical() {
        val (after, result) = checkNotNull(field(enemyHp = 120).performAttack(1, 2, TEST_NORMAL_ATTACK, TpRandom(listOf(0))))

        val loss = result.hits.single().damage
        assertEquals((loss * 100.0 / 120 * 1.5).toInt(), after.tpOf(2))
    }

    // ---- 条件4: サポート技 ----

    /** 精神一到は命中判定なしで必ず成功し、8〜12を受け取ること(倍率なし)。 */
    @Test
    fun seishin_gainsEightToTwelve() {
        val f = field(skills = listOf(TEST_SEISHIN), mp = 100)

        val after = checkNotNull(f.performSkill(1, TEST_SEISHIN.id, 1, TpRandom(listOf(4)))).first

        assertEquals(12, after.tpOf(1))
    }

    /** 覇気が命中すると使用者が8〜12を受け取ること。 */
    @Test
    fun haki_hit_gainsEightToTwelve() {
        val f = field(skills = listOf(TEST_HAKI), mp = 100)

        val after = checkNotNull(f.performSkill(1, TEST_HAKI.id, 2, TpRandom(listOf(0)))).first

        assertEquals(8, after.tpOf(1))
        assertEquals(0, after.tpOf(2))
    }

    /** 覇気が外れたら使用者は0で、避けた敵は18〜22を受け取ること。 */
    @Test
    fun haki_miss_userGainsNothing_targetEvades() {
        val f = field(skills = listOf(TEST_HAKI.copy(accuracy = 0)), mp = 100)
        val random = TpRandom(listOf(4))

        val after = checkNotNull(f.performSkill(1, TEST_HAKI.id, 2, random)).first

        assertEquals(0, after.tpOf(1))
        assertEquals(22, after.tpOf(2))
        assertEquals(listOf(5), random.untils)
    }

    // ---- ローリング・ソバット ----

    /** TPが40未満なら使えず(null)、フィールドは変わらないこと。 */
    @Test
    fun sobat_withLessThan40Tp_isNotUsable() {
        val f = field(allyTp = 39, skills = listOf(TEST_SOBAT))

        assertNull(f.performActiveSkill(1, TEST_SOBAT.id, 2, TpRandom()))
        assertTrue(!f.allies.single().canAfford(TEST_SOBAT))
        assertTrue(field(allyTp = 40, skills = listOf(TEST_SOBAT)).allies.single().canAfford(TEST_SOBAT))
    }

    /** 使うとTP40を消費して基礎威力150のダメージを与え、最後に条件1のTPを受け取ること(40-40+10)。 */
    @Test
    fun sobat_spendsTp_dealsDamage_andGainsTp() {
        val f = field(allyTp = 50, skills = listOf(TEST_SOBAT))

        val (after, use) = checkNotNull(f.performActiveSkill(1, TEST_SOBAT.id, 2, TpRandom(listOf(noCrit, 2))))

        val result = (use as SkillUseResult.Attack).result
        assertEquals(DamageFormula.calculate(150, 100, 100), result.totalDamage)
        assertEquals(TEST_SOBAT.id, result.skillId)
        assertEquals("ローリング・ソバット", result.skillName)
        assertEquals(50 - 40 + 10, after.tpOf(1))
        assertEquals(1000 - result.totalDamage, after.enemies[0].currentHp)
    }

    /** ソバットが外れても、TP40は戻らず、使用者は条件1のTPを受け取らないこと。 */
    @Test
    fun sobat_miss_stillSpendsTp() {
        val f = field(allyTp = 40, skills = listOf(TEST_SOBAT.copy(accuracy = 0)))

        val (after, use) = checkNotNull(f.performActiveSkill(1, TEST_SOBAT.id, 2, TpRandom(listOf(0))))

        assertTrue((use as SkillUseResult.Attack).result.hits.single().isMissed)
        assertEquals(0, after.tpOf(1))
    }

    /** ダメージのないスキルは従来どおり[SkillUseResult.Effect]で返ること。 */
    @Test
    fun performActiveSkill_support_returnsEffect() {
        val f = field(skills = listOf(TEST_SEISHIN), mp = 100)

        val use = checkNotNull(f.performActiveSkill(1, TEST_SEISHIN.id, 1, TpRandom())).second

        assertNotNull((use as SkillUseResult.Effect).result)
    }
}
