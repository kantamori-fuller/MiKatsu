package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model

/**
 * 戦闘中の個体のブレイクの状態。状態の遷移は
 * [jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.BreakFormula]が唯一の実装箇所である。
 */
enum class BreakState {
    /** ブレイクしていない通常の状態。ブレイクゲージが貯まる。 */
    NONE,

    /** ブレイク状態。手番は飛ばされ、被ダメージが増え、攻撃が必ず当たる。ゲージは貯まらない。 */
    BROKEN,

    /** ブレイクが解けた直後の立ち上がり状態。通常どおり行動できるが、ゲージは貯まらない。 */
    RECOVERING,
}
