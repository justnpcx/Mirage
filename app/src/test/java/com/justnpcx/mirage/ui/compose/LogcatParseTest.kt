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

    /**
     * What the kernel actually writes: the level tag is wrapped in ANSI colour codes. These must
     * neither show up as literal `^[[36m` text nor defeat the level match.
     */
    @Test
    fun stripsTheKernelsAnsiColourCodes() {
        val line = logLineOf("\u001B[36mINFO\u001B[0m[0000] sing-box started (0.59s)")
        assertEquals(LogLevel.INFO, line.level)
        assertEquals("INFO[0000] sing-box started (0.59s)", line.text)
    }

    @Test
    fun stripsAnsiAroundWarnAndErrorToo() {
        assertEquals(LogLevel.WARN, logLineOf("\u001B[33mWARN\u001B[0m[0001] something odd").level)
        assertEquals(LogLevel.ERROR, logLineOf("\u001B[31mERROR\u001B[0m[0011] failed").level)
        assertEquals(
            "ERROR[0011] failed",
            logLineOf("\u001B[31mERROR\u001B[0m[0011] failed").text,
        )
    }

    @Test
    fun keepsTheTimestampThatPrecedesTheColouredTag() {
        // the Go std logger prefixes a UTC timestamp; only the tag carries colour
        val line = logLineOf("2026/10/13 05:59:30 \u001B[36mINFO\u001B[0m[0000] started")
        assertEquals(LogLevel.INFO, line.level)
        assertEquals("2026/10/13 05:59:30 INFO[0000] started", line.text)
    }

    @Test
    fun parseStripsAnsiOnEveryLine() {
        val lines = parseLog("\u001B[36mINFO\u001B[0m[0000] a\nplain b\n")
        assertEquals(listOf("INFO[0000] a", "plain b"), lines.map { it.text })
        assertEquals(listOf(LogLevel.INFO, LogLevel.NORMAL), lines.map { it.level })
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
