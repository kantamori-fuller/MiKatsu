package jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model

/**
 * バトル画面で、プレイヤーに表示するテキスト(バトルナレーター)の種類。
 *
 * 画面に表示する文言そのものはViewModelに持たせず、何を伝えるべきかと必要な引数だけをこの型で表し、
 * 実際の文言への変換はScreen側が文字列リソースを用いて行う。
 */
sealed interface BattleNarration {

    /**
     * 味方の手番で、行動の選択を促す。
     *
     * @property actorName 行動する味方の表示名。
     */
    data class SelectAction(val actorName: String) : BattleNarration

    /** 攻撃対象を選ぶ状態(対象選択状態)。敵の選択を促す。 */
    data object SelectTarget : BattleNarration

    /** 使うスキルを選ぶ状態(スキル一覧の表示中)。スキルの選択を促す。 */
    data object SelectSkill : BattleNarration

    /** スキルの対象となる味方を選ぶ状態。味方の選択を促す。 */
    data object SelectAllyTarget : BattleNarration

    /**
     * 敵が行動を始めたことを伝える。
     *
     * @property actorName 行動する敵の表示名。
     */
    data class EnemyTurn(val actorName: String) : BattleNarration

    /**
     * 攻撃の結果(誰が誰にどれだけのダメージを与えたか)を伝える。
     *
     * @property attackerName 攻撃した個体の表示名。
     * @property targetName 攻撃を受けた個体の表示名。
     * @property damage 与えたダメージ量。
     */
    data class AttackResult(
        val attackerName: String,
        val targetName: String,
        val damage: Int,
    ) : BattleNarration

    /**
     * 回復スキルの結果(誰が誰にどのスキルを使い、HPがどれだけ回復したか)を伝える。
     *
     * @property actorName スキルを使った味方の表示名。
     * @property targetName 対象になった味方の表示名。
     * @property skillName 使ったスキルの名前。
     * @property healedAmount 実際に回復したHP量。
     */
    data class SkillResult(
        val actorName: String,
        val targetName: String,
        val skillName: String,
        val healedAmount: Int,
    ) : BattleNarration
}
