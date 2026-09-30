package jp.co.fuller.morikan.bootcamp.mikatsu.ui.partyformation.components

import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import jp.co.fuller.morikan.bootcamp.mikatsu.domain.model.Character
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.R
import jp.co.fuller.morikan.bootcamp.mikatsu.ui.components.FormationSlotFrame

/**
 * 編成画面の陣形の表における、編集可能な1マス。
 *
 * キャラクターのドロップ先として振る舞い、配置済みのキャラクターは長押しで別のマスへ
 * ドラッグでき、タップで陣形から外せる。配置処理そのものは行わず、操作をコールバックで
 * 通知するのみとすることで、配置ルールをViewModel(ドメイン)側に閉じ込めることを目的とする。
 *
 * ドラッグ中のキャラクターがこのマスの上にあるかどうかは、枠の強調表示にのみ使う
 * 一時的な見た目の状態であるため、このComposable内で保持する。
 *
 * @param character このマスに配置されているキャラクター。空きマスの場合は`null`。
 * @param onDrop キャラクターがこのマスへドロップされたときに、そのキャラクターIDを引数に呼ばれる。
 * @param onClick このマスがタップされたときに呼ばれるコールバック。
 * @param modifier このComposableに適用する[Modifier]。
 */
@Composable
internal fun FormationSlot(
    character: Character?,
    onDrop: (characterId: Int) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isDropHovered by remember { mutableStateOf(false) }
    val currentOnDrop by rememberUpdatedState(onDrop)
    val dropTarget = remember {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                isDropHovered = false
                val characterId = event.draggedCharacterId() ?: return false
                currentOnDrop(characterId)
                return true
            }

            override fun onEntered(event: DragAndDropEvent) {
                isDropHovered = true
            }

            override fun onExited(event: DragAndDropEvent) {
                isDropHovered = false
            }

            override fun onEnded(event: DragAndDropEvent) {
                isDropHovered = false
            }
        }
    }

    val dragShadow = rememberCharacterDragShadow(character?.name.orEmpty())
    FormationSlotFrame(
        modifier = modifier
            .dragAndDropTarget(
                shouldStartDragAndDrop = { event -> event.draggedCharacterId() != null },
                target = dropTarget,
            )
            .then(
                if (character != null) {
                    Modifier.characterDragSourceWithClick(character.id, onClick, dragShadow)
                } else {
                    Modifier
                },
            ),
        highlighted = isDropHovered,
    ) {
        Text(
            text = character?.name ?: stringResource(R.string.formation_empty_slot),
            modifier = Modifier.padding(8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (character != null) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.outline
            },
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
