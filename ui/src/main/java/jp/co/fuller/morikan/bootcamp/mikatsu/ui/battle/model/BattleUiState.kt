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
 * 現在値であり、味方の[mana]はスキルの使用で減少する現在値(敵は現段階では消費されず[maxMana]と同じ)になる。
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
 * スキル一覧に表示する、手番の味方が使えるアクティブスキル1件分のUIモデル。
 *
 * @property id アクティブスキルのID(アクティブ採番のマスターデータのID。FPIDではない)。
 * @property name スキルの名前。
 * @property mpCost 使用に必要なMP。
 * @property hpCost 使用に必要なHP。消費しない場合は0。
 * @property isUsable 現在のMP・HPですべてのコストを支払えるかどうか。`false`のとき一覧で選択できない。
 */
data class BattleSkillUiModel(
    val id: Int,
    val name: String,
    val mpCost: Int,
    val hpCost: Int,
    val isUsable: Boolean,
)

/**
 * プレイヤーが行動を選択している途中の段階(選択状態)。
 *
 * 通常攻撃の対象選択と、スキルの「スキル選択→味方の対象選択」の流れを1つの型で表し、
 * 同時に複数の選択状態にならないことを型で保証することを目的とする。
 */
sealed interface BattleSelection {

    /** 何も選択していない(行動選択の状態)。 */
    data object None : BattleSelection

    /** 通常攻撃の対象となる敵を選んでいる。 */
    data object AttackTarget : BattleSelection

    /** 使うスキルを一覧から選んでいる。 */
    data object SkillList : BattleSelection

    /**
     * 選んだスキルの対象となる味方を選んでいる。
     *
     * @property skillId 選択済みのスキルID。
     */
    data class SkillTarget(val skillId: Int) : BattleSelection
}

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
 * @property selection プレイヤーが行動を選択している途中の段階。「攻撃」「スキル」ボタンの押下で
 *   遷移し、キャンセルまたは対象の選択で[BattleSelection.None]に戻る。
 * @property skills 現在の手番の味方が使えるアクティブスキルの一覧。手番が味方でなければ空。
 * @property outcome 戦闘の勝敗。決着がついていなければ`null`。
 * @property progress マップの進行度(今いる戦闘が何戦目か)。最初は1。
 * @property processingNarration 演出中だけ表示する一時的なナレーション(敵の行動・攻撃結果)。
 *   それ以外のナレーションは状態から算出する。
 */
data class BattleUiState(
    val enemies: List<BattleUnitUiModel> = emptyList(),
    val partySlots: List<BattleUnitUiModel?> = List(Formation.SLOT_COUNT) { null },
    val currentActorId: Int? = null,
    val isPlayerTurn: Boolean = false,
    val isProcessing: Boolean = false,
    val selection: BattleSelection = BattleSelection.None,
    val skills: List<BattleSkillUiModel> = emptyList(),
    val outcome: BattleOutcome? = null,
    val processingNarration: BattleNarration? = null,
    val progress: Int = 1,
) {
    /** プレイヤーが入力できる状態(プレイヤーの手番・演出中でない・勝敗未決)かどうか。 */
    private val isInputAcceptable: Boolean
        get() = isPlayerTurn && !isProcessing && outcome == null

    /** 攻撃対象となる敵を選択している状態かどうか。 */
    val isSelectingTarget: Boolean
        get() = selection == BattleSelection.AttackTarget

    /** スキルの選択中(スキル一覧または味方の対象選択)かどうか。 */
    val isSelectingSkill: Boolean
        get() = selection is BattleSelection.SkillList || selection is BattleSelection.SkillTarget

    /**
     * 「攻撃」ボタンを押下できるかどうか。
     *
     * プレイヤーの手番で、演出中でなく、勝敗が決まっておらず、スキルの選択中でないときのみ`true`となる。
     */
    val isAttackEnabled: Boolean
        get() = isInputAcceptable && !isSelectingSkill

    /**
     * 「スキル」ボタンを押下できるかどうか。
     *
     * [isAttackEnabled]と同じ条件に加え、手番の味方が使えるスキルを1つ以上持ち、
     * 攻撃対象の選択中でないときのみ`true`となる。
     */
    val isSkillEnabled: Boolean
        get() = isInputAcceptable && skills.isNotEmpty() && !isSelectingTarget

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
     * 演出中は[processingNarration]、選択状態に応じて[BattleNarration.SelectTarget]・
     * [BattleNarration.SelectSkill]・[BattleNarration.SelectAllyTarget]、
     * プレイヤーの手番なら[BattleNarration.SelectAction]となる。該当しなければ`null`。
     */
    val narration: BattleNarration?
        get() = when {
            processingNarration != null -> processingNarration
            outcome != null -> null
            selection == BattleSelection.AttackTarget -> BattleNarration.SelectTarget
            selection == BattleSelection.SkillList -> BattleNarration.SelectSkill
            selection is BattleSelection.SkillTarget -> BattleNarration.SelectAllyTarget
            isPlayerTurn && currentActorId != null -> BattleNarration.SelectAction(nameOf(currentActorId))
            else -> null
        }
}
