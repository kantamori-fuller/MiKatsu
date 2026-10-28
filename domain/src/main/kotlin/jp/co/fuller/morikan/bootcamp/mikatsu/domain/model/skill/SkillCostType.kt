package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill

/**
 * アクティブスキルを使うために支払うコストの種類(コストタイプ)。
 *
 * 将来HPなど別の資源をコストにするスキルを追加できるよう、コストの種類を明示的に区別する。
 */
enum class SkillCostType {
    /** MPスキル。MPを消費する。 */
    MP,
}
