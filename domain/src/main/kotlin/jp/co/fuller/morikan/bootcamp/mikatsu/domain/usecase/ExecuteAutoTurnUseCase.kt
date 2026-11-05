package jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.EnemyTargetSelector
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.performAutoTurn
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AutoTurnResult
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.BattleRepository
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.SkillRepository
import kotlin.random.Random

/**
 * 現在の手番が「CPUが動かす手番」(敵、洗脳された個体、発狂した個体)のとき、その行動を実行するUseCase。
 *
 * 敵・洗脳は通常攻撃を、届く相手から[EnemyTargetSelector]で選んで行う。発狂は60%で何もせず、そうでなければ使えるスキルと相手をランダムに選ぶ
 * (`performAutoTurn`。詳細は`mental-ailments.md`)。ダメージ処理は味方の攻撃・スキル([AttackUseCase]・[UseSkillUseCase])と同じ共通関数を使う。
 * 手番の終了は呼び出し側が[EndTurnUseCase]で行う。
 *
 * 威力・発動回数・命中率は通常攻撃のスキルデータ([SkillRepository.getNormalAttack])から読む。乱数は注入された[Random]から引く。
 *
 * @property battleRepository 戦闘状態を保持するRepository。
 * @property skillRepository 通常攻撃のスキルデータを提供するRepository。
 * @property enemyTargetSelector 洗脳・敵の攻撃対象を選ぶ方法。
 * @property random 命中判定・発狂の判定などの乱数。
 */
class ExecuteAutoTurnUseCase(
    private val battleRepository: BattleRepository,
    private val skillRepository: SkillRepository,
    private val enemyTargetSelector: EnemyTargetSelector,
    private val random: Random,
) {

    /**
     * 指定した個体の手番の行動を実行する。
     *
     * 呼び出し側が演出(ナレーション)で示した個体と、実際に行動する個体が必ず一致するよう、行動者をFPIDで受け取り、
     * 現在の手番の個体でなければ何もしない。
     *
     * @param actorFpid 行動させる個体のFPID。
     * @return 行動の結果(攻撃した・スキルを使った・発狂で何もしない・届かない・何もできない)。[actorFpid]が現在の手番でない場合や、
     *   CPUが動かす個体でない(手動で操作する味方)・ブレイク中・行動不能の場合は`null`(何も変更しない)。
     */
    suspend operator fun invoke(actorFpid: Int): AutoTurnResult? {
        // updateBattleFieldのtransformは再実行され得るため、最後に計算した結果を外の変数に残す。
        val normalAttack = skillRepository.getNormalAttack()
        var result: AutoTurnResult? = null
        battleRepository.updateBattleField { field ->
            val turn = field.performAutoTurn(actorFpid, normalAttack, enemyTargetSelector, random)
            result = turn?.second
            turn?.first ?: field
        }
        return result
    }
}
