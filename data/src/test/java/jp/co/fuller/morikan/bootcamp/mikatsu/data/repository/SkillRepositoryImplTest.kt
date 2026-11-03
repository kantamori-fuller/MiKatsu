package jp.co.fuller.morikan.bootcamp.mikatsu.data.repository

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleStat
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkillEffect
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

    /** 研ぎ澄ます(Aスキル6)が仕様どおりのデータで、設定可能な一覧に出ること。ID5は欠番。 */
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
        assertNull(repository.getActiveSkill(5))
    }
}
