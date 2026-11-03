package jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.EnemyTargetSelector
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.performAttack
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.selectableTargets
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AttackResult
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.BattleRepository
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.SkillRepository
import kotlin.random.Random

/**
 * 現在の手番が敵のとき、その敵の通常攻撃を実行するUseCase。
 *
 * 通常攻撃が届く(レンジ・陣営の条件を満たす)相手を[selectableTargets]で求め、その中から誰を狙うかを
 * [EnemyTargetSelector]に委ねる。ダメージ処理は味方の攻撃([AttackUseCase])と同じ共通関数を使う。
 * 届く相手がいない場合や、敵がブレイク中で行動できない場合は何もしない。手番の終了は呼び出し側が[EndTurnUseCase]で行う。
 *
 * 威力・攻撃回数・命中率は通常攻撃のスキルデータ([SkillRepository.getNormalAttack])から読む。
 *
 * @property battleRepository 戦闘状態を保持するRepository。
 * @property skillRepository 通常攻撃のスキルデータを提供するRepository。
 * @property enemyTargetSelector 敵の攻撃対象を選ぶ方法。
 * @property random 命中判定の乱数。
 */
class ExecuteEnemyTurnUseCase(
    private val battleRepository: BattleRepository,
    private val skillRepository: SkillRepository,
    private val enemyTargetSelector: EnemyTargetSelector,
    private val random: Random,
) {

    /**
     * 指定した敵が、対象を選んで通常攻撃する。
     *
     * 呼び出し側が演出(ナレーション)で示した敵と、実際に攻撃する敵が必ず一致するよう、
     * 行動者をFPIDで受け取り、現在の手番の敵でなければ何もしない。
     *
     * @param actorFpid 攻撃させる敵のFPID。
     * @return 攻撃の結果。[actorFpid]が現在の手番の敵でない場合や、攻撃が届く対象がいない場合は`null`(何も変更しない)。
     */
    suspend operator fun invoke(actorFpid: Int): AttackResult? {
        // updateBattleFieldのtransformは再実行され得るため、最後に計算した結果を外の変数に残す。
        val normalAttack = skillRepository.getNormalAttack()
        var result: AttackResult? = null
        battleRepository.updateBattleField { field ->
            val attacker = field.enemies.firstOrNull { it.fpid == actorFpid && it.fpid == field.currentActorFpid && !it.isBroken }
            val candidates = attacker?.let { field.selectableTargets(it.fpid, normalAttack) }.orEmpty()
            val target = attacker?.takeIf { candidates.isNotEmpty() }?.let { enemyTargetSelector.select(field, it, candidates) }
            val attack = if (attacker != null && target != null) {
                field.performAttack(attacker.fpid, target.fpid, normalAttack, random)
            } else {
                null
            }
            result = attack?.second
            attack?.first ?: field
        }
        return result
    }
}
