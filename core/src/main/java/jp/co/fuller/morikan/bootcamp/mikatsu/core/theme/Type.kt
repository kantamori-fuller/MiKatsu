package jp.co.fuller.morikan.bootcamp.mikatsu.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * アプリ全体で使用するMaterial3のタイポグラフィ設定。
 *
 * [MiKatsuTheme]から[androidx.compose.material3.MaterialTheme]へ渡され、
 * 各画面のテキストスタイルの基準として使われることを目的とする。
 */
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)
