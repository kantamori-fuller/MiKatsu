package jp.co.fuller.morikan.bootcamp.mikatsu.ui.battle.model

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleStat
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.SkillResult
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.SkillTargetResult
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.StatChangeResult
import org.junit.Assert.assertEquals
import org.junit.Test

/** [toNarrations](スキル結果)が、回復の従来の文と、ステータス変化・外れの文を正しく組み立てることを検証するテスト。 */
class SkillNarrationsTest {

    private val names = mapOf(1 to "味方", 2 to "味方B", 3 to "ゴブリン")
    private val nameOf: (Int) -> String = { names.getValue(it) }

    /** 回復スキルは従来どおり、対象1体なら1文、複数なら合計の1文になること。 */
    @Test
    fun heal_keepsExistingNarration() {
        val single = SkillResult(1, 2, "ディア", listOf(SkillTargetResult(2, 50)))
        val multi = SkillResult(1, 2, "ディア", listOf(SkillTargetResult(1, 10), SkillTargetResult(2, 50)))

        assertEquals(listOf(BattleNarration.SkillResult("味方", "味方B", "ディア", 50)), single.toNarrations(nameOf))
        assertEquals(listOf(BattleNarration.MultiSkillResult("味方", "ディア", 2, 60)), multi.toNarrations(nameOf))
    }

    /** 精神一到は「Aの精神一到!」の後に「Aの破壊力が1段階上がった」が続くこと。 */
    @Test
    fun seishin_usedThenStatUp() {
        val result = SkillResult(
            1, 3, "精神一到",
            listOf(SkillTargetResult(1, 0, true, listOf(StatChangeResult(BattleStat.ATTACK, 1)))),
        )

        assertEquals(
            listOf(
                BattleNarration.SkillUsed("味方", "精神一到"),
                BattleNarration.StatStageChanged("味方", BattleStat.ATTACK, 1),
            ),
            result.toNarrations(nameOf),
        )
    }

    /** 覇気が命中すると、俊敏・耐久力の順に1文ずつ下がった文が続くこと。 */
    @Test
    fun haki_hit_usedThenEachDebuff() {
        val result = SkillResult(
            1, 4, "覇気",
            listOf(
                SkillTargetResult(
                    3, 0, true,
                    listOf(StatChangeResult(BattleStat.AGILITY, -1), StatChangeResult(BattleStat.DEFENSE, -1)),
                ),
            ),
        )

        assertEquals(
            listOf(
                BattleNarration.SkillUsed("味方", "覇気"),
                BattleNarration.StatStageChanged("ゴブリン", BattleStat.AGILITY, -1),
                BattleNarration.StatStageChanged("ゴブリン", BattleStat.DEFENSE, -1),
            ),
            result.toNarrations(nameOf),
        )
    }

    /** 覇気が外れたら「Aの覇気! しかし外れた!」の1文だけになること。 */
    @Test
    fun haki_miss_singleSentence() {
        val result = SkillResult(1, 4, "覇気", listOf(SkillTargetResult(3, 0, false)))

        assertEquals(listOf(BattleNarration.SkillMissed("味方", "覇気")), result.toNarrations(nameOf))
    }

    /** 命中率・回避率の変化も、他のステータスと同じ文(StatStageChanged)になること。 */
    @Test
    fun accuracyAndEvasion_changesUseSameSentence() {
        val result = SkillResult(
            1, 9, "テスト",
            listOf(
                SkillTargetResult(1, 0, true, listOf(StatChangeResult(BattleStat.ACCURACY, 1))),
                SkillTargetResult(3, 0, true, listOf(StatChangeResult(BattleStat.EVASION, -1))),
            ),
        )

        assertEquals(
            listOf(
                BattleNarration.SkillUsed("味方", "テスト"),
                BattleNarration.StatStageChanged("味方", BattleStat.ACCURACY, 1),
                BattleNarration.StatStageChanged("ゴブリン", BattleStat.EVASION, -1),
            ),
            result.toNarrations(nameOf),
        )
    }
}
