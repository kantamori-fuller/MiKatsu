package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.BreakFormula
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.CriticalFormula
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.HitPointFormula
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.PassiveSkill

/**
 * バトルフィールドに配置された、敵の1個体を表すドメインモデル。
 *
 * 同じ[Enemy](同じEID・同じ名前)がフィールド上に複数体登場し得るため、
 * EIDとは別にその戦闘限定で一意な[fpid](フィールドポーンID)を割り当てることで、
 * フィールド上の個々の敵を区別できるようにすることを目的とする。
 *
 * @property fpid その戦闘限定で一意な、フィールド上の個体を識別するID。味方([BattleAlly])を
 *   含めたフィールド全体で重複しない。
 * @property enemy 個体の元になった敵のマスターデータ(種族・ステータス)。
 * @property currentHp 現在のHP。戦闘開始時は最大HPと同じ。
 * @property currentTp 現在のTP([BattleFieldPawn.currentTp])。戦闘開始時は0で、次の戦闘へは持ち越さない。
 * @property slot 敵陣営の2行×3列の配置におけるマスの番号([BattleFieldPawn.slot])。出現順に0から連番で割り当てられ、
 *   0〜2が前衛、3〜5が後衛となる。
 * @property remainingDistance 次の手番までに進む必要がある残り距離([BattleFieldPawn.remainingDistance])。
 * @property turnCount この個体が終えた自分の手番の回数([BattleFieldPawn.turnCount])。
 * @property breakGauge ブレイクゲージ([BattleFieldPawn.breakGauge])。戦闘開始時は0で、次の戦闘へは持ち越さない。
 * @property breakResistance ブレイク耐性([BattleFieldPawn.breakResistance])。性質は[breakGauge]と同じ。
 * @property breakPointRate ブレイクポイント倍率([BattleFieldPawn.breakPointRate])。性質は[breakGauge]と同じ。
 * @property criticalDamageRate クリティカル倍率の基本値([BattleFieldPawn.criticalDamageRate])。性質は[breakGauge]と同じ。
 * @property breakState ブレイクの状態([BattleFieldPawn.breakState])。性質は[breakGauge]と同じ。
 * @property isDefending 防御状態か([BattleFieldPawn.isDefending])。性質は[breakGauge]と同じ。
 * @property skillCooldowns クールタイム中のスキル([BattleFieldPawn.skillCooldowns])。戦闘開始時は空で、次の戦闘へは持ち越さない。
 * @property statModifiers かけられているバフ・デバフ([BattleFieldPawn.statModifiers])。命中率・回避率の段階もここに含まれる。戦闘開始時は空。
 * @property ailments かかっている状態異常([BattleFieldPawn.ailments])。戦闘開始時は空で、次の戦闘へは持ち越さない。
 * @property currentMp 現在のMP([BattleFieldPawn.currentMp])。戦闘開始時は最大MP([Enemy.mana])と同じ。汚染・呪怨で減る(スキルはまだ使わない)。
 */
data class BattleEnemy(
    override val fpid: Int,
    val enemy: Enemy,
    override val currentHp: Int,
    override val slot: Int,
    override val remainingDistance: Double,
    override val turnCount: Int,
    override val currentTp: Int = 0,
    override val breakGauge: Double = 0.0,
    override val breakResistance: Int = BreakFormula.BASE_RESISTANCE,
    override val breakPointRate: Int = BreakFormula.BASE_BREAK_POINT_RATE,
    override val criticalDamageRate: Int = CriticalFormula.BASE_DAMAGE_RATE,
    override val breakState: BreakState = BreakState.NONE,
    override val isDefending: Boolean = false,
    override val statModifiers: List<StatModifier> = emptyList(),
    override val skillCooldowns: List<SkillCooldown> = emptyList(),
    override val ailments: List<AilmentState> = emptyList(),
    override val currentMp: Int = enemy.mana,
) : BattleFieldPawn {
    override val maxHp: Int get() = HitPointFormula.maxHpOf(enemy.hp)
    override val maxMp: Int get() = enemy.mana
    override val attack: Int get() = enemy.attack
    override val defense: Int get() = enemy.defense
    override val manaOutput: Int get() = enemy.manaOutput
    override val manaResistance: Int get() = enemy.manaResistance
    override val agility: Int get() = enemy.agility
    override val luck: Int get() = enemy.luck
    override val elementResistances: ElementResistances get() = enemy.elementResistances
    override val passiveSkills: List<PassiveSkill> get() = emptyList()
}
