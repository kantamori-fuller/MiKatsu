package jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jp.co.fuller.morikan.bootcamp.mikatsu.core.theme.BreakGaugeYellow
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.ActionTimeline
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.BreakFormula
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BreakState
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.R
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleUnitDetailUiModel
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.StatChangeDetail
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.labelRes
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleUnitPosition

/** 裏の戦闘画面にかける黒いフェードの不透明度。 */
private const val FADE_ALPHA = 0.6f

/** ポップアップの最大の高さ。内容が多い場合はこの高さの中でスクロールする。 */
private val POPUP_MAX_HEIGHT = 560.dp

/**
 * カード長押しで開く、個体の詳細ポップアップ。戦闘画面の上に重ねて表示する。
 *
 * 裏の画面には黒い半透明のフェードをかけ、そのタップは届かないようにする。ポップアップの外側のタップと
 * 端末の戻る操作で[onDismiss]を呼び、内側のタップでは閉じない。内容は縦にスクロールできる。
 *
 * @param detail 表示する個体の詳細。
 * @param onDismiss 閉じる操作が行われたときに呼ばれるコールバック。
 * @param modifier このComposableに適用する[Modifier]。
 */
@Composable
internal fun UnitDetailPopup(
    detail: BattleUnitDetailUiModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onDismiss)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = FADE_ALPHA))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .heightIn(max = POPUP_MAX_HEIGHT)
                // 内側のタップを受け止め、外側のタップ(閉じる)へ伝えない。
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                UnitDetailContent(detail)
            }
        }
    }
}

/**
 * [UnitDetailPopup]の中身。名前・HP/MP・ブレイクゲージ・位置・ステータス・戦闘値・スキルを縦に並べる。
 *
 * @param detail 表示する個体の詳細。
 */
@Composable
private fun UnitDetailContent(detail: BattleUnitDetailUiModel) {
    Text(detail.name, style = MaterialTheme.typography.titleLarge)
    Text(
        stringResource(
            when (detail.position) {
                BattleUnitPosition.FRONT -> R.string.battle_position_front
                BattleUnitPosition.BACK -> R.string.battle_position_back
                BattleUnitPosition.BACK_AS_FRONT -> R.string.battle_position_back_as_front
            },
        ),
        style = MaterialTheme.typography.labelLarge,
    )
    GaugeRow(
        text = stringResource(R.string.battle_hp_format, detail.hp, detail.maxHp),
        progress = detail.hp.toFloat() / detail.maxHp.coerceAtLeast(1),
    )
    GaugeRow(
        text = stringResource(R.string.battle_mp_format, detail.mp, detail.maxMp),
        progress = detail.mp.toFloat() / detail.maxMp.coerceAtLeast(1),
        color = MaterialTheme.colorScheme.tertiary,
    )
    GaugeRow(
        text = stringResource(R.string.battle_bg_format, detail.breakGauge, BreakFormula.MAX_GAUGE.toInt()),
        progress = detail.breakGauge.toFloat() / BreakFormula.MAX_GAUGE.toFloat(),
        color = BreakGaugeYellow,
    )
    when (detail.breakState) {
        BreakState.BROKEN -> Text(stringResource(R.string.battle_break_state_broken), style = MaterialTheme.typography.labelMedium)
        BreakState.RECOVERING -> Text(stringResource(R.string.battle_break_state_recovering), style = MaterialTheme.typography.labelMedium)
        BreakState.NONE -> Unit
    }

    SectionTitle(R.string.battle_detail_section_status)
    ValueRow(R.string.label_hp, detail.maxHp.toString())
    ValueRow(R.string.label_mana, detail.maxMp.toString())
    ValueRow(R.string.label_attack, detail.attack.toString())
    ValueRow(R.string.label_defense, detail.defense.toString())
    ValueRow(R.string.label_mana_output, detail.manaOutput.toString())
    ValueRow(R.string.label_mana_resistance, detail.manaResistance.toString())
    ValueRow(R.string.label_agility, detail.agility.toString())
    ValueRow(R.string.label_luck, detail.luck.toString())

    SectionTitle(R.string.battle_detail_section_stat_changes)
    StatChanges(detail.statChanges)

    SectionTitle(R.string.battle_detail_section_battle_values)
    ValueRow(R.string.battle_detail_accuracy, detail.accuracy.toString())
    ValueRow(R.string.battle_detail_evasion, detail.evasion.toString())
    ValueRow(
        R.string.battle_detail_critical_rate,
        stringResource(R.string.battle_detail_percent_format, detail.criticalRate),
    )
    ValueRow(R.string.battle_detail_critical_damage, detail.criticalDamage.toString())
    ValueRow(R.string.battle_detail_break_resistance, detail.breakResistance.toString())
    ValueRow(R.string.battle_detail_break_point_rate, detail.breakPointRate.toString())
    ValueRow(R.string.battle_detail_turn_count, detail.turnCount.toString())
    ValueRow(
        R.string.battle_detail_remaining_distance,
        stringResource(R.string.battle_detail_distance_format, detail.remainingDistance, ActionTimeline.BASE_DISTANCE.toInt()),
    )

    SectionTitle(R.string.label_active_skills)
    SkillNames(detail.activeSkillNames)
    SectionTitle(R.string.label_passive_skills)
    SkillNames(detail.passiveSkillNames)
}

/**
 * 数値表記とバーを縦に並べた1組のゲージ表示。
 *
 * @param text バーの上に表示する「現在/最大」などの表記。
 * @param progress バーの進み具合(0〜1)。
 * @param color バーの色。
 */
@Composable
private fun GaugeRow(text: String, progress: Float, color: Color = MaterialTheme.colorScheme.primary) {
    Column {
        Text(text, style = MaterialTheme.typography.labelMedium)
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = color,
        )
    }
}

/**
 * 見出し1行を表示する。
 *
 * @param titleRes 見出しの文字列リソースID。
 */
@Composable
private fun SectionTitle(titleRes: Int) {
    Text(
        stringResource(titleRes),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 8.dp),
    )
}

/**
 * 「項目名 ........ 値」の1行を表示する。
 *
 * @param labelRes 項目名の文字列リソースID。
 * @param value 表示する値。
 */
@Composable
private fun ValueRow(labelRes: Int, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(labelRes), style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * スキル名を「、」区切りで並べる。1つも無ければ「なし」を表示する。
 *
 * @param names 表示するスキル名の一覧。
 */
@Composable
private fun SkillNames(names: List<String>) {
    Text(
        text = if (names.isEmpty()) {
            stringResource(R.string.battle_detail_skill_none)
        } else {
            names.joinToString(stringResource(R.string.battle_detail_skill_separator))
        },
        style = MaterialTheme.typography.bodyMedium,
    )
}

/**
 * かかっているステータス変化を、ステータスごとに1行で表示する。1つもなければ「なし」を表示する。
 * 例: 「破壊力 +2 (+1 残り10 / +1 残り3)」。合計は±12に収めた有効な段階、内訳は各エントリの段階と残りターン。
 *
 * @param changes 表示するステータス変化。
 */
@Composable
private fun StatChanges(changes: List<StatChangeDetail>) {
    if (changes.isEmpty()) {
        Text(stringResource(R.string.battle_detail_skill_none), style = MaterialTheme.typography.bodyMedium)
        return
    }
    val separator = stringResource(R.string.battle_detail_stat_change_entry_separator)
    changes.forEach { change ->
        Text(
            text = stringResource(
                R.string.battle_detail_stat_change_row,
                stringResource(change.stat.labelRes),
                signed(change.totalStage),
                change.entries
                    .map {
                        val turns = it.remainingTurns
                        if (turns == null) {
                            stringResource(R.string.battle_detail_stat_change_entry_permanent, signed(it.stages))
                        } else {
                            stringResource(R.string.battle_detail_stat_change_entry, signed(it.stages), turns)
                        }
                    }
                    .joinToString(separator),
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/**
 * 段階を符号付きの文字列にする。0以上は「+」を付ける(0は「+0」)。
 *
 * @param stage 段階。
 * @return 「+2」「-1」のような文字列。
 */
private fun signed(stage: Int): String = if (stage >= 0) "+$stage" else stage.toString()
