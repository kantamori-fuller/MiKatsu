package jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Character
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.CharacterRepository
import kotlinx.coroutines.flow.Flow

class ObserveCharactersUseCase(private val repository: CharacterRepository) {
    operator fun invoke(): Flow<List<Character>> = repository.observeCharacters()
}
