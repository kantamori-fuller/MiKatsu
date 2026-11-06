package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.StatStageFormula
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.StatusAilmentFormula
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.TacticalPointFormula
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.PassiveSkill

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

    /**
     * 陣営ごとの2行×3列の配置におけるマスの番号(0〜5)。`slot / 3`が行(0=前衛・1=後衛)、`slot % 3`が列を表す。
     * 味方は陣形([Formation])のスロット番号、敵は出現順の連番で、いずれも同じ規則で読む。
     */
    val slot: Int

    /** 現在のHP。ダメージを受けると減少し、0未満にはならない。 */
    val currentHp: Int

    /** 現在のMP。スキルのコストや状態異常(汚染・呪怨)で減少し、0未満にはならない。敵も持つ(戦闘開始時は最大MPと同じ)。 */
    val currentMp: Int

    /** 最大MP。 */
    val maxMp: Int

    /** 最大HP。 */
    val maxHp: Int

    /** 破壊力(A)の基礎値。戦闘で使うときはバフ・デバフを反映した[effectiveStat]を用いる(他の基礎ステータスも同様)。 */
    val attack: Int

    /** 耐久力(B)。ダメージ計算で被攻撃者側として使う。 */
    val defense: Int

    /** マナ出力(C)の基礎値。特殊型攻撃のダメージ計算で攻撃者側として使う。 */
    val manaOutput: Int

    /** マナ耐性(D)の基礎値。特殊型攻撃のダメージ計算で被攻撃者側として使う。 */
    val manaResistance: Int

    /** 素早さ(S)の基礎値。行動順を決める速度の元になる値([jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.ActionSpeedFormula])。 */
    val agility: Int

    /** 幸運(L)の基礎値。同時に手番が来た個体の順番を決める際、高い方が先になる。 */
    val luck: Int

    /**
     * かけられているステータスの段階式バフ・デバフ。かけた分ごとに別のエントリで持つ。
     * 戦闘中だけの値で、戦闘ごとに空から始まり、次の戦闘へは持ち越さない。
     */
    val statModifiers: List<StatModifier>

    /**
     * かかっている状態異常。同じ状態異常は1エントリだけで、種類ごとの効果と処理は
     * [jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.StatusAilmentFormula]が唯一の実装箇所。
     * 戦闘中だけの値で、戦闘ごとに空から始まり、次の戦闘へは持ち越さない。戦闘不能になると全解除される。
     */
    val ailments: List<AilmentState>

    /**
     * ブレイクゲージ(0〜100)。攻撃でダメージを受けるたびに貯まり、100に達するとブレイク状態になって0に戻る。
     * 戦闘中だけの値で、戦闘開始時は0。小数のまま持ち、表示するときだけ切り捨てる。
     */
    val breakGauge: Double

    /**
     * ブレイク耐性。基本は100で、範囲は0〜999。数値は「被ゲージの倍率(%)」であり、耐性が上がると数値は下がる
     * (耐性が20上がる=100→80)。戦闘中だけの値。
     */
    val breakResistance: Int

    /**
     * ブレイクポイント倍率。基本は150。**攻撃する側**のステータスで、与えるゲージの量に掛かる(%)。
     * 将来のバフ・デバフで変わる戦闘中だけの値。
     */
    val breakPointRate: Int

    /**
     * クリティカル倍率(%)の基本値。全個体が同じ固定値([jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.CriticalFormula.BASE_DAMAGE_RATE])を
     * 戦闘中だけ持つ。段階の増減([BattleStat.CRITICAL_DAMAGE])はこれに加算される。
     */
    val criticalDamageRate: Int

    /**
     * 防御状態か。防御中は受けるダメージが30%カットされ、クリティカルが無効になる
     * ([jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.DefenseFormula])。次の自分の手番が始まるか、ブレイクすると解除される。
     * 戦闘中だけの値で、戦闘ごとに`false`から始まり、次の戦闘へは持ち越さない。
     */
    val isDefending: Boolean

    /**
     * 現在のTP(タクティカルポイント)。0〜[maxTp]の整数で、攻撃・手番の開始・防御などで増え、
     * 100を超えた分は捨てる([jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.TacticalPointFormula])。
     * 味方・敵のどちらも持つ。味方は次の戦闘へ持ち越し、敵は戦闘ごとに0から始まる。
     */
    val currentTp: Int

    /** 最大TP。全個体共通の固定値([jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.TacticalPointFormula.MAX_TP])。 */
    val maxTp: Int get() = TacticalPointFormula.MAX_TP

    /** ブレイクの状態。[BreakState.BROKEN]の間は行動できず、被ダメージ増加と被必中の効果を受ける。 */
    val breakState: BreakState

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

    /**
     * この個体が持つパッシブスキル。ダメージ軽減などの常時効果を、味方・敵を問わず
     * 同じ仕組みで適用するために共通で持つ。持たない個体は空。
     */
    val passiveSkills: List<PassiveSkill>

    /**
     * 12属性の耐性。味方は[Character.elementResistances]、敵は[Enemy.elementResistances]と同じ値。
     * 判定と計算は[jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.ElementResistanceFormula]が行う。
     */
    val elementResistances: ElementResistances

    /**
     * 状態異常耐性。味方は[Character.ailmentResistances]、敵は[Enemy.ailmentResistances]と同じ値。
     * 成功率への掛け方は[jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.AilmentResistanceFormula]が行う。
     */
    val ailmentResistances: AilmentResistances

    /**
     * クールタイム(CT)中のスキルの記録。使うと[SkillCooldown]が加わり、本人の手番が終わるたびに進み、明けると消える。
     * 戦闘中だけの値で、`Character`・`Enemy`には持たず、次の戦闘へは持ち越さない。
     */
    val skillCooldowns: List<SkillCooldown>

    /**
     * スキルがクールタイム中(まだ使えない)かどうか。
     *
     * @param skillId 調べるアクティブスキルのID。
     * @return CTが残っていれば`true`。
     */
    fun isOnCooldown(skillId: Int): Boolean = skillCooldowns.any { it.skillId == skillId }

    /**
     * ステータスの基礎値(バフ・デバフを反映しない値)を返す。基礎ステータス6種と各プロパティの対応表。
     *
     * @param stat 取得するステータス。命中率・回避率は基礎値を持たないため渡せない(例外)。
     * @return 基礎値。
     */
    fun baseStat(stat: BattleStat): Int = when (stat) {
        BattleStat.ATTACK -> attack
        BattleStat.DEFENSE -> defense
        BattleStat.MANA_OUTPUT -> manaOutput
        BattleStat.MANA_RESISTANCE -> manaResistance
        BattleStat.AGILITY -> agility
        BattleStat.LUCK -> luck
        BattleStat.ACCURACY, BattleStat.EVASION, BattleStat.CRITICAL_RATE, BattleStat.CRITICAL_DAMAGE ->
            error("命中率・回避率・クリティカル関連は基礎値を持たない。段階はeffectiveStageで取得する: $stat")
    }

    /**
     * ステータスの有効な段階(そのステータスの全エントリの合計を、ステータスごとの上限に収めた値)を返す。
     * 上限は通常±12で、クリティカル発生率だけ±6。
     *
     * @param stat 調べるステータス。
     * @return 有効な段階。
     */
    fun effectiveStage(stat: BattleStat): Int = StatStageFormula.effectiveStage(statModifiers, stat)

    /**
     * バフ・デバフを反映した、戦闘で実際に使うステータスの値を返す。ダメージ計算や行動順はこの値を使う。
     *
     * @param stat 取得するステータス。命中率・回避率・クリティカル関連は6種用の倍率表で値を出すと誤りになるため渡せない(例外)。
     * @return `floor(基礎値 × 段階の倍率)`。
     */
    fun effectiveStat(stat: BattleStat): Int =
        StatusAilmentFormula.adjustStat(stat, StatStageFormula.apply(baseStat(stat), effectiveStage(stat)), ailments)

    /**
     * 指定した状態異常にかかっているか。
     *
     * @param ailment 調べる状態異常。
     * @return かかっていれば`true`。
     */
    fun hasAilment(ailment: StatusAilment): Boolean = ailments.any { it.ailment == ailment }

    /** 行動不能にする状態異常(スタン・凍結・感電・石化・睡眠)のうち、enum順で先頭のもの。かかっていなければ`null`。 */
    val incapacitatingAilment: StatusAilment?
        get() = StatusAilmentFormula.incapacitatingAilmentOf(ailments)

    /** 状態異常で行動不能か。ブレイクとは別で、行動不能の個体は攻撃・スキル・防御ができず、手番が来たら飛ばされる。 */
    val isIncapacitated: Boolean get() = incapacitatingAilment != null

    /**
     * 洗脳または発狂で、プレイヤーが操作できず自動で行動する個体か。
     * この間は魅了・挑発の制限は効かない(行動を決める優先順位は発狂 > 洗脳 > 手動操作。`mental-ailments.md`)。
     */
    val isAutoActing: Boolean
        get() = hasAilment(StatusAilment.MADNESS) || hasAilment(StatusAilment.BRAINWASH)

    /**
     * 挑発により、攻撃技(基礎威力のあるスキル)しか使えず防御もできない状態か。洗脳・発狂で自動で動いている間は制限が効かない。
     */
    val isRestrictedToAttackSkills: Boolean
        get() = hasAilment(StatusAilment.TAUNT) && !isAutoActing

    /** ブレイク状態([BreakState.BROKEN])かどうか。 */
    val isBroken: Boolean get() = breakState == BreakState.BROKEN

    /** 立ち上がり状態([BreakState.RECOVERING])かどうか。 */
    val isRecovering: Boolean get() = breakState == BreakState.RECOVERING

    /** 戦闘不能(現在HPが0以下)かどうか。戦闘不能の個体は行動順から外れ、攻撃対象にも選べない。 */
    val isDefeated: Boolean get() = currentHp <= 0
}
