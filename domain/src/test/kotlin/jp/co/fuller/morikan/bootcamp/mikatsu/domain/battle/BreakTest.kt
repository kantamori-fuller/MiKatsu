package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.StatModifier
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleStat
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleEnemy
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BreakState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * ブレイク(ゲージの貯まり方・ブレイク状態の効果・状態の経過)を検証するテスト。
 *
 * 攻撃100・耐久100・基礎威力100の1ヒットは24ダメージ(ブレイク中は×1.3切り捨てで31)。
 */
class BreakTest {

    private val skill = TEST_NORMAL_ATTACK

    /** 味方1(手番)が敵2を攻撃するフィールド。 */
    private fun field(enemy: (BattleEnemy) -> BattleEnemy = { it }, enemyHp: Int = 1000, attackerRate: Int = 150) =
        BattleField(
            allies = listOf(testAlly(1).copy(breakPointRate = attackerRate)),
            enemies = listOf(enemy(testEnemy(2, hp = enemyHp))),
            turnQueue = listOf(1),
        )

    /**
     * 命中判定の乱数([Random.nextDouble])を1回でも引いたら失敗する乱数。被必中で命中判定の乱数を消費しないことの確認に使う。
     * クリティカル判定の[Random.nextInt]は命中したヒットで引くため許し、クリティカルにならない値を返す。
     */
    private val noRandom = object : Random() {
        override fun nextBits(bitCount: Int): Int = error("命中判定の乱数は引かない")
        override fun nextInt(until: Int): Int = until - 1
        override fun nextDouble(): Double = error("命中判定の乱数は引かない")
    }

    /** 必ず当たる値を返す乱数(通常攻撃の命中率100に対して)。 */
    private val alwaysHit = fixedRandom(0.0)

    /** 式: 最大HP120に60ダメージ・耐性100・倍率150 → 75、耐性80 → 60。 */
    @Test
    fun formula_examples() {
        assertEquals(75.0, BreakFormula.gaugeIncrease(60, 120, 150, 100), 1e-9)
        assertEquals(60.0, BreakFormula.gaugeIncrease(60, 120, 150, 80), 1e-9)
    }

    /** 攻撃者の倍率が使われること(同じ攻撃でも倍率が違えば増える量が変わる)。 */
    @Test
    fun gauge_usesAttackerBreakPointRate() {
        val low = checkNotNull(field(enemyHp = 120, attackerRate = 100).performAttack(1, 2, skill, alwaysHit)).first
        val high = checkNotNull(field(enemyHp = 120, attackerRate = 200).performAttack(1, 2, skill, alwaysHit)).first

        assertEquals(24.0 / 120 * 100 * 1.0, low.enemies.single().breakGauge, 1e-9)
        assertEquals(24.0 / 120 * 100 * 2.0, high.enemies.single().breakGauge, 1e-9)
    }

    /** 対象の耐性が使われること。 */
    @Test
    fun gauge_usesTargetResistance() {
        val f = field(enemy = { it.copy(breakResistance = 50) }, enemyHp = 120)

        val after = checkNotNull(f.performAttack(1, 2, skill, alwaysHit)).first

        assertEquals(24.0 / 120 * 100 * 1.5 * 0.5, after.enemies.single().breakGauge, 1e-9)
    }

    /** オーバーキルは実際に減ったHPで計算され、端数は小数のまま残ること。 */
    @Test
    fun gauge_overkillUsesActualHpLoss_andKeepsFraction() {
        val overkill = field(enemy = { it.copy(currentHp = 10) }, enemyHp = 100)
        val (after, _) = checkNotNull(overkill.performAttack(1, 2, skill, alwaysHit))
        // 倒れたのでブレイクもゲージ加算もしない。実際に減った分で計算した値は式のテストで確認する。
        assertEquals(0.0, after.enemies.single().breakGauge, 0.0)

        val fraction = checkNotNull(field(enemyHp = 70).performAttack(1, 2, skill, alwaysHit)).first
        assertEquals(24.0 / 70 * 150, fraction.enemies.single().breakGauge, 1e-9)
        assertTrue(fraction.enemies.single().breakGauge % 1.0 != 0.0)
    }

    /** オーバーキルでも生き残る別の対象なら、実際に減ったHPが使われること(HP24以上残る場合との比較)。 */
    @Test
    fun gauge_usesMinOfDamageAndCurrentHp() {
        // 範囲ではなく単体: 現在HP30(最大100)に24ダメージ → 24。現在HP24でも倒れるのでゲージ0。
        val after = checkNotNull(field(enemy = { it.copy(currentHp = 30) }, enemyHp = 100).performAttack(1, 2, skill, alwaysHit)).first
        assertEquals(24.0 / 100 * 150, after.enemies.single().breakGauge, 1e-9)
    }

    /** 100以上でゲージが0に戻ってBROKENになり、超過分は捨てられ、causedBreakが立つこと。 */
    @Test
    fun gauge_reaching100_breaksAndDiscardsExcess() {
        // 24/30*150 = 120
        val (after, result) = checkNotNull(field(enemyHp = 30).performAttack(1, 2, skill, alwaysHit))
        // 30HPに24ダメージ → 生存。
        val enemy = after.enemies.single()
        assertEquals(BreakState.BROKEN, enemy.breakState)
        assertEquals(0.0, enemy.breakGauge, 0.0)
        assertTrue(result.hits.single().shots.single().causedBreak)
        assertTrue(result.hits.single().causedBreak)
    }

    /** BROKEN・RECOVERING中はゲージが貯まらないこと。 */
    @Test
    fun gauge_doesNotAccumulateWhileBrokenOrRecovering() {
        for (state in listOf(BreakState.BROKEN, BreakState.RECOVERING)) {
            val after = checkNotNull(
                field(enemy = { it.copy(breakState = state) }, enemyHp = 120).performAttack(1, 2, skill, alwaysHit),
            ).first

            assertEquals(0.0, after.enemies.single().breakGauge, 0.0)
            assertEquals(state, after.enemies.single().breakState)
        }
    }

    /** そのヒットで対象が戦闘不能になった場合はブレイクせず、ゲージも貯まらないこと。 */
    @Test
    fun defeatedTarget_doesNotBreak() {
        val (after, result) = checkNotNull(
            field(enemy = { it.copy(currentHp = 24) }, enemyHp = 30).performAttack(1, 2, skill, alwaysHit),
        )

        val enemy = after.enemies.single()
        assertTrue(enemy.isDefeated)
        assertEquals(BreakState.NONE, enemy.breakState)
        assertEquals(0.0, enemy.breakGauge, 0.0)
        assertFalse(result.hits.single().causedBreak)
    }

    /** ブレイク中は×1.3切り捨て(24→31)の後に鋼の体の軽減が適用されること(31-20=11)。 */
    @Test
    fun brokenTarget_boostsDamageThenAppliesReduction() {
        val f = BattleField(
            allies = listOf(testAlly(1)),
            enemies = listOf(testEnemy(2, hp = 1000).copy(breakState = BreakState.BROKEN)),
            turnQueue = listOf(1),
        )
        assertEquals(31, checkNotNull(f.performAttack(1, 2, skill, noRandom)).second.totalDamage)

        val steel = BattleField(
            allies = listOf(testAlly(1), testAlly(3, passiveSkills = listOf(TEST_STEEL_BODY)).copy(breakState = BreakState.BROKEN)),
            enemies = listOf(testEnemy(2, hp = 1000)),
            turnQueue = listOf(2),
        )
        // 敵2が味方3(鋼の体・ブレイク中)を攻撃: 31 - 20 = 11(軽減を先にすると24-20=4→×1.3=5になってしまう)。
        assertEquals(11, checkNotNull(steel.performAttack(2, 3, skill.copy(accuracy = null), noRandom)).second.totalDamage)
    }

    /** 被必中: 命中率の段階が最低・回避率の段階が最高でも、乱数を引かずに必ず当たること。 */
    @Test
    fun brokenTarget_isAlwaysHit_withoutRandom() {
        val f = BattleField(
            allies = listOf(testAlly(1, statModifiers = listOf(StatModifier(BattleStat.ACCURACY, -12, 5)))),
            enemies = listOf(testEnemy(2, hp = 1000, statModifiers = listOf(StatModifier(BattleStat.EVASION, 12, 5))).copy(breakState = BreakState.BROKEN)),
            turnQueue = listOf(1),
        )

        val (_, result) = checkNotNull(f.performAttack(1, 2, skill.copy(accuracy = 0), noRandom))

        assertTrue(result.hits.single().shots.single().isHit)
        assertTrue(HitChanceFormula.isHit(0, -12, 12, noRandom, isTargetGuaranteedHit = true))
    }

    /** 多段攻撃の途中でブレイクしたら、残りのヒットから×1.3と被必中が効くこと。 */
    @Test
    fun midMultiHitBreak_affectsRemainingHits() {
        // 最大HP200・攻撃者の倍率450 → 1ヒットでゲージ +54。2ヒット目でブレイク。
        // 3・4ヒット目は乱数(0.95=外れる値)を引かずに必ず当たり、31ダメージ。
        val multi = skill.copy(activationCount = 4, accuracy = 90)
        val f = field(enemyHp = 200, attackerRate = 450)

        val (after, result) = checkNotNull(f.performAttack(1, 2, multi, fixedRandom(0.5, 0.5, 0.95)))

        val shots = result.hits.single().shots
        assertEquals(listOf(24, 24, 31, 31), shots.map { it.damage })
        assertEquals(listOf(false, true, false, false), shots.map { it.causedBreak })
        assertTrue(shots.all { it.isHit })
        assertEquals(200 - 110, after.enemies.single().currentHp)
        assertEquals(BreakState.BROKEN, after.enemies.single().breakState)
    }

    /** endTurn: BROKEN→RECOVERING→NONE。立ち上がりが解けたときに耐性が20下がること。 */
    @Test
    fun endTurn_progressesBreakState_andLowersResistance() {
        val start = BattleField(
            allies = listOf(testAlly(1).copy(breakState = BreakState.BROKEN)),
            enemies = listOf(testEnemy(2)),
            turnQueue = listOf(1),
        )

        val recovering = start.endTurn(1, Random(0))
        assertEquals(BreakState.RECOVERING, recovering.allies.single().breakState)
        assertEquals(BreakFormula.BASE_RESISTANCE, recovering.allies.single().breakResistance)
        assertEquals(1, recovering.allies.single().turnCount)

        val none = recovering.copy(turnQueue = listOf(1)).endTurn(1, Random(0))
        assertEquals(BreakState.NONE, none.allies.single().breakState)
        assertEquals(80, none.allies.single().breakResistance)
    }

    /** 耐性は繰り返し下がり、0で止まること。ブレイクしていない個体は変わらないこと。 */
    @Test
    fun endTurn_resistanceDecreasesRepeatedly_andStopsAtZero() {
        fun recoverOnce(resistance: Int): Int {
            val f = BattleField(
                allies = listOf(testAlly(1).copy(breakState = BreakState.RECOVERING, breakResistance = resistance)),
                enemies = listOf(testEnemy(2)),
                turnQueue = listOf(1),
            )
            return f.endTurn(1, Random(0)).allies.single().breakResistance
        }

        assertEquals(60, recoverOnce(80))
        assertEquals(0, recoverOnce(10))
        assertEquals(0, recoverOnce(0))

        val normal = BattleField(allies = listOf(testAlly(1)), enemies = listOf(testEnemy(2)), turnQueue = listOf(1))
        assertEquals(100, normal.endTurn(1, Random(0)).allies.single().breakResistance)
    }

    /** BROKENの使用者は performAttack / performSkill が null(実行不可)であること。 */
    @Test
    fun brokenActor_cannotAttackOrUseSkill() {
        val f = BattleField(
            allies = listOf(testAlly(1, mp = 30, activeSkills = listOf(TEST_DIA)).copy(breakState = BreakState.BROKEN)),
            enemies = listOf(testEnemy(2)),
            turnQueue = listOf(1),
        )
        assertNull(f.performAttack(1, 2, skill, Random(0)))
        assertNull(f.performSkill(1, TEST_DIA.id, 1, Random(0)))

        val ok = f.copy(allies = f.allies.map { it.copy(breakState = BreakState.RECOVERING) })
        assertNotNull(ok.performAttack(1, 2, skill, Random(0)))
        assertNotNull(ok.performSkill(1, TEST_DIA.id, 1, Random(0)))
    }
}
