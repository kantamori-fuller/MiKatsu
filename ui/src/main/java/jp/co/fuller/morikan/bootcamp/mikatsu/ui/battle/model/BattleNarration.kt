package jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleStat
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.CriticalType

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
     * @property critical そのヒットのクリティカルの種別。通常は[CriticalType.NONE]。
     * @property isCriticalBlocked 対象の防御でクリティカルを防いだか。`true`のとき[critical]は[CriticalType.NONE]。
     * @property skillName 通常攻撃以外の技名。`null`なら「攻撃」と表示する。
     */
    data class AttackResult(
        val attackerName: String,
        val targetName: String,
        val damage: Int,
        val critical: CriticalType = CriticalType.NONE,
        val isCriticalBlocked: Boolean = false,
        val skillName: String? = null,
    ) : BattleNarration

    /**
     * 対象1体への攻撃が全て外れたことを伝える。
     *
     * @property attackerName 攻撃した個体の表示名。
     * @property targetName 攻撃を外した相手の表示名。
     * @property skillName 通常攻撃以外の技名。`null`なら「攻撃」と表示する。
     */
    data class AttackMissed(
        val attackerName: String,
        val targetName: String,
        val skillName: String? = null,
    ) : BattleNarration

    /**
     * 複数回攻撃の1ヒットが命中したことを伝える。
     *
     * @property shotNumber 何回目のヒットか(1始まり)。
     * @property damage そのヒットのダメージ量。
     * @property critical そのヒットのクリティカルの種別。通常は[CriticalType.NONE]。
     * @property isCriticalBlocked 対象の防御でクリティカルを防いだか。`true`のとき[critical]は[CriticalType.NONE]。
     */
    data class ShotHit(
        val shotNumber: Int,
        val damage: Int,
        val critical: CriticalType = CriticalType.NONE,
        val isCriticalBlocked: Boolean = false,
    ) : BattleNarration

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
     * @property skillName 通常攻撃以外の技名。`null`なら「攻撃」と表示する。
     */
    data class AreaAttackStart(val attackerName: String, val skillName: String? = null) : BattleNarration

    /**
     * 範囲攻撃が全員に全発外れたことを伝える。
     *
     * @property attackerName 攻撃した個体の表示名。
     * @property skillName 通常攻撃以外の技名。`null`なら「攻撃」と表示する。
     */
    data class AreaAttackAllMissed(val attackerName: String, val skillName: String? = null) : BattleNarration

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
     * 範囲攻撃で、クリティカルが出た対象を伝える。ヒットごとに1文ずつ使う。
     *
     * @property targetName クリティカルを受けた個体の表示名。
     */
    data class AreaCritical(val targetName: String) : BattleNarration

    /**
     * 範囲攻撃で、バッドクリティカルが出た対象を伝える。ヒットごとに1文ずつ使う。
     *
     * @property targetName バッドクリティカルを受けた個体の表示名。
     */
    data class AreaBadCritical(val targetName: String) : BattleNarration

    /**
     * 範囲攻撃で、対象が防御でクリティカルを防いだことを伝える。ヒットごとに1文ずつ使う。
     *
     * @property targetName 防御でクリティカルを防いだ個体の表示名。
     */
    data class AreaCriticalBlocked(val targetName: String) : BattleNarration

    /**
     * 味方が防御したことを伝える(「〇〇は防御している!」)。
     *
     * @property actorName 防御した個体の表示名。
     */
    data class Defend(val actorName: String) : BattleNarration

    /**
     * 先制の手番が始まったことを伝える(「Aの先制行動!」)。
     *
     * @property actorName 先制行動する味方の表示名。
     */
    data class PreemptiveAction(val actorName: String) : BattleNarration

    /**
     * 先制の手番が、戦闘不能・ブレイク・使える先制スキルが無いなどで取り消されたことを伝える(「Aは先制行動できなかった!」)。
     *
     * @property actorName 先制行動できなかった味方の表示名。
     */
    data class PreemptiveFailed(val actorName: String) : BattleNarration

    /**
     * 攻撃を受けた個体がブレイクしたことを伝える。
     *
     * @property targetName ブレイクした個体の表示名。
     */
    data class Broke(val targetName: String) : BattleNarration

    /**
     * ブレイク中の個体が、手番が来ても動けないことを伝える。
     *
     * @property actorName 動けない個体の表示名。
     */
    data class BreakStunned(val actorName: String) : BattleNarration

    /**
     * ブレイク中の個体が、動けないまま手番を終えて立ち上がったことを伝える。
     *
     * @property actorName 立ち上がった個体の表示名。
     */
    data class BreakRecovered(val actorName: String) : BattleNarration

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

    /**
     * ステータス変化スキルの使用を伝える(「Aの覇気!」)。
     *
     * @property actorName スキルを使った味方の表示名。
     * @property skillName 使ったスキルの名前。
     */
    data class SkillUsed(val actorName: String, val skillName: String) : BattleNarration

    /**
     * ステータス変化スキルが対象に外れたことを伝える(「Aの覇気! しかし外れた!」)。
     *
     * @property actorName スキルを使った味方の表示名。
     * @property skillName 使ったスキルの名前。
     */
    data class SkillMissed(val actorName: String, val skillName: String) : BattleNarration

    /**
     * 対象のステータスが段階的に上がった/下がったことを伝える。
     *
     * @property targetName 対象の表示名。
     * @property stat 変化したステータス。
     * @property stages 段階の増減(正なら上がった、負なら下がった)。表示には絶対値を使う。
     */
    data class StatStageChanged(val targetName: String, val stat: BattleStat, val stages: Int) : BattleNarration

    /**
     * 対象1体への攻撃が命中したが、ダメージの文が続かない吸収・反射のときの、ヒットの文(「AがBに攻撃!」)。
     *
     * @property attackerName 攻撃した個体の表示名。
     * @property targetName 攻撃を受けた個体の表示名。
     * @property skillName 通常攻撃以外の技名。`null`なら「攻撃」と表示する。
     */
    data class AttackLead(
        val attackerName: String,
        val targetName: String,
        val skillName: String? = null,
    ) : BattleNarration

    /**
     * 複数回攻撃の1ヒットが命中したことだけを伝える(「2回目: 命中!」)。吸収・反射のヒットで、続けて吸収・反射の文が来る。
     *
     * @property shotNumber 何回目のヒットか(1始まり)。
     */
    data class ShotLanded(val shotNumber: Int) : BattleNarration

    /**
     * 対象1体への攻撃(1発)が、対象の属性耐性で無効になったことを伝える(「AがBに攻撃! しかしBには効かなかった!」)。
     *
     * @property attackerName 攻撃した個体の表示名。
     * @property targetName 無効にした相手の表示名。
     * @property skillName 通常攻撃以外の技名。`null`なら「攻撃」と表示する。
     */
    data class AttackNullified(
        val attackerName: String,
        val targetName: String,
        val skillName: String? = null,
    ) : BattleNarration

    /**
     * 複数回攻撃の1ヒットが、対象の属性耐性で無効になったことを伝える(「2回目: 効かなかった!」)。
     *
     * @property shotNumber 何回目のヒットか(1始まり)。
     */
    data class ShotNullified(val shotNumber: Int) : BattleNarration

    /**
     * 範囲攻撃で、属性耐性により無効になった相手をまとめて伝える(「Bには効かなかった!」)。
     *
     * @property targetNames 無効にした相手の表示名。
     */
    data class AreaNullified(val targetNames: List<String>) : BattleNarration

    /**
     * 攻撃を属性耐性の吸収で受け、HPが回復したことを伝える(「Bは攻撃を吸収した! HPが30回復」)。
     *
     * @property targetName 吸収した個体の表示名。
     * @property healedAmount 実際に回復したHP量。
     */
    data class AttackAbsorbed(val targetName: String, val healedAmount: Int) : BattleNarration

    /**
     * 攻撃が反射され、攻撃者がダメージを受けたことを伝える(「Bは攻撃を跳ね返した! Aに30ダメージ」)。
     *
     * @property targetName 反射した個体の表示名。
     * @property attackerName 反射されて自分がダメージを受けた攻撃者の表示名。
     * @property damage 攻撃者が受けたダメージ。
     */
    data class AttackReflected(val targetName: String, val attackerName: String, val damage: Int) : BattleNarration

    /**
     * 攻撃が反射されたが、攻撃者の耐性で無効になったことを伝える(「Bは攻撃を跳ね返した! しかしAには効かなかった!」)。
     *
     * @property targetName 反射した個体の表示名。
     * @property attackerName 攻撃者の表示名。
     */
    data class AttackReflectedNullified(val targetName: String, val attackerName: String) : BattleNarration

    /**
     * 攻撃が反射されたが、攻撃者が吸収してHPが回復したことを伝える(「Bは攻撃を跳ね返した! AはHPが30回復」)。
     *
     * @property targetName 反射した個体の表示名。
     * @property attackerName 攻撃者の表示名。
     * @property healedAmount 攻撃者が実際に回復したHP量。
     */
    data class AttackReflectedAbsorbed(
        val targetName: String,
        val attackerName: String,
        val healedAmount: Int,
    ) : BattleNarration

    /**
     * ステータス変化スキルが対象の属性耐性で無効になったことを伝える(「Aの覇気! しかしBには効かなかった!」)。対象が1体のときに使う。
     *
     * @property actorName スキルを使った味方の表示名。
     * @property skillName 使ったスキルの名前。
     * @property targetName 無効にした相手の表示名。
     */
    data class SkillNullified(val actorName: String, val skillName: String, val targetName: String) : BattleNarration

    /**
     * スキルの使用後に続けて、対象の属性耐性で効果が無効になったことを伝える(「Bには効かなかった!」)。
     *
     * @property targetName 無効にした相手の表示名。
     */
    data class EffectNullified(val targetName: String) : BattleNarration

    /**
     * スキルの効果が反射されたことを伝える(「Bは効果を跳ね返した!」)。続けて使用者にかかった変化か[EffectReflectedNullified]が来る。
     *
     * @property targetName 反射した個体の表示名。
     */
    data class EffectReflected(val targetName: String) : BattleNarration

    /**
     * 反射された効果が、使用者の耐性で無効になったことを伝える(「しかしAには効かなかった!」)。
     *
     * @property actorName 効果を跳ね返された使用者の表示名。
     */
    data class EffectReflectedNullified(val actorName: String) : BattleNarration
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
