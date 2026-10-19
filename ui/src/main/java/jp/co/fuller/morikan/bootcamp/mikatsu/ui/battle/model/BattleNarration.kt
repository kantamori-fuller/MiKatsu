package jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model

/**
 * バトル画面で、プレイヤーに次の操作を指示するテキスト(バトルナレーター)の種類。
 *
 * 画面に表示する文言そのものはViewModelに持たせず、どの指示を出すべきかだけをこの型で表し、
 * 実際の文言への変換はScreen側が文字列リソースを用いて行う。
 */
enum class BattleNarration {
    /** 味方が選択されていない状態。味方キャラの選択を促す。 */
    SelectCharacter,

    /** 味方が選択され、攻撃などの行動を決めるべき状態。行動の選択を促す。 */
    SelectAction,

    /** 攻撃対象を選ぶ状態(対象選択状態)。敵の選択を促す。 */
    SelectTarget,
}
