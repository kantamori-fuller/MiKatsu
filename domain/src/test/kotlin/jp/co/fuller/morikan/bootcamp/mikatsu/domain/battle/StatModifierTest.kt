package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleStat
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.StatModifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** バフ・デバフの持続(手番終了での経過・付与された手番の扱い)と、ダメージ計算・行動順への反映を検証するテスト。 */
class StatModifierTest {

    private val random = Random(0)

    /** 味方1(素早さ100・手番中)と敵2(素早さ1)。味方が10回行動しても敵の手番は来ない。 */
    private fun field(): BattleField = BattleField(
        allies = listOf(testAlly(1, agility = 9999)),
        enemies = listOf(testEnemy(2, agility = 1, hp = 10000)),
        turnQueue = listOf(1),
    )

    /** 手番を終えて、次の手番も同じ個体になるまで進める(相手が遅いので味方が連続で動く)。 */
    private fun BattleField.endOwnTurn(fpid: Int): BattleField = endTurn(fpid, random)

    /** 自分にかけた手番は数えず、次の手番から数えて10回目の手番終了で切れること。 */
    @Test
    fun selfBuff_notCountedInCastTurn_expiresAfterTenOwnTurnEnds() {
        var f = field().addStatModifier(1, BattleStat.ATTACK, 1, 10)
        assertTrue(f.allies[0].statModifiers.single().grantedDuringOwnTurn)

        f = f.endOwnTurn(1) // かけた手番の終了: 減らない
        assertEquals(10, f.allies[0].statModifiers.single().remainingTurns)
        assertEquals(false, f.allies[0].statModifiers.single().grantedDuringOwnTurn)

        repeat(9) { f = f.endOwnTurn(1) }
        assertEquals(1, f.allies[0].statModifiers.single().remainingTurns)

        f = f.endOwnTurn(1) // 10回目
        assertTrue(f.allies[0].statModifiers.isEmpty())
    }

    /** 他人の手番中にかけられた分は、対象の次の手番終了から数え、対象の手番終了で減ること。 */
    @Test
    fun debuffFromOthersTurn_countsFromTargetsNextTurnEnd() {
        // 敵2の手番中に、味方1へかける。
        var f = field().copy(turnQueue = listOf(2)).addStatModifier(1, BattleStat.DEFENSE, -1, 2)
        assertEquals(false, f.allies[0].statModifiers.single().grantedDuringOwnTurn)

        f = f.endTurn(2, random) // 敵の手番終了では味方の分は減らない
        assertEquals(2, f.allies[0].statModifiers.single().remainingTurns)

        // 味方1の手番終了で減る
        f = f.copy(turnQueue = listOf(1)).endOwnTurn(1)
        assertEquals(1, f.allies[0].statModifiers.single().remainingTurns)
        f = f.copy(turnQueue = listOf(1)).endOwnTurn(1)
        assertTrue(f.allies[0].statModifiers.isEmpty())
    }

    /** かけた分ごとに別エントリで、別々に切れること。 */
    @Test
    fun stacking_entriesExpireSeparately() {
        var f = field().addStatModifier(1, BattleStat.ATTACK, 1, 1).addStatModifier(1, BattleStat.ATTACK, 2, 3)
        assertEquals(2, f.allies[0].statModifiers.size)
        assertEquals(3, f.allies[0].effectiveStage(BattleStat.ATTACK))

        f = f.endOwnTurn(1) // 付与手番
        f = f.endOwnTurn(1) // 1つ目が切れる
        assertEquals(listOf(2), f.allies[0].statModifiers.map { it.stages })
        assertEquals(2, f.allies[0].effectiveStage(BattleStat.ATTACK))
    }

    /** 上限を超えた分も記録として残り、有効段階は+12のまま、古い分が切れても残りの合計から求まること。 */
    @Test
    fun overCap_recordedAndKeptWhenOldEntryExpires() {
        var f = field()
            .addStatModifier(1, BattleStat.ATTACK, 12, 1)
            .addStatModifier(1, BattleStat.ATTACK, 1, 5)
        assertEquals(2, f.allies[0].statModifiers.size)
        assertEquals(12, f.allies[0].effectiveStage(BattleStat.ATTACK))

        f = f.endOwnTurn(1)
        f = f.endOwnTurn(1) // +12の分が切れる
        assertEquals(listOf(1), f.allies[0].statModifiers.map { it.stages })
        assertEquals(1, f.allies[0].effectiveStage(BattleStat.ATTACK))
    }

    /** 有効なステータスは基礎値のまま残り、effectiveStatだけが倍率を反映すること。 */
    @Test
    fun effectiveStat_appliesStage_baseStaysSame() {
        val ally = testAlly(1, attack = 100, defense = 100, agility = 100, luck = 10)
            .copy(statModifiers = listOf(StatModifier(BattleStat.ATTACK, 4, 5), StatModifier(BattleStat.LUCK, -2, 5)))

        assertEquals(100, ally.attack)
        assertEquals(200, ally.effectiveStat(BattleStat.ATTACK))
        assertEquals(10, ally.luck)
        assertEquals(6, ally.effectiveStat(BattleStat.LUCK)) // 10 × 2/3 = 6.66 → 6
        assertEquals(100, ally.effectiveStat(BattleStat.DEFENSE))
    }

    /** 有効な破壊力・耐久力でダメージが変わること(破壊力+4は2倍、耐久力-6は2/5倍)。 */
    @Test
    fun damage_usesEffectiveAttackAndDefense() {
        val plain = BattleField(allies = listOf(testAlly(1)), enemies = listOf(testEnemy(2, hp = 10000)), turnQueue = listOf(1))
        val baseDamage = checkNotNull(plain.performAttack(1, 2, TEST_NORMAL_ATTACK, random)).second.hits.single().damage

        val buffed = plain.copy(allies = listOf(testAlly(1, statModifiers = listOf(StatModifier(BattleStat.ATTACK, 4, 5)))))
        val buffedDamage = checkNotNull(buffed.performAttack(1, 2, TEST_NORMAL_ATTACK, random)).second.hits.single().damage
        assertEquals(DamageFormula.calculate(100, 200, 100), buffedDamage)
        assertTrue(buffedDamage > baseDamage)

        val debuffed = plain.copy(enemies = listOf(testEnemy(2, hp = 10000, statModifiers = listOf(StatModifier(BattleStat.DEFENSE, -6, 5)))))
        val debuffedDamage = checkNotNull(debuffed.performAttack(1, 2, TEST_NORMAL_ATTACK, random)).second.hits.single().damage
        assertEquals(DamageFormula.calculate(100, 100, 40), debuffedDamage)
    }

    /** 有効な俊敏で次の手番までの時間が変わり、行動順が入れ替わること(残り距離は保存のまま)。 */
    @Test
    fun agility_changesTurnOrder() {
        val base = BattleField(allies = listOf(testAlly(1, agility = 100)), enemies = listOf(testEnemy(2, agility = 150)))
        assertEquals(2, base.advanceToNextTurn(random).currentActorFpid)

        val slowed = base.copy(enemies = listOf(testEnemy(2, agility = 150, statModifiers = listOf(StatModifier(BattleStat.AGILITY, -4, 5)))))
        assertEquals(1, slowed.advanceToNextTurn(random).currentActorFpid)
        // timeUntilTurnも有効な俊敏を使う。
        val before = base.timeUntilTurn(2)!!
        val after = slowed.timeUntilTurn(2)!!
        assertTrue(after > before)
    }

    /** 同時に手番が来たときの幸運の比較に有効な幸運が使われること。 */
    @Test
    fun luck_effectiveValueDecidesTieOrder() {
        val f = BattleField(
            allies = listOf(testAlly(1, agility = 100, luck = 10)),
            enemies = listOf(testEnemy(2, agility = 100, luck = 8, statModifiers = listOf(StatModifier(BattleStat.LUCK, 4, 5)))),
        )

        assertEquals(listOf(2, 1), f.advanceToNextTurn(random).turnQueue)
    }
}
