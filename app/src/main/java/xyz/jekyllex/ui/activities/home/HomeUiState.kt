package xyz.jekyllex.ui.activities.home

import android.net.Uri
import xyz.jekyllex.models.File

data class HomeUiState(
    val cwd: String = "",
    val files: List<File> = emptyList(),
    val filesCount: Int = 0,
    val query: String = "",
    val isCreating: Boolean = false,
    val pendingCopyUri: Uri? = null,
    val showCopyConfirm: Boolean = false,
    val showNotifRationale: Boolean = false,
)
