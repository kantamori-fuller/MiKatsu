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

/**
 * `domain`モジュールのUseCase群をHiltへ提供するDIモジュール。
 *
 * UseCaseは`domain`モジュールでDIフレームワークに依存しない素のクラスとして定義されているため
 * (`@Inject`を付与していないため)、Hiltが依存関係を解決できるようにここで生成方法を教える
 * ことを目的とする。UseCase自体は状態を持たないため[SingletonComponent]にインストールする。
 */
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    /**
     * @param repository 注入される[CharacterRepository]。
     * @return 全キャラクター一覧を購読する[ObserveCharactersUseCase]。
     */
    @Provides
    fun provideObserveCharactersUseCase(repository: CharacterRepository): ObserveCharactersUseCase =
        ObserveCharactersUseCase(repository)

    /**
     * @param repository 注入される[CharacterRepository]。
     * @return 指定IDのキャラクターを取得する[GetCharacterUseCase]。
     */
    @Provides
    fun provideGetCharacterUseCase(repository: CharacterRepository): GetCharacterUseCase =
        GetCharacterUseCase(repository)

    /**
     * @param repository 注入される[CharacterRepository]。
     * @return キャラクターを保存する[SaveCharacterUseCase]。
     */
    @Provides
    fun provideSaveCharacterUseCase(repository: CharacterRepository): SaveCharacterUseCase =
        SaveCharacterUseCase(repository)

    /**
     * @param repository 注入される[CharacterRepository]。
     * @return 指定IDのキャラクターを削除する[DeleteCharacterUseCase]。
     */
    @Provides
    fun provideDeleteCharacterUseCase(repository: CharacterRepository): DeleteCharacterUseCase =
        DeleteCharacterUseCase(repository)
}
