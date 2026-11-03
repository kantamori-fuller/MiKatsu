package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model

/**
 * 攻撃の1ヒットがクリティカルだったかの種別(どちらの発生判定が出たかではなく、最終的なクリティカル倍率による分類)。ナレーションで表示を分けるために[AttackShot]へ記録する。
 */
enum class CriticalType {
    /** クリティカルでない通常のヒット(外れたヒットも含む)。 */
    NONE,

    /** クリティカル。発生判定が出て、最終的な倍率が100%以上(ちょうど100%を含む)。 */
    CRITICAL,

    /** バッドクリティカル。発生判定が出て、最終的な倍率が100%未満(通常より弱くなる)。 */
    BAD_CRITICAL,
}
