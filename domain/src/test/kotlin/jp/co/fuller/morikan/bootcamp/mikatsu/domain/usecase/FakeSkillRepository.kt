package jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.TEST_DIA
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.TEST_NORMAL_ATTACK
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.TEST_OTHER_PASSIVE
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.TEST_STEEL_BODY
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkill
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.PassiveSkill
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.SkillRepository

/**
 * 通常攻撃・ディア・2つのパッシブスキルを返すテスト用の[SkillRepository]。
 *
 * 本物と同じく、通常攻撃(Aスキル1)は設定可能な一覧にも設定済みスキルの解決にも含めない。
 *
 * @param normalAttack 通常攻撃として返すスキル。
 */
class FakeSkillRepository(private val normalAttack: ActiveSkill = TEST_NORMAL_ATTACK) : SkillRepository {
    override suspend fun getNormalAttack(): ActiveSkill = normalAttack
    override suspend fun getActiveSkills(): List<ActiveSkill> = listOf(TEST_DIA)
    override suspend fun getPassiveSkills(): List<PassiveSkill> = listOf(TEST_STEEL_BODY, TEST_OTHER_PASSIVE)
    override suspend fun getActiveSkill(id: Int): ActiveSkill? = getActiveSkills().firstOrNull { it.id == id }
    override suspend fun getPassiveSkill(id: Int): PassiveSkill? = getPassiveSkills().firstOrNull { it.id == id }
}
