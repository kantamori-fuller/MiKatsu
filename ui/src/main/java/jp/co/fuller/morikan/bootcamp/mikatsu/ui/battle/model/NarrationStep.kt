package jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField

/**
 * 演出中に1文ずつ表示するナレーションと、その文を出すときに画面へ表示する戦闘フィールドの組。
 *
 * ナレーションの進行と、HPなどの表示を連動させる(「1回目の文なら1回目の時点のHP」)ことを目的とする。
 *
 * @property narration 表示するナレーション。
 * @property field この文を出すときに表示する戦闘フィールドのスナップショット。`null`なら表示を変えない
 *   (ダメージのないスキル・防御など、即時反映済みの行動や、スナップショットを持たない結果)。
 */
data class NarrationStep(
    val narration: BattleNarration,
    val field: BattleField? = null,
)
