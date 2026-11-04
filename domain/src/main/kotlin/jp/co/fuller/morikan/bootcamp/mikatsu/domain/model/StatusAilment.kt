package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model

/**
 * 状態異常(ネガティブエフェクトの一種)の種類。ステータスの段階式バフ・デバフ([BattleStat])とは別の仕組みで、
 * 個体ごとに種類ごとに1つだけ持つ([AilmentState])。この宣言順が、扱い・表示・手番終了時の処理の順になる。
 * 効果の計算・付与・手番終了時の処理は`StatusAilmentFormula`が唯一の実装箇所。
 */
enum class StatusAilment {
    /** スタン。行動不能。 */
    STUN,

    /** 毒。手番終了時に最大HPの1/8のダメージ。 */
    POISON,

    /** 火傷。手番終了時に固定30のダメージ。凍傷・凍結とは排他。 */
    BURN,

    /** 凍傷。手番終了時に固定10のダメージ。破壊力が2/3になる。火傷とは排他。 */
    FROSTBITE,

    /** 凍結。行動不能。ダメージを受けると解除され、そのヒットは2倍。火傷とは排他。 */
    FREEZE,

    /** 感染。手番終了時に最大HPの1/分母のダメージ(分母は減っていく)で、同じ陣営へ伝染する。 */
    INFECTION,

    /** 流血。手番終了時に最大HPの1/16のダメージ。受けるダメージが1.2倍。 */
    BLEED,

    /** 麻痺。素早さとブレイク耐性の数値が0.7倍になる。 */
    PARALYSIS,

    /** 感電。行動不能。手番終了時に最大HPの1/16のダメージ。 */
    SHOCK,

    /** 石化。行動不能。受けるダメージが常に1。他の状態異常を打ち消し、他の状態異常を受け付けない。 */
    PETRIFY,

    /** 汚染。手番終了時に最大MPの1/8とTP12を失う。 */
    CONTAMINATION,

    /** 呪怨。手番終了時に最大HPの1/16と最大MPの1/8を失う。 */
    CURSE,

    /** 睡眠。行動不能。ダメージを受けると解除され、睡眠中の対象への攻撃は必ずクリティカル。 */
    SLEEP,

    /** MPスキル封印。コストにMPを含むスキルが使えない。 */
    MP_SEAL,

    /** TPスキル封印。コストにTPを含むスキルが使えない。 */
    TP_SEAL,

    /** 目眩。使用者の最終命中率が0.4倍になる。 */
    DIZZINESS,

    /** 烙印。HPの回復量が1以上なら1になる。 */
    BRAND,
}

/**
 * 個体にかかっている状態異常1種類分の記録。同じ状態異常は1エントリだけ持つ。
 * 戦闘中だけの値で、次の戦闘へは持ち越さず、戦闘不能になると全解除される。
 *
 * @property ailment 状態異常の種類。
 * @property remainingTurns 残りターン数。対象本人の手番終了時に1減り、0で解除される。
 * @property grantedDuringOwnTurn かけられたのが対象本人の手番中か。`true`の間はその手番の終了でダメージも減算も行わず、
 *   フラグを下ろすだけ(バフ・デバフの`StatModifier.grantedDuringOwnTurn`と同じ考え方)。
 * @property infectionDenominator 感染のときだけ使う、ダメージの分母(最大HPの1/分母)。16から始まり、ダメージのたびに2ずつ減って最小2で止まる。
 *   感染以外では使わない。
 */
data class AilmentState(
    val ailment: StatusAilment,
    val remainingTurns: Int,
    val grantedDuringOwnTurn: Boolean = false,
    val infectionDenominator: Int = 16,
)
