# Chapter 1 - Spike

I'm writing this in Lisbon airport, an hour before my flight back to London is due to leave. On this holiday I've decided I'm pretty committed to writing this book, and now I'm wondering what technologies I should use for its production.

What criteria should I use to make the decision? Well the working title of the book is "Modern Programming in Kotlin" so that might help. Defining modern programming is something that I hope will become clear by the end, but as I think it through, I realise that deciding how to write the book is fundamentally an engineering decision. Engineering is about trade-offs, choice, optimisation. It's about making the best use of your time or your client's money. And very often in programming that comes down to trading the cost to make something now against the cost of fixing it later.

I know that this book will take a long time to write, and I also know that it will be full of code samples that we'd both like to actually work by the time you come to read them. So I'm looking for a way that I can mix code and prose in a way that is easy to write and maintain. 

And that's why these words are being written into the IntelliJ IDE rather than Word. They are plain text now, although I'm already thinking of them as Markdown - I just haven't *needed* to emphasise anything yet. I suspect that this prose will end up as comments in Kotlin source files, and that some sort of build system will put the book together for me. In fact, if you're reading this on paper you can't see it yet, but this file is already surrounded by a Gradle build and is in the context of a Git repository. This is the way that modern programming projects in Kotlin will start, and so it feels rather fitting that the book does too.


## The First Kotlin File

Risk reduction is high on the list of modern programming concerns. I want to know that the idea of embedding the content of this book in its own source code is tenable. So I'm going to start with a spike - a prototype that goes deep into the heart of the problem but doesn't attempt breadth. This way we hope to find major problems that might scupper an idea with the least effort.

So I'm typing this text as comments into a Kotlin source file called `2-spike.kt`. So far I've found that block comment markers don't get in the way of my text, but that I also don't get a nice Markdown preview of the formatting. That's alright though, as the nature of Markdown is that I can \*see\* the formatting even though it's only characters.

So the text is OK. What about the code?

```kotlin
import org.junit.Assert.assertEquals
import org.junit.Test

class SpikeTest {

    @Test fun `2 plus 2 is 4`() {
        assertEquals(4, add(2, 2))
    }
}

fun add(a: Int, b: Int) = a + b
```

Well I can run that in the IDE and from Gradle, so I guess that's a start. It does occur to me that when the book is printed that we're going to want to see comments in the code as well as the book text, so I'd better start using a special comment marker for the latter. Taking a lead from Javadoc, which also extends standard block comments, I'm going to go with

```text
⁠/*-
this is book text
⁠-*/
```

for now and see how it goes.

It occurs to me that this is the opposite of the normal Markdown approach, where we can embed code inside blocks, with the text as the top level. Here the code is the top level, and the Markdown is embedded in it. The advantage is that we have to do nothing to our text to compile it, the disadvantage is that something is going to have to process the files to do something with the text before we can render the Markdown. That seems reasonable to me though, as I write code for a living, and an awful lot of that code does just this sort of thing to text. In fact we now have the opportunity to practice explaining the writing of code on the code that publishes itself.

## First Attempt at Publishing

Let's write some code to take a mixed prose and source file and write a Markdown version. I'm not entirely sure what that output should look like yet, but I'll know it when I see it. This is the sweet spot for Approvals Tests, which will allow us to make rapid progress but at the same time know when we've slipped back.

OK, time to write some code.

```kotlin
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
```

Here I've written an example file content as a Kotlin here document, and then an identity translate function. Running the test creates a file `CodeExtractorTests.writes_a_markdown_file_from_Kotlin_file.actual` with the contents

~~~text
/*-
Title
=====
This is Markdown paragraph
-*/

/* This is a code comment
*/
fun aFunction() {
   return 42
}
/*-
More book text.
-*/
~~~

which is to say the source, as translate does nothing to it. The test fails, as there was no approved contents to compare with the actual - we can make it pass by approving the content with

```bash
cp 'CodeExtractorTests.writes_a_markdown_file_from_Kotlin_file.actual' 'CodeExtractorTests.writes_a_markdown_file_from_Kotlin_file.approved'
```

and running it again.

Now we need to improve the `translate` function. I was about to start by stripping out the lines beginning with `/*-` and `-*/`, but if we do that first we'll loose information about where the code starts. In fact thinking it through I realise that this page has code that we don't want to view (the `package` and `import` statements at the top), and I'm sure that in general there will be other code that is required to compile but doesn't contribute to the narrative. Maybe we need to explicitly mark code to be included.

```kotlin
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
```

Here I've used a line comment with a backtick `//``` to mark the beginning and end of the code we want to see.

Now we can first implement the code to strip out the block comments that hide our prose from the Kotlin compiler

```kotlin
fun translate(source: String) = source.split("\n")
    .filterNot { it.startsWith("/*-") || it.startsWith("-*/") }
    .joinToString("\n")
```

The first run of the test fails as the actual file is different from the approved - inspecting it I can see that the differences are indeed the stripped out block comment markers - the file is now

~~~text
package should.not.be.shown
Title
=====
This is Markdown paragraph
import should.not.be.shown
//`
/* This is a code comment
*/
fun aFunction() {
   return 42
}
//`
More book text.
~~~

Let's get to work putting the code between our special markers into a Markdown code block.

```kotlin
fun translate(source: String): String {
    var inCodeBlock = false
    var inTextBlock = false
    return source.split("\n")
        .map {
            when {
                !inCodeBlock && it.startsWith("//`") -> {
                    inCodeBlock = true
                    "```kotlin"
                }
                inCodeBlock && it.startsWith("//`") -> {
                    inCodeBlock = false
                    "```"
                }
                !inTextBlock && it.startsWith("/*-") -> {
                    inTextBlock = true
                    ""
                }
                inTextBlock && it.startsWith("-*/") -> {
                    inTextBlock = false
                    ""
                }
                inTextBlock -> it
                inCodeBlock -> it
                else -> ""
            }
        }
        .joinToString("\n")
}
```

Now I won't pretend that was easy to write, or that I'm proud of it, but it does work, yielding

~~~


Title
=====
This is Markdown paragraph


```kotlin
/* This is a code comment
*/
fun aFunction() {
   return 42
}
```

More book text.
~~~

Note that I've chosen to leave blank lines where markers and ignored text are for now, as they make making sense of the output easier.

Now of course, I have to try the code on the file that I'm typing into right now, as that is the real point.

```kotlin
fun main(args: Array<String>) {
    val markdown = translate(File("src/test/java/com/oneeyedmen/book/Chapter_01_Spike/01_c_Spike-First-Attempt-at-Publishing.kt").readText())
    File("build/delme").apply {
        mkdirs()
        resolve("out.md").writeText(markdown)
    }
}
```

It doesn't quite work as I expected - it doesn't find publish code markers (\''') when they are indented with spaces. I suppose we should add that case to our test suite.

```kotlin
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
```

and implement quickly and dirtyly to see if it's good.

```kotlin
fun translate(source: String): String {
    var inCodeBlock = false
    var inTextBlock = false
    return source.split("\n")
        .map {
            when {
                !inCodeBlock && it.trim().startsWith("//`") -> {
                    inCodeBlock = true
                    "```kotlin"
                }
                inCodeBlock && it.trim().startsWith("//`") -> {
                    inCodeBlock = false
                    "```"
                }
                !inTextBlock && it.trim().startsWith("/*-") -> {
                    inTextBlock = true
                    ""
                }
                inTextBlock && it.trim().startsWith("-*/") -> {
                    inTextBlock = false
                    ""
                }
                inTextBlock -> it
                inCodeBlock -> it
                else -> ""
            }
        }
        .joinToString("\n")
}
```

This works, and, after fixing some places in this file that I had messed up the formatting, it works here too.

It feels like there is something general trying to get out of that `inBlock..` code, but we'll come back to it when I'm less tired. I'll just make a small change to make it look less bad.

```kotlin
fun translate(source: String): String {
    var inCodeBlock = false
    var inTextBlock = false
    return source.split("\n")
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
                    ""
                }
                inTextBlock && line.firstNonSpaceCharsAre("-*/") -> {
                    inTextBlock = false
                    ""
                }
                inTextBlock -> line
                inCodeBlock -> line
                else -> ""
            }
        }
        .joinToString("\n")
}

fun String.firstNonSpaceCharsAre(s: String) = this.trimStart().startsWith(s)
```


## Conclusions

What has this chapter been trying to prove?

Firstly I've been trying to prove to myself that this model works for me when actually explaining concepts. Unless you're viewing the source version of this text, you won't see that there is a subtlety around the different versions of the code existing in the same source file. When explaining code on my blog I would have to move forward and back between different source versions, cutting and pasting them into the code. Here, I can keep the different versions in the same file, which is a big productivity win.

Secondly, I'm trying to demonstrate risk reduction - making sure that I don't commit too much effort into a doomed venture. We've done just enough here to show that the book-in-comments approach has legs. No doubt I'll spend days on tooling to produce the book before I'm finished - the code is rough and the inability to write about my own notation without invoking it is a shame - but right now we have confidence without too much cost.

Tooling is another take-away. Modern developers will go to great lengths to increase their own productivity by building tools to leverage their time. Sometimes these are simple scripts, sometimes full applications. I'm hoping to leverage the power of an IDE that I already know with some text processing to produce something greater than the sum of its parts.

Leverage is my final thought. Great developers find ways of solving several problems at once. I don't claim to be a great developer, but I am secretly pleased with having tested the writing model, written some tooling to support it, and perhaps demonstrated some aspects of modern programming, all at the same time.

## Combining Files

We're almost done in this first stint. The next thing I need to do to increase my confidence that this is working is to combine all the sections of this chapter into a whole, so that I and others can read it.

This text is being written into `01_d_Spike-Combining-Files.kt`, in a directory (actually Java / Kotlin package) `com.oneeyedmen.book.Chapter_01_Spike`. Listing all the source in the directory we see

* `01_a_Spike-Introduction.kt`
* `01_b_Spike-First-Kotlin-File.kt`
* `01_c_Spike-First-Attempt-at-Publishing.kt`
* `01_d_Spike-Combining-Files.kt`

In the time it's taken to write the contents, these filenames have changed several times, with the aim to allow me to see what is in them and at the same time process them automatically. Let's see if I succeeded in that second goal by processing all the files in sequence.


```kotlin
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
```

Looking back at the last version of the `translate` function you'll see that I have changed returning a Sequence - this will allow me to avoid having all the text of all files in memory. I've also put `null`s into that Sequence where we are skipping a line, and then filtered out nulls from the Sequence with `filterNotNull`, so that we don't have a blank line for each source line we aren't outputting.

The rest of the code is pretty standard Kotlin - not very pretty or abstracted yet but good enough. It would probably read very much the same if I wrote it in Python - which I would if I didn't have Kotlin here as it would be just too verbose in Java.

I had to up update the test as well as the code to account for the change to the signature of `translate`. The test is now


```kotlin
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
```

and the approved file shows no blank lines where we the skip a line in the source

~~~text
Title
=====
This is Markdown paragraph
```kotlin
  /* This is a code comment
  */
  fun aFunction() {
     return 42
  }
```
More book text.
~~~

Running the code and looking at the rendered Markdown with IntelliJ's Markdown plugin I see one glaring problem. Where one file ends and another starts we need to separate them with a blank line if the combined Markdown isn't to be interpreted as a contiguous paragraph. Let's fix that by adding a blank line to the end of each file's lines.

```kotlin
val translatedLines: Sequence<String> = sourceFilesIn(dir)
    .flatMap { translate(it).plus("\n") }
```

There is one other issue that I can see, which has to do with trying to show in the text how I mark the prose regions. Where I wrote

```text
I'm going to go with

⁠/*-
this is book text
⁠-*/
```

the markers were interpreted as markers and messed up the output. I add a pipe character `|` to the beginning of those marker lines to get things running. I don't have a solution to this at the moment, bar the pipe, but suspect that we'll need some way of escaping our own codes. I'm trusting that as I gain fluency with Markdown something clever will come up. If you can see the markers without `|`s above I guess I succeeded in the end.

## Request for Feedback

Well you've either got this far, or skipped here having decided that the content wasn't from you. Could you spare a couple of minutes to help me gauge whether I am wasting my time?

If so then create an [email to me](mailto:duncan@oneeyedmen.com?subject=Book+Feedback), copy the following into an email, start answering at the top, keep going until you don't think you owe me any more or your precious time, and send. I'm sorry that I don't have an embedded form or anything - yet. Maybe if there is enough encouragement the processing of the form will become a chapter!

>  1. Should I continue writing this book? [y \| n]
>  2. Are you in the book's target market (3 - 10+ years of programming experience)? [y \| n]
>  3. What do you think of the recursive nature of the material, writing about the development of the software to assist the writing? [0 (disaster) - 10 (triumph)]
>  4. How likely are you to recommend the book to a friend or colleague? [0 - 10]
>  5. Would you pay to read the completed book? [y \| n]
>  6. Do you care about paper copies of books? [y \| n]
>  7. Anything else you'd like to say?

Thank you

Duncan


