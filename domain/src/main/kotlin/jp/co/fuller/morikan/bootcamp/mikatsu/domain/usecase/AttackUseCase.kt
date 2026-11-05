package jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.performAttack
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AttackResult
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.BattleRepository
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.SkillRepository
import kotlin.random.Random

/**
 * 味方の通常攻撃を実行し、対象の現在HPを減らすUseCase。
 *
 * 攻撃者・対象は、同じキャラクター・同じ敵種が複数体登場しても一意に特定できるよう、
 * 必ずFPID(フィールドポーンID)で指定する。手番の終了は本UseCaseでは行わず、
 * 呼び出し側が[EndTurnUseCase]で行う。
 *
 * 威力・発動回数・レンジ・範囲・対象選択は通常攻撃のスキルデータ([SkillRepository.getNormalAttack])から読む。
 *
 * @property battleRepository 戦闘状態を保持するRepository。
 * @property skillRepository 通常攻撃のスキルデータを提供するRepository。
 * @property random 命中判定の乱数。
 */
class AttackUseCase(
    private val battleRepository: BattleRepository,
    private val skillRepository: SkillRepository,
    private val random: Random,
) {

    /**
     * 攻撃者から対象へ通常攻撃を行い、ダメージを適用する。
     *
     * 攻撃者が現在の行動者ではない場合、攻撃者か対象が見つからない場合、攻撃者か対象が
     * 戦闘不能の場合、対象がレンジ外・陣営違いで選べない場合、攻撃者が洗脳・発狂で自動で動く個体の場合
     * (プレイヤーは操作できない。自動行動は[ExecuteAutoTurnUseCase]が行う)は、何も変更せずに`null`を返す。
     *
     * @param attackerFpid 攻撃者のFPID。
     * @param targetFpid 選んだ攻撃対象(アンカー)のFPID。
     * @return 攻撃の結果。攻撃できなかった場合は`null`。
     */
    suspend operator fun invoke(attackerFpid: Int, targetFpid: Int): AttackResult? {
        // updateBattleFieldのtransformは再実行され得るため、最後に計算した結果を外の変数に残す。
        val normalAttack = skillRepository.getNormalAttack()
        var result: AttackResult? = null
        battleRepository.updateBattleField { field ->
            val isAuto = field.allPawns.firstOrNull { it.fpid == attackerFpid }?.isAutoActing == true
            val attack = if (isAuto) null else field.performAttack(attackerFpid, targetFpid, normalAttack, random)
            result = attack?.second
            attack?.first ?: field
        }
        return result
    }
}
