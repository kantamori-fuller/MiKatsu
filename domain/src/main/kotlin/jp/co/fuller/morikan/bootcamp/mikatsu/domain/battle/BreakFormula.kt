package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleFieldPawn
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BreakState
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.updatePawn

/**
 * ブレイク(ブレイクゲージ・ブレイク耐性・ブレイク状態)に関する定数と計算式、状態の変化を集約した置き場。
 *
 * ブレイクの計算・状態の変化をここ1箇所に集め、攻撃処理や手番の処理に処理をばらまかないことを目的とする。
 * 攻撃でダメージを与えるたびにゲージが貯まり([BattleField.accumulateBreakGauge])、100に達すると
 * [BreakState.BROKEN]になる。本人の手番が終わるたびに状態が1段階進み([BattleField.progressBreakState])、
 * BROKEN → RECOVERING(立ち上がり) → NONE と戻る。
 */
object BreakFormula {

    /** ブレイクゲージの最大値。これに達するとブレイクする。 */
    const val MAX_GAUGE = 100.0

    /** ブレイク耐性の基本値。 */
    const val BASE_RESISTANCE = 100

    /** ブレイク耐性の下限。 */
    const val MIN_RESISTANCE = 0

    /** ブレイク耐性の上限。 */
    const val MAX_RESISTANCE = 999

    /** 立ち上がり状態が解けたときに上がる耐性(ブレイク耐性の数値は、この値だけ減る)。 */
    const val RESISTANCE_GAIN_ON_RECOVERED = 20

    /** ブレイクポイント倍率の基本値(%)。 */
    const val BASE_BREAK_POINT_RATE = 150

    /** ブレイク中に受けるダメージの倍率(%)。 */
    const val BROKEN_DAMAGE_RATE_PERCENT = 130

    /** 倍率(%)を割合へ直すための値。 */
    private const val PERCENT = 100.0

    /**
     * 1ヒットで増えるブレイクゲージの量を求める。
     *
     * `実際に減ったHP ÷ 対象の最大HP × 100 × (攻撃者のブレイクポイント倍率 / 100) × (対象のブレイク耐性 / 100)`。
     * 端数は丸めず小数のまま返す。
     *
     * @param actualHpLoss そのヒットで実際に減ったHP(オーバーキル分を含まない)。
     * @param targetMaxHp 対象の最大HP。0以下のときは1として扱う(0除算を避ける)。
     * @param attackerBreakPointRate 攻撃者のブレイクポイント倍率(%)。
     * @param targetBreakResistance 対象のブレイク耐性(被ゲージの倍率。%)。
     * @return 増えるゲージの量。
     */
    fun gaugeIncrease(
        actualHpLoss: Int,
        targetMaxHp: Int,
        attackerBreakPointRate: Int,
        targetBreakResistance: Int,
    ): Double =
        actualHpLoss.coerceAtLeast(0).toDouble() / targetMaxHp.coerceAtLeast(1) * MAX_GAUGE *
            (attackerBreakPointRate / PERCENT) * (targetBreakResistance / PERCENT)

    /**
     * ブレイク中の個体が受けるダメージの増加を適用する。
     *
     * ダメージ計算式の最終結果に×1.3を掛けて切り捨てる。ブレイク中でなければそのまま返す。
     * パッシブスキルによる軽減は、この後に適用する。
     *
     * @param damage [DamageFormula]で求めた最終ダメージ。
     * @param isTargetBroken 対象がブレイク中か。
     * @return 増加後のダメージ。
     */
    fun boostDamage(damage: Int, isTargetBroken: Boolean): Int =
        if (isTargetBroken) {
            (damage.toLong() * BROKEN_DAMAGE_RATE_PERCENT / 100).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        } else {
            damage
        }
}

/**
 * 攻撃の1ヒットでダメージを与えた後の対象に、ブレイクゲージを加算する。
 *
 * ダメージの適用後の戦闘フィールドに対して呼ぶ。対象が戦闘不能になった・ブレイク中・立ち上がり中のときは何もしない。
 * 加算後に[BreakFormula.MAX_GAUGE]以上になったら、ゲージを0に戻して(超過分は捨てる)[BreakState.BROKEN]にし、防御状態も解除する。
 * 貯まるのは攻撃によるダメージだけで、HPコストの支払いや回復では呼ばない。
 * 計算に使う対象のブレイク耐性は、麻痺中なら0.7倍(切り捨て)にした値([effectiveBreakResistance])で、保存している耐性自体は書き換えない。
 *
 * @receiver ダメージ適用後の戦闘フィールド。
 * @param attackerBreakPointRate 攻撃者のブレイクポイント倍率。
 * @param targetFpid ダメージを受けた個体のFPID。
 * @param hpLoss そのヒットで実際に減ったHP。
 * @return ゲージを加算した戦闘フィールドと、このヒットで対象がブレイクしたかの組。
 */
internal fun BattleField.accumulateBreakGauge(
    attackerBreakPointRate: Int,
    targetFpid: Int,
    hpLoss: Int,
): Pair<BattleField, Boolean> {
    val target = allPawns.firstOrNull { it.fpid == targetFpid } ?: return this to false
    if (target.isDefeated || target.breakState != BreakState.NONE) return this to false
    val gauge = target.breakGauge + BreakFormula.gaugeIncrease(
        actualHpLoss = hpLoss,
        targetMaxHp = target.maxHp,
        attackerBreakPointRate = attackerBreakPointRate,
        targetBreakResistance = target.effectiveBreakResistance(),
    )
    val broke = gauge >= BreakFormula.MAX_GAUGE
    val newGauge = if (broke) 0.0 else gauge
    val newState = if (broke) BreakState.BROKEN else BreakState.NONE
    val updated = updatePawn(
        fpid = targetFpid,
        transformAlly = { it.copy(breakGauge = newGauge, breakState = newState, isDefending = it.isDefending && !broke) },
        transformEnemy = { it.copy(breakGauge = newGauge, breakState = newState, isDefending = it.isDefending && !broke) },
    )
    return updated to broke
}

/**
 * 手番を終えた個体のブレイクの状態を1段階進める。
 *
 * [BreakState.BROKEN]なら[BreakState.RECOVERING](立ち上がり)へ、[BreakState.RECOVERING]なら[BreakState.NONE]へ戻し、
 * このときブレイク耐性を[BreakFormula.RESISTANCE_GAIN_ON_RECOVERED]だけ上げる(数値を減らす。下限は
 * [BreakFormula.MIN_RESISTANCE])。[BreakState.NONE]なら何もしない。
 *
 * @receiver 更新前の戦闘フィールド。
 * @param fpid 手番を終えた個体のFPID。
 * @return 状態を進めた戦闘フィールド。
 */
internal fun BattleField.progressBreakState(fpid: Int): BattleField = updatePawn(
    fpid = fpid,
    transformAlly = { it.copy(breakState = it.nextBreakState(), breakResistance = it.nextBreakResistance()) },
    transformEnemy = { it.copy(breakState = it.nextBreakState(), breakResistance = it.nextBreakResistance()) },
)

/** 手番終了後のブレイクの状態。 */
private fun BattleFieldPawn.nextBreakState(): BreakState = when (breakState) {
    BreakState.NONE -> BreakState.NONE
    BreakState.BROKEN -> BreakState.RECOVERING
    BreakState.RECOVERING -> BreakState.NONE
}

/** 手番終了後のブレイク耐性。立ち上がりが解けるときだけ、数値が減る。 */
private fun BattleFieldPawn.nextBreakResistance(): Int =
    if (breakState == BreakState.RECOVERING) {
        (breakResistance - BreakFormula.RESISTANCE_GAIN_ON_RECOVERED).coerceAtLeast(BreakFormula.MIN_RESISTANCE)
    } else {
        breakResistance
    }
