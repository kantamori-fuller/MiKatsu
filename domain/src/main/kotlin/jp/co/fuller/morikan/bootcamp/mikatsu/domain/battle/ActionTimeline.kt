package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleFieldPawn
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleStat
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.EndTurnResult
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
private fun BattleFieldPawn.timeToTurn(): Double = remainingDistance / ActionSpeedFormula.speedOf(effectiveStat(BattleStat.AGILITY))

/**
 * 次に行動する個体が決まるまで時間を進め、手番が来た個体を[BattleField.turnQueue]に確定させる。
 *
 * 取り消された先制の手番を知る必要がない呼び出し側(戦闘開始・次の戦闘へ進むとき)向けに、[advanceToNextTurnWithCancellations]の結果のうち
 * 戦闘フィールドだけを返す。詳細はそちらを参照。
 *
 * @receiver 更新前の戦闘フィールド。
 * @param random 幸運値が同じ個体の順番を決めるための乱数。
 * @return 手番を進めた戦闘フィールド。
 */
fun BattleField.advanceToNextTurn(random: Random): BattleField = advanceToNextTurnWithCancellations(random).first

/**
 * 次に行動する個体が決まるまで時間を進め、手番が来た個体を[BattleField.turnQueue]に確定させる。取り消された先制の手番も返す。
 *
 * 1. 勝敗が決まっていれば手番を空にし、予約と先制の印も破棄して返す。
 * 2. 先頭が先制の手番で、その個体が行動できなければ取り消す([cancelInvalidPreemptiveTurns])。手番開始のTPと防御の解除より前に行う。
 * 3. 手番待ちの個体から戦闘不能の個体を除き(先制の手番は取り消しの判定のため除かない)、まだ誰か残っていればそのまま返す(誰かの手番中)。
 * 4. 誰もいなければ、生きている個体の次の手番までの時間の最小値だけ全員の距離を進め、
 *    最小値と同時とみなせる個体を幸運値の高い順(同値の中はランダム)に並べて手番とする。
 *
 * 新たに現在の行動者になった個体(キューの先頭)の防御状態は、どちらの経路でもここで解除する([releaseDefense])。
 * 現在の行動者が決まったところで、その個体に手番開始のTP(6〜8)を加える([gainTurnStartTp])。ブレイクで自動的に飛ばされる手番でも受け取る。
 * 先制の手番も通常の手番と同じく、ここで防御の解除と手番開始のTPを受け取る。
 * 乱数は同時手番のくじの後に引く。勝敗が決まっていれば加えない。
 * 一度決めた順番は[BattleField.turnQueue]に保存されるため、再度呼んでもくじは引き直さない(ただし新しい手番として呼ぶたびにTPは加わるため、1回の手番につき1回だけ呼ぶこと)。
 *
 * @receiver 更新前の戦闘フィールド。
 * @param random 幸運値が同じ個体の順番を決めるための乱数。
 * @return 手番を進めた戦闘フィールドと、取り消された先制の手番の個体のFPID(取り消した順)の組。
 */
fun BattleField.advanceToNextTurnWithCancellations(random: Random): Pair<BattleField, List<Int>> {
    if (outcome != null) {
        return copy(turnQueue = emptyList(), pendingPreemptive = emptyList(), preemptiveTurns = emptySet()) to emptyList()
    }

    val (settled, cancelled) = cancelInvalidPreemptiveTurns()
    val aliveFpids = settled.allPawns.filterNot { it.isDefeated }.map { it.fpid }.toSet()
    val remainingQueue = settled.turnQueue.filter { it in aliveFpids || it in settled.preemptiveTurns }
    if (remainingQueue.isNotEmpty()) {
        val actor = remainingQueue.first()
        val next = settled
            .copy(turnQueue = remainingQueue, preemptiveTurns = settled.preemptiveTurns.filter { it in remainingQueue }.toSet())
            .releaseDefense(actor)
            .gainTurnStartTp(actor, random)
        return next to cancelled
    }

    val alive = settled.allPawns.filterNot { it.isDefeated }
    val minTime = alive.minOf { it.timeToTurn() }
    val arrivals = alive
        .filter { it.timeToTurn() - minTime <= ActionTimeline.TIME_EPSILON * max(1.0, minTime) }
        .map { it.fpid }
        .toSet()

    val advanced = alive.fold(settled) { field, pawn ->
        val distance = (pawn.remainingDistance - minTime * ActionSpeedFormula.speedOf(pawn.effectiveStat(BattleStat.AGILITY)))
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
        .groupBy { it.effectiveStat(BattleStat.LUCK) }
        .toSortedMap(compareByDescending { it })
        .values
        .flatMap { group -> group.map { it.fpid }.shuffled(random) }
    // 手番開始のTPは、同時手番のくじ(shuffle)の後に引く。
    val next = advanced.copy(turnQueue = queue, preemptiveTurns = emptySet()).releaseDefense(queue.first()).gainTurnStartTp(queue.first(), random)
    return next to cancelled
}

/**
 * 現在の行動者の手番を終え、次の手番が来る個体を決める。取り消された先制の手番は捨てる。
 *
 * 取り消された先制の手番を知りたい呼び出し側は[endTurnWithCancellations]を使う。こちらはその結果のうち戦闘フィールドだけを返す。
 *
 * @receiver 更新前の戦闘フィールド。
 * @param fpid 手番を終える個体のFPID。
 * @param random 予約の並びと次の手番の同順位を決めるための乱数。
 * @return 手番を終えた戦闘フィールド。
 */
fun BattleField.endTurn(fpid: Int, random: Random): BattleField = endTurnWithEvents(fpid, random).first

/**
 * 現在の行動者の手番を終え、次の手番が来る個体を決める。取り消された先制の手番も返す。
 *
 * 手番終了時の状態異常の出来事([TurnEndEvent])を知りたい呼び出し側は[endTurnWithEvents]を使う。こちらはその結果のうち
 * 戦闘フィールドと、取り消された先制の手番だけを返す。詳細は[endTurnWithEvents]を参照。
 *
 * @receiver 更新前の戦闘フィールド。
 * @param fpid 手番を終える個体のFPID。
 * @param random 予約の並びと次の手番の同順位を決めるための乱数。
 * @return 手番を終えた戦闘フィールドと、取り消された先制の手番の個体のFPIDの組。現在の行動者でなければ、更新前のフィールドと空の組。
 */
fun BattleField.endTurnWithCancellations(fpid: Int, random: Random): Pair<BattleField, List<Int>> {
    val (field, result) = endTurnWithEvents(fpid, random)
    return field to result.cancelledPreemptive
}

/**
 * 現在の行動者の手番を終え、次の手番が来る個体を決める。取り消された先制の手番と、手番終了時の状態異常の出来事も返す。
 *
 * [fpid]が現在の行動者でなければ何もしない。一致すれば、その個体を手番待ちから外し(先制の手番なら先制の印も外し)、
 * 残り距離を[ActionTimeline.BASE_DISTANCE]に戻して自身の手番の回数を1増やし、ブレイクの状態を1段階進め
 * ([progressBreakState])、バフ・デバフ([progressStatModifiers])とスキルのCT([progressCooldowns])を1手番分進め、
 * 状態異常を処理する([processTurnEndAilments]。ダメージ・伝染・解除。行動不能で飛ばされた手番も1手番として数える)。
 * 先制の手番も通常の手番とまったく同じ扱いなので、これらはすべて同様に行う。
 * 状態異常のダメージで戦闘不能になっても、勝敗の判定は[advanceToNextTurnWithCancellations]が既存どおり行う。
 * そのうえで、手番を進める処理([advanceToNextTurnWithCancellations])の**前**に、その時点の予約をすべて行動順の先頭へ割り込ませる
 * ([applyPreemptiveReservations])。先制の手番の最中に入った予約も、その手番の終了でここを通るため、残っている先制の手番より前に入る。
 * 乱数は、状態異常の感染の伝染 → 割り込みのくじ → 手番開始のTPの順に引く。
 *
 * @receiver 更新前の戦闘フィールド。
 * @param fpid 手番を終える個体のFPID。
 * @param random 感染の伝染・予約の並び・次の手番の同順位を決めるための乱数。
 * @return 手番を終えた戦闘フィールドと、取り消された先制の手番・状態異常の出来事の組([EndTurnResult])。現在の行動者でなければ、更新前のフィールドと空の結果。
 */
fun BattleField.endTurnWithEvents(fpid: Int, random: Random): Pair<BattleField, EndTurnResult> {
    if (fpid != currentActorFpid) return this to EndTurnResult()
    val ended = updatePawn(
        fpid = fpid,
        transformAlly = { it.copy(remainingDistance = ActionTimeline.BASE_DISTANCE, turnCount = it.turnCount + 1) },
        transformEnemy = { it.copy(remainingDistance = ActionTimeline.BASE_DISTANCE, turnCount = it.turnCount + 1) },
    ).copy(turnQueue = turnQueue.drop(1), preemptiveTurns = preemptiveTurns - fpid)
        .progressBreakState(fpid)
        .progressStatModifiers(fpid)
        .progressCooldowns(fpid)
    val (afterAilments, events) = ended.processTurnEndAilments(fpid, random)
    val (next, cancelled) = afterAilments.applyPreemptiveReservations(random).advanceToNextTurnWithCancellations(random)
    return next to EndTurnResult(cancelledPreemptive = cancelled, events = events)
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
