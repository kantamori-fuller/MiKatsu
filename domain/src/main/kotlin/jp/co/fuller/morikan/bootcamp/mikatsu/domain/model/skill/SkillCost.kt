package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill

/**
 * スキル1回の使用で消費するコスト1種類分。
 *
 * スキル一覧の「コスト消費」列の「MP30&HP10」は、`SkillCost(MP, 30)`と`SkillCost(HP, 10)`の
 * 2要素のリストで表す。コストがない(0)スキルは空リストにする。
 *
 * @property type コストの種類。
 * @property amount 消費量。
 */
data class SkillCost(
    val type: SkillCostType,
    val amount: Int,
)
