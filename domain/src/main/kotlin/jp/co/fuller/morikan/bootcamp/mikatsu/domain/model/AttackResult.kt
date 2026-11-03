package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model

/**
 * 実行された攻撃1回分の結果。
 *
 * 画面が「誰が、誰に、どれだけのダメージを与えたか(外れたか)」をナレーションとして表示できるようにする
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
 * 攻撃を受けた1体分の結果。ヒットごとの当たり外れを[shots]に持つ。
 *
 * @property targetFpid 攻撃を受けた個体のFPID。
 * @property shots 実際に行ったヒットごとの結果。行った順に並ぶ。対象が戦闘不能になって打ち切られた分は含まない。
 */
data class AttackHit(
    val targetFpid: Int,
    val shots: List<AttackShot>,
) {
    /** その個体に与えたダメージ量(全ヒットの合計)。 */
    val damage: Int
        get() = shots.sumOf { it.damage }

    /** 対象がこの攻撃のどこかのヒットでブレイクしたか。 */
    val causedBreak: Boolean
        get() = shots.any { it.causedBreak }

    /** 実際に行った全てのヒットが外れたか。 */
    val isMissed: Boolean
        get() = shots.none { it.isHit }
}

/**
 * 1ヒット分の結果。ナレーションで「何回目が当たった/外れた」を示すために用いる。
 *
 * @property isHit 命中したか。
 * @property damage そのヒットのダメージ。外れたヒットは0。
 * @property causedBreak このヒットで対象がブレイクしたか。
 */
data class AttackShot(
    val isHit: Boolean,
    val damage: Int,
    val causedBreak: Boolean = false,
)
