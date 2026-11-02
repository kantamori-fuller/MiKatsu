package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleEnemy
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleFieldPawn
import kotlin.random.Random

/**
 * 選べる候補からランダムに1体を選ぶ[EnemyTargetSelector]。
 *
 * 狙われやすさを考慮しない暫定の選び方であり、将来は考慮する実装に差し替える。
 *
 * @property random 対象を選ぶ乱数。テストで結果を固定できるよう外から受け取る。
 */
class RandomEnemyTargetSelector(private val random: Random) : EnemyTargetSelector {

    override fun select(field: BattleField, attacker: BattleEnemy, candidates: List<BattleFieldPawn>): BattleFieldPawn? =
        candidates.randomOrNull(random)
}
