package com.ollamaandroid.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

private sealed interface MarkdownBlock {
    data class Paragraph(val text: String) : MarkdownBlock
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock
}

private fun parseBlocks(content: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    var remaining = content
    while (true) {
        val fenceStart = remaining.indexOf("```")
        if (fenceStart < 0) {
            if (remaining.isNotBlank()) blocks += MarkdownBlock.Paragraph(remaining.trim('\n'))
            break
        }
        val before = remaining.substring(0, fenceStart)
        if (before.isNotBlank()) blocks += MarkdownBlock.Paragraph(before.trim('\n'))

        val afterFence = remaining.substring(fenceStart + 3)
        val newlineIdx = afterFence.indexOf('\n')
        val language = if (newlineIdx >= 0) afterFence.substring(0, newlineIdx).trim() else ""
        val codeStart = if (newlineIdx >= 0) newlineIdx + 1 else afterFence.length
        val rest = afterFence.substring(codeStart)
        val fenceEnd = rest.indexOf("```")
        if (fenceEnd < 0) {
            // Unterminated fence (common mid-stream): render what we have as code.
            blocks += MarkdownBlock.CodeBlock(language, rest.trimEnd('\n'))
            break
        }
        blocks += MarkdownBlock.CodeBlock(language, rest.substring(0, fenceEnd).trimEnd('\n'))
        remaining = rest.substring(fenceEnd + 3)
    }
    return blocks
}

/**
 * Renders inline markdown: **bold**, *italic*, and `code` spans.
 * Deliberately minimal — enough for typical chat replies without a
 * full markdown engine dependency.
 */
private fun buildInlineAnnotated(text: String, codeBackground: Color): AnnotatedString =
    buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end > i + 1) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    } else {
                        append(text[i]); i++
                    }
                }
                text[i] == '`' -> {
                    val end = text.indexOf('`', i + 1)
                    if (end > i) {
                        withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground)) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(text[i]); i++
                    }
                }
                text[i] == '*' && i + 1 < text.length && !text[i + 1].isWhitespace() -> {
                    val end = text.indexOf('*', i + 1)
                    if (end > i) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(text[i]); i++
                    }
                }
                else -> {
                    append(text[i]); i++
                }
            }
        }
    }

@Composable
fun MarkdownText(
    content: String,
    modifier: Modifier = Modifier,
) {
    val blocks = remember(content) { parseBlocks(content) }
    val codeBackground = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Paragraph -> {
                    val annotated = remember(block.text, codeBackground) {
                        buildInlineAnnotated(block.text, codeBackground)
                    }
                    Text(
                        text = annotated,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                is MarkdownBlock.CodeBlock -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            if (block.language.isNotBlank()) {
                                Text(
                                    text = block.language,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 4.dp),
                                )
                            }
                            Text(
                                text = block.code,
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                            )
                        }
                    }
                }
            }
        }
    }
}
