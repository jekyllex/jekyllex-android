/*
 * MIT License
 *
 * Copyright (c) 2024 Gourav Khunger
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

package xyz.jekyllex.utils

object Commands {
    fun cat(vararg file: String): Array<String> = arrayOf("cat", *file)
    fun rm(vararg files: String): Array<String> = arrayOf("rm", *files)
    fun rmDir(vararg dirs: String): Array<String> = arrayOf("rm", "-rf", *dirs)
    fun mv(src: String, dest: String): Array<String> = arrayOf("mv", src, dest)
    fun stat(vararg command: String): Array<String> = arrayOf("stat", *command)
    fun echo(vararg command: String): Array<String> = arrayOf("echo", *command)
    fun touch(vararg command: String): Array<String> = arrayOf("touch", *command)
    fun mkDir(vararg command: String): Array<String> = arrayOf("mkdir", *command)
    fun diskUsage(vararg command: String): Array<String> = arrayOf("du", *command)
    fun shell(vararg command: String): Array<String> = arrayOf("/bin/sh", "-c", *command)

    fun git(vararg command: String): Array<String> = arrayOf("git", *command)
    fun gem(vararg command: String): Array<String> = arrayOf("gem", *command)
    fun ruby(vararg command: String): Array<String> = arrayOf("ruby", *command)
    fun curl(vararg command: String): Array<String> = arrayOf("curl", *command)
    fun bundle(vararg command: String): Array<String> = arrayOf("bundle", *command)
    fun jekyll(vararg command: String): Array<String> = arrayOf("jekyll", *command)

    fun getFromYAML(file: String, vararg properties: String): Array<String> = ruby(
        "-e", "require 'safe_yaml';_=SafeYAML.load_file('${file}');p ${
            properties.joinToString(", ") { "_['${it}']" }
        };"
    )

    fun guessDestinationUrl(file: String) = ruby(
        "-e", "require 'jekyll';" +
                "Jekyll.logger.log_level=:error;" +
                "c=Jekyll.configuration({'config': '_config.yml'});" +
                "base=c['baseurl'] || '';" +
                "s=Jekyll::Site.new(c);" +
                "s.collections.each { |n,_| " +
                    "if '$file'.include?(\"_#{n}/\") then " +
                        "puts Jekyll::Document.new(" +
                            "'$file'," + ":site=>s," +
                            ":collection=>s.collections[n]" +
                        ").tap(&:read).url.prepend(base); exit; " +
                    "end" +
                "}; " +
                "_p=Jekyll::Page.new(" +
                    "s" + ",'.'," + "''," + "'$file'" +
                ");" +
                "puts _p.url.prepend(base);",
    )
}
