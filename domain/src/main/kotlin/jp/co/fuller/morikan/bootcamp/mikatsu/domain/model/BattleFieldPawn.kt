package jp.co.fuller.morikan.bootcamp.mikatsu.domain.model

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.StatStageFormula
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

    /** 最大HP。 */
    val maxHp: Int

    /** 破壊力(A)の基礎値。戦闘で使うときはバフ・デバフを反映した[effectiveStat]を用いる(他の基礎ステータスも同様)。 */
    val attack: Int

    /** 耐久力(B)。ダメージ計算で被攻撃者側として使う。 */
    val defense: Int

    /** マナ出力(C)の基礎値。現状、戦闘では使わない。 */
    val manaOutput: Int

    /** マナ耐性(D)の基礎値。現状、戦闘では使わない。 */
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
     * 命中率。基本値は[jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.HitChanceFormula.BASE_RATE]で、戦闘ごとにその値から始まる。
     * 将来のバフ・デバフで加減算される戦闘中だけの値で、保存データやマスターデータには持たない。
     */
    val accuracy: Int

    /** 回避率。基本値と性質は[accuracy]と同じ。 */
    val evasion: Int

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
     * ステータスの基礎値(バフ・デバフを反映しない値)を返す。基礎ステータス6種と各プロパティの対応表。
     *
     * @param stat 取得するステータス。
     * @return 基礎値。
     */
    fun baseStat(stat: BattleStat): Int = when (stat) {
        BattleStat.ATTACK -> attack
        BattleStat.DEFENSE -> defense
        BattleStat.MANA_OUTPUT -> manaOutput
        BattleStat.MANA_RESISTANCE -> manaResistance
        BattleStat.AGILITY -> agility
        BattleStat.LUCK -> luck
    }

    /**
     * ステータスの有効な段階(そのステータスの全エントリの合計を±12に収めた値)を返す。
     *
     * @param stat 調べるステータス。
     * @return 有効な段階。
     */
    fun effectiveStage(stat: BattleStat): Int = StatStageFormula.effectiveStage(statModifiers, stat)

    /**
     * バフ・デバフを反映した、戦闘で実際に使うステータスの値を返す。ダメージ計算や行動順はこの値を使う。
     *
     * @param stat 取得するステータス。
     * @return `floor(基礎値 × 段階の倍率)`。
     */
    fun effectiveStat(stat: BattleStat): Int = StatStageFormula.apply(baseStat(stat), effectiveStage(stat))

    /** ブレイク状態([BreakState.BROKEN])かどうか。 */
    val isBroken: Boolean get() = breakState == BreakState.BROKEN

    /** 立ち上がり状態([BreakState.RECOVERING])かどうか。 */
    val isRecovering: Boolean get() = breakState == BreakState.RECOVERING

    /** 戦闘不能(現在HPが0以下)かどうか。戦闘不能の個体は行動順から外れ、攻撃対象にも選べない。 */
    val isDefeated: Boolean get() = currentHp <= 0
}
