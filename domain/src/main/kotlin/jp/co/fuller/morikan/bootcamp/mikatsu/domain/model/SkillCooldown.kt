package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model

/**
 * 個体が使ったスキル1つ分の、クールタイム(CT)の記録。
 *
 * CTは「使った後、本人の手番が[remainingTurns]回終わるまで使えない」を表す。使った手番の終わりは数えないため、
 * 使った手番の最中に作られた記録([startedDuringOwnTurn])は、その手番の終了ではフラグを下ろすだけで残りを減らさない
 * (バフ・デバフの[StatModifier.grantedDuringOwnTurn]と同じ考え方。これがないと、CT1のスキルが使った手番の終わりに明けてしまい、
 * 次の自分の手番にまた使えてしまう)。戦闘中だけの値で、次の戦闘へは持ち越さない。
 *
 * @property skillId CT中のアクティブスキルのID。
 * @property remainingTurns 残りの手番数。本人の手番が終わるたびに1減り、0になると記録が消えてスキルが使えるようになる。
 * @property startedDuringOwnTurn 記録が本人の手番の最中に作られたか。`true`の間は、その手番の終了で残りを減らさない。
 */
data class SkillCooldown(
    val skillId: Int,
    val remainingTurns: Int,
    val startedDuringOwnTurn: Boolean = false,
)
