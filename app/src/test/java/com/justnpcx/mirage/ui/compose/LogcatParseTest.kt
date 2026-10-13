package com.justnpcx.mirage.ui.compose

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The log viewer's only real logic: which line is which level, and how the raw text splits.
 *
 * Plain JUnit — these are pure functions with no Android dependency.
 */
class LogcatParseTest {

    @Test
    fun classifiesTheKernelsPrefixes() {
        assertEquals(LogLevel.INFO, logLineOf("INFO[0000] sing-box started").level)
        assertEquals(LogLevel.ERROR, logLineOf("ERROR[0011] connection: failed").level)
        assertEquals(LogLevel.WARN, logLineOf("WARN[0000] something odd").level)
    }

    @Test
    fun classifiesTheAlternativeBracketForm() {
        assertEquals(LogLevel.INFO, logLineOf("2026-10-13 10:00 [Info] hello").level)
        assertEquals(LogLevel.ERROR, logLineOf("2026-10-13 10:00 [Error] boom").level)
        assertEquals(LogLevel.WARN, logLineOf("2026-10-13 10:00 [Warning] hmm").level)
    }

    @Test
    fun plainTextIsNormal() {
        assertEquals(LogLevel.NORMAL, logLineOf("just a trace line").level)
        // a bare INFO without the bracket is not a level marker
        assertEquals(LogLevel.NORMAL, logLineOf("INFO something").level)
    }

    @Test
    fun parseSplitsLinesAndDropsTheTrailingEmptyOne() {
        val lines = parseLog("a\nb\nc\n")
        assertEquals(listOf("a", "b", "c"), lines.map { it.text })
    }

    @Test
    fun parseDropsBlankLines() {
        // a blank row carries nothing, so interior blanks go too
        val lines = parseLog("a\n\nb\n")
        assertEquals(listOf("a", "b"), lines.map { it.text })
    }

    @Test
    fun parseOfEmptyIsEmpty() {
        assertEquals(0, parseLog("").size)
        assertEquals(0, parseLog("\n").size)
    }
}
