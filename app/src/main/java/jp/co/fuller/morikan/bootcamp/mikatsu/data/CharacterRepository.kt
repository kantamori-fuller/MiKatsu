package jp.co.fuller.morikan.bootcamp.mikatsu.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class CharacterRepository(context: Context) {

    private val appContext = context.applicationContext
    private val dataFile = File(appContext.filesDir, "characters.json")
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getAll(): List<Character> {
        if (!dataFile.exists()) return emptyList()
        val array = JSONArray(dataFile.readText())
        return (0 until array.length()).map { index -> array.getJSONObject(index).toCharacter() }
    }

    fun getById(id: Int): Character? = getAll().find { it.id == id }

    fun save(
        id: Int?,
        name: String,
        hp: Int,
        mana: Int,
        attack: Int,
        defense: Int,
        manaOutput: Int,
        manaResistance: Int,
        agility: Int,
        luck: Int,
    ): Character {
        val characters = getAll().toMutableList()
        val resolvedId = id ?: reserveNextId()
        val character = Character(
            id = resolvedId,
            name = name,
            hp = hp,
            mana = mana,
            attack = attack,
            defense = defense,
            manaOutput = manaOutput,
            manaResistance = manaResistance,
            agility = agility,
            luck = luck,
        )
        val index = characters.indexOfFirst { it.id == resolvedId }
        if (index >= 0) characters[index] = character else characters.add(character)
        writeAll(characters)
        return character
    }

    fun delete(id: Int) {
        writeAll(getAll().filterNot { it.id == id })
    }

    private fun reserveNextId(): Int {
        val nextId = prefs.getInt(KEY_NEXT_ID, 1)
        prefs.edit().putInt(KEY_NEXT_ID, nextId + 1).apply()
        return nextId
    }

    private fun writeAll(characters: List<Character>) {
        val array = JSONArray()
        characters.forEach { array.put(it.toJson()) }
        dataFile.writeText(array.toString())
    }

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
        private const val PREFS_NAME = "mikatsu_prefs"
        private const val KEY_NEXT_ID = "next_character_id"
    }
}
