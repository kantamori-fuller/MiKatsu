package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleStat
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.StatModifier
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkill
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillCategory
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillTargetSide
import org.junit.Assert.assertEquals
import org.junit.Test

/** [performAttack]が、特殊型攻撃で破壊力・耐久力の代わりにマナ出力C・マナ耐性Dを使うことを検証するテスト。 */
class SpecialAttackTest {

    /** 基礎威力100・命中率`null`(必中)・敵対象の攻撃。スキルタイプだけを変えて使う。 */
    private fun skill(vararg categories: SkillCategory) = testActiveSkill(
        id = 50,
        name = "テスト攻撃",
        targetSide = SkillTargetSide.ENEMY,
        basePower = 100,
        categories = categories.toSet(),
    )

    /** 味方1(手番)と敵2。破壊力100・耐久100に対し、マナ出力・マナ耐性を変えて物理型・特殊型の式の違いを出す。 */
    private fun field(
        manaOutput: Int = 200,
        manaResistance: Int = 100,
        allyModifiers: List<StatModifier> = emptyList(),
        enemyModifiers: List<StatModifier> = emptyList(),
    ) = BattleField(
        allies = listOf(testAlly(1, attack = 100, manaOutput = manaOutput, statModifiers = allyModifiers)),
        enemies = listOf(testEnemy(2, hp = 10000, defense = 100, manaResistance = manaResistance, statModifiers = enemyModifiers)),
        turnQueue = listOf(1),
    )

    private fun damageOf(field: BattleField, skill: ActiveSkill): Int =
        checkNotNull(field.performAttack(1, 2, skill, SequenceRandom())).second.totalDamage

    /** 特殊型攻撃は破壊力・耐久力ではなくC=200・D=100で計算する(威力100: 100*200*11/(100*50)+2=46。物理型なら24)。 */
    @Test
    fun specialAttack_usesManaOutputAndManaResistance() {
        assertEquals(46, damageOf(field(), skill(SkillCategory.SPECIAL_ATTACK)))
        assertEquals(24, damageOf(field(), skill(SkillCategory.PHYSICAL_ATTACK)))
    }

    /** 耐久力ではなくマナ耐性Dが効く(D=200: 100*100*11/(200*50)+2=13)。 */
    @Test
    fun specialAttack_usesManaResistanceOfTarget() {
        assertEquals(13, damageOf(field(manaOutput = 100, manaResistance = 200), skill(SkillCategory.SPECIAL_ATTACK)))
    }

    /** 特殊型と物理型を併せ持つスキルは、物理型の式(破壊力・耐久力)を使うこと。 */
    @Test
    fun bothCategories_usesPhysicalFormula() {
        assertEquals(24, damageOf(field(), skill(SkillCategory.SPECIAL_ATTACK, SkillCategory.PHYSICAL_ATTACK)))
    }

    /** スキルタイプが空・特殊型を含まないスキルも、今までどおり物理型の式を使うこと。 */
    @Test
    fun noSpecialCategory_usesPhysicalFormula() {
        assertEquals(24, damageOf(field(), skill()))
        assertEquals(24, damageOf(field(), skill(SkillCategory.HEAL)))
    }

    /** バフ・デバフ後のC・Dを使うこと(Cが+4段階で2倍の400、Dが-4段階で100*4/(4+4)=50 → 100*400*11/(50*50)+2=178)。 */
    @Test
    fun specialAttack_usesEffectiveManaStats() {
        val modified = field(
            allyModifiers = listOf(StatModifier(BattleStat.MANA_OUTPUT, stages = 4, remainingTurns = null)),
            enemyModifiers = listOf(StatModifier(BattleStat.MANA_RESISTANCE, stages = -4, remainingTurns = null)),
        )

        assertEquals(178, damageOf(modified, skill(SkillCategory.SPECIAL_ATTACK)))
    }

    /** 破壊力・耐久力のバフは、特殊型攻撃には影響しないこと。 */
    @Test
    fun specialAttack_ignoresAttackAndDefenseBuffs() {
        val modified = field(
            allyModifiers = listOf(StatModifier(BattleStat.ATTACK, stages = 4, remainingTurns = null)),
            enemyModifiers = listOf(StatModifier(BattleStat.DEFENSE, stages = 4, remainingTurns = null)),
        )

        assertEquals(46, damageOf(modified, skill(SkillCategory.SPECIAL_ATTACK)))
    }
}
