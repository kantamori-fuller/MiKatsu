package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model

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
 */
data class BattleEnemy(
    override val fpid: Int,
    val enemy: Enemy,
) : BattleFieldPawn
