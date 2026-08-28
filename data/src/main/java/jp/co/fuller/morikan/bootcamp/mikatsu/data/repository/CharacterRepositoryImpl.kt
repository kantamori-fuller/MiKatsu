package jp.co.fuller.morikan.bootcamp.mikatsu.data.repository

import jp.co.fuller.morikan.bootcamp.mikatsu.data.local.CharacterLocalDataSource
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Character
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.CharacterDraft
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.CharacterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [CharacterRepository]のローカルストレージによる実装。
 *
 * データの正(Single Source of Truth)として[charactersFlow]をメモリ上に保持し、
 * [CharacterLocalDataSource]への読み書きが発生するたびに最新化することで、
 * UI層が常に最新のキャラクター一覧を購読できるようにすることを目的とする。
 * アプリ全体で[charactersFlow]を1つに保つため、[Singleton]としてアプリ全体で
 * インスタンスを共有する(共有しない場合、購読側と更新側で別インスタンスの
 * [charactersFlow]を参照してしまい、更新が即座に反映されなくなる)。
 *
 * @property localDataSource 実際の永続化を担うローカルデータソース。
 */
@Singleton
class CharacterRepositoryImpl @Inject constructor(
    private val localDataSource: CharacterLocalDataSource,
) : CharacterRepository {

    /** アプリ起動時点でローカルストレージから読み込んだ内容を初期値とする、一覧のSSOT。 */
    private val charactersFlow = MutableStateFlow(localDataSource.getAll())

    /**
     * @return [charactersFlow]をそのまま公開し、UI層に最新の一覧を配信する。
     */
    override fun observeCharacters(): Flow<List<Character>> = charactersFlow.asStateFlow()

    /**
     * ファイルI/Oをメインスレッドから外すため、[Dispatchers.IO]上でローカルデータソースを参照する。
     *
     * @param id 取得したいキャラクターのID。
     * @return 該当するキャラクター。存在しない場合は`null`。
     */
    override suspend fun getCharacter(id: Int): Character? = withContext(Dispatchers.IO) {
        localDataSource.getById(id)
    }

    /**
     * ローカルデータソースへ保存したのち、[charactersFlow]を再読み込みして最新化する。
     *
     * @param draft 保存する入力内容。
     * @return 保存後のキャラクター(採番されたIDを含む)。
     */
    override suspend fun saveCharacter(draft: CharacterDraft): Character = withContext(Dispatchers.IO) {
        val saved = localDataSource.save(draft)
        charactersFlow.value = localDataSource.getAll()
        saved
    }

    /**
     * ローカルデータソースから削除したのち、[charactersFlow]を再読み込みして最新化する。
     *
     * @param id 削除対象のキャラクターID。
     */
    override suspend fun deleteCharacter(id: Int) = withContext(Dispatchers.IO) {
        localDataSource.delete(id)
        charactersFlow.value = localDataSource.getAll()
    }
}
