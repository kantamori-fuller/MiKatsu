package jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Character
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.CharacterDraft
import kotlinx.coroutines.flow.Flow

interface CharacterRepository {
    fun observeCharacters(): Flow<List<Character>>
    suspend fun getCharacter(id: Int): Character?
    suspend fun saveCharacter(draft: CharacterDraft): Character
    suspend fun deleteCharacter(id: Int)
}
