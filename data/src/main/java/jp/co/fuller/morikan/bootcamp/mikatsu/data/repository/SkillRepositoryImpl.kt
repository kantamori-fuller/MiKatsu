package jp.co.fuller.morikan.bootcamp.mikatsu.data.repository

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkill
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkillEffect
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.PassiveSkill
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.PassiveSkillEffect
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
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.SkillRepository
import javax.inject.Inject

/**
 * [SkillRepository]の暫定実装。スキル一覧(`data/スキル一覧.xlsx`)を書き写した固定のマスターデータを返す。
 *
 * スキルIDはアクティブ・パッシブで独立して採番する(Aスキル1=通常攻撃、Aスキル2=ディア、Pスキル1=鋼の体)。
 * 同じ数値でも種別が違えば別のスキルなので、IDは必ず種別とセットで扱う。
 * 通常攻撃(Aスキル1)は全キャラクター・全敵が標準で持つため、設定可能な一覧や設定済みスキルの解決には含めない。
 *
 * ## シートの1行を書き写す手順(テンプレート)
 *
 * Aスキルは、シートの列を上から順に下の雛形の各行へ当てはめる(属性はシートのタブ名)。
 * ```
 * ActiveSkill(
 *     id = 0,                                      // ID
 *     name = "",                                   // 名前
 *     element = SkillElement.NONE,                 // 属性(シートのタブ。無/炎/崩/水/氷/生/空/雷/神/地/金/死/侵/龍)
 *     categories = setOf(SkillCategory.XXX),       // スキルタイプ(複数可。物理型攻撃/特殊型攻撃/回復/変化/バフ/デバフ/ポジティブエフェクト/ネガティブエフェクト/トラップ)
 *     physicalAttribute = null,                    // 物理属性(衝撃/斬撃/刺突。空欄はnull)
 *     costs = listOf(SkillCost(SkillCostType.MP, 30), SkillCost(SkillCostType.HP, 10)), // コストタイプ+コスト消費(MP30&HP10。0なら emptyList())
 *     coolTime = 0,                                // CT(1tなら1)
 *     contactType = SkillContactType.CONTACT,      // 接触(接触/非接触)
 *     range = SkillRange.RANGE_1,                  // レンジ(1/2/3)
 *     area = SkillArea.SINGLE,                     // 範囲(単体/陣/列/エリア全体/フィールド全体)
 *     targetSelection = SkillTargetSelection.ONE,  // 対象選択(一体選択/全て対象)
 *     basePower = null,                            // 基礎威力(ダメージなしはnull)
 *     hitCount = 1,                                // 攻撃回数(2回なら2)
 *     accuracy = 100,                              // 命中率(0〜100。空欄=命中が関係しないスキルはnull)
 *     effects = emptyList(),                       // 効果(なしならemptyList())
 *     targetSide = SkillTargetSide.ENEMY,          // 対象の陣営(シートにまだない列。敵ならENEMY、味方ならALLY)
 * )
 * ```
 * Pスキルは、列が少ない次の雛形に当てはめる。
 * ```
 * PassiveSkill(
 *     id = 0,                                      // ID
 *     name = "",                                   // 名前
 *     categories = emptySet(),                     // スキルタイプ
 *     effects = listOf(PassiveSkillEffect.XXX),    // 効果
 * )
 * ```
 * 書き終えたら、Aスキルは[FIXED_ACTIVE_SKILLS]の末尾へ、Pスキルは[FIXED_PASSIVE_SKILLS]の末尾へ追加する。
 */
class SkillRepositoryImpl @Inject constructor() : SkillRepository {

    override suspend fun getNormalAttack(): ActiveSkill = NORMAL_ATTACK

    override suspend fun getActiveSkills(): List<ActiveSkill> = FIXED_ACTIVE_SKILLS

    override suspend fun getPassiveSkills(): List<PassiveSkill> = FIXED_PASSIVE_SKILLS

    override suspend fun getActiveSkill(id: Int): ActiveSkill? = FIXED_ACTIVE_SKILLS.firstOrNull { it.id == id }

    override suspend fun getPassiveSkill(id: Int): PassiveSkill? = FIXED_PASSIVE_SKILLS.firstOrNull { it.id == id }

    companion object {
        /** 無属性Aスキル ID1「通常攻撃」。全キャラクター・全敵が標準で持つ(設定枠を使わない)。 */
        private val NORMAL_ATTACK = ActiveSkill(
            id = 1,
            name = "通常攻撃",
            element = SkillElement.NONE,
            categories = setOf(SkillCategory.PHYSICAL_ATTACK),
            physicalAttribute = PhysicalAttribute.IMPACT,
            costs = emptyList(),
            coolTime = 0,
            contactType = SkillContactType.CONTACT,
            range = SkillRange.RANGE_1,
            area = SkillArea.SINGLE,
            targetSelection = SkillTargetSelection.ONE,
            basePower = 100,
            hitCount = 1,
            accuracy = 100,
            effects = emptyList(),
            targetSide = SkillTargetSide.ENEMY,
        )

        /** 設定可能なAスキル(通常攻撃を除く)。 */
        private val FIXED_ACTIVE_SKILLS = listOf(
            // ディア(スキル一覧にはまだない。属性・レンジ・範囲は仮置き)
            ActiveSkill(
                id = 2,
                name = "ディア",
                element = SkillElement.NONE,
                categories = setOf(SkillCategory.HEAL, SkillCategory.STATUS),
                physicalAttribute = null,
                costs = listOf(SkillCost(SkillCostType.MP, 10)),
                coolTime = 0,
                contactType = SkillContactType.NON_CONTACT,
                range = SkillRange.RANGE_3,
                area = SkillArea.AREA,
                targetSelection = SkillTargetSelection.ONE,
                basePower = null,
                hitCount = 1,
                accuracy = null,
                effects = listOf(ActiveSkillEffect.Heal(amount = 50)),
                targetSide = SkillTargetSide.ALLY,
            ),
        )

        /** 設定可能なPスキル。 */
        private val FIXED_PASSIVE_SKILLS = listOf(
            PassiveSkill(
                id = 1,
                name = "鋼の体",
                categories = emptySet(),
                effects = listOf(PassiveSkillEffect.DamageReduction(amount = 20)),
            ),
        )
    }
}
