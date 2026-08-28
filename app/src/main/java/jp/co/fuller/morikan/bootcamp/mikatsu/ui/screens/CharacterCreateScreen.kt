package jp.co.fuller.morikan.bootcamp.mikatsu.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import jp.co.fuller.morikan.bootcamp.mikatsu.data.CharacterRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterCreateScreen(
    characterId: Int?,
    repository: CharacterRepository,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember { mutableStateOf("") }
    var hp by remember { mutableStateOf("") }
    var mana by remember { mutableStateOf("") }
    var attack by remember { mutableStateOf("") }
    var defense by remember { mutableStateOf("") }
    var manaOutput by remember { mutableStateOf("") }
    var manaResistance by remember { mutableStateOf("") }
    var agility by remember { mutableStateOf("") }
    var luck by remember { mutableStateOf("") }

    LaunchedEffect(characterId) {
        if (characterId != null) {
            repository.getById(characterId)?.let { character ->
                name = character.name
                hp = character.hp.toString()
                mana = character.mana.toString()
                attack = character.attack.toString()
                defense = character.defense.toString()
                manaOutput = character.manaOutput.toString()
                manaResistance = character.manaResistance.toString()
                agility = character.agility.toString()
                luck = character.luck.toString()
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (characterId == null) "キャラを作成" else "キャラを編集") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("名前") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            StatField(label = "H. 体力", value = hp, onValueChange = { hp = it })
            StatField(label = "M. マナ総量", value = mana, onValueChange = { mana = it })
            StatField(label = "A. 破壊力", value = attack, onValueChange = { attack = it })
            StatField(label = "B. 耐久力", value = defense, onValueChange = { defense = it })
            StatField(label = "C. マナ出力", value = manaOutput, onValueChange = { manaOutput = it })
            StatField(label = "D. マナ耐性", value = manaResistance, onValueChange = { manaResistance = it })
            StatField(label = "S. 敏捷", value = agility, onValueChange = { agility = it })
            StatField(label = "L. 幸運", value = luck, onValueChange = { luck = it })
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    repository.save(
                        id = characterId,
                        name = name,
                        hp = hp.toIntOrNull() ?: 0,
                        mana = mana.toIntOrNull() ?: 0,
                        attack = attack.toIntOrNull() ?: 0,
                        defense = defense.toIntOrNull() ?: 0,
                        manaOutput = manaOutput.toIntOrNull() ?: 0,
                        manaResistance = manaResistance.toIntOrNull() ?: 0,
                        agility = agility.toIntOrNull() ?: 0,
                        luck = luck.toIntOrNull() ?: 0,
                    )
                    onSaved()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("保存")
            }
        }
    }
}

@Composable
private fun StatField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> if (input.all { it in '0'..'9' }) onValueChange(input) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(8.dp))
}
