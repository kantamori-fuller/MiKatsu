package jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AttackHit
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AttackResult
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AttackShot
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
 * 通常攻撃以外の技(ローリング・ソバットなど)では、「攻撃!」の部分だけを技名にする(1発ごとの文・まとめ・ブレイクなどはそのまま)。
 * 文の組み立てをViewModelの演出の進行から切り離し、単体でテストできるようにすることを目的とする。
 *
 * @receiver 攻撃の結果。
 * @param nameOf FPIDから表示名を引く関数。
 * @return 順に表示するナレーション。
 */
fun AttackResult.toNarrations(nameOf: (Int) -> String): List<BattleNarration> =
    attackNarrations(nameOf) + hits.filter { it.causedBreak }.map { BattleNarration.Broke(nameOf(it.targetFpid)) }

/**
 * 攻撃そのもの(当たり外れとダメージ)のナレーションの列を作る。ブレイクの文は[toNarrations]が後ろに足す。
 *
 * @receiver 攻撃の結果。
 * @param nameOf FPIDから表示名を引く関数。
 * @return 順に表示するナレーション。
 */
private fun AttackResult.attackNarrations(nameOf: (Int) -> String): List<BattleNarration> {
    val attackerName = nameOf(attackerFpid)
    // 通常攻撃は「攻撃」のまま、それ以外の技は技名を表示する。
    val skill = skillName.takeUnless { isNormalAttack }
    val single = hits.singleOrNull()
    if (single != null) {
        val targetName = nameOf(single.targetFpid)
        val shots = single.shots
        return when {
            single.isMissed -> listOf(BattleNarration.AttackMissed(attackerName, targetName, skill))
            shots.size == 1 -> {
                val shot = shots.single()
                when (shot.resistance) {
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
            }
            else -> {
                val perShot = shots.flatMapIndexed { index, shot ->
                    when {
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
                }
                // ダメージを与えたヒットが1つもない(すべて無効・吸収・反射)ときは、合計ダメージのまとめを出さない。
                val hasDamagingShot = shots.any { it.isHit && it.resistance == ResistanceOutcome.NONE }
                if (hasDamagingShot) {
                    perShot + BattleNarration.AttackTotal(
                        attackerName = attackerName,
                        targetName = targetName,
                        totalDamage = single.damage,
                        shotCount = shots.size,
                        hitCount = shots.count { it.isHit },
                    )
                } else {
                    perShot
                }
            }
        }
    }
    if (hits.all { it.isMissed }) return listOf(BattleNarration.AreaAttackAllMissed(attackerName, skill))
    val (missed, landed) = hits.partition { it.isMissed }
    // 耐性は対象ごとに一定なので、命中した対象は耐性の扱いで分けられる。
    fun AttackHit.outcome() = shots.first { it.isHit }.resistance
    val damaged = landed.filter { it.outcome() == ResistanceOutcome.NONE }
    val nullified = landed.filter { it.outcome() == ResistanceOutcome.NULLIFIED }
    return buildList {
        add(BattleNarration.AreaAttackStart(attackerName, skill))
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
            }
        }
    }
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
                if (reflected.causedBreak) listOf(BattleNarration.Broke(attackerName)) else emptyList()
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
