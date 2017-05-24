package com.oneeyedmen.book.Chapter_02_Entrenchment

import com.oneeyedmen.book.Chapter_01_Spike.ContextD1.firstNonSpaceCharsAre
import com.oneeyedmen.book.approvalsRule
import org.junit.Rule
import org.junit.Test

/*-
Deciding to go on is all very well, but I don't think I'm much closer to knowing how to implement file inclusion. In these circumstances I've learned to just play with code until inspiration strikes. In particular - refactor until where the new feature should live is obvious.

We shouldn't refactor code that doesn't work though, so let's back-out our last change and remove the inclusion test to see what we have.
-*/

object ContextG1 {

    //`
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
    //`
}

/*-
I never was happy with the `translate` method - let's refactor to see what comes out. I don't believe that we can be in a code block and a text block at the same time, but the code doesn't express this. I'm going to replace the two variables with one, first by replacing inCodeBlock with an object. Note that each one of these steps keeps the tests passing.
-*/

object ContextG2 {

    //`
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
    //`

    class CodeExtractorTests : AbstractCodeExtractorTests(ContextG2::translate)
}

/*-
Then inTextBlock. It turns out that we don't really care if we are in a text block or not when we see it's end markers, so we can also simplify the when clauses and keep the tests passing.
-*/

object ContextG3 {

    //`
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
    //`

    class CodeExtractorTests : AbstractCodeExtractorTests(ContextG3::translate)
}

/*-
and then replace null with Other and create a State class
-*/

object ContextG4 {

    //`
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
    //`

    class CodeExtractorTests : AbstractCodeExtractorTests(ContextG4::translate)
}

/*-
Now I can delegate what happens to non-transition lines to the State
-*/

object ContextG5 {

    //`
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
    //`

    class CodeExtractorTests : AbstractCodeExtractorTests(ContextG5::translate)
}

/*-
Now some lines cause the state to change, and some don't. I'm going to pull out a method called `advance` that returns the next state and the string that should be output.
-*/

object ContextG6 {

    //`
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
    //`

    class CodeExtractorTests : AbstractCodeExtractorTests(ContextG6::translate)
}

/*-
Now our processing is nice and symmetrical we can move advance to the `State`
-*/

object ContextG7 {

    //`
    sealed class State {
        fun advance(line: String): Pair<State, String?> {
            return when {
                this !is State.InCodeBlock && line.firstNonSpaceCharsAre("//`") -> {
                    State.InCodeBlock to "```kotlin"
                }
                this is State.InCodeBlock && line.firstNonSpaceCharsAre("//`") -> {
                    State.Other to "```"
                }
                line.firstNonSpaceCharsAre("/*-") -> {
                    State.InTextBlock to null
                }
                line.firstNonSpaceCharsAre("-*/") -> {
                    State.Other to null
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
    //`

    fun translate(sourceLines: String): Sequence<String> {
        var state: State = State.Other
        return sourceLines.splitToSequence("\n")
            .map { line ->
                state.advance(line)
            }
            .onEach { state = it.first }
            .map { it.second }
            .filterNotNull()
    }

    class CodeExtractorTests : AbstractCodeExtractorTests(ContextG7::translate)
}

/*-
Now we can delegate to InCodeBlock to make things, if not simpler, at least explicit.
-*/

object ContextG8 {

    //`
    sealed class State {
        open fun advance(line: String): Pair<State, String?> = when {
            line.firstNonSpaceCharsAre("//`") -> State.InCodeBlock to "```kotlin"
            line.firstNonSpaceCharsAre("/*-") -> State.InTextBlock to null
            line.firstNonSpaceCharsAre("-*/") -> State.Other to null
            else -> this to outputFor(line)
        }
        abstract fun outputFor(line: String): String?

        object InCodeBlock : State() {
            override fun advance(line: String) = if (line.firstNonSpaceCharsAre("//`")) State.Other to "```"
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
    //`

    fun translate(sourceLines: String): Sequence<String> {
        var state: State = State.Other
        return sourceLines.splitToSequence("\n")
            .map { line ->
                state.advance(line)
            }
            .onEach { state = it.first }
            .map { it.second }
            .filterNotNull()
    }

    class CodeExtractorTests : AbstractCodeExtractorTests(ContextG8::translate)
}

/*-
To be honest I'm not entirely sure that this is better than the first formulation. That had the advantage of being transparent - you could see what is was doing. Now we have polymorphism and pairs and not-very well-named methods in the mix - they had better pull their weight if we are going to decide that this excursion was worthwhile. I wouldn't be sorry to just revert this refactor - if nothing else I've realised that the code had redundant logic that when removed would improve the old version.

Luckily it's lunchtime, so I get a natural break to allow my brain to mull over what I have done and see if it's helpful.
-*/




abstract class AbstractCodeExtractorTests(private val method: (String) -> Sequence<String>) {

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
        approver.assertApproved(method(source.trimMargin()).joinToString("\n"))
    }
}
