package com.oneeyedmen.book.Chapter_03_Parser

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

/*-
Flushed with success I reorganise things and tidy up before bed.

Firstly, the top level functions are
-*/

//`
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

private fun translate(file: File): Sequence<String> = Translator.translate(file.readText())

private fun sourceFilesIn(dir: File) = dir
    .listFiles { file -> file.isSourceFile() }
    .toList()
    .sortedBy(File::getName)
    .asSequence()

private fun File.isSourceFile() = isFile && !isHidden && name.endsWith(".kt")
//`

/*-
I then pull the parser running into a `Translator` object - this allows us to lazily create the parser and runner and reuse them. Kotlin gives us other ways of achieving this aim, but I'm trying this `object` scoping out to see how it feels.
-*/

//`
object Translator {
    private val parser = Parboiled.createParser(BookParser::class.java)
    private val runner = ReportingParseRunner<Block>(parser.Root())

    fun translate(source: String): Sequence<String> = parse(source).flatMap { it.render() }.asSequence()

    fun parse(source: String): List<Block> = runner.run(source).valueStack.toList().asReversed()
}
//`

/*-
As promised I move the `render` function to a method on `Block`, with a default implementation in the interface
-*/

//`
interface Block {
    val lines: MutableList<String>
    fun addLine(line: String) = lines.add(line)
    fun render(): List<String> = lines
}

data class TextBlock(override val lines: MutableList<String>) : Block {
    constructor(vararg lines: String) : this(mutableListOf(*lines))
}

data class CodeBlock(override val lines: MutableList<String>) : Block {
    constructor(vararg lines: String) : this(mutableListOf(*lines))
    override fun render() = listOf("\n```kotlin\n") + lines + "```\n\n"
}
//`

/*-
and finally use install the Kotlin All-Open compiler plugin and configure it to make `@BuildParseTree` annotated classes open, so that we can remove the `open` modifiers from every method in the `BookParser`
-*/

//`
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

    fun <T : Block> BlockRule(startRule: Rule, endRule: Rule, accumulator: Var<T>) = Sequence(
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

    fun Root() = ZeroOrMore(
        FirstOf(
            TextBlockRule(),
            CodeBlockRule(),
            Line()))

}
//`

/*-
Now, a day's work from where we decided to try a proper parser, I am in a position to add the file include directive.

Firstly, a test for the parsing.
-*/

object ContextD1 {
    class ParserTests {

        @Test fun `single text block`() {
            check("""
                |hidden
                |/*-
                |first shown
                |last shown
                |-*/
                |hidden""",
                expected = listOf(
                    TextBlock("first shown\n", "last shown\n"))
            )
        }

        @Test fun `two text blocks`() {
            check("""
                |/*-
                |first shown
                |-*/
                |hidden
                |/*-
                |last shown
                |-*/""",
                expected = listOf(
                    TextBlock("first shown\n"),
                    TextBlock("last shown\n"))
            )
        }

        @Test fun `indented code block`() {
            check("""
                |hidden
                |    //`
                |    first shown
                |    last shown
                |    //`
                |hidden""",
                expected = listOf(
                    CodeBlock("    first shown\n", "    last shown\n"))
            )
        }

        @Test fun `mix and match`() {
            check("""
                |hidden
                |/*-
                |first text
                |last text
                |-*/
                |    //`
                |    first code
                |    last code
                |    //`
                |hidden""",
                expected = listOf(
                    TextBlock("first text\n", "last text\n"),
                    CodeBlock("    first code\n", "    last code\n")
                )
            )
        }

        //`
        @Test fun `include directive`() {
            check("""
                |hidden
                |//#include "filename1.txt"
                |//#include "filename2.txt"
                |hidden""",
                expected = listOf(
                    IncludeDirective("filename1.txt"),
                    IncludeDirective("filename2.txt")
                )
            )
        }
        //`

        private fun check(input: String, expected: List<Block>) {
            assertEquals(expected, parse(input.trimMargin()))
        }

        private fun parse(input: String) = Parser.parse(input)
    }

    object Parser  {
        private val parser = Parboiled.createParser(ContextD2.BookParser::class.java)
        private val runner = ReportingParseRunner<Block>(parser.Root())
        fun parse(source: String): List<Block> = runner.run(source)
            .valueStack
            .toList()
            .asReversed()
    }


}

/*-
We can punt on how `IncludeDirective` should behave for now
-*/

//`
data class IncludeDirective(var path: String) : Block {
    override val lines: MutableList<String>
        get() = TODO("not implemented")
}
//`

/*-
and *just* implement the parsing
-*/

object ContextD2 {
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

        fun <T : Block> BlockRule(startRule: Rule, endRule: Rule, accumulator: Var<T>) = Sequence(
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

        //`
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
        //`
    }
}

/*-
Of course it took me another hour to get `IncludeRule` working, even after discovering Parboiled's useful `TracingParseRunner`. I am beginning to internalise, if not actually understand, the interaction between `Rule`s and `Action`s - leading to the creation of `pushIncludeDirective`, so maybe I'm becoming a real programmer after all these years. Not so fast that I trust my intuition though, which is why the test has two include directives to check that I don't end up sharing state like last time.
-*/



