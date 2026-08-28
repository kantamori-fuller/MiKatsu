package jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository

import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Character
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.CharacterDraft
import kotlinx.coroutines.flow.Flow

/**
 * キャラクターデータへのアクセスを抽象化するRepositoryインターフェース。
 *
 * ドメイン層・UI層はこのインターフェースにのみ依存し、データの保存方式
 * (ローカルファイル、DB、通信など)の詳細を知らない(依存性逆転の原則)。
 * 実装は`data`モジュールが提供する。
 */
interface CharacterRepository {

    /**
     * 保存されている全キャラクターの一覧を購読する。
     *
     * データの正(Single Source of Truth)はこのFlowが流す値であり、
     * 保存・削除が行われるたびに最新の一覧が流れる。
     *
     * @return 全キャラクターの一覧を流し続ける[Flow]。
     */
    fun observeCharacters(): Flow<List<Character>>

    /**
     * 指定したIDのキャラクターを1件取得する。
     *
     * @param id 取得したいキャラクターのID。
     * @return 該当するキャラクター。存在しない場合は`null`。
     */
    suspend fun getCharacter(id: Int): Character?

    /**
     * キャラクターを保存する。
     *
     * [CharacterDraft.id]が`null`の場合は新しいIDを採番して新規保存し、
     * 値が設定されている場合は同じIDのデータへ上書き保存する。
     *
     * @param draft 保存する入力内容。
     * @return 保存後のキャラクター(採番されたIDを含む)。
     */
    suspend fun saveCharacter(draft: CharacterDraft): Character

    /**
     * 指定したIDのキャラクターを削除する。
     *
     * @param id 削除対象のキャラクターID。
     */
    suspend fun deleteCharacter(id: Int)
}
