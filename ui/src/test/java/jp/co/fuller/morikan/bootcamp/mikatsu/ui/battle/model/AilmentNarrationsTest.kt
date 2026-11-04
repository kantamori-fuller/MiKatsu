package jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AttackHit
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AttackResult
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AttackShot
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.EffectApplication
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.ReflectedEffect
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.ResistanceOutcome
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.SkillResult
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.SkillTargetResult
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.StatusAilment
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.TurnEndEvent
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 状態異常のナレーション(スキルの付与・即死、手番終了時の出来事、攻撃のヒットでの解除)の組み立てを検証するテスト。
 */
class AilmentNarrationsTest {

    private val names = mapOf(1 to "味方", 2 to "味方B", 3 to "ゴブリンA", 4 to "ゴブリンB")
    private val nameOf: (Int) -> String = { names.getValue(it) }
    private val field = BattleField(allies = emptyList(), enemies = emptyList())

    private fun ailment(a: StatusAilment, ok: Boolean) = EffectApplication.Ailment(a, ok)

    /** 成功した状態異常は「Bは毒になった!」と1文ずつ並ぶ。 */
    @Test
    fun skill_successfulAilments() {
        val result = SkillResult(
            1, 8, "カオスリズム",
            listOf(
                SkillTargetResult(
                    3, 0,
                    effectResults = listOf(ailment(StatusAilment.POISON, true), ailment(StatusAilment.BURN, true)),
                ),
            ),
        )

        assertEquals(
            listOf(
                BattleNarration.SkillUsed("味方", "カオスリズム"),
                BattleNarration.AilmentInflicted("ゴブリンA", StatusAilment.POISON),
                BattleNarration.AilmentInflicted("ゴブリンA", StatusAilment.BURN),
            ),
            result.toNarrations(nameOf),
        )
    }

    /** 全効果が失敗なら「しかしBには効かなかった!」、一部だけなら成功分の後に「Bには〈名〉が効かなかった!」。 */
    @Test
    fun skill_failedAilments_allOrPartial() {
        val all = SkillResult(
            1, 8, "カオスリズム",
            listOf(SkillTargetResult(3, 0, effectResults = listOf(ailment(StatusAilment.POISON, false), ailment(StatusAilment.BURN, false)))),
        )
        val partial = SkillResult(
            1, 8, "カオスリズム",
            listOf(SkillTargetResult(3, 0, effectResults = listOf(ailment(StatusAilment.POISON, false), ailment(StatusAilment.BURN, true)))),
        )

        assertEquals(
            listOf(BattleNarration.SkillUsed("味方", "カオスリズム"), BattleNarration.AilmentAllFailed("ゴブリンA")),
            all.toNarrations(nameOf),
        )
        assertEquals(
            listOf(
                BattleNarration.SkillUsed("味方", "カオスリズム"),
                BattleNarration.AilmentInflicted("ゴブリンA", StatusAilment.BURN),
                BattleNarration.AilmentFailed("ゴブリンA", StatusAilment.POISON),
            ),
            partial.toNarrations(nameOf),
        )
    }

    /** 外れた(命中しなかった)ときは従来どおり「しかし外れた!」の1文だけ。 */
    @Test
    fun skill_missed_keepsExistingSentence() {
        val result = SkillResult(1, 8, "カオスリズム", listOf(SkillTargetResult(3, 0, isHit = false)))

        assertEquals(listOf(BattleNarration.SkillMissed("味方", "カオスリズム")), result.toNarrations(nameOf))
    }

    /** 反射された状態異常は、使用者に対する成功・失敗の文になる。 */
    @Test
    fun skill_reflectedAilment_usesActorName() {
        val result = SkillResult(
            1, 8, "カオスリズム",
            listOf(
                SkillTargetResult(
                    3, 0,
                    resistance = ResistanceOutcome.REFLECTED,
                    reflected = ReflectedEffect(false, effectResults = listOf(ailment(StatusAilment.POISON, true))),
                ),
            ),
        )

        assertEquals(
            listOf(
                BattleNarration.SkillUsed("味方", "カオスリズム"),
                BattleNarration.EffectReflected("ゴブリンA"),
                BattleNarration.AilmentInflicted("味方", StatusAilment.POISON),
            ),
            result.toNarrations(nameOf),
        )
    }

    /** 発動回数2以上は、回ごとに「N回目: 」を付けて報告し、外れた対象は「Bには外れた!」で伝える。 */
    @Test
    fun skill_multiRound_prefixesEachRound() {
        val death = listOf(EffectApplication.InstantDeath(true))
        val fail = listOf(EffectApplication.InstantDeath(false))
        val result = SkillResult(
            1, 9, "命頂戴",
            listOf(
                SkillTargetResult(3, 0, effectResults = fail, round = 0),
                SkillTargetResult(4, 0, isHit = false, round = 0),
                SkillTargetResult(3, 0, effectResults = death, round = 1),
            ),
        )

        assertEquals(
            listOf(
                BattleNarration.SkillUsed("味方", "命頂戴"),
                BattleNarration.InRound(1, BattleNarration.AreaMissed(listOf("ゴブリンB"))),
                BattleNarration.InRound(1, BattleNarration.AilmentAllFailed("ゴブリンA")),
                BattleNarration.InRound(2, BattleNarration.InstantDeathSucceeded("ゴブリンA")),
            ),
            result.toNarrations(nameOf),
        )
    }

    /** 手番終了時の出来事が、文と直後のフィールドの組になる。 */
    @Test
    fun turnEndEvents_toSteps() {
        val events = listOf(
            TurnEndEvent.HpDamage(3, StatusAilment.POISON, 12, field),
            TurnEndEvent.MpLoss(3, StatusAilment.CURSE, 5, field),
            TurnEndEvent.TpLoss(3, StatusAilment.CONTAMINATION, 12, field),
            TurnEndEvent.Spread(3, 4, field),
            TurnEndEvent.Cured(3, StatusAilment.POISON, field),
            TurnEndEvent.Defeated(3, field),
        )

        assertEquals(
            listOf(
                BattleNarration.AilmentHpDamage("ゴブリンA", StatusAilment.POISON, 12),
                BattleNarration.AilmentMpLoss("ゴブリンA", 5),
                BattleNarration.AilmentTpLoss("ゴブリンA", 12),
                BattleNarration.AilmentSpread("ゴブリンA", "ゴブリンB"),
                BattleNarration.AilmentCured("ゴブリンA", StatusAilment.POISON),
                BattleNarration.UnitDefeated("ゴブリンA"),
            ),
            events.map { it.toNarrationStep(nameOf).narration },
        )
        assertEquals(List(6) { field }, events.map { it.toNarrationStep(nameOf).field })
    }

    /** 攻撃のヒットで睡眠・凍結が解除されたら、そのヒットの文の後に解除の文が続く。 */
    @Test
    fun attack_releasedAilments_followHitSentence() {
        val shot = AttackShot(isHit = true, damage = 30, curedAilments = listOf(StatusAilment.SLEEP))
        val result = AttackResult(1, listOf(AttackHit(3, listOf(shot))))

        assertEquals(
            listOf(
                BattleNarration.AttackResult("味方", "ゴブリンA", 30),
                BattleNarration.AilmentReleasedByDamage("ゴブリンA", StatusAilment.SLEEP),
            ),
            result.toNarrations(nameOf),
        )
    }

    /** 範囲の多段攻撃では、解除の文に「N回目: 」を付けない。 */
    @Test
    fun attack_areaMultiStrike_releaseHasNoRoundPrefix() {
        val released = AttackShot(isHit = true, damage = 30, curedAilments = listOf(StatusAilment.FREEZE))
        val plain = AttackShot(isHit = true, damage = 20)
        val result = AttackResult(
            1,
            listOf(AttackHit(3, listOf(released, plain)), AttackHit(4, listOf(plain, plain))),
            skillName = "クイックシュート",
            skillId = 7,
        )

        val narrations = result.toNarrations(nameOf)

        assertEquals(true, BattleNarration.AilmentReleasedByDamage("ゴブリンA", StatusAilment.FREEZE) in narrations)
    }
}
