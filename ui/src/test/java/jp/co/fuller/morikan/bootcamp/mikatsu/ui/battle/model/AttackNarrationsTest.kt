package jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AttackHit
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AttackResult
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AttackShot
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
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

    /** 範囲の多段攻撃では、ブレイクの文が回ごとに(接頭辞なしで)出て、末尾にまとめて重ねて付かないこと。 */
    @Test
    fun areaMulti_break_isReportedPerRound() {
        val result = AttackResult(
            1,
            listOf(
                AttackHit(2, listOf(AttackShot(true, 24, causedBreak = true))),
                AttackHit(3, listOf(AttackShot(true, 24), AttackShot(true, 24, causedBreak = true))),
            ),
        )

        assertEquals(
            listOf(
                BattleNarration.AreaAttackStart("味方"),
                BattleNarration.InRound(1, BattleNarration.AreaHit(listOf(AreaHitEntry("ゴブリンA", 24), AreaHitEntry("ゴブリンB", 24)))),
                BattleNarration.Broke("ゴブリンA"),
                BattleNarration.InRound(2, BattleNarration.AreaHit(listOf(AreaHitEntry("ゴブリンB", 24)))),
                BattleNarration.Broke("ゴブリンB"),
                BattleNarration.AreaAttackTotal("味方", listOf(AreaHitEntry("ゴブリンA", 24), AreaHitEntry("ゴブリンB", 48))),
            ),
            result.toNarrations(nameOf),
        )
    }

    /** 範囲の発動回数1の攻撃では、ブレイクした対象ごとに1文ずつ、まとめの文の後ろに付くこと(従来どおり)。 */
    @Test
    fun area_oneShot_break_appendsOneSentencePerBrokenTarget() {
        val result = AttackResult(
            1,
            listOf(
                AttackHit(2, listOf(AttackShot(true, 24, causedBreak = true))),
                AttackHit(3, listOf(AttackShot(true, 24, causedBreak = true))),
            ),
        )

        val narrations = result.toNarrations(nameOf)

        assertEquals(
            listOf(BattleNarration.Broke("ゴブリンA"), BattleNarration.Broke("ゴブリンB")),
            narrations.takeLast(2),
        )
        assertEquals(BattleNarration.AreaAttackStart("味方"), narrations.first())
    }

    /** 範囲の多段攻撃は、回ごとに「外れた相手 → ダメージ → クリティカル」を出し、最後に対象ごとの合計のまとめを出すこと。 */
    @Test
    fun areaMulti_reportsEachRoundThenTotals() {
        val result = AttackResult(
            1,
            listOf(
                AttackHit(2, listOf(AttackShot(true, 30), AttackShot(false, 0))),
                AttackHit(3, listOf(AttackShot(true, 25, critical = CriticalType.CRITICAL), AttackShot(true, 28))),
            ),
            skillId = 7,
            skillName = "クイックシュート",
        )

        assertEquals(
            listOf(
                BattleNarration.AreaAttackStart("味方", "クイックシュート"),
                BattleNarration.InRound(1, BattleNarration.AreaHit(listOf(AreaHitEntry("ゴブリンA", 30), AreaHitEntry("ゴブリンB", 25)))),
                BattleNarration.InRound(1, BattleNarration.AreaCritical("ゴブリンB")),
                BattleNarration.InRound(2, BattleNarration.AreaMissed(listOf("ゴブリンA"))),
                BattleNarration.InRound(2, BattleNarration.AreaHit(listOf(AreaHitEntry("ゴブリンB", 28)))),
                BattleNarration.AreaAttackTotal("味方", listOf(AreaHitEntry("ゴブリンA", 30), AreaHitEntry("ゴブリンB", 53))),
            ),
            result.toNarrations(nameOf),
        )
    }

    /** 範囲の多段攻撃が全回・全対象で外れたら、「しかし外れた!」の1文だけになること。 */
    @Test
    fun areaMulti_allMissed_isOneSentence() {
        val result = AttackResult(
            1,
            listOf(
                AttackHit(2, listOf(AttackShot(false, 0), AttackShot(false, 0))),
                AttackHit(3, listOf(AttackShot(false, 0), AttackShot(false, 0))),
            ),
        )

        assertEquals(listOf<BattleNarration>(BattleNarration.AreaAttackAllMissed("味方")), result.toNarrations(nameOf))
    }

    /** 表示用スナップショットを区別するための、目印だけを変えたダミーの戦闘フィールド。 */
    private fun marker(n: Int) = BattleField(allies = emptyList(), enemies = emptyList(), turnQueue = listOf(n))

    /** 単体の多段では、各ヒットの文がそのヒットのfieldAfter、最後の文(まとめ)は技全体のfieldAfterになること。 */
    @Test
    fun steps_single_multiShot_mapsEachShotToItsSnapshot() {
        val result = AttackResult(
            1,
            listOf(AttackHit(2, listOf(AttackShot(true, 24, fieldAfter = marker(1)), AttackShot(true, 24, fieldAfter = marker(2))))),
            fieldBeforeShots = marker(0),
            fieldAfter = marker(9),
        )

        assertEquals(listOf(marker(1), marker(2), marker(9)), result.toNarrationSteps(nameOf).map { it.field })
    }

    /** 範囲の発動回数1では、開始の文がfieldBeforeShots、以降が最後のヒットのfieldAfter、最後の文だけ技全体のfieldAfterになること。 */
    @Test
    fun steps_area_oneShot_startUsesBeforeShots_lastUsesFinalField() {
        val result = AttackResult(
            1,
            listOf(
                AttackHit(2, listOf(AttackShot(true, 24, fieldAfter = marker(1)))),
                AttackHit(3, listOf(AttackShot(true, 24, critical = CriticalType.CRITICAL, fieldAfter = marker(2)))),
            ),
            fieldBeforeShots = marker(0),
            fieldAfter = marker(9),
        )

        val steps = result.toNarrationSteps(nameOf)

        assertEquals(listOf(marker(0), marker(2), marker(9)), steps.map { it.field })
        assertEquals(
            listOf(BattleNarration.AreaAttackStart("味方"), BattleNarration.AreaHit(listOf(AreaHitEntry("ゴブリンA", 24), AreaHitEntry("ゴブリンB", 24))), BattleNarration.AreaCritical("ゴブリンB")),
            steps.map { it.narration },
        )
    }

    /** 範囲の多段では、N回目の文がN回目で最後に実行されたヒットのfieldAfter、最後の文(まとめ)が技全体のfieldAfterになること。 */
    @Test
    fun steps_areaMulti_mapsRoundsToLastShotOfRound() {
        val result = AttackResult(
            1,
            listOf(
                AttackHit(2, listOf(AttackShot(true, 24, fieldAfter = marker(1)), AttackShot(true, 24, fieldAfter = marker(3)))),
                AttackHit(3, listOf(AttackShot(true, 24, fieldAfter = marker(2)), AttackShot(true, 24, fieldAfter = marker(4)))),
            ),
            fieldBeforeShots = marker(0),
            fieldAfter = marker(9),
        )

        // 開始 / 1回目(両対象の後 = marker2) / 2回目(marker4) / まとめ(技全体)
        assertEquals(listOf(marker(0), marker(2), marker(4), marker(9)), result.toNarrationSteps(nameOf).map { it.field })
    }

    /** スナップショットを持たない結果(テストでの組み立て)では、すべての文のfieldがnullになること。 */
    @Test
    fun steps_withoutSnapshots_haveNullFields() {
        val result = AttackResult(1, listOf(AttackHit(2, listOf(AttackShot(true, 24), AttackShot(true, 24)))))

        assertEquals(listOf(null, null, null), result.toNarrationSteps(nameOf).map { it.field })
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

    /** 範囲の多段攻撃は、各回の中で ダメージの行 → クリティカルの文(ヒットごと) → ブレイクの文の順になり、最後にまとめが付くこと。 */
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
                BattleNarration.InRound(1, BattleNarration.AreaHit(listOf(AreaHitEntry("ゴブリンA", 45), AreaHitEntry("ゴブリンB", 18)))),
                BattleNarration.InRound(1, BattleNarration.AreaCritical("ゴブリンA")),
                BattleNarration.InRound(1, BattleNarration.AreaBadCritical("ゴブリンB")),
                BattleNarration.InRound(2, BattleNarration.AreaHit(listOf(AreaHitEntry("ゴブリンA", 45)))),
                BattleNarration.InRound(2, BattleNarration.AreaCritical("ゴブリンA")),
                BattleNarration.Broke("ゴブリンA"),
                BattleNarration.AreaAttackTotal("味方", listOf(AreaHitEntry("ゴブリンA", 90), AreaHitEntry("ゴブリンB", 18))),
            ),
            result.toNarrations(nameOf),
        )
    }

    /** 対象1体・1ヒットで防御がクリティカルを防いだら、その旨を持った1文になること。 */
    @Test
    fun single_oneShot_criticalBlocked() {
        val result = AttackResult(1, listOf(AttackHit(2, listOf(AttackShot(true, 30, isCriticalBlocked = true)))))

        assertEquals(
            listOf(BattleNarration.AttackResult("味方", "ゴブリンA", 30, CriticalType.NONE, isCriticalBlocked = true)),
            result.toNarrations(nameOf),
        )
    }

    /** 多段では、防御がクリティカルを防いだヒットの行にだけ印が付くこと。 */
    @Test
    fun single_multiShot_criticalBlocked_perShot() {
        val result = AttackResult(
            1,
            listOf(AttackHit(2, listOf(AttackShot(true, 30, isCriticalBlocked = true), AttackShot(true, 30)))),
        )

        assertEquals(
            listOf(
                BattleNarration.ShotHit(1, 30, CriticalType.NONE, isCriticalBlocked = true),
                BattleNarration.ShotHit(2, 30),
                BattleNarration.AttackTotal("味方", "ゴブリンA", 60, 2, 2),
            ),
            result.toNarrations(nameOf),
        )
    }

    /** 範囲攻撃では、クリティカルの文と同じ位置(ダメージの文の後)に、防御でクリティカルを防いだ文が出ること。 */
    @Test
    fun area_criticalBlocked_sentenceInCriticalPosition() {
        val result = AttackResult(
            1,
            listOf(
                AttackHit(2, listOf(AttackShot(true, 30, isCriticalBlocked = true))),
                AttackHit(3, listOf(AttackShot(true, 45, critical = CriticalType.CRITICAL))),
            ),
        )

        assertEquals(
            listOf(
                BattleNarration.AreaAttackStart("味方"),
                BattleNarration.AreaHit(listOf(AreaHitEntry("ゴブリンA", 30), AreaHitEntry("ゴブリンB", 45))),
                BattleNarration.AreaCriticalBlocked("ゴブリンA"),
                BattleNarration.AreaCritical("ゴブリンB"),
            ),
            result.toNarrations(nameOf),
        )
    }

    /** ローリング・ソバット(対象1体・1ヒット)は、「攻撃!」の部分が技名になること(外れ・クリティカルも同じ)。 */
    @Test
    fun sobat_single_usesSkillName() {
        val hit = AttackResult(1, listOf(AttackHit(2, listOf(AttackShot(true, 45)))), skillId = 5, skillName = "ローリング・ソバット")
        val miss = AttackResult(1, listOf(AttackHit(2, listOf(AttackShot(false, 0)))), skillId = 5, skillName = "ローリング・ソバット")
        val critical = AttackResult(
            1,
            listOf(AttackHit(2, listOf(AttackShot(true, 60, critical = CriticalType.CRITICAL)))),
            skillId = 5,
            skillName = "ローリング・ソバット",
        )

        assertEquals(
            listOf(BattleNarration.AttackResult("味方", "ゴブリンA", 45, skillName = "ローリング・ソバット")),
            hit.toNarrations(nameOf),
        )
        assertEquals(
            listOf(BattleNarration.AttackMissed("味方", "ゴブリンA", "ローリング・ソバット")),
            miss.toNarrations(nameOf),
        )
        assertEquals(
            listOf(BattleNarration.AttackResult("味方", "ゴブリンA", 60, CriticalType.CRITICAL, skillName = "ローリング・ソバット")),
            critical.toNarrations(nameOf),
        )
    }

    /** 範囲攻撃の技は、「Aのローリング・ソバット!」と技名で始まり、全員外れなら技名つきの1文になること。 */
    @Test
    fun sobat_area_usesSkillName() {
        val landed = AttackResult(
            1,
            listOf(AttackHit(2, listOf(AttackShot(true, 24))), AttackHit(3, listOf(AttackShot(true, 25)))),
            skillId = 5,
            skillName = "ローリング・ソバット",
        )
        val allMissed = AttackResult(
            1,
            listOf(AttackHit(2, listOf(AttackShot(false, 0))), AttackHit(3, listOf(AttackShot(false, 0)))),
            skillId = 5,
            skillName = "ローリング・ソバット",
        )

        assertEquals(BattleNarration.AreaAttackStart("味方", "ローリング・ソバット"), landed.toNarrations(nameOf).first())
        assertEquals(
            listOf(BattleNarration.AreaAttackAllMissed("味方", "ローリング・ソバット")),
            allMissed.toNarrations(nameOf),
        )
    }

    /** 通常攻撃(スキルID1)は、技名が入っていても「攻撃」のまま(技名を持たない)であること。 */
    @Test
    fun normalAttack_keepsAttackWording() {
        val normal = AttackResult(1, listOf(AttackHit(2, listOf(AttackShot(true, 24)))), skillId = 1, skillName = "通常攻撃")

        assertEquals(listOf(BattleNarration.AttackResult("味方", "ゴブリンA", 24)), normal.toNarrations(nameOf))
    }
}
