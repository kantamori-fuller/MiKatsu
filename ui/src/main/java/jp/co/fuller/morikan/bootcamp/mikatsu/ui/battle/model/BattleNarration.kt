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
}
