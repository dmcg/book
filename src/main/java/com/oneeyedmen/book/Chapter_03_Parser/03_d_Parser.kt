package com.oneeyedmen.book.Chapter_03_Parser

import org.parboiled.BaseParser
import org.parboiled.Parboiled
import org.parboiled.Rule
import org.parboiled.annotations.BuildParseTree
import org.parboiled.parserunners.ReportingParseRunner
import org.parboiled.support.Var
import java.io.File

/*-
Flushed with success I reorganise things and tidy up before bed.

Firstly, the top level functions are
-*/

object ContextD1 {
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
}



