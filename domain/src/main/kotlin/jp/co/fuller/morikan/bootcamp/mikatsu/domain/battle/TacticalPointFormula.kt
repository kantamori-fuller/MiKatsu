package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkill
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillCategory
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.updatePawn
import kotlin.random.Random

/**
 * TP(タクティカルポイント)の定数・獲得量の計算・フィールドへの加算を集約した唯一の実装箇所。
 *
 * TPは味方・敵の全個体が持つ0〜[MAX_TP]の整数で、100を超えた分は捨てる。
 * 獲得量は基本の範囲(両端を含む整数の一様乱数)から整数を引き、倍率をかけるときは引いた値に倍率をかけて切り捨てる。
 * 乱数を引く順序を固定してテストを決定的にするため、乱数は[Random]を引数で受け取る。
 * 計算と加算をここに集め、攻撃・スキル・手番の処理にTPのルールをばらまかないことを目的とする。
 */
object TacticalPointFormula {

    /** 最大TP。全個体共通。 */
    const val MAX_TP = 100

    /** 戦闘開始時に全個体が得るTP(固定)。 */
    const val BATTLE_START_TP = 10

    /** 防御コマンドで得るTP(固定)。 */
    const val DEFEND_TP = 5

    /** 自分の手番が始まったときに得るTPの範囲。 */
    val TURN_START_RANGE: IntRange = 6..8

    /** 攻撃技を当てたときに得るTPの範囲。 */
    val ATTACK_RANGE: IntRange = 8..12

    /** 攻撃技を当てた相手が全員防御中だったときに得るTPの範囲。 */
    val ATTACK_DEFENDING_RANGE: IntRange = 6..9

    /** サポート技(攻撃技以外)を当てたときに得るTPの範囲。 */
    val SUPPORT_RANGE: IntRange = 8..12

    /** 相手の技を避けたときに得るTPの範囲。 */
    val EVADE_RANGE: IntRange = 18..22

    /** 防御中の個体が攻撃を受けたとき、失ったHPの割合から得るTPにかける倍率(%)。 */
    private const val DEFENDING_DAMAGE_RATE_PERCENT = 150

    /** 倍率(%)を割合へ直すための値。 */
    private const val PERCENT = 100

    /**
     * 範囲から整数を1つ引く。両端を含む一様乱数。
     *
     * @param range 引く範囲。
     * @param random 乱数。
     * @return 範囲内の整数。
     */
    private fun draw(range: IntRange, random: Random): Int = range.first + random.nextInt(range.last - range.first + 1)

    /**
     * 自分の手番が始まったときに得るTPを引く。
     *
     * @param random 乱数。
     * @return 6〜8の整数。
     */
    fun drawTurnStart(random: Random): Int = draw(TURN_START_RANGE, random)

    /**
     * 技を当てた使用者が得るTPを引く(条件1・4)。
     *
     * 攻撃技([isAttackSkill])は、当てた相手が全員防御中([allHitsOnDefending])なら6〜9、そうでなければ8〜12を引き、
     * クリティカルが出ていれば([maxCriticalRate]が`null`でない)引いた値に倍率(%)をかけて切り捨てる。
     * 攻撃技以外は8〜12を引き、倍率はかけない。
     *
     * @param isAttackSkill 物理型攻撃または特殊型攻撃を含む技か。
     * @param allHitsOnDefending 命中したヒットがすべて、命中した時点で防御中の相手に対してだったか。
     * @param maxCriticalRate 命中したヒットのうち最終種別がクリティカルのものの最大の実際の倍率(%)。なければ`null`。
     * @param random 乱数。
     * @return 獲得するTP。
     */
    fun drawSkillUse(isAttackSkill: Boolean, allHitsOnDefending: Boolean, maxCriticalRate: Int?, random: Random): Int {
        if (!isAttackSkill) return draw(SUPPORT_RANGE, random)
        val base = draw(if (allHitsOnDefending) ATTACK_DEFENDING_RANGE else ATTACK_RANGE, random)
        return if (maxCriticalRate == null) base else (base.toLong() * maxCriticalRate / PERCENT).toInt()
    }

    /**
     * 技を避けたときに得るTPを引く(条件6)。
     *
     * @param random 乱数。
     * @return 18〜22の整数。
     */
    fun drawEvade(random: Random): Int = draw(EVADE_RANGE, random)

    /**
     * 攻撃を受けた1ヒット分の、受けた側が得るTP(小数)を求める(条件2)。乱数は使わない。
     *
     * `実際に減ったHP ÷ 最大HP × 100`に、防御中なら×1.5、クリティカルなら×(倍率/100)をかける。
     * 対象ごとの合計を攻撃の最後に切り捨てて加算する。
     *
     * @param hpLoss そのヒットで実際に減ったHP。
     * @param maxHp 受けた側の最大HP。
     * @param isDefending そのヒットの時点で防御中だったか。
     * @param criticalRate そのヒットが最終種別クリティカルなら攻撃者の実際の倍率(%)、そうでなければ`null`。
     * @return そのヒットで得るTP(切り捨て前)。
     */
    fun damageTaken(hpLoss: Int, maxHp: Int, isDefending: Boolean, criticalRate: Int?): Double {
        if (maxHp <= 0) return 0.0
        var tp = hpLoss.toDouble() / maxHp * PERCENT
        if (isDefending) tp = tp * DEFENDING_DAMAGE_RATE_PERCENT / PERCENT
        if (criticalRate != null) tp = tp * criticalRate / PERCENT
        return tp
    }

    /**
     * スキルが攻撃技(物理型攻撃または特殊型攻撃を含む)か。通常攻撃を含む。
     *
     * @param skill 調べるスキル。
     * @return 攻撃技なら`true`。
     */
    fun isAttackSkill(skill: ActiveSkill): Boolean =
        SkillCategory.PHYSICAL_ATTACK in skill.categories || SkillCategory.SPECIAL_ATTACK in skill.categories
}

/**
 * 1回の技で、使用者が技を当てた結果。使用者のTP(条件1・4)の計算に使う。
 *
 * @property isHit 1体以上に1発以上命中したか。`false`なら使用者はTPを得ない。
 * @property allHitsOnDefending 命中したヒットがすべて、命中した時点で防御中の相手に対してだったか。
 * @property maxCriticalRate 命中したヒットのうち最終種別がクリティカルのものの最大の実際の倍率(%)。なければ`null`。
 */
internal data class TpUserReport(
    val isHit: Boolean,
    val allHitsOnDefending: Boolean = false,
    val maxCriticalRate: Int? = null,
)

/**
 * 1回の技で、対象1体が受けた結果。対象のTP(条件2・6)の計算に使う。
 *
 * @property fpid 対象のFPID。
 * @property tpFromDamage 受けたヒットごとの[TacticalPointFormula.damageTaken]の合計(切り捨て前)。
 * @property evaded 1発以上避けたか。
 */
internal data class TpTargetReport(
    val fpid: Int,
    val tpFromDamage: Double = 0.0,
    val evaded: Boolean = false,
)

/**
 * 指定した個体のTPを加算した[BattleField]を返す。結果は0〜[TacticalPointFormula.MAX_TP]に収め、超えた分は捨てる。
 *
 * @receiver 加算前の戦闘フィールド。
 * @param fpid TPを得る個体のFPID。
 * @param amount 加算量。
 * @return 加算後の戦闘フィールド。該当する個体がなければ変わらない。
 */
fun BattleField.gainTp(fpid: Int, amount: Int): BattleField = updatePawn(
    fpid = fpid,
    transformAlly = { it.copy(currentTp = (it.currentTp + amount).coerceIn(0, it.maxTp)) },
    transformEnemy = { it.copy(currentTp = (it.currentTp + amount).coerceIn(0, it.maxTp)) },
)

/**
 * 全個体(味方・敵)に戦闘開始のTP([TacticalPointFormula.BATTLE_START_TP])を加えた[BattleField]を返す。
 *
 * @receiver 戦闘開始時に組み立てたフィールド。
 * @return TPを加算したフィールド。
 */
fun BattleField.grantBattleStartTp(): BattleField =
    allPawns.fold(this) { field, pawn -> field.gainTp(pawn.fpid, TacticalPointFormula.BATTLE_START_TP) }

/**
 * 自分の手番が始まった個体に、手番開始のTPを加えた[BattleField]を返す。
 *
 * @receiver 加算前の戦闘フィールド。
 * @param fpid 手番が始まった個体のFPID。
 * @param random 乱数。
 * @return TPを加算したフィールド。
 */
internal fun BattleField.gainTurnStartTp(fpid: Int, random: Random): BattleField =
    gainTp(fpid, TacticalPointFormula.drawTurnStart(random))

/**
 * 1回の技の処理がすべて終わった後で、使用者と対象のTPをまとめて加算する。
 *
 * 乱数を引く順序は、まず使用者(命中していれば条件1・4)、次に[targets]の順に各対象(条件2は乱数なし、避けていれば条件6)。
 * 成立しない条件では乱数を引かない。
 *
 * @receiver 技の処理が終わった時点の戦闘フィールド。
 * @param actorFpid 技の使用者のFPID。
 * @param skill 使った技。攻撃技かどうかの判定に使う。
 * @param user 使用者の結果。
 * @param targets 対象ごとの結果。[resolveTargets]の順に並べる。
 * @param random 乱数。
 * @return TPを加算したフィールド。
 */
internal fun BattleField.settleTp(
    actorFpid: Int,
    skill: ActiveSkill,
    user: TpUserReport,
    targets: List<TpTargetReport>,
    random: Random,
): BattleField {
    var field = this
    if (user.isHit) {
        val gain = TacticalPointFormula.drawSkillUse(
            isAttackSkill = TacticalPointFormula.isAttackSkill(skill),
            allHitsOnDefending = user.allHitsOnDefending,
            maxCriticalRate = user.maxCriticalRate,
            random = random,
        )
        field = field.gainTp(actorFpid, gain)
    }
    targets.forEach { target ->
        field = field.gainTp(target.fpid, target.tpFromDamage.toInt())
        if (target.evaded) field = field.gainTp(target.fpid, TacticalPointFormula.drawEvade(random))
    }
    return field
}
