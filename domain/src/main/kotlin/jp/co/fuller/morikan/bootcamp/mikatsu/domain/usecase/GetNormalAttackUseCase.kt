package jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkill
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.SkillRepository

/**
 * 通常攻撃(Aスキル1)のスキルデータを取得するUseCase。
 *
 * バトル画面が、通常攻撃のレンジ・範囲・対象選択から「攻撃で選べる相手」を求めるために使用する。
 *
 * @property skillRepository スキルのマスターデータを提供するRepository。
 */
class GetNormalAttackUseCase(private val skillRepository: SkillRepository) {

    /**
     * 通常攻撃のスキルデータを取得する。
     *
     * @return 通常攻撃のアクティブスキル。
     */
    suspend operator fun invoke(): ActiveSkill = skillRepository.getNormalAttack()
}
