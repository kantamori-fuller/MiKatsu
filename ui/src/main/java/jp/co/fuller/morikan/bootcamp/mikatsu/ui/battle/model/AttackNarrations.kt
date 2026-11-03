package jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AttackResult
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.CriticalType

/**
 * 攻撃結果を、1文ずつ順に表示するナレーションの列へ変換する。
 *
 * 対象が1体で実際に行ったヒットが1回なら1文、2回以上なら1発ごとの文とまとめの文になる。
 * 全発が外れた場合は1発ごとの表示をせず「外れた」の1文だけにする。範囲攻撃(対象が2体以上)では
 * 1ヒットごとの表示はせず、開始・外れた相手のまとめ・命中した相手のまとめの順に表示する。
 * 範囲攻撃では、ダメージの文の後に、クリティカル・バッドクリティカルが出たヒットと、防御でクリティカルを防いだヒットごとに1文ずつ足す。
 * 攻撃の文の後に、そのヒットでブレイクした対象ごとに「ブレイクした」の文を、ブレイクした順に1文ずつ足す。
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
    val single = hits.singleOrNull()
    if (single != null) {
        val targetName = nameOf(single.targetFpid)
        val shots = single.shots
        return when {
            single.isMissed -> listOf(BattleNarration.AttackMissed(attackerName, targetName))
            shots.size == 1 -> listOf(BattleNarration.AttackResult(
                    attackerName = attackerName,
                    targetName = targetName,
                    damage = single.damage,
                    critical = shots.single().critical,
                    isCriticalBlocked = shots.single().isCriticalBlocked,
                ),
            )
            else -> shots.mapIndexed { index, shot ->
                if (shot.isHit) {
                    BattleNarration.ShotHit(
                        shotNumber = index + 1,
                        damage = shot.damage,
                        critical = shot.critical,
                        isCriticalBlocked = shot.isCriticalBlocked,
                    )
                } else {
                    BattleNarration.ShotMissed(shotNumber = index + 1)
                }
            } + BattleNarration.AttackTotal(
                attackerName = attackerName,
                targetName = targetName,
                totalDamage = single.damage,
                shotCount = shots.size,
                hitCount = shots.count { it.isHit },
            )
        }
    }
    if (hits.all { it.isMissed }) return listOf(BattleNarration.AreaAttackAllMissed(attackerName))
    val (missed, landed) = hits.partition { it.isMissed }
    return buildList {
        add(BattleNarration.AreaAttackStart(attackerName))
        if (missed.isNotEmpty()) add(BattleNarration.AreaMissed(missed.map { nameOf(it.targetFpid) }))
        add(BattleNarration.AreaHit(landed.map { AreaHitEntry(nameOf(it.targetFpid), it.damage) }))
        landed.forEach { hit ->
            hit.shots.forEach { shot ->
                if (shot.isCriticalBlocked) add(BattleNarration.AreaCriticalBlocked(nameOf(hit.targetFpid)))
                when (shot.critical) {
                    CriticalType.CRITICAL -> add(BattleNarration.AreaCritical(nameOf(hit.targetFpid)))
                    CriticalType.BAD_CRITICAL -> add(BattleNarration.AreaBadCritical(nameOf(hit.targetFpid)))
                    CriticalType.NONE -> Unit
                }
            }
        }
    }
}
