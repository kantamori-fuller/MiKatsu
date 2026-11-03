package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.StatModifier
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleStat
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AttackShot
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillArea
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillTargetSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** [performAttack]がヒットごと・対象ごとに命中判定を行うことを検証するテスト。 */
class PerformAttackHitTest {

    /** 攻撃100・耐久100の1ヒットは24ダメージ。命中率90のスキルで、乱数0.5は当たり、0.95は外れ。 */
    private val skill = TEST_NORMAL_ATTACK.copy(accuracy = 90)

    private fun field(enemyHp: Int = 1000) = BattleField(
        allies = listOf(testAlly(1)),
        enemies = listOf(testEnemy(2, hp = enemyHp)),
        turnQueue = listOf(1),
    )

    /** 外れたヒットはダメージ0でHPが減らず、isMissedになること。 */
    @Test
    fun miss_dealsNoDamage() {
        val (after, result) = checkNotNull(field().performAttack(1, 2, skill, fixedRandom(0.95)))

        val hit = result.hits.single()
        assertEquals(listOf(AttackShot(isHit = false, damage = 0)), hit.shots)
        assertTrue(hit.isMissed)
        assertEquals(0, hit.damage)
        assertEquals(1000, after.enemies.single().currentHp)
    }

    /** 外れたヒットにはパッシブスキルの軽減が適用されず(ダメージ0のまま)、当たったヒットだけ軽減されること。 */
    @Test
    fun miss_doesNotApplyReduction() {
        val allyTarget = BattleField(
            allies = listOf(testAlly(1), testAlly(2, passiveSkills = listOf(TEST_STEEL_BODY))),
            enemies = listOf(testEnemy(3)),
            turnQueue = listOf(3),
        )
        // 敵3が味方2(鋼の体)へ2回攻撃: 1回目は外れ(0)、2回目は当たり(24-20=4)。外れたヒットは軽減後の最低1も適用されず0のまま。
        val enemySkill = skill.copy(activationCount = 2)
        val (_, result) = checkNotNull(allyTarget.performAttack(3, 2, enemySkill, fixedRandom(0.95, 0.5)))

        assertEquals(listOf(0, 4), result.hits.single().shots.map { it.damage })
        assertEquals(24 - 20, result.hits.single().damage)
    }

    /** 外れも発動回数を1回消費し、ヒットごとに当たり外れが分かれること。 */
    @Test
    fun miss_consumesHitCount_andShotsAreIndependent() {
        val twoHits = skill.copy(activationCount = 2)

        val (after, result) = checkNotNull(field().performAttack(1, 2, twoHits, fixedRandom(0.95, 0.5)))

        assertEquals(
            listOf(AttackShot(false, 0), AttackShot(true, 24)),
            result.hits.single().shots,
        )
        assertFalse(result.hits.single().isMissed)
        assertEquals(976, after.enemies.single().currentHp)
    }

    /** 範囲攻撃では対象ごとに判定され、当たり外れが分かれること。 */
    @Test
    fun area_isJudgedPerTarget() {
        val f = BattleField(
            allies = listOf(testAlly(1)),
            enemies = listOf(testEnemy(2, hp = 1000, slot = 0), testEnemy(3, hp = 1000, slot = 1)),
            turnQueue = listOf(1),
        )
        val areaSkill = skill.copy(area = SkillArea.AREA, targetSelection = SkillTargetSelection.ALL)

        val (after, result) = checkNotNull(f.performAttack(1, 2, areaSkill, fixedRandom(0.95, 0.5)))

        assertEquals(listOf(true, false), result.hits.map { it.isMissed })
        assertEquals(listOf(1000, 976), after.enemies.map { it.currentHp })
    }

    /** 命中率0のスキルは必ず外れ、100のスキルは必ず当たること(乱数の両端でも)。 */
    @Test
    fun zeroAlwaysMisses_hundredAlwaysHits() {
        for (roll in listOf(0.0, Math.nextDown(1.0))) {
            assertTrue(checkNotNull(field().performAttack(1, 2, skill.copy(accuracy = 0), fixedRandom(roll))).second.hits.single().isMissed)
            assertFalse(checkNotNull(field().performAttack(1, 2, skill.copy(accuracy = 100), fixedRandom(roll))).second.hits.single().isMissed)
        }
    }

    /** 対象の回避率の段階が最終命中率に反映されること(命中90・回避+1なら90×6/7≒77.14%)。 */
    @Test
    fun accuracyAndEvasion_affectChance() {
        val f = BattleField(
            allies = listOf(testAlly(1)),
            enemies = listOf(testEnemy(2, hp = 1000, statModifiers = listOf(StatModifier(BattleStat.EVASION, 1, 5)))),
            turnQueue = listOf(1),
        )

        assertFalse(checkNotNull(f.performAttack(1, 2, skill, fixedRandom(0.77))).second.hits.single().isMissed)
        assertTrue(checkNotNull(f.performAttack(1, 2, skill, fixedRandom(0.78))).second.hits.single().isMissed)
    }

    /** 命中率がnull(必中)のスキルは乱数にかかわらず当たること。 */
    @Test
    fun nullAccuracy_alwaysHits() {
        val result = checkNotNull(field().performAttack(1, 2, skill.copy(accuracy = null), fixedRandom(0.999))).second

        assertFalse(result.hits.single().isMissed)
    }

    /** 外れたヒットも含め、対象が戦闘不能になったら残りのヒットは打ち切られること。 */
    @Test
    fun cutoff_afterDefeat_evenWithMissesBefore() {
        val f = field(enemyHp = 20)

        val (after, result) = checkNotNull(f.performAttack(1, 2, skill.copy(activationCount = 5), fixedRandom(0.95, 0.5, 0.5)))

        assertEquals(listOf(false, true), result.hits.single().shots.map { it.isHit })
        assertEquals(0, after.enemies.single().currentHp)
    }
}
