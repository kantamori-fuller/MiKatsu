package jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.SequenceRandom
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.TEST_DIA
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.ail
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.testAlly
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.testEnemy
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.withAilments
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.StatusAilment
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** プレイヤー向けのUseCase(攻撃・スキル・防御)が、洗脳・発狂で自動で動く個体の操作を弾き、挑発中の防御も弾くことを検証するテスト。 */
class AutoActingPlayerUseCaseTest {

    private fun repository(ailment: StatusAilment) = FakeBattleRepository(
        BattleField(
            allies = listOf(testAlly(1, mp = 100, activeSkills = listOf(TEST_DIA)).withAilments(ail(ailment)), testAlly(2)),
            enemies = listOf(testEnemy(10)),
            turnQueue = listOf(1),
        ),
    )

    /** 洗脳・発狂の個体の通常攻撃・スキル・防御は、何も変えずに弾かれること。 */
    @Test
    fun brainwashedOrMad_cannotBeOperated() = runBlocking {
        listOf(StatusAilment.BRAINWASH, StatusAilment.MADNESS).forEach { ailment ->
            val repo = repository(ailment)
            val before = repo.current

            assertNull("$ailment", AttackUseCase(repo, FakeSkillRepository(), SequenceRandom())(1, 10))
            assertNull("$ailment", UseSkillUseCase(repo, SequenceRandom())(1, TEST_DIA.id, 2))
            assertFalse("$ailment", DefendUseCase(repo)(1))

            assertEquals("$ailment", before, repo.current)
        }
    }

    /** 挑発中の防御とスキル(ダメージなし)は弾かれ、通常攻撃は通ること。 */
    @Test
    fun taunted_cannotDefendOrUseNonAttackSkill_butCanAttack() = runBlocking {
        val repo = repository(StatusAilment.TAUNT)

        assertFalse(DefendUseCase(repo)(1))
        assertNull(UseSkillUseCase(repo, SequenceRandom())(1, TEST_DIA.id, 2))
        assertTrue(AttackUseCase(repo, FakeSkillRepository(), SequenceRandom())(1, 10) != null)
    }
}
