package jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AttackHit
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AttackResult
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AttackShot
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.ResistanceOutcome
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.CriticalType

/**
 * 攻撃結果を、1文ずつ順に表示するナレーションの列へ変換する。
 *
 * 対象が1体で実際に行ったヒットが1回なら1文、2回以上なら1発ごとの文とまとめの文になる。
 * 全発が外れた場合は1発ごとの表示をせず「外れた」の1文だけにする。範囲攻撃(対象が2体以上)では
 * 1ヒットごとの表示はせず、開始・外れた相手のまとめ・命中した相手のまとめの順に表示する。
 * 範囲攻撃では、ダメージの文の後に、クリティカル・バッドクリティカルが出たヒットと、防御でクリティカルを防いだヒットごとに1文ずつ足す。
 * 攻撃の文の後に、そのヒットでブレイクした対象ごとに「ブレイクした」の文を、ブレイクした順に1文ずつ足す。
 * 属性耐性による無効は「しかしBには効かなかった!」(多段は「2回目: 効かなかった!」、範囲は「Bには効かなかった!」)、吸収は「Bは攻撃を吸収した! HPが30回復」、
 * 反射は「Bは攻撃を跳ね返した! Aに30ダメージ」と攻撃者側の結果(無効・吸収・クリティカル・ブレイク)の文になり、クリティカルの文は本文の後に併記する。
 * ダメージを与えたヒットがひとつもない多段攻撃は、合計ダメージのまとめを出さない。
 * 範囲の多段攻撃は回ごとに報告する(詳細は[toNarrationSteps])。
 * 攻撃のダメージで睡眠・凍結が解除されたヒットは、その後に「Bは目を覚ました!」「Bの凍結が解けた!」の文を足す(反射では攻撃者の分)。
 * 通常攻撃以外の技(ローリング・ソバットなど)では、「攻撃!」の部分だけを技名にする(1発ごとの文・まとめ・ブレイクなどはそのまま)。
 * 文の組み立てをViewModelの演出の進行から切り離し、単体でテストできるようにすることを目的とする。
 *
 * @receiver 攻撃の結果。
 * @param nameOf FPIDから表示名を引く関数。
 * @return 順に表示するナレーション。
 */
fun AttackResult.toNarrations(nameOf: (Int) -> String): List<BattleNarration> =
    toNarrationSteps(nameOf).map { it.narration }

/**
 * 攻撃結果を、ナレーションと表示するフィールドの組([NarrationStep])の列へ変換する。文面は[toNarrations]の説明のとおり。
 *
 * 各文にどのスナップショットを対応させるかも、ここで決める(ナレーションの進行とHPなどの表示を連動させるため)。
 * - 単体: 各ヒットの文(「N回目: …」とその後に続く吸収・反射・クリティカルの文)はそのヒットの[AttackShot.fieldAfter]。
 * - 範囲で発動回数1: 開始の文は[AttackResult.fieldBeforeShots]、以降は最後に実行されたヒットの[AttackShot.fieldAfter]。
 * - 範囲の多段(対象2体以上かつ発動回数2以上): 開始の文は[AttackResult.fieldBeforeShots]、N回目の文はN回目で最後に実行されたヒットの[AttackShot.fieldAfter]。
 *   回ごとに「N回目: Bに30、Cに25ダメージ!」のように報告し(ブレイクの文は接頭辞なし)、最後に対象ごとの合計のまとめを出す。
 * - 技全体の最後の文は必ず[AttackResult.fieldAfter](技の最後にまとめて付くTPの反映のため)。
 * スナップショットが`null`(テストで組み立てた結果)の文は、フィールドも`null`になる。
 *
 * @receiver 攻撃の結果。
 * @param nameOf FPIDから表示名を引く関数。
 * @return 順に表示するナレーションと、その文を出すときに表示するフィールドの列。
 */
fun AttackResult.toNarrationSteps(nameOf: (Int) -> String): List<NarrationStep> {
    val steps = if (isAreaMultiStrike()) {
        areaMultiStrikeSteps(nameOf)
    } else {
        attackSteps(nameOf) + hits.filter { it.causedBreak }.map {
            NarrationStep(BattleNarration.Broke(nameOf(it.targetFpid)), lastExecutedField())
        }
    }
    if (steps.isEmpty()) return steps
    return steps.dropLast(1) + steps.last().copy(field = fieldAfter)
}

/**
 * 範囲の多段攻撃(対象が2体以上で、いずれかの対象に2回以上のヒットがある)か。
 *
 * @receiver 攻撃の結果。
 * @return 範囲の多段攻撃なら`true`。
 */
private fun AttackResult.isAreaMultiStrike(): Boolean = hits.size >= 2 && hits.any { it.shots.size >= 2 }

/**
 * [round]回目(0始まり)で最後に実行されたヒットの、反映後のフィールドを返す。回数優先の処理順なので、
 * [AttackResult.hits]の順で[round]回目のヒットを持つ最後の対象のものが該当する。
 *
 * @receiver 攻撃の結果。
 * @param round 回(0始まり)。
 * @return そのヒットの[AttackShot.fieldAfter]。該当するヒットがない、またはスナップショットがなければ`null`。
 */
private fun AttackResult.fieldAfterRound(round: Int): BattleField? =
    hits.lastOrNull { it.shots.size > round }?.shots?.get(round)?.fieldAfter

/**
 * 技全体で最後に実行されたヒットの、反映後のフィールドを返す。
 *
 * @receiver 攻撃の結果。
 * @return そのヒットの[AttackShot.fieldAfter]。ヒットがない、またはスナップショットがなければ`null`。
 */
private fun AttackResult.lastExecutedField(): BattleField? =
    fieldAfterRound(hits.maxOf { it.shots.size } - 1)

/**
 * 範囲の多段攻撃を、回ごとに報告するナレーションの列へ変換する。
 *
 * 全回・全対象が外れなら「Aの○○! しかし外れた!」の1文だけ。そうでなければ、開始の文 → 回ごと
 * (外れた相手 → ダメージを与えた相手 → 無効 → ヒットごとのクリティカル・吸収・反射の文 → その回でブレイクした対象)→ 合計のまとめ、の順にする。
 * ブレイク以外の文には「N回目: 」を付ける([BattleNarration.InRound])。
 *
 * @receiver 攻撃の結果。
 * @param nameOf FPIDから表示名を引く関数。
 * @return 順に表示するナレーションとフィールドの列。最後の文のフィールドは呼び出し側が最終フィールドへ置き換える。
 */
private fun AttackResult.areaMultiStrikeSteps(nameOf: (Int) -> String): List<NarrationStep> {
    val attackerName = nameOf(attackerFpid)
    val skill = skillName.takeUnless { isNormalAttack }
    if (hits.all { it.isMissed }) {
        return listOf(NarrationStep(BattleNarration.AreaAttackAllMissed(attackerName, skill), fieldAfter))
    }
    val steps = mutableListOf(NarrationStep(BattleNarration.AreaAttackStart(attackerName, skill), fieldBeforeShots))
    for (round in 0 until hits.maxOf { it.shots.size }) {
        val roundShots = hits.mapNotNull { hit -> hit.shots.getOrNull(round)?.let { hit to it } }
        val field = fieldAfterRound(round)
        val missed = roundShots.filter { !it.second.isHit }
        val landed = roundShots.filter { it.second.isHit }
        val damaged = landed.filter { it.second.resistance == ResistanceOutcome.NONE }
        val nullified = landed.filter { it.second.resistance == ResistanceOutcome.NULLIFIED }
        val sentences = buildList<BattleNarration> {
            if (missed.isNotEmpty()) add(BattleNarration.AreaMissed(missed.map { nameOf(it.first.targetFpid) }))
            if (damaged.isNotEmpty()) {
                add(BattleNarration.AreaHit(damaged.map { AreaHitEntry(nameOf(it.first.targetFpid), it.second.damage) }))
            }
            if (nullified.isNotEmpty()) add(BattleNarration.AreaNullified(nullified.map { nameOf(it.first.targetFpid) }))
            landed.forEach { (hit, shot) ->
                val targetName = nameOf(hit.targetFpid)
                when (shot.resistance) {
                    ResistanceOutcome.NONE, ResistanceOutcome.NULLIFIED ->
                        addAll(criticalNarrations(targetName, shot.critical, shot.isCriticalBlocked))
                    ResistanceOutcome.ABSORBED, ResistanceOutcome.REFLECTED ->
                        addAll(resistedShotNarrations(shot, attackerName, targetName))
                }
                addAll(curedNarrations(targetName, shot.curedAilments))
            }
            roundShots.filter { it.second.causedBreak }.forEach { add(BattleNarration.Broke(nameOf(it.first.targetFpid))) }
        }
        sentences.forEach { sentence ->
            // ブレイクの文(反射で攻撃者がブレイクした文を含む)と睡眠・凍結の解除の文には「N回目: 」を付けない。
            val narration = if (sentence is BattleNarration.Broke || sentence is BattleNarration.AilmentReleasedByDamage) {
                sentence
            } else {
                BattleNarration.InRound(round + 1, sentence)
            }
            steps += NarrationStep(narration, field)
        }
    }
    // ダメージを与えたヒット(命中かつ耐性なし)がある対象だけ、全回の合計をまとめる。
    val totals = hits.filter { hit -> hit.shots.any { it.isHit && it.resistance == ResistanceOutcome.NONE } }
        .map { AreaHitEntry(nameOf(it.targetFpid), it.damage) }
    if (totals.isNotEmpty()) steps += NarrationStep(BattleNarration.AreaAttackTotal(attackerName, totals), lastExecutedField())
    return steps
}

/**
 * 攻撃そのもの(当たり外れとダメージ。単体と、発動回数1の範囲)のナレーションの列を作る。ブレイクの文は[toNarrationSteps]が後ろに足す。
 *
 * @receiver 攻撃の結果。
 * @param nameOf FPIDから表示名を引く関数。
 * @return 順に表示するナレーションとフィールドの列。
 */
private fun AttackResult.attackSteps(nameOf: (Int) -> String): List<NarrationStep> {
    val attackerName = nameOf(attackerFpid)
    // 通常攻撃は「攻撃」のまま、それ以外の技は技名を表示する。
    val skill = skillName.takeUnless { isNormalAttack }
    val single = hits.singleOrNull()
    if (single != null) {
        val targetName = nameOf(single.targetFpid)
        val shots = single.shots
        return when {
            single.isMissed ->
                listOf(NarrationStep(BattleNarration.AttackMissed(attackerName, targetName, skill), shots.lastOrNull()?.fieldAfter))
            shots.size == 1 -> {
                val shot = shots.single()
                val narrations = when (shot.resistance) {
                    ResistanceOutcome.NONE -> listOf(
                        BattleNarration.AttackResult(
                            attackerName = attackerName,
                            targetName = targetName,
                            damage = single.damage,
                            critical = shot.critical,
                            isCriticalBlocked = shot.isCriticalBlocked,
                            skillName = skill,
                        ),
                    )
                    ResistanceOutcome.NULLIFIED ->
                        listOf(BattleNarration.AttackNullified(attackerName, targetName, skill)) +
                            criticalNarrations(targetName, shot.critical, shot.isCriticalBlocked)
                    else -> listOf<BattleNarration>(BattleNarration.AttackLead(attackerName, targetName, skill)) +
                        resistedShotNarrations(shot, attackerName, targetName)
                }
                (narrations + curedNarrations(targetName, shot.curedAilments)).map { NarrationStep(it, shot.fieldAfter) }
            }
            else -> {
                val perShot = shots.flatMapIndexed { index, shot ->
                    val narrations = when {
                        !shot.isHit -> listOf(BattleNarration.ShotMissed(shotNumber = index + 1))
                        shot.resistance == ResistanceOutcome.NONE -> listOf(
                            BattleNarration.ShotHit(
                                shotNumber = index + 1,
                                damage = shot.damage,
                                critical = shot.critical,
                                isCriticalBlocked = shot.isCriticalBlocked,
                            ),
                        )
                        shot.resistance == ResistanceOutcome.NULLIFIED ->
                            listOf<BattleNarration>(BattleNarration.ShotNullified(shotNumber = index + 1)) +
                                criticalNarrations(targetName, shot.critical, shot.isCriticalBlocked)
                        else -> listOf<BattleNarration>(BattleNarration.ShotLanded(shotNumber = index + 1)) +
                            resistedShotNarrations(shot, attackerName, targetName)
                    }
                    (narrations + curedNarrations(targetName, shot.curedAilments)).map { NarrationStep(it, shot.fieldAfter) }
                }
                // ダメージを与えたヒットが1つもない(すべて無効・吸収・反射)ときは、合計ダメージのまとめを出さない。
                val hasDamagingShot = shots.any { it.isHit && it.resistance == ResistanceOutcome.NONE }
                if (hasDamagingShot) {
                    perShot + NarrationStep(
                        BattleNarration.AttackTotal(
                            attackerName = attackerName,
                            targetName = targetName,
                            totalDamage = single.damage,
                            shotCount = shots.size,
                            hitCount = shots.count { it.isHit },
                        ),
                        shots.last().fieldAfter,
                    )
                } else {
                    perShot
                }
            }
        }
    }
    if (hits.all { it.isMissed }) {
        return listOf(NarrationStep(BattleNarration.AreaAttackAllMissed(attackerName, skill), lastExecutedField()))
    }
    val (missed, landed) = hits.partition { it.isMissed }
    // 耐性は対象ごとに一定なので、命中した対象は耐性の扱いで分けられる。
    fun AttackHit.outcome() = shots.first { it.isHit }.resistance
    val damaged = landed.filter { it.outcome() == ResistanceOutcome.NONE }
    val nullified = landed.filter { it.outcome() == ResistanceOutcome.NULLIFIED }
    val afterField = lastExecutedField()
    val steps = mutableListOf(NarrationStep(BattleNarration.AreaAttackStart(attackerName, skill), fieldBeforeShots))
    val rest = buildList<BattleNarration> {
        if (missed.isNotEmpty()) add(BattleNarration.AreaMissed(missed.map { nameOf(it.targetFpid) }))
        if (damaged.isNotEmpty()) add(BattleNarration.AreaHit(damaged.map { AreaHitEntry(nameOf(it.targetFpid), it.damage) }))
        if (nullified.isNotEmpty()) add(BattleNarration.AreaNullified(nullified.map { nameOf(it.targetFpid) }))
        landed.forEach { hit ->
            val targetName = nameOf(hit.targetFpid)
            hit.shots.filter { it.isHit }.forEach { shot ->
                when (shot.resistance) {
                    ResistanceOutcome.NONE, ResistanceOutcome.NULLIFIED ->
                        addAll(criticalNarrations(targetName, shot.critical, shot.isCriticalBlocked))
                    ResistanceOutcome.ABSORBED, ResistanceOutcome.REFLECTED -> addAll(resistedShotNarrations(shot, attackerName, targetName))
                }
                addAll(curedNarrations(targetName, shot.curedAilments))
            }
        }
    }
    return steps + rest.map { NarrationStep(it, afterField) }
}

/**
 * 吸収・反射になった1ヒットのナレーションを作る(無効は呼び出し側が形を選ぶ)。
 * 単体・多段では、呼び出し側がこの前にヒットの文(「AがBに攻撃!」「2回目: 命中!」)を置く。
 *
 * 吸収は「Bは攻撃を吸収した! HPが30回復」の後に、対象側のクリティカルの文を足す。
 * 反射は「Bは攻撃を跳ね返した! Aに30ダメージ」(攻撃者の耐性が無効・反射なら「しかしAには効かなかった!」、攻撃者が吸収したなら「AはHPが30回復」)に続けて、
 * 反射されたヒットのクリティカルの文(Aにクリティカル! など)と、反射ダメージで攻撃者がブレイクしたときの「Aはブレイクした!」を足す。
 *
 * @param shot 吸収または反射になったヒット。
 * @param attackerName 攻撃者の表示名。
 * @param targetName 対象の表示名。
 * @return 順に表示するナレーション。
 */
private fun resistedShotNarrations(shot: AttackShot, attackerName: String, targetName: String): List<BattleNarration> =
    when (shot.resistance) {
        ResistanceOutcome.ABSORBED ->
            listOf<BattleNarration>(BattleNarration.AttackAbsorbed(targetName, shot.absorbedAmount)) +
                criticalNarrations(targetName, shot.critical, shot.isCriticalBlocked)
        ResistanceOutcome.REFLECTED -> {
            val reflected = checkNotNull(shot.reflected)
            val head = when (reflected.outcome) {
                ResistanceOutcome.NULLIFIED -> BattleNarration.AttackReflectedNullified(targetName, attackerName)
                ResistanceOutcome.ABSORBED ->
                    BattleNarration.AttackReflectedAbsorbed(targetName, attackerName, reflected.absorbedAmount)
                else -> BattleNarration.AttackReflected(targetName, attackerName, reflected.damage)
            }
            listOf(head) +
                criticalNarrations(attackerName, reflected.critical, reflected.isCriticalBlocked) +
                (if (reflected.causedBreak) listOf(BattleNarration.Broke(attackerName)) else emptyList()) +
                curedNarrations(attackerName, reflected.curedAilments)
        }
        ResistanceOutcome.NONE, ResistanceOutcome.NULLIFIED -> emptyList()
    }

/**
 * クリティカル・バッドクリティカル・防御で防いだクリティカルを、「Bにクリティカル!」の形の文へ変換する。
 *
 * 無効・吸収・反射のヒットでは本文にクリティカルを織り込めないため、本文の後に併記する。
 *
 * @param name クリティカルを受けた個体(反射では攻撃者)の表示名。
 * @param critical クリティカルの種別。
 * @param isBlocked 防御でクリティカルを防いだか。
 * @return 該当する文。なければ空。
 */
private fun criticalNarrations(name: String, critical: CriticalType, isBlocked: Boolean): List<BattleNarration> = buildList {
    if (isBlocked) add(BattleNarration.AreaCriticalBlocked(name))
    when (critical) {
        CriticalType.CRITICAL -> add(BattleNarration.AreaCritical(name))
        CriticalType.BAD_CRITICAL -> add(BattleNarration.AreaBadCritical(name))
        CriticalType.NONE -> Unit
    }
}
