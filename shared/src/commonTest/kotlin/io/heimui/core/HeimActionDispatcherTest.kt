package io.heimui.core

import io.heimui.core.data.mapper.toDomain
import io.heimui.core.data.serialization.HeimJson
import io.heimui.core.domain.model.HeimValue
import io.heimui.core.domain.model.action.HeimAction
import io.heimui.core.domain.model.action.SetStateAction
import io.heimui.core.domain.model.action.UnknownAction
import io.heimui.core.domain.model.component.ButtonComponent
import io.heimui.core.domain.model.action.ShowSnackbarAction
import io.heimui.core.presentation.action.HeimActionDispatcher
import io.heimui.core.presentation.action.HeimActionInterceptor
import io.heimui.core.presentation.state.HeimStateManager
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HeimActionDispatcherTest {

    @Test
    fun `test action dispatcher executes interceptor chain`() = runTest {
        val executionLog = mutableListOf<String>()
        val stateManager = HeimStateManager(screenId = "test_screen")

        val loggingInterceptor = object : HeimActionInterceptor {
            override suspend fun intercept(
                action: HeimAction,
                stateManager: HeimStateManager,
                next: suspend (HeimAction) -> Unit
            ) {
                executionLog.add("logged: ${(action as ShowSnackbarAction).message}")
                next(action)
            }
        }

        val modifyingInterceptor = object : HeimActionInterceptor {
            override suspend fun intercept(
                action: HeimAction,
                stateManager: HeimStateManager,
                next: suspend (HeimAction) -> Unit
            ) {
                val snackbar = action as ShowSnackbarAction
                next(snackbar.copy(message = "${snackbar.message} [modified]"))
            }
        }

        val dispatcher = HeimActionDispatcher.build {
            addInterceptor(loggingInterceptor)
            addInterceptor(modifyingInterceptor)
        }

        var finalReceivedAction: HeimAction? = null
        dispatcher.dispatch(ShowSnackbarAction("Hello World"), stateManager) { action ->
            finalReceivedAction = action
        }

        assertEquals("logged: Hello World", executionLog.first())
        assertTrue(finalReceivedAction is ShowSnackbarAction)
        assertEquals("Hello World [modified]", (finalReceivedAction as ShowSnackbarAction).message)
    }
    @Test
    fun `set_state and tracking survive the wire`() {
        val screen = HeimJson.decodeScreen(
            """
            {"id":"s","root":{"type":"button","id":"b","title":"Pro","actions":[
              {"type":"set_state","key":" plan ","value":"pro",
               "tracking":{"event":"plan_selected","tier":2}},
              {"type":"set_state","key":"cleared"}
            ]}}
            """.trimIndent()
        ).toDomain()

        val actions = (screen.root as ButtonComponent).actions
        val first = assertIs<SetStateAction>(actions[0])

        // Trimmed, because a key with a stray space would silently never match a visible_if.
        assertEquals("plan", first.key)
        assertEquals(HeimValue.Str("pro"), first.value)

        // Tracking is carried verbatim and never interpreted — the SDK does not know what an
        // "event" or a "tier" is, and must not start knowing.
        assertEquals(HeimValue.Str("plan_selected"), first.tracking?.get("event"))
        assertEquals(HeimValue.Int64(2), first.tracking?.get("tier"))

        // A missing value is an explicit null, so a payload can clear a key it previously set.
        assertEquals(HeimValue.Null, assertIs<SetStateAction>(actions[1]).value)
        assertNull(actions[1].tracking)
    }

    @Test
    fun `an action type this client has never heard of does not take the screen down`() {
        val screen = HeimJson.decodeScreen(
            """{"id":"s","root":{"type":"button","id":"b","title":"x","actions":[
                 {"type":"teleport","destination":"mars"}]}}"""
        ).toDomain()

        // Forward compatibility is the whole reason a client can lag behind its server.
        assertIs<UnknownAction>((screen.root as ButtonComponent).actions.single())
    }

    @Test
    fun `tracking carries nested provider blocks verbatim`() {
        // One block per analytics provider, because each names the same interaction differently.
        // The SDK must carry all of it without knowing that "warehouse" or "attributes" mean
        // anything — the moment it knows, every new provider is a new SDK release.
        val screen = HeimJson.decodeScreen(
            """
            {"id":"s","root":{"type":"button","id":"b","title":"x","actions":[
              {"type":"navigate","screen_id":"catalog","tracking":{
                "primary":{"name":"select_category",
                           "params":{"category_id":"audio","position":2}},
                "warehouse":{"event":"catalog.category.selected",
                             "attributes":[{"key":"surface","value":"home"}]}
              }}]}}
            """.trimIndent()
        ).toDomain()

        val tracking = (screen.root as ButtonComponent).actions.single().tracking!!

        val primary = assertIs<HeimValue.Obj>(tracking["primary"]).fields
        assertEquals(HeimValue.Str("select_category"), primary["name"])
        assertEquals(
            HeimValue.Str("audio"),
            assertIs<HeimValue.Obj>(primary["params"]).fields["category_id"]
        )

        // Arrays survive too, so a provider wanting a list of key/value pairs is expressible.
        val attributes = assertIs<HeimValue.Arr>(
            assertIs<HeimValue.Obj>(tracking["warehouse"]).fields["attributes"]
        ).items
        assertEquals(
            HeimValue.Str("surface"),
            assertIs<HeimValue.Obj>(attributes.single()).fields["key"]
        )
    }

}
