package jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.ActionTimeline
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleAlly
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleEnemy
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Character
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Formation
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.CharacterRepository
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.EnemyRepository
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.SkillRepository

/**
 * 今回のバトルにおける味方陣営・敵陣営を、FPID(フィールドポーンID)を割り当てた
 * [BattleField]として組み立てるUseCase。
 *
 * FPIDは「誰が誰に効果を及ぼすか」を陣営を問わず指定するための重要な識別子であるため、
 * 味方・敵のどちらか一方だけで独立に採番するのではなく、この1箇所で両陣営を通じて
 * 一意な値を割り当てる。同じ敵種(EID)がフィールドに複数体登場する場合の展開も
 * ここで行う。
 *
 * 敵は出現順にスロット0〜5へ並べる(0〜2が前衛、3〜5が後衛)。
 *
 * 敵の出現条件(敵種ごとの登場数など)を決定する戦闘ロジックは未実装のため、
 * 現段階では暫定的に各敵種を[ENCOUNTER_COUNT_PER_ENEMY]体ずつ登場させる。
 *
 * @property characterRepository 味方キャラクターのデータへのアクセスを提供するRepository。
 * @property enemyRepository 敵のマスターデータへのアクセスを提供するRepository。
 * @property skillRepository スキルのマスターデータへのアクセスを提供するRepository。味方が設定したスキルIDの解決に使う。
 */
class GetBattleFieldUseCase(
    private val characterRepository: CharacterRepository,
    private val enemyRepository: EnemyRepository,
    private val skillRepository: SkillRepository,
) {

    /**
     * 味方・敵にFPIDを割り当てた[BattleField]を組み立てる。
     *
     * 味方のFPIDは陣形のスロット番号順に割り当てる。
     *
     * @param formation 味方として参戦するキャラクターの陣形。
     * @return FPIDを割り当てた[BattleField]。
     */
    suspend operator fun invoke(formation: Formation): BattleField {
        val allies = formation.characterIdsBySlot
            .mapIndexedNotNull { slot, id ->
                id?.let { characterRepository.getCharacter(it) }?.let { character -> slot to character }
            }
            .mapIndexed { index, (slot, character) ->
                newAlly(
                    fpid = index + 1,
                    character = character,
                    slot = slot,
                    currentHp = character.hp,
                    currentMp = character.mana,
                )
            }
        return BattleField(allies = allies, enemies = spawnEnemies(firstFpid = allies.size + 1))
    }

    /**
     * 勝利後の次の戦闘のために、前の戦闘の味方を引き継いだ新しい[BattleField]を組み立てる。
     *
     * 味方は[BattleField.allies]をスロット番号順に並べてFPIDを1から振り直し、キャラクター・スロット・
     * 現在HP・現在MPを引き継ぐ(戦闘不能の味方も現在HP0のまま参戦する)。残り距離と手番の回数は初期状態に戻す。
     * 敵は[invoke]と同じ構成で新しく生成し、味方に続く番号でFPIDを採番する。
     *
     * @param previous 直前の戦闘のフィールド。
     * @return 味方の状態を引き継いだ、次の戦闘のフィールド。最初の手番はまだ決まっていない。
     */
    suspend fun createNextBattleField(previous: BattleField): BattleField {
        val allies = previous.allies
            .sortedBy { it.slot }
            .mapIndexed { index, ally ->
                newAlly(
                    fpid = index + 1,
                    character = ally.character,
                    slot = ally.slot,
                    currentHp = ally.currentHp,
                    currentMp = ally.currentMp,
                )
            }
        return BattleField(allies = allies, enemies = spawnEnemies(firstFpid = allies.size + 1))
    }

    /**
     * 初期状態(残り距離が基準値、手番の回数0)の味方個体を生成する。
     *
     * @param fpid 割り当てるFPID。
     * @param character 味方のキャラクター。
     * @param slot 陣形上のスロット番号。
     * @param currentHp 現在HP。
     * @param currentMp 現在MP。
     * @return 生成した味方個体。キャラクターが設定したスキルIDは、マスターデータから解決して持たせる。
     */
    private suspend fun newAlly(fpid: Int, character: Character, slot: Int, currentHp: Int, currentMp: Int) = BattleAlly(
        fpid = fpid,
        character = character,
        currentHp = currentHp,
        currentMp = currentMp,
        slot = slot,
        remainingDistance = ActionTimeline.BASE_DISTANCE,
        turnCount = 0,
        activeSkills = character.activeSkillIds.distinct().mapNotNull { skillRepository.getActiveSkill(it) },
        passiveSkills = character.passiveSkillIds.distinct().mapNotNull { skillRepository.getPassiveSkill(it) },
    )

    /**
     * 敵のマスターデータから、満タンHPの敵個体を[ENCOUNTER_COUNT_PER_ENEMY]体ずつ生成する。
     *
     * 初戦と次戦の敵の生成・FPID採番・スロットの割り当てを同じ処理に集約し、重複させないことを目的とする。
     * スロットは出現順に0から連番で割り当て(0〜2が前衛、3〜5が後衛)、スロット数([Formation.SLOT_COUNT])を
     * 超える分は出現させない。FPIDの採番ルールは変えない。
     *
     * @param firstFpid 最初の敵に割り当てるFPID。以降は連番で採番する。
     * @return FPIDとスロットを割り当てた敵個体の一覧。
     */
    private suspend fun spawnEnemies(firstFpid: Int): List<BattleEnemy> =
        enemyRepository.getEnemies()
            .flatMap { enemy -> List(ENCOUNTER_COUNT_PER_ENEMY) { enemy } }
            .take(Formation.SLOT_COUNT)
            .mapIndexed { slot, enemy ->
                BattleEnemy(
                    fpid = firstFpid + slot,
                    enemy = enemy,
                    currentHp = enemy.hp,
                    slot = slot,
                    remainingDistance = ActionTimeline.BASE_DISTANCE,
                    turnCount = 0,
                )
            }

    companion object {
        /** 敵種ごとにフィールドへ登場させる暫定の体数。 */
        private const val ENCOUNTER_COUNT_PER_ENEMY = 2
    }
}
