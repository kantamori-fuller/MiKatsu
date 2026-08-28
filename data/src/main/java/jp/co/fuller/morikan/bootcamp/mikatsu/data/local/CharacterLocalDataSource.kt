package jp.co.fuller.morikan.bootcamp.mikatsu.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Character
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.CharacterDraft
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject

/**
 * キャラクターデータを端末内ローカルストレージへ読み書きするデータソース。
 *
 * 一覧データは端末内ファイル(`characters.json`)にJSON配列として保存し、
 * 次に採番すべきキャラクターIDは[mikatsuDataStore](DataStore)で永続管理する。
 * このクラスはストレージの実装詳細を隠蔽し、[CharacterRepositoryImpl]からのみ利用される。
 *
 * @constructor Hiltにより自動的に生成される。
 * @param context キャラクターデータの保存先(ファイル・DataStore)を得るためのアプリケーションContext。
 */
class CharacterLocalDataSource @Inject constructor(@ApplicationContext context: Context) {

    private val appContext = context.applicationContext
    private val dataFile = File(appContext.filesDir, "characters.json")
    private val dataStore = appContext.mikatsuDataStore

    /**
     * 保存されている全キャラクターを読み込む。
     *
     * @return 保存済みキャラクターの一覧。データファイルが存在しない場合は空リスト。
     */
    fun getAll(): List<Character> {
        if (!dataFile.exists()) return emptyList()
        val array = JSONArray(dataFile.readText())
        return (0 until array.length()).map { index -> array.getJSONObject(index).toCharacter() }
    }

    /**
     * 指定したIDのキャラクターを1件取得する。
     *
     * @param id 取得したいキャラクターのID。
     * @return 該当するキャラクター。存在しない場合は`null`。
     */
    fun getById(id: Int): Character? = getAll().find { it.id == id }

    /**
     * キャラクターを保存する。
     *
     * [CharacterDraft.id]が`null`の場合は[reserveNextId]で新しいIDを採番して追加し、
     * 値が設定されている場合は同じIDの既存データを上書きする。
     *
     * @param draft 保存する入力内容。
     * @return 保存後のキャラクター(採番されたIDを含む)。
     */
    suspend fun save(draft: CharacterDraft): Character {
        val characters = getAll().toMutableList()
        val resolvedId = draft.id ?: reserveNextId()
        val character = Character(
            id = resolvedId,
            name = draft.name,
            hp = draft.hp,
            mana = draft.mana,
            attack = draft.attack,
            defense = draft.defense,
            manaOutput = draft.manaOutput,
            manaResistance = draft.manaResistance,
            agility = draft.agility,
            luck = draft.luck,
        )
        val index = characters.indexOfFirst { it.id == resolvedId }
        if (index >= 0) characters[index] = character else characters.add(character)
        writeAll(characters)
        return character
    }

    /**
     * 指定したIDのキャラクターを削除する。
     *
     * @param id 削除対象のキャラクターID。
     */
    fun delete(id: Int) {
        writeAll(getAll().filterNot { it.id == id })
    }

    /**
     * 新規キャラクター用のIDを採番する。
     *
     * 採番済みの次IDをDataStoreへ永続化しているため、
     * キャラクターが削除されて総数が減ってもIDが再利用されることはない。
     * 読み取りと書き込みを[DataStore.edit]のトランザクション内で行うことで、
     * 同時に保存操作が発生してもID の重複採番が起きないようにする。
     *
     * @return 新規キャラクターに割り当てるID。
     */
    private suspend fun reserveNextId(): Int {
        var reservedId = 1
        dataStore.edit { preferences ->
            reservedId = preferences[KEY_NEXT_ID] ?: 1
            preferences[KEY_NEXT_ID] = reservedId + 1
        }
        return reservedId
    }

    /**
     * キャラクター一覧をJSON配列として丸ごとファイルへ書き込む。
     *
     * @param characters 書き込む対象の全キャラクター一覧。
     */
    private fun writeAll(characters: List<Character>) {
        val array = JSONArray()
        characters.forEach { array.put(it.toJson()) }
        dataFile.writeText(array.toString())
    }

    /**
     * JSONオブジェクトを[Character]へ変換する。
     *
     * @receiver 変換元のJSONオブジェクト。
     * @return 変換後の[Character]。
     */
    private fun JSONObject.toCharacter() = Character(
        id = getInt("id"),
        name = getString("name"),
        hp = getInt("hp"),
        mana = getInt("mana"),
        attack = getInt("attack"),
        defense = getInt("defense"),
        manaOutput = getInt("manaOutput"),
        manaResistance = getInt("manaResistance"),
        agility = getInt("agility"),
        luck = getInt("luck"),
    )

    /**
     * [Character]をファイル保存用のJSONオブジェクトへ変換する。
     *
     * @receiver 変換元の[Character]。
     * @return 変換後のJSONオブジェクト。
     */
    private fun Character.toJson() = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("hp", hp)
        put("mana", mana)
        put("attack", attack)
        put("defense", defense)
        put("manaOutput", manaOutput)
        put("manaResistance", manaResistance)
        put("agility", agility)
        put("luck", luck)
    }

    companion object {
        private val KEY_NEXT_ID = intPreferencesKey("next_character_id")
    }
}
