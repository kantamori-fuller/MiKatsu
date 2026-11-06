package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleStat
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BreakState
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.CriticalType
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.StatModifier
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.DefendUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.FakeBattleRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * 防御(30%カット・クリティカル無効化・成立条件・解除のタイミング)を検証するテスト。
 *
 * 攻撃100・耐久100・基礎威力100の1ヒットは24ダメージ(クリティカル150%なら36、ブレイク中は×1.3で31)。
 */
class DefenseTest {

    /** 味方1が防御中の状態で、敵2(手番)がそれを攻撃する戦場。ブレイクを起こさないよう敵のブレイクポイント倍率は0。 */
    private fun defendingAllyField(attackerAttack: Int = 100, allyMods: (Boolean) -> Boolean = { it }): BattleField = BattleField(
        allies = listOf(testAlly(1, hp = 10000, passiveSkills = emptyList()).copy(isDefending = allyMods(true))),
        enemies = listOf(testEnemy(2, attack = attackerAttack).copy(breakPointRate = 0)),
        turnQueue = listOf(2),
    )

    /** 式: 70%を掛けて切り捨て、最低1。防御していなければそのまま。 */
    @Test
    fun reduceDamage_floorsAndMinimumOne() {
        assertEquals(16, DefenseFormula.reduceDamage(24, true))
        assertEquals(1, DefenseFormula.reduceDamage(1, true))
        assertEquals(1, DefenseFormula.reduceDamage(0, true))
        assertEquals(24, DefenseFormula.reduceDamage(24, false))
    }

    /** 防御中の味方は、攻撃を24→16ダメージで受けること。 */
    @Test
    fun defendingTarget_takes70Percent() {
        val (after, result) = checkNotNull(defendingAllyField().performAttack(2, 1, TEST_NORMAL_ATTACK, CriticalRandom(23)))

        assertEquals(16, result.totalDamage)
        assertEquals(10000 - 16, after.allies[0].currentHp)
    }

    /** 計算順: ブレイク×1.3(24→31)の後に防御(31→21)。逆順なら20になる。 */
    @Test
    fun order_defenseAfterBreakBoost() {
        val field = defendingAllyField().let { f ->
            f.copy(allies = f.allies.map { it.copy(breakState = BreakState.BROKEN) })
        }

        val (_, result) = checkNotNull(field.performAttack(2, 1, TEST_NORMAL_ATTACK, CriticalRandom(23)))

        assertEquals(21, result.totalDamage)
    }

    /** 計算順: 防御(68→47)の後にPスキル軽減(-20→27)。逆順なら33になる。 */
    @Test
    fun order_defenseBeforePassiveReduction() {
        val field = defendingAllyField(attackerAttack = 300).let { f ->
            f.copy(allies = f.allies.map { it.copy(passiveSkills = listOf(TEST_STEEL_BODY)) })
        }

        val (_, result) = checkNotNull(field.performAttack(2, 1, TEST_NORMAL_ATTACK, CriticalRandom(23)))

        assertEquals(27, result.totalDamage)
    }

    /** 防御中にクリティカル(最終150%)が出ても無効で、通常ヒット扱い(倍率なし)の24→16。無効化を記録し、乱数は1回引くこと。 */
    @Test
    fun critical_isBlockedWhenDefending() {
        val random = CriticalRandom(0)

        val (_, result) = checkNotNull(defendingAllyField().performAttack(2, 1, TEST_NORMAL_ATTACK, random))

        val shot = result.hits.single().shots.single()
        assertEquals(CriticalType.NONE, shot.critical)
        assertTrue(shot.isCriticalBlocked)
        assertEquals(16, shot.damage)
        assertEquals(1, random.criticalDraws)
    }

    /** 防御していなければ、同じ乱数でクリティカル(24×150%=36)になり、無効化は記録されないこと。 */
    @Test
    fun critical_occursWhenNotDefending() {
        val field = defendingAllyField { false }

        val (_, result) = checkNotNull(field.performAttack(2, 1, TEST_NORMAL_ATTACK, CriticalRandom(0)))

        val shot = result.hits.single().shots.single()
        assertEquals(CriticalType.CRITICAL, shot.critical)
        assertFalse(shot.isCriticalBlocked)
        assertEquals(36, shot.damage)
    }

    /** 最終倍率がちょうど100%(150-2段階×25)のクリティカルも無効になること。 */
    @Test
    fun critical_exactly100Percent_isBlocked() {
        val attacker = testEnemy(2, statModifiers = listOf(StatModifier(BattleStat.CRITICAL_DAMAGE, -2, null))).copy(breakPointRate = 0)
        val field = defendingAllyField().copy(enemies = listOf(attacker))

        val (_, result) = checkNotNull(field.performAttack(2, 1, TEST_NORMAL_ATTACK, CriticalRandom(0)))

        val shot = result.hits.single().shots.single()
        assertTrue(shot.isCriticalBlocked)
        assertEquals(16, shot.damage)
    }

    /** 最終倍率が100%未満のバッドクリティカルは無効にならず、そのまま発生(24×75%=18→防御で12)すること。 */
    @Test
    fun badCritical_isNotBlocked() {
        val attacker = testEnemy(2, statModifiers = listOf(StatModifier(BattleStat.CRITICAL_RATE, -1, null))).copy(breakPointRate = 0)
        val field = defendingAllyField().copy(enemies = listOf(attacker))

        val (_, result) = checkNotNull(field.performAttack(2, 1, TEST_NORMAL_ATTACK, CriticalRandom(0)))

        val shot = result.hits.single().shots.single()
        assertEquals(CriticalType.BAD_CRITICAL, shot.critical)
        assertFalse(shot.isCriticalBlocked)
        assertEquals(12, shot.damage)
    }

    /** ブレイクゲージは防御適用後の実際に減ったHPで貯まること(最大HP100・16ダメージ・倍率150→24)。 */
    @Test
    fun breakGauge_usesDamageAfterDefense() {
        val field = defendingAllyField().let { f ->
            f.copy(
                allies = f.allies.map { it.copy(character = it.character.copy(hp = 100 - HitPointFormula.BASE_HP), currentHp = 100) },
                enemies = f.enemies.map { it.copy(breakPointRate = 150) },
            )
        }

        val (after, _) = checkNotNull(field.performAttack(2, 1, TEST_NORMAL_ATTACK, CriticalRandom(23)))

        assertEquals(24.0, after.allies[0].breakGauge, 1e-9)
    }

    /** 防御中にブレイクしたら即解除され、多段攻撃ではブレイクさせたヒットは防御あり(16)、残りは防御なし・ブレイク×1.3(31)になること。 */
    @Test
    fun breakDuringMultiHit_releasesDefenseFromNextHit() {
        // 最大HP32・倍率300: 1発目の16ダメージでゲージ 16/32*100*3=150 → ブレイク。2発目は防御なしのブレイク中で31。
        val field = defendingAllyField().let { f ->
            f.copy(
                allies = f.allies.map { it.copy(character = it.character.copy(hp = 32 - HitPointFormula.BASE_HP), currentHp = 32) },
                enemies = f.enemies.map { it.copy(breakPointRate = 300) },
            )
        }

        val (after, result) = checkNotNull(
            field.performAttack(2, 1, TEST_NORMAL_ATTACK.copy(activationCount = 2), CriticalRandom(23)),
        )

        val shots = result.hits.single().shots
        assertEquals(listOf(16, 31), shots.map { it.damage })
        assertTrue(shots[0].causedBreak)
        assertFalse(after.allies[0].isDefending)
        assertEquals(BreakState.BROKEN, after.allies[0].breakState)
    }

    /** 敵(isDefendingの共通化の確認): 敵が防御中でも同じ式でダメージが減ること。 */
    @Test
    fun defendingEnemy_alsoTakes70Percent() {
        val field = BattleField(
            allies = listOf(testAlly(1)),
            enemies = listOf(testEnemy(2, hp = 10000).copy(isDefending = true)),
            turnQueue = listOf(1),
        )

        val (_, result) = checkNotNull(field.performAttack(1, 2, TEST_NORMAL_ATTACK, CriticalRandom(23)))

        assertEquals(16, result.totalDamage)
    }

    // ---- defend の成立条件 ----

    private fun turnField() = BattleField(
        allies = listOf(testAlly(1), testAlly(3)),
        enemies = listOf(testEnemy(2)),
        turnQueue = listOf(1, 3, 2),
    )

    /** 現在の行動者の味方は防御でき、その個体だけ防御状態になること。 */
    @Test
    fun defend_succeedsForCurrentAlly() {
        val after = checkNotNull(turnField().defend(1))

        assertTrue(after.allies.first { it.fpid == 1 }.isDefending)
        assertFalse(after.allies.first { it.fpid == 3 }.isDefending)
        assertEquals(listOf(1, 3, 2), after.turnQueue)
    }

    /** 現在の行動者でない味方・敵・存在しないFPIDは防御できないこと。 */
    @Test
    fun defend_rejectsNonActorEnemyAndUnknown() {
        assertNull(turnField().defend(3))
        assertNull(turnField().copy(turnQueue = listOf(2)).defend(2))
        assertNull(turnField().defend(99))
    }

    /** 戦闘不能・ブレイク中・勝敗確定済みは防御できないこと。 */
    @Test
    fun defend_rejectsDefeatedBrokenAndFinished() {
        val defeated = turnField().let { f -> f.copy(allies = f.allies.map { if (it.fpid == 1) it.copy(currentHp = 0) else it }) }
        val broken = turnField().let { f -> f.copy(allies = f.allies.map { if (it.fpid == 1) it.copy(breakState = BreakState.BROKEN) else it }) }
        val finished = turnField().let { f -> f.copy(enemies = f.enemies.map { it.copy(currentHp = 0) }) }

        assertNull(defeated.defend(1))
        assertNull(broken.defend(1))
        assertNull(finished.defend(1))
    }

    /** [DefendUseCase]は成立すればtrueで防御状態を保存し、不成立ならfalseで何も変えないこと。 */
    @Test
    fun defendUseCase_updatesRepositoryOnlyWhenValid() = runBlocking {
        val repo = FakeBattleRepository(turnField())
        val useCase = DefendUseCase(repo)

        assertFalse(useCase(3))
        assertFalse(repo.current.allies.any { it.isDefending })
        assertTrue(useCase(1))
        assertTrue(repo.current.allies.first { it.fpid == 1 }.isDefending)
    }

    // ---- 解除のタイミング ----

    /** 敵の素早さを高くし、味方1(手番)の次は敵2、その次が味方1に戻る並びのフィールド。 */
    private fun alternatingField() = BattleField(
        allies = listOf(testAlly(1, agility = 100)),
        enemies = listOf(testEnemy(2, agility = 400)),
        turnQueue = listOf(1),
    )

    /** 防御は他の個体の手番では解除されず、次の自分の手番が始まった瞬間に解除されること(新たに並べる経路)。 */
    @Test
    fun release_whenOwnNextTurnStarts_notDuringOthersTurn() {
        val defended = checkNotNull(alternatingField().defend(1))

        val enemyTurn = defended.endTurn(1, Random(0))
        assertEquals(2, enemyTurn.currentActorFpid)
        assertTrue(enemyTurn.allies[0].isDefending)

        val ownTurn = enemyTurn.endTurn(2, Random(0))
        assertEquals(1, ownTurn.currentActorFpid)
        assertFalse(ownTurn.allies[0].isDefending)
    }

    /** 同時に手番が来て、キューに残っていた個体が先頭になる経路でも、その個体の防御が解除されること。 */
    @Test
    fun release_whenPromotedFromRemainingQueue() {
        val field = turnField().let { f ->
            f.copy(allies = f.allies.map { if (it.fpid == 3) it.copy(isDefending = true) else it })
        }

        val next = field.endTurn(1, Random(0))

        assertEquals(3, next.currentActorFpid)
        assertFalse(next.allies.first { it.fpid == 3 }.isDefending)
    }

    /** 自分以外が先頭に来ても、他の個体の防御は解除されないこと(キュー経路)。 */
    @Test
    fun release_doesNotTouchOthersInQueue() {
        val defended = checkNotNull(turnField().defend(1))

        val next = defended.endTurn(1, Random(0))

        assertEquals(3, next.currentActorFpid)
        assertTrue(next.allies.first { it.fpid == 1 }.isDefending)
    }
}
