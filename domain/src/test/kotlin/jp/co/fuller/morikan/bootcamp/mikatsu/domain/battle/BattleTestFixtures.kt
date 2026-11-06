package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AilmentResistances
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleAlly
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleEnemy
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Character
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleStat
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.ElementResistances
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
 * @param hp 現在HP(最大HPも同値。種族値には基礎HP分を引いた値を入れ、戦闘中の最大HPがこの値になるようにする)。
 * @param attack 破壊力。
 * @param defense 耐久力。
 * @param mp 現在MP(最大MPも同値)。
 * @param slot 陣形上のスロット番号。省略時は`fpid - 1`(FPID1〜3が前衛、4〜6が後衛)。
 * @param activeSkills 使えるアクティブスキル。
 * @param passiveSkills 発揮しているパッシブスキル。
 * @param statModifiers かけられているバフ・デバフ。
 * @param manaOutput マナ出力(C)。
 * @param manaResistance マナ耐性(D)。
 * @param elementResistances 12属性の耐性。
 * @param ailmentResistances 状態異常耐性。
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
    manaOutput: Int = 0,
    manaResistance: Int = 0,
    elementResistances: ElementResistances = ElementResistances(),
    ailmentResistances: AilmentResistances = AilmentResistances(),
) = BattleAlly(
    fpid = fpid,
    character = Character(
        id = fpid, name = "味方$fpid", hp = hp - HitPointFormula.BASE_HP, mana = mp, attack = attack, defense = defense,
        manaOutput = manaOutput, manaResistance = manaResistance, agility = agility, luck = luck,
        elementResistances = elementResistances,
        ailmentResistances = ailmentResistances,
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
 * @param hp 現在HP(最大HPも同値。種族値には基礎HP分を引いた値を入れ、戦闘中の最大HPがこの値になるようにする)。
 * @param attack 破壊力。
 * @param defense 耐久力。
 * @param slot 敵陣営のスロット番号。省略時は0(前衛の左端)。
 * @param statModifiers かけられているバフ・デバフ。
 * @param manaOutput マナ出力(C)。
 * @param manaResistance マナ耐性(D)。
 * @param elementResistances 12属性の耐性。
 * @param ailmentResistances 状態異常耐性。
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
    manaOutput: Int = 0,
    manaResistance: Int = 0,
    elementResistances: ElementResistances = ElementResistances(),
    ailmentResistances: AilmentResistances = AilmentResistances(),
) = BattleEnemy(
    fpid = fpid,
    enemy = Enemy(
        id = 1, name = "敵", hp = hp - HitPointFormula.BASE_HP, mana = 0, attack = attack, defense = defense,
        manaOutput = manaOutput, manaResistance = manaResistance, agility = agility, luck = luck,
        elementResistances = elementResistances,
        ailmentResistances = ailmentResistances,
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
 * @param coolTime クールタイム。
 * @param preemptivePriority 先制行動の優先度。`null`は通常のスキル。
 * @param categories スキルタイプ。
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
    coolTime: Int = 0,
    preemptivePriority: Int? = null,
    categories: Set<SkillCategory> = setOf(SkillCategory.HEAL),
) = ActiveSkill(
    id = id,
    name = name,
    element = SkillElement.NONE,
    categories = categories,
    physicalAttribute = null,
    costs = costs,
    coolTime = coolTime,
    contactType = SkillContactType.NON_CONTACT,
    range = range,
    area = area,
    targetSelection = targetSelection,
    basePower = basePower,
    activationCount = activationCount,
    accuracy = accuracy,
    effects = effects,
    targetSide = targetSide,
    preemptivePriority = preemptivePriority,
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

/** テスト用の「研ぎ澄ます」(Aスキル6・HP30とMP50・レンジ0・自分の命中率・クリティカル発生率・クリティカル倍率を+1段階ずつ永続)。マスターデータと同じ内容。 */
val TEST_TOGISUMASU = testActiveSkill(
    id = 6,
    name = "研ぎ澄ます",
    range = SkillRange.RANGE_0,
    costs = listOf(SkillCost(SkillCostType.HP, 30), SkillCost(SkillCostType.MP, 50)),
    effects = listOf(
        ActiveSkillEffect.StatChange(BattleStat.ACCURACY, stages = 1, durationTurns = null),
        ActiveSkillEffect.StatChange(BattleStat.CRITICAL_RATE, stages = 1, durationTurns = null),
        ActiveSkillEffect.StatChange(BattleStat.CRITICAL_DAMAGE, stages = 1, durationTurns = null),
    ),
)

/** テスト用の「ローリング・ソバット」(Aスキル5・TP40・レンジ1・敵単体・基礎威力150・命中率90・物理型攻撃)。マスターデータと同じ内容。 */
val TEST_SOBAT = testActiveSkill(
    id = 5,
    name = "ローリング・ソバット",
    targetSide = SkillTargetSide.ENEMY,
    costs = listOf(SkillCost(SkillCostType.TP, 40)),
    basePower = 150,
    accuracy = 90,
).copy(categories = setOf(SkillCategory.PHYSICAL_ATTACK), physicalAttribute = PhysicalAttribute.IMPACT, contactType = SkillContactType.CONTACT)

/**
 * テスト用の「クイックシュート」(Aスキル7・MP20とTP10・CT1・レンジ2・列・全て対象・基礎威力50・2回・命中率80・特殊型攻撃・先制優先度1)。マスターデータと同じ内容。
 */
val TEST_QUICK_SHOT = testActiveSkill(
    id = 7,
    name = "クイックシュート",
    targetSide = SkillTargetSide.ENEMY,
    range = SkillRange.RANGE_2,
    area = SkillArea.COLUMN,
    targetSelection = SkillTargetSelection.ALL,
    costs = listOf(SkillCost(SkillCostType.MP, 20), SkillCost(SkillCostType.TP, 10)),
    basePower = 50,
    activationCount = 2,
    accuracy = 80,
    coolTime = 1,
    preemptivePriority = 1,
    categories = setOf(SkillCategory.SPECIAL_ATTACK),
).copy(physicalAttribute = PhysicalAttribute.PIERCE)
