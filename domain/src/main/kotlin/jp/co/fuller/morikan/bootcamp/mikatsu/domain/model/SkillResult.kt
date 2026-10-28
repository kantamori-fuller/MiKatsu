package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model

/**
 * 実行されたアクティブスキル1回分の結果。
 *
 * 画面が「誰が、誰に、どのスキルを使い、どんな効果があったか」をナレーションとして
 * 表示できるようにすることを目的とする。個体はいずれもFPIDで指定する。
 *
 * @property actorFpid スキルを使った個体のFPID。
 * @property skillId 使ったアクティブスキルのID(アクティブ採番のマスターデータのID)。
 * @property skillName 使ったスキルの名前。
 * @property targetFpid スキルの対象となった個体のFPID。
 * @property healedAmount 実際に回復したHP量。最大HPで頭打ちになるため、スキルの回復量より小さくなり得る。
 */
data class SkillResult(
    val actorFpid: Int,
    val skillId: Int,
    val skillName: String,
    val targetFpid: Int,
    val healedAmount: Int,
)
