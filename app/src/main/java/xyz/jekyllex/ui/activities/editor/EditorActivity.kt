package xyz.jekyllex.ui.activities.editor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import xyz.jekyllex.ui.activities.editor.ui.theme.JekyllExTheme
import xyz.jekyllex.ui.components.JekyllExAppBar

class EditorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JekyllExTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        JekyllExAppBar(title = {
                            Text("Editor")
                        })
                    }
                ) { innerPadding ->
                    Text(
                        text = "Editor",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
