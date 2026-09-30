package jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Enemy

/**
 * 敵のマスターデータへのアクセスを抽象化するRepositoryインターフェース。
 *
 * ここで扱う[Enemy]は敵種(EID)ごとに1件のマスターデータであり、実際に
 * バトルフィールドへ登場する個体(同じ敵種が複数体登場する場合を含む)の組み立ては
 * [jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.GetBattleEnemiesUseCase]が担う。
 * ドメイン層・UI層はこのインターフェースにのみ依存し、敵データの出所
 * (固定データ、抽選ロジック、サーバー配信など)の詳細を知らない(依存性逆転の原則)。
 * 実装は`data`モジュールが提供する。
 */
interface EnemyRepository {

    /**
     * 敵種ごとのマスターデータの一覧を取得する。
     *
     * @return 敵種ごとのマスターデータの一覧。
     */
    suspend fun getEnemies(): List<Enemy>
}
