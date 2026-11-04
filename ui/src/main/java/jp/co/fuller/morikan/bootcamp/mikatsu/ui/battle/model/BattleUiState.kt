package jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleOutcome
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BreakState
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.TacticalPointFormula
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
 * @property breakGaugeFraction ブレイクゲージの表示割合(0〜1)。ゲージを切り捨てた整数値を100で割った値で、数値は表示しない。
 * @property breakState ブレイクの状態。ブレイク中・立ち上がり中はカードにラベルを表示する。
 * @property tp 現在のTP(0〜[maxTp])。カードの右上に円ゲージと数値で表示する。
 * @property maxTp 最大TP。円ゲージの割合(`tp / maxTp`)の分母。
 * @property canReservePreemptive いま先制行動を予約できる個体か(`canReservePreemptive`の結果)。`true`ならカードに細いオレンジの枠を付ける
 *   (対象選択中は予約できないため[BattleUiState.reservableAllyIds]で除かれる)。
 * @property isPreemptiveWaiting 先制行動を予約済み、または先制の手番を待っている(いまの行動者ではない)個体か。`true`ならカードに「先制待ち」を表示する。
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
    val breakGaugeFraction: Float = 0f,
    val breakState: BreakState = BreakState.NONE,
    val tp: Int = 0,
    val maxTp: Int = TacticalPointFormula.MAX_TP,
    val canReservePreemptive: Boolean = false,
    val isPreemptiveWaiting: Boolean = false,
)

/**
 * スキル一覧に表示する、手番の味方が使えるアクティブスキル1件分のUIモデル。
 *
 * @property id アクティブスキルのID(アクティブ採番のマスターデータのID。FPIDではない)。
 * @property name スキルの名前。
 * @property mpCost 使用に必要なMP。
 * @property hpCost 使用に必要なHP。消費しない場合は0。
 * @property tpCost 使用に必要なTP。消費しない場合は0。
 * @property substituteHpCost TPが足りないときに、不足分を肩代わりして消費するHP(TP代替システム)。TPが足りている、またはTPコストがなければ0。
 *   0より大きいとき、スキル一覧のTP表示は「TP40(HP-12)」の形になる(HPが足りず使えない場合も表示する)。
 * @property remainingCoolTime スキルのCT(クールタイム)の残り手番数(使用者の`skillCooldowns`の`remainingTurns`そのまま)。CT中でなければ0。
 *   1以上のとき、スキル一覧のコストの後ろに「CT残り1」の形で表示する(使えない状態のまま)。
 * @property targetIds このスキルで選べる対象のFPID([BattleUnitUiModel.id])。レンジ・陣営の条件を満たす相手で、
 *   手番の味方から見たもの。
 * @property isSelfTargeting レンジ0(使用者自身だけが対象)のスキルか。`true`なら一覧で選んだ瞬間に対象選択を挟まず発動する。
 * @property isUsable 現在のMP・HP・TP(不足分はHPで肩代わり)ですべてのコストを支払え、かつ選べる相手([targetIds])が1体以上いるかどうか。
 *   `false`のとき一覧で選択できない。
 */
data class BattleSkillUiModel(
    val id: Int,
    val name: String,
    val mpCost: Int,
    val hpCost: Int,
    val targetIds: Set<Int>,
    val isUsable: Boolean,
    val isSelfTargeting: Boolean = false,
    val tpCost: Int = 0,
    val substituteHpCost: Int = 0,
    val remainingCoolTime: Int = 0,
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

    /** 通常攻撃の対象を選んでいる。 */
    data object AttackTarget : BattleSelection

    /** 使うスキルを一覧から選んでいる。 */
    data object SkillList : BattleSelection

    /**
     * 選んだスキルの対象を選んでいる。
     *
     * @property skillId 選択済みのスキルID。
     */
    data class SkillTarget(val skillId: Int) : BattleSelection
}

/**
 * バトル画面(BattleScreen)の画面状態を表すUiState。
 *
 * @property enemySlots 敵陣営の、スロット番号ごとの敵。要素数は[Formation.SLOT_COUNT]であり、
 *   敵がいないマスは`null`となる。スロットの行が0(0〜2)が前衛、1(3〜5)が後衛で、列は`スロット番号 % 3`。
 * @property partySlots 味方(プレイヤー)陣営の陣形の、スロット番号ごとの味方。要素数は
 *   [Formation.SLOT_COUNT]であり、味方が配置されていないマスは`null`となる。
 * @property currentActorId 現在の手番の個体のFPID([BattleUnitUiModel.id])。手番の個体がいなければ`null`。
 * @property isPlayerTurn 現在の手番が味方かどうか。
 * @property isPreemptiveTurn 現在の手番が先制の手番(行動順に割り込んだ手番)かどうか。`true`の間は、スキル一覧に先制スキルだけを出し、
 *   攻撃・防御・撤退を無効にし、スキル一覧からはキャンセルできない(先制はやめられない)。
 * @property isProcessing 演出中(敵の行動や攻撃結果の表示中)で、入力を受け付けない状態かどうか。
 *   表示用の値であり、演出中かどうかの実際の判定はViewModelが持つジョブで行う。
 * @property selection プレイヤーが行動を選択している途中の段階。「攻撃」「スキル」ボタンの押下で
 *   遷移し、キャンセルまたは対象の選択で[BattleSelection.None]に戻る。
 * @property skills 現在の手番の味方が使えるアクティブスキルの一覧。手番が味方でなければ空。
 * @property attackTargetIds 通常攻撃で選べる対象のFPID。手番が味方でなければ空。
 * @property outcome 戦闘の勝敗。決着がついていなければ`null`。
 * @property progress マップの進行度(今いる戦闘が何戦目か)。最初は1。
 * @property unitDetails 全個体の詳細。FPIDから[BattleUnitDetailUiModel]への対応表で、戦闘フィールドの更新のたびに組み立て直す。
 * @property detailUnitId 詳細ポップアップを開いている個体のFPID。開いていなければ`null`。
 * @property processingNarration 演出中だけ表示する一時的なナレーション(敵の行動・攻撃結果)。
 *   それ以外のナレーションは状態から算出する。
 */
data class BattleUiState(
    val enemySlots: List<BattleUnitUiModel?> = List(Formation.SLOT_COUNT) { null },
    val partySlots: List<BattleUnitUiModel?> = List(Formation.SLOT_COUNT) { null },
    val currentActorId: Int? = null,
    val isPlayerTurn: Boolean = false,
    val isPreemptiveTurn: Boolean = false,
    val isProcessing: Boolean = false,
    val selection: BattleSelection = BattleSelection.None,
    val skills: List<BattleSkillUiModel> = emptyList(),
    val attackTargetIds: Set<Int> = emptySet(),
    val outcome: BattleOutcome? = null,
    val processingNarration: BattleNarration? = null,
    val progress: Int = 1,
    val unitDetails: Map<Int, BattleUnitDetailUiModel> = emptyMap(),
    val detailUnitId: Int? = null,
) {
    /** 詳細ポップアップに表示する個体の詳細。開いていなければ`null`。 */
    val unitDetail: BattleUnitDetailUiModel?
        get() = detailUnitId?.let(unitDetails::get)

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
     * プレイヤーの手番で、演出中でなく、勝敗が決まっておらず、スキルの選択中でなく、
     * 通常攻撃で選べる相手([attackTargetIds])が1体以上いて、先制の手番でないときのみ`true`となる。
     */
    val isAttackEnabled: Boolean
        get() = isInputAcceptable && !isPreemptiveTurn && !isSelectingSkill && attackTargetIds.isNotEmpty()

    /**
     * 「スキル」ボタンを押下できるかどうか。
     *
     * [isAttackEnabled]と同じ条件に加え、手番の味方が使えるスキルを1つ以上持ち、
     * 攻撃対象の選択中でないときのみ`true`となる。
     */
    val isSkillEnabled: Boolean
        get() = isInputAcceptable && skills.isNotEmpty() && !isSelectingTarget

    /**
     * 「防御」ボタンを押下できるかどうか。
     *
     * プレイヤーの手番で、演出中でなく、勝敗が決まっておらず、先制の手番でなく、選択状態が[BattleSelection.None]のときのみ`true`となる。
     */
    val isDefendEnabled: Boolean
        get() = isInputAcceptable && !isPreemptiveTurn && selection == BattleSelection.None

    /**
     * 選択中に「キャンセル」ボタンを押下できるかどうか。
     *
     * 何かを選択している間は押せるが、先制の手番のスキル一覧からはキャンセルできない(先制の手番は行動しないと終われない)。
     * 先制の手番でも、スキルの対象選択からスキル一覧へ戻るキャンセルはできる。
     */
    val isCancelEnabled: Boolean
        get() = selection != BattleSelection.None && !(isPreemptiveTurn && selection == BattleSelection.SkillList)

    /**
     * 選択していないときに「撤退」ボタンを押下できるかどうか。演出中・勝敗決定後・先制の手番では押せない。
     */
    val isRetreatEnabled: Boolean
        get() = !isProcessing && outcome == null && !isPreemptiveTurn

    /**
     * いま、カードのタップで先制行動を予約できる味方のFPIDの集合。
     *
     * 攻撃・スキルの対象選択中([BattleSelection.AttackTarget]・[BattleSelection.SkillTarget])は、カードのタップを対象選びに使うため空になる。
     * それ以外は、[BattleUnitUiModel.canReservePreemptive]な味方すべて。
     */
    val reservableAllyIds: Set<Int>
        get() = if (selection == BattleSelection.AttackTarget || selection is BattleSelection.SkillTarget) {
            emptySet()
        } else {
            partySlots.filterNotNull().filter { it.canReservePreemptive }.map { it.id }.toSet()
        }

    /**
     * 指定したFPIDの個体の表示名を返す。
     *
     * @param id 個体のFPID。
     * @return 表示名。見つからなければ空文字。
     */
    fun nameOf(id: Int): String =
        (enemySlots.filterNotNull() + partySlots.filterNotNull()).firstOrNull { it.id == id }?.name.orEmpty()

    /**
     * 現在の選択状態で、タップして対象にできる個体のFPID。
     *
     * 通常攻撃の対象選択中は[attackTargetIds]、スキルの対象選択中は選んだスキルの
     * [BattleSkillUiModel.targetIds]、それ以外は空となる。敵・味方どちらのカードにも共通で用いる。
     */
    val selectableTargetIds: Set<Int>
        get() = when (val current = selection) {
            BattleSelection.AttackTarget -> attackTargetIds
            is BattleSelection.SkillTarget -> skills.firstOrNull { it.id == current.skillId }?.targetIds.orEmpty()
            else -> emptySet()
        }

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
