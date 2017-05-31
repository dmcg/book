package com.oneeyedmen.book.Chapter_03_Parser

import com.oneeyedmen.book.Chapter_01_Spike.ContextD1
import com.oneeyedmen.book.Chapter_02_Entrenchment.ContextA2
import com.oneeyedmen.book.Chapter_03_Parser.ContextB4.Block
import com.oneeyedmen.book.Chapter_03_Parser.ContextB4.CodeBlock
import com.oneeyedmen.book.Chapter_03_Parser.ContextB4.TextBlock
import org.junit.Test
import org.parboiled.Parboiled
import org.parboiled.parserunners.ReportingParseRunner
import java.io.File
import kotlin.test.assertEquals

/*-
Now that we have a parser that converts our source into a list of blocks, let's see how close we can get to having our original parser tests work against the new code.

Let's remind ourselves what our test looks like. Previously this was our only test, now we have a separate parser it looks like a low-level integration test.
-*/

object ContextC1 {
    //`
    class CodeExtractorTests {

        @Test fun writes_a_markdown_file_from_Kotlin_file() {
            checkApprovedTranslation("""
                |package should.not.be.shown
                |/*-
                |Title
                |=====
                |
                |This is Markdown paragraph
                |-*/
                |
                |object HiddenContext {
                |  //`
                |  /* This is a code comment
                |  */
                |  fun aFunction() {
                |     return 42
                |  }
                |  //`
                |}
                |
                |/*-
                |More book text.
                |-*/
                """,
                expected = """
                |Title
                |=====
                |
                |This is Markdown paragraph
                |
                |```kotlin
                |  /* This is a code comment
                |  */
                |  fun aFunction() {
                |     return 42
                |  }
                |```
                |
                |More book text.
                |""")
        }

        private fun checkApprovedTranslation(source: String, expected: String) {
            assertEquals(expected.trimMargin(), translate(source.trimMargin()).joinToString(""))
        }

        fun translate(source: String): List<String> {
            TODO()
        }
    }
    //`
}

/*-
In order to get this to pass, I'm going to parse the source into blocks, and then render each one.
-*/

object ContextC2 {
    class CodeExtractorTests {

        @Test fun writes_a_markdown_file_from_Kotlin_file() {
            checkApprovedTranslation("""
                |package should.not.be.shown
                |/*-
                |Title
                |=====
                |
                |This is Markdown paragraph
                |-*/
                |
                |object HiddenContext {
                |  //`
                |  /* This is a code comment
                |  */
                |  fun aFunction() {
                |     return 42
                |  }
                |  //`
                |}
                |
                |/*-
                |More book text.
                |-*/
                """,
                expected = """
                |Title
                |=====
                |
                |This is Markdown paragraph
                |
                |```kotlin
                |  /* This is a code comment
                |  */
                |  fun aFunction() {
                |     return 42
                |  }
                |```
                |
                |More book text.
                |""")
        }

        private fun checkApprovedTranslation(source: String, expected: String) {
            assertEquals(expected.trimMargin(), translate(source.trimMargin()).joinToString(""))
        }
    }

    //`
    fun translate(source: String): List<String> {
        val parser = Parboiled.createParser(ContextB4.BookParser::class.java)
        val blocks: List<Block> = ReportingParseRunner<Block>(parser.Root()).run(source).valueStack.toList().asReversed()
        return blocks.flatMap { render(it) }
    }

    fun render(block: Block): List<String>  = when (block) {
        is TextBlock -> block.lines
        is CodeBlock -> listOf("\n```kotlin\n") + block.lines + "```\n\n"
        else -> error("Unexpected block type")
    }
    //`
}

/*-
The `render` function took a bit of tweaking with newlines in code blocks, but it works. And I don't mind the newlines, as they allow us to express the realities of the requirements of the output Markdown. Obviously the `render` function should be a moved to a method on `Block`, but I can't resist the temptation to try the new code on the whole of the book so far. We just have to plug the latest `translate` into the old file-reading code and
-*/

object ContextC3 {

    @JvmStatic
    fun main(args: Array<String>) {
        val srcDir = File(args[0])
        val outFile = File(args[1]).apply {
            absoluteFile.parentFile.mkdirs()
        }

        val translatedLines: Sequence<String> = sourceFilesIn(srcDir)
            .flatMap { translate(it).plus("\n") }

        outFile.bufferedWriter(Charsets.UTF_8).use { writer ->
            translatedLines.forEach {
                writer.appendln(it)
            }
        }
    }

    private fun translate(file: File) = ContextC2.translate(file.readText()).asSequence()
    private fun sourceFilesIn(dir: File) = ContextD1.sourceFilesIn(dir)
}

/*-

...

it works!
-*/


