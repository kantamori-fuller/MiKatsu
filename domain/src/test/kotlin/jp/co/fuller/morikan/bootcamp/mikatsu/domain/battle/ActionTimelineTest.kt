package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** [ActionTimeline]の行動順の進行・確定・手番終了を検証するテスト。 */
class ActionTimelineTest {

    /**
     * 味方(FPID=1)と敵(FPID=2)が交互ではなく速度比どおりに行動し続けたときの、それぞれの行動回数を数える。
     *
     * @param allyAgility 味方の素早さ。
     * @param enemyAgility 敵の素早さ。
     * @param slowActions 遅い側の行動回数がこの値に達するまで進める。
     * @return 味方・敵それぞれの行動回数。
     */
    private fun simulate(allyAgility: Int, enemyAgility: Int, slowActions: Int): Pair<Int, Int> {
        val random = Random(0)
        var field = BattleField(
            allies = listOf(testAlly(1, agility = allyAgility, hp = Int.MAX_VALUE)),
            enemies = listOf(testEnemy(2, agility = enemyAgility, hp = Int.MAX_VALUE)),
        ).advanceToNextTurn(random)
        val counts = mutableMapOf(1 to 0, 2 to 0)
        val slowFpid = if (allyAgility < enemyAgility) 1 else 2
        while (counts.getValue(slowFpid) < slowActions) {
            val actor = checkNotNull(field.currentActorFpid)
            counts[actor] = counts.getValue(actor) + 1
            field = field.endTurn(actor, random)
        }
        return counts.getValue(1) to counts.getValue(2)
    }

    /** 素早さ6倍の個体は、約2倍の回数行動すること。 */
    @Test
    fun simulation_sixTimesAgility_actsAboutTwiceAsOften() {
        val (slow, fast) = simulate(allyAgility = 100, enemyAgility = 600, slowActions = 100)
        assertEquals(200.0, fast.toDouble(), 200.0 * 0.02)
        assertEquals(100, slow)
    }

    /** 素早さ3倍の個体は、約1.53倍の回数行動すること。 */
    @Test
    fun simulation_threeTimesAgility_actsAbout1Point53TimesAsOften() {
        val (slow, fast) = simulate(allyAgility = 100, enemyAgility = 300, slowActions = 100)
        assertEquals(153.0, fast.toDouble(), 153.0 * 0.02)
        assertEquals(100, slow)
    }

    /** 最初の手番は素早さが最も高い個体になること。 */
    @Test
    fun advanceToNextTurn_firstActorIsFastest() {
        val field = BattleField(
            allies = listOf(testAlly(1, agility = 100), testAlly(2, agility = 500)),
            enemies = listOf(testEnemy(3, agility = 300)),
        ).advanceToNextTurn(Random(0))

        assertEquals(listOf(2), field.turnQueue)
    }

    /** 素早さが同じなら幸運値の高い方が先になること(敵味方の区別なし)。 */
    @Test
    fun advanceToNextTurn_sameAgility_higherLuckFirst() {
        val field = BattleField(
            allies = listOf(testAlly(1, agility = 100, luck = 5)),
            enemies = listOf(testEnemy(2, agility = 100, luck = 50)),
        ).advanceToNextTurn(Random(0))

        assertEquals(listOf(2, 1), field.turnQueue)
    }

    /** 素早さも幸運値も同じなら、固定seedの結果で順番が決まり、turnQueueに確定すること。 */
    @Test
    fun advanceToNextTurn_fullTie_orderIsDecidedBySeedAndFixed() {
        val base = BattleField(
            allies = listOf(testAlly(1), testAlly(2)),
            enemies = listOf(testEnemy(3), testEnemy(4)),
        )
        val expected = listOf(1, 2, 3, 4).shuffled(Random(42))

        val field = base.advanceToNextTurn(Random(42))
        assertEquals(expected, field.turnQueue)

        // 計算し直しても、くじは引き直されず順番は確定したまま。
        assertEquals(expected, field.advanceToNextTurn(Random(7)).turnQueue)
    }

    /** 戦闘不能の個体は手番待ちに入らず、手番も回ってこないこと。 */
    @Test
    fun advanceToNextTurn_defeatedPawnNeverActs() {
        val random = Random(0)
        var field = BattleField(
            allies = listOf(testAlly(1, agility = 100), testAlly(2, agility = 900).let { it.copy(currentHp = 0) }),
            enemies = listOf(testEnemy(3, agility = 100)),
        ).advanceToNextTurn(random)

        repeat(20) {
            assertFalse(2 in field.turnQueue)
            field = field.endTurn(checkNotNull(field.currentActorFpid), random)
        }
    }

    /** 手番待ちの個体が戦闘不能になったら、手番待ちから外れること。 */
    @Test
    fun advanceToNextTurn_defeatedInQueue_isRemoved() {
        val field = BattleField(
            allies = listOf(testAlly(1), testAlly(2)),
            enemies = listOf(testEnemy(3, agility = 1)),
            turnQueue = listOf(1, 2),
        )
        val defeated = field.copy(allies = field.allies.map { if (it.fpid == 1) it.copy(currentHp = 0) else it })

        assertEquals(listOf(2), defeated.advanceToNextTurn(Random(0)).turnQueue)
    }

    /** endTurnで残り距離がBASE_DISTANCEに戻り、turnCountが+1されること。 */
    @Test
    fun endTurn_resetsDistanceAndCountsTurn() {
        // 2体が同時に手番待ちの状態から先頭が終えると、次の手番決定で距離が進まないため値を直接確認できる。
        val field = BattleField(
            allies = listOf(testAlly(1).copy(remainingDistance = 0.0), testAlly(2).copy(remainingDistance = 0.0)),
            enemies = listOf(testEnemy(3)),
            turnQueue = listOf(1, 2),
        )

        val after = field.endTurn(1, Random(0))

        assertEquals(ActionTimeline.BASE_DISTANCE, after.allies[0].remainingDistance, 0.0)
        assertEquals(1, after.allies[0].turnCount)
        assertEquals(0, after.allies[1].turnCount)
        assertEquals(listOf(2), after.turnQueue)
    }

    /** 手番の個体以外のFPIDでendTurnしても何も起きないこと。 */
    @Test
    fun endTurn_nonActor_changesNothing() {
        val field = BattleField(
            allies = listOf(testAlly(1, agility = 500)),
            enemies = listOf(testEnemy(2, agility = 100)),
        ).advanceToNextTurn(Random(0))

        assertSame(field, field.endTurn(2, Random(0)))
    }

    /** 勝敗が決まったらturnQueueが空になること。 */
    @Test
    fun advanceToNextTurn_outcomeDecided_clearsQueue() {
        val field = BattleField(
            allies = listOf(testAlly(1)),
            enemies = listOf(testEnemy(2).copy(currentHp = 0)),
            turnQueue = listOf(1),
        )

        val after = field.advanceToNextTurn(Random(0))

        assertEquals(BattleOutcome.Victory, after.outcome)
        assertTrue(after.turnQueue.isEmpty())
        assertNull(after.currentActorFpid)
    }
}
