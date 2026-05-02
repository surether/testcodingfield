package com.surether.testcodingfield.math

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

class ExpressionParseException(message: String) : IllegalArgumentException(message)

class FunctionExpression private constructor(
    private val root: Node,
    val normalizedSource: String,
) {
    fun evaluate(x: Double): Double = root.evaluate(x)

    companion object {
        fun parse(source: String): FunctionExpression {
            val normalized = normalize(source)
            if (normalized.isBlank()) throw ExpressionParseException("Expression is blank")
            val parser = Parser(normalized)
            val root = parser.parseExpression()
            parser.expectEnd()
            return FunctionExpression(root = root, normalizedSource = normalized)
        }

        private fun normalize(source: String): String {
            val cleaned = source
                .trim()
                .lowercase()
                .replace(" ", "")
                .replace("×", "*")
                .replace("÷", "/")
                .replace("−", "-")
                .replace("π", "pi")

            val expressionOnly = when {
                cleaned.startsWith("y=") -> cleaned.substringAfter("=")
                cleaned.startsWith("f(x)=") -> cleaned.substringAfter("=")
                else -> cleaned
            }

            return expressionOnly
        }
    }
}

private sealed interface Node {
    fun evaluate(x: Double): Double
}

private data class ConstantNode(val value: Double) : Node {
    override fun evaluate(x: Double): Double = value
}

private data object VariableNode : Node {
    override fun evaluate(x: Double): Double = x
}

private data class UnaryNode(
    val operator: Char,
    val child: Node,
) : Node {
    override fun evaluate(x: Double): Double =
        when (operator) {
            '-' -> -child.evaluate(x)
            else -> child.evaluate(x)
        }
}

private data class BinaryNode(
    val operator: Char,
    val left: Node,
    val right: Node,
) : Node {
    override fun evaluate(x: Double): Double {
        val a = left.evaluate(x)
        val b = right.evaluate(x)
        return when (operator) {
            '+' -> a + b
            '-' -> a - b
            '*' -> a * b
            '/' -> a / b
            '^' -> a.pow(b)
            else -> error("Unknown operator $operator")
        }
    }
}

private data class FunctionNode(
    val name: String,
    val argument: Node,
) : Node {
    override fun evaluate(x: Double): Double {
        val value = argument.evaluate(x)
        return when (name) {
            "sin" -> sin(value)
            "cos" -> cos(value)
            "tan" -> tan(value)
            "sqrt" -> sqrt(value)
            "abs" -> abs(value)
            "exp" -> exp(value)
            "ln" -> ln(value)
            "log" -> log10(value)
            else -> throw ExpressionParseException("Unknown function: $name")
        }
    }
}

private class Parser(private val source: String) {
    private var index = 0

    fun parseExpression(): Node = parseAddition()

    fun expectEnd() {
        if (index != source.length) {
            throw ExpressionParseException("Unexpected token '${source[index]}' at $index")
        }
    }

    private fun parseAddition(): Node {
        var node = parseMultiplication()
        while (true) {
            node = when {
                match('+') -> BinaryNode('+', node, parseMultiplication())
                match('-') -> BinaryNode('-', node, parseMultiplication())
                else -> return node
            }
        }
    }

    private fun parseMultiplication(): Node {
        var node = parseUnary()
        while (true) {
            node = when {
                match('*') -> BinaryNode('*', node, parseUnary())
                match('/') -> BinaryNode('/', node, parseUnary())
                startsFactor() -> BinaryNode('*', node, parseUnary())
                else -> return node
            }
        }
    }

    private fun parsePower(): Node {
        val base = parsePrimary()
        return if (match('^')) {
            BinaryNode('^', base, parseUnary())
        } else {
            base
        }
    }

    private fun parseUnary(): Node =
        when {
            match('+') -> parseUnary()
            match('-') -> UnaryNode('-', parseUnary())
            else -> parsePower()
        }

    private fun parsePrimary(): Node {
        if (match('(')) {
            val node = parseExpression()
            expect(')')
            return node
        }

        peek()?.let { char ->
            if (char.isDigit() || char == '.') return parseNumber()
            if (char.isLetter()) return parseIdentifier()
        }

        throw ExpressionParseException("Expected value at $index")
    }

    private fun parseNumber(): Node {
        val start = index
        while (peek()?.let { it.isDigit() || it == '.' } == true) index += 1
        if (peek() == 'e' && source.getOrNull(index + 1)?.let { it == '+' || it == '-' || it.isDigit() } == true) {
            index += 1
            if (peek() == '+' || peek() == '-') index += 1
            while (peek()?.isDigit() == true) index += 1
        }
        val value = source.substring(start, index).toDoubleOrNull()
            ?: throw ExpressionParseException("Invalid number at $start")
        return ConstantNode(value)
    }

    private fun parseIdentifier(): Node {
        val functionName = FUNCTION_NAMES.firstOrNull { source.startsWith(it, index) }
        if (functionName != null) {
            index += functionName.length
            return FunctionNode(functionName, parseFunctionArgument())
        }

        if (source.startsWith("pi", index)) {
            index += 2
            return ConstantNode(Math.PI)
        }

        if (source[index] == 'x') {
            index += 1
            return VariableNode
        }

        if (source[index] == 'e') {
            index += 1
            return ConstantNode(Math.E)
        }

        val start = index
        while (peek()?.isLetter() == true) index += 1
        val name = source.substring(start, index)
        throw ExpressionParseException("Unknown identifier: $name")
    }

    private fun parseFunctionArgument(): Node =
        if (match('(')) {
            val argument = parseExpression()
            expect(')')
            argument
        } else {
            parseUnary()
        }

    private fun startsFactor(): Boolean {
        val next = peek() ?: return false
        return next == '(' || next == '.' || next.isDigit() || next.isLetter()
    }

    private fun match(expected: Char): Boolean {
        if (peek() != expected) return false
        index += 1
        return true
    }

    private fun expect(expected: Char) {
        if (!match(expected)) {
            throw ExpressionParseException("Expected '$expected' at $index")
        }
    }

    private fun peek(): Char? = source.getOrNull(index)

    private companion object {
        val FUNCTION_NAMES = listOf("sqrt", "sin", "cos", "tan", "abs", "exp", "log", "ln")
    }
}
