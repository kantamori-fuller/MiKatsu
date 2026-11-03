package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillTargetSide
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random

/** パッシブスキル(鋼の体)によるダメージ軽減が、通常攻撃の最終ダメージに正しく適用されることを検証するテスト。 */
class DamageReductionTest {

    /** 攻撃100・耐久100でのダメージ(式の結果は24)。 */
    private val baseDamage = 24

    /** パッシブスキルがなければダメージは変わらない。 */
    @Test
    fun noPassive_damageUnchanged() {
        val field = BattleField(listOf(testAlly(1)), listOf(testEnemy(2)), turnQueue = listOf(2))

        val (_, result) = checkNotNull(field.performAttack(2, 1, TEST_NORMAL_ATTACK, Random(0)))

        assertEquals(baseDamage, result.totalDamage)
    }

    /** 敵が鋼の体を持つ味方を攻撃すると、ダメージが20減ること。 */
    @Test
    fun enemyAttackingSteelBodyAlly_reducedBy20() {
        val field = BattleField(
            listOf(testAlly(1, passiveSkills = listOf(TEST_STEEL_BODY))),
            listOf(testEnemy(2, attack = 300)),
            turnQueue = listOf(2),
        )
        // 攻撃300・耐久100: 300*100*11/(100*50)+2 = 68
        val (after, result) = checkNotNull(field.performAttack(2, 1, TEST_NORMAL_ATTACK, Random(0)))

        assertEquals(48, result.totalDamage)
        assertEquals(100 - 48, after.allies[0].currentHp)
    }

    /** 味方同士の攻撃(対象が鋼の体を持つ)にも適用されること。 */
    @Test
    fun allyAttackingSteelBodyAlly_reduced() {
        val field = BattleField(
            listOf(testAlly(1, attack = 300), testAlly(2, passiveSkills = listOf(TEST_STEEL_BODY))),
            listOf(testEnemy(3)),
            turnQueue = listOf(1),
        )

        val (_, result) = checkNotNull(field.performAttack(1, 2, TEST_NORMAL_ATTACK.copy(targetSide = SkillTargetSide.ALLY), Random(0)))

        assertEquals(48, result.totalDamage)
    }

    /** 軽減量がダメージ(攻撃10なら4)を上回っても、軽減後のダメージは最低1になること。 */
    @Test
    fun reductionExceedsDamage_minimumOne() {
        val field = BattleField(
            listOf(testAlly(1, passiveSkills = listOf(TEST_STEEL_BODY))),
            listOf(testEnemy(2, attack = 10)),
            turnQueue = listOf(2),
        )

        val (_, result) = checkNotNull(field.performAttack(2, 1, TEST_NORMAL_ATTACK, Random(0)))

        assertEquals(1, result.totalDamage)
    }

    /** 鋼の体を持たない敵が受けるダメージは軽減されないこと。 */
    @Test
    fun enemyWithoutPassive_notReduced() {
        val field = BattleField(
            listOf(testAlly(1, attack = 300)),
            listOf(testEnemy(2)),
            turnQueue = listOf(1),
        )

        val (_, result) = checkNotNull(field.performAttack(1, 2, TEST_NORMAL_ATTACK, Random(0)))

        assertEquals(68, result.totalDamage)
    }

    /** 軽減が複数ある場合は合計して適用されること。 */
    @Test
    fun multipleReductions_areSummed() {
        val ally = testAlly(1, passiveSkills = listOf(TEST_STEEL_BODY, TEST_STEEL_BODY.copy(id = 3)))

        assertEquals(40, ally.totalDamageReduction())
        assertEquals(28, ally.reduceDamage(68))
    }
}
