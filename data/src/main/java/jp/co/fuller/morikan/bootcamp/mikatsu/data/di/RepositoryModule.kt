package jp.co.fuller.morikan.bootcamp.mikatsu.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jp.co.fuller.morikan.bootcamp.mikatsu.data.repository.CharacterRepositoryImpl
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.CharacterRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindCharacterRepository(impl: CharacterRepositoryImpl): CharacterRepository
}
