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
 * @property turnQueue 手番が来ていて、まだ行動していない個体のFPIDを行動順に並べたもの。
 *   同時に手番が来た個体の順番は決定時にここへ確定して保存され、計算し直してもくじを引き直さない。
 *   先頭が現在の行動者([currentActorFpid])となる。
 */
data class BattleField(
    val allies: List<BattleAlly>,
    val enemies: List<BattleEnemy>,
    val turnQueue: List<Int> = emptyList(),
) {
    /**
     * 味方・敵を問わず、フィールド上の全個体をまとめた一覧。
     *
     * FPIDによる対象指定を陣営に関わらず解決したい場合に用いる。
     */
    val allPawns: List<BattleFieldPawn>
        get() = allies + enemies

    /** 現在の行動者のFPID。手番が来ている個体がいない場合は`null`。 */
    val currentActorFpid: Int?
        get() = turnQueue.firstOrNull()

    /**
     * 戦闘の勝敗。
     *
     * 敵が全員戦闘不能なら[BattleOutcome.Victory]、味方が全員戦闘不能なら[BattleOutcome.Defeat]、
     * どちらでもなければ`null`(戦闘続行中)となる。
     */
    val outcome: BattleOutcome?
        get() = when {
            enemies.all { it.isDefeated } -> BattleOutcome.Victory
            allies.all { it.isDefeated } -> BattleOutcome.Defeat
            else -> null
        }
}

/**
 * 指定したFPIDの個体だけを変換した、新しい[BattleField]を返す内部用ヘルパー。
 *
 * 味方・敵のどちらが対象でも、FPIDが一致する個体だけを`copy`する処理をここに集約し、
 * ダメージの適用や行動順の更新などが同じ仕組みを共有できるようにすることを目的とする。
 *
 * @receiver 更新前の戦闘フィールド。
 * @param fpid 変換対象の個体のFPID。
 * @param transformAlly 対象が味方だった場合に適用する変換。
 * @param transformEnemy 対象が敵だった場合に適用する変換。
 * @return 変換を適用した戦闘フィールド。該当するFPIDの個体がなければ内容は変わらない。
 */
internal fun BattleField.updatePawn(
    fpid: Int,
    transformAlly: (BattleAlly) -> BattleAlly,
    transformEnemy: (BattleEnemy) -> BattleEnemy,
): BattleField = copy(
    allies = allies.map { if (it.fpid == fpid) transformAlly(it) else it },
    enemies = enemies.map { if (it.fpid == fpid) transformEnemy(it) else it },
)

/**
 * 指定したFPIDの個体にダメージを与えた、新しい[BattleField]を返す。
 *
 * 味方・敵のどちらが対象でも、FPIDが一致する個体の現在HPだけを減らす。
 * 現在HPは0未満にならない。
 *
 * @receiver ダメージを与える前の戦闘フィールド。
 * @param targetFpid ダメージを受ける個体のFPID。
 * @param damage 与えるダメージ量。
 * @return ダメージを適用した戦闘フィールド。該当するFPIDの個体がなければ内容は変わらない。
 */
fun BattleField.applyDamage(targetFpid: Int, damage: Int): BattleField = updatePawn(
    fpid = targetFpid,
    transformAlly = { it.copy(currentHp = (it.currentHp - damage).coerceAtLeast(0)) },
    transformEnemy = { it.copy(currentHp = (it.currentHp - damage).coerceAtLeast(0)) },
)
