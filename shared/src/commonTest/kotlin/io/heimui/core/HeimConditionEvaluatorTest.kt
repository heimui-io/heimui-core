package io.heimui.core

import io.heimui.core.domain.evaluator.HeimConditionEvaluator
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HeimConditionEvaluatorTest {

    @Test
    fun testNullOrEmptyAlwaysVisible() {
        assertTrue(HeimConditionEvaluator.evaluate(null, emptyMap()))
        assertTrue(HeimConditionEvaluator.evaluate("", emptyMap()))
        assertTrue(HeimConditionEvaluator.evaluate("   ", emptyMap()))
    }

    @Test
    fun testBooleanTruthy() {
        val state = mapOf("is_logged_in" to "true", "is_guest" to "false")
        assertTrue(HeimConditionEvaluator.evaluate("is_logged_in", state))
        assertFalse(HeimConditionEvaluator.evaluate("is_guest", state))
        assertFalse(HeimConditionEvaluator.evaluate("!is_logged_in", state))
        assertTrue(HeimConditionEvaluator.evaluate("!is_guest", state))
    }

    @Test
    fun testEqualityComparison() {
        val state = mapOf("plan" to "PREMIUM", "country" to "CO")
        assertTrue(HeimConditionEvaluator.evaluate("state.plan == 'PREMIUM'", state))
        assertFalse(HeimConditionEvaluator.evaluate("state.plan == 'FREE'", state))
        assertTrue(HeimConditionEvaluator.evaluate("country != 'US'", state))
    }

    @Test
    fun testNumericComparison() {
        val state = mapOf("age" to "25", "score" to "85.5")
        assertTrue(HeimConditionEvaluator.evaluate("age >= 18", state))
        assertFalse(HeimConditionEvaluator.evaluate("age < 18", state))
        assertTrue(HeimConditionEvaluator.evaluate("score > 80", state))
    }
}
