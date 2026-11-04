package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model

/**
 * 手番終了時に状態異常で起きた出来事1つ分。画面がナレーションと表示を連動させられるよう、
 * 各出来事の直後の戦闘フィールド([field])を持つ。個体はいずれもFPIDで指定する。
 */
sealed interface TurnEndEvent {

    /** この出来事の直後の戦闘フィールド(表示用スナップショット)。 */
    val field: BattleField

    /**
     * 状態異常によるHPのダメージ。
     *
     * @property fpid ダメージを受けた個体のFPID。
     * @property ailment ダメージの原因の状態異常。
     * @property damage 実際に減ったHP。
     */
    data class HpDamage(
        val fpid: Int,
        val ailment: StatusAilment,
        val damage: Int,
        override val field: BattleField,
    ) : TurnEndEvent

    /**
     * 状態異常によるMPの減少。
     *
     * @property fpid MPが減った個体のFPID。
     * @property ailment 原因の状態異常。
     * @property amount 実際に減ったMP。
     */
    data class MpLoss(
        val fpid: Int,
        val ailment: StatusAilment,
        val amount: Int,
        override val field: BattleField,
    ) : TurnEndEvent

    /**
     * 状態異常によるTPの減少。
     *
     * @property fpid TPが減った個体のFPID。
     * @property ailment 原因の状態異常。
     * @property amount 実際に減ったTP。
     */
    data class TpLoss(
        val fpid: Int,
        val ailment: StatusAilment,
        val amount: Int,
        override val field: BattleField,
    ) : TurnEndEvent

    /**
     * 感染の伝染。
     *
     * @property fromFpid 感染元の個体のFPID。
     * @property toFpid 新たに感染した個体のFPID。
     */
    data class Spread(
        val fromFpid: Int,
        val toFpid: Int,
        override val field: BattleField,
    ) : TurnEndEvent

    /**
     * 残りターンが0になって状態異常が解除された。
     *
     * @property fpid 解除された個体のFPID。
     * @property ailment 解除された状態異常。
     */
    data class Cured(
        val fpid: Int,
        val ailment: StatusAilment,
        override val field: BattleField,
    ) : TurnEndEvent

    /**
     * 状態異常のダメージで戦闘不能になった(状態異常は全解除済み)。
     *
     * @property fpid 戦闘不能になった個体のFPID。
     */
    data class Defeated(
        val fpid: Int,
        override val field: BattleField,
    ) : TurnEndEvent
}

/**
 * 手番を終えた結果。手番の終了で起きた、先制の手番の取り消しと状態異常の出来事を呼び出し側(画面)へ伝えるための型。
 *
 * @property cancelledPreemptive 取り消された先制の手番の個体のFPID(取り消した順)。
 * @property events 手番終了時の状態異常による出来事(起きた順)。各出来事が直後のフィールドを持つ。なければ空。
 */
data class EndTurnResult(
    val cancelledPreemptive: List<Int> = emptyList(),
    val events: List<TurnEndEvent> = emptyList(),
)
