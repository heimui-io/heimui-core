package io.heimui.core.presentation.modal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.heimui.core.domain.model.action.HeimAction
import io.heimui.core.domain.model.action.ShowBottomSheetAction
import io.heimui.core.domain.model.action.ShowDialogAction

/**
 * Pluggable contract for presenting dynamic BottomSheets and Dialogs.
 */
public interface HeimModalPresenter {

    @Composable
    public fun RenderBottomSheet(
        action: ShowBottomSheetAction,
        onDismiss: () -> Unit,
        onAction: (HeimAction) -> Unit,
        content: @Composable () -> Unit
    )

    @Composable
    public fun RenderDialog(
        action: ShowDialogAction,
        onDismiss: () -> Unit,
        onAction: (HeimAction) -> Unit
    )
}

public class DefaultHeimModalPresenter : HeimModalPresenter {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun RenderBottomSheet(
        action: ShowBottomSheetAction,
        onDismiss: () -> Unit,
        onAction: (HeimAction) -> Unit,
        content: @Composable () -> Unit
    ) {
        ModalBottomSheet(
            onDismissRequest = {
                if (action.isDismissible) {
                    onDismiss()
                }
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                if (action.title != null) {
                    Text(
                        text = action.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                content()
            }
        }
    }

    @Composable
    override fun RenderDialog(
        action: ShowDialogAction,
        onDismiss: () -> Unit,
        onAction: (HeimAction) -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = action.title,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = action.message,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDismiss()
                        action.confirmActions.forEach { onAction(it) }
                    }
                ) {
                    Text(action.confirmText)
                }
            },
            dismissButton = if (action.dismissText != null) {
                {
                    TextButton(
                        onClick = {
                            onDismiss()
                            action.dismissActions.forEach { onAction(it) }
                        }
                    ) {
                        Text(action.dismissText)
                    }
                }
            } else null
        )
    }
}

public val LocalHeimModalPresenter: ProvidableCompositionLocal<HeimModalPresenter> =
    staticCompositionLocalOf {
    DefaultHeimModalPresenter()
}
