package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillArea
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillRange
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillTargetSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.random.Random

/** [performAttack]が攻撃スキルの基礎威力・攻撃回数を使い、ヒットごとにダメージ軽減を適用することを検証するテスト。 */
class PerformAttackTest {

    /** 味方1(手番・攻撃100)と敵2(耐久100)。攻撃100・耐久100の1ヒットは24ダメージ。 */
    private fun field(enemyHp: Int = 1000, enemyPassives: Boolean = false) = BattleField(
        allies = listOf(testAlly(1, passiveSkills = if (enemyPassives) listOf(TEST_STEEL_BODY) else emptyList())),
        enemies = listOf(testEnemy(2, hp = enemyHp)),
        turnQueue = listOf(1),
    )

    /** 通常攻撃は基礎威力100・1ヒットで、従来と同じ24ダメージになること。 */
    @Test
    fun normalAttack_usesBasePower100_singleHit() {
        val (after, result) = checkNotNull(field().performAttack(1, 2, TEST_NORMAL_ATTACK, Random(0)))

        assertEquals(24, result.totalDamage)
        assertEquals(1000 - 24, after.enemies[0].currentHp)
    }

    /** 基礎威力が変わればダメージも変わること(威力200: 200*100*11/5000+2=46)。 */
    @Test
    fun basePower_isApplied() {
        val skill = TEST_NORMAL_ATTACK.copy(basePower = 200)

        val (_, result) = checkNotNull(field().performAttack(1, 2, skill, Random(0)))

        assertEquals(46, result.totalDamage)
    }

    /** 攻撃回数だけ繰り返し、合計ダメージが返ること。 */
    @Test
    fun hitCount_sumsDamage() {
        val skill = TEST_NORMAL_ATTACK.copy(hitCount = 2)

        val (after, result) = checkNotNull(field().performAttack(1, 2, skill, Random(0)))

        assertEquals(48, result.totalDamage)
        assertEquals(1000 - 48, after.enemies[0].currentHp)
    }

    /** 対象が途中で戦闘不能になったら、残りのヒットは行わないこと。 */
    @Test
    fun hitCount_stopsWhenTargetDefeated() {
        val skill = TEST_NORMAL_ATTACK.copy(hitCount = 5)

        val (after, result) = checkNotNull(field(enemyHp = 30).performAttack(1, 2, skill, Random(0)))

        assertEquals(48, result.totalDamage) // 24 + 24 で30を超えたので3回目は行わない
        assertEquals(0, after.enemies[0].currentHp)
    }

    /** ダメージ軽減はヒットごとに適用されること(攻撃300なら68-20=48を2回)。 */
    @Test
    fun reduction_appliedPerHit() {
        val f = BattleField(
            listOf(testAlly(1, passiveSkills = listOf(TEST_STEEL_BODY))),
            listOf(testEnemy(2, attack = 300)),
            turnQueue = listOf(2),
        )

        val (_, result) = checkNotNull(f.performAttack(2, 1, TEST_NORMAL_ATTACK.copy(hitCount = 2), Random(0)))

        assertEquals(96, result.totalDamage)
    }

    /** 基礎威力のないスキルでは攻撃できないこと。 */
    @Test
    fun noBasePower_returnsNull() {
        assertNull(field().performAttack(1, 2, TEST_NORMAL_ATTACK.copy(basePower = null), Random(0)))
    }

    /** 敵3体(前衛2・後衛1)がいる、範囲攻撃の検証用フィールド。耐久100の1ヒットは24ダメージ。 */
    private fun multiField() = BattleField(
        allies = listOf(testAlly(1)),
        enemies = listOf(
            testEnemy(2, hp = 1000, slot = 0),
            testEnemy(3, hp = 1000, slot = 1),
            testEnemy(4, hp = 1000, slot = 3),
        ),
        turnQueue = listOf(1),
    )

    /** 陣(ALL)の攻撃が、アンカーと同じ段の敵全員に1ヒットずつ効き、他の敵には効かないこと。 */
    @Test
    fun formationAll_hitsEveryoneInTheRow() {
        val skill = TEST_NORMAL_ATTACK.copy(area = SkillArea.FORMATION, targetSelection = SkillTargetSelection.ALL)

        val (after, result) = checkNotNull(multiField().performAttack(1, 2, skill, Random(0)))

        assertEquals(listOf(2 to 24, 3 to 24), result.hits.map { it.targetFpid to it.damage })
        assertEquals(48, result.totalDamage)
        assertEquals(listOf(976, 976, 1000), after.enemies.map { it.currentHp })
    }

    /** 範囲攻撃でも、ダメージ軽減と打ち切りは対象ごとに行われること(戦闘不能になった対象だけ打ち切る)。 */
    @Test
    fun areaAll_reductionAndCutoffPerTarget() {
        val f = BattleField(
            allies = listOf(testAlly(1)),
            enemies = listOf(testEnemy(2, hp = 30), testEnemy(3, hp = 1000)),
            turnQueue = listOf(1),
        )
        val skill = TEST_NORMAL_ATTACK.copy(
            area = SkillArea.AREA, targetSelection = SkillTargetSelection.ALL, hitCount = 5,
        )

        val (after, result) = checkNotNull(f.performAttack(1, 2, skill, Random(0)))

        assertEquals(listOf(2 to 48, 3 to 120), result.hits.map { it.targetFpid to it.damage }) // 30のHPは2ヒット、1000のHPは5ヒット
        assertEquals(listOf(0, 880), after.enemies.map { it.currentHp })
    }

    /** 戦闘不能の敵は範囲攻撃の対象に含まれないこと。 */
    @Test
    fun areaAll_skipsDefeated() {
        val f = multiField().let { it.copy(enemies = it.enemies.map { e -> if (e.fpid == 3) e.copy(currentHp = 0) else e }) }
        val skill = TEST_NORMAL_ATTACK.copy(area = SkillArea.AREA, targetSelection = SkillTargetSelection.ALL, range = SkillRange.RANGE_3)

        val (_, result) = checkNotNull(f.performAttack(1, 2, skill, Random(0)))

        assertEquals(listOf(2, 4), result.hits.map { it.targetFpid })
    }

    /** 一体選択の攻撃は、範囲が広くてもアンカー1体だけに効くこと。 */
    @Test
    fun oneSelection_hitsOnlyAnchor() {
        val skill = TEST_NORMAL_ATTACK.copy(area = SkillArea.AREA, targetSelection = SkillTargetSelection.ONE)

        val (after, result) = checkNotNull(multiField().performAttack(1, 3, skill, Random(0)))

        assertEquals(listOf(3), result.hits.map { it.targetFpid })
        assertEquals(listOf(1000, 976, 1000), after.enemies.map { it.currentHp })
    }

    /** レンジ外の敵(後衛)を、レンジ1の通常攻撃で選ぶと攻撃できずnullになり、レンジ2なら届くこと。 */
    @Test
    fun outOfRange_returnsNull() {
        assertNull(multiField().performAttack(1, 4, TEST_NORMAL_ATTACK, Random(0)))
        assertNull(multiField().performAttack(1, 1, TEST_NORMAL_ATTACK, Random(0))) // 陣営違い(味方)

        val (_, result) = checkNotNull(multiField().performAttack(1, 4, TEST_NORMAL_ATTACK.copy(range = SkillRange.RANGE_2), Random(0)))
        assertEquals(listOf(4), result.hits.map { it.targetFpid })
    }

    /** 後衛の味方は、前衛が生きている間はレンジ1の通常攻撃が届かず、前衛が全滅すると届くこと。 */
    @Test
    fun backRowAttacker_reachesOnlyAfterFrontDefeated() {
        val back = BattleField(
            allies = listOf(testAlly(1, slot = 0).copy(currentHp = 100), testAlly(2, slot = 3)),
            enemies = listOf(testEnemy(3, slot = 0)),
            turnQueue = listOf(2),
        )
        assertNull(back.performAttack(2, 3, TEST_NORMAL_ATTACK, Random(0)))

        val promoted = back.copy(allies = back.allies.map { if (it.fpid == 1) it.copy(currentHp = 0) else it })
        assertEquals(listOf(3), checkNotNull(promoted.performAttack(2, 3, TEST_NORMAL_ATTACK, Random(0))).second.hits.map { it.targetFpid })
    }
}
