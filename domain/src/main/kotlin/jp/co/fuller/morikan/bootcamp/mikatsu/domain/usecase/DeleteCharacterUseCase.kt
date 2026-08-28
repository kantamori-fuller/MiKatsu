package jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.CharacterRepository

/**
 * 指定したIDのキャラクターを削除するUseCase。
 *
 * キャラ一覧画面での削除確認ダイアログ操作から呼び出され、
 * 端末内から対象キャラクターのデータを完全に取り除くことを目的とする。
 *
 * @property repository キャラクターデータへのアクセスを提供するRepository。
 */
class DeleteCharacterUseCase(private val repository: CharacterRepository) {

    /**
     * 指定したIDのキャラクターを削除する。
     *
     * @param id 削除対象のキャラクターID。
     */
    suspend operator fun invoke(id: Int) = repository.deleteCharacter(id)
}
