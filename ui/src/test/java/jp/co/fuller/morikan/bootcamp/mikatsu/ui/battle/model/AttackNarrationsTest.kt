package jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AttackHit
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AttackResult
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AttackShot
import org.junit.Assert.assertEquals
import org.junit.Test

/** [toNarrations]が、既存の攻撃ナレーションの後にブレイクした対象ごとの文を足すことを検証するテスト。 */
class AttackNarrationsTest {

    private val names = mapOf(1 to "味方", 2 to "ゴブリンA", 3 to "ゴブリンB")
    private val nameOf: (Int) -> String = { names.getValue(it) }

    /** 単体・1ヒットでブレイクしたら、攻撃の文の後に「ブレイクした」が付くこと。 */
    @Test
    fun single_break_appendsBrokeSentence() {
        val result = AttackResult(1, listOf(AttackHit(2, listOf(AttackShot(true, 24, causedBreak = true)))))

        assertEquals(
            listOf(BattleNarration.AttackResult("味方", "ゴブリンA", 24), BattleNarration.Broke("ゴブリンA")),
            result.toNarrations(nameOf),
        )
    }

    /** ブレイクしなければ文は増えないこと。 */
    @Test
    fun noBreak_addsNothing() {
        val result = AttackResult(1, listOf(AttackHit(2, listOf(AttackShot(true, 24)))))

        assertEquals(listOf(BattleNarration.AttackResult("味方", "ゴブリンA", 24)), result.toNarrations(nameOf))
    }

    /** 範囲攻撃では、ブレイクした対象ごとに1文ずつ、対象の並び(ブレイクした順)で付くこと。 */
    @Test
    fun area_break_appendsOneSentencePerBrokenTarget() {
        val result = AttackResult(
            1,
            listOf(
                AttackHit(2, listOf(AttackShot(true, 24, causedBreak = true))),
                AttackHit(3, listOf(AttackShot(true, 24), AttackShot(true, 24, causedBreak = true))),
            ),
        )

        val narrations = result.toNarrations(nameOf)

        assertEquals(
            listOf(BattleNarration.Broke("ゴブリンA"), BattleNarration.Broke("ゴブリンB")),
            narrations.takeLast(2),
        )
        assertEquals(BattleNarration.AreaAttackStart("味方"), narrations.first())
    }
}
