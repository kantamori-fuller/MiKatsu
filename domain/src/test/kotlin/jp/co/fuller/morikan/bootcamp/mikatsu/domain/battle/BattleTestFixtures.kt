package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleAlly
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleEnemy
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Character
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Enemy
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkill
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkillEffect
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.PassiveSkill
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.PassiveSkillEffect
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillCategory
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillContactType
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillCostType
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillTarget

/**
 * テスト用に味方個体を生成する。
 *
 * @param fpid 個体のFPID。
 * @param agility 素早さ。
 * @param luck 幸運。
 * @param hp 現在HP(最大HPも同値)。
 * @param attack 破壊力。
 * @param defense 耐久力。
 * @param mp 現在MP(最大MPも同値)。
 * @param activeSkills 使えるアクティブスキル。
 * @param passiveSkills 発揮しているパッシブスキル。
 * @return 生成した味方個体。
 */
fun testAlly(
    fpid: Int,
    agility: Int = 100,
    luck: Int = 0,
    hp: Int = 100,
    attack: Int = 100,
    defense: Int = 100,
    mp: Int = 0,
    activeSkills: List<ActiveSkill> = emptyList(),
    passiveSkills: List<PassiveSkill> = emptyList(),
) = BattleAlly(
    fpid = fpid,
    character = Character(
        id = fpid, name = "味方$fpid", hp = hp, mana = mp, attack = attack, defense = defense,
        manaOutput = 0, manaResistance = 0, agility = agility, luck = luck,
    ),
    currentHp = hp,
    currentMp = mp,
    slot = fpid,
    remainingDistance = ActionTimeline.BASE_DISTANCE,
    turnCount = 0,
    activeSkills = activeSkills,
    passiveSkills = passiveSkills,
)

/**
 * テスト用に敵個体を生成する。
 *
 * @param fpid 個体のFPID。
 * @param agility 素早さ。
 * @param luck 幸運。
 * @param hp 現在HP(最大HPも同値)。
 * @param attack 破壊力。
 * @param defense 耐久力。
 * @return 生成した敵個体。
 */
fun testEnemy(
    fpid: Int,
    agility: Int = 100,
    luck: Int = 0,
    hp: Int = 50,
    attack: Int = 100,
    defense: Int = 100,
) = BattleEnemy(
    fpid = fpid,
    enemy = Enemy(
        id = 1, name = "敵", hp = hp, mana = 0, attack = attack, defense = defense,
        manaOutput = 0, manaResistance = 0, agility = agility, luck = luck,
    ),
    currentHp = hp,
    remainingDistance = ActionTimeline.BASE_DISTANCE,
    turnCount = 0,
)

/** テスト用の「ディア」(MP10・味方単体のHPを50回復)。マスターデータと同じ内容。 */
val TEST_DIA = ActiveSkill(
    id = 1,
    name = "ディア",
    categories = setOf(SkillCategory.HEAL, SkillCategory.STATUS),
    contactType = SkillContactType.NON_CONTACT,
    costType = SkillCostType.MP,
    cost = 10,
    target = SkillTarget.SingleAlly,
    effect = ActiveSkillEffect.Heal(amount = 50),
)

/** テスト用の「鋼の体」(受けるダメージを20減らす)。マスターデータと同じ内容。 */
val TEST_STEEL_BODY = PassiveSkill(
    id = 1,
    name = "鋼の体",
    categories = emptySet(),
    effect = PassiveSkillEffect.DamageReduction(amount = 20),
)
