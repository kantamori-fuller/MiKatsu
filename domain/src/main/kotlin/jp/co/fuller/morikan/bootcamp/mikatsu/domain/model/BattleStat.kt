package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model

/**
 * 戦闘中のバフ・デバフ(段階式の補正)の対象になる、基礎ステータス6種。
 *
 * HP・MPは補正の対象にしない。どのステータスがどの値に対応するかは[BattleFieldPawn.baseStat]が唯一の対応表となる。
 */
enum class BattleStat {
    /** 破壊力(A)。ダメージ計算で攻撃者側として使う。 */
    ATTACK,

    /** 耐久力(B)。ダメージ計算で被攻撃者側として使う。 */
    DEFENSE,

    /** マナ出力(C)。現状、戦闘では使わない。 */
    MANA_OUTPUT,

    /** マナ耐久(D)。現状、戦闘では使わない。 */
    MANA_RESISTANCE,

    /** 俊敏(S)。行動順を決める速度の元になる。 */
    AGILITY,

    /** 幸運(L)。同時に手番が来た個体の順番を決める。 */
    LUCK,
}
