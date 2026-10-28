package jp.co.fuller.morikan.bootcamp.mikatsu.data.repository

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkill
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkillEffect
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.PassiveSkill
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.PassiveSkillEffect
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillCategory
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillContactType
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillCostType
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillTarget
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.SkillRepository
import javax.inject.Inject

/**
 * [SkillRepository]の暫定実装。固定のスキルのマスターデータを返す。
 *
 * スキルIDはアクティブ・パッシブで独立して採番する(アクティブのID1=ディア、パッシブのID1=鋼の体)。
 * 同じ数値でも種別が違えば別のスキルなので、IDは必ず種別とセットで扱う。
 * スキルの追加・調整は[FIXED_ACTIVE_SKILLS]・[FIXED_PASSIVE_SKILLS]を編集するだけで済む。
 */
class SkillRepositoryImpl @Inject constructor() : SkillRepository {

    override suspend fun getActiveSkills(): List<ActiveSkill> = FIXED_ACTIVE_SKILLS

    override suspend fun getPassiveSkills(): List<PassiveSkill> = FIXED_PASSIVE_SKILLS

    override suspend fun getActiveSkill(id: Int): ActiveSkill? = FIXED_ACTIVE_SKILLS.firstOrNull { it.id == id }

    override suspend fun getPassiveSkill(id: Int): PassiveSkill? = FIXED_PASSIVE_SKILLS.firstOrNull { it.id == id }

    companion object {
        private val FIXED_ACTIVE_SKILLS = listOf(
            ActiveSkill(
                id = 1,
                name = "ディア",
                categories = setOf(SkillCategory.HEAL, SkillCategory.STATUS),
                contactType = SkillContactType.NON_CONTACT,
                costType = SkillCostType.MP,
                cost = 10,
                target = SkillTarget.SingleAlly,
                effect = ActiveSkillEffect.Heal(amount = 50),
            ),
        )

        private val FIXED_PASSIVE_SKILLS = listOf(
            PassiveSkill(
                id = 1,
                name = "鋼の体",
                categories = emptySet(),
                effect = PassiveSkillEffect.DamageReduction(amount = 20),
            ),
        )
    }
}
