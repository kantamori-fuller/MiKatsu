package jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.TEST_NORMAL_ATTACK
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/** [GetNormalAttackUseCase]がRepositoryの通常攻撃データをそのまま返すことを検証するテスト。 */
class GetNormalAttackUseCaseTest {

    /** Repositoryが持つ通常攻撃を返すこと。 */
    @Test
    fun invoke_returnsNormalAttack() = runBlocking {
        assertEquals(TEST_NORMAL_ATTACK, GetNormalAttackUseCase(FakeSkillRepository())())
    }
}
