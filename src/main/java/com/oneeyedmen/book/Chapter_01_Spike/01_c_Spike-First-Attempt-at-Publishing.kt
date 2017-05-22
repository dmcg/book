package com.oneeyedmen.book.Chapter_01_Spike

import com.oneeyedmen.book.approvalsRule
import org.junit.Rule
import org.junit.Test
import java.io.File

/*-
First Attempt at Publishing
---------------------------

Let's write some code to take a mixed prose and source file and write a Markdown version. I'm not entirely sure what that output should look like yet, but I'll know it when I see it. This is the sweet spot for Approvals Tests, which will allow us to make rapid progress but at the same time know when we've slipped back.

OK, time to write some code.
-*/

object ContextC1 {
    //`
    class CodeExtractorTests {

        @Rule @JvmField val approver = approvalsRule()

        @Test fun writes_a_markdown_file_from_Kotlin_file() {
            val source = """
            |/*-
            |Title
            |=====
            |This is Markdown paragraph
            |-*/
            |
            |/* This is a code comment
            |*/
            |fun aFunction() {
            |   return 42
            |}
            |/*-
            |More book text.
            |-*/
            """.trimMargin()
            approver.assertApproved(translate(source))
        }
    }

    fun translate(source: String) = source
    //`
}

/*-
Here I've written an example file content as a Kotlin here document, and then an identity translate function. Running the test creates a file `src/main/java/com/oneeyedmen/book/CodeExtractorTests.writes_a_markdown_file_from_Kotlin_file.actual` with the contents of the source file and fails the test. We can make the test pass by approving the content with ``` cp 'src/main/java/com/oneeyedmen/book/CodeExtractorTests.writes_a_markdown_file_from_Kotlin_file.actual' 'src/main/java/com/oneeyedmen/book/CodeExtractorTests.writes_a_markdown_file_from_Kotlin_file.approved' ``` and running it again.

Now we need to improve the `translate` function. I was about to start by stripping out the lines beginning with `/*-` and `-*/`, but if we do that first we'll loose information about where the code starts. In fact thinking it through I realise that this page has code that we don't want to view (the `package` and `import` statements at the top), and I'm sure that in general there will be other code that is required to compile but doesn't contribute to the narrative. Maybe we need to explicitly mark code to be included.
-*/

object ContextC2 {

    class CodeExtractorTests {

        @Rule @JvmField val approver = approvalsRule()

        //`
        @Test fun writes_a_markdown_file_from_Kotlin_file() {
            val source = """
            |package should.not.be.shown
            |/*-
            |Title
            |=====
            |This is Markdown paragraph
            |-*/
            |import should.not.be.shown
            |//`
            |/* This is a code comment
            |*/
            |fun aFunction() {
            |   return 42
            |}
            |//`
            |/*-
            |More book text.
            |-*/
            """.trimMargin()
            approver.assertApproved(translate(source))
        }
        //`
    }

    fun translate(source: String) = source
}

/*-
Here I've used a line comment with a backtick `//``` to mark the beginning and end of the code we want to see.

Now we can first implement the code to strip out the block comments that hide our prose from the Kotlin compiler
-*/

object ContextC3 {

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
            |import should.not.be.shown
            |//`
            |/* This is a code comment
            |*/
            |fun aFunction() {
            |   return 42
            |}
            |//`
            |/*-
            |More book text.
            |-*/
            """.trimMargin()
            approver.assertApproved(translate(source))
        }
    }

    //`
    fun translate(source: String) = source.split("\n")
        .filterNot { it.startsWith("/*-") || it.startsWith("-*/") }
        .joinToString("\n")
    //`
}

/*-
The first run of the test fails as the actual file is different from the approved - inspecting it I can see that the differences are indeed the stripped out block comment markers - the file is now

```
[TODO]
```

Let's get to work putting the code between our special markers into a Markdown code block.
-*/

object ContextC4 {

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
            |import should.not.be.shown
            |//`
            |/* This is a code comment
            |*/
            |fun aFunction() {
            |   return 42
            |}
            |//`
            |/*-
            |More book text.
            |-*/
            """.trimMargin()
            approver.assertApproved(translate(source))
        }
    }

    //`
    fun translate(source: String): String {
        var inCodeBlock = false
        var inTextBlock = false
        return source.split("\n")
            .map {
                when {
                    !inCodeBlock && it.startsWith("//`") -> { inCodeBlock = true; "```kotlin"}
                    inCodeBlock && it.startsWith("//`") -> { inCodeBlock = false; "```"}
                    !inTextBlock && it.startsWith("/*-") -> { inTextBlock = true; ""}
                    inTextBlock && it.startsWith("-*/") -> { inTextBlock = false; ""}
                    inTextBlock -> it
                    inCodeBlock -> it
                    else -> ""
                }
            }
            .joinToString("\n")
    }
    //`
}

/*-
Now I won't pretend that was easy to write, or that I'm proud of it, but it does work, yielding

```
[TODO]
```

Note that I've chosen to leave blank lines where markers and ignored text are for now, as they make making sense of the output easier.

Now of course, I have to try the code on the file that I'm typing into right now, as that is the real point.
-*/

object ContextC5 {

    @JvmStatic
    //`
    fun main(args: Array<String>) {
        val markdown = translate(File("src/test/java/com/oneeyedmen/book/Chapter_01_Spike/01_c_Spike-First-Attempt-at-Publishing.kt").readText())
        File("build/delme").apply {
            mkdirs()
            resolve("out.md").writeText(markdown)
        }
    }
    //`

    fun translate(source: String) = ContextC4.translate(source)
}

/*-
It doesn't quite work as I expected - it doesn't find publish code markers when they are indented with spaces. I suppose we should add that case to our test suite.
-*/

object ContextC6 {

    class CodeExtractorTests {

        @Rule @JvmField val approver = approvalsRule()

        //`
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
            approver.assertApproved(translate(source))
        }
        //`
    }

/*-
and implement quickly and dirtyly to see if it's good.
-*/

    //`
    fun translate(source: String): String {
        var inCodeBlock = false
        var inTextBlock = false
        return source.split("\n")
            .map {
                when {
                    !inCodeBlock && it.trim().startsWith("//`") -> { inCodeBlock = true; "```kotlin"}
                    inCodeBlock && it.trim().startsWith("//`") -> { inCodeBlock = false; "```"}
                    !inTextBlock && it.trim().startsWith("/*-") -> { inTextBlock = true; ""}
                    inTextBlock && it.trim().startsWith("-*/") -> { inTextBlock = false; ""}
                    inTextBlock -> it
                    inCodeBlock -> it
                    else -> ""
                }
            }
            .joinToString("\n")
    }
    //`
}

/*-
This works, and, after fixing some places in this file that I had messed up the formatting, it works here too.

It feels like there is something general trying to get out of that `inBlock..` code, but we'll come back to it when I'm less tired. I'll just make a small change to make it look less bad.
-*/

object ContextC7 {

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
            approver.assertApproved(translate(source))
        }
    }

    //`
    fun translate(source: String): String {
        var inCodeBlock = false
        var inTextBlock = false
        return source.split("\n")
            .map { line ->
                when {
                    !inCodeBlock && line.firstNonSpaceCharsAre("//`") -> { inCodeBlock = true; "```kotlin"}
                    inCodeBlock && line.firstNonSpaceCharsAre("//`") -> { inCodeBlock = false; "```"}
                    !inTextBlock && line.firstNonSpaceCharsAre("/*-") -> { inTextBlock = true; ""}
                    inTextBlock && line.firstNonSpaceCharsAre("-*/") -> { inTextBlock = false; ""}
                    inTextBlock -> line
                    inCodeBlock -> line
                    else -> ""
                }
            }
            .joinToString("\n")
    }

    fun String.firstNonSpaceCharsAre(s: String) = this.trimStart().startsWith(s)
    //`
}
