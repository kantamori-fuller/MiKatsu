package jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Enemy

/**
 * バトルに登場する敵データへのアクセスを抽象化するRepositoryインターフェース。
 *
 * ドメイン層・UI層はこのインターフェースにのみ依存し、敵データの出所
 * (固定データ、抽選ロジック、サーバー配信など)の詳細を知らない(依存性逆転の原則)。
 * 実装は`data`モジュールが提供する。
 */
interface EnemyRepository {

    /**
     * 今回のバトルに登場する敵の一覧を取得する。
     *
     * @return 敵の一覧。最大6体を想定する。
     */
    suspend fun getEnemies(): List<Enemy>
}
