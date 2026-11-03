package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model

/**
 * 戦闘中のバフ・デバフ(段階式の補正)の対象。基礎ステータス6種と、命中率・回避率・クリティカル発生率・クリティカル倍率。
 *
 * HP・MPは補正の対象にしない。基礎ステータス6種は[BattleFieldPawn.baseStat]が基礎値の対応表で、
 * [BattleFieldPawn.effectiveStat]で補正後の値を求められる。命中率・回避率・クリティカル関連は基礎値を持たず、
 * 段階([BattleFieldPawn.effectiveStage])だけを命中判定([jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.HitChanceFormula])が使う。
 * 倍率表も6種とは別なので、[BattleFieldPawn.effectiveStat]に渡すと例外になる。
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

    /** 命中率。基礎ステータス6種とは別の倍率表を使い、命中判定の段階として扱う(値には変換しない)。 */
    ACCURACY,

    /** 回避率。性質は[ACCURACY]と同じ。 */
    EVASION,

    /** クリティカル発生率。段階は±6。値には変換せず、段階だけを[jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.CriticalFormula]が使う。 */
    CRITICAL_RATE,

    /** クリティカル倍率。段階は±12。性質は[CRITICAL_RATE]と同じ。 */
    CRITICAL_DAMAGE,
}
