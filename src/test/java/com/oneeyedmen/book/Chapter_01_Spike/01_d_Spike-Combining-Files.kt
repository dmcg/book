package com.oneeyedmen.book.Chapter_01_Spike

import com.oneeyedmen.book.Chapter_01_Spike.ContextD1.sourceFilesIn
import com.oneeyedmen.book.Chapter_01_Spike.ContextD1.translate
import java.io.File

/*-
Combining Files
---------------

We're almost done in this first stint. The next thing I need to do to increase my confidence that this is working is to
 combine all the sections of this chapter into a whole, so that I and others can read it.

This text is being written into `01_d_Spike-Combining-Files.kt`, in a directory (actually Java / Kotlin package)
`com.oneeyedmen.book.Chapter_01_Spike`. Listing all the source in the directory we see

* `01_a_Spike-Introduction.kt`
* `01_b_Spike-First-Kotlin-File.kt`
* `01_c_Spike-First-Attempt-at-Publishing.kt`
* `01_d_Spike-Combining-Files.kt`

In the time it's taken to write the contents these filenames have changed several times, with the aim to allow me to see
what is in them and at the same time process them automatically. Let's see if I succeeded in that second goal by
processing all the files in sequence.

-*/

object ContextD1 {

    //`
    @JvmStatic
    fun main(args: Array<String>) {
        val dir = File("src/test/java/com/oneeyedmen/book/Chapter_01_Spike")
        val translatedLines: Sequence<String> = sourceFilesIn(dir)
            .flatMap(this::translate)
            .filterNotNull()

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

    fun translate(source: File): Sequence<String?> = translate(source.readLines(Charsets.UTF_8))

    fun translate(sourceLines: List<String>): Sequence<String?> {
        var inCodeBlock = false
        var inTextBlock = false
        return sourceLines.asSequence()
            .map { line ->
                when {
                    !inCodeBlock && line.firstNonSpaceCharsAre("//`") -> { inCodeBlock = true; "```kotlin"}
                    inCodeBlock && line.firstNonSpaceCharsAre("//`") -> { inCodeBlock = false; "```"}
                    !inTextBlock && line.firstNonSpaceCharsAre("/*-") -> { inTextBlock = true; null}
                    inTextBlock && line.firstNonSpaceCharsAre("-*/") -> { inTextBlock = false; null}
                    inTextBlock -> line
                    inCodeBlock -> line
                    else -> null
                }
            }
    }

    fun String.firstNonSpaceCharsAre(s: String) = this.trimStart().startsWith(s)
    //`
}

/*-
Looking back at the last version of the `translate` function you'll see that I have changed to taking a list of Strings
and returning a Sequence - this will allow me to avoid having all the text of all files in memory. I've also put `null`s
into that Sequence where we are skipping a line - that makes it easy for the caller to recognise them.

The rest of the code is pretty standard Kotlin - not very pretty or abstracted yet but good enough. It would probably
read very much the same if I wrote it in Python - which I would if I didn't have Kotlin here as it would be just too
verbose in Java.

Running the code and looking at the rendered Markdown with IntelliJ's Markdown plugin I see one glaring problem. Where
one file ends and another starts we need to separate them with a blank line if the combined Markdown isn't to be
interpreted as a contiguous paragraph. Let's fix that by adding a blank line to the end of each file's lines.
-*/

object ContextD2 {

    @JvmStatic
    fun main(args: Array<String>) {
        val dir = File("src/test/java/com/oneeyedmen/book/Chapter_01_Spike")
        //`
        val translatedLines: Sequence<String> = sourceFilesIn(dir)
            .flatMap { translate(it).plus("\n") }
            .filterNotNull()
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
}

/*-
There is one other issue that I can see, which has to do with trying to show in the text how I mark the prose regions.
 Where I wrote

```
I'm going to go with

|/*-
|this is book text
|-*/
```

without the prepended <code>|</code> characters, the markers were of course interpreted as markers and not output. I don't have a
 solution to this at the moment, bar the pipe, but suspect that we'll need some way of escaping our own codes. I'm
 trusting that as I gain fluency with Markdown something clever will come up.
-*/