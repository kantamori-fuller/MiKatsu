package jp.co.fuller.morikan.bootcamp.mikatsu.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.CharacterRepository
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.DeleteCharacterUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.GetCharacterUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.ObserveCharactersUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.SaveCharacterUseCase

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    fun provideObserveCharactersUseCase(repository: CharacterRepository): ObserveCharactersUseCase =
        ObserveCharactersUseCase(repository)

    @Provides
    fun provideGetCharacterUseCase(repository: CharacterRepository): GetCharacterUseCase =
        GetCharacterUseCase(repository)

    @Provides
    fun provideSaveCharacterUseCase(repository: CharacterRepository): SaveCharacterUseCase =
        SaveCharacterUseCase(repository)

    @Provides
    fun provideDeleteCharacterUseCase(repository: CharacterRepository): DeleteCharacterUseCase =
        DeleteCharacterUseCase(repository)
}
