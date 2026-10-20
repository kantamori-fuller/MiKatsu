package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleAlly
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleEnemy
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import kotlin.random.Random

/**
 * 生きている味方からランダムに1体を選ぶ[EnemyTargetSelector]。
 *
 * 前衛/後衛の概念が入るまでの暫定の選び方であり、将来は狙われやすさを考慮する実装に差し替える。
 *
 * @property random 対象を選ぶ乱数。テストで結果を固定できるよう外から受け取る。
 */
class RandomEnemyTargetSelector(private val random: Random) : EnemyTargetSelector {

    override fun select(field: BattleField, attacker: BattleEnemy): BattleAlly? =
        field.allies.filterNot { it.isDefeated }.randomOrNull(random)
}
