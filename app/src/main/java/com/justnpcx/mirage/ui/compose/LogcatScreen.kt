package com.justnpcx.mirage.ui.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** How a log line is coloured. Everything the kernel writes is one of these four shapes. */
enum class LogLevel { NORMAL, INFO, WARN, ERROR }

data class LogLine(val text: String, val level: LogLevel)

data class LogcatState(val lines: List<LogLine> = emptyList())

/**
 * The kernel log, one line per row.
 *
 * A LazyColumn rather than one big Text: the log can be tens of thousands of lines and re-laying
 * out the whole thing on every refresh is what made the old TextView janky.
 */
@Composable
fun LogcatScreen(
    state: LogcatState,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // New output arrives at the bottom; follow it unless the user has scrolled away.
    LaunchedEffect(state.lines.size) {
        if (state.lines.isNotEmpty() && !listState.isScrollInProgress) {
            listState.scrollToItem(state.lines.lastIndex)
        }
    }

    SelectionContainer(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = 12.dp,
                vertical = 8.dp,
            ),
        ) {
            itemsIndexed(state.lines) { _, line ->
                Text(
                    text = line.text,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = line.level.color(),
                )
            }
        }
    }
}

@Composable
private fun LogLevel.color(): Color = when (this) {
    LogLevel.INFO -> LogInfoGreen
    LogLevel.WARN, LogLevel.ERROR -> MaterialTheme.colorScheme.error
    LogLevel.NORMAL -> MaterialTheme.colorScheme.onSurfaceVariant
}

/** Same green the old TextView used, kept so the log still reads the same. */
private val LogInfoGreen = Color(0xFF86C166)

/**
 * The kernel colours its level tag before we ever see the line:
 *
 *     ESC[36mINFO ESC[0m[0000] sing-box started (0.59s)
 *
 * Left in, those escapes are rendered literally as `^[[36m` noise, and worse, they break the
 * level match below -- `INFO` is followed by the escape, not by `[`. Drop them.
 */
private val AnsiEscape = Regex("\u001B\\[[0-9;]*[A-Za-z]")

/**
 * Classify one line. Mirrors the kernel's own prefixes; anything else is body text.
 */
fun logLineOf(text: String): LogLine {
    val clean = text.replace(AnsiEscape, "")
    return LogLine(
        text = clean,
        level = when {
            clean.contains("INFO[") || clean.contains(" [Info]") -> LogLevel.INFO
            clean.contains("ERROR[") || clean.contains(" [Error]") -> LogLevel.ERROR
            clean.contains("WARN[") || clean.contains(" [Warning]") -> LogLevel.WARN
            else -> LogLevel.NORMAL
        },
    )
}

/** Split a raw kernel log into rows. Empty lines are dropped -- a blank row carries nothing. */
fun parseLog(raw: String): List<LogLine> =
    raw.split('\n').filter { it.isNotEmpty() }.map(::logLineOf)
