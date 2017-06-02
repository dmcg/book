package com.oneeyedmen.book.Chapter_01_Spike

import com.oneeyedmen.book.approvalsRule
import org.junit.Rule
import org.junit.Test
import java.io.File

/*-
Combining Files
---------------

We're almost done in this first stint. The next thing I need to do to increase my confidence that this is working is to combine all the sections of this chapter into a whole, so that I and others can read it.

This text is being written into `01_d_Spike-Combining-Files.kt`, in a directory (actually Java / Kotlin package) `com.oneeyedmen.book.Chapter_01_Spike`. Listing all the source in the directory we see

* `01_a_Spike-Introduction.kt`
* `01_b_Spike-First-Kotlin-File.kt`
* `01_c_Spike-First-Attempt-at-Publishing.kt`
* `01_d_Spike-Combining-Files.kt`

In the time it's taken to write the contents, these filenames have changed several times, with the aim to allow me to see what is in them and at the same time process them automatically. Let's see if I succeeded in that second goal by processing all the files in sequence.

-*/

object ContextD1 {

    @JvmStatic
    //`
    fun main(args: Array<String>) {
        val dir = File("src/main/java/com/oneeyedmen/book/Chapter_01_Spike")
        val translatedLines: Sequence<String> = sourceFilesIn(dir)
            .flatMap(this::translate)

        val outDir = File("build/book").apply {
            mkdirs()
        }

        outDir.resolve(dir.name + ".md").bufferedWriter(Charsets.UTF_8).use { writer ->
            translatedLines.forEach {
                writer.appendln(it)
            }
        }
    }

    fun sourceFilesIn(dir: File) = dir
        .listFiles { file -> file.isSourceFile() }
        .toList()
        .sortedBy(File::getName)
        .asSequence()

    private fun File.isSourceFile() = isFile && !isHidden && name.endsWith(".kt")

    fun translate(source: File): Sequence<String> = translate(source.readText(Charsets.UTF_8))

    fun translate(sourceLines: String): Sequence<String> {
        var inCodeBlock = false
        var inTextBlock = false
        return sourceLines.splitToSequence("\n")
            .map { line ->
                when {
                    !inCodeBlock && line.firstNonSpaceCharsAre("//`") -> {
                        inCodeBlock = true
                        "```kotlin"
                    }
                    inCodeBlock && line.firstNonSpaceCharsAre("//`") -> {
                        inCodeBlock = false
                        "```"
                    }
                    !inTextBlock && line.firstNonSpaceCharsAre("/*-") -> {
                        inTextBlock = true
                        null
                    }
                    inTextBlock && line.firstNonSpaceCharsAre("-*/") -> {
                        inTextBlock = false
                        null
                    }
                    inTextBlock -> line
                    inCodeBlock -> line
                    else -> null
                }
            }
            .filterNotNull()
    }

    fun String.firstNonSpaceCharsAre(s: String) = this.trimStart().startsWith(s)
    //`
}

/*-
Looking back at the last version of the `translate` function you'll see that I have changed returning a Sequence - this will allow me to avoid having all the text of all files in memory. I've also put `null`s into that Sequence where we are skipping a line, and then filtered out nulls from the Sequence with `filterNotNull`, so that we don't have a blank line for each source line we aren't outputting.

The rest of the code is pretty standard Kotlin - not very pretty or abstracted yet but good enough. It would probably read very much the same if I wrote it in Python - which I would if I didn't have Kotlin here as it would be just too verbose in Java.

I had to up update the test as well as the code to account for the change to the signature of `translate`. The test is now

-*/
object ContextD2 {
    //`
    class CodeExtractorTests {

        @Rule @JvmField val approver = approvalsRule()

        @Test fun writes_a_markdown_file_from_Kotlin_file() {
            val source = """
            |package should.not.be.shown
            |/*-
            |Title
            |=====
            |This is Markdown paragraph
            |-*/
            |object HiddenContext {
            |  //`
            |  /* This is a code comment
            |  */
            |  fun aFunction() {
            |     return 42
            |  }
            |  //`
            |}
            |/*-
            |More book text.
            |-*/
            """.trimMargin()
            approver.assertApproved(translate(source).joinToString("\n"))
        }
    }
    //`

    private val translate: (String) -> Sequence<String?> = ContextD1::translate
}
/*-
and the approved file shows no blank lines where we the skip a line in the source

~~~text
-*/
//#include "ContextD2.CodeExtractorTests_writes_a_markdown_file_from_Kotlin_file.approved"
/*-
~~~

Running the code and looking at the rendered Markdown with IntelliJ's Markdown plugin I see one glaring problem. Where one file ends and another starts we need to separate them with a blank line if the combined Markdown isn't to be interpreted as a contiguous paragraph. Let's fix that by adding a blank line to the end of each file's lines.
-*/

object ContextD3 {

    @JvmStatic
    fun main(args: Array<String>) {
        val dir = File("src/main/java/com/oneeyedmen/book/Chapter_01_Spike")
        //`
        val translatedLines: Sequence<String> = sourceFilesIn(dir)
            .flatMap { translate(it).plus("\n") }
        //`

        val outDir = File("build/book").apply {
            mkdirs()
        }

        outDir.resolve(dir.name + ".md").bufferedWriter(Charsets.UTF_8).use { writer ->
            translatedLines.forEach {
                writer.appendln(it)
            }
        }
    }

    private fun sourceFilesIn(dir: File) = ContextD1.sourceFilesIn(dir)
    private fun translate(file: File) = ContextD1.translate(file)
}

/*-
There is one other issue that I can see, which has to do with trying to show in the text how I mark the prose regions. Where I wrote

```text
I'm going to go with

⁠/*-
this is book text
⁠-*/
```

the markers were interpreted as markers and messed up the output. I add a pipe character `|` to the beginning of those marker lines to get things running. I don't have a solution to this at the moment, bar the pipe, but suspect that we'll need some way of escaping our own codes. I'm trusting that as I gain fluency with Markdown something clever will come up. If you can see the markers without `|`s above I guess I succeeded in the end.
-*/