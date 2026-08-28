package jp.co.fuller.morikan.bootcamp.mikatsu.core.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/** ダイナミックカラーが使えない環境向けのダークテーマ配色。 */
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

/** ダイナミックカラーが使えない環境向けのライトテーマ配色。 */
private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

/**
 * アプリ全体のテーマを提供するルートComposable。
 *
 * 全ての画面(Screen)を[MaterialTheme]で包み、配色・タイポグラフィをアプリ内で
 * 統一することを目的とする。
 *
 * @param darkTheme ダークテーマを適用するかどうか。デフォルトは端末の設定に追従する。
 * @param dynamicColor Android 12(S)以降で壁紙連動のダイナミックカラーを使用するかどうか。
 * @param content テーマを適用する対象のコンテンツ。
 */
@Composable
fun MiKatsuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
