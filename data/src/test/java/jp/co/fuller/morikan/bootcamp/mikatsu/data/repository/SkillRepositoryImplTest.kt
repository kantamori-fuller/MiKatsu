package jp.co.fuller.morikan.bootcamp.mikatsu.data.repository

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
        assertEquals(1, normal.hitCount)
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
}
