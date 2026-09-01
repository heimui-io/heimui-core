package io.heimui.core.data.resilience

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Circuit breaker guarding the remote data source.
 *
 * Without this, every retry and every pull-to-refresh issues a full network request, so a user
 * reacting to an outage by refreshing repeatedly hammers a backend that is already degraded --
 * the exact opposite of what the emergency-bundle fallback is for.
 *
 * Uses [TimeSource.Monotonic] rather than wall-clock time so the breaker cannot be skewed by
 * device clock changes.
 */
public class HeimCircuitBreaker(
    private val failureThreshold: Int = 3,
    private val openDuration: Duration = 30.seconds,
    private val timeSource: TimeSource = TimeSource.Monotonic
) {

    public enum class State { CLOSED, OPEN, HALF_OPEN }

    private val mutex = Mutex()
    private var consecutiveFailures = 0
    private var openedAt: TimeMark? = null

    /** Current breaker state, for telemetry and tests. */
    public suspend fun currentState(): State = mutex.withLock { stateLocked() }

    private fun stateLocked(): State {
        val opened = openedAt ?: return State.CLOSED
        return if (opened.elapsedNow() >= openDuration) State.HALF_OPEN else State.OPEN
    }

    /**
     * Runs [block] unless the circuit is open.
     *
     * While open, [onOpen] is returned immediately without touching the network. After
     * [openDuration] elapses the breaker moves to HALF_OPEN and lets a single probe through:
     * success closes it, failure re-opens it for another full interval.
     */
    public suspend fun <T> withBreaker(onOpen: () -> T, block: suspend () -> T): T {
        val allowed = mutex.withLock {
            when (stateLocked()) {
                State.OPEN -> false
                State.HALF_OPEN -> {
                    // Let exactly one probe through; keep the timer armed until it reports back.
                    openedAt = timeSource.markNow()
                    true
                }
                State.CLOSED -> true
            }
        }
        if (!allowed) return onOpen()

        return try {
            val result = block()
            mutex.withLock {
                consecutiveFailures = 0
                openedAt = null
            }
            result
        } catch (e: CancellationException) {
            // Cancellation is not a service failure and must never trip the breaker.
            throw e
        } catch (e: Throwable) {
            mutex.withLock {
                consecutiveFailures++
                if (consecutiveFailures >= failureThreshold) {
                    openedAt = timeSource.markNow()
                }
            }
            throw e
        }
    }

    /** Records a non-throwing failure (for example an HTTP 5xx mapped to an error result). */
    public suspend fun recordFailure() {
        mutex.withLock {
            consecutiveFailures++
            if (consecutiveFailures >= failureThreshold) {
                openedAt = timeSource.markNow()
            }
        }
    }

    /** Records a logical success, closing the breaker. */
    public suspend fun recordSuccess() {
        mutex.withLock {
            consecutiveFailures = 0
            openedAt = null
        }
    }

    public suspend fun reset(): Unit = recordSuccess()
}
