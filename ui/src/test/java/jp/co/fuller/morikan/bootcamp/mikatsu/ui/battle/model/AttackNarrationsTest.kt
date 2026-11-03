package jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AttackHit
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AttackResult
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AttackShot
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.CriticalType
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

    /** 対象1体・1ヒットのクリティカル・バッドクリティカルは、種別を持った1文になること。 */
    @Test
    fun single_oneShot_carriesCriticalType() {
        val critical = AttackResult(1, listOf(AttackHit(2, listOf(AttackShot(true, 45, critical = CriticalType.CRITICAL)))))
        val bad = AttackResult(1, listOf(AttackHit(2, listOf(AttackShot(true, 30, critical = CriticalType.BAD_CRITICAL)))))

        assertEquals(
            listOf(BattleNarration.AttackResult("味方", "ゴブリンA", 45, CriticalType.CRITICAL)),
            critical.toNarrations(nameOf),
        )
        assertEquals(
            listOf(BattleNarration.AttackResult("味方", "ゴブリンA", 30, CriticalType.BAD_CRITICAL)),
            bad.toNarrations(nameOf),
        )
    }

    /** 対象1体・2ヒット以上は、1発ごとの行に種別が入り、合計の行は今までどおりであること。 */
    @Test
    fun single_multiShot_perShotCriticalType_totalUnchanged() {
        val result = AttackResult(
            1,
            listOf(
                AttackHit(
                    2,
                    listOf(
                        AttackShot(true, 45, critical = CriticalType.CRITICAL),
                        AttackShot(true, 30, critical = CriticalType.BAD_CRITICAL),
                        AttackShot(false, 0),
                        AttackShot(true, 30),
                    ),
                ),
            ),
        )

        assertEquals(
            listOf(
                BattleNarration.ShotHit(1, 45, CriticalType.CRITICAL),
                BattleNarration.ShotHit(2, 30, CriticalType.BAD_CRITICAL),
                BattleNarration.ShotMissed(3),
                BattleNarration.ShotHit(4, 30),
                BattleNarration.AttackTotal("味方", "ゴブリンA", 105, 4, 3),
            ),
            result.toNarrations(nameOf),
        )
    }

    /** 範囲攻撃は、ダメージの行 → クリティカルの文(ヒットごと) → ブレイクの文の順になること。外れたヒットは出さない。 */
    @Test
    fun area_criticalSentences_afterDamage_beforeBroke() {
        val result = AttackResult(
            1,
            listOf(
                AttackHit(
                    2,
                    listOf(
                        AttackShot(true, 45, critical = CriticalType.CRITICAL),
                        AttackShot(true, 45, causedBreak = true, critical = CriticalType.CRITICAL),
                    ),
                ),
                AttackHit(3, listOf(AttackShot(true, 18, critical = CriticalType.BAD_CRITICAL))),
            ),
        )

        assertEquals(
            listOf(
                BattleNarration.AreaAttackStart("味方"),
                BattleNarration.AreaHit(listOf(AreaHitEntry("ゴブリンA", 90), AreaHitEntry("ゴブリンB", 18))),
                BattleNarration.AreaCritical("ゴブリンA"),
                BattleNarration.AreaCritical("ゴブリンA"),
                BattleNarration.AreaBadCritical("ゴブリンB"),
                BattleNarration.Broke("ゴブリンA"),
            ),
            result.toNarrations(nameOf),
        )
    }
}
