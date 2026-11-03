package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleStat
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BreakState
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.CriticalType
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.StatModifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/** [performAttack]のクリティカル判定(判定のタイミング・ダメージ・適用順序)を検証するテスト。攻撃100・耐久100の1ヒットは24ダメージ。 */
class PerformAttackCriticalTest {

    /** 攻撃者のバフ・デバフを指定した、味方1(手番)対敵2の戦場。ブレイクを起こさないよう攻撃者のブレイクポイント倍率は0。 */
    private fun field(attackerMods: List<StatModifier> = emptyList()): BattleField = BattleField(
        allies = listOf(testAlly(1, statModifiers = attackerMods).copy(breakPointRate = 0)),
        enemies = listOf(testEnemy(2, hp = 10000)),
        turnQueue = listOf(1),
    )

    /** 発生率の乱数が0ならクリティカルになり、24×150/100=36ダメージになること。 */
    @Test
    fun critical_multipliesBy150Percent() {
        val (_, result) = checkNotNull(field().performAttack(1, 2, TEST_NORMAL_ATTACK, CriticalRandom(0)))

        val shot = result.hits.single().shots.single()
        assertEquals(CriticalType.CRITICAL, shot.critical)
        assertEquals(36, shot.damage)
    }

    /** 乱数が当たらなければ通常のダメージ(24)で、種別はNONE。 */
    @Test
    fun noCritical_normalDamage() {
        val (_, result) = checkNotNull(field().performAttack(1, 2, TEST_NORMAL_ATTACK, CriticalRandom(5)))

        assertEquals(CriticalType.NONE, result.hits.single().shots.single().critical)
        assertEquals(24, result.totalDamage)
    }

    /** 発生率の段階が負でバッドの判定が出ても、最終倍率150-75=75%は100未満なのでBAD_CRITICAL: 24×75/100=18。 */
    @Test
    fun badCritical_reducesDamage() {
        val f = field(listOf(StatModifier(BattleStat.CRITICAL_RATE, -1, null)))

        val (_, result) = checkNotNull(f.performAttack(1, 2, TEST_NORMAL_ATTACK, CriticalRandom(0)))

        val shot = result.hits.single().shots.single()
        assertEquals(CriticalType.BAD_CRITICAL, shot.critical)
        assertEquals(18, shot.damage)
    }

    /** 倍率の段階+2なら200%: 24×200/100=48。 */
    @Test
    fun damageStage_changesMultiplier() {
        val f = field(listOf(StatModifier(BattleStat.CRITICAL_DAMAGE, 2, null)))

        val (_, result) = checkNotNull(f.performAttack(1, 2, TEST_NORMAL_ATTACK, CriticalRandom(0)))

        assertEquals(48, result.totalDamage)
    }

    /** 発生率の段階は±6で頭打ち: +8を重ねても+6(24/24)で、どの乱数でもクリティカルになること。 */
    @Test
    fun rateStage_cappedAtSix() {
        val f = field(listOf(StatModifier(BattleStat.CRITICAL_RATE, 8, null)))

        val (_, result) = checkNotNull(f.performAttack(1, 2, TEST_NORMAL_ATTACK, CriticalRandom(23)))

        assertEquals(CriticalType.CRITICAL, result.hits.single().shots.single().critical)
    }

    /** 倍率の下限: 段階が低すぎて倍率0%でも、ダメージは最低1。 */
    @Test
    fun criticalDamage_minimumOne() {
        val f = field(listOf(StatModifier(BattleStat.CRITICAL_DAMAGE, -12, null)))

        val (_, result) = checkNotNull(f.performAttack(1, 2, TEST_NORMAL_ATTACK, CriticalRandom(0)))

        assertEquals(1, result.totalDamage)
    }

    /** 外れたヒットではクリティカルを判定せず、乱数も引かないこと(種別はNONE)。 */
    @Test
    fun missedHit_doesNotJudgeOrDrawRandom() {
        val random = CriticalRandom(0, 0.99)

        val (_, result) = checkNotNull(field().performAttack(1, 2, TEST_NORMAL_ATTACK.copy(accuracy = 50), random))

        assertEquals(CriticalType.NONE, result.hits.single().shots.single().critical)
        assertEquals(0, random.criticalDraws)
    }

    /** 命中したヒットごとに1回ずつ判定し(命中率null・多段でも)、乱数はヒット数だけ引くこと。 */
    @Test
    fun judgedOncePerLandedHit() {
        val random = CriticalRandom(0)

        val (_, result) = checkNotNull(field().performAttack(1, 2, TEST_NORMAL_ATTACK.copy(accuracy = null, activationCount = 3), random))

        assertEquals(3, random.criticalDraws)
        assertEquals(108, result.totalDamage)
    }

    /** 範囲攻撃では対象ごとに判定すること。 */
    @Test
    fun area_judgedPerTarget() {
        val f = BattleField(
            allies = listOf(testAlly(1).copy(breakPointRate = 0)),
            enemies = listOf(testEnemy(2, hp = 1000), testEnemy(3, hp = 1000, slot = 1)),
            turnQueue = listOf(1),
        )
        val random = CriticalRandom(0)
        val skill = TEST_NORMAL_ATTACK.copy(
            range = jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillRange.RANGE_3,
            area = jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillArea.AREA,
            targetSelection = jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillTargetSelection.ALL,
        )

        val (_, result) = checkNotNull(f.performAttack(1, 2, skill, random))

        assertEquals(2, random.criticalDraws)
        assertEquals(listOf(36, 36), result.hits.map { it.damage })
    }

    /** 敵の攻撃(同じperformAttack)も味方と同じく判定すること。 */
    @Test
    fun enemyAttack_isJudgedToo() {
        val f = BattleField(
            allies = listOf(testAlly(1, hp = 1000)),
            enemies = listOf(testEnemy(2)),
            turnQueue = listOf(2),
        )

        val (_, result) = checkNotNull(f.performAttack(2, 1, TEST_NORMAL_ATTACK, CriticalRandom(0)))

        assertEquals(36, result.totalDamage)
    }

    /**
     * 適用順序: クリティカル → ブレイク中×1.3 → Pスキル軽減。
     * 攻撃200なら基礎46 → クリティカル69 → ブレイク89 → 鋼の体-20で69(ブレイクを先にすると88になり数値が変わる)。
     */
    @Test
    fun order_criticalThenBreakBoostThenReduction() {
        val f = BattleField(
            allies = listOf(testAlly(1, hp = 1000, passiveSkills = listOf(TEST_STEEL_BODY)).copy(breakState = BreakState.BROKEN)),
            enemies = listOf(testEnemy(2, attack = 200)),
            turnQueue = listOf(2),
        )

        val (_, result) = checkNotNull(f.performAttack(2, 1, TEST_NORMAL_ATTACK, CriticalRandom(0)))

        assertEquals(69, result.totalDamage)
    }

    /** ブレイクゲージは実際に減ったHP(クリティカル込み)で貯まること: 最大HP720に36ダメージ×倍率150% → 7.5。 */
    @Test
    fun breakGauge_usesCriticalDamage() {
        val f = BattleField(
            allies = listOf(testAlly(1)),
            enemies = listOf(testEnemy(2, hp = 720)),
            turnQueue = listOf(1),
        )

        val (after, _) = checkNotNull(f.performAttack(1, 2, TEST_NORMAL_ATTACK, CriticalRandom(0)))

        assertEquals(36.0 / 720 * 100 * 1.5, after.enemies[0].breakGauge, 1e-9)
    }

    /** クリティカルの2つの段階は基礎値を持たないので、effectiveStatに渡すと例外になること。 */
    @Test
    fun effectiveStat_throwsForCriticalStats() {
        val ally = testAlly(1)
        assertThrows(IllegalStateException::class.java) { ally.effectiveStat(BattleStat.CRITICAL_RATE) }
        assertThrows(IllegalStateException::class.java) { ally.effectiveStat(BattleStat.CRITICAL_DAMAGE) }
    }

    /** 全個体のクリティカル倍率の基本値は150の固定値であること。 */
    @Test
    fun baseDamageRate_is150ForEveryone() {
        assertEquals(150, testAlly(1).criticalDamageRate)
        assertEquals(150, testEnemy(2).criticalDamageRate)
    }

    /** 倍率段階-3でクリティカルの判定 → 75% → BAD_CRITICAL(ダメージ24×75/100=18)。 */
    @Test
    fun classify_criticalJudgedButRateBelow100_isBad() {
        val f = field(listOf(StatModifier(BattleStat.CRITICAL_DAMAGE, -3, null)))

        val shot = checkNotNull(f.performAttack(1, 2, TEST_NORMAL_ATTACK, CriticalRandom(0))).second.hits.single().shots.single()

        assertEquals(CriticalType.BAD_CRITICAL, shot.critical)
        assertEquals(18, shot.damage)
    }

    /** 倍率段階+4でバッドの判定 → 250-75=175% → CRITICAL(ダメージ24×175/100=42)。 */
    @Test
    fun classify_badJudgedButRateAtLeast100_isCritical() {
        val f = field(listOf(StatModifier(BattleStat.CRITICAL_RATE, -1, null), StatModifier(BattleStat.CRITICAL_DAMAGE, 4, null)))

        val shot = checkNotNull(f.performAttack(1, 2, TEST_NORMAL_ATTACK, CriticalRandom(0))).second.hits.single().shots.single()

        assertEquals(CriticalType.CRITICAL, shot.critical)
        assertEquals(42, shot.damage)
    }

    /** 倍率段階-2でクリティカル → ちょうど100% → CRITICAL(ダメージ24)。 */
    @Test
    fun classify_exactly100_isCritical() {
        val f = field(listOf(StatModifier(BattleStat.CRITICAL_DAMAGE, -2, null)))

        val shot = checkNotNull(f.performAttack(1, 2, TEST_NORMAL_ATTACK, CriticalRandom(0))).second.hits.single().shots.single()

        assertEquals(CriticalType.CRITICAL, shot.critical)
        assertEquals(24, shot.damage)
    }

    /** 倍率0%(段階-6でクリティカル)はBAD_CRITICALで、ダメージは最低1。判定が出ない場合はNONEのまま。 */
    @Test
    fun classify_zeroRate_isBad_andNotJudgedStaysNone() {
        val f = field(listOf(StatModifier(BattleStat.CRITICAL_DAMAGE, -6, null)))

        val bad = checkNotNull(f.performAttack(1, 2, TEST_NORMAL_ATTACK, CriticalRandom(0))).second.hits.single().shots.single()
        val none = checkNotNull(f.performAttack(1, 2, TEST_NORMAL_ATTACK, CriticalRandom(5))).second.hits.single().shots.single()

        assertEquals(CriticalType.BAD_CRITICAL, bad.critical)
        assertEquals(1, bad.damage)
        assertEquals(CriticalType.NONE, none.critical)
        assertEquals(24, none.damage)
    }
}
