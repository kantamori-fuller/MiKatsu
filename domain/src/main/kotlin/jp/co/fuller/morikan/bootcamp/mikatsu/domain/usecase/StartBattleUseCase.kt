package jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.battle.advanceToNextTurn
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Formation
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.BattleRepository
import kotlin.random.Random

/**
 * 戦闘を開始し、初期状態の戦闘フィールドを[BattleRepository]へ設定するUseCase。
 *
 * FPIDの採番は今後も[GetBattleFieldUseCase]だけが行い、本UseCaseはその結果を
 * 戦闘状態の正(SSOT)であるRepositoryへ保存することを目的とする。
 *
 * @property getBattleFieldUseCase FPIDを割り当てて戦闘フィールドを組み立てるUseCase。
 * @property battleRepository 戦闘状態を保持するRepository。
 * @property random 最初の手番で素早さ・幸運値が同じ個体の順番を決める乱数。
 */
class StartBattleUseCase(
    private val getBattleFieldUseCase: GetBattleFieldUseCase,
    private val battleRepository: BattleRepository,
    private val random: Random,
) {

    /**
     * 陣形から戦闘フィールドを組み立て、最初の手番を決めたうえで戦闘状態として設定する。
     *
     * @param formation 味方として参戦するキャラクターの陣形。
     */
    suspend operator fun invoke(formation: Formation) {
        battleRepository.setBattleField(getBattleFieldUseCase(formation).advanceToNextTurn(random))
    }
}
