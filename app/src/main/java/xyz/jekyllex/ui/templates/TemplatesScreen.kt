/*
 * MIT License
 *
 * Copyright (c) 2026 Gourav Khunger
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

package xyz.jekyllex.ui.templates

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xyz.jekyllex.R
import xyz.jekyllex.data.StartersRepository
import xyz.jekyllex.models.Starter
import xyz.jekyllex.ui.components.JekyllExAppBar

@Composable
fun TemplatesScreen(
    isCreating: Boolean,
    onBack: () -> Unit,
    onOpenPreview: (String, String) -> Unit = { _, _ -> },
    onUseTemplate: (Starter, (Boolean, String) -> Unit) -> Unit,
) {
    val context = LocalContext.current
    var reload by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }
    var starters by remember { mutableStateOf(emptyList<Starter>()) }
    var selected by remember { mutableStateOf<Starter?>(null) }

    LaunchedEffect(reload) {
        loading = true
        error = false
        val result = withContext(Dispatchers.IO) {
            runCatching { StartersRepository.fetch() }
        }
        starters = result.getOrDefault(emptyList())
        error = result.isFailure
        loading = false
    }

    BackHandler(enabled = isCreating) { }

    selected?.let { starter ->
        StarterDetailDialog(
            starter = starter,
            isCreating = isCreating,
            onDismiss = { if (!isCreating) selected = null },
            onPreview = {
                selected = null
                onOpenPreview(starter.url, starter.name)
            },
            onUse = {
                onUseTemplate(starter) { ok, folder ->
                    if (ok) {
                        Toast.makeText(context, "Created $folder", Toast.LENGTH_SHORT).show()
                        selected = null
                        onBack()
                    } else {
                        val message = if (folder.isEmpty()) {
                            "A command is already running"
                        } else {
                            "Failed to clone template"
                        }
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }
            },
        )
    }

    Scaffold(
        topBar = {
            JekyllExAppBar(
                title = { Text("Templates") },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isCreating) {
                        Icon(
                            contentDescription = "Go back",
                            painter = painterResource(id = R.drawable.back),
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(20.dp)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            when {
                loading -> CircularProgressIndicator()
                error -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Couldn't load templates", style = MaterialTheme.typography.bodyMedium)
                    Button(
                        onClick = { reload++ },
                        modifier = Modifier.padding(top = 12.dp),
                    ) { Text("Retry") }
                }
                starters.isEmpty() -> Text("No templates")
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(starters, key = { it.git + it.name }) { starter ->
                        StarterCard(starter) { selected = starter }
                    }
                }
            }
        }
    }
}

@Composable
private fun StarterCard(starter: Starter, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column {
            StarterImage(
                url = starter.image,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
            )
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = starter.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (starter.description.isNotEmpty()) {
                    Text(
                        text = starter.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StarterDetailDialog(
    starter: Starter,
    isCreating: Boolean,
    onDismiss: () -> Unit,
    onPreview: () -> Unit,
    onUse: () -> Unit,
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                StarterImage(
                    url = starter.image,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(8.dp)),
                )
                Text(
                    text = starter.name,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 12.dp),
                )
                starter.version?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                if (starter.description.isNotEmpty()) {
                    Text(
                        text = starter.description,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (starter.url.isNotEmpty()) {
                        TextButton(
                            onClick = onPreview,
                            enabled = !isCreating,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Live preview")
                        }
                    }
                    Button(
                        onClick = onUse,
                        enabled = !isCreating,
                        modifier = Modifier.weight(1f),
                    ) {
                        if (isCreating) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text("Use this")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StarterImage(url: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var bitmap by remember(url) { mutableStateOf<Bitmap?>(null) }
    var loading by remember(url) { mutableStateOf(url.isNotEmpty()) }

    LaunchedEffect(url) {
        if (url.isEmpty()) {
            loading = false
            return@LaunchedEffect
        }
        loading = true
        bitmap = withContext(Dispatchers.IO) {
            StartersRepository.loadImage(context.cacheDir, url)
        }
        loading = false
    }

    val preview = bitmap
    when {
        preview != null -> Image(
            bitmap = preview.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
        loading -> Box(modifier = modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp),
            )
        }
        else -> Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No preview", style = MaterialTheme.typography.labelSmall)
        }
    }
}
