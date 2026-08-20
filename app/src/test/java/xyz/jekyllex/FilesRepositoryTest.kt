package xyz.jekyllex

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.jekyllex.data.FilesRepository
import java.io.File
import kotlin.io.path.createTempDirectory

class FilesRepositoryTest {
    @Test
    fun listSkipsGitAndSortsByName() {
        val dir = createTempDirectory().toFile()
        dir.deleteOnExit()
        File(dir, "b.txt").writeText("x")
        File(dir, "a.txt").writeText("x")
        File(dir, ".git").mkdir()

        val names = FilesRepository().list(dir.absolutePath).map { it.name }

        assertEquals(listOf("a.txt", "b.txt"), names)
        assertTrue(names.none { it == ".git" })
    }
}
