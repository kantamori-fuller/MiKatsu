package jp.co.fuller.morikan.bootcamp.mikatsu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import jp.co.fuller.morikan.bootcamp.mikatsu.core.theme.MiKatsuTheme
import jp.co.fuller.morikan.bootcamp.mikatsu.data.repository.CharacterRepositoryImpl
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.repository.CharacterRepository
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.DeleteCharacterUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.GetCharacterUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.ObserveCharactersUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.usecase.SaveCharacterUseCase
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.di.MiKatsuViewModelFactory
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.navigation.MiKatsuNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository: CharacterRepository = CharacterRepositoryImpl(applicationContext)
        val viewModelFactory = MiKatsuViewModelFactory(
            observeCharactersUseCase = ObserveCharactersUseCase(repository),
            getCharacterUseCase = GetCharacterUseCase(repository),
            saveCharacterUseCase = SaveCharacterUseCase(repository),
            deleteCharacterUseCase = DeleteCharacterUseCase(repository),
        )

        setContent {
            MiKatsuTheme {
                MiKatsuNavHost(
                    viewModelFactory = viewModelFactory,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
