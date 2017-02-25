package com.oneeyedmen.book.Chapter_01_Spike

import com.oneeyedmen.okeydoke.junit.ApprovalsRule
import org.junit.Rule
import org.junit.Test
import java.io.File

/*-
First Attempt at Publishing
---------------------------

Let's write some code to take a mixed prose and source file and write a Markdown version. I'm not entirely sure what
 that output should look like yet, but I'll know it when I see it. This is the sweet spot for Approvals Tests, which
 will allow us to make rapid progress but at the same time know when we've slipped back.

OK, time to write some code.
-*/

object Context1 {
    //`
    class CodeExtractorTests1 {

        @Rule @JvmField val approver = ApprovalsRule.usualRule()

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
Here I've written an example file content as a Kotlin here document, and then an identity translate function. Running
 the test creates a file `src/main/java/com/oneeyedmen/book/CodeExtractorTests.writes_a_markdown_file_from_Kotlin_file.actual`
 with the contents of the source file and fails the test. We can make the test pass by approving the content with
```
cp 'src/main/java/com/oneeyedmen/book/CodeExtractorTests.writes_a_markdown_file_from_Kotlin_file.actual' 'src/main/java/com/oneeyedmen/book/CodeExtractorTests.writes_a_markdown_file_from_Kotlin_file.approved'
```

and running it again.

Now we need to improve the `translate` function. I was about to start by stripping out the lines beginning with `/*-` and `-*/`,
 but if we do that first we'll loose information about where the code starts. In fact thinking it through I realise that
 this page has code that we don't want to view (the `package` and `import` statements at the top), and I'm sure that in
 general there will be other code that is required to compile but doesn't contribute to the narrative. Maybe we need to
 explicitly mark code to be included.
-*/

object Context2 {

    class CodeExtractorTests2 {

        @Rule @JvmField val approver = ApprovalsRule.usualRule()

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

object Context3 {

    class CodeExtractorTests3 {

        @Rule @JvmField val approver = ApprovalsRule.usualRule()

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
The first run of the test fails as the actual file is different from the approved - inspecting it I can see that the
 differences are indeed the stripped out block comment markers - the file is now

```
[TODO]
```

Let's get to work putting the code between our special markers into a Markdown code block.
-*/

object Context4 {

    class CodeExtractorTests4 {

        @Rule @JvmField val approver = ApprovalsRule.usualRule()

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

Note that I've chosen to leave blank lines where markers and ignored text are for now, as they make making sense of the
output easier.

Now of course, I have to try the code on the file that I'm typing into right now, as that is the real point.
-*/

object Context5 {

    //`
    @JvmStatic
    fun main(args: Array<String>) {
        val markdown = translate(File("src/test/java/com/oneeyedmen/book/Chapter_01_Spike/01_Spike_c.kt").readText())
        File("delme").apply {
            mkdirs()
            resolve("out.md").writeText(markdown)
        }
    }
    //`

    fun translate(source: String) = Context4.translate(source)
}

/*-
It doesn't quite work as I expected - it doesn't find publish code markers when they are indented with spaces. I suppose we should
add that case to our test suite.
-*/

object Context6 {

    class CodeExtractorTests6 {

        @Rule @JvmField val approver = ApprovalsRule.usualRule()

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

object Context7 {

    @JvmStatic
    fun main(args: Array<String>) {
        val markdown = translate(File("src/test/java/com/oneeyedmen/book/Chapter_01_Spike/01_Spike_c.kt").readText())
        File("delme").apply {
            mkdirs()
            resolve("out.md").writeText(markdown)
        }
    }

    fun translate(source: String): String  = Context6.translate(source)
}

/*-
This works, and, after fixing some places in this file that I had messed up the formatting, it works here too.

It feels like there is something general trying to get out of that `inBlock..` code, but we'll come back to it when I'm
less tired. I'll just make a small change to make it look less bad.
-*/

object Context8 {

    class CodeExtractorTests8 {

        @Rule @JvmField val approver = ApprovalsRule.usualRule()

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

/*-
What has this episode been trying to prove?

Firstly I've been trying to prove to myself that this model works for me when actually explaining concepts. Unless
 you're viewing the source version of this text, you won't see that there is a subtlety around the different versions
 of the code existing in the same source file. When explaining code on my blog I would have to move forward and back
 between different source versions, cutting and pasting them into the code. Here, I can keep the different versions
 in the same file, which is a big productivity win.

Secondly, I'm trying to demonstrate risk reduction - making sure that I don't commit too much effort into a doomed
 venture. We've done just enough here to show that the book-in-comments approach has legs. No doubt I'll spend days
 on tooling to produce the book before I'm finished, but right now we have confidence without too much cost.

Tooling is another take-away. Modern developers will go to great lengths to increase their own productivity by building
 tools to leverage their time. Sometimes these are simple scripts, sometimes full applications. I'm hoping to leverage
 the power of an IDE that I already know with some text processing to produce something greater than the sum of its
 parts.

Leverage is my final thought. Great developers find ways of solving several problems at once. I don't claim to be a
  great developer, but I am secretly pleased with having tested the writing model, written some tooling to support it,
  and perhaps demonstrated some aspects of modern programming, all at the same time.
-*/

