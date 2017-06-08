package com.oneeyedmen.book.Chapter_06_Unindent_Source

import com.oneeyedmen.book.AllOpen
import com.oneeyedmen.book.Chapter_03_Parser.ContextF1
import com.oneeyedmen.book.Chapter_06_Unindent_Source.ContextA7.SourceBlock
import org.junit.Ignore
import org.junit.Test
import org.parboiled.Action
import org.parboiled.BaseParser
import org.parboiled.Parboiled
import org.parboiled.Rule
import org.parboiled.annotations.BuildParseTree
import org.parboiled.parserunners.ReportingParseRunner
import org.parboiled.support.Var
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.fail


/*-
# Chapter 6 - Unindent Source

I find that predicting what the consequences of a functional change in to a codebase are a good bellwether. A healthy codebase is one in which changes don't propagate far from their root - if I have to touch more than a single class to change this behaviour I would question how well factored the code is. In addition, predicting and then testing our prediction allows us to gauge how well we understand what we have built.

As we have a clean separation between parsing and rendering, `CodeBlock` is the only production code that should have to change. The tests are a different story. The only tests we have for the render methods are currently the badly named `CodeExtractorTests` - effectively an integration test of parsing and rendering, but not currently updated for the `#include` directive processing.

In my excitement to get an actual PDF published I'd overlooked this deficit. One of the main arguments in favour of a separate QA function in a software team is to reign in the natural optimism of developers, and in this case I've accumulated a little technical debt. Should we pay it down, or get on with making progress?

This is one of those situations where it often depends on the codebase and your relationship with your customer. If the codebase has a lot of debt, and it might be difficult to take time after shipping this feature to re-balance things, I'd definitely add some tests now. It is easier to ask forgiveness than permission, and even easier if you have to do neither because you invisibly do the right thing.

In this case though, the relationship between customer and developer is pretty good. I appreciate my desire to publish more readable code as soon as possible, and trust myself to give me permission to pay down debt when it starts affecting progress. So let's crack on.

Let's at least add some rendering tests first though.
-*/

object ContextA1 {

    //`
    class SourceBlockRenderingTests {

        val nullRenderingContext: (String) -> List<String> = {
            fail("Shouldn't need to use the rendering context")
        }

        @Test fun `A TextBlock renders as its lines`() {
            assertEquals(listOf(
                "line1\n",
                "line2\n"),
                TextBlock("line1\n", "line2\n").render(nullRenderingContext))
        }

        @Test fun `A CodeBlock renders as a Markdown codeblock`() {
            assertEquals(listOf(
                "\n```kotlin\n",
                "line1\n",
                "line2\n",
                "```\n\n"),
                CodeBlock("line1\n", "line2\n").render(nullRenderingContext))
        }
    }
    //`

    private fun CodeBlock(vararg lines: String) = ContextF1.CodeBlock(*lines)
    private fun TextBlock(vararg lines: String) = ContextF1.TextBlock(*lines)
}

/*-
Oh that's interesting, the CodeBlock's newlines really aren't in the place that I expect. Lets fix that first - it at very least violates the Principle of Least Surprise.
-*/

object ContextA2 {

    @AllOpen
    class SourceBlockRenderingTests {

        val nullRenderingContext: (String) -> List<String> = {
            fail("Shouldn't need to use the rendering context")
        }

        fun CodeBlock(vararg lines: String): ContextA7.Block = ContextA2.CodeBlock(*lines)


        //`
        @Test fun `A CodeBlock renders as a Markdown codeblock`() {
            assertEquals(listOf(
                "\n",
                "```kotlin\n",
                "line1\n",
                "line2\n",
                "```\n",
                "\n"),
                CodeBlock("line1\n", "line2\n").render(nullRenderingContext))
        }
    }

    data class CodeBlock(override val lines: MutableList<String>) : ContextA7.SourceBlock() {
        constructor(vararg lines: String) : this(mutableListOf(*lines))

        override fun render(contentResolver: (String) -> List<String>) = listOf("\n", "```kotlin\n") + lines + listOf("```\n", "\n")
    }
    //`
}

/*~
The more I look at that, the more I dislike the fact that we are dealing with lines with the newline still attached, maybe I'll get around to addressing that the next time we're in the parser.

Now I go to write a test for the indentation removal, and immediately notice that the source above is an example of code where I would like to see the first function indented so that the context of the class definition is clear. So let's just go straight to making that work.
~*/

object ContextA3 {


    class SourceBlockRenderingTests : ContextA2.SourceBlockRenderingTests() {


        override fun CodeBlock(vararg lines: String): SourceBlock = ContextA3.CodeBlock(*lines)

        //`
        @Test fun `A CodeBlock renders as a Markdown codeblock with the minimum indent removed`() {
            assertEquals(listOf(
                "\n",
                "```kotlin\n",
                "    line1\n",
                "line2\n",
                "        line3\n",
                "```\n",
                "\n"),
                CodeBlock(
                    "        line1\n",
                    "    line2\n",
                    "            line3\n"
                ).render(nullRenderingContext))
        }
    }

    data class CodeBlock(override val lines: MutableList<String>) : SourceBlock() {
        constructor(vararg lines: String) : this(mutableListOf(*lines))

        override fun render(contentResolver: (String) -> List<String>) =
            listOf("\n", "```kotlin\n") +
                lines.toLinesWithMinimumIndentRemoved() +
                listOf("```\n", "\n")
    }

    fun List<String>.toLinesWithMinimumIndentRemoved(): List<String> {
        val minimumIndent = this.map { it.indentLength }.min() ?: 0
        return this.map { it.substring(minimumIndent) }
    }

    val String.indentLength get() = this.indexOfFirst { !it.isWhitespace() }
    //`
}
/*-
That's reasonably straightforward, and reads nicely because of our extension methods. Another Kotlin subtlety is hidden in the definition of `minimumIndent`. `Iterable.min()` will return `null` if there are no items in the list - in the same situation `Collections.min()` throws `NoSuchElementException`. Without Kotlin's insistence that I couldn't call `String.substring()` with a `Int?` (nullable Int) there would have been a failure if I ever tried to render an empty `CodeBlock`. That's the sort of thing that's embarrassing, and I can't put my hand on my heart and say that I was planning to write a test for the empty case. As it is, I wrestle with my engineering judgement and conclude that it isn't worth adding, but I'd be open to persuasion if my pair felt strongly.

Now I run the code on the source of this book, and my smugness at the type-system obviating the need for tests turns out to be a little optimistic. I get `String index out of range: -1` invoking `substring()` in `toLinesWithMinimumIndentRemoved()`. Hah - Kotlin `indexOfFirst()` returns `-1` and not null when the predicate doesn't match any item. I know that -1 is an established convention for not found, but really Kotlin, it looks like you're just trying to embarrass me.

The problem is lines that are only whitespace, or, I suppose, have no characters at all. Let's add some tests for those, and now we've bitten the bullet of multiple tests, one for the empty code block.
-*/

object ContextA4 {

    @Ignore("instructive failure")
    class SourceBlockRenderingTests : ContextA3.SourceBlockRenderingTests() {

        override fun CodeBlock(vararg lines: String): SourceBlock = ContextA4.CodeBlock(*lines)

        //`
        @Test fun `An empty CodeBlock renders OK`() {
            assertEquals(listOf(
                "\n",
                "```kotlin\n",
                "```\n",
                "\n"),
                CodeBlock().render(nullRenderingContext))
        }

        @Test fun `A CodeBlock with blank lines renders OK`() {
            assertEquals(listOf(
                "\n",
                "```kotlin\n",
                "\n",
                "line1\n",
                "    line2\n",
                "```\n",
                "\n"),
                CodeBlock(
                    "\n",
                    "    line1\n",
                    "        line2\n"
                ).render(nullRenderingContext))
        }
        //`
    }

/*-
The indent for a blank line is obviously 0 not -1, so lets force that.
-*/
    //`
    val String.indentLength get() = Math.max(0, this.indexOfFirst { !it.isWhitespace() })
    //`

    data class CodeBlock(override val lines: MutableList<String>) : SourceBlock() {
        constructor(vararg lines: String) : this(mutableListOf(*lines))

        override fun render(contentResolver: (String) -> List<String>) =
            listOf("\n", "```kotlin\n") +
                lines.toLinesWithMinimumIndentRemoved() +
                listOf("```\n", "\n")
    }

    fun List<String>.toLinesWithMinimumIndentRemoved(): List<String> {
        val minimumIndent = this.map { it.indentLength }.min() ?: 0
        return this.map { it.substring(minimumIndent) }
    }
}

/*-
Oh good grief, my test still fails, because now the minimum indent is 0, no indent is removed from any line. This is making me look like a fool.

OK, new strategy - any line that is entirely whitespace gets an indent of null. Then I'll remove those nulls from the list before taking the min. I'd better beef up the test a bit first.
-*/

object ContextA5 {

    @Ignore("fails instructive failure")
    class SourceBlockRenderingTests : ContextA4.SourceBlockRenderingTests() {

        override fun CodeBlock(vararg lines: String): SourceBlock = ContextA5.CodeBlock(*lines)

        override
        //`
        @Test fun `A CodeBlock with blank lines renders OK`() {
            assertEquals(listOf(
                "\n",
                "```kotlin\n",
                "\n",
                "line1\n",
                "    line2\n",
                "    \n",
                "```\n",
                "\n"),
                CodeBlock(
                    "\n",
                    "    line1\n",
                    "        line2\n",
                    "        \n"
                ).render(nullRenderingContext))
        }
        //`
    }

/*-
And now the implementation is
-*/
    //`

    fun List<String>.toLinesWithMinimumIndentRemoved(): List<String> {
        val minimumIndent = this.map { it.indentLength }.filterNotNull().min() ?: 0
        return this.map { it.substring(minimumIndent) }
    }

    val String.indentLength get() : Int? =
        when {
            this.isNullOrBlank() -> null
            else -> Math.max(0, this.indexOfFirst { !it.isWhitespace() })
        }
    //`

    data class CodeBlock(override val lines: MutableList<String>) : SourceBlock() {
        constructor(vararg lines: String) : this(mutableListOf(*lines))

        override fun render(contentResolver: (String) -> List<String>) =
            listOf("\n", "```kotlin\n") +
                lines.toLinesWithMinimumIndentRemoved() +
                listOf("```\n", "\n")
    }

}

/*-
I run that and

`java.lang.StringIndexOutOfBoundsException: String index out of range: -3`

Gah. Now we're trying to remove the first four characters from the line that is just `\n`. OK, last chance to look like a professional programmer.
-*/

object ContextA6 {

    class SourceBlockRenderingTests : ContextA5.SourceBlockRenderingTests() {

        override fun CodeBlock(vararg lines: String) = ContextA7.CodeBlock(*lines)

    }
}

//`
fun List<String>.toLinesWithMinimumIndentRemoved(): List<String> {
    val minimumIndent = this.map { it.indentLength }.filterNotNull().min() ?: 0
    return this.map { it.substringIfPossible(minimumIndent) }
}

val String.indentLength get() : Int? =
when {
    this.isNullOrBlank() -> null
    else -> Math.max(0, this.indexOfFirst { !it.isWhitespace() })
}

fun String.substringIfPossible(startIndex: Int) =
    if (startIndex > length)
        this
    else
        this.substring(startIndex)
//`


object ContextA7 {
    interface Block {
        fun render(contentResolver: (String) -> List<String>): List<String>
    }

    abstract class SourceBlock() : Block {
        abstract val lines: MutableList<String>
        fun addLine(line: String) = lines.add(line)
        override fun render(contentResolver: (String) -> List<String>): List<String> = lines
    }

    data class CodeBlock(override val lines: MutableList<String>) : SourceBlock() {
        constructor(vararg lines: String) : this(mutableListOf(*lines))

        override fun render(contentResolver: (String) -> List<String>) =
            listOf("\n", "```kotlin\n") +
                lines.toLinesWithMinimumIndentRemoved() +
                listOf("```\n", "\n")
    }

    data class TextBlock(override val lines: MutableList<String>) : SourceBlock() {
        constructor(vararg lines: String) : this(mutableListOf(*lines))
    }

    data class IncludeDirective(var path: String) : Block {
        override fun render(contentResolver: (String) -> List<String>) = contentResolver(path)
    }

    object Translator {
        private val parser = Parboiled.createParser(BookParser::class.java)
        private val runner = ReportingParseRunner<Block>(parser.Root())

        fun translate(source: String, contentResolver: (String) -> List<String>): Sequence<String> = parse(source)
            .flatMap { it.render(contentResolver) }
            .asSequence()

        fun parse(source: String): List<Block> = runner.run(source).valueStack.toList().asReversed()
    }

    fun translate(file: File): Sequence<String> =
        Translator.translate(file.readText(),
            contentResolver = { path ->
                file.parentFile.resolve(path).readLines().map { it + "\n" }
            }
        )

    @BuildParseTree
    class BookParser : BaseParser<Any>() {

        fun NewLine() = Ch('\n')
        fun OptionalNewLine() = Optional(NewLine())
        fun WhiteSpace() = AnyOf(" \t")
        fun OptionalWhiteSpace() = ZeroOrMore(WhiteSpace())
        fun Line() = Sequence(ZeroOrMore(NoneOf("\n")), NewLine())

        fun TextBlockRule(): Rule {
            val result = Var(TextBlock())
            return BlockRule(String("/*-"), String("-*/"), result)
        }

        fun CodeBlockRule(): Rule {
            val result = Var(CodeBlock())
            return BlockRule(String("//`"), String("//`"), result)
        }

        fun <T : SourceBlock> BlockRule(startRule: Rule, endRule: Rule, accumulator: Var<T>) = Sequence(
            BlockEnter(startRule),
            ZeroOrMore(
                LinesNotStartingWith(endRule),
                accumulator.get().addLine(match())),
            BlockExit(endRule),
            push(accumulator.get())
        )

        fun LinesNotStartingWith(rule: Rule) = Sequence(
            OptionalWhiteSpace(),
            TestNot(rule),
            ZeroOrMore(NoneOf("\n")),
            NewLine())

        fun BlockEnter(rule: Rule) = Sequence(OptionalWhiteSpace(), rule, NewLine())
        fun BlockExit(rule: Rule) = Sequence(OptionalWhiteSpace(), rule, OptionalNewLine())

        fun IncludeRule(): Rule {
            return Sequence(
                OptionalWhiteSpace(),
                String("//#include "),
                OptionalWhiteSpace(),
                Ch('"'),
                ZeroOrMore(NoneOf("\"")),
                pushIncludeDirective,
                Ch('"'),
                OptionalWhiteSpace(),
                NewLine()
            )
        }

        private val pushIncludeDirective = Action<Any> { context ->
            context.valueStack.push(IncludeDirective(context.match))
            true
        }

        fun Root() = ZeroOrMore(
            FirstOf(
                IncludeRule(),
                TextBlockRule(),
                CodeBlockRule(),
                Line()))

    }

    fun main(args: Array<String>) {
        val srcDir = File(args[0])
        val outFile = File(args[1]).apply {
            absoluteFile.parentFile.mkdirs()
        }

        val translatedLines: Sequence<String> = sourceFilesIn(srcDir)
            .flatMap { translate(it).plus("\n") }

        outFile.bufferedWriter(Charsets.UTF_8).use { writer ->
            translatedLines.forEach {
                writer.append(it)
            }
        }
    }

    fun sourceFilesIn(dir: File) = dir
        .listFiles { file -> file.isSourceFile() }
        .toList()
        .sortedBy(File::getName)
        .asSequence()

    fun File.isSourceFile() = isFile && !isHidden && name.endsWith(".kt")
}

/*-
Well it's 23:40 here, so I'm glad to say that not only passes, but runs successfully on the entire book so far.

This has been another humbling episode. I have tried as far as is sensible to show the process of coding in real life, as so many books skip to "here's one I prepared earlier." If you also occasionally have trouble implementing an at-first-glance simple feature take heart that I have worked with many excellent developers who would not, at the very least, have spotted the pitfalls here until the tests failed. The code makes a monkey out of the best of us, which is why tests and an enquiring, even sceptical, mind are so useful.

Are we done? To be honest I'm not convinced that I've considered all the corner cases here. It really wouldn't surprise me to find a formatting glitch in the book and trace it back here, nor to end up with another IndexOutOfBoundsException. If this code were running in an unattended system, or anywhere that I might get a late-night call to fix, I would want to make sure that another pair of eyes had reviewed the code and tests. As it is, the consequences of failure are just that I will have to context switch back into this code and debug it. Whilst the cost of that context switch is easy to underestimate; this is engineering, and it's my job to judge the trade-offs. In this case, it's a chance I'm prepared to take.
-*/





