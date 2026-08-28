package jp.co.fuller.morikan.bootcamp.mikatsu.ui.navigation

object MiKatsuDestinations {
    const val MAIN_MENU = "main_menu"
    const val CHARACTER_LIST = "character_list"

    const val ARG_CHARACTER_ID = "characterId"
    const val NEW_CHARACTER_ID = -1
    const val CHARACTER_CREATE = "character_create?$ARG_CHARACTER_ID={$ARG_CHARACTER_ID}"

    fun characterCreateRoute(characterId: Int? = null): String =
        "character_create?$ARG_CHARACTER_ID=${characterId ?: NEW_CHARACTER_ID}"
}
