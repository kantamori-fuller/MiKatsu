package jp.co.fuller.morikan.bootcamp.mikatsu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import jp.co.fuller.morikan.bootcamp.mikatsu.data.CharacterRepository
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.screens.CharacterCreateScreen
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.screens.CharacterListScreen
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.screens.MainMenuScreen
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.theme.MiKatsuTheme

sealed interface Screen {
    data object MainMenu : Screen
    data class CharacterCreate(val characterId: Int? = null) : Screen
    data object CharacterList : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = CharacterRepository(applicationContext)
        setContent {
            MiKatsuTheme {
                var screen by remember { mutableStateOf<Screen>(Screen.MainMenu) }
                when (val current = screen) {
                    is Screen.MainMenu -> MainMenuScreen(
                        onCreateCharacter = { screen = Screen.CharacterCreate() },
                        onShowCharacterList = { screen = Screen.CharacterList },
                        modifier = Modifier.fillMaxSize(),
                    )

                    is Screen.CharacterCreate -> CharacterCreateScreen(
                        characterId = current.characterId,
                        repository = repository,
                        onBack = { screen = Screen.MainMenu },
                        onSaved = { screen = Screen.MainMenu },
                        modifier = Modifier.fillMaxSize(),
                    )

                    is Screen.CharacterList -> CharacterListScreen(
                        repository = repository,
                        onBack = { screen = Screen.MainMenu },
                        onSelectCharacter = { id -> screen = Screen.CharacterCreate(id) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}
