package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.BreakFormula
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.CriticalFormula
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkill
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.PassiveSkill

/**
 * バトルフィールドに配置された、味方の1個体を表すドメインモデル。
 *
 * [BattleEnemy]と対になるモデルであり、[Character]自身のID(端末内での永続的な識別子)とは
 * 別に、その戦闘限定で一意な[fpid](フィールドポーンID)を割り当てることで、敵味方を問わず
 * 同じ仕組みでフィールド上の対象を指定できるようにすることを目的とする。
 *
 * @property fpid その戦闘限定で一意な、フィールド上の個体を識別するID。敵([BattleEnemy])を
 *   含めたフィールド全体で重複しない。
 * @property character 個体の元になった味方キャラクターのデータ。
 * @property currentHp 現在のHP。戦闘開始時は最大HPと同じ。
 * @property currentMp 現在のMP。戦闘開始時は最大MP([maxMp])と同じで、スキルの使用で減少する。
 *   次の戦闘へ持ち越される。
 * @property slot 陣形([Formation])上で配置されているマスのスロット番号([BattleFieldPawn.slot])。上段(0〜2)が前衛、下段(3〜5)が後衛。
 * @property remainingDistance 次の手番までに進む必要がある残り距離([BattleFieldPawn.remainingDistance])。
 * @property turnCount この個体が終えた自分の手番の回数([BattleFieldPawn.turnCount])。
 * @property breakGauge ブレイクゲージ([BattleFieldPawn.breakGauge])。戦闘開始時は0で、次の戦闘へは持ち越さない。
 * @property breakResistance ブレイク耐性([BattleFieldPawn.breakResistance])。性質は[breakGauge]と同じ。
 * @property breakPointRate ブレイクポイント倍率([BattleFieldPawn.breakPointRate])。性質は[breakGauge]と同じ。
 * @property criticalDamageRate クリティカル倍率の基本値([BattleFieldPawn.criticalDamageRate])。性質は[breakGauge]と同じ。
 * @property breakState ブレイクの状態([BattleFieldPawn.breakState])。性質は[breakGauge]と同じ。
 * @property statModifiers かけられているバフ・デバフ([BattleFieldPawn.statModifiers])。命中率・回避率の段階もここに含まれる。戦闘開始時は空で、次の戦闘へは持ち越さない。
 * @property activeSkills [Character.activeSkillIds]から解決した、使えるアクティブスキル。
 * @property passiveSkills [Character.passiveSkillIds]から解決した、発揮しているパッシブスキル。
 */
data class BattleAlly(
    override val fpid: Int,
    val character: Character,
    override val currentHp: Int,
    val currentMp: Int,
    override val slot: Int,
    override val remainingDistance: Double,
    override val turnCount: Int,
    override val breakGauge: Double = 0.0,
    override val breakResistance: Int = BreakFormula.BASE_RESISTANCE,
    override val breakPointRate: Int = BreakFormula.BASE_BREAK_POINT_RATE,
    override val criticalDamageRate: Int = CriticalFormula.BASE_DAMAGE_RATE,
    override val breakState: BreakState = BreakState.NONE,
    override val statModifiers: List<StatModifier> = emptyList(),
    val activeSkills: List<ActiveSkill> = emptyList(),
    override val passiveSkills: List<PassiveSkill> = emptyList(),
) : BattleFieldPawn {
    override val maxHp: Int get() = character.hp

    /** 最大MP。[Character.mana]と同じ値。 */
    val maxMp: Int get() = character.mana

    override val attack: Int get() = character.attack
    override val defense: Int get() = character.defense
    override val manaOutput: Int get() = character.manaOutput
    override val manaResistance: Int get() = character.manaResistance
    override val agility: Int get() = character.agility
    override val luck: Int get() = character.luck
}
