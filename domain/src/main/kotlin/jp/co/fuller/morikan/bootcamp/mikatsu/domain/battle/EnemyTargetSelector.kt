package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleFieldPawn

/**
 * CPUが動かす個体(敵・洗脳された味方)が攻撃する対象の選び方を差し替えられるようにするための抽象。
 *
 * 選べる相手(レンジ・陣営の条件を満たす個体)は呼び出し側が[selectableTargets]で求めて渡し、
 * この抽象はその中から誰を狙うかだけを決める。将来は狙われやすさなどをここで差し替える想定である。
 */
fun interface EnemyTargetSelector {

    /**
     * 攻撃者が狙う対象を、候補の中から選ぶ。
     *
     * @param field 現在の戦闘フィールド。
     * @param attacker 攻撃する個体(敵、または洗脳された個体)。
     * @param candidates 攻撃者が選べる対象の一覧([selectableTargets]の結果)。空でないこと。
     * @return 狙う対象。[candidates]に含まれる個体。候補から選べなければ`null`。
     */
    fun select(field: BattleField, attacker: BattleFieldPawn, candidates: List<BattleFieldPawn>): BattleFieldPawn?
}
