package jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.EnemyTargetSelector
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.performAttack
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AttackResult
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.BattleRepository

/**
 * 現在の手番が敵のとき、その敵の通常攻撃を実行するUseCase。
 *
 * 狙う相手の選び方は[EnemyTargetSelector]に委ね、ダメージ処理は味方の攻撃([AttackUseCase])と
 * 同じ共通関数を使う。手番の終了は呼び出し側が[EndTurnUseCase]で行う。
 *
 * @property battleRepository 戦闘状態を保持するRepository。
 * @property enemyTargetSelector 敵の攻撃対象を選ぶ方法。
 */
class ExecuteEnemyTurnUseCase(
    private val battleRepository: BattleRepository,
    private val enemyTargetSelector: EnemyTargetSelector,
) {

    /**
     * 指定した敵が、対象を選んで通常攻撃する。
     *
     * 呼び出し側が演出(ナレーション)で示した敵と、実際に攻撃する敵が必ず一致するよう、
     * 行動者をFPIDで受け取り、現在の手番の敵でなければ何もしない。
     *
     * @param actorFpid 攻撃させる敵のFPID。
     * @return 攻撃の結果。[actorFpid]が現在の手番の敵でない場合や、攻撃できる対象がいない場合は`null`。
     */
    suspend operator fun invoke(actorFpid: Int): AttackResult? {
        // updateBattleFieldのtransformは再実行され得るため、最後に計算した結果を外の変数に残す。
        var result: AttackResult? = null
        battleRepository.updateBattleField { field ->
            val attacker = field.enemies.firstOrNull { it.fpid == actorFpid && it.fpid == field.currentActorFpid }
            val target = attacker?.let { enemyTargetSelector.select(field, it) }
            val attack = if (attacker != null && target != null) {
                field.performAttack(attacker.fpid, target.fpid)
            } else {
                null
            }
            result = attack?.second
            attack?.first ?: field
        }
        return result
    }
}
