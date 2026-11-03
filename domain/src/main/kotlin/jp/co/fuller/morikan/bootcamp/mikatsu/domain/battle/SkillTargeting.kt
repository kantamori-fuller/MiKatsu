package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleAlly
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleFieldPawn
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Formation
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkill
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillArea
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillRange
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillTargetSelection
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillTargetSide
import kotlin.math.abs

/**
 * 陣営の中での段(前衛・後衛)。
 *
 * 味方も敵も2行×3列の配置を持ち、スロット番号の行(`slot / 3`)が0なら前衛、1なら後衛となる。
 * レンジ・陣の判定は、配置上の段ではなく[BattleField.effectiveRowOf]が返す「有効な段」で行う。
 *
 * @property depth 陣営の最前列(敵と向かい合う側)からの深さ。前衛は0、後衛は1。距離の計算に使う。
 */
enum class BattleRow(val depth: Int) {
    /** 前衛。敵と向かい合う段。 */
    FRONT(0),

    /** 後衛。前衛の後ろの段。 */
    BACK(1),
}

/**
 * この個体の、配置上の段(有効な段ではない)。スロット番号の行が0なら前衛、それ以外は後衛。
 *
 * @receiver 段を調べる個体。
 * @return 配置上の段。
 */
private fun BattleFieldPawn.placedRow(): BattleRow =
    if (slot / Formation.COLUMNS == 0) BattleRow.FRONT else BattleRow.BACK

/**
 * この個体の、配置上の列(0〜[Formation.COLUMNS]-1)。前衛と後衛を突き通す「列」の判定に使う。
 *
 * @receiver 列を調べる個体。
 * @return 列の番号。
 */
private fun BattleFieldPawn.column(): Int = slot % Formation.COLUMNS

/**
 * 2つの個体が同じ陣営(ともに味方、またはともに敵)かどうか。
 *
 * @receiver 比べる一方の個体。
 * @param other 比べるもう一方の個体。
 * @return 同じ陣営なら`true`。
 */
private fun BattleFieldPawn.isSameSideAs(other: BattleFieldPawn): Boolean = (this is BattleAlly) == (other is BattleAlly)

/**
 * 指定した個体と同じ陣営の個体の一覧(自分自身を含む)。
 *
 * @receiver 陣営を調べるフィールド。
 * @param pawn 基準となる個体。
 * @return [pawn]と同じ陣営の全個体(戦闘不能を含む)。
 */
private fun BattleField.sideOf(pawn: BattleFieldPawn): List<BattleFieldPawn> =
    if (pawn is BattleAlly) allies else enemies

/**
 * 指定した個体の「有効な段」を求める。
 *
 * 基本は配置上の段だが、その陣営の前衛に生存個体が1体もいない(全員戦闘不能、または空)ときは、
 * 後衛の個体も前衛として扱う(繰り上がり)。前衛が全滅した陣営が、敵からも届かず敵にも届かない
 * 詰みの状態になるのを防ぐためである。レンジ・陣の判定は常にこの有効な段で行う。
 *
 * @receiver 判定の基になる戦闘フィールド。
 * @param pawn 段を調べる個体。このフィールドに属する個体であること。
 * @return 有効な段。
 */
fun BattleField.effectiveRowOf(pawn: BattleFieldPawn): BattleRow {
    if (pawn.placedRow() == BattleRow.FRONT) return BattleRow.FRONT
    val hasLivingFront = sideOf(pawn).any { it.placedRow() == BattleRow.FRONT && !it.isDefeated }
    return if (hasLivingFront) BattleRow.BACK else BattleRow.FRONT
}

/**
 * 使用者から対象までの距離を求める。レンジ([jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillRange.reach])
 * と比べて、届くかどうかを判定するために使う。
 *
 * 一直線上の位置を「味方後衛=0・味方前衛=1・敵前衛=2・敵後衛=3」と考え、両者の有効な段
 * ([effectiveRowOf])から次のように数える。
 * - 反対の陣営: `1 + 使用者の深さ + 対象の深さ`(前衛→前衛=1、前衛↔後衛=2、後衛→後衛=3)。
 * - 同じ陣営: `|使用者の深さ − 対象の深さ|`(同じ段=0、前衛↔後衛=1)。
 *
 * @receiver 判定の基になる戦闘フィールド。
 * @param actor スキルを使う個体。
 * @param target 対象の個体。
 * @return 使用者から対象までの距離。
 */
fun BattleField.distanceBetween(actor: BattleFieldPawn, target: BattleFieldPawn): Int {
    val actorDepth = effectiveRowOf(actor).depth
    val targetDepth = effectiveRowOf(target).depth
    return if (actor.isSameSideAs(target)) abs(actorDepth - targetDepth) else 1 + actorDepth + targetDepth
}

/**
 * 使用者がそのスキルで選べる(タップできる)対象の一覧を求める。
 *
 * レンジ0([SkillRange.RANGE_0])のスキルは、陣営・範囲に関係なく、生存している使用者自身だけが候補となる。
 * それ以外は、次の条件をすべて満たす、戦闘不能でない個体が候補となる。
 * - 陣営: 範囲が[SkillArea.FIELD]なら両陣営。それ以外は[ActiveSkill.targetSide]に従い、
 *   [SkillTargetSide.ALLY]は使用者を含む自陣営、[SkillTargetSide.ENEMY]は反対の陣営。
 * - レンジ内: 使用者からの距離([distanceBetween])がスキルのレンジ以下。
 *
 * 使用者が見つからない、または戦闘不能のときは候補なしとする。候補が0体のスキル(通常攻撃を含む)は使えない。
 * 個体はFPIDで扱うため、結果の各個体の[BattleFieldPawn.fpid]で対象を指定できる。
 *
 * @receiver 判定の基になる戦闘フィールド。
 * @param actorFpid スキルを使う個体のFPID。
 * @param skill 使うスキル。
 * @return 選べる対象の一覧(味方陣営、敵陣営の順)。選べる相手がいなければ空。
 */
fun BattleField.selectableTargets(actorFpid: Int, skill: ActiveSkill): List<BattleFieldPawn> {
    val actor = allPawns.firstOrNull { it.fpid == actorFpid } ?: return emptyList()
    if (actor.isDefeated) return emptyList()
    if (skill.range == SkillRange.RANGE_0) return listOf(actor)
    return allPawns.filter { pawn ->
        !pawn.isDefeated &&
            isInTargetSide(actor, pawn, skill) &&
            distanceBetween(actor, pawn) <= skill.range.reach
    }
}

/**
 * 個体が、スキルの陣営条件に合っているかどうか。
 *
 * @param actor スキルを使う個体。
 * @param pawn 判定する個体。
 * @param skill 使うスキル。
 * @return 範囲が[SkillArea.FIELD]なら常に`true`。それ以外は[ActiveSkill.targetSide]の陣営に属していれば`true`。
 */
private fun isInTargetSide(actor: BattleFieldPawn, pawn: BattleFieldPawn, skill: ActiveSkill): Boolean =
    skill.area == SkillArea.FIELD ||
        when (skill.targetSide) {
            SkillTargetSide.ALLY -> actor.isSameSideAs(pawn)
            SkillTargetSide.ENEMY -> !actor.isSameSideAs(pawn)
        }

/**
 * プレイヤー(または敵のAI)が選んだ対象(アンカー)から、スキルが実際に効く個体の一覧を求める。
 *
 * アンカーは[selectableTargets]の候補に含まれている必要があり、含まれなければ`null`(不成立)とする。
 * レンジ0のスキルは、範囲・対象選択に関係なく使用者自身のみが対象になる。
 * それ以外は、スキルの対象選択と範囲によって対象が決まる。
 * - 対象選択が[SkillTargetSelection.ONE](一体選択)、または範囲が[SkillArea.SINGLE]: アンカー1体のみ。
 * - 対象選択が[SkillTargetSelection.ALL](全て対象)のとき、アンカーが属する「まとまり」の生存個体全員。
 *   [SkillArea.FORMATION]=アンカーと同じ陣営・同じ有効な段、[SkillArea.COLUMN]=アンカーと同じ陣営・同じ列(前衛+後衛)、
 *   [SkillArea.AREA]=アンカーの陣営の全員、[SkillArea.FIELD]=敵味方全員。
 *
 * まとまりの他のメンバーは、レンジの外にいても対象になる(レンジはアンカーにだけ効く)。
 * 戦闘不能の個体は、どの場合も対象に含めない(回復で蘇生しない)。
 *
 * @receiver 判定の基になる戦闘フィールド。
 * @param actorFpid スキルを使う個体のFPID。
 * @param skill 使うスキル。
 * @param anchorFpid 選んだ対象(アンカー)のFPID。
 * @return 効果が及ぶ個体の一覧(アンカーを含む)。アンカーが選べない個体であれば`null`。
 */
fun BattleField.resolveTargets(actorFpid: Int, skill: ActiveSkill, anchorFpid: Int): List<BattleFieldPawn>? {
    val anchor = selectableTargets(actorFpid, skill).firstOrNull { it.fpid == anchorFpid } ?: return null
    if (skill.range == SkillRange.RANGE_0) return listOf(anchor)
    if (skill.targetSelection == SkillTargetSelection.ONE || skill.area == SkillArea.SINGLE) return listOf(anchor)
    val living = allPawns.filterNot { it.isDefeated }
    return when (skill.area) {
        SkillArea.SINGLE -> listOf(anchor)
        SkillArea.FORMATION -> living.filter { it.isSameSideAs(anchor) && effectiveRowOf(it) == effectiveRowOf(anchor) }
        SkillArea.COLUMN -> living.filter { it.isSameSideAs(anchor) && it.column() == anchor.column() }
        SkillArea.AREA -> living.filter { it.isSameSideAs(anchor) }
        SkillArea.FIELD -> living
    }
}
