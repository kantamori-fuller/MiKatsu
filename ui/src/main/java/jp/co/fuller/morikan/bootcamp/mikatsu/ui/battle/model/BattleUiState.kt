package jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleOutcome
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Formation

/**
 * バトル画面に表示する、敵または味方1体分の状態を表すUIモデル。
 *
 * 敵([jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Enemy])と
 * 味方([jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Character])はドメイン上
 * 別のモデルだが、バトル画面での見た目(名前・HP・MPの数値とゲージ)は共通のため、
 * 表示専用の型としてこの1つに集約する。[hp]は戦闘中のダメージで減少する
 * 現在値であり、[mana]は現段階ではまだ消費されないため常に[maxMana]と同じ値になる。
 *
 * @property id 表示元のFPID(フィールドポーンID)。味方・敵を問わず、フィールド全体を
 *   通じて一意な値となる([jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleFieldPawn.fpid]
 *   参照)。同じ敵種(EID)がフィールドに複数体登場し得るため、キャラクターIDやEIDでは
 *   なくFPIDを用いる。
 * @property name 表示する名前。同名の個体が複数いる場合は末尾にアルファベットが付く。
 * @property hp 現在のHP。
 * @property maxHp 最大HP。
 * @property mana 現在のMP。
 * @property maxMana 最大MP。
 * @property isDefeated 戦闘不能(HPが0)かどうか。カードはグレーアウトされる。
 * @property isActive 現在の手番の個体かどうか。
 */
data class BattleUnitUiModel(
    val id: Int,
    val name: String,
    val hp: Int,
    val maxHp: Int,
    val mana: Int,
    val maxMana: Int,
    val isDefeated: Boolean,
    val isActive: Boolean,
)

/**
 * バトル画面(BattleScreen)の画面状態を表すUiState。
 *
 * @property enemies 敵陣営の一覧。最大6体を想定する。
 * @property partySlots 味方(プレイヤー)陣営の陣形の、スロット番号ごとの味方。要素数は
 *   [Formation.SLOT_COUNT]であり、味方が配置されていないマスは`null`となる。
 * @property currentActorId 現在の手番の個体のFPID([BattleUnitUiModel.id])。手番の個体がいなければ`null`。
 * @property isPlayerTurn 現在の手番が味方かどうか。
 * @property isProcessing 演出中(敵の行動や攻撃結果の表示中)で、入力を受け付けない状態かどうか。
 *   表示用の値であり、演出中かどうかの実際の判定はViewModelが持つジョブで行う。
 * @property isSelectingTarget 攻撃対象を選択している状態(対象選択状態)かどうか。
 *   「攻撃」ボタンの押下で`true`になり、キャンセルまたは敵の選択で`false`に戻る。
 * @property outcome 戦闘の勝敗。決着がついていなければ`null`。
 * @property processingNarration 演出中だけ表示する一時的なナレーション(敵の行動・攻撃結果)。
 *   それ以外のナレーションは状態から算出する。
 */
data class BattleUiState(
    val enemies: List<BattleUnitUiModel> = emptyList(),
    val partySlots: List<BattleUnitUiModel?> = List(Formation.SLOT_COUNT) { null },
    val currentActorId: Int? = null,
    val isPlayerTurn: Boolean = false,
    val isProcessing: Boolean = false,
    val isSelectingTarget: Boolean = false,
    val outcome: BattleOutcome? = null,
    val processingNarration: BattleNarration? = null,
) {
    /**
     * 「攻撃」ボタンを押下できるかどうか。
     *
     * プレイヤーの手番で、演出中でなく、勝敗が決まっていないときのみ`true`となる。
     */
    val isAttackEnabled: Boolean
        get() = isPlayerTurn && !isProcessing && outcome == null

    /**
     * 指定したFPIDの個体の表示名を返す。
     *
     * @param id 個体のFPID。
     * @return 表示名。見つからなければ空文字。
     */
    fun nameOf(id: Int): String =
        (enemies + partySlots.filterNotNull()).firstOrNull { it.id == id }?.name.orEmpty()

    /**
     * 現在の状態に応じて表示すべきバトルナレーター。
     *
     * 演出中は[processingNarration]、対象選択状態なら[BattleNarration.SelectTarget]、
     * プレイヤーの手番なら[BattleNarration.SelectAction]となる。該当しなければ`null`。
     */
    val narration: BattleNarration?
        get() = when {
            processingNarration != null -> processingNarration
            outcome != null -> null
            isSelectingTarget -> BattleNarration.SelectTarget
            isPlayerTurn && currentActorId != null -> BattleNarration.SelectAction(nameOf(currentActorId))
            else -> null
        }
}
