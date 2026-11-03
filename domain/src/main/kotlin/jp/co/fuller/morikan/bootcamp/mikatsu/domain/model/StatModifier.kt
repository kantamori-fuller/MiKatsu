package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model

/**
 * 個体にかけられた、ステータスの段階式バフ・デバフ「1回分」の記録。
 *
 * 重ねがけは1回ごとに別のエントリとして持ち、それぞれが別々の残りターンで切れる。
 * 戦闘中だけの値で、次の戦闘へは持ち越さない。
 *
 * @property stat 補正するステータス。
 * @property stages 段階の増減。バフは正、デバフは負。
 * @property remainingTurns 残りターン数。対象**本人**の手番終了時に1減り、0になるとエントリが消える。`null`は永続で、減らず、その戦闘の終わりまで消えない。
 * @property grantedDuringOwnTurn かけられたのが対象本人の手番中か。`true`の間はその手番の終了で`remainingTurns`を減らさず、
 *   フラグだけを下ろす(自分にかけた手番を数えない)。他人の手番中にかけられた分は`false`で、本人の次の手番終了から数える。
 */
data class StatModifier(
    val stat: BattleStat,
    val stages: Int,
    val remainingTurns: Int?,
    val grantedDuringOwnTurn: Boolean = false,
)
