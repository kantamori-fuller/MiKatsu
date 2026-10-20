package jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.BattleField
import kotlinx.coroutines.flow.Flow

/**
 * 戦闘中に変化する状態([BattleField])へのアクセスを抽象化するRepositoryインターフェース。
 *
 * 現在HPなど戦闘中に変化する状態の正(SSOT)をViewModelではなくここに持たせ、
 * ViewModelは購読した結果からUiStateを構築することを目的とする。
 */
interface BattleRepository {

    /**
     * 現在の戦闘フィールドを購読する。
     *
     * @return 戦闘フィールドの[Flow]。戦闘が開始されていない間は`null`を流す。
     */
    fun observeBattleField(): Flow<BattleField?>

    /**
     * 戦闘フィールドを丸ごと設定する(戦闘開始時の初期化に用いる)。
     *
     * @param battleField 設定する戦闘フィールド。
     */
    suspend fun setBattleField(battleField: BattleField)

    /**
     * 戦闘フィールドを、読み取り・変更・書き込みを一括(atomic)に行って更新する。
     * 戦闘が開始されていない場合は何もしない。
     *
     * @param transform 現在の戦闘フィールドから新しい戦闘フィールドを作る変換。
     */
    suspend fun updateBattleField(transform: (BattleField) -> BattleField)
}
