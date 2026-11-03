package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleAlly
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.CriticalType
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.updatePawn

/**
 * 防御状態([jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleFieldPawn.isDefending])の効果の計算と、状態の変化を集約した唯一の実装箇所。
 *
 * 防御中の個体が攻撃を受けると、ダメージが30%カットされ([reduceDamage])、クリティカルが無効になる([blocksCritical])。
 * 防御は「次の自分の手番が始まる」([BattleField.advanceToNextTurn])か「ブレイクする」([accumulateBreakGauge])と解除される。
 * 計算・状態の変化をここに集め、攻撃処理や手番の処理に処理をばらまかないことを目的とする。
 */
object DefenseFormula {

    /** 防御中の個体が受けるダメージの割合(%)。30%カットなので70。 */
    const val DAMAGE_RATE_PERCENT = 70

    /**
     * 防御によるダメージ軽減を適用する。
     *
     * 防御中なら`max(floor(ダメージ × 70 / 100), 1)`(整数演算)。防御中でなければそのまま返す。
     * ブレイク中の被ダメージ増加([BreakFormula.boostDamage])の後、パッシブスキルの軽減([reduceDamage])の前に適用する。
     *
     * @param damage 防御を適用する前のダメージ。
     * @param isTargetDefending 対象が防御中か。
     * @return 軽減後のダメージ。
     */
    fun reduceDamage(damage: Int, isTargetDefending: Boolean): Int =
        if (isTargetDefending) {
            (damage.toLong() * DAMAGE_RATE_PERCENT / 100).coerceIn(1L, Int.MAX_VALUE.toLong()).toInt()
        } else {
            damage
        }

    /**
     * 防御によって、このヒットのクリティカルが無効になるかを判定する。
     *
     * 防御中で、ヒットの最終的な種別が[CriticalType.CRITICAL](最終倍率100%以上。ちょうど100%を含む)のときだけ無効になる。
     * [CriticalType.BAD_CRITICAL](100%未満)はダメージが減るだけなので、防御中でもそのまま発生する。
     *
     * @param isTargetDefending 対象が防御中か。
     * @param finalType [CriticalFormula.finalType]で求めたヒットの最終的な種別。
     * @return クリティカルが無効になるなら`true`。
     */
    fun blocksCritical(isTargetDefending: Boolean, finalType: CriticalType): Boolean =
        isTargetDefending && finalType == CriticalType.CRITICAL
}

/**
 * 現在の行動者の味方が防御し、次の自分の手番が始まるまで防御状態になった[BattleField]を返す。
 *
 * 使用者が現在の行動者ではない、味方ではない、戦闘不能である、ブレイク中で行動できない、
 * または勝敗が決まっている場合は防御できないため`null`を返す。
 * 手番の終了はここでは行わず、呼び出し側が[endTurn]で行う。使用者はFPIDで指定する。
 *
 * @receiver 防御前の戦闘フィールド。
 * @param actorFpid 防御する個体のFPID。
 * @return 防御状態にした戦闘フィールド。防御できない場合は`null`。
 */
fun BattleField.defend(actorFpid: Int): BattleField? {
    if (outcome != null) return null
    if (actorFpid != currentActorFpid) return null
    val actor: BattleAlly = allies.firstOrNull { it.fpid == actorFpid } ?: return null
    if (actor.isDefeated || actor.isBroken) return null
    return updatePawn(
        fpid = actorFpid,
        transformAlly = { it.copy(isDefending = true) },
        transformEnemy = { it.copy(isDefending = true) },
    )
}

/**
 * 指定した個体の防御状態を解除する。防御していなければ何も変わらない。
 *
 * @receiver 更新前の戦闘フィールド。
 * @param fpid 解除する個体のFPID。`null`なら何もしない。
 * @return 解除後の戦闘フィールド。
 */
internal fun BattleField.releaseDefense(fpid: Int?): BattleField {
    if (fpid == null) return this
    return updatePawn(
        fpid = fpid,
        transformAlly = { it.copy(isDefending = false) },
        transformEnemy = { it.copy(isDefending = false) },
    )
}
