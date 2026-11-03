package jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BreakState

/**
 * 個体の陣形上の位置を、詳細ポップアップに表示する区分で表す。
 */
enum class BattleUnitPosition {
    /** 配置上も前衛。 */
    FRONT,

    /** 配置上も実際にも後衛。 */
    BACK,

    /** 配置上は後衛だが、前衛が全滅しているため前衛として扱われている(繰り上がり)。 */
    BACK_AS_FRONT,
}

/**
 * 長押しで開く詳細ポップアップに表示する、敵または味方1体分の詳細を表すUIモデル。
 *
 * カード用の[BattleUnitUiModel]を肥大化させないよう、詳細項目はこのモデルに分けて持つ。
 * ブレイクゲージ・残り距離は表示用に切り捨てた整数で持つ。
 *
 * @property id 表示元のFPID(フィールドポーンID)。
 * @property name 表示する名前。同名の個体が複数いる場合は末尾にアルファベットが付く。
 * @property hp 現在のHP。
 * @property maxHp 最大HP(ステータスのHと同じ値)。
 * @property mp 現在のMP。
 * @property maxMp 最大MP(ステータスのMと同じ値)。
 * @property breakGauge ブレイクゲージを切り捨てた整数(0〜100)。
 * @property breakState ブレイクの状態。
 * @property position 陣形上の位置。
 * @property attack ステータスA(破壊力)。
 * @property defense ステータスB(耐久力)。
 * @property manaOutput ステータスC(マナ出力)。
 * @property manaResistance ステータスD(マナ耐性)。
 * @property agility ステータスS(敏捷)。
 * @property luck ステータスL(幸運)。
 * @property accuracy 命中率(%なしの数値)。
 * @property evasion 回避率(%なしの数値)。
 * @property breakResistance ブレイク耐性。
 * @property breakPointRate ブレイクポイント倍率。
 * @property turnCount 行動した回数。
 * @property remainingDistance 次の手番までの残り距離を切り捨てた整数。基準は`ActionTimeline.BASE_DISTANCE`。
 * @property activeSkillNames 設定しているAスキルの名前(通常攻撃は含まない)。
 * @property passiveSkillNames 設定しているPスキルの名前。
 */
data class BattleUnitDetailUiModel(
    val id: Int,
    val name: String,
    val hp: Int,
    val maxHp: Int,
    val mp: Int,
    val maxMp: Int,
    val breakGauge: Int,
    val breakState: BreakState,
    val position: BattleUnitPosition,
    val attack: Int,
    val defense: Int,
    val manaOutput: Int,
    val manaResistance: Int,
    val agility: Int,
    val luck: Int,
    val accuracy: Int,
    val evasion: Int,
    val breakResistance: Int,
    val breakPointRate: Int,
    val turnCount: Int,
    val remainingDistance: Int,
    val activeSkillNames: List<String>,
    val passiveSkillNames: List<String>,
)
