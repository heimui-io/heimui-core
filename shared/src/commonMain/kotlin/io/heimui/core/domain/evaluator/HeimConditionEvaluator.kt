package io.heimui.core.domain.evaluator

object HeimConditionEvaluator {

    /**
     * Evaluates a visibleIf expression against the current state map.
     * Supports:
     * - Direct boolean/key check: "is_accepted" (evaluates truthy/falsy)
     * - Equality: "state.country == 'US'" or "country == 'US'"
     * - Inequality: "state.plan != 'FREE'"
     * - Numeric comparisons: "age >= 18", "score < 50"
     */
    fun evaluate(expression: String?, state: Map<String, String>): Boolean {
        if (expression.isNullOrBlank()) return true

        val trimmed = expression.trim()

        // Handle negation prefix: "!state.is_hidden"
        if (trimmed.startsWith("!")) {
            val inner = trimmed.removePrefix("!").trim()
            return !evaluate(inner, state)
        }

        // Comparison operators
        val operators = listOf("==", "!=", ">=", "<=", ">", "<")
        for (op in operators) {
            if (trimmed.contains(op)) {
                val parts = trimmed.split(op, limit = 2)
                if (parts.size == 2) {
                    val leftRaw = resolveValue(parts[0].trim(), state)
                    val rightRaw = resolveValue(parts[1].trim(), state)

                    return when (op) {
                        "==" -> leftRaw.equals(rightRaw, ignoreCase = true)
                        "!=" -> !leftRaw.equals(rightRaw, ignoreCase = true)
                        ">=" -> compareNumbers(leftRaw, rightRaw) { a, b -> a >= b }
                        "<=" -> compareNumbers(leftRaw, rightRaw) { a, b -> a <= b }
                        ">" -> compareNumbers(leftRaw, rightRaw) { a, b -> a > b }
                        "<" -> compareNumbers(leftRaw, rightRaw) { a, b -> a < b }
                        else -> true
                    }
                }
            }
        }

        // Truthy check for single key
        val value = resolveValue(trimmed, state)
        return isTruthy(value)
    }

    private fun resolveValue(token: String, state: Map<String, String>): String {
        var cleanToken = token.trim()
        
        // Remove quotes if string literal
        if ((cleanToken.startsWith("'") && cleanToken.endsWith("'")) ||
            (cleanToken.startsWith("\"") && cleanToken.endsWith("\""))) {
            return cleanToken.substring(1, cleanToken.length - 1)
        }

        // Handle {{state.key}} or state.key or key
        if (cleanToken.startsWith("{{") && cleanToken.endsWith("}}")) {
            cleanToken = cleanToken.removePrefix("{{").removeSuffix("}}").trim()
        }
        if (cleanToken.startsWith("state.")) {
            cleanToken = cleanToken.removePrefix("state.")
        }

        return state[cleanToken] ?: cleanToken
    }

    private fun compareNumbers(left: String, right: String, comparison: (Double, Double) -> Boolean): Boolean {
        val numLeft = left.toDoubleOrNull() ?: return false
        val numRight = right.toDoubleOrNull() ?: return false
        return comparison(numLeft, numRight)
    }

    private fun isTruthy(value: String): Boolean {
        if (value.isBlank()) return false
        return when (value.lowercase()) {
            "true", "1", "yes" -> true
            "false", "0", "no", "null", "undefined" -> false
            else -> true
        }
    }
}
