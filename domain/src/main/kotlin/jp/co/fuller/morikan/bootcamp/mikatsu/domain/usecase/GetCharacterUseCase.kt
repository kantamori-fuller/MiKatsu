package jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Character
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.CharacterRepository

/**
 * 指定したIDのキャラクターを1件取得するUseCase。
 *
 * キャラクター編集画面を開いたときに、対象キャラクターの保存済みデータを
 * フォームへ復元するために使用することを目的とする。
 *
 * @property repository キャラクターデータへのアクセスを提供するRepository。
 */
class GetCharacterUseCase(private val repository: CharacterRepository) {

    /**
     * 指定したIDのキャラクターを取得する。
     *
     * @param id 取得したいキャラクターのID。
     * @return 該当するキャラクター。存在しない場合は`null`。
     */
    suspend operator fun invoke(id: Int): Character? = repository.getCharacter(id)
}
