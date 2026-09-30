package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model

/**
 * ある戦闘における、フィールド上の味方陣営・敵陣営をまとめて表すドメインモデル。
 *
 * [allies]・[enemies]それぞれの[BattleFieldPawn.fpid]は、[allPawns]が示す通り
 * このフィールド全体を通じて一意である。攻撃・回復などの効果が「誰が誰に効果を
 * 及ぼすか」をFPIDだけで陣営を問わず指定できるようにすることを目的とする。
 *
 * @property allies 味方陣営の一覧。
 * @property enemies 敵陣営の一覧。
 */
data class BattleField(
    val allies: List<BattleAlly>,
    val enemies: List<BattleEnemy>,
) {
    /**
     * 味方・敵を問わず、フィールド上の全個体をまとめた一覧。
     *
     * FPIDによる対象指定を陣営に関わらず解決したい場合に用いる。
     */
    val allPawns: List<BattleFieldPawn>
        get() = allies + enemies
}
