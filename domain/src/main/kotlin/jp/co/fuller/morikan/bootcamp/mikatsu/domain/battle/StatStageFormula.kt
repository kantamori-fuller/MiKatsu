package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleStat
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.StatModifier
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.updatePawn
import kotlin.math.abs

/**
 * ステータスの段階式バフ・デバフ(基礎ステータス6種)の計算と、個体への記録・経過を扱う唯一の実装箇所。
 *
 * 段階nの倍率は、n≥0なら`(4+n)/4`、n<0なら`4/(4+|n|)`。適用後の値は`floor(基礎値 × 分子 / 分母)`を
 * 整数演算で求め、浮動小数を使わない。段階は[MIN_STAGE]〜[MAX_STAGE]に収める。
 */
object StatStageFormula {

    /** 段階の下限。 */
    const val MIN_STAGE = -12

    /** 段階の上限。 */
    const val MAX_STAGE = 12

    /** 倍率の基準となる値(段階0で分子・分母がともにこの値になる)。 */
    private const val BASE = 4

    /**
     * 段階の倍率の分子を返す。
     *
     * @param stage 段階。範囲外は[MIN_STAGE]〜[MAX_STAGE]に収める。
     * @return 倍率の分子。
     */
    fun numerator(stage: Int): Int = stage.coerceIn(MIN_STAGE, MAX_STAGE).let { if (it >= 0) BASE + it else BASE }

    /**
     * 段階の倍率の分母を返す。
     *
     * @param stage 段階。範囲外は[MIN_STAGE]〜[MAX_STAGE]に収める。
     * @return 倍率の分母。
     */
    fun denominator(stage: Int): Int = stage.coerceIn(MIN_STAGE, MAX_STAGE).let { if (it >= 0) BASE else BASE + abs(it) }

    /**
     * 基礎値に段階の倍率を掛けた値を求める。
     *
     * @param base 基礎値。
     * @param stage 段階。範囲外は[MIN_STAGE]〜[MAX_STAGE]に収める。
     * @return `floor(基礎値 × 分子 / 分母)`。
     */
    fun apply(base: Int, stage: Int): Int = Math.floorDiv(base * numerator(stage), denominator(stage))

    /**
     * 記録されたエントリから、あるステータスの有効な段階を求める。
     *
     * そのステータスの全エントリの段階を合計し、[MIN_STAGE]〜[MAX_STAGE]に収める。
     * 記録そのものは上限を超えても残るため、古い分が切れても残りの合計から求め直される。
     *
     * @param modifiers 個体のエントリの一覧。
     * @param stat 求めるステータス。
     * @return 有効な段階。
     */
    fun effectiveStage(modifiers: List<StatModifier>, stat: BattleStat): Int =
        modifiers.filter { it.stat == stat }.sumOf { it.stages }.coerceIn(MIN_STAGE, MAX_STAGE)
}

/**
 * 個体へ、ステータスの段階式バフ・デバフ1回分を新しいエントリとして記録する。
 *
 * かけられたのが対象本人の手番中([BattleField.currentActorFpid]が対象)なら、その手番は持続に数えないよう
 * [StatModifier.grantedDuringOwnTurn]を立てる。
 *
 * @receiver 更新前の戦闘フィールド。
 * @param fpid 対象の個体のFPID。
 * @param stat 補正するステータス。
 * @param stages 段階の増減。
 * @param durationTurns 持続ターン数。
 * @return エントリを追加した戦闘フィールド。該当するFPIDの個体がなければ内容は変わらない。
 */
internal fun BattleField.addStatModifier(fpid: Int, stat: BattleStat, stages: Int, durationTurns: Int): BattleField {
    val modifier = StatModifier(
        stat = stat,
        stages = stages,
        remainingTurns = durationTurns,
        grantedDuringOwnTurn = fpid == currentActorFpid,
    )
    return updatePawn(
        fpid = fpid,
        transformAlly = { it.copy(statModifiers = it.statModifiers + modifier) },
        transformEnemy = { it.copy(statModifiers = it.statModifiers + modifier) },
    )
}

/**
 * 手番を終えた個体のバフ・デバフを1手番分進める。
 *
 * 手番中にかけられたエントリ([StatModifier.grantedDuringOwnTurn])はフラグを下ろすだけで残りターンを減らさない。
 * それ以外は残りターンを1減らし、0になったエントリを削除する。
 *
 * @receiver 更新前の戦闘フィールド。
 * @param fpid 手番を終えた個体のFPID。
 * @return 経過させた戦闘フィールド。
 */
internal fun BattleField.progressStatModifiers(fpid: Int): BattleField = updatePawn(
    fpid = fpid,
    transformAlly = { it.copy(statModifiers = it.statModifiers.afterOwnTurn()) },
    transformEnemy = { it.copy(statModifiers = it.statModifiers.afterOwnTurn()) },
)

/** 本人の手番終了後のエントリの一覧。 */
private fun List<StatModifier>.afterOwnTurn(): List<StatModifier> = mapNotNull { modifier ->
    if (modifier.grantedDuringOwnTurn) {
        modifier.copy(grantedDuringOwnTurn = false)
    } else {
        modifier.copy(remainingTurns = modifier.remainingTurns - 1).takeIf { it.remainingTurns > 0 }
    }
}
