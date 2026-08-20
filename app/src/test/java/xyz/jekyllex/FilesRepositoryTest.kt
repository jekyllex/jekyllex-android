package xyz.jekyllex

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.jekyllex.data.FilesRepository
import java.io.File

class FilesRepositoryTest {
    @Test
    fun listSkipsGitAndSortsByName() {
        val dir = createTempDir()
        File(dir, "b.txt").writeText("x")
        File(dir, "a.txt").writeText("x")
        File(dir, ".git").mkdir()

        val names = FilesRepository().list(dir.absolutePath).map { it.name }

        assertEquals(listOf("a.txt", "b.txt"), names)
        assertTrue(names.none { it == ".git" })
    }
}
