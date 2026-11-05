package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AutoTurnResult
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleAlly
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleStat
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.SkillCooldown
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.StatusAilment
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkill
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.ActiveSkillEffect
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillArea
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillCategory
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillCost
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillCostType
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillRange
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillTargetSelection
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.skill.SkillTargetSide
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleFieldPawn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * CPUが動かす手番([performAutoTurn])のうち、洗脳・発狂・魅了した敵の行動を検証するテスト。
 * 洗脳は自分以外の自陣営への通常攻撃、発狂は60%で何もせず乱数を 行動判定 → スキル → 相手 の順に引く、魅了した敵は何もできない。
 */
class AutoTurnTest {

    /** 候補の先頭を選ぶ攻撃対象の選び方(候補も記録する)。 */
    private class FirstSelector : EnemyTargetSelector {
        var lastCandidates: List<Int> = emptyList()

        override fun select(field: BattleField, attacker: BattleFieldPawn, candidates: List<BattleFieldPawn>): BattleFieldPawn? {
            lastCandidates = candidates.map { it.fpid }
            return candidates.firstOrNull()
        }
    }

    /** 発狂時に選ばれるテスト用のダメージのないスキル(ディアと同じ。MP10・レンジ3・エリア全体から一体選択・HP50回復)。 */
    private val dia = TEST_DIA

    /** 回数2の範囲攻撃スキル(MPなし・レンジ3・エリア全体・全て対象・基礎威力50・必中)。 */
    private val areaAttack = testActiveSkill(
        id = 20,
        name = "範囲攻撃",
        targetSide = SkillTargetSide.ENEMY,
        range = SkillRange.RANGE_3,
        area = SkillArea.AREA,
        targetSelection = SkillTargetSelection.ALL,
        basePower = 50,
        accuracy = null,
        categories = setOf(SkillCategory.PHYSICAL_ATTACK),
    )

    private fun BattleAlly.mad(vararg others: jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.AilmentState) =
        withAilments(ail(StatusAilment.MADNESS), *others)

    /** 洗脳された味方1(手番)と、同じ陣営の味方2・3、敵10の戦闘。 */
    private fun brainwashedAllyField() = BattleField(
        allies = listOf(
            testAlly(1).withAilments(ail(StatusAilment.BRAINWASH)),
            testAlly(2, slot = 3),
            testAlly(3, slot = 1),
        ),
        enemies = listOf(testEnemy(10, hp = 1000)),
        turnQueue = listOf(1),
    )

    /** 洗脳された味方は、自分以外の自陣営だけを候補にして通常攻撃し、敵は狙わないこと。 */
    @Test
    fun brainwash_ally_attacksOnlyOtherAllies() {
        val selector = FirstSelector()

        val (after, result) = checkNotNull(brainwashedAllyField().performAutoTurn(1, TEST_NORMAL_ATTACK, selector, SequenceRandom()))

        assertEquals(listOf(2, 3), selector.lastCandidates)
        val attacked = (result as AutoTurnResult.Attacked).result
        assertEquals(listOf(2), attacked.hits.map { it.targetFpid })
        assertTrue(after.pawn(2).currentHp < 100)
        assertEquals(100, after.pawn(1).currentHp)
        assertEquals(1000, after.pawn(10).currentHp)
    }

    /** 洗脳された味方は、他の味方が全員戦闘不能なら届く相手がおらず、何もしないこと(フィールドも変わらない)。 */
    @Test
    fun brainwash_ally_noOtherAlly_cannotReach() {
        val field = brainwashedAllyField().copy(
            allies = brainwashedAllyField().allies.map { if (it.fpid == 1) it else it.copy(currentHp = 0) },
        )

        val (after, result) = checkNotNull(field.performAutoTurn(1, TEST_NORMAL_ATTACK, FirstSelector(), SequenceRandom()))

        assertEquals(AutoTurnResult.CannotReach, result)
        assertEquals(field, after)
    }

    /** 洗脳された敵は、ほかの敵を通常攻撃し、味方は狙わないこと。 */
    @Test
    fun brainwash_enemy_attacksOtherEnemies() {
        val field = BattleField(
            allies = listOf(testAlly(1)),
            enemies = listOf(testEnemy(10, slot = 0).withAilments(ail(StatusAilment.BRAINWASH)), testEnemy(11, slot = 1)),
            turnQueue = listOf(10),
        )
        val selector = FirstSelector()

        val (after, result) = checkNotNull(field.performAutoTurn(10, TEST_NORMAL_ATTACK, selector, SequenceRandom()))

        assertEquals(listOf(11), selector.lastCandidates)
        assertEquals(listOf(11), (result as AutoTurnResult.Attacked).result.hits.map { it.targetFpid })
        assertEquals(100, after.pawn(1).currentHp)
    }

    /** 洗脳された敵が単独なら届く相手がおらず、何もしないこと。 */
    @Test
    fun brainwash_enemy_alone_cannotReach() {
        val field = BattleField(
            allies = listOf(testAlly(1)),
            enemies = listOf(testEnemy(10).withAilments(ail(StatusAilment.BRAINWASH))),
            turnQueue = listOf(10),
        )

        val (after, result) = checkNotNull(field.performAutoTurn(10, TEST_NORMAL_ATTACK, FirstSelector(), SequenceRandom()))

        assertEquals(AutoTurnResult.CannotReach, result)
        assertEquals(field, after)
    }

    /** 手動で操作する味方・現在の行動者でない個体・ブレイクや行動不能の個体は、何もせずnullになること。 */
    @Test
    fun notCpuDriven_returnsNull() {
        val manual = BattleField(allies = listOf(testAlly(1)), enemies = listOf(testEnemy(10)), turnQueue = listOf(1))
        val notCurrent = brainwashedAllyField().copy(turnQueue = listOf(2))
        val asleep = brainwashedAllyField().copy(
            allies = brainwashedAllyField().allies.map { if (it.fpid == 1) it.withAilments(ail(StatusAilment.BRAINWASH), ail(StatusAilment.SLEEP)) else it },
        )

        assertNull(manual.performAutoTurn(1, TEST_NORMAL_ATTACK, FirstSelector(), SequenceRandom()))
        assertNull(notCurrent.performAutoTurn(1, TEST_NORMAL_ATTACK, FirstSelector(), SequenceRandom()))
        assertNull(asleep.performAutoTurn(1, TEST_NORMAL_ATTACK, FirstSelector(), SequenceRandom()))
    }

    /** 発狂した味方・敵とダミー1体の戦闘(味方1が手番)。 */
    private fun madField(skills: List<ActiveSkill> = listOf(dia), mp: Int = 100, tweak: (BattleAlly) -> BattleAlly = { it }) = BattleField(
        allies = listOf(tweak(testAlly(1, mp = mp, activeSkills = skills).mad()), testAlly(2, hp = 100, mp = mp)),
        enemies = listOf(testEnemy(10, hp = 1000)),
        turnQueue = listOf(1),
    )

    /** 行動判定が59なら何もせず(これ以外の乱数は引かない)、60なら行動に進むこと(60%の境界)。 */
    @Test
    fun madness_idleBoundary_59idle_60acts() {
        val idleRandom = SequenceRandom(ints = listOf(59))
        val actRandom = SequenceRandom(ints = listOf(60, 0, 0))

        val (idleField, idle) = checkNotNull(madField().performAutoTurn(1, TEST_NORMAL_ATTACK, FirstSelector(), idleRandom))
        val (_, act) = checkNotNull(madField().performAutoTurn(1, TEST_NORMAL_ATTACK, FirstSelector(), actRandom))

        assertEquals(AutoTurnResult.MadnessIdle, idle)
        assertEquals(madField(), idleField)
        assertEquals(listOf(100), idleRandom.untils)
        assertTrue(act is AutoTurnResult.Attacked)
    }

    /** 乱数は 行動判定(100) → スキルの選択(候補数) → 相手の選択(候補数) の順に引くこと。 */
    @Test
    fun madness_randomOrder_actionThenSkillThenTarget() {
        // 通常攻撃とディアの2つが候補。スキル=1(ディア)、相手=0(自分)。
        val random = SequenceRandom(ints = listOf(60, 1, 0))

        val (after, result) = checkNotNull(madField().performAutoTurn(1, TEST_NORMAL_ATTACK, FirstSelector(), random))

        // 発狂用の対象は陣営を無視した全員(味方1・2・敵10)なので3体。
        assertEquals(listOf(100, 2, 3), random.untils.take(3))
        val skill = (result as AutoTurnResult.UsedSkill).result
        assertEquals(dia.id, skill.skillId)
        assertEquals(listOf(1), skill.targets.map { it.targetFpid })
        assertEquals(90, after.pawn(1).currentMp)
    }

    /** 相手は陣営を無視した候補から選ばれ、自分自身も選ばれ得ること(通常攻撃で自分を攻撃する)。 */
    @Test
    fun madness_targetCandidates_includeSelfAndIgnoreSide() {
        val random = SequenceRandom(ints = listOf(60, 0, 0))

        val (after, result) = checkNotNull(madField(skills = emptyList()).performAutoTurn(1, TEST_NORMAL_ATTACK, FirstSelector(), random))

        assertEquals(listOf(1), (result as AutoTurnResult.Attacked).result.hits.map { it.targetFpid })
        assertTrue(after.pawn(1).currentHp < 100)
        // 味方2・敵10も同じ候補に入る(2番目と3番目を選ぶと、陣営を問わず攻撃する)。
        val allyTarget = checkNotNull(madField(skills = emptyList()).performAutoTurn(1, TEST_NORMAL_ATTACK, FirstSelector(), SequenceRandom(ints = listOf(60, 0, 1))))
        val enemyTarget = checkNotNull(madField(skills = emptyList()).performAutoTurn(1, TEST_NORMAL_ATTACK, FirstSelector(), SequenceRandom(ints = listOf(60, 0, 2))))
        assertEquals(listOf(2), (allyTarget.second as AutoTurnResult.Attacked).result.hits.map { it.targetFpid })
        assertEquals(listOf(10), (enemyTarget.second as AutoTurnResult.Attacked).result.hits.map { it.targetFpid })
    }

    /** 今使えないスキル(MP不足・CT中・MP封印)は候補に入らず、スキルの候補数に数えられないこと。 */
    @Test
    fun madness_unusableSkills_areNotCandidates() {
        val expensive = dia.copy(id = 30, costs = listOf(SkillCost(SkillCostType.MP, 500)))
        val cooling = dia.copy(id = 31)
        val sealedField = madField(skills = listOf(dia)) { it.mad(ail(StatusAilment.MP_SEAL)) }
        val random1 = SequenceRandom(ints = listOf(60, 0, 0))
        val random2 = SequenceRandom(ints = listOf(60, 0, 0))
        val random3 = SequenceRandom(ints = listOf(60, 0, 0))

        madField(skills = listOf(expensive)).performAutoTurn(1, TEST_NORMAL_ATTACK, FirstSelector(), random1)
        madField(skills = listOf(cooling)) { it.copy(skillCooldowns = listOf(SkillCooldown(31, 1))) }
            .performAutoTurn(1, TEST_NORMAL_ATTACK, FirstSelector(), random2)
        sealedField.performAutoTurn(1, TEST_NORMAL_ATTACK, FirstSelector(), random3)

        // どれも候補は通常攻撃だけ(スキルの候補数は1)。
        assertEquals(1, random1.untils[1])
        assertEquals(1, random2.untils[1])
        assertEquals(1, random3.untils[1])
    }

    /** 今使えるスキルが1つもない(通常攻撃もCT中)ときは、何もせず、スキル・相手の乱数も引かないこと。 */
    @Test
    fun madness_noCandidates_idle() {
        val field = BattleField(
            allies = listOf(testAlly(1)),
            enemies = listOf(testEnemy(10).withAilments(ail(StatusAilment.MADNESS)).copy(skillCooldowns = listOf(SkillCooldown(1, 1)))),
            turnQueue = listOf(10),
        )
        val random = SequenceRandom(ints = listOf(99))

        val (after, result) = checkNotNull(field.performAutoTurn(10, TEST_NORMAL_ATTACK, FirstSelector(), random))

        assertEquals(AutoTurnResult.MadnessIdle, result)
        assertEquals(field, after)
        assertEquals(listOf(100), random.untils)
    }

    /** 発狂した敵は通常攻撃だけで、味方・敵・自分を等確率の候補から攻撃すること。 */
    @Test
    fun madness_enemy_usesOnlyNormalAttack() {
        val field = BattleField(
            allies = listOf(testAlly(1)),
            enemies = listOf(testEnemy(10, hp = 1000).withAilments(ail(StatusAilment.MADNESS))),
            turnQueue = listOf(10),
        )
        val random = SequenceRandom(ints = listOf(60, 0, 0))

        val (_, result) = checkNotNull(field.performAutoTurn(10, TEST_NORMAL_ATTACK, FirstSelector(), random))

        assertEquals(listOf(100, 1, 2), random.untils.take(3))
        assertEquals(listOf(1), (result as AutoTurnResult.Attacked).result.hits.map { it.targetFpid })
    }

    /** レンジ0のスキルは自分だけが対象になること(相手の候補数は1)。 */
    @Test
    fun madness_range0Skill_targetsOnlySelf() {
        val buff = testActiveSkill(
            id = 21, name = "自己バフ", range = SkillRange.RANGE_0,
            effects = listOf(ActiveSkillEffect.StatChange(BattleStat.ATTACK, 1, 5)),
            categories = setOf(SkillCategory.BUFF),
        )
        // 通常攻撃をCT中にして、候補をバフだけにする。
        val field = madField(skills = listOf(buff)) { it.copy(skillCooldowns = listOf(SkillCooldown(1, 1))) }
        val random = SequenceRandom(ints = listOf(60, 0, 0))

        val (after, result) = checkNotNull(field.performAutoTurn(1, TEST_NORMAL_ATTACK, FirstSelector(), random))

        assertEquals(listOf(100, 1, 1), random.untils.take(3))
        assertEquals(listOf(1), (result as AutoTurnResult.UsedSkill).result.targets.map { it.targetFpid })
        assertEquals(1, after.pawn(1).effectiveStage(BattleStat.ATTACK))
    }

    /** 範囲スキルは、選んだ1体をアンカーにして、通常のルールで広がること(味方2を選ぶと味方陣営の全員)。 */
    @Test
    fun madness_areaSkill_expandsFromAnchor() {
        val field = madField(skills = listOf(areaAttack)) { it.copy(skillCooldowns = listOf(SkillCooldown(1, 1))) }
        // スキルの候補はareaAttackだけ(通常攻撃はCT中)。相手の候補は[1, 2, 10]で、2番目(味方2)をアンカーにする。
        val random = SequenceRandom(ints = listOf(60, 0, 1))

        val (_, result) = checkNotNull(field.performAutoTurn(1, TEST_NORMAL_ATTACK, FirstSelector(), random))

        assertEquals(listOf(1, 2), (result as AutoTurnResult.Attacked).result.hits.map { it.targetFpid })
    }

    /** 発狂が洗脳より優先されること(両方かかっていても発狂として動く)。 */
    @Test
    fun madness_takesPriorityOverBrainwash() {
        val field = madField(skills = emptyList()) { it.withAilments(ail(StatusAilment.MADNESS), ail(StatusAilment.BRAINWASH)) }
        val random = SequenceRandom(ints = listOf(59))

        val (_, result) = checkNotNull(field.performAutoTurn(1, TEST_NORMAL_ATTACK, FirstSelector(), random))

        assertEquals(AutoTurnResult.MadnessIdle, result)
    }

    /** 魅了した敵は、反対の陣営(味方)を選べず通常攻撃もできないので、何もできないこと。 */
    @Test
    fun charm_enemy_cannotDoAnything() {
        val field = BattleField(
            allies = listOf(testAlly(1)),
            enemies = listOf(testEnemy(10).withAilments(ail(StatusAilment.CHARM))),
            turnQueue = listOf(10),
        )

        val (after, result) = checkNotNull(field.performAutoTurn(10, TEST_NORMAL_ATTACK, FirstSelector(), SequenceRandom()))

        assertEquals(AutoTurnResult.NothingToDo, result)
        assertEquals(field, after)
    }

    /** 挑発した敵は、通常攻撃(攻撃技)ができるので通常どおり攻撃すること。 */
    @Test
    fun taunt_enemy_stillAttacks() {
        val field = BattleField(
            allies = listOf(testAlly(1)),
            enemies = listOf(testEnemy(10).withAilments(ail(StatusAilment.TAUNT))),
            turnQueue = listOf(10),
        )

        val (_, result) = checkNotNull(field.performAutoTurn(10, TEST_NORMAL_ATTACK, FirstSelector(), SequenceRandom()))

        assertTrue(result is AutoTurnResult.Attacked)
    }

    /** 発狂した味方がスキルで自分を倒しても、フィールドが壊れず戦闘不能になること。 */
    @Test
    fun madness_selfAttack_canDefeatSelfWithoutBreaking() {
        val field = madField(skills = emptyList()) { it.copy(currentHp = 1) }
        val random = SequenceRandom(ints = listOf(60, 0, 0))

        val (after, _) = checkNotNull(field.performAutoTurn(1, TEST_NORMAL_ATTACK, FirstSelector(), random))

        assertTrue(after.pawn(1).isDefeated)
        assertFalse(after.pawn(1).hasAilment(StatusAilment.MADNESS))
    }
}
