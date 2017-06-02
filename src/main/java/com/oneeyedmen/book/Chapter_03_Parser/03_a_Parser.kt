package com.oneeyedmen.book.Chapter_03_Parser

import com.oneeyedmen.book.Chapter_03_Parser.ContextA4.BookParser
import com.oneeyedmen.book.Chapter_03_Parser.ContextA4.TextBlock
import com.oneeyedmen.book.approvalsRule
import org.junit.Test
import org.parboiled.BaseParser
import org.parboiled.Parboiled
import org.parboiled.Rule
import org.parboiled.annotations.BuildParseTree
import org.parboiled.parserunners.ReportingParseRunner
import org.parboiled.support.ParseTreeUtils
import org.parboiled.support.Var
import kotlin.test.assertEquals
import kotlin.test.fail

/*-
The story so far. I have an-almost functioning parser for my prose-in-code format, but it keeps on collapsing under its own weight. I'd like to ignore this and get on with publishing the first chapter, but I can't see how to add the features that I need to make this a reality without being left with an irredeemable mess. At this point I'm offered some help to show me how to use a grown-up parser, and I leap at the chance.

Alan and I go looking for parser libraries for Java. ANTLR (ANother Tool for Language Recognition) is the standard, but it is a parser generator - meaning that it generates Java source files that you have to compile. For a large project that might be acceptable, but here, its a build step too far.

Google's fifth result is Parboiled. Parboiled allows us to specify our format in Java code, and make use of it without code generation, so it looks like a good bet. It also has a functioning Markdown parser as one of its examples, which feels like a good portent. We tell Gradle about it's JAR file and convert a simple calculator example to Kotlin to see if it runs at all.
-*/

object Context1 {

    //`
    class ParserTests {

        @org.junit.Rule @JvmField val approver = approvalsRule()

        @Test fun test() {
            val parser = Parboiled.createParser(CalculatorParser::class.java)
            val result = ReportingParseRunner<Any>(parser.Expression()).run("1+2");
            approver.assertApproved(ParseTreeUtils.printNodeTree(result));
        }
    }

    @BuildParseTree
    open class CalculatorParser : BaseParser<Any>() {

        open fun Expression(): Rule {
            return Sequence(
                Term(),
                ZeroOrMore(AnyOf("+-"), Term())
            )
        }

        open fun Term(): Rule {
            return Sequence(
                Factor(),
                ZeroOrMore(AnyOf("*/"), Factor())
            )
        }

        open fun Factor(): Rule {
            return FirstOf(
                Number(),
                Sequence('(', Expression(), ')')
            )
        }

        open fun Number(): Rule {
            return OneOrMore(CharRange('0', '9'))
        }
    }
    //`
}

/*-
The only change we had to make from IntelliJ's conversion of the Java to Kotlin was to add the `open` keyword, as it turns out that Parboiled makes use of bytecode generation to enhance our parser class.

We used another Approvals Test here so that we could record the output from `parseNodeTree` for your benefit - it looks like this

~~~text
-*/
//#include "Context1.ParserTests_test.approved"
/*-
~~~
which is a representation of the `Rule`s that matched the parsed text `1+2`.

So far so good.

Now to try a subset of the book format.
-*/

object Context2 {

    //`
    class ParserTests {

        val parser = Parboiled.createParser(Context3.BookParser::class.java) // TODO - remove Context3

        @Test fun test() {
            check("""
            |hidden
            |/*-
            |first shown
            |last shown
            |-*/
            |hidden
            """);
        }

        private fun check(input: String) {
            val parsingResult = ReportingParseRunner<Any>(parser.Root()).run(input.trimMargin())
            println(ParseTreeUtils.printNodeTree(parsingResult))
            if (!parsingResult.matched) {
                fail()
            }
        }
    }
    //`
}

/*-
Full disclosure - this test was the end result of at least a couple of hours of iterating in on a solution as we gained understanding of how Parboiled works, and tried to find a way of having the tests tell us how well we were doing. The `println` is a sure sign that we're really only using JUnit as way of exploring an API rather than writing TDD or regression tests.

 What does the parser look like?
-*/

object Context3 {
    //`
    @BuildParseTree
    open class BookParser : BaseParser<Any>() {

        open fun NewLine() = Ch('\n')
        open fun OptionalNewLine() = Optional(NewLine())
        open fun Line() = Sequence(ZeroOrMore(NoneOf("\n")), NewLine())

        open fun TextBlock(): Rule {
            return Sequence(
                Sequence(String("/*-"), NewLine()),
                ZeroOrMore(
                    Sequence(
                        TestNot(String("-*/")),
                        ZeroOrMore(NoneOf("\n")),
                        NewLine())),
                Sequence(String("-*/"), OptionalNewLine())
            )
        }

        open fun Root() = ZeroOrMore(FirstOf(TextBlock(), Line()))
    }
    //`
}

/*-
I won't take this opportunity to explain PEG parsers, largely because you already know that at the time of writing I have no more than 4 hours experience. It's a selling point of Parboiled that you can probably read the code and have a good idea how it works. That isn't to say that we just rattled this out - it took a good 3 hours of experiment and blind alleys, even pairing with a trained computer scientist - and I'm still not convinced that it will work in all real circumstances. In order to find out we're going to need a better way of seeing what matches.

Reading the documentation shows that we can add `Actions` in `Sequence`s to capture results, which are added to a stack that is made available in the `parsingResult`. Let's define a `TextBlock` class to hold the lines that we're interested in and ask Parboiled to push one on the stack when when the result is available.
-*/

object ContextA4 {
    //`
    data class TextBlock(val lines: MutableList<String>) {
        constructor(vararg lines: String) : this(mutableListOf(*lines))

        fun addLine(line: String) = lines.add(line)
    }
    //`

    @BuildParseTree
    open class BookParser : BaseParser<Any>() {

        open fun NewLine() = Ch('\n')
        open fun OptionalNewLine() = Optional(NewLine())
        open fun Line() = Sequence(ZeroOrMore(NoneOf("\n")), NewLine())

        //`
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
        //`

        open fun Root() = ZeroOrMore(FirstOf(TextBlockRule(), Line()))
    }
}

/*-
Now our test can access the result stack and actually make assertions. As `TextBlock` is a `data` class we get a working definition of `equals` for free.
-*/

object ContextA5 {

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

        private fun check(input: String, expected: List<TextBlock>) {
            val parsingResult = ReportingParseRunner<Any>(parser.Root()).run(input.trimMargin())
            if (!parsingResult.matched) {
                fail()
            }
            assertEquals(expected, parsingResult.valueStack.toList().asReversed())
        }
    }
    //`
}

/*-
This obviously isn't perfect, but it is a promising start. Parboiled is evidently working very hard behind the scenes to make this Rule-as-function which is then interpreted in context work in Java - this worries me a bit, but I'm prepared to suspend disbelief for now.
-*/



