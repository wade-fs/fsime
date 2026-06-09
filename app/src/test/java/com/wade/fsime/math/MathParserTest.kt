package com.wade.fsime.math

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.PI

class MathParserTest {

    private fun createParser(isRadianMode: Boolean): MathParser {
        val parser = MathParser.create()
        parser.isRadianMode = isRadianMode
        return parser
    }

    @Test
    fun testReportedIssue() {
        // Issue 1: sin(30) in degree mode
        val parser = createParser(isRadianMode = false)
        assertEquals(0.5, parser.parse("sin(30)"), 1e-10)

        // Issue 2: sin(30°) should work
        assertEquals(0.5, parser.parse("sin(30°)"), 1e-10)
    }

    @Test
    fun testSinDegreeMode() {
        val parser = createParser(isRadianMode = false)
        // sin(30) should be 0.5 in degree mode
        assertEquals(0.5, parser.parse("sin(30)"), 1e-10)
        // sin(30°) should be 0.5 regardless of mode because of explicit unit
        assertEquals(0.5, parser.parse("sin(30°)"), 1e-10)
        // sin(pi/6) should be 0.5 because of pi
        assertEquals(0.5, parser.parse("sin(pi/6)"), 1e-10)
    }

    @Test
    fun testSinRadianMode() {
        val parser = createParser(isRadianMode = true)
        // sin(pi/6) should be 0.5
        assertEquals(0.5, parser.parse("sin(pi/6)"), 1e-10)
        // sin(30°) should still be 0.5 because of explicit unit
        assertEquals(0.5, parser.parse("sin(30°)"), 1e-10)
        // sin(30) in radian mode should be sin(30 radians)
        val res = parser.parse("sin(30)")
        println("sin(30) in Radian mode: $res")
        assertEquals(kotlin.math.sin(30.0), res, 1e-10)
    }

    @Test
    fun testExplicitUnits() {
        val parser = createParser(isRadianMode = true) // Mode shouldn't matter for explicit units
        // 45° should be pi/4
        val res = parser.parse("45°")
        println("45° result: $res, expected: ${PI / 4.0}")
        assertEquals(PI / 4.0, res, 1e-10)
        // 45°30' should be 45.5 degrees in radians
        assertEquals(Math.toRadians(45.5), parser.parse("45°30'"), 1e-10)
    }

    @Test
    fun testNestedExpressions() {
        val parser = createParser(isRadianMode = false)
        // sin(2 * 15) -> sin(30) -> 0.5
        assertEquals(0.5, parser.parse("sin(2 * 15)"), 1e-10)
        // sin(30 + 0°) -> sin(30) but has explicit unit in string, so treats all as radians?
        // Wait, if ANY part has °, the WHOLE expression is treated as radians.
        // So sin(30 + 0°) becomes sin(30 radians + 0 radians)
        // This is consistent with "If seen ° ' \" or pi, then it's Radian mode"
    }
}
