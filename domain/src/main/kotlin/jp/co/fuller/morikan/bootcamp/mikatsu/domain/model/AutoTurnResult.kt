package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model

/**
 * CPUが動かす手番(敵・洗脳または発狂の個体。`ExecuteAutoTurnUseCase`)の結果。
 *
 * 画面が「攻撃した」「スキルを使った」「発狂で何もしない」「届かない」「何もできない」を区別して、それぞれの文を出せるようにするための型。
 * 手番の終了は含まない(呼び出し側が行う)。
 */
sealed interface AutoTurnResult {

    /**
     * 攻撃(通常攻撃、または発狂で選ばれた基礎威力のあるスキル)を行った。
     *
     * @property result 攻撃の結果。
     */
    data class Attacked(val result: AttackResult) : AutoTurnResult

    /**
     * 発狂で選ばれた、ダメージを与えないスキルを使った。
     *
     * @property result スキルの結果。
     */
    data class UsedSkill(val result: SkillResult) : AutoTurnResult

    /** 発狂で、何もしなかった(60%の判定、または今使えるスキルが1つもなかった)。 */
    data object MadnessIdle : AutoTurnResult

    /** 通常攻撃が届く相手がいなくて、何もしなかった(敵・洗脳)。 */
    data object CannotReach : AutoTurnResult

    /** 魅了などで選べる行動が何もなく、何もできなかった(魅了した敵は通常攻撃しかないので、反対陣営を狙えず何もできない)。 */
    data object NothingToDo : AutoTurnResult
}
