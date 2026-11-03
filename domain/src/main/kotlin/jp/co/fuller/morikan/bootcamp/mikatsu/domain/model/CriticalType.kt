package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model

/**
 * 攻撃の1ヒットがクリティカルだったかの種別。ナレーションで表示を分けるために[AttackShot]へ記録する。
 */
enum class CriticalType {
    /** クリティカルでない通常のヒット(外れたヒットも含む)。 */
    NONE,

    /** クリティカルヒット。ダメージが増える。 */
    CRITICAL,

    /** バッドクリティカル。倍率が下がり、クリティカルより弱い(通常より弱くなることもある)。 */
    BAD_CRITICAL,
}
