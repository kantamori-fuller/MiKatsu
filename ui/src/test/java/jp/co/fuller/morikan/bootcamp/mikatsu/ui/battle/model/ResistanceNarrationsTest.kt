package jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AttackHit
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AttackResult
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AttackShot
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleStat
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.CriticalType
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.ReflectedDamage
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.ReflectedEffect
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.ResistanceOutcome
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.SkillResult
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.SkillTargetResult
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.StatChangeResult
import org.junit.Assert.assertEquals
import org.junit.Test

/** 属性耐性による無効・吸収・反射が、攻撃・スキルのナレーションへ正しく変換されることを検証するテスト。 */
class ResistanceNarrationsTest {

    private val names = mapOf(1 to "味方", 2 to "ゴブリンA", 3 to "ゴブリンB")
    private val nameOf: (Int) -> String = { names.getValue(it) }

    private val nullified = AttackShot(isHit = true, damage = 0, resistance = ResistanceOutcome.NULLIFIED)

    /** 単体・1ヒットの無効は「しかしBには効かなかった」の1文になること。 */
    @Test
    fun single_nullified() {
        val result = AttackResult(1, listOf(AttackHit(2, listOf(nullified))))

        assertEquals(listOf(BattleNarration.AttackNullified("味方", "ゴブリンA")), result.toNarrations(nameOf))
    }

    /** 技名は通常攻撃以外のとき、無効の文にも入ること。 */
    @Test
    fun single_nullified_withSkillName() {
        val result = AttackResult(1, listOf(AttackHit(2, listOf(nullified))), skillId = 5, skillName = "ローリング・ソバット")

        assertEquals(
            listOf(BattleNarration.AttackNullified("味方", "ゴブリンA", "ローリング・ソバット")),
            result.toNarrations(nameOf),
        )
    }

    /** 無効のヒットでクリティカルが出ていたら、無効の文の後に併記されること。 */
    @Test
    fun single_nullified_withCritical_appendsCritical() {
        val result = AttackResult(1, listOf(AttackHit(2, listOf(nullified.copy(critical = CriticalType.CRITICAL)))))

        assertEquals(
            listOf(BattleNarration.AttackNullified("味方", "ゴブリンA"), BattleNarration.AreaCritical("ゴブリンA")),
            result.toNarrations(nameOf),
        )
    }

    /** 多段の無効は「2回目: 効かなかった」になり、ダメージがひとつもないので合計の文は出ないこと。 */
    @Test
    fun multi_nullified_noTotal() {
        val result = AttackResult(1, listOf(AttackHit(2, listOf(nullified, nullified))))

        assertEquals(
            listOf(BattleNarration.ShotNullified(1), BattleNarration.ShotNullified(2)),
            result.toNarrations(nameOf),
        )
    }

    /** 多段で命中と無効が混ざるときは、1発ごとの文の後に合計の文が付くこと。 */
    @Test
    fun multi_mixed_keepsTotal() {
        val result = AttackResult(1, listOf(AttackHit(2, listOf(AttackShot(true, 24), nullified))))

        assertEquals(
            listOf(
                BattleNarration.ShotHit(1, 24),
                BattleNarration.ShotNullified(2),
                BattleNarration.AttackTotal("味方", "ゴブリンA", 24, 2, 2),
            ),
            result.toNarrations(nameOf),
        )
    }

    /** 範囲攻撃の無効は「Bには効かなかった」でまとめられ、命中した相手と分かれること。 */
    @Test
    fun area_nullifiedAndHit() {
        val result = AttackResult(
            1,
            listOf(AttackHit(2, listOf(nullified)), AttackHit(3, listOf(AttackShot(true, 24)))),
        )

        assertEquals(
            listOf(
                BattleNarration.AreaAttackStart("味方", null),
                BattleNarration.AreaHit(listOf(AreaHitEntry("ゴブリンB", 24))),
                BattleNarration.AreaNullified(listOf("ゴブリンA")),
            ),
            result.toNarrations(nameOf),
        )
    }

    /** 吸収は「Bは攻撃を吸収した! HPが30回復」の1文になること。 */
    @Test
    fun single_absorbed() {
        val shot = AttackShot(true, 0, resistance = ResistanceOutcome.ABSORBED, absorbedAmount = 30)
        val result = AttackResult(1, listOf(AttackHit(2, listOf(shot))))

        assertEquals(
            listOf(BattleNarration.AttackLead("味方", "ゴブリンA"), BattleNarration.AttackAbsorbed("ゴブリンA", 30)),
            result.toNarrations(nameOf))
    }

    /** 反射は「Bは攻撃を跳ね返した! Aに30ダメージ」になり、攻撃者のクリティカルとブレイクが続くこと。 */
    @Test
    fun single_reflected_withCriticalAndBreak() {
        val shot = AttackShot(
            true, 0, resistance = ResistanceOutcome.REFLECTED,
            reflected = ReflectedDamage(damage = 30, critical = CriticalType.CRITICAL, causedBreak = true),
        )
        val result = AttackResult(1, listOf(AttackHit(2, listOf(shot))), skillId = 5, skillName = "ローリング・ソバット")

        assertEquals(
            listOf(
                BattleNarration.AttackLead("味方", "ゴブリンA", "ローリング・ソバット"),
                BattleNarration.AttackReflected("ゴブリンA", "味方", 30),
                BattleNarration.AreaCritical("味方"),
                BattleNarration.Broke("味方"),
            ),
            result.toNarrations(nameOf),
        )
    }

    /** 反射を攻撃者が無効にしたとき・吸収したときの文になること。 */
    @Test
    fun single_reflected_attackerNullifiesOrAbsorbs() {
        val nullifiedByAttacker = AttackShot(
            true, 0, resistance = ResistanceOutcome.REFLECTED,
            reflected = ReflectedDamage(0, outcome = ResistanceOutcome.NULLIFIED),
        )
        val absorbedByAttacker = AttackShot(
            true, 0, resistance = ResistanceOutcome.REFLECTED,
            reflected = ReflectedDamage(0, outcome = ResistanceOutcome.ABSORBED, absorbedAmount = 30),
        )

        assertEquals(
            listOf(BattleNarration.AttackLead("味方", "ゴブリンA"), BattleNarration.AttackReflectedNullified("ゴブリンA", "味方")),
            AttackResult(1, listOf(AttackHit(2, listOf(nullifiedByAttacker)))).toNarrations(nameOf),
        )
        assertEquals(
            listOf(BattleNarration.AttackLead("味方", "ゴブリンA"), BattleNarration.AttackReflectedAbsorbed("ゴブリンA", "味方", 30)),
            AttackResult(1, listOf(AttackHit(2, listOf(absorbedByAttacker)))).toNarrations(nameOf),
        )
    }

    /** デバフが対象1体に無効なら「Aの覇気! しかしBには効かなかった!」の1文になること。 */
    @Test
    fun skill_nullified_single() {
        val result = SkillResult(1, 4, "覇気", listOf(SkillTargetResult(2, 0, resistance = ResistanceOutcome.NULLIFIED)))

        assertEquals(listOf(BattleNarration.SkillNullified("味方", "覇気", "ゴブリンA")), result.toNarrations(nameOf))
    }

    /** デバフが反射されたら、「Bは効果を跳ね返した!」の後に使用者のステータス変化が続くこと。 */
    @Test
    fun skill_reflected_thenActorStatChanges() {
        val target = SkillTargetResult(
            2, 0, resistance = ResistanceOutcome.REFLECTED,
            reflected = ReflectedEffect(false, listOf(StatChangeResult(BattleStat.AGILITY, -1))),
        )
        val result = SkillResult(1, 4, "覇気", listOf(target))

        assertEquals(
            listOf(
                BattleNarration.SkillUsed("味方", "覇気"),
                BattleNarration.EffectReflected("ゴブリンA"),
                BattleNarration.StatStageChanged("味方", BattleStat.AGILITY, -1),
            ),
            result.toNarrations(nameOf),
        )
    }

    /** 反射されたデバフが使用者側でも無効なら「しかしAには効かなかった!」が続くこと。 */
    @Test
    fun skill_reflected_actorNullifies() {
        val target = SkillTargetResult(
            2, 0, resistance = ResistanceOutcome.REFLECTED,
            reflected = ReflectedEffect(isNullified = true),
        )
        val result = SkillResult(1, 4, "覇気", listOf(target))

        assertEquals(
            listOf(
                BattleNarration.SkillUsed("味方", "覇気"),
                BattleNarration.EffectReflected("ゴブリンA"),
                BattleNarration.EffectReflectedNullified("味方"),
            ),
            result.toNarrations(nameOf),
        )
    }

    /** 複数対象で一部だけ無効なら、「Aのスキル!」の後に無効の対象の文とかかった対象の変化が並ぶこと。 */
    @Test
    fun skill_area_partlyNullified() {
        val result = SkillResult(
            1, 4, "覇気",
            listOf(
                SkillTargetResult(2, 0, resistance = ResistanceOutcome.NULLIFIED),
                SkillTargetResult(3, 0, statChanges = listOf(StatChangeResult(BattleStat.DEFENSE, -1))),
            ),
        )

        assertEquals(
            listOf(
                BattleNarration.SkillUsed("味方", "覇気"),
                BattleNarration.EffectNullified("ゴブリンA"),
                BattleNarration.StatStageChanged("ゴブリンB", BattleStat.DEFENSE, -1),
            ),
            result.toNarrations(nameOf),
        )
    }

    /** 多段で吸収・反射のヒットは「2回目: 命中!」の後に吸収・反射の文が続くこと。 */
    @Test
    fun multi_absorbedAndReflected_showLanded() {
        val absorbed = AttackShot(true, 0, resistance = ResistanceOutcome.ABSORBED, absorbedAmount = 30)
        val reflected = AttackShot(
            true, 0, resistance = ResistanceOutcome.REFLECTED, reflected = ReflectedDamage(damage = 30),
        )
        val result = AttackResult(1, listOf(AttackHit(2, listOf(absorbed, reflected))))

        assertEquals(
            listOf(
                BattleNarration.ShotLanded(1),
                BattleNarration.AttackAbsorbed("ゴブリンA", 30),
                BattleNarration.ShotLanded(2),
                BattleNarration.AttackReflected("ゴブリンA", "味方", 30),
            ),
            result.toNarrations(nameOf),
        )
    }
}
