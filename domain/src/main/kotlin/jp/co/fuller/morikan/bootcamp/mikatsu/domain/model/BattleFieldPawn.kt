package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model

/**
 * バトルフィールド上に存在する、FPID(フィールドポーンID)を持つ対象に共通のインターフェース。
 *
 * [BattleAlly](味方)・[BattleEnemy](敵)は陣営が異なるだけで、いずれもフィールド上の
 * 1個体である点は変わらない。攻撃や回復などの効果は陣営を問わずFPIDを介して対象を
 * 指定することになるため、双方に共通のFPIDアクセス手段を提供することを目的とする。
 */
sealed interface BattleFieldPawn {

    /** その戦闘限定で一意な、フィールド上の個体を識別するID。味方・敵を通じて重複しない。 */
    val fpid: Int

    /** 現在のHP。ダメージを受けると減少し、0未満にはならない。 */
    val currentHp: Int

    /** 最大HP。 */
    val maxHp: Int

    /** 破壊力(A)。ダメージ計算で攻撃者側として使う。 */
    val attack: Int

    /** 耐久力(B)。ダメージ計算で被攻撃者側として使う。 */
    val defense: Int

    /** 素早さ(S)。行動順を決める速度の元になる値([jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.ActionSpeedFormula])。 */
    val agility: Int

    /** 幸運(L)。同時に手番が来た個体の順番を決める際、高い方が先になる。 */
    val luck: Int

    /**
     * 次の手番までに進む必要がある残り距離。開始時と行動後は
     * [jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.ActionTimeline.BASE_DISTANCE]に戻る。
     *
     * 「次の手番までの時間」(`remainingDistance / ActionSpeedFormula.speedOf(agility)`)は
     * 状態として保存せず、必要な都度この値と現在の素早さから計算する。こうすることで、将来戦闘中に
     * 素早さが変化しても、すでに進んだ距離は保たれたまま、残り時間だけが自動で計算し直される。
     */
    val remainingDistance: Double

    /**
     * この個体が終えた、自分自身の手番の回数。フィールド全体のターン数は存在せず、各個体が
     * それぞれ手番の回数を持つ。将来、バフ/デバフの持続をこの回数で数えるために用いる。
     */
    val turnCount: Int

    /** 戦闘不能(現在HPが0以下)かどうか。戦闘不能の個体は行動順から外れ、攻撃対象にも選べない。 */
    val isDefeated: Boolean get() = currentHp <= 0
}
