package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleAlly
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleEnemy
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkill
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillArea
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillRange
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillTargetSelection
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillTargetSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * レンジ(距離)・有効な段(繰り上がり)・選べる相手([selectableTargets])・範囲と対象選択([resolveTargets])を検証するテスト。
 *
 * スロットは「0〜2=前衛、3〜5=後衛、列は`slot % 3`」。
 */
class SkillTargetingTest {

    /** 味方のFPIDは1〜、敵のFPIDは11〜を使って陣営を見分けやすくする。 */
    private fun ally(fpid: Int, slot: Int, hp: Int = 100): BattleAlly = testAlly(fpid, slot = slot).copy(currentHp = hp)

    private fun enemy(fpid: Int, slot: Int, hp: Int = 50): BattleEnemy = testEnemy(fpid, slot = slot).copy(currentHp = hp)

    private fun field(allies: List<BattleAlly>, enemies: List<BattleEnemy>) =
        BattleField(allies = allies, enemies = enemies, turnQueue = listOf(allies.first().fpid))

    private fun attackSkill(
        range: SkillRange = SkillRange.RANGE_1,
        area: SkillArea = SkillArea.SINGLE,
        selection: SkillTargetSelection = SkillTargetSelection.ONE,
        side: SkillTargetSide = SkillTargetSide.ENEMY,
    ): ActiveSkill = testActiveSkill(
        id = 1,
        name = "テスト",
        targetSide = side,
        range = range,
        area = area,
        targetSelection = selection,
        basePower = 100,
    )

    private fun BattleField.selectableFpids(actor: Int, skill: ActiveSkill) = selectableTargets(actor, skill).map { it.fpid }

    private fun BattleField.resolvedFpids(actor: Int, skill: ActiveSkill, anchor: Int) =
        resolveTargets(actor, skill, anchor)?.map { it.fpid }?.sorted()

    /** 前衛・後衛がそろっている場合の距離: 前→前=1、前→後=2、後→前=2、後→後=3。 */
    @Test
    fun distance_oppositeSides() {
        val f = field(
            allies = listOf(ally(1, slot = 0), ally(2, slot = 3)),
            enemies = listOf(enemy(11, slot = 0), enemy(12, slot = 3)),
        )
        val (a1, a2) = f.allies
        val (e1, e2) = f.enemies

        assertEquals(1, f.distanceBetween(a1, e1))
        assertEquals(2, f.distanceBetween(a1, e2))
        assertEquals(2, f.distanceBetween(a2, e1))
        assertEquals(3, f.distanceBetween(a2, e2))
        // 敵から味方を見ても同じ距離になる。
        assertEquals(3, f.distanceBetween(e2, a2))
        assertEquals(2, f.distanceBetween(e1, a2))
    }

    /** 同じ陣営の距離: 同じ段=0、前衛↔後衛=1。 */
    @Test
    fun distance_sameSide() {
        val f = field(
            allies = listOf(ally(1, slot = 0), ally(2, slot = 1), ally(3, slot = 3)),
            enemies = listOf(enemy(11, slot = 0)),
        )
        val (a1, a2, a3) = f.allies

        assertEquals(0, f.distanceBetween(a1, a1))
        assertEquals(0, f.distanceBetween(a1, a2))
        assertEquals(1, f.distanceBetween(a1, a3))
        assertEquals(1, f.distanceBetween(a3, a1))
    }

    /** レンジ1は前衛の相手だけ、レンジ2は前衛と後衛の両方に届くこと(前衛の使用者から)。 */
    @Test
    fun selectable_rangeFromFrontActor() {
        val f = field(
            allies = listOf(ally(1, slot = 0)),
            enemies = listOf(enemy(11, slot = 0), enemy(12, slot = 3)),
        )

        assertEquals(listOf(11), f.selectableFpids(1, attackSkill(SkillRange.RANGE_1)))
        assertEquals(listOf(11, 12), f.selectableFpids(1, attackSkill(SkillRange.RANGE_2)))
        assertEquals(listOf(11, 12), f.selectableFpids(1, attackSkill(SkillRange.RANGE_3)))
    }

    /** 後衛の使用者は、レンジ1では届かず、レンジ2で敵前衛、レンジ3で敵後衛まで届くこと。 */
    @Test
    fun selectable_rangeFromBackActor() {
        val f = field(
            allies = listOf(ally(1, slot = 0), ally(2, slot = 3)),
            enemies = listOf(enemy(11, slot = 0), enemy(12, slot = 3)),
        )

        assertEquals(emptyList<Int>(), f.selectableFpids(2, attackSkill(SkillRange.RANGE_1)))
        assertEquals(listOf(11), f.selectableFpids(2, attackSkill(SkillRange.RANGE_2)))
        assertEquals(listOf(11, 12), f.selectableFpids(2, attackSkill(SkillRange.RANGE_3)))
    }

    /** 同じ陣営を選ぶスキルは、使用者自身を含み、前衛↔後衛の距離1以内(レンジ1)で選べること。 */
    @Test
    fun selectable_allySideIncludesSelf() {
        val f = field(
            allies = listOf(ally(1, slot = 0), ally(2, slot = 3)),
            enemies = listOf(enemy(11, slot = 0)),
        )

        val skill = attackSkill(side = SkillTargetSide.ALLY)

        assertEquals(listOf(1, 2), f.selectableFpids(1, skill))
        assertEquals(listOf(1, 2), f.selectableFpids(2, skill))
    }

    /** 戦闘不能の個体は選べず、使用者が戦闘不能・不明なら候補なしになること。 */
    @Test
    fun selectable_excludesDefeated_andNoActorMeansNone() {
        val f = field(
            allies = listOf(ally(1, slot = 0)),
            enemies = listOf(enemy(11, slot = 0, hp = 0), enemy(12, slot = 1)),
        )

        assertEquals(listOf(12), f.selectableFpids(1, attackSkill()))
        assertEquals(emptyList<Int>(), f.selectableFpids(99, attackSkill()))
        val actorDown = f.copy(allies = listOf(ally(1, slot = 0, hp = 0)))
        assertEquals(emptyList<Int>(), actorDown.selectableFpids(1, attackSkill()))
    }

    /** フィールド全体のスキルは、陣営に関わらず両陣営を選べること(レンジ内のみ)。 */
    @Test
    fun selectable_fieldAreaIncludesBothSides() {
        val f = field(
            allies = listOf(ally(1, slot = 0), ally(2, slot = 3)),
            enemies = listOf(enemy(11, slot = 0), enemy(12, slot = 3)),
        )

        val skill = attackSkill(range = SkillRange.RANGE_1, area = SkillArea.FIELD)

        assertEquals(listOf(1, 2, 11), f.selectableFpids(1, skill))
    }

    /** 敵が使用者のとき、味方陣営が「反対の陣営」になること(同じ距離のルールで動く)。 */
    @Test
    fun selectable_enemyActorTargetsAllies() {
        val f = field(
            allies = listOf(ally(1, slot = 0), ally(2, slot = 3)),
            enemies = listOf(enemy(11, slot = 0), enemy(12, slot = 3)),
        )

        assertEquals(listOf(1), f.selectableFpids(11, attackSkill(SkillRange.RANGE_1)))
        // 後衛の敵からは、レンジ1では何も届かない。
        assertEquals(emptyList<Int>(), f.selectableFpids(12, attackSkill(SkillRange.RANGE_1)))
        assertEquals(listOf(1, 2), f.selectableFpids(12, attackSkill(SkillRange.RANGE_3)))
    }

    /** 敵の前衛が全滅すると敵後衛が前衛として扱われ、レンジ1でも届くこと。 */
    @Test
    fun promotion_enemyBackBecomesFront() {
        val f = field(
            allies = listOf(ally(1, slot = 0)),
            enemies = listOf(enemy(11, slot = 0, hp = 0), enemy(12, slot = 3)),
        )

        assertEquals(BattleRow.FRONT, f.effectiveRowOf(f.enemies[1]))
        assertEquals(1, f.distanceBetween(f.allies[0], f.enemies[1]))
        assertEquals(listOf(12), f.selectableFpids(1, attackSkill(SkillRange.RANGE_1)))
    }

    /** 前衛に生存個体がいれば、後衛は後衛のままであること。 */
    @Test
    fun promotion_notWhenFrontAlive() {
        val f = field(
            allies = listOf(ally(1, slot = 0)),
            enemies = listOf(enemy(11, slot = 0), enemy(12, slot = 3)),
        )

        assertEquals(BattleRow.BACK, f.effectiveRowOf(f.enemies[1]))
    }

    /** 前衛が空(誰も配置されていない)でも、後衛が前衛として扱われること。味方の使用者側にも働くこと。 */
    @Test
    fun promotion_emptyFront_andAllyBackActor() {
        val f = field(
            allies = listOf(ally(1, slot = 3)),
            enemies = listOf(enemy(11, slot = 0)),
        )

        assertEquals(BattleRow.FRONT, f.effectiveRowOf(f.allies[0]))
        assertEquals(listOf(11), f.selectableFpids(1, attackSkill(SkillRange.RANGE_1)))
    }

    /** 一体選択(ONE)では、範囲が広くてもアンカー1体だけが対象になること。 */
    @Test
    fun resolve_oneSelection_isAnchorOnly() {
        val f = field(
            allies = listOf(ally(1, slot = 0)),
            enemies = listOf(enemy(11, slot = 0), enemy(12, slot = 1), enemy(13, slot = 3)),
        )

        for (area in SkillArea.entries) {
            val skill = attackSkill(range = SkillRange.RANGE_3, area = area, selection = SkillTargetSelection.ONE)
            assertEquals("area=$area", listOf(12), f.resolvedFpids(1, skill, 12))
        }
    }

    /** 範囲が単体(SINGLE)なら、全て対象(ALL)でもアンカー1体だけが対象になること。 */
    @Test
    fun resolve_singleArea_isAnchorOnlyEvenWithAll() {
        val f = field(
            allies = listOf(ally(1, slot = 0)),
            enemies = listOf(enemy(11, slot = 0), enemy(12, slot = 1)),
        )

        val skill = attackSkill(area = SkillArea.SINGLE, selection = SkillTargetSelection.ALL)

        assertEquals(listOf(11), f.resolvedFpids(1, skill, 11))
    }

    /** 陣(ALL)は、アンカーと同じ陣営・同じ有効な段の生存個体全員(レンジ外のメンバーも含む)が対象になること。 */
    @Test
    fun resolve_formationAll_sameRowOfSameSide() {
        // 敵の前衛が3体(うち1体は戦闘不能)・後衛が1体。使用者の味方は前衛でレンジ1。
        val f = field(
            allies = listOf(ally(1, slot = 0)),
            enemies = listOf(enemy(11, slot = 0), enemy(12, slot = 1), enemy(13, slot = 2, hp = 0), enemy(14, slot = 3)),
        )
        val skill = attackSkill(range = SkillRange.RANGE_1, area = SkillArea.FORMATION, selection = SkillTargetSelection.ALL)

        assertEquals(listOf(11, 12), f.resolvedFpids(1, skill, 11))
        // 後衛はレンジ1の外なのでアンカーにできない。
        assertNull(f.resolvedFpids(1, skill, 14))
    }

    /** 陣(ALL)は、繰り上がった後衛も前衛と同じ陣として扱うこと。 */
    @Test
    fun resolve_formationAll_usesEffectiveRow() {
        val f = field(
            allies = listOf(ally(1, slot = 0)),
            enemies = listOf(enemy(11, slot = 0, hp = 0), enemy(12, slot = 3), enemy(13, slot = 4)),
        )
        val skill = attackSkill(range = SkillRange.RANGE_1, area = SkillArea.FORMATION, selection = SkillTargetSelection.ALL)

        assertEquals(listOf(12, 13), f.resolvedFpids(1, skill, 12))
    }

    /** 列(ALL)は、アンカーと同じ陣営・同じ列の前衛と後衛が対象になること(レンジ外の後衛も含む)。 */
    @Test
    fun resolve_columnAll_throughFrontAndBack() {
        val f = field(
            allies = listOf(ally(1, slot = 0)),
            enemies = listOf(enemy(11, slot = 0), enemy(12, slot = 1), enemy(13, slot = 3), enemy(14, slot = 4)),
        )
        val skill = attackSkill(range = SkillRange.RANGE_1, area = SkillArea.COLUMN, selection = SkillTargetSelection.ALL)

        assertEquals(listOf(11, 13), f.resolvedFpids(1, skill, 11))
        assertEquals(listOf(12, 14), f.resolvedFpids(1, skill, 12))
    }

    /** エリア全体(ALL)は、アンカーの陣営の生存個体全員が対象になること。味方陣営をアンカーにすれば味方全員になること。 */
    @Test
    fun resolve_areaAll_wholeSideOfAnchor() {
        val f = field(
            allies = listOf(ally(1, slot = 0), ally(2, slot = 3), ally(3, slot = 1, hp = 0)),
            enemies = listOf(enemy(11, slot = 0), enemy(12, slot = 3)),
        )

        val enemySkill = attackSkill(range = SkillRange.RANGE_1, area = SkillArea.AREA, selection = SkillTargetSelection.ALL)
        assertEquals(listOf(11, 12), f.resolvedFpids(1, enemySkill, 11))

        val allySkill = attackSkill(range = SkillRange.RANGE_3, area = SkillArea.AREA, side = SkillTargetSide.ALLY, selection = SkillTargetSelection.ALL)
        assertEquals(listOf(1, 2), f.resolvedFpids(1, allySkill, 2))
    }

    /** フィールド全体(ALL)は、敵味方の生存個体全員が対象になること。 */
    @Test
    fun resolve_fieldAll_everyoneAlive() {
        val f = field(
            allies = listOf(ally(1, slot = 0), ally(2, slot = 3, hp = 0)),
            enemies = listOf(enemy(11, slot = 0), enemy(12, slot = 3)),
        )
        val skill = attackSkill(range = SkillRange.RANGE_1, area = SkillArea.FIELD, selection = SkillTargetSelection.ALL)

        assertEquals(listOf(1, 11, 12), f.resolvedFpids(1, skill, 11))
    }

    /** アンカーが選べない個体(レンジ外・陣営違い・戦闘不能・存在しない)なら不成立(null)になること。 */
    @Test
    fun resolve_anchorNotSelectable_returnsNull() {
        val f = field(
            allies = listOf(ally(1, slot = 0), ally(2, slot = 1)),
            enemies = listOf(enemy(11, slot = 0), enemy(12, slot = 3), enemy(13, slot = 1, hp = 0)),
        )
        val skill = attackSkill(range = SkillRange.RANGE_1)

        assertNull(f.resolveTargets(1, skill, 12)) // レンジ外
        assertNull(f.resolveTargets(1, skill, 2)) // 陣営違い(味方)
        assertNull(f.resolveTargets(1, skill, 13)) // 戦闘不能
        assertNull(f.resolveTargets(1, skill, 99)) // 存在しない
        assertEquals(listOf(11), f.resolvedFpids(1, skill, 11))
    }
}
