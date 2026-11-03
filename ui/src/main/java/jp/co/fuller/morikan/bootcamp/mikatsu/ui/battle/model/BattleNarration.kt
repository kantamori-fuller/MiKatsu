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
     * 敵の攻撃が、届く(レンジ・陣営の条件を満たす)相手がいなかったことを伝える。
     *
     * @property actorName 攻撃しようとした敵の表示名。
     */
    data class EnemyCannotReach(val actorName: String) : BattleNarration

    /**
     * 対象1体への攻撃が1回命中したときの結果(誰が誰にどれだけのダメージを与えたか)を伝える。
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
     * 対象1体への攻撃が全て外れたことを伝える。
     *
     * @property attackerName 攻撃した個体の表示名。
     * @property targetName 攻撃を外した相手の表示名。
     */
    data class AttackMissed(
        val attackerName: String,
        val targetName: String,
    ) : BattleNarration

    /**
     * 複数回攻撃の1ヒットが命中したことを伝える。
     *
     * @property shotNumber 何回目のヒットか(1始まり)。
     * @property damage そのヒットのダメージ量。
     */
    data class ShotHit(val shotNumber: Int, val damage: Int) : BattleNarration

    /**
     * 複数回攻撃の1ヒットが外れたことを伝える。
     *
     * @property shotNumber 何回目のヒットか(1始まり)。
     */
    data class ShotMissed(val shotNumber: Int) : BattleNarration

    /**
     * 複数回攻撃のまとめ(合計ダメージと命中回数)を伝える。
     *
     * @property attackerName 攻撃した個体の表示名。
     * @property targetName 攻撃を受けた個体の表示名。
     * @property totalDamage 与えたダメージの合計。
     * @property shotCount 実際に行ったヒット数。
     * @property hitCount そのうち命中した数。
     */
    data class AttackTotal(
        val attackerName: String,
        val targetName: String,
        val totalDamage: Int,
        val shotCount: Int,
        val hitCount: Int,
    ) : BattleNarration

    /**
     * 範囲攻撃(対象が複数)の開始を伝える。
     *
     * @property attackerName 攻撃した個体の表示名。
     */
    data class AreaAttackStart(val attackerName: String) : BattleNarration

    /**
     * 範囲攻撃が全員に全発外れたことを伝える。
     *
     * @property attackerName 攻撃した個体の表示名。
     */
    data class AreaAttackAllMissed(val attackerName: String) : BattleNarration

    /**
     * 範囲攻撃で、全発が外れた相手をまとめて伝える。
     *
     * @property targetNames 外れた相手の表示名。
     */
    data class AreaMissed(val targetNames: List<String>) : BattleNarration

    /**
     * 範囲攻撃で、命中した相手と与えたダメージをまとめて伝える。
     *
     * @property entries 命中した相手ごとのダメージ。
     */
    data class AreaHit(val entries: List<AreaHitEntry>) : BattleNarration

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

    /**
     * 範囲の回復スキルの結果(誰が何体のHPを合計どれだけ回復したか)を伝える。対象が複数のときに使う。
     *
     * @property actorName スキルを使った味方の表示名。
     * @property skillName 使ったスキルの名前。
     * @property targetCount 対象になった個体の数。
     * @property totalHealed 全対象で実際に回復したHP量の合計。
     */
    data class MultiSkillResult(
        val actorName: String,
        val skillName: String,
        val targetCount: Int,
        val totalHealed: Int,
    ) : BattleNarration
}

/**
 * 範囲攻撃で命中した1体分の、表示名と与えたダメージ。
 *
 * @property targetName 命中した相手の表示名。
 * @property damage その相手に与えたダメージ量(全ヒットの合計)。
 */
data class AreaHitEntry(
    val targetName: String,
    val damage: Int,
)
