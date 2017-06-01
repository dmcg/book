package com.oneeyedmen.book.Chapter_03_Parser

import org.parboiled.Action
import org.parboiled.BaseParser
import org.parboiled.Parboiled
import org.parboiled.Rule
import org.parboiled.annotations.BuildParseTree
import org.parboiled.parserunners.ReportingParseRunner
import org.parboiled.support.Var
import java.io.File


/*-
Now we know where `includes` are in our source, we need to process them. In order to do this we need to know how to resolve a path to its contents.
-*/

object ContextF1 {

/*-
For now, I'm just going to pass a `contentResolver` function to the render method of `Block`, which, now it is the parent of `IncludeDirective`, had better be restructured a bit
-*/

    //`
    interface Block {
        fun render(contentResolver: (String) -> List<String>): List<String>
    }

    abstract class SourceBlock() : Block {
        abstract val lines: MutableList<String>
        fun addLine(line: String) = lines.add(line)
        override fun render(contentResolver: (String) -> List<String>): List<String> = lines
    }

    data class TextBlock(override val lines: MutableList<String>) : SourceBlock() {
        constructor(vararg lines: String) : this(mutableListOf(*lines))
    }

    data class CodeBlock(override val lines: MutableList<String>) : SourceBlock() {
        constructor(vararg lines: String) : this(mutableListOf(*lines))

        override fun render(contentResolver: (String) -> List<String>) = listOf("\n```kotlin\n") + lines + "```\n\n"
    }

    data class IncludeDirective(var path: String) : Block {
        override fun render(contentResolver: (String) -> List<String>) = contentResolver(path)
    }
    //`

/*-
Now our `Translator` object can pass the contentResolver to each block's render
-*/

    //`
    object Translator {
        private val parser = Parboiled.createParser(BookParser::class.java)
        private val runner = ReportingParseRunner<Block>(parser.Root())

        fun translate(source: String, contentResolver: (String) -> List<String>): Sequence<String> = parse(source)
            .flatMap { it.render(contentResolver) }
            .asSequence()

        fun parse(source: String): List<Block> = runner.run(source).valueStack.toList().asReversed()
    }
    //`

/*-
leaving the top-level `translate(File)` function to supply an implementation of the `contentResolver`
-*/

    //`
    private fun translate(file: File): Sequence<String> =
        Translator.translate(file.readText(),
            contentResolver = { path ->
                file.parentFile.resolve(path).readLines().map { it + "\n" }
            }
        )
    //`

/*-
and yes it did take me a while to realise that I needed to re-add the newlines stripped by `File.readLines`.

<blockquote class="sidebar">
Sidebar

This wasn't helped by my originally writing every line to the output using `Writer.appendLn`. Now that the lines in a `Block` include their own newline, this meant that every line ended up with two newlines in the output, except for those in the included files, but Markdown rendering hid this well. Fixing the two newlines problem revealed the no-newlines problem.

Had I had any tests for the top-level rendering this would have been obvious. At this stage in the process though, my integration testing consists of rendering the book so far and looking at the Markdown as displayed by Markoff, a Mac renderer app.
</blockquote>

-*/

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

    private fun sourceFilesIn(dir: File) = dir
        .listFiles { file -> file.isSourceFile() }
        .toList()
        .sortedBy(File::getName)
        .asSequence()

    private fun File.isSourceFile() = isFile && !isHidden && name.endsWith(".kt")
}

/*-
After an unconscionable amount of time I'm finally in a position to render the first chapter well enough to be reviewed, so I insert the required `//#include` directives and check the rendering. There is still an issue with the Markdown renderer interpreting

````markdown
``` three quotes in code blocks
````

which occurs when we include some Markdown inside files, but Googling reveals that I can mark those blocks with four `````'s.

Of course now I have another two chapters of material generated in my attempt to render the first, so I fix those up too and package up the three Markdown files for review.
-*/