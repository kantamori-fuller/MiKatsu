package jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.CharacterRepository

class DeleteCharacterUseCase(private val repository: CharacterRepository) {
    suspend operator fun invoke(id: Int) = repository.deleteCharacter(id)
}
