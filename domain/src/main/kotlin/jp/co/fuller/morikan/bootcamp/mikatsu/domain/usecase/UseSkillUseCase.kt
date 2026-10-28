package jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.performSkill
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.SkillResult
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.BattleRepository

/**
 * 味方のアクティブスキルを使い、コスト(MP)の消費と効果の適用を行うUseCase。
 *
 * 使用者・対象は、同じキャラクター・同じ敵種が複数体登場しても一意に特定できるよう、
 * 必ずFPID(フィールドポーンID)で指定する。手番の終了は本UseCaseでは行わず、
 * 呼び出し側が[EndTurnUseCase]で行う。
 *
 * @property battleRepository 戦闘状態を保持するRepository。
 */
class UseSkillUseCase(private val battleRepository: BattleRepository) {

    /**
     * 使用者から対象へスキルを使う。
     *
     * 使用者が現在の行動者ではない場合、スキルを持っていない場合、MPが足りない場合、
     * 対象がスキルの対象指定に合わない場合は、何も変更せずに`null`を返す。
     *
     * @param actorFpid スキルを使う個体のFPID。
     * @param skillId 使うアクティブスキルのID(アクティブ採番のマスターデータのID)。
     * @param targetFpid 対象の個体のFPID。
     * @return スキルの結果。使えなかった場合は`null`。
     */
    suspend operator fun invoke(actorFpid: Int, skillId: Int, targetFpid: Int): SkillResult? {
        // updateBattleFieldのtransformは再実行され得るため、最後に計算した結果を外の変数に残す。
        var result: SkillResult? = null
        battleRepository.updateBattleField { field ->
            val use = field.performSkill(actorFpid, skillId, targetFpid)
            result = use?.second
            use?.first ?: field
        }
        return result
    }
}
