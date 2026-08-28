package jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Enemy
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.EnemyRepository

/**
 * 今回のバトルに登場する敵の一覧を取得するUseCase。
 *
 * バトル画面が敵陣営の表示に必要なデータを取得できるようにすることを目的とする。
 *
 * @property repository 敵データへのアクセスを提供するRepository。
 */
class GetEnemiesUseCase(private val repository: EnemyRepository) {

    /**
     * 今回のバトルに登場する敵の一覧を取得する。
     *
     * @return 敵の一覧。最大6体を想定する。
     */
    suspend operator fun invoke(): List<Enemy> = repository.getEnemies()
}
