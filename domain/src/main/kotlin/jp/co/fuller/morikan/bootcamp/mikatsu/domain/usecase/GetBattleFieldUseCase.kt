package jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleAlly
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleEnemy
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.CharacterRepository
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.EnemyRepository

/**
 * 今回のバトルにおける味方陣営・敵陣営を、FPID(フィールドポーンID)を割り当てた
 * [BattleField]として組み立てるUseCase。
 *
 * FPIDは「誰が誰に効果を及ぼすか」を陣営を問わず指定するための重要な識別子であるため、
 * 味方・敵のどちらか一方だけで独立に採番するのではなく、この1箇所で両陣営を通じて
 * 一意な値を割り当てる。同じ敵種(EID)がフィールドに複数体登場する場合の展開も
 * ここで行う。
 *
 * 敵の出現条件(敵種ごとの登場数など)を決定する戦闘ロジックは未実装のため、
 * 現段階では暫定的に各敵種を[ENCOUNTER_COUNT_PER_ENEMY]体ずつ登場させる。
 *
 * @property characterRepository 味方キャラクターのデータへのアクセスを提供するRepository。
 * @property enemyRepository 敵のマスターデータへのアクセスを提供するRepository。
 */
class GetBattleFieldUseCase(
    private val characterRepository: CharacterRepository,
    private val enemyRepository: EnemyRepository,
) {

    /**
     * 味方・敵にFPIDを割り当てた[BattleField]を組み立てる。
     *
     * @param partyCharacterIds 味方として参戦するキャラクターのID一覧。
     * @return FPIDを割り当てた[BattleField]。
     */
    suspend operator fun invoke(partyCharacterIds: List<Int>): BattleField {
        var nextFpid = 1

        val allies = partyCharacterIds
            .mapNotNull { id -> characterRepository.getCharacter(id) }
            .map { character -> BattleAlly(fpid = nextFpid++, character = character) }

        val enemies = enemyRepository.getEnemies().flatMap { enemy ->
            List(ENCOUNTER_COUNT_PER_ENEMY) { BattleEnemy(fpid = nextFpid++, enemy = enemy) }
        }

        return BattleField(allies = allies, enemies = enemies)
    }

    companion object {
        /** 敵種ごとにフィールドへ登場させる暫定の体数。 */
        private const val ENCOUNTER_COUNT_PER_ENEMY = 2
    }
}
