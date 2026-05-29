package com.example.utils

import java.util.Stack
import java.util.Locale

object MathEvaluator {

    /**
     * Evaluates a mathematical expression with standard precedence (*, /, +, -)
     */
    fun evaluate(expression: String): String {
        val cleanExpr = expression.replace(" ", "").replace(",", ".")
        if (cleanExpr.isEmpty()) return ""

        try {
            var tokens = tokenize(cleanExpr)
            while (tokens.isNotEmpty() && tokens.last() in setOf("+", "-", "*", "/")) {
                tokens = tokens.dropLast(1)
            }
            if (tokens.isEmpty()) return ""
            
            val rpn = infixToRPN(tokens)
            val result = evaluateRPN(rpn)
            
            // Format output elegantly
            return if (result % 1.0 == 0.0) {
                result.toLong().toString()
            } else {
                String.format(Locale.US, "%.5f", result).trimEnd('0').trimEnd('.')
            }
        } catch (e: ArithmeticException) {
            return "Fehler: Division durch 0"
        } catch (e: Exception) {
            return ""
        }
    }

    private fun tokenize(expr: String): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        val n = expr.length

        while (i < n) {
            val c = expr[i]
            if (c.isDigit() || c == '.') {
                val sb = StringBuilder()
                while (i < n && (expr[i].isDigit() || expr[i] == '.')) {
                    sb.append(expr[i])
                    i++
                }
                tokens.add(sb.toString())
            } else if (c in setOf('+', '-', '*', '/')) {
                // If '-' is at the start or follows an operator, it is a negative sign, not an operator
                if (c == '-' && (tokens.isEmpty() || tokens.last() in setOf("+", "-", "*", "/"))) {
                    val sb = StringBuilder("-")
                    i++
                    while (i < n && (expr[i].isDigit() || expr[i] == '.')) {
                        sb.append(expr[i])
                        i++
                    }
                    if (sb.length > 1) {
                        tokens.add(sb.toString())
                    } else {
                        // Just '-' lonely. Treat as subtraction
                        tokens.add("-")
                    }
                } else {
                    tokens.add(c.toString())
                    i++
                }
            } else {
                i++ // Ignore spaces or unhandled characters
            }
        }
        return tokens
    }

    private fun infixToRPN(tokens: List<String>): List<String> {
        val output = mutableListOf<String>()
        val operators = Stack<String>()
        val precedence = mapOf("+" to 1, "-" to 1, "*" to 2, "/" to 2)

        for (token in tokens) {
            if (token.toDoubleOrNull() != null) {
                output.add(token)
            } else if (token in precedence.keys) {
                while (!operators.isEmpty() && precedence.getOrDefault(operators.peek(), 0) >= precedence.getOrDefault(token, 0)) {
                    output.add(operators.pop())
                }
                operators.push(token)
            }
        }

        while (!operators.isEmpty()) {
            output.add(operators.pop())
        }

        return output
    }

    private fun evaluateRPN(tokens: List<String>): Double {
        val stack = Stack<Double>()

        for (token in tokens) {
            val number = token.toDoubleOrNull()
            if (number != null) {
                stack.push(number)
            } else {
                if (stack.size < 2) throw Exception("Ungenügende Operanden")
                val b = stack.pop()
                val a = stack.pop()
                val res = when (token) {
                    "+" -> a + b
                    "-" -> a - b
                    "*" -> a * b
                    "/" -> {
                        if (b == 0.0) throw ArithmeticException("Division durch Null")
                        a / b
                    }
                    else -> throw Exception("Unbekannter Operator")
                }
                stack.push(res)
            }
        }

        if (stack.size != 1) throw Exception("Invalider Rechenverlauf")
        return stack.pop()
    }
}
