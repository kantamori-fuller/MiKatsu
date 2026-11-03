package jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleStat
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BreakState
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleSkillUiModel
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleUiState
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleUnitDetailUiModel
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleUnitPosition
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.BattleUnitUiModel
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.StatChangeDetail
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model.StatChangeEntry

/*
 * 戦闘画面と、その部品(UnitStatCard・UnitDetailPopup)のPreviewだけが使うダミーデータ。
 * 本番のコードからは参照しない。
 */

/** Previewで使う、味方1体分のダミーのカード状態。手番中の個体として返す。 */
internal fun previewAlly(
    id: Int = 1,
    name: String = "アリス",
    hp: Int = 90,
    isActive: Boolean = true,
): BattleUnitUiModel = BattleUnitUiModel(
    id = id, name = name, hp = hp, maxHp = 120, mana = 40, maxMana = 50,
    isDefeated = false, isActive = isActive, tp = 30,
)

/** Previewで使う、敵1体分のダミーのカード状態。 */
internal fun previewEnemy(
    id: Int = 3,
    name: String = "ゴブリンA",
    hp: Int = 60,
    isDefeated: Boolean = false,
): BattleUnitUiModel = BattleUnitUiModel(
    id = id, name = name, hp = hp, maxHp = 80, mana = 0, maxMana = 0,
    isDefeated = isDefeated, isActive = false, breakGaugeFraction = 0.3f,
)

/**
 * Previewで使う、味方の手番(行動を選ぶ前)の戦闘画面状態。
 *
 * 味方2体(前衛)と敵3体(うち1体は戦闘不能)がおり、スキルを2つ持つ。
 */
internal fun previewBattleUiState(): BattleUiState {
    val allies = listOf(previewAlly(1, "アリス"), previewAlly(2, "ボブ", hp = 120, isActive = false))
    val enemies = listOf(
        previewEnemy(3, "ゴブリンA"),
        previewEnemy(4, "ゴブリンB", hp = 0, isDefeated = true),
        previewEnemy(5, "オークA", hp = 80),
    )
    return BattleUiState(
        enemySlots = enemies + List(3) { null },
        partySlots = listOf(allies[0], null, allies[1], null, null, null),
        currentActorId = 1,
        isPlayerTurn = true,
        skills = listOf(
            BattleSkillUiModel(id = 2, name = "ディア", mpCost = 10, hpCost = 0, targetIds = setOf(1, 2), isUsable = true),
            BattleSkillUiModel(
                id = 5, name = "ローリング・ソバット", mpCost = 0, hpCost = 0, targetIds = setOf(3, 5),
                isUsable = false, tpCost = 40,
            ),
        ),
        attackTargetIds = setOf(3, 5),
        progress = 3,
    )
}

/** Previewで使う、ステータス変化・防御中を含む個体の詳細ポップアップの内容。 */
internal fun previewUnitDetail(): BattleUnitDetailUiModel = BattleUnitDetailUiModel(
    id = 1, name = "アリス", hp = 90, maxHp = 120, mp = 40, maxMp = 50, tp = 30, maxTp = 100,
    breakGauge = 25, breakState = BreakState.NONE, position = BattleUnitPosition.FRONT,
    attack = 30, defense = 20, manaOutput = 10, manaResistance = 10, agility = 25, luck = 5,
    accuracy = 100, evasion = 100, criticalRate = 4, criticalDamage = 150,
    breakResistance = 100, breakPointRate = 150, turnCount = 2, remainingDistance = 6500,
    activeSkillNames = listOf("ディア", "精神一到"), passiveSkillNames = listOf("鋼の体"),
    isDefending = true,
    statChanges = listOf(
        StatChangeDetail(
            stat = BattleStat.ATTACK, totalStage = 2,
            entries = listOf(StatChangeEntry(1, 10), StatChangeEntry(1, null)),
        ),
        StatChangeDetail(stat = BattleStat.AGILITY, totalStage = -1, entries = listOf(StatChangeEntry(-1, 3))),
    ),
)
