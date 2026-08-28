package jp.co.fuller.morikan.bootcamp.mikatsu.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jp.co.fuller.morikan.bootcamp.mikatsu.data.repository.CharacterRepositoryImpl
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.CharacterRepository
import javax.inject.Singleton

/**
 * [CharacterRepository]インターフェースと、その実装である[CharacterRepositoryImpl]を
 * Hiltへ束縛するDIモジュール。
 *
 * `domain`モジュールが具象実装を知らないまま(依存性逆転の原則)、
 * `ui`/`app`モジュールが[CharacterRepository]の注入を受けられるようにすることを目的とする。
 * アプリ全体で単一のインスタンスを共有するため[SingletonComponent]にインストールする。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * [CharacterRepository]が要求された際に[CharacterRepositoryImpl]を注入するよう束縛する。
     *
     * @param impl Hiltが生成した[CharacterRepositoryImpl]のインスタンス。
     * @return [CharacterRepository]として扱われる実装。
     */
    @Binds
    @Singleton
    abstract fun bindCharacterRepository(impl: CharacterRepositoryImpl): CharacterRepository
}
