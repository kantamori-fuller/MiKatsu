package jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Character
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.CharacterRepository

class GetCharacterUseCase(private val repository: CharacterRepository) {
    suspend operator fun invoke(id: Int): Character? = repository.getCharacter(id)
}
