package xyz.jekyllex

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.jekyllex.utils.Commands
import xyz.jekyllex.utils.Constants.HOME_DIR
import xyz.jekyllex.utils.formatDir
import xyz.jekyllex.utils.getProjectDir
import xyz.jekyllex.utils.isDenied
import xyz.jekyllex.utils.toCommand

class CommandAndPathTest {
    @Test
    fun toCommandSplitsOnSpacesAndKeepsQuotedArgs() {
        assertArrayEquals(
            arrayOf("git", "clone", "https://example.com/repo.git"),
            "git clone https://example.com/repo.git".toCommand()
        )
        assertArrayEquals(
            arrayOf("echo", "hello world"),
            "echo \"hello world\"".toCommand()
        )
        assertArrayEquals(
            arrayOf("echo", "hello world"),
            "echo 'hello world'".toCommand()
        )
    }

    @Test
    fun denyListBlocksSudoAndLn() {
        assertTrue(arrayOf("sudo").isDenied())
        assertTrue(arrayOf("ln", "-s", "a", "b").isDenied())
        assertFalse(arrayOf("git", "status").isDenied())
    }

    @Test
    fun formatDirReplacesHomeWithTilde() {
        assertEquals("~", HOME_DIR.formatDir("/"))
        assertEquals("~/blog", "$HOME_DIR/blog".formatDir("/"))
    }

    @Test
    fun getProjectDirReturnsFirstSegmentUnderHome() {
        assertEquals("$HOME_DIR/blog", "$HOME_DIR/blog/_posts/hi.md".getProjectDir())
        assertEquals(null, HOME_DIR.getProjectDir())
    }

    @Test
    fun yamlAndGuessCommandsPassPathViaArgv() {
        val evil = "'; system('id'); '"
        val yaml = Commands.getFromYAML(evil, "title")
        assertTrue(yaml.contains("--"))
        assertTrue(yaml.contains(evil))
        assertFalse(yaml[2].contains(evil))

        val guess = Commands.guessDestinationUrl(evil)
        assertTrue(guess.contains("--"))
        assertTrue(guess.contains(evil))
        assertFalse(guess[2].contains("'$evil'"))
    }
}
