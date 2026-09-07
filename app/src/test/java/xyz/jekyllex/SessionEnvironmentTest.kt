/*
 * MIT License
 *
 * Copyright (c) 2026 Gourav Khunger
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package xyz.jekyllex

import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.jekyllex.models.Session
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.io.path.createTempDirectory

class SessionEnvironmentTest {
    @Test
    fun execAppliesInjectedJekyllEnv() {
        val dir = createTempDirectory().toFile()
        dir.deleteOnExit()
        val done = CountDownLatch(1)
        val session = Session(
            0,
            { cwd -> arrayOf("PWD=$cwd", "JEKYLL_ENV=production") },
            dir.path
        )
        try {
            session.exec(arrayOf("/bin/sh", "-c", "printf '%s\\n' \"\$JEKYLL_ENV\"")) {
                done.countDown()
            }
            assertTrue(done.await(5, TimeUnit.SECONDS))
            assertTrue(session.logs.value.any { it == "production" })
        } finally {
            session.close()
        }
    }
}
