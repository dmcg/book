package com.oneeyedmen.book.Chapter_03_Parser

import com.oneeyedmen.book.Chapter_03_Parser.ContextD1.Block
import com.oneeyedmen.book.Chapter_03_Parser.ContextD1.CodeBlock
import com.oneeyedmen.book.Chapter_03_Parser.ContextD1.TextBlock
import org.junit.Test
import org.parboiled.Action
import org.parboiled.BaseParser
import org.parboiled.Parboiled
import org.parboiled.Rule
import org.parboiled.annotations.BuildParseTree
import org.parboiled.parserunners.ReportingParseRunner
import org.parboiled.support.Var
import kotlin.reflect.KClass
import kotlin.test.assertEquals


/*-
Now, a day's work from where we decided to try a proper parser, I am in a position to add the file include directive.

Firstly, a test for the parsing.
-*/

object ContextE1 {
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
            assertEquals(expected, parser.parse(input.trimMargin()))
        }

        val parser: Parser<*> = Parser(BookParser::class, BookParser::Root)
    }

    class Parser<T: BaseParser<Block>>(parserClass: KClass<T>, rootRule: (T) -> Rule) {
        private val parser = Parboiled.createParser<T, Block>(parserClass.java)
        private val runner = ReportingParseRunner<Block>(rootRule(parser))
        fun parse(source: String): List<Block> = runner.run(source)
            .valueStack
            .toList()
            .asReversed()
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

    @BuildParseTree
    class BookParser : BaseParser<Block>() {

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
