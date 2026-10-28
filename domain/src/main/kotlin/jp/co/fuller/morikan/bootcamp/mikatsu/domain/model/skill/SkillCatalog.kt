package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill

/**
 * 設定可能なスキルの一覧(マスターデータ)。
 *
 * キャラクター編集画面が、設定できるアクティブ・パッシブスキルをまとめて受け取ることを目的とする。
 *
 * @property activeSkills 設定可能なアクティブスキルの一覧。
 * @property passiveSkills 設定可能なパッシブスキルの一覧。
 */
data class SkillCatalog(
    val activeSkills: List<ActiveSkill>,
    val passiveSkills: List<PassiveSkill>,
)
