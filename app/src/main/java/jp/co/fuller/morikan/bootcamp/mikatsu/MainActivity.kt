package jp.co.fuller.morikan.bootcamp.mikatsu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import jp.co.fuller.morikan.bootcamp.mikatsu.core.theme.MiKatsuTheme
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.MiKatsuNavHost

/**
 * アプリの唯一のActivityであり、Compose画面全体の起点となるエントリポイント。
 *
 * `@AndroidEntryPoint`を付与することで、[MiKatsuNavHost]配下の各画面が
 * `hiltViewModel()`によりHiltから依存性注入を受けたViewModelを取得できるようにする
 * ことを目的とする。画面遷移そのものの定義は[MiKatsuNavHost]に委ねる。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * Activity生成時に呼ばれ、エッジツーエッジ表示を有効化したうえで
     * アプリのテーマとNavHostを画面全体に描画する。
     *
     * @param savedInstanceState 再生成前の保存状態。本Activityでは使用しない。
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MiKatsuTheme {
                MiKatsuNavHost(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
