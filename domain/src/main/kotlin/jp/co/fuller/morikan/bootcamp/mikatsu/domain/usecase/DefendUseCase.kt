package jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.defend
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.BattleRepository

/**
 * 味方が防御し、次の自分の手番が始まるまで防御状態になるUseCase。
 *
 * 使用者はFPID(フィールドポーンID)で指定する。手番の終了は本UseCaseでは行わず、
 * 呼び出し側が[EndTurnUseCase]で行う。
 *
 * @property battleRepository 戦闘状態を保持するRepository。
 */
class DefendUseCase(
    private val battleRepository: BattleRepository,
) {

    /**
     * 指定した個体を防御状態にする。
     *
     * 使用者が現在の行動者ではない・味方ではない・戦闘不能・ブレイク中・挑発中の場合や、洗脳・発狂で自動で動く個体の場合
     * (プレイヤーは操作できない)、勝敗が決まっている場合は、何も変更せずに`false`を返す。
     *
     * @param actorFpid 防御する個体のFPID。
     * @return 防御できたら`true`。
     */
    suspend operator fun invoke(actorFpid: Int): Boolean {
        // updateBattleFieldのtransformは再実行され得るため、最後に計算した結果を外の変数に残す。
        var defended = false
        battleRepository.updateBattleField { field ->
            val isAuto = field.allPawns.firstOrNull { it.fpid == actorFpid }?.isAutoActing == true
            val next = if (isAuto) null else field.defend(actorFpid)
            defended = next != null
            next ?: field
        }
        return defended
    }
}
