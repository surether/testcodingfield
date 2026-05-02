package com.surether.testcodingfield.math

import org.junit.Assert.assertEquals
import org.junit.Test

class FunctionExpressionTest {
    @Test
    fun parsesQuadraticExpression() {
        val expression = FunctionExpression.parse("y = x^2 + 2x + 1")

        assertEquals(16.0, expression.evaluate(3.0), 0.0001)
    }

    @Test
    fun parsesTrigonometricExpression() {
        val expression = FunctionExpression.parse("sin(pi / 2)")

        assertEquals(1.0, expression.evaluate(0.0), 0.0001)
    }

    @Test
    fun parsesFunctionWithoutParentheses() {
        val expression = FunctionExpression.parse("sin x")

        assertEquals(1.0, expression.evaluate(Math.PI / 2), 0.0001)
    }

    @Test
    fun keepsExponentPrecedenceAboveUnaryMinus() {
        val expression = FunctionExpression.parse("-x^2")

        assertEquals(-9.0, expression.evaluate(3.0), 0.0001)
    }

    @Test
    fun parsesAbsoluteValueExpression() {
        val expression = FunctionExpression.parse("abs(x) - 2")

        assertEquals(3.0, expression.evaluate(-5.0), 0.0001)
    }
}
