package jp.co.fuller.morikan.bootcamp.mikatsu.data.repository

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Enemy
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.EnemyRepository
import javax.inject.Inject

/**
 * [EnemyRepository]の暫定実装。
 *
 * 敵の出現条件や強さを決定する戦闘ロジックは未実装のため、バトル画面のUIを
 * 確認できるよう固定の敵データを返す。実際の出現ロジックが実装され次第、
 * 呼び出し側([GetEnemiesUseCase][jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.GetEnemiesUseCase]、
 * バトル画面)に影響を与えることなくこの内部実装のみを差し替えられる。
 */
class EnemyRepositoryImpl @Inject constructor() : EnemyRepository {

    override suspend fun getEnemies(): List<Enemy> = FIXED_ENEMIES

    companion object {
        private val FIXED_ENEMIES = listOf(
            Enemy(id = 1, name = "ゴブリン", hp = 30, maxHp = 30, mana = 10, maxMana = 10),
            Enemy(id = 2, name = "ゴブリン", hp = 30, maxHp = 30, mana = 10, maxMana = 10),
            Enemy(id = 3, name = "オーク", hp = 50, maxHp = 50, mana = 5, maxMana = 5),
        )
    }
}
