package xyz.jekyllex.ui.navigation

import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import xyz.jekyllex.data.ProcessRepository
import xyz.jekyllex.ui.activities.editor.EditorView
import xyz.jekyllex.ui.activities.home.HomeScreen
import xyz.jekyllex.ui.activities.home.HomeViewModel
import xyz.jekyllex.ui.activities.settings.SettingsView
import xyz.jekyllex.ui.activities.viewer.WebPageScreen

@Composable
fun JekyllExNav(
    homeViewModel: HomeViewModel,
    process: ProcessRepository,
    pickFileLauncher: ActivityResultLauncher<String>,
    requestPermissionLauncher: ActivityResultLauncher<String>,
) {
    val backStack = remember { mutableStateListOf<Any>(HomeDestination) }
    val processService by process.bound.collectAsStateWithLifecycle()
    val pop = { if (backStack.size > 1) backStack.removeLastOrNull() }

    NavDisplay(
        backStack = backStack,
        onBack = { pop() },
        entryProvider = { key ->
            when (key) {
                is HomeDestination -> NavEntry(key) {
                    HomeScreen(
                        homeViewModel = homeViewModel,
                        pickFileLauncher = pickFileLauncher,
                        requestPermissionLauncher = requestPermissionLauncher,
                        onOpenFile = { path -> backStack.add(EditorDestination(path)) },
                        onOpenSettings = { backStack.add(SettingsDestination) },
                    )
                }

                is EditorDestination -> NavEntry(key) {
                    EditorView(
                        file = key.path,
                        processService = processService,
                        onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
                        onRenamed = { path ->
                            if (backStack.size > 1) backStack.removeLastOrNull()
                            backStack.add(EditorDestination(path))
                        },
                    )
                }

                is SettingsDestination -> NavEntry(key) {
                    SettingsView(
                        onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
                        onOpenPage = { url, title ->
                            backStack.add(PageDestination(url, title))
                        },
                    )
                }

                is PageDestination -> NavEntry(key) {
                    WebPageScreen(
                        initialUrl = key.url,
                        initialTitle = key.title,
                        onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
                    )
                }

                else -> NavEntry(Unit) { }
            }
        }
    )
}
