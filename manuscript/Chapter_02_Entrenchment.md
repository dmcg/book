# Chapter 2 - Entrenchment

Writing software is hard. We probably agree on that - I know that I find it hard, and you probably aren't investing time reading a book on a subject that you have no problem mastering. One of the many many hard problems is deciding, minute by minute, hour by hour, day by, well, you get the drift, what we should do next.

So far we have, if you'll forgive a military metaphor, taken some ground. Our previous foray resulted in some confidence that this book might be worth writing, and that the technology and process I proposed might do the job. But in order to gain this confidence we have overextended a bit. Some things don't work at all, and others are not implemented well.

Agile development calls this technical debt, but that term is a bit pejorative, and doesn't allow us to distinguish between, if you'll forgive a finance metaphor, leverage and lack of servicing. [TODO]

I was about to suggest that we review what does and doesn't work, maybe have a little refactor to make things clearer, but then a customer arrived who wanted to use the code in real life. Granted that customer was me, wanting to use the tool to simplify publishing code-heavy articles in my blog, but a customer is a customer. We're always having to balance immediately gratifying our customer or users against code quality - it can lead to some difficult negotiations - but in this case I'm insistent that writing a blog article outweighs the nagging doubt that the code just isn't good enough, and I find that I agree.

Our current main method is hard-coded to the source path in this project. Finding a way to run the code from the command-line, specifying the source directory and destination file as parameters, would get me off my back, so I set to.


```kotlin
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
```

I test this using IntelliJ's runner and then go rummaging in the far corners of the Internet to find out how to invoke Gradle to both build and then run this class. Full-disclosure, this led to over an hour of trying to work out what Gradle was building where - code can seem very simple compared to build systems. As I know that my chances of remembering what incantations are required are low, I capture the required commands in a top level script in the project directory -

```bash
#!/usr/bin/env bash

./gradlew clean installDist distZip
echo Zip file built in build/distributions
echo Start script built in build/install/book/bin/book
```

I then put on my blogger's hat and write a couple of articles on the relationship between objects and functions in Kotlin, using the same formatting rules as this book. It goes pretty well, giving me more confidence that I could write a whole book this way, but it does reveal a minor issue in the translation.

It turns out that my blog publishing software, unlike IntelliJ's Markdown renderer, requires a blank line before a code block. Our current translate logic removes blank lines. The fix is simple - output any blank sources lines.

Returning to this project after a little time, I review the code and the prose, and find that I can just about follow my original thread. I'm still far from convinced that anyone else could make sense of it though. The best way to find out would be to share the first chapter with friends to get their feedback, but the published Markdown files aren't yet actually readable. In particular

1. I have no way of escaping my own formatting markers, so I can't represent them in the output text.

2. The contents of the approved files are not shown (as I write this they are represented by `[TODO]` markers).

Markdown doesn't have any standard way of representing included text, but as we're reading and writing every line it shouldn't be too hard to do this ourselves. That leaves the escaping as a bigger risk, as I don't have a clue what to do.


What was the problem again? I want to be able to write express in Markdown the syntax for my 'book text' markers, namely `/*-` and `-*/`, but in a quoted section, so that you can read

```text
⁠/*-
this is book text
⁠-*/
```

but when I type that and run it through the translation, it (correctly) interprets the markers and messes up the formatting. I had bodged around the problem by prepending `| characters to the lines that cause problems, but that makes them very hard to read.

I talk the problem through with my friend Alan, and we hatch the plan of interpreting the traditional `\` as an escape character which will just be removed from the output, allowing me to write `/*-` and `*/` at the beginning of a line. Of course in order to write `\` there will need to be additional logic to replace a double-escape with a single.

Thinking through the consequences this looks a less good plan, as I may wish to use genuine escape sequences in the Kotlin code that is output as part of the book. I *could* just ignore `/*-` `-*/` pair in quote blocks, but then I'd be having to parse Markdown which looks like a can of pigeons.

So instead I guess that we should pick a character other than `\` to prevent text matches but not be rendered in our final output. This looks like a job for Unicode, so now we have 2 problems.

A bit of Googling reveals Unicode U+2060 - "WORD JOINER", which is apparently similar to a zero-width space, but not a point at which a line can be broken. This seems perfect - I can enter it with the Mac hex input keyboard mode, and it prevents our special text being interpreted at the beginning of a line. It is rendered properly in IntelliJ, which is to say, not at all, which is great if you're reading, but not so good when editing. I can probably use its HTML entity form `\&#2060;` if I want to make it more visible.

Frankly this is all making my head hurt. My spidey senses tell me that this going to cost me in the future, but for now, I need to get on with things and this change required no code, so I find all the places where I used the `| hack and replace them with the invisible word joiner.


Ironically, writing about the attempt to fix the representing-our-own-codes-in-our-output problem has given us the same problem in another set of files, those for this chapter, currently Chapter 2. I'd like to be able to review the whole book so far, which means converting two chapters' worth of Kotlin files into Markdown for reading. Luckily at the top of this chapter I worked out how to build the current software to be invoked from the command-line, so I add a top-level script to invoke this twice for the two current chapters.

```bash
#!/usr/bin/env bash

build/install/book/bin/book src/main/java/com/oneeyedmen/book/Chapter_01_Spike build/book/Chapter_01_Spike.md

build/install/book/bin/book src/main/java/com/oneeyedmen/book/Chapter_02_Entrenchment build/book/Chapter_02_Entrenchment.md
```

Running this yields two Markdown files that I can preview with IntelliJ's Markdown plugin to check that they look sensible.

This script leaves me feeling quite dirty - it can only work with one particular working directory, it has duplication, and it will require editing every time that I add a chapter (but at least not every file within a chapter). If I was sharing this code with a team I think that pride would cause me to do a better job, but for now momentum is more important than long-term efficiency, so I suck it up and move on to the next thing keeping me from sending out a review text - including the contents of files.

To review - the issue is that currently the book text has `[TODO]` markers in place of the contents of the Approvals Tests approved files. I could copy and paste the text from the file into the manuscript of course, but that breaks one of those rules that we ignore at our peril - the Single Point of Truth. If I do copy and paste the text then it can get out of sync with the file contents, which would be confusing for you, the reader. It doesn't seem like automatically including files should be hard, and I can think of some other places that it might come in handy, so let's just get on and implement it.

Our approach to special codes so far has been to enrich Kotlin comments, so that the compiler ignores our information. It's easy to extend that approach, and I settle on

```text
//#include "filename.txt"
```

as worth trying.

Let's get on and write a new test for including files.

```kotlin
class CodeExtractorTests {

    @Rule @JvmField val approver = approvalsRule()

    @Test fun writes_a_markdown_file_from_Kotlin_file() {
        val source = """
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
        """.trimMargin()
        approver.assertApproved(translate(source).joinToString("\n"))
    }

    @Test fun includes_a_file() {
        val source = """
        |/*-
        |Book text
        |
        |//#include "included-file.txt"
        |
        |more text
        |/*-
        """.trimMargin()
        approver.assertApproved(translate(source).joinToString("\n"))
    }
}
```

Whereas previously we had a single test for all of our formatting, #include seems like an orthogonal concern, so it gets its own test.

Before we go on I decide to remove the duplication in the test methods

```kotlin
class CodeExtractorTests {

    @Rule @JvmField val approver = approvalsRule()

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
        """)
    }

    @Test fun includes_a_file() {
        checkApprovedTranslation("""
        |/*-
        |Book text
        |
        |//#include "included-file.txt"
        |
        |more text
        |/*-
        """)
    }

    private fun checkApprovedTranslation(source: String) {
        approver.assertApproved(translate(source.trimMargin()).joinToString("\n"))
    }
}
```

which by removing clutter lets us focus on what we are really trying to demonstrate, albeit the actual result is hidden in the approved files.

Speaking of which, we can't really approve the output for `includes_a_file` as we don't. In order to get a little further lets create a file next to this Kotlin file with some text to be included

```
included file first line
included file last line
```

It's worth noting at this point that what to put in this file took some thought. I originally wrote `included file line 1` and `included file line 2` but realised that you would have to know that there wasn't a line 3 to know that the results were correct. I have also not put a line break at the end of the included file, because the files that we want to include do not have trailing break. Should the inclusion add a break? My gut feeling is yes, but I'm not going to think too hard. Instead I'll go with that gut feeling and see if it works out when run over the book text so far.

Where to put the file also raises some questions. The files that we want to include are currently in the same directory as the file that includes them. Until I have a need for another behaviour I guess that

```text
//#include "filename.txt"
```

should resolve `filename.txt` relative to the the source file. But so far, our tests have just translated strings and have no concept of their fileyness.

We could change our tests to read the source from the filesystem, but that would mean that we had to look in a source file, an approved file, and the test, to work out what is going on. Instead I'm going to try punting the problem upstream, by requiring that callers of the `translate` function provide a way of resolving a file path to the contents of that filename, in addition to the string that they want translated. In the test, we'll use a lambda that just returns a known string.

```kotlin
@Test fun includes_a_file() {
    checkApprovedTranslation(
        source = """
            |/*-
            |Book text
            |
            |//#include "included-file.txt"
            |
            |more text
            |/*-""",
        fileContents = """
            |included file first line
            |included file last line""")
}

private fun checkApprovedTranslation(source: String, fileContents: String) {
    approver.assertApproved(translate(source.trimMargin(), { fileContents }))
}

fun translate(source: String, fileReader: (String) -> String): String {
    TODO()
}
```

With this scheme it turns out that I needn't have created the file, so I delete it. I'm also regretting the technical overdraft that is `translate`, but I really don't have a better idea at the moment, so I'm inclined to leave the logic as-is, but at least try to make it not much worse. Perhaps the easiest way of achieving that is to treat file inclusion as a pre-processor step before our previous logic.

```kotlin
fun translate(source: String, fileReader: (String) -> String) =
    translate(preprocess(source, fileReader))

fun preprocess(source: String, fileReader: (String) -> String): String {
    TODO()
}
```

This keeps things simple, but as I think through the consequences I realise that it means that the contents of the included file will be subject to the translation process, which would result in a complete mess when our use-case is including a file which sometimes includes our un-translated codes.

So it's back to the drawing board.

I'm mulling it over when I realise that I now have two problems caused by translating our codes when we don't want them translated - this, and the last issue that I had solved using the Unicode Word Joiner. In both cases the only time we see the issue in practice is

```text
inside Markdown quote blocks
```

I had previously rejected the idea of suspending translation inside these blocks, but now that it solves two problems it's looking more attractive. But it also feels kind of arbitrary. Notwithstanding my desire to make progress quickly to publish Chapter 1 for review, the aim of this book is to show largely unedited the process of writing software. I could come back and re-write this chapter when I can make it look smooth, but the truth is that I'm having a bit of a crisis of confidence about the whole translation code. I think it's time to step back and see if we are still on the right track.

This is a very familiar stage in a software project for me. We have a kinda-cool idea - embedding the book text into the source of its examples rather than vice-versa. A spike shows that it works well for the customer - in this case me. We build on that spike, but then find that a number of tactical decisions driven by a desire to maintain momentum lead to a muddied model. It's all a bit harder than we expected, and we rightly question whether we are on the right path at all.

So let's review my options.

I could abandon the prose-in-code model - there are other ways to embed code and make sure that it compiles and works as expected. But none that I've tried allow the degree of refactoring and cross-referencing that I'm enjoying here, at least not with the immediate ability to visualise the output that this solution has. On the whole I'm inclined to continue down the path, and not just because the path is giving me things to write about.

Is there some more off-the-shelf processing system that I could use? Adopting a standard templating language would bring things like escaping and file inclusion for free. Perhaps more importantly, they will have worked through the sort of issues that I'm having - is a file included before or after expansion for example. On the downside, they are usually aimed at embedding in files like HTML rather than Kotlin source, so I might find that I end up in a dead-end where I can't solve a problem at all.

Assuming that I continue with prose-in-code, what has this experiment taught me about the translation?

1. The source files have to be parsable by the Kotlin compiler and IntelliJ has to be able to treat them as regular source. That's why the prose has to go into comment blocks.

2. I want to distinguish between prose comment blocks and code comment blocks - leading to the use of `/*-` and `-*/` markers, taking a lead from Javadoc. Actually I don't think that I've actually made use the distinction, but its cost is low.

3. Code from the file should be expanded into Markdown code blocks, but only for some sections of the file. We can't use a block comment to mark those sections because they would then not be visible to the Kotlin compiler, so instead we use `//\`` to mark the start and end of the blocks. The compiler will ignore the lines as they begin with a comment, and I can associate the backtick as Markdown's quote character. On reflection `//\`\`\`` would have been even better - maybe that's a change I can make later.

4. So far our special syntax has been able to occupy a line of its own, although in the case of the `//\`` markers they need to be indented to allow the code they are in to be reformatted.

5. There has to be a way to have the markup rules written to the output rather than interpreted.

6. I need a way of including the contents of a file in a Markdown quote block, without interpreting the contents of the file.

I can't see any way that an off-the-shelf templating system could meet these requirements - the need to interoperate with Kotlin comments being the key issue. But I still really like this way of working, so I'm going to push on with the code that I have.

Deciding to go on is all very well, but I don't think I'm much closer to knowing how to implement file inclusion. In these circumstances I've learned to just play with code until inspiration strikes. In particular - refactor until where the new feature should live is obvious.

We shouldn't refactor code that doesn't work though, so let's back-out our last change and remove the inclusion test to see what we have.

```kotlin
class CodeExtractorTests {

    @Rule @JvmField val approver = approvalsRule()

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
        """)
    }

    private fun checkApprovedTranslation(source: String) {
        approver.assertApproved(translate(source.trimMargin()).joinToString("\n"))
    }
}

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
                line.isBlank() -> line
                else -> null
            }
        }
        .filterNotNull()
}
```

I never was happy with the `translate` method - let's refactor to see what comes out. I don't believe that we can be in a code block and a text block at the same time, but the code doesn't express this. I'm going to replace the two variables with one, first by replacing `inCodeBlock` with an object. Note that each one of these steps keeps the tests passing.

```kotlin
object InCodeBlock

fun translate(sourceLines: String): Sequence<String> {
    var state: InCodeBlock? = null
    var inTextBlock = false
    return sourceLines.splitToSequence("\n")
        .map { line ->
            when {
                state != InCodeBlock && line.firstNonSpaceCharsAre("//`") -> {
                    state = InCodeBlock
                    "```kotlin"
                }
                state == InCodeBlock && line.firstNonSpaceCharsAre("//`") -> {
                    state = null
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
                state == InCodeBlock -> line
                line.isBlank() -> line
                else -> null
            }
        }
        .filterNotNull()
}
```

Then `inTextBlock`. It turns out that we don't really care if we are in a text block or not when we see it's end markers, so we can also simplify the when clauses and keep the tests passing.

```kotlin
object InCodeBlock
object InTextBlock

fun translate(sourceLines: String): Sequence<String> {
    var state: Any? = null
    return sourceLines.splitToSequence("\n")
        .map { line ->
            when {
                state != InCodeBlock && line.firstNonSpaceCharsAre("//`") -> {
                    state = InCodeBlock
                    "```kotlin"
                }
                state == InCodeBlock && line.firstNonSpaceCharsAre("//`") -> {
                    state = null
                    "```"
                }
                line.firstNonSpaceCharsAre("/*-") -> {
                    state = InTextBlock
                    null
                }
                line.firstNonSpaceCharsAre("-*/") -> {
                    state = null
                    null
                }
                state == InTextBlock -> line
                state == InCodeBlock -> line
                line.isBlank() -> line
                else -> null
            }
        }
        .filterNotNull()
}
```

and then replace null with Other and create a State class

```kotlin
sealed class State {
    object InCodeBlock : State()
    object InTextBlock : State()
    object Other : State()
}

fun translate(sourceLines: String): Sequence<String> {
    var state: State = State.Other
    return sourceLines.splitToSequence("\n")
        .map { line ->
            when {
                state != State.InCodeBlock && line.firstNonSpaceCharsAre("//`") -> {
                    state = State.InCodeBlock
                    "```kotlin"
                }
                state == State.InCodeBlock && line.firstNonSpaceCharsAre("//`") -> {
                    state = State.Other
                    "```"
                }
                line.firstNonSpaceCharsAre("/*-") -> {
                    state = State.InTextBlock
                    null
                }
                line.firstNonSpaceCharsAre("-*/") -> {
                    state = State.Other
                    null
                }
                state == State.InTextBlock -> line
                state == State.InCodeBlock -> line
                line.isBlank() -> line
                else -> null
            }
        }
        .filterNotNull()
}
```

Now I can delegate what happens to non-transition lines to the State

```kotlin
sealed class State {
    abstract fun outputFor(line: String): String?

    object InCodeBlock : State() {
        override fun outputFor(line: String) = line
    }

    object InTextBlock : State() {
        override fun outputFor(line: String) = line
    }

    object Other : State() {
        override fun outputFor(line: String) = if (line.isBlank()) line else null
    }

}

fun translate(sourceLines: String): Sequence<String> {
    var state: State = State.Other
    return sourceLines.splitToSequence("\n")
        .map { line ->
            when {
                state != State.InCodeBlock && line.firstNonSpaceCharsAre("//`") -> {
                    state = State.InCodeBlock
                    "```kotlin"
                }
                state == State.InCodeBlock && line.firstNonSpaceCharsAre("//`") -> {
                    state = State.Other
                    "```"
                }
                line.firstNonSpaceCharsAre("/*-") -> {
                    state = State.InTextBlock
                    null
                }
                line.firstNonSpaceCharsAre("-*/") -> {
                    state = State.Other
                    null
                }
                else -> state.outputFor(line)
            }
        }
        .filterNotNull()
}
```

Now some lines cause the state to change, and some don't. I'm going to pull out a method called `advance` that returns the next state and the string that should be output.

```kotlin
sealed class State {
    abstract fun outputFor(line: String): String?

    object InCodeBlock : State() {
        override fun outputFor(line: String) = line
    }

    object InTextBlock : State() {
        override fun outputFor(line: String) = line
    }

    object Other : State() {
        override fun outputFor(line: String) = if (line.isBlank()) line else null
    }

}

fun translate(sourceLines: String): Sequence<String> {
    var state: State = State.Other
    return sourceLines.splitToSequence("\n")
        .map { line ->
            advance(state, line)
        }
        .onEach { state = it.first }
        .map { it.second }
        .filterNotNull()
}

private fun advance(state: State, line: String): Pair<State, String?> {
    return when {
        state != State.InCodeBlock && line.firstNonSpaceCharsAre("//`") -> {
            State.InCodeBlock to "```kotlin"
        }
        state == State.InCodeBlock && line.firstNonSpaceCharsAre("//`") -> {
            State.Other to "```"
        }
        line.firstNonSpaceCharsAre("/*-") -> {
            State.InTextBlock to null
        }
        line.firstNonSpaceCharsAre("-*/") -> {
            State.Other to null
        }
        else -> {
            state to state.outputFor(line)
        }
    }
}
```

Now our processing is nice and symmetrical we can move advance to the `State`

```kotlin
sealed class State {
    fun advance(line: String): Pair<State, String?> {
        return when {
            this !is InCodeBlock && line.firstNonSpaceCharsAre("//`") -> {
                InCodeBlock to "```kotlin"
            }
            this is InCodeBlock && line.firstNonSpaceCharsAre("//`") -> {
                Other to "```"
            }
            line.firstNonSpaceCharsAre("/*-") -> {
                InTextBlock to null
            }
            line.firstNonSpaceCharsAre("-*/") -> {
                Other to null
            }
            else -> {
                this to outputFor(line)
            }
        }
    }
    abstract fun outputFor(line: String): String?

    object InCodeBlock : State() {
        override fun outputFor(line: String) = line
    }

    object InTextBlock : State() {
        override fun outputFor(line: String) = line
    }

    object Other : State() {
        override fun outputFor(line: String) = if (line.isBlank()) line else null
    }

}
```

Now we can delegate to InCodeBlock to make things, if not simpler, at least explicit.

```kotlin
sealed class State {
    open fun advance(line: String): Pair<State, String?> = when {
        line.firstNonSpaceCharsAre("//`") -> InCodeBlock to "```kotlin"
        line.firstNonSpaceCharsAre("/*-") -> InTextBlock to null
        line.firstNonSpaceCharsAre("-*/") -> Other to null
        else -> this to outputFor(line)
    }
    abstract fun outputFor(line: String): String?

    object InCodeBlock : State() {
        override fun advance(line: String) = if (line.firstNonSpaceCharsAre("//`")) Other to "```"
            else super.advance(line)
        override fun outputFor(line: String) = line
    }

    object InTextBlock : State() {
        override fun outputFor(line: String) = line
    }

    object Other : State() {
        override fun outputFor(line: String) = if (line.isBlank()) line else null
    }
}
```

To be honest I'm not entirely sure that this is better than the first formulation. That had the advantage of being transparent - you could see what is was doing. Now we have polymorphism and pairs and not-very well-named methods in the mix - they had better pull their weight if we are going to decide that this excursion was worthwhile. I wouldn't be sorry to just revert this refactor - if nothing else I've realised that the code had redundant logic that when removed would improve the old version.

Luckily it's lunchtime, so I get a natural break to allow my brain to mull over what I have done and see if it's helpful.

...

I had a chance on a long bike ride to think things through, and came up with a plan, but trying that really didn't work so I won't bore you with it. I talk things over with Alan, who has a PhD in Computer Science, and he reminds me that he always said that I should use the right tool for the job - a proper parser. Now my education was as a physicist, so I have large gaps in my knowledge of proper computing, and I know that many other programmers I respect consider that being able to devise and parse little languages is a sign of maturity. When Alan offers to pair with me to use introduce a pukka parser I figure that I'd be a fool not to take the learning opportunity, even though it will potentially delay my goal of getting a review copy of Chapter One published.

