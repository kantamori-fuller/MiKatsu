package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model

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
 * @property slot 敵陣営の2行×3列の配置におけるマスの番号([BattleFieldPawn.slot])。出現順に0から連番で割り当てられ、
 *   0〜2が前衛、3〜5が後衛となる。
 * @property remainingDistance 次の手番までに進む必要がある残り距離([BattleFieldPawn.remainingDistance])。
 * @property turnCount この個体が終えた自分の手番の回数([BattleFieldPawn.turnCount])。
 */
data class BattleEnemy(
    override val fpid: Int,
    val enemy: Enemy,
    override val currentHp: Int,
    override val slot: Int,
    override val remainingDistance: Double,
    override val turnCount: Int,
) : BattleFieldPawn {
    override val maxHp: Int get() = enemy.hp
    override val attack: Int get() = enemy.attack
    override val defense: Int get() = enemy.defense
    override val agility: Int get() = enemy.agility
    override val luck: Int get() = enemy.luck
    override val passiveSkills: List<PassiveSkill> get() = emptyList()
}
