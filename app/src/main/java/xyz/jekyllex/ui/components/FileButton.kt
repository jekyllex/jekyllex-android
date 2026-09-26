/*
 * MIT License
 *
 * Copyright (c) 2024 Gourav Khunger
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package xyz.jekyllex.ui.components

import android.content.Intent
import android.net.Uri
import android.webkit.URLUtil
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import java.io.File as JFile
import xyz.jekyllex.R
import xyz.jekyllex.models.File
import xyz.jekyllex.utils.buildStatsString
import xyz.jekyllex.utils.matchRanges

@Composable
fun FileButton(
    file: File,
    modifier: Modifier = Modifier,
    query: String = "",
    refresh: () -> Unit = {},
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val openDeleteDialog = remember { mutableStateOf(false) }
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongClick by rememberUpdatedState(onLongClick)

    if (openDeleteDialog.value) {
        val jFile = JFile(file.path)

        GenericDialog(
            dialogTitle = "Delete",
            dialogText = "Are you sure you want to delete " +
                    "this ${if (jFile.isDirectory) "folder" else "file"}?",
            onDismissRequest = { openDeleteDialog.value = false },
            onConfirmation = {
                openDeleteDialog.value = false
                if (jFile.isDirectory) jFile.deleteRecursively()
                else jFile.delete()
                refresh()
            },
        )
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = currentOnClick,
                onLongClick = currentOnLongClick,
            ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 8.dp)
                ) {
                    HighlightedText(
                        text = file.title ?: file.name.substringAfterLast('/'),
                        query = query,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Start,
                        modifier = Modifier
                            .weight(1f)
                            .align(Alignment.CenterVertically)
                    )
                    Row(Modifier.align(Alignment.CenterVertically)) {
                        if (URLUtil.isValidUrl(file.url ?: ""))
                            IconButton(
                                modifier = Modifier.size(24.dp).padding(end = 4.dp),
                                onClick = {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(file.url))
                                    )
                                }
                            ) {
                                Icon(
                                    painterResource(id = R.drawable.open_url),
                                    "Open in browser",
                                )
                            }
                        IconButton(
                            modifier = Modifier.size(24.dp),
                            onClick = { openDeleteDialog.value = true }
                        ) {
                            Icon(
                                Icons.Outlined.Delete,
                                "Delete",
                            )
                        }
                    }
                }
                AnimatedContent(
                    label = "Description animation",
                    targetState = file.description,
                    transitionSpec = {
                        fadeIn() + slideInVertically(animationSpec = tween(400)) togetherWith
                                fadeOut(animationSpec = tween(200))
                    }
                ) { description ->
                    if (description == null) return@AnimatedContent
                    HighlightedText(
                        text = description,
                        query = query,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 6.dp),
                    )
                }
                if (file.title != null || file.name.contains('/'))
                    HighlightedText(
                        text = "./${file.name}",
                        query = query,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                AnimatedContent(
                    label = "Stats animation",
                    targetState = buildStatsString(file.isDir, file.size, file.lastModified),
                    transitionSpec = {
                        fadeIn() + slideInVertically(animationSpec = tween(400)) togetherWith
                                fadeOut(animationSpec = tween(200))
                    }
                ) { description ->
                    if (description == null) return@AnimatedContent
                    HighlightedText(
                        text = description,
                        query = query,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
        }
    }
}

@Composable
private fun HighlightedText(
    text: String,
    query: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    textAlign: TextAlign? = null,
) {
    val mark = SpanStyle(
        fontWeight = FontWeight.SemiBold,
        background = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
    )
    Text(
        text = highlight(text, query, mark),
        style = style,
        modifier = modifier,
        maxLines = maxLines,
        overflow = overflow,
        textAlign = textAlign,
    )
}

private fun highlight(text: String, query: String, mark: SpanStyle): AnnotatedString {
    val ranges = matchRanges(text, query)
    if (ranges.isEmpty()) return AnnotatedString(text)
    return buildAnnotatedString {
        var cursor = 0
        for (range in ranges) {
            val start = range.first
            val end = range.last + 1
            if (start > cursor) append(text.substring(cursor, start))
            withStyle(mark) { append(text.substring(start, end)) }
            cursor = end
        }
        if (cursor < text.length) append(text.substring(cursor))
    }
}
