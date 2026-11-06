package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AilmentState
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleAlly
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleFieldPawn
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleStat
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.StatusAilment
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.TurnEndEvent
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.applyDamage
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkill
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillCostType
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.updatePawn
import kotlin.random.Random

/**
 * 状態異常の定数・効果の計算・付与・解除・手番終了時の処理を集約した、状態異常の唯一の実装箇所。
 *
 * 状態異常は個体ごとに種類ごとに1つ([AilmentState])で、戦闘中だけの値。付与は[inflictAilment]、
 * 手番終了時のダメージ・伝染・解除は[processTurnEndAilments]が行う。ステータスや命中率などへの補正の数式もここに置き、
 * 攻撃・スキル・手番の処理に状態異常のルールをばらまかないことを目的とする。
 * 乱数は[Random]を引数で受け取り、テストで引く順序を固定できるようにする。
 */
object StatusAilmentFormula {

    /** 感染のダメージの分母の初期値(最大HPの1/16から始まる)。伝染先もこの値から始まる。 */
    const val BASE_INFECTION_DENOMINATOR = 16

    /** 感染のダメージの分母の最小値。ダメージのたびに[INFECTION_DENOMINATOR_STEP]ずつ減り、ここで止まる。 */
    const val MIN_INFECTION_DENOMINATOR = 2

    /** 感染のダメージのたびに分母から引く値。 */
    const val INFECTION_DENOMINATOR_STEP = 2

    /** 感染が手番終了時に同じ陣営へ伝染する確率(%)。 */
    const val INFECTION_SPREAD_PERCENT = 30

    /** 火傷の固定ダメージ(HP)。 */
    const val BURN_DAMAGE = 30

    /** 凍傷の固定ダメージ(HP)。 */
    const val FROSTBITE_DAMAGE = 10

    /** 汚染で失うTP。 */
    const val CONTAMINATION_TP_LOSS = 12

    /** 石化中の状態異常ダメージ。 */
    internal const val PETRIFIED_DAMAGE = 1

    /** 毒の割合ダメージの分母(最大HPの1/8)。 */
    internal const val POISON_DENOMINATOR = 8

    /** 流血・感電・呪怨(HP)の割合ダメージの分母(最大HPの1/16)。 */
    internal const val SIXTEENTH_DENOMINATOR = 16

    /** 汚染・呪怨(MP)の割合ダメージの分母(最大MPの1/8)。 */
    internal const val MP_DENOMINATOR = 8

    /** 恐怖が手番の開始時にスタンを引き起こす確率(%)。 */
    const val FEAR_STUN_PERCENT = 30

    /** 発狂が手番で何もしない確率(%)。 */
    const val MADNESS_IDLE_PERCENT = 60

    /** 精神系の状態異常(洗脳・発狂・魅了・恐怖・挑発)。enum順がそのまま強さの順(洗脳が最強)。 */
    private val MENTAL = setOf(
        StatusAilment.BRAINWASH,
        StatusAilment.MADNESS,
        StatusAilment.CHARM,
        StatusAilment.FEAR,
        StatusAilment.TAUNT,
    )

    /** 行動不能にする状態異常。ブレイクと違い、被ダメージ増加・被必中は付かない。 */
    private val INCAPACITATING = setOf(
        StatusAilment.STUN,
        StatusAilment.FREEZE,
        StatusAilment.SHOCK,
        StatusAilment.PETRIFY,
        StatusAilment.SLEEP,
    )

    /** ダメージを受けると解除される状態異常。 */
    private val RELEASED_BY_DAMAGE = setOf(StatusAilment.SLEEP, StatusAilment.FREEZE)

    /**
     * 行動不能にする状態異常のうち、enum順で先頭のものを返す。
     *
     * @param ailments かかっている状態異常。
     * @return 行動不能にする状態異常。なければ`null`。
     */
    fun incapacitatingAilmentOf(ailments: List<AilmentState>): StatusAilment? =
        ailments.map { it.ailment }.filter { it in INCAPACITATING }.minByOrNull { it.ordinal }

    /**
     * 状態異常による、ステータスの補正を適用する(段階補正後の値に対する、段階とは独立の補正)。
     *
     * 凍傷は破壊力を4/6に、麻痺は素早さを0.7倍にする(どちらも切り捨て)。±12の段階には数えない。
     *
     * @param stat 補正するステータス。
     * @param value 段階補正後の値。
     * @param ailments かかっている状態異常。
     * @return 補正後の値。
     */
    fun adjustStat(stat: BattleStat, value: Int, ailments: List<AilmentState>): Int = when {
        stat == BattleStat.ATTACK && ailments.any { it.ailment == StatusAilment.FROSTBITE } -> Math.floorDiv(value * 4, 6)
        stat == BattleStat.AGILITY && ailments.any { it.ailment == StatusAilment.PARALYSIS } -> Math.floorDiv(value * 7, 10)
        else -> value
    }

    /**
     * 麻痺中のブレイク耐性(被ゲージの倍率の数値)を0.7倍(切り捨て)にした値を求める。保存している耐性そのものは書き換えない。
     *
     * @param resistance 保存しているブレイク耐性。
     * @param ailments かかっている状態異常。
     * @return ブレイクゲージの計算に使う耐性。
     */
    fun effectiveBreakResistance(resistance: Int, ailments: List<AilmentState>): Int =
        if (ailments.any { it.ailment == StatusAilment.PARALYSIS }) resistance * 7 / 10 else resistance

    /**
     * 目眩の攻撃者の最終命中率の倍率。
     */
    const val DIZZINESS_HIT_RATE = 0.4

    /**
     * 凍結中の対象へのダメージを2倍(切り捨てだが整数なので正確に2倍)にする。
     *
     * @param damage 補正前のダメージ。
     * @param isFrozen 対象が凍結中か。
     * @return 補正後のダメージ。
     */
    fun boostForFreeze(damage: Int, isFrozen: Boolean): Int = if (isFrozen) damage * 2 else damage

    /**
     * 流血中の対象へのダメージを1.2倍(切り捨て)にする。
     *
     * @param damage 補正前のダメージ。
     * @param isBleeding 対象が流血中か。
     * @return 補正後のダメージ。
     */
    fun boostForBleed(damage: Int, isBleeding: Boolean): Int =
        if (isBleeding) (damage.toLong() * 12 / 10).coerceAtMost(Int.MAX_VALUE.toLong()).toInt() else damage

    /**
     * HP・MPの回復量を烙印で制限する。烙印中は、回復量が1以上なら1にする。
     *
     * @param amount 回復量。
     * @param isBranded 対象が烙印中か。
     * @return 制限後の回復量。
     */
    fun limitHealForBrand(amount: Int, isBranded: Boolean): Int = if (isBranded && amount >= 1) 1 else amount

    /**
     * 成功判定を行う。成功率100以上のときは乱数を引かない。それ以外は`random.nextInt(100) < 成功率`。
     *
     * @param successRate 成功率(0〜100)。
     * @param random 乱数。
     * @return 成功したら`true`。
     */
    fun rollSuccess(successRate: Int, random: Random): Boolean = successRate >= 100 || random.nextInt(100) < successRate

    /**
     * スキルがMPスキル封印・TPスキル封印で使えなくなっているかを返す。通常攻撃(コストなし)は封印されない。
     *
     * @param skill 使おうとするスキル。
     * @param ailments 使用者にかかっている状態異常。
     * @return コストにMP(MPスキル封印中)またはTP(TPスキル封印中)を含むなら`true`。
     */
    fun isSealed(skill: ActiveSkill, ailments: List<AilmentState>): Boolean =
        (ailments.any { it.ailment == StatusAilment.MP_SEAL } && skill.costOf(SkillCostType.MP) > 0) ||
            (ailments.any { it.ailment == StatusAilment.TP_SEAL } && skill.costOf(SkillCostType.TP) > 0)

    /**
     * 割合ダメージを求める。`floor(最大値 / 分母)`で、最低1。
     *
     * @param max 最大値(最大HPまたは最大MP)。
     * @param denominator 分母。
     * @return ダメージ量(1以上)。
     */
    fun fractionLoss(max: Int, denominator: Int): Int = (max / denominator).coerceAtLeast(1)

    /**
     * 新しく付与する状態異常が、かかっている状態異常と排他で付与できないかを返す。
     * 火傷は凍傷・凍結と、凍傷・凍結は火傷と排他(後からかけた方が失敗する)。凍傷と凍結は共存できる。
     *
     * @param ailment 付与しようとする状態異常。
     * @param current かかっている状態異常。
     * @return 排他で付与できないなら`true`。
     */
    fun isExcluded(ailment: StatusAilment, current: List<AilmentState>): Boolean {
        val has = { target: StatusAilment -> current.any { it.ailment == target } }
        return when (ailment) {
            StatusAilment.BURN -> has(StatusAilment.FROSTBITE) || has(StatusAilment.FREEZE)
            StatusAilment.FROSTBITE, StatusAilment.FREEZE -> has(StatusAilment.BURN)
            else -> false
        }
    }

    /**
     * 精神系の状態異常(洗脳・発狂・魅了・恐怖・挑発)かを返す。精神系どうしは強さで排他になる([isBlockedByStrongerMental])。
     *
     * @param ailment 調べる状態異常。
     * @return 精神系なら`true`。
     */
    fun isMental(ailment: StatusAilment): Boolean = ailment in MENTAL

    /**
     * 付与しようとする精神系の状態異常が、すでにかかっているより強い精神系のせいで付与できないかを返す。
     *
     * 強さは 洗脳 > 発狂 > 魅了 > 恐怖 > 挑発(enum順)。精神系でない状態異常は常に`false`。
     * 同じ種類は排他ではなく、残りターンの長い方で上書きする([inflictAilment])。
     *
     * @param ailment 付与しようとする状態異常。
     * @param current かかっている状態異常。
     * @return より強い精神系がかかっていて付与できないなら`true`。
     */
    fun isBlockedByStrongerMental(ailment: StatusAilment, current: List<AilmentState>): Boolean =
        isMental(ailment) && current.any { isMental(it.ailment) && it.ailment.ordinal < ailment.ordinal }

    /**
     * 解除の対象となる、ダメージで解除される状態異常(睡眠・凍結)かを返す。
     *
     * @param ailment 調べる状態異常。
     * @return ダメージを受けると解除されるなら`true`。
     */
    fun isReleasedByDamage(ailment: StatusAilment): Boolean = ailment in RELEASED_BY_DAMAGE

    /**
     * 感染のダメージのあと、次の分母を求める。2ずつ減り、最小2で止まる。
     *
     * @param denominator 今の分母。
     * @return 次の分母。
     */
    fun nextInfectionDenominator(denominator: Int): Int =
        (denominator - INFECTION_DENOMINATOR_STEP).coerceAtLeast(MIN_INFECTION_DENOMINATOR)
}

/** 手番終了時の損失の種類。 */
private enum class LossKind { HP, MP, TP }

/**
 * 状態異常1種類が手番終了時に与える損失(HP・MP・TP。順に処理する)を求める。
 *
 * @param pawn 損失を受ける個体。
 * @param state 状態異常の記録(感染の分母に使う)。
 * @return 損失の種類と量の列。損失のない状態異常は空。
 */
private fun lossesOf(pawn: BattleFieldPawn, state: AilmentState): List<Pair<LossKind, Int>> {
    val hp: (Int) -> Pair<LossKind, Int> = { amount ->
        LossKind.HP to if (pawn.hasAilment(StatusAilment.PETRIFY)) StatusAilmentFormula.PETRIFIED_DAMAGE else amount
    }
    return when (state.ailment) {
        StatusAilment.POISON -> listOf(hp(StatusAilmentFormula.fractionLoss(pawn.maxHp, StatusAilmentFormula.POISON_DENOMINATOR)))
        StatusAilment.BURN -> listOf(hp(StatusAilmentFormula.BURN_DAMAGE))
        StatusAilment.FROSTBITE -> listOf(hp(StatusAilmentFormula.FROSTBITE_DAMAGE))
        StatusAilment.INFECTION -> listOf(hp(StatusAilmentFormula.fractionLoss(pawn.maxHp, state.infectionDenominator)))
        StatusAilment.BLEED, StatusAilment.SHOCK -> listOf(hp(StatusAilmentFormula.fractionLoss(pawn.maxHp, StatusAilmentFormula.SIXTEENTH_DENOMINATOR)))
        StatusAilment.CURSE -> listOf(
            hp(StatusAilmentFormula.fractionLoss(pawn.maxHp, StatusAilmentFormula.SIXTEENTH_DENOMINATOR)),
            LossKind.MP to StatusAilmentFormula.fractionLoss(pawn.maxMp, StatusAilmentFormula.MP_DENOMINATOR),
        )
        StatusAilment.CONTAMINATION -> listOf(
            LossKind.MP to StatusAilmentFormula.fractionLoss(pawn.maxMp, StatusAilmentFormula.MP_DENOMINATOR),
            LossKind.TP to StatusAilmentFormula.CONTAMINATION_TP_LOSS,
        )
        else -> emptyList()
    }
}


/**
 * 2つの個体が同じ陣営かを返す。
 *
 * @param a 一方の個体。
 * @param b もう一方の個体。
 * @return ともに味方、またはともに敵なら`true`。
 */
private fun isSameSide(a: BattleFieldPawn, b: BattleFieldPawn): Boolean = (a is BattleAlly) == (b is BattleAlly)


/**
 * 個体の状態異常の一覧を変換した[BattleField]を返す。一覧はenum順に整える。
 *
 * @param fpid 対象の個体のFPID。
 * @param transform 状態異常の一覧の変換。
 * @return 変換後の戦闘フィールド。
 */
internal fun BattleField.updateAilments(fpid: Int, transform: (List<AilmentState>) -> List<AilmentState>): BattleField = updatePawn(
    fpid = fpid,
    transformAlly = { it.copy(ailments = transform(it.ailments).sortedBy { s -> s.ailment.ordinal }) },
    transformEnemy = { it.copy(ailments = transform(it.ailments).sortedBy { s -> s.ailment.ordinal }) },
)

/**
 * 個体に状態異常を付与した[BattleField]を返す。付与のルールはここに集約している。
 *
 * - 戦闘不能の個体・石化中の個体(石化以外)には付与できない(失敗)。
 * - 火傷と凍傷・火傷と凍結は排他で、既に片方がある個体に後からかけた方は失敗([isExcluded])。
 * - 精神系(洗脳・発狂・魅了・恐怖・挑発)どうしは強さで排他。より強い精神系がかかっていれば失敗([isBlockedByStrongerMental])、
 *   より弱い精神系がかかっていれば、それを(何も出さずに)消して付与する。同じ種類は下の「同じ状態異常」と同じ。
 * - 石化は付与した個体の他の状態異常をすべて解除する。
 * - 同じ状態異常が既にあれば、残りターンを長い方で上書きする(感染の分母はそのまま)。
 * - 対象本人の手番中([BattleField.currentActorFpid])に付与したものは[AilmentState.grantedDuringOwnTurn]を立てる。
 *
 * @receiver 付与前の戦闘フィールド。
 * @param fpid 対象の個体のFPID。
 * @param ailment 付与する状態異常。
 * @param durationTurns 持続ターン数。
 * @return 付与後の戦闘フィールドと、付与できたかの組。失敗のときはフィールドは変わらない。
 */
internal fun BattleField.inflictAilment(fpid: Int, ailment: StatusAilment, durationTurns: Int): Pair<BattleField, Boolean> {
    val pawn = allPawns.firstOrNull { it.fpid == fpid } ?: return this to false
    if (pawn.isDefeated) return this to false
    if (pawn.hasAilment(StatusAilment.PETRIFY) && ailment != StatusAilment.PETRIFY) return this to false
    if (StatusAilmentFormula.isExcluded(ailment, pawn.ailments)) return this to false
    if (StatusAilmentFormula.isBlockedByStrongerMental(ailment, pawn.ailments)) return this to false
    val granted = fpid == currentActorFpid
    val existing = pawn.ailments.firstOrNull { it.ailment == ailment }
    val fresh = AilmentState(ailment, durationTurns, grantedDuringOwnTurn = granted)
    val updated = this.updateAilments(fpid) { list ->
        val kept = when {
            ailment == StatusAilment.PETRIFY -> list.filter { it.ailment == StatusAilment.PETRIFY }
            // 自分より弱い精神系は消える(消えたことの文は出さない)。
            StatusAilmentFormula.isMental(ailment) ->
                list.filterNot { StatusAilmentFormula.isMental(it.ailment) && it.ailment.ordinal > ailment.ordinal }
            else -> list
        }
        when {
            existing == null -> kept + fresh
            existing.remainingTurns >= durationTurns -> kept
            else -> kept.map { if (it.ailment == ailment) it.copy(remainingTurns = durationTurns, grantedDuringOwnTurn = granted) else it }
        }
    }
    return updated to true
}

/**
 * 個体を即死させた(HPを0にして戦闘不能にし、状態異常を全解除した)[BattleField]を返す。石化中でも効く。
 *
 * @receiver 即死前の戦闘フィールド。
 * @param fpid 対象の個体のFPID。
 * @return 即死後の戦闘フィールド。
 */
internal fun BattleField.instantKill(fpid: Int): BattleField {
    val pawn = allPawns.firstOrNull { it.fpid == fpid } ?: return this
    return applyDamage(fpid, pawn.currentHp)
}

/**
 * ダメージでHPが実際に減った個体から、睡眠・凍結を解除する。戦闘不能の個体は[applyDamage]ですでに全解除済みのため何もしない。
 *
 * @receiver ダメージ適用後の戦闘フィールド。
 * @param fpid ダメージを受けた個体のFPID。
 * @return 解除後の戦闘フィールドと、解除された状態異常(enum順)の組。
 */
internal fun BattleField.releaseOnDamage(fpid: Int): Pair<BattleField, List<StatusAilment>> {
    val pawn = allPawns.firstOrNull { it.fpid == fpid } ?: return this to emptyList()
    if (pawn.isDefeated) return this to emptyList()
    val released = pawn.ailments.map { it.ailment }.filter { StatusAilmentFormula.isReleasedByDamage(it) }
    if (released.isEmpty()) return this to emptyList()
    return updateAilments(fpid) { list -> list.filterNot { StatusAilmentFormula.isReleasedByDamage(it.ailment) } } to released
}

/**
 * 手番を終えた個体の状態異常を、enum順に1つずつ処理する。
 *
 * 状態異常ごとに「ダメージ等 → (感染なら伝染判定) → 残りターンを1減らし0で解除」の順に行う。
 * - ダメージ等は[lossesOf]のとおり。HPのダメージは[applyDamage]で適用し、ブレイクゲージ・TP(攻撃を受けた条件)は貯めず、睡眠・凍結も解除しない。
 *   MP・TPの減少は実際に減った量が0なら出来事にしない。TPは0未満にならない。
 * - HPのダメージで戦闘不能になったら、状態異常は全解除され([applyDamage])、以降の処理を打ち切る。
 * - 感染は、ダメージのあとに、同じ陣営の生存・未感染・石化していない個体(候補)がいれば、30%で候補からランダムに1体へ伝染する
 *   (候補がいなければ乱数を引かない。伝染先は分母16、残りターンは伝染元の減算前の値。命中・耐性・成功判定はない)。
 *   乱数は伝染判定(`nextInt(100)`)→伝染先の選択(`nextInt(候補数)`)の順に引く。分母はダメージのたびに2ずつ減る(最小2)。
 * - 手番中にかけられた分([AilmentState.grantedDuringOwnTurn])は、ダメージも減算もせずフラグを下ろすだけ。
 * 各出来事の直後のフィールドを[TurnEndEvent]に持たせる。
 *
 * @receiver 手番終了の他の処理が済んだ戦闘フィールド。
 * @param fpid 手番を終えた個体のFPID。
 * @param random 感染の伝染に使う乱数。
 * @return 処理後の戦闘フィールドと、起きた出来事(起きた順)の組。
 */
internal fun BattleField.processTurnEndAilments(fpid: Int, random: Random): Pair<BattleField, List<TurnEndEvent>> {
    var field = this
    val events = mutableListOf<TurnEndEvent>()
    val order = (allPawns.firstOrNull { it.fpid == fpid } ?: return this to emptyList()).ailments.map { it.ailment }.sortedBy { it.ordinal }
    for (ailment in order) {
        val pawn = field.allPawns.firstOrNull { it.fpid == fpid } ?: break
        if (pawn.isDefeated) break
        val state = pawn.ailments.firstOrNull { it.ailment == ailment } ?: continue
        if (state.grantedDuringOwnTurn) {
            field = field.updateAilments(fpid) { list ->
                list.map { if (it.ailment == ailment) it.copy(grantedDuringOwnTurn = false) else it }
            }
            continue
        }
        for ((kind, amount) in lossesOf(pawn, state)) {
            val before = field.allPawns.first { it.fpid == fpid }
            when (kind) {
                LossKind.HP -> {
                    field = field.applyDamage(fpid, amount)
                    val after = field.allPawns.first { it.fpid == fpid }
                    events += TurnEndEvent.HpDamage(fpid, ailment, before.currentHp - after.currentHp, field)
                    if (after.isDefeated) {
                        events += TurnEndEvent.Defeated(fpid, field)
                        return field to events
                    }
                }
                LossKind.MP -> {
                    val lost = minOf(amount, before.currentMp)
                    field = field.updatePawn(
                        fpid = fpid,
                        transformAlly = { it.copy(currentMp = it.currentMp - lost) },
                        transformEnemy = { it.copy(currentMp = it.currentMp - lost) },
                    )
                    if (lost > 0) events += TurnEndEvent.MpLoss(fpid, ailment, lost, field)
                }
                LossKind.TP -> {
                    val lost = minOf(amount, before.currentTp)
                    field = field.gainTp(fpid, -lost)
                    if (lost > 0) events += TurnEndEvent.TpLoss(fpid, ailment, lost, field)
                }
            }
        }
        if (ailment == StatusAilment.INFECTION) {
            val self = field.allPawns.first { it.fpid == fpid }
            val candidates = field.allPawns.filter {
                isSameSide(it, self) && !it.isDefeated &&
                    !it.hasAilment(StatusAilment.INFECTION) && !it.hasAilment(StatusAilment.PETRIFY)
            }
            if (candidates.isNotEmpty() && random.nextInt(100) < StatusAilmentFormula.INFECTION_SPREAD_PERCENT) {
                val target = candidates[random.nextInt(candidates.size)]
                // うつる先の感染耐性で判定する。失敗したら伝染しない(引き直し・出来事なし)。
                if (StatusAilmentFormula.rollSuccess(target.ailmentResistanceTo(StatusAilment.INFECTION), random)) {
                    val spread = AilmentState(StatusAilment.INFECTION, state.remainingTurns, grantedDuringOwnTurn = false)
                    field = field.updateAilments(target.fpid) { it + spread }
                    events += TurnEndEvent.Spread(fpid, target.fpid, field)
                }
            }
        }
        val remaining = state.remainingTurns - 1
        field = field.updateAilments(fpid) { list ->
            list.mapNotNull {
                when {
                    it.ailment != ailment -> it
                    remaining <= 0 -> null
                    else -> it.copy(
                        remainingTurns = remaining,
                        infectionDenominator = if (ailment == StatusAilment.INFECTION) {
                            StatusAilmentFormula.nextInfectionDenominator(it.infectionDenominator)
                        } else {
                            it.infectionDenominator
                        },
                    )
                }
            }
        }
        // 恐怖によるスタンは、その手番だけの扱いなので「治った」の文は出さない。
        if (remaining <= 0 && !state.causedByFear) events += TurnEndEvent.Cured(fpid, ailment, field)
    }
    return field to events
}


/**
 * 恐怖([StatusAilment.FEAR])の個体が、自分の手番の開始時に30%でその手番だけスタンするかを判定し、スタンさせた[BattleField]を返す。
 *
 * 防御の解除・手番開始のTPの**後**に、[advanceToNextTurnWithCancellations]の両方の分岐から呼ぶ(乱数はTPの乱数の後に1回引く)。
 * 戦闘不能・ブレイク中・すでに状態異常で行動不能の個体、恐怖でない個体には判定せず、乱数も引かない。
 * スタンは残り1ターン・`grantedDuringOwnTurn = false`で付けるので、その手番の終了([processTurnEndAilments])で減って解除される
 * (手番中の付与として`true`にすると2手番動けなくなる)。恐怖由来だと分かるよう[AilmentState.causedByFear]を立てる。
 * 先制の手番でも判定する(先制の取り消しの判定はこれより前に済んでいる)。
 *
 * @receiver 手番開始のTPまで済んだ戦闘フィールド。
 * @param fpid 手番が来た個体のFPID。
 * @param random 判定の乱数(`nextInt(100) < 30`でスタン)。
 * @return 判定後の戦闘フィールド。スタンしなければ変わらない。
 */
internal fun BattleField.rollFearAtTurnStart(fpid: Int, random: Random): BattleField {
    val pawn = allPawns.firstOrNull { it.fpid == fpid } ?: return this
    if (!pawn.hasAilment(StatusAilment.FEAR) || pawn.isDefeated || pawn.isBroken || pawn.isIncapacitated) return this
    if (random.nextInt(100) >= StatusAilmentFormula.FEAR_STUN_PERCENT) return this
    val stun = AilmentState(StatusAilment.STUN, remainingTurns = 1, grantedDuringOwnTurn = false, causedByFear = true)
    return updateAilments(fpid) { it + stun }
}

/**
 * ブレイクゲージの計算に使う、麻痺を反映したブレイク耐性を返す。保存している[BattleFieldPawn.breakResistance]は書き換えない。
 *
 * @receiver 耐性を調べる個体。
 * @return 麻痺中は耐性の0.7倍(切り捨て)、それ以外は[BattleFieldPawn.breakResistance]。
 */
internal fun BattleFieldPawn.effectiveBreakResistance(): Int =
    StatusAilmentFormula.effectiveBreakResistance(breakResistance, ailments)
