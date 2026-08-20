package xyz.jekyllex.ui.navigation

data object HomeDestination

data class EditorDestination(val path: String)

data object SettingsDestination

data class PageDestination(val url: String, val title: String)
