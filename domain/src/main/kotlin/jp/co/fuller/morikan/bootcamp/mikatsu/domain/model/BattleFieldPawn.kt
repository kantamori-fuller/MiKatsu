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
}
