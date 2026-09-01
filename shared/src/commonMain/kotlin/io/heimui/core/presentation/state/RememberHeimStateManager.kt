package io.heimui.core.presentation.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Draft storage available to the composition. Null by default: persisting form input is an
 * explicit host decision, since the host owns the (ideally encrypted) storage driver.
 */
public val LocalHeimFormDraftStorage: ProvidableCompositionLocal<HeimFormDraftStorage?> =
    staticCompositionLocalOf { null }

/**
 * Creates a [HeimStateManager] that survives configuration changes and process death.
 *
 * Two distinct mechanisms, because they solve different problems:
 *  - `rememberSaveable` carries the in-memory map across Activity recreation (rotation), which is
 *    fast and needs no I/O.
 *  - [HeimFormDraftStorage] carries it across full process death, restored asynchronously on first
 *    composition.
 *
 * A plain `remember` -- which is what the SDK shipped with -- survives neither.
 */
@Composable
public fun rememberHeimStateManager(
    screenId: String,
    screenVersion: String = DEFAULT_SCREEN_VERSION,
    draftStorage: HeimFormDraftStorage? = LocalHeimFormDraftStorage.current
): HeimStateManager {
    val scope = rememberCoroutineScope()

    val manager = remember(screenId, draftStorage) {
        HeimStateManager(
            screenId = screenId,
            draftStorage = draftStorage,
            scope = scope,
            screenVersion = screenVersion
        )
    }

    // Configuration-change survival: a flat Map<String, String> is natively saveable.
    var savedSnapshot by rememberSaveable(screenId) { mutableStateOf<Map<String, String>>(emptyMap()) }
    var restored by rememberSaveable(screenId, screenVersion) { mutableStateOf(false) }

    LaunchedEffect(screenId, screenVersion, draftStorage) {
        if (restored) return@LaunchedEffect
        if (savedSnapshot.isNotEmpty()) {
            manager.restoreDraft(savedSnapshot)
        } else {
            val draft = draftStorage?.getDraft(screenId)
            // Drop drafts written against a different screen version: a stateKey may since have
            // changed meaning, and restoring it would populate the wrong field.
            if (draft != null && draft[DRAFT_VERSION_KEY] == screenVersion) {
                manager.restoreDraft(draft - DRAFT_VERSION_KEY)
            } else if (draft != null) {
                draftStorage.clearDraft(screenId)
            }
        }
        restored = true
    }

    // Keep the saveable snapshot in step so rotation does not lose the newest keystrokes.
    LaunchedEffect(manager) {
        manager.formState.collect { savedSnapshot = it }
    }

    DisposableEffect(manager) {
        onDispose {
            scope.launch { withContext(NonCancellable) { manager.flushDraft() } }
        }
    }

    return manager
}

public const val DRAFT_VERSION_KEY: String = "__heim_screen_version"
private const val DEFAULT_SCREEN_VERSION = "1.0.0"
