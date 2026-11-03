package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleAlly
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleEnemy
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Character
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleStat
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Enemy
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.StatModifier
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkill
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkillEffect
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.PassiveSkill
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.PassiveSkillEffect
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.PhysicalAttribute
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillArea
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillCategory
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillCost
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillElement
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillRange
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillTargetSelection
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillTargetSide
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillContactType
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillCostType

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
 * @param slot 陣形上のスロット番号。省略時は`fpid - 1`(FPID1〜3が前衛、4〜6が後衛)。
 * @param activeSkills 使えるアクティブスキル。
 * @param passiveSkills 発揮しているパッシブスキル。
 * @param statModifiers かけられているバフ・デバフ。
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
    slot: Int = fpid - 1,
    activeSkills: List<ActiveSkill> = emptyList(),
    passiveSkills: List<PassiveSkill> = emptyList(),
    statModifiers: List<StatModifier> = emptyList(),
) = BattleAlly(
    fpid = fpid,
    character = Character(
        id = fpid, name = "味方$fpid", hp = hp, mana = mp, attack = attack, defense = defense,
        manaOutput = 0, manaResistance = 0, agility = agility, luck = luck,
    ),
    currentHp = hp,
    currentMp = mp,
    slot = slot,
    remainingDistance = ActionTimeline.BASE_DISTANCE,
    turnCount = 0,
    activeSkills = activeSkills,
    passiveSkills = passiveSkills,
    statModifiers = statModifiers,
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
 * @param slot 敵陣営のスロット番号。省略時は0(前衛の左端)。
 * @param statModifiers かけられているバフ・デバフ。
 * @return 生成した敵個体。
 */
fun testEnemy(
    fpid: Int,
    agility: Int = 100,
    luck: Int = 0,
    hp: Int = 50,
    attack: Int = 100,
    defense: Int = 100,
    slot: Int = 0,
    statModifiers: List<StatModifier> = emptyList(),
) = BattleEnemy(
    fpid = fpid,
    enemy = Enemy(
        id = 1, name = "敵", hp = hp, mana = 0, attack = attack, defense = defense,
        manaOutput = 0, manaResistance = 0, agility = agility, luck = luck,
    ),
    currentHp = hp,
    slot = slot,
    remainingDistance = ActionTimeline.BASE_DISTANCE,
    turnCount = 0,
    statModifiers = statModifiers,
)

/**
 * テスト用のアクティブスキルを、必要な項目だけ指定して生成する。
 *
 * @param id スキルID。
 * @param name スキル名。
 * @param costs コスト。
 * @param targetSide 対象にする陣営。
 * @param range レンジ。
 * @param area 範囲。
 * @param targetSelection 対象選択。
 * @param basePower 基礎威力。
 * @param activationCount 発動回数。
 * @param accuracy 命中率。`null`は必中。
 * @param effects 効果。
 * @return 生成したアクティブスキル。
 */
fun testActiveSkill(
    id: Int,
    name: String,
    costs: List<SkillCost> = emptyList(),
    targetSide: SkillTargetSide = SkillTargetSide.ALLY,
    range: SkillRange = SkillRange.RANGE_1,
    area: SkillArea = SkillArea.SINGLE,
    targetSelection: SkillTargetSelection = SkillTargetSelection.ONE,
    basePower: Int? = null,
    activationCount: Int = 1,
    accuracy: Int? = null,
    effects: List<ActiveSkillEffect> = emptyList(),
) = ActiveSkill(
    id = id,
    name = name,
    element = SkillElement.NONE,
    categories = setOf(SkillCategory.HEAL),
    physicalAttribute = null,
    costs = costs,
    coolTime = 0,
    contactType = SkillContactType.NON_CONTACT,
    range = range,
    area = area,
    targetSelection = targetSelection,
    basePower = basePower,
    activationCount = activationCount,
    accuracy = accuracy,
    effects = effects,
    targetSide = targetSide,
)

/** テスト用の通常攻撃(Aスキル1。基礎威力100・1回攻撃)。マスターデータと同じ内容。 */
val TEST_NORMAL_ATTACK = testActiveSkill(
    id = 1,
    name = "通常攻撃",
    targetSide = SkillTargetSide.ENEMY,
    basePower = 100,
    accuracy = 100,
).copy(categories = setOf(SkillCategory.PHYSICAL_ATTACK), physicalAttribute = PhysicalAttribute.IMPACT, contactType = SkillContactType.CONTACT)

/** テスト用の「ディア」(Aスキル2・MP10・レンジ3・エリア全体から一体選択・HPを50回復)。マスターデータと同じ内容。 */
val TEST_DIA = testActiveSkill(
    id = 2,
    name = "ディア",
    range = SkillRange.RANGE_3,
    area = SkillArea.AREA,
    costs = listOf(SkillCost(SkillCostType.MP, 10)),
    effects = listOf(ActiveSkillEffect.Heal(amount = 50)),
)

/** テスト用の「鋼の体」(Pスキル1・受けるダメージを20減らす)。マスターデータと同じ内容。 */
val TEST_STEEL_BODY = PassiveSkill(
    id = 1,
    name = "鋼の体",
    categories = emptySet(),
    effects = listOf(PassiveSkillEffect.DamageReduction(amount = 20)),
)

/** テスト用のもう1つのパッシブスキル(Pスキル2)。ディア(Aスキル2)と同じ数値のIDでも別のスキルとして扱われることの確認に使う。 */
val TEST_OTHER_PASSIVE = PassiveSkill(
    id = 2,
    name = "別のPスキル",
    categories = emptySet(),
    effects = listOf(PassiveSkillEffect.DamageReduction(amount = 1)),
)

/** テスト用の「精神一到」(Aスキル3・MP20・レンジ0・自分の破壊力を+1段階10ターン・命中判定なし)。マスターデータと同じ内容。 */
val TEST_SEISHIN = testActiveSkill(
    id = 3,
    name = "精神一到",
    range = SkillRange.RANGE_0,
    costs = listOf(SkillCost(SkillCostType.MP, 20)),
    effects = listOf(ActiveSkillEffect.StatChange(BattleStat.ATTACK, stages = 1, durationTurns = 10)),
)

/** テスト用の「覇気」(Aスキル4・MP30・レンジ3・敵単体・命中率100・敏捷と耐久力を-1段階8ターン)。マスターデータと同じ内容。 */
val TEST_HAKI = testActiveSkill(
    id = 4,
    name = "覇気",
    targetSide = SkillTargetSide.ENEMY,
    range = SkillRange.RANGE_3,
    costs = listOf(SkillCost(SkillCostType.MP, 30)),
    accuracy = 100,
    effects = listOf(
        ActiveSkillEffect.StatChange(BattleStat.AGILITY, stages = -1, durationTurns = 8),
        ActiveSkillEffect.StatChange(BattleStat.DEFENSE, stages = -1, durationTurns = 8),
    ),
)
