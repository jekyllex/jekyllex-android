package xyz.jekyllex.data

import android.content.Context
import xyz.jekyllex.utils.Settings

class AppContainer(context: Context) {
    val settings = Settings(context.applicationContext)
    val files = FilesRepository()
}
