package io.heimui.core

import io.heimui.core.data.resilience.HeimCircuitBreaker
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class HeimCircuitBreakerTest {

    @Test
    fun `breaker opens after the failure threshold and stops issuing calls`() = runTest {
        val timeSource = TestTimeSource()
        val breaker = HeimCircuitBreaker(
            failureThreshold = 3,
            openDuration = 30.seconds,
            timeSource = timeSource
        )
        var attempts = 0

        repeat(3) {
            assertFailsWith<IllegalStateException> {
                breaker.withBreaker(onOpen = { error("should not short-circuit yet") }) {
                    attempts++
                    error("boom")
                }
            }
        }
        assertEquals(3, attempts)
        assertEquals(HeimCircuitBreaker.State.OPEN, breaker.currentState())

        // While open, the block must not run at all: this is what stops a user hammering a
        // degraded backend by pull-to-refreshing.
        val fallback = breaker.withBreaker(onOpen = { "fallback" }) {
            attempts++
            "network"
        }
        assertEquals("fallback", fallback)
        assertEquals(3, attempts)
    }

    @Test
    fun `breaker half-opens after the cooldown and closes on a successful probe`() = runTest {
        val timeSource = TestTimeSource()
        val breaker = HeimCircuitBreaker(
            failureThreshold = 1,
            openDuration = 30.seconds,
            timeSource = timeSource
        )

        assertFailsWith<IllegalStateException> {
            breaker.withBreaker(onOpen = { error("unreachable") }) { error("boom") }
        }
        assertEquals(HeimCircuitBreaker.State.OPEN, breaker.currentState())

        timeSource += 31.seconds
        assertEquals(HeimCircuitBreaker.State.HALF_OPEN, breaker.currentState())

        val result = breaker.withBreaker(onOpen = { "fallback" }) { "recovered" }
        assertEquals("recovered", result)
        assertEquals(HeimCircuitBreaker.State.CLOSED, breaker.currentState())
    }

    @Test
    fun `cancellation does not trip the breaker`() = runTest {
        val timeSource = TestTimeSource()
        val breaker = HeimCircuitBreaker(
            failureThreshold = 1,
            openDuration = 30.seconds,
            timeSource = timeSource
        )

        assertFailsWith<kotlinx.coroutines.CancellationException> {
            breaker.withBreaker(onOpen = { error("unreachable") }) {
                throw kotlinx.coroutines.CancellationException("navigated away")
            }
        }

        // Navigating away from a screen is not a service failure.
        assertEquals(HeimCircuitBreaker.State.CLOSED, breaker.currentState())
        assertTrue(breaker.withBreaker(onOpen = { false }) { true })
    }
}
