package jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleAlly
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleEnemy
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Character
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Enemy

/**
 * テスト用に味方個体を生成する。
 *
 * @param fpid 個体のFPID。
 * @param agility 素早さ。
 * @param luck 幸運。
 * @param hp 現在HP(最大HPも同値)。
 * @param attack 破壊力。
 * @param defense 耐久力。
 * @return 生成した味方個体。
 */
fun testAlly(
    fpid: Int,
    agility: Int = 100,
    luck: Int = 0,
    hp: Int = 100,
    attack: Int = 100,
    defense: Int = 100,
) = BattleAlly(
    fpid = fpid,
    character = Character(
        id = fpid, name = "味方$fpid", hp = hp, mana = 0, attack = attack, defense = defense,
        manaOutput = 0, manaResistance = 0, agility = agility, luck = luck,
    ),
    currentHp = hp,
    slot = fpid,
    remainingDistance = ActionTimeline.BASE_DISTANCE,
    turnCount = 0,
)

/**
 * テスト用に敵個体を生成する。
 *
 * @param fpid 個体のFPID。
 * @param agility 素早さ。
 * @param luck 幸運。
 * @param hp 現在HP(最大HPも同値)。
 * @param attack 破壊力。
 * @param defense 耐久力。
 * @return 生成した敵個体。
 */
fun testEnemy(
    fpid: Int,
    agility: Int = 100,
    luck: Int = 0,
    hp: Int = 50,
    attack: Int = 100,
    defense: Int = 100,
) = BattleEnemy(
    fpid = fpid,
    enemy = Enemy(
        id = 1, name = "敵", hp = hp, mana = 0, attack = attack, defense = defense,
        manaOutput = 0, manaResistance = 0, agility = agility, luck = luck,
    ),
    currentHp = hp,
    remainingDistance = ActionTimeline.BASE_DISTANCE,
    turnCount = 0,
)
