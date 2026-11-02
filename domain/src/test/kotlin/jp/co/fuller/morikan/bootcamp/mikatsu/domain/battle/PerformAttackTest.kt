package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

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
        val (after, result) = checkNotNull(field().performAttack(1, 2, TEST_NORMAL_ATTACK))

        assertEquals(24, result.damage)
        assertEquals(1000 - 24, after.enemies[0].currentHp)
    }

    /** 基礎威力が変わればダメージも変わること(威力200: 200*100*11/5000+2=46)。 */
    @Test
    fun basePower_isApplied() {
        val skill = TEST_NORMAL_ATTACK.copy(basePower = 200)

        val (_, result) = checkNotNull(field().performAttack(1, 2, skill))

        assertEquals(46, result.damage)
    }

    /** 攻撃回数だけ繰り返し、合計ダメージが返ること。 */
    @Test
    fun hitCount_sumsDamage() {
        val skill = TEST_NORMAL_ATTACK.copy(hitCount = 2)

        val (after, result) = checkNotNull(field().performAttack(1, 2, skill))

        assertEquals(48, result.damage)
        assertEquals(1000 - 48, after.enemies[0].currentHp)
    }

    /** 対象が途中で戦闘不能になったら、残りのヒットは行わないこと。 */
    @Test
    fun hitCount_stopsWhenTargetDefeated() {
        val skill = TEST_NORMAL_ATTACK.copy(hitCount = 5)

        val (after, result) = checkNotNull(field(enemyHp = 30).performAttack(1, 2, skill))

        assertEquals(48, result.damage) // 24 + 24 で30を超えたので3回目は行わない
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

        val (_, result) = checkNotNull(f.performAttack(2, 1, TEST_NORMAL_ATTACK.copy(hitCount = 2)))

        assertEquals(96, result.damage)
    }

    /** 基礎威力のないスキルでは攻撃できないこと。 */
    @Test
    fun noBasePower_returnsNull() {
        assertNull(field().performAttack(1, 2, TEST_NORMAL_ATTACK.copy(basePower = null)))
    }
}
