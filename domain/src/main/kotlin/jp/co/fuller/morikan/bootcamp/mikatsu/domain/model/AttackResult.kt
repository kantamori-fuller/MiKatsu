package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model

/**
 * 実行された攻撃1回分の結果。
 *
 * 画面が「誰が、誰に、どれだけのダメージを与えたか(外れたか)」をナレーションとして表示できるようにする
 * ことを目的とする。範囲攻撃では対象が複数になるため、対象ごとの結果を[hits]に持つ。
 * 個体はいずれもFPIDで指定する。
 *
 * @property attackerFpid 攻撃した個体のFPID。
 * @property hits 攻撃を受けた個体ごとの結果。1体以上で、攻撃を受けた順に並ぶ。
 * @property skillId 攻撃に使ったアクティブスキルのID。通常攻撃は[NORMAL_ATTACK_SKILL_ID]。
 * @property skillName 攻撃に使ったスキルの名前。ナレーションで通常攻撃以外の技名を示すために使う。
 * @property fieldBeforeShots コスト消費・CT開始の直後で、1発目の前の戦闘フィールド。ナレーションと表示を連動させるための表示用スナップショット
 *   (技の最初の文の時点の表示)。`performAttack`が必ず埋め、`null`はテストでの組み立て用。
 * @property fieldAfter TP加算(`settleTp`)まで済んだ技の最終の戦闘フィールド。ナレーションと表示を連動させるための表示用スナップショット
 *   (技の最後の文の時点の表示)。`performAttack`が必ず埋め、`null`はテストでの組み立て用。
 */
data class AttackResult(
    val attackerFpid: Int,
    val hits: List<AttackHit>,
    val skillId: Int = NORMAL_ATTACK_SKILL_ID,
    val skillName: String = "",
    val fieldBeforeShots: BattleField? = null,
    val fieldAfter: BattleField? = null,
) {
    /** 通常攻撃で使ったか(技名ではなく「攻撃!」と表示する対象か)。 */
    val isNormalAttack: Boolean
        get() = skillId == NORMAL_ATTACK_SKILL_ID

    /** 全対象に与えたダメージの合計。 */
    val totalDamage: Int
        get() = hits.sumOf { it.damage }

    companion object {
        /** 通常攻撃(Aスキル1)のスキルID。 */
        const val NORMAL_ATTACK_SKILL_ID = 1
    }
}

/**
 * 攻撃を受けた1体分の結果。ヒットごとの当たり外れを[shots]に持つ。
 *
 * @property targetFpid 攻撃を受けた個体のFPID。
 * @property shots 実際に行ったヒットごとの結果。行った順に並ぶ。対象が戦闘不能になって打ち切られた分は含まない。
 */
data class AttackHit(
    val targetFpid: Int,
    val shots: List<AttackShot>,
) {
    /** その個体に与えたダメージ量(全ヒットの合計)。 */
    val damage: Int
        get() = shots.sumOf { it.damage }

    /** 対象がこの攻撃のどこかのヒットでブレイクしたか。 */
    val causedBreak: Boolean
        get() = shots.any { it.causedBreak }

    /** 実際に行った全てのヒットが外れたか。 */
    val isMissed: Boolean
        get() = shots.none { it.isHit }
}

/**
 * 1ヒット分の結果。ナレーションで「何回目が当たった/外れた」を示すために用いる。
 *
 * @property isHit 命中したか。
 * @property damage そのヒットのダメージ。外れたヒットは0。
 * @property causedBreak このヒットで対象がブレイクしたか。
 * @property critical このヒットのクリティカルの種別。最終的なクリティカル倍率が100%未満なら[CriticalType.BAD_CRITICAL]、以上なら[CriticalType.CRITICAL]で、
 *   発生判定が出なかったヒットと外れたヒットは[CriticalType.NONE]。
 * @property isCriticalBlocked 対象の防御によってクリティカルが無効になったか。このときヒットは通常ヒット扱いで[critical]は[CriticalType.NONE]。
 * @property resistance 対象の属性耐性によるヒットの扱い。無効・反射・吸収のヒットは命中した扱いで、[damage]は0(対象のHPは減らない)。
 * @property absorbedAmount [resistance]が[ResistanceOutcome.ABSORBED]のとき、対象が実際に回復したHP量(最大HPで頭打ち)。それ以外は0。
 * @property reflected [resistance]が[ResistanceOutcome.REFLECTED]のとき、攻撃者に跳ね返ったダメージの結果。それ以外は`null`。
 * @property curedAilments このヒットで解除された状態異常(ダメージを受けた睡眠・凍結)。なければ空。
 * @property fieldAfter このヒットを反映した直後(ダメージ・ブレイク・吸収・反射の反映後)の戦闘フィールド。外れたヒットは直前と同じフィールド。
 *   ナレーションと表示を連動させるための表示用スナップショットで、`performAttack`が必ず埋める。`null`はテストでの組み立て用。
 */
data class AttackShot(
    val isHit: Boolean,
    val damage: Int,
    val causedBreak: Boolean = false,
    val critical: CriticalType = CriticalType.NONE,
    val isCriticalBlocked: Boolean = false,
    val resistance: ResistanceOutcome = ResistanceOutcome.NONE,
    val absorbedAmount: Int = 0,
    val reflected: ReflectedDamage? = null,
    val curedAilments: List<StatusAilment> = emptyList(),
    val fieldAfter: BattleField? = null,
)

/**
 * 属性耐性によって、攻撃・効果が対象に対してどう扱われたか。
 */
enum class ResistanceOutcome {
    /** 耐性の影響なし(倍率の耐性を含む通常の扱い)。 */
    NONE,

    /** 無効。ダメージ・効果がかからない。 */
    NULLIFIED,

    /** 反射。ダメージ・効果が使用者に跳ね返る。 */
    REFLECTED,

    /** 吸収。攻撃のダメージ分のHPを回復する。 */
    ABSORBED,
}

/**
 * 反射によって、攻撃者が自分自身を攻撃したものとして受けたダメージの結果。
 *
 * 反射されたヒットのクリティカル判定結果をそのまま使い、攻撃者の防御・ブレイク・属性耐性・パッシブスキルを反映して求める。
 * 攻撃者の耐性が反射のときは再反射せず、無効扱いになる。
 *
 * @property damage 攻撃者が実際に受けたダメージ。無効・吸収のときは0。
 * @property outcome 攻撃者の耐性による扱い。[ResistanceOutcome.NONE]は通常どおり受けた、[ResistanceOutcome.NULLIFIED]は無効
 *   (攻撃者が反射の耐性を持つ場合を含む)、[ResistanceOutcome.ABSORBED]は攻撃者が吸収。[ResistanceOutcome.REFLECTED]にはならない。
 * @property absorbedAmount [outcome]が[ResistanceOutcome.ABSORBED]のとき、攻撃者が実際に回復したHP量。それ以外は0。
 * @property critical 反射されたヒットのクリティカルの種別(攻撃者の防御でクリティカルが無効になったときは[CriticalType.NONE])。
 * @property isCriticalBlocked 攻撃者の防御によってクリティカルが無効になったか。
 * @property causedBreak 反射ダメージで攻撃者がブレイクしたか。
 * @property curedAilments 反射ダメージで攻撃者から解除された状態異常(睡眠・凍結)。なければ空。
 */
data class ReflectedDamage(
    val damage: Int,
    val outcome: ResistanceOutcome = ResistanceOutcome.NONE,
    val absorbedAmount: Int = 0,
    val critical: CriticalType = CriticalType.NONE,
    val isCriticalBlocked: Boolean = false,
    val causedBreak: Boolean = false,
    val curedAilments: List<StatusAilment> = emptyList(),
)
