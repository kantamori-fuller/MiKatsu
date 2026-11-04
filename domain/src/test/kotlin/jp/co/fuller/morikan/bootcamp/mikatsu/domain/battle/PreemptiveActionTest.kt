package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleAlly
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleStat
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BreakState
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.SkillCooldown
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.StatModifier
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkill
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 先制行動の予約の条件、行動順への割り込み、並び順、前借り、先制の手番の扱い、取り消し、勝敗決定時の破棄を検証するテスト。 */
class PreemptiveActionTest {

    /** 優先度5の先制スキル(クイックシュートと同じ内容でIDだけ違う)。 */
    private val highPriority: ActiveSkill = TEST_QUICK_SHOT.copy(id = 8, name = "優先度5", preemptivePriority = 5)

    /** 優先度9の先制スキル。 */
    private val topPriority: ActiveSkill = TEST_QUICK_SHOT.copy(id = 9, name = "優先度9", preemptivePriority = 9)

    /**
     * 先制スキルを使える味方を生成する(MP100・TP50。前衛のスロット[fpid]-1)。
     *
     * @param fpid FPID。
     * @param agility 素早さ。
     * @param luck 幸運。
     * @param skills 設定しているアクティブスキル。
     * @param slot スロット番号。
     * @param modifiers かけられているバフ・デバフ。
     * @return 味方個体。
     */
    private fun ally(
        fpid: Int,
        agility: Int = 100,
        luck: Int = 0,
        skills: List<ActiveSkill> = listOf(TEST_QUICK_SHOT),
        slot: Int = fpid - 1,
        modifiers: List<StatModifier> = emptyList(),
    ): BattleAlly = testAlly(fpid, agility = agility, luck = luck, mp = 100, slot = slot, activeSkills = skills, statModifiers = modifiers)
        .copy(currentTp = 50)

    /** 味方1(現在の行動者)・2・3・4と、とても遅い敵10(前衛)の戦闘フィールド。 */
    private fun field(
        queue: List<Int> = listOf(1),
        allies: List<BattleAlly> = listOf(ally(1), ally(2), ally(3), ally(4)),
    ) = BattleField(
        allies = allies,
        enemies = listOf(testEnemy(10, agility = 1, hp = 100000)),
        turnQueue = queue,
    )

    private fun BattleField.ally(fpid: Int): BattleAlly = allies.first { it.fpid == fpid }

    private fun BattleField.updateAlly(fpid: Int, transform: (BattleAlly) -> BattleAlly): BattleField =
        copy(allies = allies.map { if (it.fpid == fpid) transform(it) else it })

    private fun BattleField.reserve(vararg fpids: Int): BattleField =
        fpids.fold(this) { f, id -> checkNotNull(f.reservePreemptive(id)) { "予約できない: $id" } }

    // ---- 予約できる条件 ----

    /** 現在の行動者ではない、使える先制スキルを持つ生きた味方は予約できること。 */
    @Test
    fun canReserve_normalCase() {
        assertTrue(field().canReservePreemptive(2))
    }

    /** 予約は予約順に記録するだけで、行動順(turnQueue)はまだ変えないこと。 */
    @Test
    fun reserve_recordsInOrder_withoutChangingQueue() {
        val reserved = field(queue = listOf(1, 10)).reserve(3, 2)

        assertEquals(listOf(3, 2), reserved.pendingPreemptive)
        assertEquals(listOf(1, 10), reserved.turnQueue)
        assertTrue(reserved.preemptiveTurns.isEmpty())
    }

    /** 現在の行動者・敵・存在しない個体は予約できないこと。 */
    @Test
    fun cannotReserve_currentActor_enemy_unknown() {
        val f = field()

        assertFalse(f.canReservePreemptive(1))
        assertFalse(f.canReservePreemptive(10))
        assertFalse(f.canReservePreemptive(99))
        assertNull(f.reservePreemptive(1))
    }

    /** 戦闘不能・ブレイク中の味方は予約できないこと。 */
    @Test
    fun cannotReserve_defeated_broken() {
        val f = field().updateAlly(2) { it.copy(currentHp = 0) }.updateAlly(3) { it.copy(breakState = BreakState.BROKEN) }

        assertFalse(f.canReservePreemptive(2))
        assertFalse(f.canReservePreemptive(3))
        assertTrue(f.canReservePreemptive(4))
    }

    /** すでに予約している個体は、もう一度予約できないこと。 */
    @Test
    fun cannotReserve_alreadyReserved() {
        val f = field().reserve(2)

        assertFalse(f.canReservePreemptive(2))
        assertNull(f.reservePreemptive(2))
    }

    /** 先制の手番として行動順に入っている個体(現在の行動者でなくても)は予約できないこと。 */
    @Test
    fun cannotReserve_alreadyInQueueAsPreemptiveTurn() {
        val f = field(queue = listOf(1, 2)).copy(preemptiveTurns = setOf(2))

        assertFalse(f.canReservePreemptive(2))
    }

    /** 勝敗が決まっていれば予約できないこと。 */
    @Test
    fun cannotReserve_afterOutcomeDecided() {
        val won = field().copy(enemies = listOf(testEnemy(10).copy(currentHp = 0)))

        assertFalse(won.canReservePreemptive(2))
    }

    /** 先制スキルを持たない味方は予約できないこと。 */
    @Test
    fun cannotReserve_withoutPreemptiveSkill() {
        val f = field(allies = listOf(ally(1), ally(2, skills = listOf(TEST_DIA, TEST_NORMAL_ATTACK))))

        assertFalse(f.canReservePreemptive(2))
    }

    /** 先制スキルのCTが明けていなければ予約できず、他に使える先制スキルがあれば予約できること。 */
    @Test
    fun cannotReserve_whenPreemptiveSkillOnCooldown_unlessAnotherIsUsable() {
        val cooldown = SkillCooldown(skillId = 7, remainingTurns = 1)
        val onlyOne = field().updateAlly(2) { it.copy(skillCooldowns = listOf(cooldown)) }
        val another = field(allies = listOf(ally(1), ally(2, skills = listOf(TEST_QUICK_SHOT, highPriority))))
            .updateAlly(2) { it.copy(skillCooldowns = listOf(cooldown)) }

        assertFalse(onlyOne.canReservePreemptive(2))
        assertTrue(another.canReservePreemptive(2))
    }

    /** コストを払えなければ予約できないこと(MP不足は代替できない)。 */
    @Test
    fun cannotReserve_whenCannotAfford() {
        val f = field().updateAlly(2) { it.copy(currentMp = 19) }

        assertFalse(f.canReservePreemptive(2))
        assertTrue(f.updateAlly(2) { it.copy(currentMp = 20) }.canReservePreemptive(2))
    }

    /** 選べる相手がいなければ予約できないこと(前衛の味方がいる後衛の味方からは、レンジ1のスキルは届かない)。 */
    @Test
    fun cannotReserve_whenNoSelectableTarget() {
        val rangeOne = TEST_QUICK_SHOT.copy(range = SkillRange.RANGE_1)
        val f = field(allies = listOf(ally(1), ally(2, skills = listOf(rangeOne), slot = 3)))

        assertFalse(f.canReservePreemptive(2))
    }

    // ---- 割り込み ----

    /** 割り込みは、現在の行動者の手番の直後・残っている同時手番より前に入り、先制の印が付くこと。予約は空になる。 */
    @Test
    fun interrupt_afterCurrentActor_beforeRemainingSimultaneousTurns() {
        val f = field(queue = listOf(1, 10)).reserve(2)

        val after = f.endTurn(1, SequenceRandom())

        assertEquals(listOf(2, 10), after.turnQueue)
        assertEquals(2, after.currentActorFpid)
        assertTrue(after.isPreemptiveTurn)
        assertEquals(setOf(2), after.preemptiveTurns)
        assertTrue(after.pendingPreemptive.isEmpty())
    }

    /** 予約がなければ、今までどおり行動順は変わらないこと。 */
    @Test
    fun noReservation_behavesLikeBefore() {
        val after = field(queue = listOf(1, 10)).endTurn(1, SequenceRandom())

        assertEquals(10, after.currentActorFpid)
        assertFalse(after.isPreemptiveTurn)
        assertTrue(after.preemptiveTurns.isEmpty())
    }

    /** 敵の手番の最中に予約しても、その敵の手番が終わるときに割り込むこと。 */
    @Test
    fun interrupt_duringEnemyTurn() {
        val f = field(queue = listOf(10)).reserve(3)

        val after = f.endTurn(10, SequenceRandom())

        assertEquals(3, after.currentActorFpid)
        assertTrue(after.isPreemptiveTurn)
    }

    /** 先制の手番の最中に予約された分は、その手番の終了で、残っている先制の手番より前に入ること。 */
    @Test
    fun interrupt_reservedDuringPreemptiveTurn_goesBeforeRemainingPreemptiveTurns() {
        // 2と3が先制の手番に入る(素早さ順で2→3)。
        var f = field(allies = listOf(ally(1), ally(2, agility = 200), ally(3, agility = 150), ally(4)), queue = listOf(1)).reserve(2, 3)
        f = f.endTurn(1, SequenceRandom())
        assertEquals(listOf(2, 3), f.turnQueue)

        // 2の先制の手番中に4が予約する。
        f = f.reserve(4).endTurn(2, SequenceRandom())

        assertEquals(listOf(4, 3), f.turnQueue)
        assertEquals(setOf(4, 3), f.preemptiveTurns)
        assertEquals(4, f.currentActorFpid)
    }

    /** 先制の手番が終わると、その個体の先制の印は外れること。 */
    @Test
    fun endOfPreemptiveTurn_removesMark() {
        val f = field(queue = listOf(1)).reserve(2).endTurn(1, SequenceRandom())

        val after = f.endTurn(2, SequenceRandom())

        assertTrue(after.preemptiveTurns.isEmpty())
        assertFalse(after.isPreemptiveTurn)
    }

    // ---- 並び順 ----

    /** 優先度の高い個体が、素早さに関わらず先になること(予約した順でもない)。 */
    @Test
    fun order_priorityFirst() {
        val f = field(
            allies = listOf(ally(1), ally(2, agility = 500), ally(3, agility = 1, skills = listOf(highPriority))),
        ).reserve(2, 3)

        assertEquals(listOf(3, 2), f.endTurn(1, SequenceRandom()).turnQueue)
    }

    /** 優先度は、複数の先制スキルの中で一番高い値が使われること。 */
    @Test
    fun order_usesHighestPriorityAmongSkills() {
        val f = field(
            allies = listOf(
                ally(1),
                ally(2, agility = 1, skills = listOf(TEST_QUICK_SHOT, topPriority)),
                ally(3, agility = 500, skills = listOf(highPriority)),
            ),
        ).reserve(3, 2)

        assertEquals(listOf(2, 3), f.endTurn(1, SequenceRandom()).turnQueue)
    }

    /** 優先度が同じなら素早さの高い順になること。 */
    @Test
    fun order_thenAgility() {
        val f = field(allies = listOf(ally(1), ally(2, agility = 100), ally(3, agility = 200))).reserve(2, 3)

        assertEquals(listOf(3, 2), f.endTurn(1, SequenceRandom()).turnQueue)
    }

    /** 素早さの比較はバフ・デバフ後の値(素早さ+4段階で2倍)で行うこと。 */
    @Test
    fun order_usesEffectiveAgility() {
        val buff = listOf(StatModifier(BattleStat.AGILITY, stages = 4, remainingTurns = 3))
        val f = field(allies = listOf(ally(1), ally(2, agility = 100, modifiers = buff), ally(3, agility = 150))).reserve(3, 2)

        assertEquals(listOf(2, 3), f.endTurn(1, SequenceRandom()).turnQueue)
    }

    /** 優先度・素早さが同じなら幸運の高い順になること(バフ後の値)。 */
    @Test
    fun order_thenLuck_usingEffectiveLuck() {
        val luckBuff = listOf(StatModifier(BattleStat.LUCK, stages = 4, remainingTurns = 3))
        val plain = field(allies = listOf(ally(1), ally(2, luck = 5), ally(3, luck = 10))).reserve(2, 3)
        val buffed = field(allies = listOf(ally(1), ally(2, luck = 6, modifiers = luckBuff), ally(3, luck = 10))).reserve(3, 2)

        assertEquals(listOf(3, 2), plain.endTurn(1, SequenceRandom()).turnQueue)
        assertEquals(listOf(2, 3), buffed.endTurn(1, SequenceRandom()).turnQueue)
    }

    /** すべて同じならくじで決まり、くじは手番開始のTPの乱数より前に引かれること。 */
    @Test
    fun order_thenLottery_drawnBeforeTurnStartTp() {
        val keep = SequenceRandom(ints = listOf(1))
        val swap = SequenceRandom(ints = listOf(0))
        val f = field(allies = listOf(ally(1), ally(2), ally(3))).reserve(2, 3)

        assertEquals(listOf(2, 3), f.endTurn(1, keep).turnQueue)
        assertEquals(listOf(3, 2), f.endTurn(1, swap).turnQueue)
        // くじ(幅2) → 手番開始のTP(幅3)の順に引く。
        assertEquals(listOf(2, 3), keep.untils)
    }

    // ---- 前借り ----

    /** すでに通常の手番として並んでいた個体は、通常の手番が取り除かれて先制の手番だけになること。 */
    @Test
    fun borrow_removesNormalTurnFromQueue() {
        val f = field(queue = listOf(1, 2, 10)).reserve(2)

        val after = f.endTurn(1, SequenceRandom())

        assertEquals(listOf(2, 10), after.turnQueue)
        assertEquals(setOf(2), after.preemptiveTurns)

        // 先制の手番が終わったあと、通常の手番が改めて来ることはない(10の手番になる)。
        assertEquals(10, after.endTurn(2, SequenceRandom()).currentActorFpid)
    }

    // ---- 先制の手番は通常の手番と同じ扱い ----

    /** 先制の手番の開始で、防御の解除と手番開始のTP(6〜8)を受け取ること。 */
    @Test
    fun preemptiveTurn_releasesDefense_andGainsTurnStartTp() {
        val f = field().updateAlly(2) { it.copy(isDefending = true) }.reserve(2)

        val after = f.endTurn(1, SequenceRandom(ints = listOf(2)))

        assertFalse(after.ally(2).isDefending)
        assertEquals(50 + 8, after.ally(2).currentTp)
    }

    /** 先制の手番の終了で、残り距離のリセット・手番の回数+1・ブレイクの進行・バフ・デバフの経過・CTの経過が行われること。 */
    @Test
    fun preemptiveTurnEnd_isTreatedLikeNormalTurnEnd() {
        val modifier = StatModifier(BattleStat.ATTACK, stages = 1, remainingTurns = 2)
        // 素早さ1の個体にして、先制の手番が終わった直後に次の手番が来ないようにする(残り距離のリセットを確認するため)。
        var f = field(allies = listOf(ally(1), ally(2, agility = 1), ally(3), ally(4)))
            .updateAlly(2) {
                it.copy(
                    remainingDistance = 3000.0,
                    breakState = BreakState.NONE,
                    statModifiers = listOf(modifier),
                    skillCooldowns = listOf(SkillCooldown(skillId = 99, remainingTurns = 2)),
                )
            }
            .reserve(2)
        f = f.endTurn(1, SequenceRandom())
        assertEquals(2, f.currentActorFpid)

        f = f.endTurn(2, SequenceRandom())

        val ally2 = f.ally(2)
        // 3000から10000へ戻り、その後に他の個体の手番までの時間だけ進んだ分(素早さ1なので高々数千)しか減っていない。
        assertTrue(ally2.remainingDistance > 3000.0)
        assertEquals(1, ally2.turnCount)
        assertEquals(1, ally2.statModifiers.single().remainingTurns)
        assertEquals(1, ally2.skillCooldowns.single().remainingTurns)
    }

    /** ブレイクの状態も、先制の手番の終了で1段階進むこと(ブレイク中は予約できないため、立ち上がり中の個体で確認する)。 */
    @Test
    fun preemptiveTurnEnd_progressesRecovering() {
        val f = field().updateAlly(2) { it.copy(breakState = BreakState.RECOVERING) }.reserve(2)

        val after = f.endTurn(1, SequenceRandom()).endTurn(2, SequenceRandom())

        assertEquals(BreakState.NONE, after.ally(2).breakState)
    }

    /** 先制の手番でクイックシュートを使うと、使った手番の終わりではCTが減らないこと(先制の手番も1手番として数える)。 */
    @Test
    fun preemptiveTurn_countsAsOneTurnForCooldown() {
        var f = field().reserve(2).endTurn(1, SequenceRandom())

        f = checkNotNull(f.performAttack(2, 10, TEST_QUICK_SHOT, SequenceRandom())).first
        assertEquals(listOf(SkillCooldown(7, 1, startedDuringOwnTurn = true)), f.ally(2).skillCooldowns)

        f = f.endTurn(2, SequenceRandom())
        assertEquals(listOf(SkillCooldown(7, 1, startedDuringOwnTurn = false)), f.ally(2).skillCooldowns)
        // CT中なので、次に予約しようとしても予約できない。
        assertFalse(f.canReservePreemptive(2))
    }

    // ---- 取り消し ----

    /**
     * 取り消された先制の手番の個体について、手番は使われず、個体の状態(TP・防御・残り距離・手番の回数・CTなど)が一切変わらないことを確認する。
     *
     * 手番を終えた直後の割り込みと、先頭での取り消し(手番開始のTPと防御の解除より前)だけを取り出して調べる。
     * 取り消し後の通常の行動順の決定は、このあと[advanceToNextTurnWithCancellations]が行うため、ここには含めない。
     *
     * @param before 予約した直後(割り込み前)のフィールドに、取り消しの原因を加えたもの。現在の行動者は味方1。
     * @param fpid 取り消されるはずの個体。
     */
    private fun assertCancelled(before: BattleField, fpid: Int) {
        val interrupted = before.copy(turnQueue = before.turnQueue.drop(1)).applyPreemptiveReservations(SequenceRandom())
        assertEquals(fpid, interrupted.currentActorFpid)

        val (after, cancelled) = interrupted.cancelInvalidPreemptiveTurns()

        assertEquals(listOf(fpid), cancelled)
        assertFalse(after.isPreemptiveTurn)
        assertTrue(after.preemptiveTurns.isEmpty())
        assertFalse(fpid in after.turnQueue)
        assertEquals(before.ally(fpid), after.ally(fpid))
    }

    /** 予約後に戦闘不能になっていたら、先制の手番は取り消され、手番を使わないこと。 */
    @Test
    fun cancel_defeated() {
        val f = field(queue = listOf(1)).updateAlly(2) { it.copy(isDefending = true, remainingDistance = 3000.0) }
            .reserve(2)
            .updateAlly(2) { it.copy(currentHp = 0) }

        assertCancelled(f, 2)
    }

    /** 予約後にブレイクしていたら、先制の手番は取り消され、手番を使わないこと。 */
    @Test
    fun cancel_broken() {
        val f = field(queue = listOf(1)).updateAlly(2) { it.copy(isDefending = true, remainingDistance = 3000.0) }
            .reserve(2)
            .updateAlly(2) { it.copy(breakState = BreakState.BROKEN) }

        assertCancelled(f, 2)
    }

    /** 予約後に使える先制スキルが無くなっていたら(CT・コスト・対象のどれでも)、先制の手番は取り消されること。 */
    @Test
    fun cancel_noUsablePreemptiveSkill_cooldownCostTarget() {
        val base = field(queue = listOf(1)).updateAlly(2) { it.copy(isDefending = true, remainingDistance = 3000.0) }.reserve(2)

        assertCancelled(base.updateAlly(2) { it.copy(skillCooldowns = listOf(SkillCooldown(7, 1))) }, 2)
        assertCancelled(base.updateAlly(2) { it.copy(currentMp = 0) }, 2)
        // 対象がいない: 敵が全員戦闘不能だと勝敗が決まるため、敵を後衛へ下げ、前衛の敵を残して届かないレンジ1のスキルで確認する。
        val rangeOne = TEST_QUICK_SHOT.copy(range = SkillRange.RANGE_1)
        val noTarget = field(queue = listOf(1), allies = listOf(ally(1), ally(2, skills = listOf(TEST_QUICK_SHOT), slot = 3)))
            .reserve(2)
            .updateAlly(2) { it.copy(activeSkills = listOf(rangeOne)) }
        assertCancelled(noTarget, 2)
    }

    /** 取り消された個体の通常の手番(前借りで取り除いたもの)は、行動ゲージが0のままなら普通に手番が来ること。 */
    @Test
    fun cancel_borrowedNormalTurn_stillComesNormally() {
        val f = field(queue = listOf(1, 2), allies = listOf(ally(1), ally(2).copy(remainingDistance = 0.0)))
            .reserve(2)
            .updateAlly(2) { it.copy(currentMp = 0) }

        val (after, cancelled) = f.endTurnWithCancellations(1, SequenceRandom())

        assertEquals(listOf(2), cancelled)
        assertEquals(2, after.currentActorFpid)
        assertFalse(after.isPreemptiveTurn)
    }

    /** 先頭の取り消しの後ろに有効な先制の手番があれば、その個体が先制の手番になり、TPなども有効な個体だけが受け取ること。 */
    @Test
    fun cancel_firstInvalid_nextValidStillActs() {
        val f = field(allies = listOf(ally(1), ally(2, agility = 200), ally(3, agility = 100)))
            .reserve(2, 3)
            .updateAlly(2) { it.copy(currentHp = 0) }

        val (after, cancelled) = f.endTurnWithCancellations(1, SequenceRandom(ints = listOf(2)))

        assertEquals(listOf(2), cancelled)
        assertEquals(3, after.currentActorFpid)
        assertTrue(after.isPreemptiveTurn)
        assertEquals(setOf(3), after.preemptiveTurns)
        assertEquals(50 + 8, after.ally(3).currentTp)
        assertEquals(50, after.ally(2).currentTp)
    }

    /** 2番目以降の先制の手番が戦闘不能になっても、先頭に来た時点で取り消されて報告されること(黙って消えない)。 */
    @Test
    fun cancel_laterPreemptiveTurnDefeated_reportedWhenReachingHead() {
        var f = field(allies = listOf(ally(1), ally(2, agility = 200), ally(3, agility = 100))).reserve(2, 3).endTurn(1, SequenceRandom())
        f = f.updateAlly(3) { it.copy(currentHp = 0) }

        val (after, cancelled) = f.endTurnWithCancellations(2, SequenceRandom())

        assertEquals(listOf(3), cancelled)
        assertFalse(after.isPreemptiveTurn)
    }

    // ---- 勝敗が決まったら破棄 ----

    /** 勝敗が決まったら、予約と先制の印はすべて破棄され、割り込みも起きないこと。 */
    @Test
    fun outcomeDecided_discardsReservationsAndMarks() {
        val f = field().reserve(2, 3).copy(enemies = listOf(testEnemy(10).copy(currentHp = 0)))

        val (after, cancelled) = f.endTurnWithCancellations(1, SequenceRandom())

        assertTrue(after.pendingPreemptive.isEmpty())
        assertTrue(after.preemptiveTurns.isEmpty())
        assertTrue(after.turnQueue.isEmpty())
        assertTrue(cancelled.isEmpty())
    }

    /** すでに先制の印が付いたキューがあっても、勝敗が決まったら印は破棄されること。 */
    @Test
    fun outcomeDecided_discardsExistingPreemptiveMarks() {
        val f = field().reserve(2, 3).endTurn(1, SequenceRandom())
            .copy(enemies = listOf(testEnemy(10).copy(currentHp = 0)))

        val after = f.endTurn(2, SequenceRandom())

        assertTrue(after.preemptiveTurns.isEmpty())
        assertTrue(after.turnQueue.isEmpty())
    }
}
