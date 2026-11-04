package jp.co.fuller.morikan.bootcamp.mikatsu.ui.charactercreate.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import jp.co.fuller.morikan.bootcamp.mikatsu.core.theme.MiKatsuTheme

/**
 * ステータス入力用の1行のテキストフィールド。
 *
 * 数値キーボードを表示する見た目上の設定のみを担い、半角数字以外の入力を弾く実際の
 * 検証ロジックは[onValueChange]の呼び出し先(ViewModel側)が担当する。
 *
 * @param label フィールド上部に表示するラベル(例: "H. 体力")。
 * @param value 現在の入力値。
 * @param onValueChange 入力が変化したときに呼ばれるコールバック。
 * @param keyboardType 表示するキーボードの種類。既定は数値キーボード。負数を入力できる欄は、数値キーボードでは「-」を打てない端末があるため文字キーボードを指定する。
 */
@Composable
internal fun StatField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Number,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(8.dp))
}

/** ステータス入力欄の表示を確認するためのPreview。 */
@Preview(showBackground = true, name = "ステータス入力欄")
@Composable
private fun StatFieldPreview() {
    MiKatsuTheme(darkTheme = false) {
        StatField(label = "H. 体力", value = "120", onValueChange = {})
    }
}
