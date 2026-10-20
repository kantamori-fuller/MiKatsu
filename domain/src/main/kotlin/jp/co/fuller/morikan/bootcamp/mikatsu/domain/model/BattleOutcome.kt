package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model

/**
 * 戦闘の勝敗を表す。決着がついていない間は、勝敗自体を`null`で表現する。
 */
enum class BattleOutcome {
    /** 敵が全員戦闘不能になった。 */
    Victory,

    /** 味方が全員戦闘不能になった。 */
    Defeat,
}
