package xyz.jekyllex.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import xyz.jekyllex.services.ProcessService
import xyz.jekyllex.services.SessionManager

class ProcessRepository {
    private val _bound = MutableStateFlow<ProcessService?>(null)
    val bound: StateFlow<ProcessService?> = _bound.asStateFlow()

    val isRunning: Boolean
        get() = _bound.value?.isRunning == true

    val sessionManager: SessionManager?
        get() = _bound.value?.sessionManager

    fun attach(service: ProcessService) {
        _bound.value = service
    }

    fun detach() {
        _bound.value = null
    }

    fun exec(cmd: Array<String>, dir: String? = null, onDone: () -> Unit = {}) {
        _bound.value?.exec(cmd, dir, onDone)
    }

    fun cd(dir: String) {
        _bound.value?.cd(dir)
    }

    fun kill() {
        _bound.value?.killProcess()
    }
}
