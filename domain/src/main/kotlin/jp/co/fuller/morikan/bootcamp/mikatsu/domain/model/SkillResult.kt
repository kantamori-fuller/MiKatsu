package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model

/**
 * 実行されたアクティブスキル1回分の結果。
 *
 * 画面が「誰が、誰に、どのスキルを使い、どんな効果があったか」をナレーションとして
 * 表示できるようにすることを目的とする。範囲スキルでは対象が複数になるため、対象ごとの結果を[targets]に持つ。
 * 個体はいずれもFPIDで指定する。
 *
 * @property actorFpid スキルを使った個体のFPID。
 * @property skillId 使ったアクティブスキルのID(アクティブ採番のマスターデータのID)。
 * @property skillName 使ったスキルの名前。
 * @property targets スキルの効果を受けた個体ごとの結果。1体以上。
 */
data class SkillResult(
    val actorFpid: Int,
    val skillId: Int,
    val skillName: String,
    val targets: List<SkillTargetResult>,
) {
    /** 全対象で実際に回復したHP量の合計。 */
    val totalHealed: Int
        get() = targets.sumOf { it.healedAmount }
}

/**
 * スキルの効果を受けた1体分の結果。
 *
 * @property targetFpid スキルの対象となった個体のFPID。
 * @property healedAmount 実際に回復したHP量。最大HPで頭打ちになるため、スキルの回復量より小さくなり得る。外れた対象は0。
 * @property isHit スキルが対象に命中したか。外れた対象には効果が一切かからない。命中率のないスキルは常に`true`。
 * @property statChanges かかったステータス変化(外れたときは空)。段階の上限で頭打ちになる場合も、記録はされるため「かかった」扱い。
 *   無効・反射になった対象は空(反射された分は[reflected]に持つ)。
 * @property resistance 対象の属性耐性による扱い(デバフ・ネガティブエフェクトのスキルのみ)。無効と吸収はどちらも
 *   効果がかからないため[ResistanceOutcome.NULLIFIED]として記録する。無効・反射になった対象も命中した扱い([isHit]は`true`)。
 * @property reflected [resistance]が[ResistanceOutcome.REFLECTED]のとき、使用者に跳ね返った効果の結果。それ以外は`null`。
 */
data class SkillTargetResult(
    val targetFpid: Int,
    val healedAmount: Int,
    val isHit: Boolean = true,
    val statChanges: List<StatChangeResult> = emptyList(),
    val resistance: ResistanceOutcome = ResistanceOutcome.NONE,
    val reflected: ReflectedEffect? = null,
)

/**
 * 反射によって、使用者に跳ね返った効果の結果。
 *
 * @property isNullified 使用者の耐性(無効・反射・吸収)によって使用者にもかからなかったか(再反射はしない)。
 * @property statChanges 使用者にかかったステータス変化。[isNullified]のときは空。
 */
data class ReflectedEffect(
    val isNullified: Boolean,
    val statChanges: List<StatChangeResult> = emptyList(),
)

/**
 * 対象にかかったステータス変化1つ分。ナレーションの「Bの敏捷が1段階下がった!」に用いる。
 *
 * @property stat 補正されたステータス。
 * @property stages 段階の増減(バフは正、デバフは負)。
 */
data class StatChangeResult(
    val stat: BattleStat,
    val stages: Int,
)
