package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model

/**
 * 実行された攻撃1回分の結果。
 *
 * 画面が「誰が、誰に、どれだけのダメージを与えたか」をナレーションとして表示できるようにする
 * ことを目的とする。範囲攻撃では対象が複数になるため、対象ごとの結果を[hits]に持つ。
 * 個体はいずれもFPIDで指定する。
 *
 * @property attackerFpid 攻撃した個体のFPID。
 * @property hits 攻撃を受けた個体ごとの結果。1体以上で、攻撃を受けた順に並ぶ。
 */
data class AttackResult(
    val attackerFpid: Int,
    val hits: List<AttackHit>,
) {
    /** 全対象に与えたダメージの合計。 */
    val totalDamage: Int
        get() = hits.sumOf { it.damage }
}

/**
 * 攻撃を受けた1体分の結果。
 *
 * @property targetFpid 攻撃を受けた個体のFPID。
 * @property damage その個体に与えたダメージ量(全ヒットの合計)。
 */
data class AttackHit(
    val targetFpid: Int,
    val damage: Int,
)
