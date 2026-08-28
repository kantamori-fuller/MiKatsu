package jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Character
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.CharacterRepository
import kotlinx.coroutines.flow.Flow

/**
 * 保存されている全キャラクターの一覧を購読するUseCase。
 *
 * キャラ一覧画面が最新の一覧をリアルタイムに表示できるようにすることを目的とする。
 *
 * @property repository キャラクターデータへのアクセスを提供するRepository。
 */
class ObserveCharactersUseCase(private val repository: CharacterRepository) {

    /**
     * 全キャラクターの一覧を流し続ける[Flow]を返す。
     *
     * @return 保存・削除の度に最新化される、全キャラクターの一覧の[Flow]。
     */
    operator fun invoke(): Flow<List<Character>> = repository.observeCharacters()
}
