package jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillCatalog
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.SkillRepository

/**
 * 設定可能なアクティブ・パッシブスキルの一覧を取得するUseCase。
 *
 * キャラクター編集画面が、キャラクターに設定できるスキルを選択肢として表示するために使用する。
 *
 * @property skillRepository スキルのマスターデータを提供するRepository。
 */
class GetSkillsUseCase(private val skillRepository: SkillRepository) {

    /**
     * 設定可能なスキルをすべて取得する。
     *
     * @return アクティブ・パッシブスキルの一覧。
     */
    suspend operator fun invoke(): SkillCatalog =
        SkillCatalog(skillRepository.getActiveSkills(), skillRepository.getPassiveSkills())
}
