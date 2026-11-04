package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.StatusAilment
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.TurnEndEvent
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.applyDamage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 手番終了時の状態異常の処理([processTurnEndAilments]・[endTurnWithEvents])を検証するテスト。
 * 各ダメージ量と最低1、MP・TPの減少、感染の分母と伝染、残りターンの減算と解除、手番中に付与された分、戦闘不能での打ち切りを確かめる。
 */
class StatusAilmentTurnEndTest {

    /** 味方1(手番・最大HP100・MP80)・味方2・敵4がいる戦闘。味方1に[ailments]をかける。 */
    private fun field(vararg ailments: jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AilmentState, hp: Int = 100): BattleField =
        BattleField(
            allies = listOf(
                testAlly(1, hp = hp, mp = 80).withAilments(*ailments),
                testAlly(2, hp = 160),
                testAlly(3),
            ),
            enemies = listOf(testEnemy(4)),
            turnQueue = listOf(2, 4),
        )

    private fun BattleField.run(random: SequenceRandom = SequenceRandom()) = processTurnEndAilments(1, random)

    /** HPの割合ダメージと固定ダメージ。毒1/8・火傷30・凍傷10・流血1/16・感電1/16。 */
    @Test
    fun hpDamage_amounts() {
        val cases = mapOf(
            StatusAilment.POISON to 12,
            StatusAilment.BURN to 30,
            StatusAilment.FROSTBITE to 10,
            StatusAilment.BLEED to 6,
            StatusAilment.SHOCK to 6,
        )
        cases.forEach { (ailment, damage) ->
            val (after, events) = field(ail(ailment)).run()

            assertEquals("$ailment", 100 - damage, after.pawn(1).currentHp)
            assertEquals("$ailment", TurnEndEvent.HpDamage(1, ailment, damage, events.first().field), events.first())
        }
    }

    /** 割合ダメージは最低1。 */
    @Test
    fun fractionalDamage_isAtLeastOne() {
        val (after, _) = field(ail(StatusAilment.POISON), hp = 4).copy(
            allies = listOf(testAlly(1, hp = 4).withAilments(ail(StatusAilment.POISON)), testAlly(2)),
        ).run()

        assertEquals(3, after.pawn(1).currentHp)
    }

    /** 呪怨はHP1/16とMP1/8、汚染はMP1/8とTP12を失う。TPは0未満にならず、減らなければ出来事にしない。 */
    @Test
    fun curseAndContamination_loseMpAndTp() {
        val (curse, curseEvents) = field(ail(StatusAilment.CURSE)).run()
        assertEquals(94, curse.pawn(1).currentHp)
        assertEquals(70, curse.pawn(1).currentMp)
        assertEquals(listOf("HpDamage", "MpLoss"), curseEvents.map { it::class.simpleName })

        val withTp = field(ail(StatusAilment.CONTAMINATION)).let { f ->
            f.copy(allies = f.allies.map { if (it.fpid == 1) it.copy(currentTp = 30) else it })
        }
        val (contaminated, events) = withTp.run()
        assertEquals(70, contaminated.pawn(1).currentMp)
        assertEquals(18, contaminated.pawn(1).currentTp)
        assertEquals(listOf("MpLoss", "TpLoss"), events.map { it::class.simpleName })

        val lowTp = withTp.copy(allies = withTp.allies.map { if (it.fpid == 1) it.copy(currentTp = 5) else it })
        assertEquals(0, lowTp.run().first.pawn(1).currentTp)
        val noTp = withTp.copy(allies = withTp.allies.map { if (it.fpid == 1) it.copy(currentTp = 0) else it })
        assertEquals(listOf("MpLoss"), noTp.run().second.map { it::class.simpleName })
    }

    /** 敵のMPも汚染・呪怨で減る。 */
    @Test
    fun enemyMp_isReducedByCurse() {
        val enemy = testEnemy(4, hp = 160).let {
            it.copy(enemy = it.enemy.copy(mana = 80), currentMp = 80).withAilments(ail(StatusAilment.CURSE))
        }
        val f = BattleField(allies = listOf(testAlly(1)), enemies = listOf(enemy), turnQueue = listOf(4, 1))

        val (after, _) = f.processTurnEndAilments(4, SequenceRandom())

        assertEquals(70, after.pawn(4).currentMp)
        assertEquals(150, after.pawn(4).currentHp)
    }

    /** 残りターンを1減らし、0になったら解除して出来事にする。 */
    @Test
    fun remainingTurns_decrementAndCure() {
        val (after, events) = field(ail(StatusAilment.POISON, turns = 2), ail(StatusAilment.BURN, turns = 1)).run()

        assertEquals(1, after.pawn(1).ailments.single { it.ailment == StatusAilment.POISON }.remainingTurns)
        assertEquals(listOf(StatusAilment.POISON), after.pawn(1).ailmentKinds())
        assertEquals(listOf("HpDamage", "HpDamage", "Cured"), events.map { it::class.simpleName })
        assertEquals(StatusAilment.BURN, (events.last() as TurnEndEvent.Cured).ailment)
    }

    /** 状態異常はenum順に処理する。 */
    @Test
    fun processedInEnumOrder() {
        val (_, events) = field(ail(StatusAilment.BLEED), ail(StatusAilment.POISON)).run()

        assertEquals(listOf(StatusAilment.POISON, StatusAilment.BLEED), events.filterIsInstance<TurnEndEvent.HpDamage>().map { it.ailment })
    }

    /** 手番中にかけられた分は、ダメージも減算もせずフラグを下ろすだけ。 */
    @Test
    fun grantedDuringOwnTurn_onlyLowersFlag() {
        val (after, events) = field(ail(StatusAilment.POISON, turns = 3, granted = true)).run()

        assertEquals(100, after.pawn(1).currentHp)
        assertTrue(events.isEmpty())
        val state = after.pawn(1).ailments.single()
        assertEquals(3, state.remainingTurns)
        assertEquals(false, state.grantedDuringOwnTurn)
    }

    /** このダメージではブレイクゲージもTPも貯まらず、睡眠・凍結も解除されない。 */
    @Test
    fun damage_doesNotChargeBreakTpOrReleaseSleep() {
        val (after, _) = field(ail(StatusAilment.POISON), ail(StatusAilment.SLEEP), ail(StatusAilment.FREEZE)).run()

        assertEquals(0.0, after.pawn(1).breakGauge, 0.0)
        assertEquals(0, after.pawn(1).currentTp)
        assertEquals(listOf(StatusAilment.POISON, StatusAilment.FREEZE, StatusAilment.SLEEP), after.pawn(1).ailmentKinds())
    }

    /** 石化中のダメージは1。 */
    @Test
    fun petrified_damageIsOne() {
        val (after, _) = field(ail(StatusAilment.PETRIFY), ail(StatusAilment.BURN)).run()

        assertEquals(99, after.pawn(1).currentHp)
    }

    /** ダメージで戦闘不能になったら全解除し、以降の処理(MPの減少・別の状態異常)を打ち切る。 */
    @Test
    fun defeat_clearsAllAndStops() {
        val (after, events) = field(ail(StatusAilment.BURN), ail(StatusAilment.CURSE), ail(StatusAilment.BLEED), hp = 5).run()

        assertEquals(0, after.pawn(1).currentHp)
        assertTrue(after.pawn(1).ailments.isEmpty())
        assertEquals(80, after.pawn(1).currentMp)
        assertEquals(listOf("HpDamage", "Defeated"), events.map { it::class.simpleName })
        assertEquals(5, (events.first() as TurnEndEvent.HpDamage).damage)
    }

    /** 感染のダメージは最大HPの1/分母で、分母は16から2ずつ減り、最小2で止まる。 */
    @Test
    fun infection_denominatorShrinksToTwo() {
        val infected = testAlly(1, hp = 160, mp = 80).withAilments(ail(StatusAilment.INFECTION, turns = 20))
        var f = BattleField(allies = listOf(infected), enemies = listOf(testEnemy(4)), turnQueue = listOf(4, 1))
        val damages = mutableListOf<Int>()
        repeat(10) {
            val (next, events) = f.processTurnEndAilments(1, SequenceRandom())
            damages += events.filterIsInstance<TurnEndEvent.HpDamage>().single().damage
            f = next.copy(allies = next.allies.map { it.copy(currentHp = 160) })
        }

        assertEquals(listOf(10, 11, 13, 16, 20, 26, 40, 80, 80, 80), damages)
        assertEquals(2, f.pawn(1).ailments.single().infectionDenominator)
    }

    /** 感染は30%で同じ陣営の生存・未感染・石化していない個体へうつり、伝染先は分母16・残りターンは伝染元の減算前の値。 */
    @Test
    fun infection_spreadsToSameSideCandidate() {
        val f = BattleField(
            allies = listOf(
                testAlly(1, hp = 160).withAilments(ail(StatusAilment.INFECTION, turns = 4, denominator = 8)),
                testAlly(2).withAilments(ail(StatusAilment.INFECTION)),
                testAlly(3).withAilments(ail(StatusAilment.PETRIFY)),
                testAlly(4).copy(currentHp = 0),
                testAlly(5),
            ),
            enemies = listOf(testEnemy(9)),
            turnQueue = listOf(9, 1),
        )
        val random = SequenceRandom(ints = listOf(29, 0))

        val (after, events) = f.processTurnEndAilments(1, random)

        val spread = events.filterIsInstance<TurnEndEvent.Spread>().single()
        assertEquals(1 to 5, spread.fromFpid to spread.toFpid)
        val target = after.pawn(5).ailments.single()
        assertEquals(StatusAilment.INFECTION, target.ailment)
        assertEquals(4, target.remainingTurns)
        assertEquals(16, target.infectionDenominator)
        assertEquals(3, after.pawn(1).ailments.single().remainingTurns)
        assertEquals(6, after.pawn(1).ailments.single().infectionDenominator)
        assertEquals(listOf(100, 1), random.untils)
        // 敵には伝染しない。
        assertTrue(after.pawn(9).ailments.isEmpty())
    }

    /** 伝染判定に外れたとき(30以上)は、伝染先の選択の乱数を引かない。候補がいなければ乱数を一切引かない。 */
    @Test
    fun infection_randomDrawRules() {
        val base = BattleField(
            allies = listOf(testAlly(1, hp = 160).withAilments(ail(StatusAilment.INFECTION)), testAlly(2)),
            enemies = listOf(testEnemy(9)),
            turnQueue = listOf(9, 1),
        )
        val missed = SequenceRandom(ints = listOf(30))
        val (after, events) = base.processTurnEndAilments(1, missed)
        assertTrue(events.none { it is TurnEndEvent.Spread })
        assertTrue(after.pawn(2).ailments.isEmpty())
        assertEquals(listOf(100), missed.untils)

        val noCandidate = base.copy(allies = base.allies.filter { it.fpid == 1 })
        val none = SequenceRandom()
        noCandidate.processTurnEndAilments(1, none)
        assertTrue(none.untils.isEmpty())
    }

    /** 手番終了([endTurnWithEvents])が、状態異常の出来事を返し、行動不能で飛ばされた手番も1手番として数える。 */
    @Test
    fun endTurn_returnsEventsAndCountsSkippedTurn() {
        val f = BattleField(
            allies = listOf(testAlly(1).withAilments(ail(StatusAilment.SLEEP, turns = 1), ail(StatusAilment.POISON, turns = 2))),
            enemies = listOf(testEnemy(4)),
            turnQueue = listOf(1, 4),
        )

        val (after, result) = f.endTurnWithEvents(1, SequenceRandom())

        assertEquals(1, after.pawn(1).turnCount)
        assertEquals(listOf(StatusAilment.POISON), after.pawn(1).ailmentKinds())
        assertEquals(listOf("HpDamage", "Cured"), result.events.map { it::class.simpleName })
        assertEquals((result.events[0] as TurnEndEvent.HpDamage).field.pawn(1).currentHp, 88)
    }

    /** 状態異常のダメージで最後の味方が倒れたら、既存どおり勝敗が決まり手番は空になる。 */
    @Test
    fun endTurn_defeatDecidesOutcome() {
        val f = BattleField(
            allies = listOf(testAlly(1, hp = 3).withAilments(ail(StatusAilment.BURN))),
            enemies = listOf(testEnemy(4)),
            turnQueue = listOf(1, 4),
        )

        val (after, result) = f.endTurnWithEvents(1, SequenceRandom())

        assertEquals(jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleOutcome.Defeat, after.outcome)
        assertTrue(after.turnQueue.isEmpty())
        assertTrue(result.events.last() is TurnEndEvent.Defeated)
    }

    /** 戦闘不能になった個体(ダメージ・即死・反射を問わずHP0)の状態異常は全解除される。 */
    @Test
    fun applyDamage_clearsAilmentsOnDefeat() {
        val f = field(ail(StatusAilment.POISON), ail(StatusAilment.SLEEP))

        val dead = f.applyDamageForTest(1, 1000)
        val alive = f.applyDamageForTest(1, 10)

        assertTrue(dead.pawn(1).ailments.isEmpty())
        assertEquals(2, alive.pawn(1).ailments.size)
    }

    private fun BattleField.applyDamageForTest(fpid: Int, damage: Int) =
        applyDamage(fpid, damage)
}
