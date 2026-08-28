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

class CharacterRepositoryImpl @Inject constructor(
    private val localDataSource: CharacterLocalDataSource,
) : CharacterRepository {

    private val charactersFlow = MutableStateFlow(localDataSource.getAll())

    override fun observeCharacters(): Flow<List<Character>> = charactersFlow.asStateFlow()

    override suspend fun getCharacter(id: Int): Character? = withContext(Dispatchers.IO) {
        localDataSource.getById(id)
    }

    override suspend fun saveCharacter(draft: CharacterDraft): Character = withContext(Dispatchers.IO) {
        val saved = localDataSource.save(draft)
        charactersFlow.value = localDataSource.getAll()
        saved
    }

    override suspend fun deleteCharacter(id: Int) = withContext(Dispatchers.IO) {
        localDataSource.delete(id)
        charactersFlow.value = localDataSource.getAll()
    }
}
