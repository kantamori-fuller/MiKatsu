package jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model

/**
 * バトル画面に表示する、敵または味方1体分の状態を表すUIモデル。
 *
 * 敵([jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Enemy])と
 * 味方([jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Character])はドメイン上
 * 別のモデルだが、バトル画面での見た目(名前・HP・MPの数値とゲージ)は共通のため、
 * 表示専用の型としてこの1つに集約する。戦闘の具体的なロジック実装前の現段階では
 * 被ダメージ等の状態を持たないため、[hp]/[mana]は常にそれぞれ[maxHp]/[maxMana]と同じ値になる。
 *
 * @property id 表示元(敵またはキャラクター)のID。
 * @property name 表示する名前。
 * @property hp 現在のHP。
 * @property maxHp 最大HP。
 * @property mana 現在のMP。
 * @property maxMana 最大MP。
 */
data class BattleUnitUiModel(
    val id: Int,
    val name: String,
    val hp: Int,
    val maxHp: Int,
    val mana: Int,
    val maxMana: Int,
)

/**
 * バトル画面(BattleScreen)の画面状態を表すUiState。
 *
 * @property enemies 敵陣営の一覧。最大6体を想定する。
 * @property party 味方(プレイヤー)陣営の一覧。現状は最大2名までとなる。
 */
data class BattleUiState(
    val enemies: List<BattleUnitUiModel> = emptyList(),
    val party: List<BattleUnitUiModel> = emptyList(),
)
