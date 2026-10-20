package jp.co.fuller.morikan.bootcamp.mikatsu.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.EnemyTargetSelector
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.RandomEnemyTargetSelector
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.BattleRepository
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.CharacterRepository
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.EnemyRepository
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.AttackUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.DeleteCharacterUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.EndTurnUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.ExecuteEnemyTurnUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.GetBattleFieldUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.GetCharacterUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.ObserveBattleFieldUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.ObserveCharactersUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.SaveCharacterUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.StartBattleUseCase
import kotlin.random.Random

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

    /**
     * @param characterRepository 注入される[CharacterRepository]。
     * @param enemyRepository 注入される[EnemyRepository]。
     * @return 今回のバトルの味方・敵にFPIDを割り当てて取得する[GetBattleFieldUseCase]。
     */
    @Provides
    fun provideGetBattleFieldUseCase(
        characterRepository: CharacterRepository,
        enemyRepository: EnemyRepository,
    ): GetBattleFieldUseCase = GetBattleFieldUseCase(characterRepository, enemyRepository)

    /**
     * @param getBattleFieldUseCase 注入される[GetBattleFieldUseCase]。
     * @param battleRepository 注入される[BattleRepository]。
     * @param random 注入される[Random]。
     * @return 戦闘を開始する[StartBattleUseCase]。
     */
    @Provides
    fun provideStartBattleUseCase(
        getBattleFieldUseCase: GetBattleFieldUseCase,
        battleRepository: BattleRepository,
        random: Random,
    ): StartBattleUseCase = StartBattleUseCase(getBattleFieldUseCase, battleRepository, random)

    /**
     * @param battleRepository 注入される[BattleRepository]。
     * @return 戦闘フィールドを購読する[ObserveBattleFieldUseCase]。
     */
    @Provides
    fun provideObserveBattleFieldUseCase(battleRepository: BattleRepository): ObserveBattleFieldUseCase =
        ObserveBattleFieldUseCase(battleRepository)

    /**
     * @param battleRepository 注入される[BattleRepository]。
     * @return 通常攻撃を実行する[AttackUseCase]。
     */
    @Provides
    fun provideAttackUseCase(battleRepository: BattleRepository): AttackUseCase =
        AttackUseCase(battleRepository)

    /**
     * @param battleRepository 注入される[BattleRepository]。
     * @param random 注入される[Random]。
     * @return 現在の行動者の手番を終える[EndTurnUseCase]。
     */
    @Provides
    fun provideEndTurnUseCase(battleRepository: BattleRepository, random: Random): EndTurnUseCase =
        EndTurnUseCase(battleRepository, random)

    /**
     * @param random 注入される[Random]。
     * @return 生きている味方からランダムに攻撃対象を選ぶ[EnemyTargetSelector]。
     */
    @Provides
    fun provideEnemyTargetSelector(random: Random): EnemyTargetSelector = RandomEnemyTargetSelector(random)

    /**
     * @param battleRepository 注入される[BattleRepository]。
     * @param enemyTargetSelector 注入される[EnemyTargetSelector]。
     * @return 敵の手番の攻撃を実行する[ExecuteEnemyTurnUseCase]。
     */
    @Provides
    fun provideExecuteEnemyTurnUseCase(
        battleRepository: BattleRepository,
        enemyTargetSelector: EnemyTargetSelector,
    ): ExecuteEnemyTurnUseCase = ExecuteEnemyTurnUseCase(battleRepository, enemyTargetSelector)

    /**
     * 行動順・攻撃対象の選択に使う乱数を提供する。UseCaseが乱数を引数で受け取れる(テストで固定できる)ようにするため。
     *
     * @return 標準の[Random]。
     */
    @Provides
    fun provideRandom(): Random = Random.Default
}
