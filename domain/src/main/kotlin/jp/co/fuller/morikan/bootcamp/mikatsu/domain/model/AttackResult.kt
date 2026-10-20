package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model

/**
 * 実行された攻撃1回分の結果。
 *
 * 画面が「誰が、誰に、どれだけのダメージを与えたか」をナレーションとして表示できるようにする
 * ことを目的とする。個体はいずれもFPIDで指定する。
 *
 * @property attackerFpid 攻撃した個体のFPID。
 * @property targetFpid 攻撃を受けた個体のFPID。
 * @property damage 与えたダメージ量。
 */
data class AttackResult(
    val attackerFpid: Int,
    val targetFpid: Int,
    val damage: Int,
)
