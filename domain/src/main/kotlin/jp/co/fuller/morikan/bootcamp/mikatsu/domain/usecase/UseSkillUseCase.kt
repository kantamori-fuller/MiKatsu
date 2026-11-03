package jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.performActiveSkill
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.SkillUseResult
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.BattleRepository
import kotlin.random.Random

/**
 * 味方のアクティブスキルを使い、コスト(MP・HP・TP)の消費と効果・ダメージの適用を行うUseCase。
 *
 * ダメージを与えるスキルは攻撃の結果([SkillUseResult.Attack])、与えないスキルはスキルの結果([SkillUseResult.Effect])を返す。
 *
 * 使用者・対象は、同じキャラクター・同じ敵種が複数体登場しても一意に特定できるよう、
 * 必ずFPID(フィールドポーンID)で指定する。手番の終了は本UseCaseでは行わず、
 * 呼び出し側が[EndTurnUseCase]で行う。
 *
 * @property battleRepository 戦闘状態を保持するRepository。
 * @property random 命中率のあるスキルの命中判定に使う乱数。
 */
class UseSkillUseCase(
    private val battleRepository: BattleRepository,
    private val random: Random,
) {

    /**
     * 使用者から対象へスキルを使う。
     *
     * 使用者が現在の行動者ではない場合、スキルを持っていない場合、コストが足りない場合、
     * 対象がレンジ外・陣営違い・戦闘不能などで選べない場合は、何も変更せずに`null`を返す。
     * 命中率のあるスキルが外れた場合は、コストだけ消費して成功として結果を返す。
     *
     * @param actorFpid スキルを使う個体のFPID。
     * @param skillId 使うアクティブスキルのID(アクティブ採番のマスターデータのID)。
     * @param targetFpid 選んだ対象(アンカー)のFPID。範囲スキルでは、この個体を基準に対象が広がる。
     * @return スキルの結果(ダメージの有無で種類が変わる)。使えなかった場合は`null`。
     */
    suspend operator fun invoke(actorFpid: Int, skillId: Int, targetFpid: Int): SkillUseResult? {
        // updateBattleFieldのtransformは再実行され得るため、最後に計算した結果を外の変数に残す。
        var result: SkillUseResult? = null
        battleRepository.updateBattleField { field ->
            val use = field.performActiveSkill(actorFpid, skillId, targetFpid, random)
            result = use?.second
            use?.first ?: field
        }
        return result
    }
}
