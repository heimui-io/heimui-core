package io.heimui.core.domain.evaluator

public object HeimConditionEvaluator {

    /**
     * Extracts all variable keys referenced in a visibleIf expression.
     */
    public fun referencedKeys(expression: String?): Set<String> {
        if (expression.isNullOrBlank()) return emptySet()
        val keys = mutableSetOf<String>()
        val tokens = expression.split("==", "!=", ">=", "<=", ">", "<", "!", "&&", "||", " ")
        for (raw in tokens) {
            val token = raw.trim()
            if (token.isEmpty() || token.startsWith("'") || token.startsWith("\"")) continue
            if (token.toDoubleOrNull() != null || token.equals("true", true) || token.equals("false", true)) continue
            var cleanKey = token
            if (cleanKey.startsWith("{{") && cleanKey.endsWith("}}")) {
                cleanKey = cleanKey.removePrefix("{{").removeSuffix("}}").trim()
            }
            if (cleanKey.startsWith("state.")) {
                cleanKey = cleanKey.removePrefix("state.")
            }
            if (cleanKey.isNotEmpty()) {
                keys.add(cleanKey)
            }
        }
        return keys
    }

    /**
     * Evaluates a visibleIf expression against the current state map.
     * Supports:
     * - Direct boolean/key check: "is_accepted" (evaluates truthy/falsy)
     * - Equality: "state.country == 'US'" or "country == 'US'"
     * - Inequality: "state.plan != 'FREE'"
     * - Numeric comparisons: "age >= 18", "score < 50"
     */
    public fun evaluate(expression: String?, state: Map<String, String>): Boolean {
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
                        else -> false
                    }
                }
            }
        }

        // Direct key evaluation (Fail-Closed)
        val cleanToken = cleanKey(trimmed)
        if (state.containsKey(cleanToken)) {
            val value = state[cleanToken].orEmpty()
            return isTruthy(value)
        }

        // Literal boolean check
        if (trimmed.equals("true", ignoreCase = true)) return true
        if (trimmed.equals("false", ignoreCase = true)) return false

        // Missing key defaults to false (fail-closed)
        return false
    }

    private fun cleanKey(token: String): String {
        var cleanToken = token.trim()
        if (cleanToken.startsWith("{{") && cleanToken.endsWith("}}")) {
            cleanToken = cleanToken.removePrefix("{{").removeSuffix("}}").trim()
        }
        if (cleanToken.startsWith("state.")) {
            cleanToken = cleanToken.removePrefix("state.")
        }
        return cleanToken
    }

    private fun resolveValue(token: String, state: Map<String, String>): String {
        val trimmed = token.trim()
        
        // Literal string
        if ((trimmed.startsWith("'") && trimmed.endsWith("'")) ||
            (trimmed.startsWith("\"") && trimmed.endsWith("\""))) {
            return trimmed.substring(1, trimmed.length - 1)
        }

        val key = cleanKey(trimmed)
        return state[key] ?: if (trimmed.toDoubleOrNull() != null || trimmed.equals("true", true) || trimmed.equals("false", true)) trimmed else ""
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
            "false", "0", "no", "null", "undefined", "" -> false
            else -> true
        }
    }
}
