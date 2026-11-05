package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleAlly
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleFieldPawn
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleStat
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkill
import kotlin.random.Random

/*
 * 先制行動(カードをタップして予約し、現在の行動者の手番が終わったところで行動順に割り込む仕組み)。
 * 予約・割り込み・取り消しの判定はこのファイルが唯一の実装箇所で、UIも同じ関数で判定する。
 * 割り込みは[endTurn]から、取り消しは[advanceToNextTurn]の系統から呼ばれる。
 */

/**
 * 味方が持つ先制行動スキル([ActiveSkill.preemptivePriority]があるスキル)の一覧。
 *
 * @receiver 調べる味方。
 * @return 先制行動スキル。持たなければ空。
 */
fun BattleAlly.preemptiveSkills(): List<ActiveSkill> = activeSkills.filter { it.isPreemptive }

/**
 * 味方の先制行動の優先度。持っている先制行動スキルの中で一番高い値で、並び順の最初の基準になる。
 *
 * @receiver 調べる味方。
 * @return 優先度。先制行動スキルを持たなければ[Int.MIN_VALUE]。
 */
private fun BattleAlly.preemptivePriority(): Int =
    preemptiveSkills().maxOfOrNull { checkNotNull(it.preemptivePriority) } ?: Int.MIN_VALUE

/**
 * 使用者が、スキルを今使えるかどうかを返す。
 *
 * CTが明けていて([BattleFieldPawn.isOnCooldown])、(味方なら)コストを払え([canAfford])、選べる相手がいる([selectableTargets]が空でない。
 * 魅了・洗脳・発狂による対象の変化を含む)ことをすべて満たし、さらに挑発中でない(挑発中は基礎威力のあるスキルだけ。
 * [BattleFieldPawn.isRestrictedToAttackSkills])と`true`。手番かどうか・ブレイク中かどうかは見ない。
 * スキル一覧の使用可否と、先制行動の予約・取り消しの判定、発狂で選べるスキルの候補が同じ基準になるようここに集約している。
 * 敵はコストを払わないので、CTと対象だけを見る。
 *
 * @receiver 判定の基になる戦闘フィールド。
 * @param actorFpid 使用者のFPID。
 * @param skill 使おうとするアクティブスキル。
 * @return 使えるなら`true`。使用者が見つからなければ`false`。
 */
fun BattleField.isSkillUsable(actorFpid: Int, skill: ActiveSkill): Boolean {
    val actor = allPawns.firstOrNull { it.fpid == actorFpid } ?: return false
    if (actor.isRestrictedToAttackSkills && skill.basePower == null) return false
    val affordable = (actor as? BattleAlly)?.canAfford(skill) ?: true
    return !actor.isOnCooldown(skill.id) && affordable && selectableTargets(actorFpid, skill).isNotEmpty()
}

/**
 * 味方が、先制の手番で行動できる状態かどうか。
 *
 * 戦闘不能でなく、ブレイク中でも状態異常による行動不能でもなく、洗脳・発狂で自動で動いてもおらず([BattleFieldPawn.isAutoActing])、
 * いま使える([isSkillUsable]。魅了の対象の制限・挑発の攻撃技の制限を含む)先制行動スキルが1つ以上あれば`true`。
 * 予約の条件の一部であり、先制の手番が先頭に来た時点で取り消すかどうかの判定にも使う。
 *
 * @receiver 判定の基になる戦闘フィールド。
 * @param fpid 判定する個体のFPID。
 * @return 先制の手番で行動できるなら`true`。味方でなければ`false`。
 */
internal fun BattleField.canActPreemptively(fpid: Int): Boolean {
    val ally = allies.firstOrNull { it.fpid == fpid } ?: return false
    if (ally.isDefeated || ally.isBroken || ally.isIncapacitated || ally.isAutoActing) return false
    return ally.preemptiveSkills().any { isSkillUsable(fpid, it) }
}

/**
 * 個体が、いま先制行動を予約できるかどうか。UIのカードのタップも同じ関数で判定する。
 *
 * 次をすべて満たすときだけ`true`。
 * - 味方で、戦闘不能でなく、ブレイク中でなく、洗脳・発狂でもない。
 * - 勝敗が決まっていない。
 * - 現在の行動者ではない。
 * - まだ予約していない([BattleField.pendingPreemptive])、かつ先制の手番として行動順に入っていない([BattleField.preemptiveTurns])。
 * - 先制行動スキルのうち1つ以上が、CTが明けていて、コストを払え、選べる相手がいる([canActPreemptively])。
 *
 * @receiver 判定の基になる戦闘フィールド。
 * @param fpid 判定する個体のFPID。
 * @return 予約できるなら`true`。
 */
fun BattleField.canReservePreemptive(fpid: Int): Boolean =
    outcome == null &&
        fpid != currentActorFpid &&
        fpid !in pendingPreemptive &&
        fpid !in preemptiveTurns &&
        canActPreemptively(fpid)

/**
 * 個体の先制行動を予約した[BattleField]を返す。予約は[BattleField.pendingPreemptive]の末尾に記録するだけで、行動順はまだ変えない。
 *
 * 割り込みは、現在の行動者の手番が終わるとき([endTurn])に行われる。予約できない場合([canReservePreemptive]が`false`)は`null`。
 *
 * @receiver 予約前の戦闘フィールド。
 * @param fpid 予約する個体のFPID。
 * @return 予約した戦闘フィールド。予約できなければ`null`。
 */
fun BattleField.reservePreemptive(fpid: Int): BattleField? {
    if (!canReservePreemptive(fpid)) return null
    return copy(pendingPreemptive = pendingPreemptive + fpid)
}

/**
 * 予約済みの先制行動をすべて取り出し、行動順の先頭へ割り込ませた[BattleField]を返す。
 *
 * 取り出した個体は、優先度([BattleAlly.preemptivePriority])の高い順 → バフ・デバフ後の素早さの高い順 → 同じく幸運の高い順に並べ、
 * それでも同じ個体同士は[random]でくじを引いて決める。並べた個体は[BattleField.turnQueue]の先頭
 * (残っている同時手番や、まだ残っている先制の手番よりも前)に入り、[BattleField.preemptiveTurns]に印を付ける。
 * すでに通常の手番として並んでいた個体は、その通常の手番を取り除く(前借り。先制の手番が終わると行動ゲージが戻るため、通常の手番は消費される)。
 * 予約は空にする。勝敗が決まっていれば、予約と先制の印をすべて破棄する。
 * くじは、次の手番の決定(手番開始のTPの乱数)より前に引くよう、[endTurn]が[advanceToNextTurn]の前に呼ぶ。
 *
 * @receiver 割り込み前の戦闘フィールド(手番を終えた個体は[BattleField.turnQueue]から外した後)。
 * @param random 同順位のくじを引くための乱数。
 * @return 割り込み後の戦闘フィールド。
 */
internal fun BattleField.applyPreemptiveReservations(random: Random): BattleField {
    if (pendingPreemptive.isEmpty()) return this
    if (outcome != null) return copy(pendingPreemptive = emptyList(), preemptiveTurns = emptySet())
    val ordered = pendingPreemptive
        .distinct()
        .mapNotNull { id -> allies.firstOrNull { it.fpid == id } }
        .groupBy { Triple(it.preemptivePriority(), it.effectiveStat(BattleStat.AGILITY), it.effectiveStat(BattleStat.LUCK)) }
        .toSortedMap(
            compareByDescending<Triple<Int, Int, Int>> { it.first }
                .thenByDescending { it.second }
                .thenByDescending { it.third },
        )
        .values
        .flatMap { group -> group.map { it.fpid }.shuffled(random) }
    val interrupting = ordered.toSet()
    return copy(
        turnQueue = ordered + turnQueue.filterNot { it in interrupting },
        pendingPreemptive = emptyList(),
        preemptiveTurns = preemptiveTurns + interrupting,
    )
}

/**
 * 行動順の先頭にある先制の手番が行動できなくなっていれば、その手番を行動順から外し続ける。
 *
 * 先頭が先制の手番で、その個体が戦闘不能・ブレイク中・使える先制行動スキルが無い(CT・コスト・対象のどれか)ときに外す。
 * 手番は使わないため、TP・防御・残り距離・CTには一切触れない(手番開始のTPと防御の解除は、これより後に行われる)。
 * 外した後の行動順は通常どおり決まる(前借りで消した通常の手番も、行動ゲージが0のままなら通常どおり手番が来る)。
 *
 * @receiver 取り消し前の戦闘フィールド。
 * @return 取り消し後の戦闘フィールドと、取り消した個体のFPID(外した順)の組。ナレーションに使う。
 */
internal fun BattleField.cancelInvalidPreemptiveTurns(): Pair<BattleField, List<Int>> {
    var field = this
    val cancelled = mutableListOf<Int>()
    while (true) {
        val head = field.turnQueue.firstOrNull() ?: break
        if (head !in field.preemptiveTurns || field.canActPreemptively(head)) break
        field = field.copy(turnQueue = field.turnQueue.drop(1), preemptiveTurns = field.preemptiveTurns - head)
        cancelled += head
    }
    return field to cancelled
}
