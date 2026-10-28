package jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.TEST_DIA
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.TEST_STEEL_BODY
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkill
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.PassiveSkill
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.SkillRepository

/**
 * ディア・鋼の体だけを返すテスト用の[SkillRepository]。
 */
class FakeSkillRepository : SkillRepository {
    override suspend fun getActiveSkills(): List<ActiveSkill> = listOf(TEST_DIA)
    override suspend fun getPassiveSkills(): List<PassiveSkill> = listOf(TEST_STEEL_BODY)
    override suspend fun getActiveSkill(id: Int): ActiveSkill? = getActiveSkills().firstOrNull { it.id == id }
    override suspend fun getPassiveSkill(id: Int): PassiveSkill? = getPassiveSkills().firstOrNull { it.id == id }
}
