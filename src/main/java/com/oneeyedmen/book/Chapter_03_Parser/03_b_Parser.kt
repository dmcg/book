package com.oneeyedmen.book.Chapter_03_Parser

import com.oneeyedmen.book.Chapter_03_Parser.ContextB2.BookParser
import com.oneeyedmen.book.Chapter_03_Parser.ContextB2.BookParser.*
import org.junit.Ignore
import org.junit.Test
import org.parboiled.BaseParser
import org.parboiled.Parboiled
import org.parboiled.Rule
import org.parboiled.annotations.BuildParseTree
import org.parboiled.parserunners.ReportingParseRunner
import org.parboiled.support.Var
import kotlin.test.assertEquals

/*-
One interesting effect of the use of Parboiled is that we have introduced an intermediate form in our parsing. Previously we parsed the input and generated the output lines in one step (if you discount filtering nulls). In the new version we have built a list of `TextBlock` objects that we will later have to render into a Markdown file. This separation of concerns introduces complexity, but it also buys us flexibility and allows us to focus on one task at a time. Whilst there is a risk that we will have some difficulty with the rendering, I think it's small, so I'm going to concentrate on extending the parsing for now - adding the code block markers.

First let's add a test
-*/


object ContextB1 {

    class ParserTests {

        val parser = Parboiled.createParser(BookParser::class.java)

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

        @Ignore("code not yet written")
        //`
        @Test fun `single code block`() {
            check("""
                |hidden
                |   //`
                |   first shown
                |   last shown
                |   //`
                |hidden""",
                expected = listOf(
                    CodeBlock("first shown\n", "last shown\n"))
            )
        }
        //`

        private fun check(input: String, expected: List<Any>) {
            val parsingResult = ReportingParseRunner<Any>(parser.Root()).run(input.trimMargin())
            assertEquals(expected, parsingResult.valueStack.toList().asReversed())
        }
    }
}

/*-
and now duplicate-and-edit an implementation
-*/

object ContextB2 {

    @BuildParseTree
    open class BookParser : BaseParser<Any>() {

        open fun NewLine() = Ch('\n')
        open fun OptionalNewLine() = Optional(NewLine())
        open fun Line() = Sequence(ZeroOrMore(NoneOf("\n")), NewLine())

        //`
        interface Block {
            val lines: MutableList<String>
            fun addLine(line: String) = lines.add(line)
        }

        data class TextBlock(override val lines: MutableList<String>) : Block {
            constructor(vararg lines: String) : this(mutableListOf(*lines))
        }

        data class CodeBlock(override val lines: MutableList<String>) : Block {
            constructor(vararg lines: String) : this(mutableListOf(*lines))
        }

        open fun TextBlockRule(): Rule {
            val block = Var<TextBlock>(TextBlock())
            return Sequence(
                Sequence(String("/*-"), NewLine()),
                ZeroOrMore(
                    Sequence(
                        TestNot(String("-*/")),
                        ZeroOrMore(NoneOf("\n")),
                        NewLine()),
                    block.get().addLine(match())),
                Sequence(String("-*/"), OptionalNewLine()),
                push(block.get())
            )
        }

        open fun CodeBlockRule(): Rule {
            val block = Var<CodeBlock>(CodeBlock())
            return Sequence(
                Sequence(String("//`"), NewLine()),
                ZeroOrMore(
                    Sequence(
                        TestNot(String("//`")),
                        ZeroOrMore(NoneOf("\n")),
                        NewLine()),
                    block.get().addLine(match())),
                Sequence(String("//`"), OptionalNewLine()),
                push(block.get())
            )
        }

        open fun Root() = ZeroOrMore(
            FirstOf(
                TextBlockRule(),
                CodeBlockRule(),
                Line()))

        //`
    }
}

/*-
Unfortunately this fails, and a little thought reveals that it's because our block-matching code only works for block markers at the start of a line. The previous behaviour was that blocks were defined by markers as the first non-space characters. My instinct here is to back up and extract some reusable Rule from `TextBlockRule`, and then use that to implement `CodeBlockRule`. Otherwise I'm going to have diverging Rules that I have to try to find the commonality in.

So, reverting to our two tests for the text block, we refactor the parser and define `CodeBlockRule` in much the same way as text `TextBlockRule`.
-*/

object ContextB3 {
    class ParserTests {

        val parser = Parboiled.createParser(BookParser::class.java)

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

        @Test fun `single code block`() {
            check("""
                |hidden
                |//`
                |first shown
                |last shown
                |//`
                |hidden""",
                expected = listOf(
                    CodeBlock("first shown\n", "last shown\n"))
            )
        }

        @Test fun `mix and match`() {
            check("""
                |hidden
                |/*-
                |first text
                |last text
                |-*/
                |//`
                |first code
                |last code
                |//`
                |hidden""",
                expected = listOf(
                    TextBlock("first text\n", "last text\n"),
                    CodeBlock("first code\n", "last code\n")
                )
            )
        }

        private fun check(input: String, expected: List<Any>) {
            assertEquals(expected, parse(input.trimMargin()))
        }

        private fun parse(input: String) = ReportingParseRunner<Any>(parser.Root()).run(input).valueStack.toList().asReversed()
    }

    @BuildParseTree
    open class BookParser : BaseParser<Any>() {

        open fun NewLine() = Ch('\n')
        open fun OptionalNewLine() = Optional(NewLine())
        open fun Line() = Sequence(ZeroOrMore(NoneOf("\n")), NewLine())

        //`
        open fun TextBlockRule(): Rule {
            val result = Var(TextBlock())
            return BlockRule(String("/*-"), String("-*/"), result)
        }

        open fun CodeBlockRule(): Rule {
            val result = Var(CodeBlock())
            return BlockRule(String("//`"), String("//`"), result)
        }

        open fun <T: Block> BlockRule(startRule: Rule, endRule: Rule, accumulator: Var<T>) = Sequence(
            BlockEnter(startRule),
            ZeroOrMore(
                LinesNotStartingWith(endRule),
                accumulator.get().addLine(match())),
            BlockExit(endRule),
            push(accumulator.get())
        )

        open fun LinesNotStartingWith(rule: Rule) = Sequence(
            TestNot(rule),
            ZeroOrMore(NoneOf("\n")),
            NewLine())

        open fun BlockEnter(rule: Rule) = Sequence(rule, NewLine())
        open fun BlockExit(rule: Rule) = Sequence(rule, OptionalNewLine())
        //`

        open fun Root() = ZeroOrMore(
            FirstOf(
                TextBlockRule(),
                CodeBlockRule(),
                Line()))

    }
}

/*-
I test this with an unindented code block, and with mixed text and code blocks, before turning to indented blocks.
-*/

object ContextB4 {

    //`
    class ParserTests {

        val parser = Parboiled.createParser(BookParser::class.java)

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

        private fun check(input: String, expected: List<Any>) {
            assertEquals(expected, parse(input.trimMargin()))
        }

        private fun parse(input: String) = ReportingParseRunner<Any>(parser.Root()).run(input).valueStack.toList().asReversed()
    }

    interface Block {
        val lines: MutableList<String>
        fun addLine(line: String) = lines.add(line)
    }

    data class TextBlock(override val lines: MutableList<String>) : Block {
        constructor(vararg lines: String) : this(mutableListOf(*lines))
    }

    data class CodeBlock(override val lines: MutableList<String>) : Block {
        constructor(vararg lines: String) : this(mutableListOf(*lines))
    }

    @BuildParseTree
    open class BookParser : BaseParser<Any>() {

        open fun NewLine() = Ch('\n')
        open fun OptionalNewLine() = Optional(NewLine())
        open fun WhiteSpace() = AnyOf(" \t")
        open fun OptionalWhiteSpace() = ZeroOrMore(WhiteSpace())
        open fun Line() = Sequence(ZeroOrMore(NoneOf("\n")), NewLine())

        open fun TextBlockRule(): Rule {
            val result = Var(TextBlock())
            return BlockRule(String("/*-"), String("-*/"), result)
        }

        open fun CodeBlockRule(): Rule {
            val result = Var(CodeBlock())
            return BlockRule(String("//`"), String("//`"), result)
        }

        open fun <T: Block> BlockRule(startRule: Rule, endRule: Rule, accumulator: Var<T>) = Sequence(
            BlockEnter(startRule),
            ZeroOrMore(
                LinesNotStartingWith(endRule),
                accumulator.get().addLine(match())),
            BlockExit(endRule),
            push(accumulator.get())
        )

        open fun LinesNotStartingWith(rule: Rule) = Sequence(
            OptionalWhiteSpace(),
            TestNot(rule),
            ZeroOrMore(NoneOf("\n")),
            NewLine())

        open fun BlockEnter(rule: Rule) = Sequence(OptionalWhiteSpace(), rule, NewLine())
        open fun BlockExit(rule: Rule) = Sequence(OptionalWhiteSpace(), rule, OptionalNewLine())

        open fun Root() = ZeroOrMore(
            FirstOf(
                TextBlockRule(),
                CodeBlockRule(),
                Line()))

    }
    //`
}

/*-
Well that went surprisingly well, in that it passes the tests and is pretty understandable as a parser. The main wrinkle is that I don't seem to be able to inline the

```kotlin
val result = Var(CodeBlock())
```

expressions - this must be something to do with the aggressive bytecode manipulation that Parboiled is performing. I expect that I will either understand this or be bitten by it again before the end of this project.

I'm a little dissatisfied by the fact that the lines in the `Block` objects contain the line separators and indents, but it's easier to throw away information than try to reconstruct it, so reason that it should stay at least for now. At least the tests document the behaviour.
-*/


