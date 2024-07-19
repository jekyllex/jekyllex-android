package xyz.jekyllex.ui.activities.editor

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.jekyllex.R
import xyz.jekyllex.ui.activities.editor.components.Editor
import xyz.jekyllex.ui.activities.editor.components.Preview
import xyz.jekyllex.ui.theme.JekyllExTheme
import xyz.jekyllex.ui.components.JekyllExAppBar
import xyz.jekyllex.utils.formatDir

class EditorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val file = intent.getStringExtra("file") ?: ""

        enableEdgeToEdge()

        setContent {
            JekyllExTheme {
                EditorView(file)
            }
        }
    }
}

@Composable
fun EditorView (file: String = "") {
    val context = LocalContext.current as Activity

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            JekyllExAppBar(
                title = {
                    Column {
                        Text(
                            maxLines = 1,
                            fontSize = 20.sp,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(0.dp),
                            text = file.substringAfterLast("/"),
                        )
                        Text(
                            maxLines = 1,
                            fontSize = 14.sp,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(0.dp),
                            text = file.formatDir("/"),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { context.finish() }) {
                        Icon(
                            contentDescription = "Go back",
                            painter = painterResource(id = R.drawable.back),
                            modifier = Modifier.padding(start = 8.dp).size(20.dp)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        val tabs = listOf("Editor", "Preview")
        var tabIndex by remember { mutableIntStateOf(0) }

        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(innerPadding)) {
            TabRow(selectedTabIndex = tabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(text = { Text(title) },
                        selected = tabIndex == index,
                        onClick = { tabIndex = index }
                    )
                }
            }
            when (tabIndex) {
                0 -> Editor(file, innerPadding)
                1 -> Preview()
            }
        }
    }
}
