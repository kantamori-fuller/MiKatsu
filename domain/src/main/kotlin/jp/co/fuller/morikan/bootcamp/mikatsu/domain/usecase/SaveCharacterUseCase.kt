package jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Character
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.CharacterDraft
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.CharacterRepository

/**
 * キャラクターを保存(新規作成または上書き)するUseCase。
 *
 * キャラクター作成/編集画面の「保存」操作から呼び出され、
 * 入力内容を端末内へ永続化することを目的とする。
 *
 * @property repository キャラクターデータへのアクセスを提供するRepository。
 */
class SaveCharacterUseCase(private val repository: CharacterRepository) {

    /**
     * ドラフトの内容でキャラクターを保存する。
     *
     * [CharacterDraft.id]が`null`の場合は新規保存、値がある場合は上書き保存となる。
     *
     * @param draft 保存する入力内容。
     * @return 保存後のキャラクター(採番されたIDを含む)。
     */
    suspend operator fun invoke(draft: CharacterDraft): Character = repository.saveCharacter(draft)
}
