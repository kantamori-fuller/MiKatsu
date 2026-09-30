package jp.co.fuller.morikan.bootcamp.mikatsu.data.repository

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Enemy
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.EnemyRepository
import javax.inject.Inject

/**
 * [EnemyRepository]の暫定実装。
 *
 * 敵の出現条件や強さを決定する戦闘ロジックは未実装のため、バトル画面のUIを
 * 確認できるよう固定の敵マスターデータを返す。実際の出現ロジックが実装され次第、
 * 呼び出し側([GetBattleFieldUseCase][jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.GetBattleFieldUseCase]、
 * バトル画面)に影響を与えることなくこの内部実装のみを差し替えられる。
 */
class EnemyRepositoryImpl @Inject constructor() : EnemyRepository {

    override suspend fun getEnemies(): List<Enemy> = FIXED_ENEMIES

    companion object {
        private const val PLACEHOLDER_STAT_VALUE = 100

        private val FIXED_ENEMIES = listOf(
            Enemy(
                id = 1,
                name = "ゴブリン",
                hp = PLACEHOLDER_STAT_VALUE,
                mana = PLACEHOLDER_STAT_VALUE,
                attack = PLACEHOLDER_STAT_VALUE,
                defense = PLACEHOLDER_STAT_VALUE,
                manaOutput = PLACEHOLDER_STAT_VALUE,
                manaResistance = PLACEHOLDER_STAT_VALUE,
                agility = PLACEHOLDER_STAT_VALUE,
                luck = PLACEHOLDER_STAT_VALUE,
            ),
            Enemy(
                id = 2,
                name = "オーク",
                hp = PLACEHOLDER_STAT_VALUE,
                mana = PLACEHOLDER_STAT_VALUE,
                attack = PLACEHOLDER_STAT_VALUE,
                defense = PLACEHOLDER_STAT_VALUE,
                manaOutput = PLACEHOLDER_STAT_VALUE,
                manaResistance = PLACEHOLDER_STAT_VALUE,
                agility = PLACEHOLDER_STAT_VALUE,
                luck = PLACEHOLDER_STAT_VALUE,
            ),
        )
    }
}
