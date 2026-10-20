package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleAlly
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleEnemy
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField

/**
 * 敵が攻撃する対象の選び方を差し替えられるようにするための抽象。
 *
 * 将来は前衛/後衛による狙われやすさをここで差し替える想定である。
 */
fun interface EnemyTargetSelector {

    /**
     * 攻撃者の敵が狙う味方を選ぶ。
     *
     * @param field 現在の戦闘フィールド。
     * @param attacker 攻撃する敵。
     * @return 狙う味方。攻撃できる味方がいなければ`null`。
     */
    fun select(field: BattleField, attacker: BattleEnemy): BattleAlly?
}
