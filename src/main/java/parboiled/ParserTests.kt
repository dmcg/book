package parboiled

import com.oneeyedmen.book.approvalsRule
import org.junit.Test
import org.parboiled.*
import org.parboiled.annotations.BuildParseTree
import org.parboiled.parserunners.ReportingParseRunner
import org.parboiled.support.ParseTreeUtils
import org.parboiled.support.Var
import kotlin.test.fail




object Context1 {
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
}

object Context2 {
    class ParserTests {

        val parser = Parboiled.createParser(BookParser::class.java)

        @Test fun test() {
            val input = """
            |/*-
            |first shown
            |last shown
            |-*/
            |hidden
            """.trimMargin() + "\n"
            check(input, parser.Root());
        }

        @Test fun `matches TextBlock`() {
            val input = """
            |/*-
            |first shown
            |last shown
            |-*/
            """.trimMargin()
            check(input, parser.TextBlockRule())
        }

        @Test fun `matches TextBlock followed by newline`() {
            val input = """
            |/*-
            |first shown
            |last shown
            |-*/hidden
            """.trimMargin() + "\n"
            check(input, parser.TextBlockRule())
        }

        @Test fun `matches two TextBlocks`() {
            val input = """
            |/*-
            |first shown
            |-*/
            |/*-
            |last shown
            |-*/
            |hidden
            """.trimMargin() + "\n"
            check(input, parser.TextBlockRule())
        }

        private fun check(input: String, rule: Rule) {
            val parsingResult = ReportingParseRunner<Any>(rule).run(input)
            println(ParseTreeUtils.printNodeTree(parsingResult))
            if (!parsingResult.matched) {
                fail()
            }
        }

    }


    @BuildParseTree
    open class BookParser : BaseParser<Any>() {

        open val newline = Ch('\n')
        open val optionalNewline = Optional(newline)

        open val textBlockStart = String("/*-")
        open val textBlockEnd = String("-*/")

        open fun Root() = ZeroOrMore(FirstOf(TextBlockRule(), Line()))

        open fun TextBlockRule(): Rule {
            val block = Var<MutableList<String>>(mutableListOf())
            return Sequence(
                BlockEnter(textBlockStart),
                ZeroOrMore(LinesNotStartingWith(textBlockEnd), block.get().add(match())),
                BlockExit(textBlockEnd),
                push(TextBlock(block.get()))
            )
        }


        open fun BlockEnter(rule: Rule) = Sequence(rule, newline)
        open fun BlockExit(rule: Rule) = Sequence(rule, optionalNewline)
        open fun LinesNotStartingWith(rule: Rule) = Sequence(TestNot(rule), ZeroOrMore(NoneOf("\n")), newline)

        open fun Line() = Sequence(ZeroOrMore(NoneOf("\n")), newline)
    }
}

data class TextBlock(val lines: List<String>)


