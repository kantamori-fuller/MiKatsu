package jp.co.fuller.morikan.bootcamp.mikatsu.data.repository

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleStat
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.StatusAilment
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkillEffect
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.PhysicalAttribute
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillArea
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillCategory
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillContactType
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillCost
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillCostType
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillElement
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillRange
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillTargetSelection
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillTargetSide
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** [SkillRepositoryImpl]のマスターデータが、通常攻撃を設定対象から除き、AスキルとPスキルのIDを独立に扱うことを検証するテスト。 */
class SkillRepositoryImplTest {

    private val repository = SkillRepositoryImpl()

    /** 通常攻撃はAスキル1で、基礎威力100・1回・コストなし・敵対象であること。 */
    @Test
    fun normalAttack_isActiveSkill1_withSheetValues() = runBlocking {
        val normal = repository.getNormalAttack()

        assertEquals(1, normal.id)
        assertEquals(100, normal.basePower)
        assertEquals(1, normal.activationCount)
        assertTrue(normal.costs.isEmpty())
        assertEquals(SkillTargetSide.ENEMY, normal.targetSide)
    }

    /** 設定可能な一覧と、設定済みIDの解決には通常攻撃が含まれない(旧データのAスキル1は無視される)こと。 */
    @Test
    fun normalAttack_isExcludedFromCatalogAndResolution() = runBlocking {
        assertTrue(repository.getActiveSkills().none { it.id == 1 })
        assertNull(repository.getActiveSkill(1))
        assertEquals("ディア", repository.getActiveSkill(2)?.name)
    }

    /** Aスキル1(通常攻撃)とPスキル1(鋼の体)のように、IDが同じでも種別ごとに別のスキルであること。 */
    @Test
    fun passiveAndActiveIds_areIndependent() = runBlocking {
        assertEquals("鋼の体", repository.getPassiveSkill(1)?.name)
        assertNull(repository.getPassiveSkill(2))
    }

    /** 精神一到(Aスキル3)が仕様どおりのデータで、設定可能な一覧に出ること。 */
    @Test
    fun seishinIttou_matchesSpec() = runBlocking {
        val skill = checkNotNull(repository.getActiveSkill(3))

        assertEquals("精神一到", skill.name)
        assertEquals(SkillElement.NONE, skill.element)
        assertEquals(setOf(SkillCategory.BUFF), skill.categories)
        assertNull(skill.physicalAttribute)
        assertEquals(listOf(SkillCost(SkillCostType.MP, 20)), skill.costs)
        assertEquals(0, skill.coolTime)
        assertEquals(SkillContactType.NON_CONTACT, skill.contactType)
        assertEquals(SkillRange.RANGE_0, skill.range)
        assertEquals(SkillArea.SINGLE, skill.area)
        assertEquals(SkillTargetSelection.ONE, skill.targetSelection)
        assertNull(skill.basePower)
        assertEquals(1, skill.activationCount)
        assertNull(skill.accuracy)
        assertEquals(listOf(ActiveSkillEffect.StatChange(BattleStat.ATTACK, 1, 10)), skill.effects)
        assertEquals(SkillTargetSide.ALLY, skill.targetSide)
        assertTrue(repository.getActiveSkills().any { it.id == 3 })
    }

    /** 覇気(Aスキル4)が仕様どおりのデータで、設定可能な一覧に出ること。 */
    @Test
    fun haki_matchesSpec() = runBlocking {
        val skill = checkNotNull(repository.getActiveSkill(4))

        assertEquals("覇気", skill.name)
        assertEquals(setOf(SkillCategory.DEBUFF), skill.categories)
        assertEquals(listOf(SkillCost(SkillCostType.MP, 30)), skill.costs)
        assertEquals(SkillRange.RANGE_3, skill.range)
        assertEquals(SkillArea.SINGLE, skill.area)
        assertEquals(SkillTargetSelection.ONE, skill.targetSelection)
        assertNull(skill.basePower)
        assertEquals(1, skill.activationCount)
        assertEquals(100, skill.accuracy)
        assertEquals(
            listOf(
                ActiveSkillEffect.StatChange(BattleStat.AGILITY, -1, 8),
                ActiveSkillEffect.StatChange(BattleStat.DEFENSE, -1, 8),
            ),
            skill.effects,
        )
        assertEquals(SkillTargetSide.ENEMY, skill.targetSide)
        assertTrue(repository.getActiveSkills().any { it.id == 4 })
    }

    /** 研ぎ澄ます(Aスキル6)が仕様どおりのデータで、設定可能な一覧に出ること。 */
    @Test
    fun togisumasu_matchesSpec() = runBlocking {
        val skill = checkNotNull(repository.getActiveSkill(6))

        assertEquals("研ぎ澄ます", skill.name)
        assertEquals(SkillElement.NONE, skill.element)
        assertEquals(setOf(SkillCategory.BUFF), skill.categories)
        assertNull(skill.physicalAttribute)
        assertEquals(listOf(SkillCost(SkillCostType.HP, 30), SkillCost(SkillCostType.MP, 50)), skill.costs)
        assertEquals(0, skill.coolTime)
        assertEquals(SkillContactType.NON_CONTACT, skill.contactType)
        assertEquals(SkillRange.RANGE_0, skill.range)
        assertEquals(SkillArea.SINGLE, skill.area)
        assertEquals(SkillTargetSelection.ONE, skill.targetSelection)
        assertNull(skill.basePower)
        assertEquals(1, skill.activationCount)
        assertNull(skill.accuracy)
        assertEquals(
            listOf(
                ActiveSkillEffect.StatChange(BattleStat.ACCURACY, 1, null),
                ActiveSkillEffect.StatChange(BattleStat.CRITICAL_RATE, 1, null),
                ActiveSkillEffect.StatChange(BattleStat.CRITICAL_DAMAGE, 1, null),
            ),
            skill.effects,
        )
        assertEquals(SkillTargetSide.ALLY, skill.targetSide)
        assertTrue(repository.getActiveSkills().any { it.id == 6 })
    }

    /** ローリング・ソバット(Aスキル5)が仕様どおりのデータで、設定可能な一覧に出ること。 */
    @Test
    fun rollingSobat_matchesSpec() = runBlocking {
        val skill = checkNotNull(repository.getActiveSkill(5))

        assertEquals("ローリング・ソバット", skill.name)
        assertEquals(SkillElement.NONE, skill.element)
        assertEquals(setOf(SkillCategory.PHYSICAL_ATTACK), skill.categories)
        assertEquals(PhysicalAttribute.IMPACT, skill.physicalAttribute)
        assertEquals(listOf(SkillCost(SkillCostType.TP, 40)), skill.costs)
        assertEquals(0, skill.coolTime)
        assertEquals(SkillContactType.CONTACT, skill.contactType)
        assertEquals(SkillRange.RANGE_1, skill.range)
        assertEquals(SkillArea.SINGLE, skill.area)
        assertEquals(SkillTargetSelection.ONE, skill.targetSelection)
        assertEquals(150, skill.basePower)
        assertEquals(1, skill.activationCount)
        assertEquals(90, skill.accuracy)
        assertEquals(emptyList<ActiveSkillEffect>(), skill.effects)
        assertEquals(SkillTargetSide.ENEMY, skill.targetSide)
        assertTrue(repository.getActiveSkills().any { it.id == 5 })
    }

    /** クイックシュート(Aスキル7)が仕様どおりのデータで、先制行動スキルとして設定可能な一覧に出ること。 */
    @Test
    fun quickShot_matchesSpec() = runBlocking {
        val skill = checkNotNull(repository.getActiveSkill(7))

        assertEquals("クイックシュート", skill.name)
        assertEquals(SkillElement.NONE, skill.element)
        assertEquals(setOf(SkillCategory.SPECIAL_ATTACK), skill.categories)
        assertEquals(PhysicalAttribute.PIERCE, skill.physicalAttribute)
        assertEquals(listOf(SkillCost(SkillCostType.MP, 20), SkillCost(SkillCostType.TP, 10)), skill.costs)
        assertEquals(1, skill.coolTime)
        assertEquals(SkillContactType.NON_CONTACT, skill.contactType)
        assertEquals(SkillRange.RANGE_2, skill.range)
        assertEquals(SkillArea.COLUMN, skill.area)
        assertEquals(SkillTargetSelection.ALL, skill.targetSelection)
        assertEquals(50, skill.basePower)
        assertEquals(2, skill.activationCount)
        assertEquals(80, skill.accuracy)
        assertEquals(emptyList<ActiveSkillEffect>(), skill.effects)
        assertEquals(SkillTargetSide.ENEMY, skill.targetSide)
        assertEquals(1, skill.preemptivePriority)
        assertTrue(skill.isPreemptive)
        assertTrue(skill.usesSpecialDamage)
        assertTrue(repository.getActiveSkills().any { it.id == 7 })
    }

    /** クイックシュート以外のスキル(通常攻撃を含む)は、先制行動スキルではないこと。 */
    @Test
    fun otherSkills_areNotPreemptive() = runBlocking {
        val others = repository.getActiveSkills().filter { it.id != 7 } + repository.getNormalAttack()

        assertTrue(others.all { it.preemptivePriority == null && !it.isPreemptive })
    }

    /** カオスリズム(Aスキル8)がスキル一覧どおり、12種の状態異常を各5ターン・成功率100で与えるネガティブエフェクトであること。 */
    @Test
    fun chaosRhythm_isActiveSkill8_withSheetValues() = runBlocking {
        val skill = checkNotNull(repository.getActiveSkill(8))

        assertEquals("カオスリズム", skill.name)
        assertEquals(setOf(SkillCategory.NEGATIVE_EFFECT), skill.categories)
        assertEquals(listOf(SkillCost(SkillCostType.HP, 50), SkillCost(SkillCostType.TP, 10)), skill.costs)
        assertEquals(0, skill.coolTime)
        assertEquals(SkillContactType.NON_CONTACT, skill.contactType)
        assertEquals(SkillRange.RANGE_3, skill.range)
        assertEquals(SkillArea.FORMATION, skill.area)
        assertEquals(SkillTargetSelection.ONE, skill.targetSelection)
        assertNull(skill.basePower)
        assertEquals(1, skill.activationCount)
        assertEquals(70, skill.accuracy)
        assertEquals(SkillTargetSide.ENEMY, skill.targetSide)
        assertEquals(
            listOf(
                StatusAilment.POISON, StatusAilment.BURN, StatusAilment.INFECTION, StatusAilment.BLEED,
                StatusAilment.PARALYSIS, StatusAilment.SHOCK, StatusAilment.CONTAMINATION, StatusAilment.CURSE,
                StatusAilment.MP_SEAL, StatusAilment.TP_SEAL, StatusAilment.DIZZINESS, StatusAilment.BRAND,
            ).map { ActiveSkillEffect.InflictAilment(it, durationTurns = 5, successRate = 100) },
            skill.effects,
        )
    }

    /** 命頂戴(Aスキル9)がスキル一覧どおり、敵全体へ3回、成功率30の即死を試みるネガティブエフェクトであること。 */
    @Test
    fun lifeTaker_isActiveSkill9_withSheetValues() = runBlocking {
        val skill = checkNotNull(repository.getActiveSkill(9))

        assertEquals("命頂戴", skill.name)
        assertEquals(setOf(SkillCategory.NEGATIVE_EFFECT), skill.categories)
        assertEquals(listOf(SkillCost(SkillCostType.MP, 10)), skill.costs)
        assertEquals(SkillRange.RANGE_3, skill.range)
        assertEquals(SkillArea.AREA, skill.area)
        assertEquals(SkillTargetSelection.ALL, skill.targetSelection)
        assertNull(skill.basePower)
        assertEquals(3, skill.activationCount)
        assertEquals(100, skill.accuracy)
        assertEquals(SkillTargetSide.ENEMY, skill.targetSide)
        assertEquals(listOf<ActiveSkillEffect>(ActiveSkillEffect.InstantDeath(successRate = 30)), skill.effects)
    }
}
