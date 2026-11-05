package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkill

/**
 * 手動で操作する味方が、いまの手番で何か行動を選べるかを返す。「何もできない手番」を自動で飛ばすための唯一の判定箇所。
 *
 * 次のどれか1つでも選べれば`true`(撤退は数えない)。
 * - 通常攻撃: 選べる相手がいる([isSkillUsable]。魅了では反対の陣営が選べないので、敵しか狙えない通常攻撃は選べなくなる)。
 * - 使えるスキル: 設定済みのAスキルのうち、[isSkillUsable]を満たすもの(CT・コスト・対象・挑発の攻撃技の制限を含む)。
 * - 防御: [defend]が成立する(挑発中は成立しない)。
 *
 * 洗脳・発狂の個体は手動では操作できないので`false`(ただし呼び出し側は、先に発狂・洗脳の自動行動として扱うこと)。
 * 戦闘不能・ブレイク中・行動不能の状態異常の個体も`false`。呼び出し側はそれらを先に扱うこと。
 * 先制の手番では、スキル一覧は先制スキルだけになるが、ここでは判定を変えない
 * (先制の手番は、使える先制スキルが無ければ先頭に来た時点で取り消されるため、ここには来ない)。
 *
 * @receiver 判定の基になる戦闘フィールド。
 * @param fpid 判定する味方のFPID。現在の行動者であること(防御の成立条件のため)。
 * @param normalAttack 通常攻撃のスキルデータ(通常攻撃で選べる相手を求めるため)。
 * @return 通常攻撃・使えるスキル・防御のどれかを選べれば`true`。味方として見つからなければ`false`。
 */
fun BattleField.hasAnyManualAction(fpid: Int, normalAttack: ActiveSkill): Boolean {
    val ally = allies.firstOrNull { it.fpid == fpid } ?: return false
    if (ally.isDefeated || ally.isBroken || ally.isIncapacitated || ally.isAutoActing) return false
    return isSkillUsable(fpid, normalAttack) ||
        ally.activeSkills.any { isSkillUsable(fpid, it) } ||
        defend(fpid) != null
}
