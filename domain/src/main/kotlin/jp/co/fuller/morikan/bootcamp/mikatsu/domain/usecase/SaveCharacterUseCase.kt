package jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Character
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.CharacterDraft
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.CharacterRepository

class SaveCharacterUseCase(private val repository: CharacterRepository) {
    suspend operator fun invoke(draft: CharacterDraft): Character = repository.saveCharacter(draft)
}
