package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleFieldPawn
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.updatePawn
import kotlin.math.max
import kotlin.random.Random

/**
 * 個体ごとの素早さで行動順が決まる個別ターン制(行動順の進行)を扱う、状態を持たない純粋関数の置き場。
 *
 * リアルタイム要素はなく、行動順は常に[BattleField]の状態だけから前もって決まる。
 * 各個体は「次の手番までの残り距離」を持ち、速度([ActionSpeedFormula.speedOf])で距離を詰めていく。
 * 残り距離が最初に0になった個体から手番が来る。ランダム要素(同順位のくじ引き)は
 * テスト可能にするため[Random]を引数で受け取る。
 */
object ActionTimeline {

    /** 次の手番までに進む必要がある距離の基準値。開始時と行動後の残り距離はこの値になる。 */
    const val BASE_DISTANCE = 10000.0

    /** 「同時に手番が来た」とみなす時間差の相対許容値(浮動小数点の誤差対策)。 */
    internal const val TIME_EPSILON = 1e-9
}

/**
 * 次の手番までの時間を、残り距離と現在の素早さから計算する。
 *
 * 保存せず毎回計算することで、素早さが変化した場合も行動順に即座に反映されるようにする。
 *
 * @receiver 時間を求める個体。
 * @return 次の手番までの時間。
 */
private fun BattleFieldPawn.timeToTurn(): Double = remainingDistance / ActionSpeedFormula.speedOf(agility)

/**
 * 次に行動する個体が決まるまで時間を進め、手番が来た個体を[BattleField.turnQueue]に確定させる。
 *
 * 1. 勝敗が決まっていれば手番を空にして返す。
 * 2. 手番待ちの個体から戦闘不能の個体を除き、まだ誰か残っていればそのまま返す(誰かの手番中)。
 * 3. 誰もいなければ、生きている個体の次の手番までの時間の最小値だけ全員の距離を進め、
 *    最小値と同時とみなせる個体を幸運値の高い順(同値の中はランダム)に並べて手番とする。
 *
 * 一度決めた順番は[BattleField.turnQueue]に保存されるため、再度呼んでもくじは引き直さない。
 *
 * @receiver 更新前の戦闘フィールド。
 * @param random 幸運値が同じ個体の順番を決めるための乱数。
 * @return 手番を進めた戦闘フィールド。
 */
fun BattleField.advanceToNextTurn(random: Random): BattleField {
    if (outcome != null) return copy(turnQueue = emptyList())

    val aliveFpids = allPawns.filterNot { it.isDefeated }.map { it.fpid }.toSet()
    val remainingQueue = turnQueue.filter { it in aliveFpids }
    if (remainingQueue.isNotEmpty()) return copy(turnQueue = remainingQueue)

    val alive = allPawns.filterNot { it.isDefeated }
    val minTime = alive.minOf { it.timeToTurn() }
    val arrivals = alive
        .filter { it.timeToTurn() - minTime <= ActionTimeline.TIME_EPSILON * max(1.0, minTime) }
        .map { it.fpid }
        .toSet()

    val advanced = alive.fold(this) { field, pawn ->
        val distance = (pawn.remainingDistance - minTime * ActionSpeedFormula.speedOf(pawn.agility))
            .coerceAtLeast(0.0)
            .let { if (pawn.fpid in arrivals) 0.0 else it }
        field.updatePawn(
            fpid = pawn.fpid,
            transformAlly = { it.copy(remainingDistance = distance) },
            transformEnemy = { it.copy(remainingDistance = distance) },
        )
    }

    val queue = alive
        .filter { it.fpid in arrivals }
        .groupBy { it.luck }
        .toSortedMap(compareByDescending { it })
        .values
        .flatMap { group -> group.map { it.fpid }.shuffled(random) }
    return advanced.copy(turnQueue = queue)
}

/**
 * 現在の行動者の手番を終え、次の手番が来る個体を決める。
 *
 * [fpid]が現在の行動者でなければ何もしない。一致すれば、その個体を手番待ちから外し、
 * 残り距離を[ActionTimeline.BASE_DISTANCE]に戻して自身の手番の回数を1増やしたうえで、
 * [advanceToNextTurn]で次の手番を決める。
 *
 * @receiver 更新前の戦闘フィールド。
 * @param fpid 手番を終える個体のFPID。
 * @param random 次の手番の同順位を決めるための乱数。
 * @return 手番を終えた戦闘フィールド。
 */
fun BattleField.endTurn(fpid: Int, random: Random): BattleField {
    if (fpid != currentActorFpid) return this
    val ended = updatePawn(
        fpid = fpid,
        transformAlly = { it.copy(remainingDistance = ActionTimeline.BASE_DISTANCE, turnCount = it.turnCount + 1) },
        transformEnemy = { it.copy(remainingDistance = ActionTimeline.BASE_DISTANCE, turnCount = it.turnCount + 1) },
    ).copy(turnQueue = turnQueue.drop(1))
    return ended.advanceToNextTurn(random)
}

/**
 * 指定した個体に次の手番が来るまでの時間を返す。将来の行動順の予測表示に用いる。
 *
 * @receiver 戦闘フィールド。
 * @param fpid 時間を知りたい個体のFPID。
 * @return 次の手番までの時間(手番待ちの個体は0)。個体が見つからない、または戦闘不能なら`null`。
 */
fun BattleField.timeUntilTurn(fpid: Int): Double? {
    val pawn = allPawns.firstOrNull { it.fpid == fpid && !it.isDefeated } ?: return null
    return pawn.timeToTurn()
}
