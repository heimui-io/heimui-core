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
        // The exact shape a real design system uses: one block per analytics provider, because
        // each names the same click differently. The SDK must carry all of it without knowing
        // that "amplitude" or "eventAction" mean anything.
        val screen = HeimJson.decodeScreen(
            """
            {"id":"s","root":{"type":"button","id":"b","title":"x","actions":[
              {"type":"navigate","screen_id":"shopfront","tracking":{
                "analytics":{"eventName":"categorias","eventAction":"click",
                             "attributes":{"event_category":"home","interaction":"event"}},
                "amplitude":{"event":"click categoria home","properties":[{"key":"event_id","value":7}]}
              }}]}}
            """.trimIndent()
        ).toDomain()

        val tracking = (screen.root as ButtonComponent).actions.single().tracking!!

        val analytics = assertIs<HeimValue.Obj>(tracking["analytics"]).fields
        assertEquals(HeimValue.Str("categorias"), analytics["eventName"])
        assertEquals(HeimValue.Str("click"), analytics["eventAction"])
        assertEquals(
            HeimValue.Str("home"),
            assertIs<HeimValue.Obj>(analytics["attributes"]).fields["event_category"]
        )

        // Arrays survive too, so a provider that wants a list of key/value pairs is expressible.
        val props = assertIs<HeimValue.Arr>(
            assertIs<HeimValue.Obj>(tracking["amplitude"]).fields["properties"]
        ).items
        assertEquals(HeimValue.Int64(7), assertIs<HeimValue.Obj>(props.single()).fields["value"])
    }

}
