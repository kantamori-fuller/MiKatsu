package jp.co.fuller.morikan.bootcamp.mikatsu.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * アプリ起動時に最初に表示されるメインメニュー画面。
 *
 * 「キャラを作成」「キャラ一覧」の2つの導線を提供することを目的とする。
 * 状態や入力を持たない画面のため専用のViewModelは設けていない。
 *
 * @param onCreateCharacter 「キャラを作成」ボタンが押されたときに呼ばれるコールバック。
 *   キャラ作成画面(新規作成)への遷移をNavHost側に委ねる。
 * @param onShowCharacterList 「キャラ一覧」ボタンが押されたときに呼ばれるコールバック。
 *   キャラ一覧画面への遷移をNavHost側に委ねる。
 * @param modifier このComposableに適用する[Modifier]。
 */
@Composable
fun MainMenuScreen(
    onCreateCharacter: () -> Unit,
    onShowCharacterList: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("MiKatsu", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(32.dp))
            Button(onClick = onCreateCharacter, modifier = Modifier.fillMaxWidth()) {
                Text("キャラを作成")
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onShowCharacterList, modifier = Modifier.fillMaxWidth()) {
                Text("キャラ一覧")
            }
        }
    }
}
