The story so far. I have an-almost functioning parser for my prose-in-code format, but it keeps on collapsing under its own weight. I'd like to ignore this and get on with publishing the first chapter, but I can't see how to add the features that I need to make this a reality without being left with an irredeemable mess. At this point I'm offered some help to show me how to use a grown-up parser, and I leap at the chance.

Alan and I go looking for parser libraries for Java. ANTLR (ANother Tool for Language Recognition) is the standard, but it is a parser generator - meaning that it generates Java source files that you have to compile. For a large project that might be acceptable, but here, its a build step too far.

Google's fifth result is Parboiled. Parboiled allows us to specify our format in Java code, and make use of it without code generation, so it looks like a good bet. It also has a functioning Markdown parser as one of its examples, which feels like a good portent. We tell Gradle about it's JAR file and convert a simple calculator example to Kotlin to see if it runs at all.

```kotlin
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
```

The only change we had to make from IntelliJ's conversion of the Java to Kotlin was to add the `open` keyword, as it turns out that Parboiled makes use of bytecode generation to enhance our parser class.

We used another Approvals Test here so that we could record the output from `parseNodeTree` for your benefit - it looks like this

````text
[Expression] '1+2'
  [Term] '1'
    [Factor] '1'
      [Number] '1'
        [0..9] '1'
    [ZeroOrMore]
  [ZeroOrMore] '+2'
    [Sequence] '+2'
      [[+-]] '+'
      [Term] '2'
        [Factor] '2'
          [Number] '2'
            [0..9] '2'
        [ZeroOrMore]
````
which is a representation of the `Rule`s that matched the parsed text `1+2`.

So far so good.

Now to try a subset of the book format.

```kotlin
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
```

Full disclosure - this test was the end result of at least a couple of hours of iterating in on a solution as we gained understanding of how Parboiled works, and tried to find a way of having the tests tell us how well we were doing. The `println` is a sure sign that we're really only using JUnit as way of exploring an API rather than writing TDD or regression tests.

 What does the parser look like?

```kotlin
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
```

I won't take this opportunity to explain PEG parsers, largely because you already know that at the time of writing I have no more than 4 hours experience. It's a selling point of Parboiled that you can probably read the code and have a good idea how it works. That isn't to say that we just rattled this out - it took a good 3 hours of experiment and blind alleys, even pairing with a trained computer scientist - and I'm still not convinced that it will work in all real circumstances. In order to find out we're going to need a better way of seeing what matches.

Reading the documentation shows that we can add `Actions` in `Sequence`s to capture results, which are added to a stack that is made available in the `parsingResult`. Let's define a `TextBlock` class to hold the lines that we're interested in and ask Parboiled to push one on the stack when when the result is available.

```kotlin
    data class TextBlock(val lines: MutableList<String>) {
        constructor(vararg lines: String) : this(mutableListOf(*lines))

        fun addLine(line: String) = lines.add(line)
    }
```


```kotlin
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
```

Now our test can access the result stack and actually make assertions. As `TextBlock` is a `data` class we get a working definition of `equals` for free.

```kotlin
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
```

This obviously isn't perfect, but it is a promising start. Parboiled is evidently working very hard behind the scenes to make this Rule-as-function which is then interpreted in context work in Java - this worries me a bit, but I'm prepared to suspend disbelief for now.

One interesting effect of the use of Parboiled is that we have introduced an intermediate form in our parsing. Previously we parsed the input and generated the output lines in one step (if you discount filtering nulls). In the new version we have built a list of `TextBlock` objects that we will later have to render into a Markdown file. This separation of concerns introduces complexity, but it also buys us flexibility and allows us to focus on one task at a time. Whilst there is a risk that we will have some difficulty with the rendering, I think it's small, so I'm going to concentrate on extending the parsing for now - adding the code block markers.

First let's add a test

```kotlin
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
```

and now duplicate-and-edit an implementation

```kotlin
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

```

Unfortunately this fails, and a little thought reveals that it's because our block-matching code only works for block markers at the start of a line. The previous behaviour was that blocks were defined by markers as the first non-space characters. My instinct here is to back up and extract some reusable Rule from `TextBlockRule`, and then use that to implement `CodeBlockRule`. Otherwise I'm going to have diverging Rules that I have to try to find the commonality in.

So, reverting to our two tests for the text block, we refactor the parser and define `CodeBlockRule` in much the same way as text `TextBlockRule`.

```kotlin
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
```

I test this with an unindented code block, and with mixed text and code blocks, before turning to indented blocks.

```kotlin
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
```

Well that went surprisingly well, in that it passes the tests and is pretty understandable as a parser. The main wrinkle is that I don't seem to be able to inline the

\\`
val result = Var(CodeBlock())
\\`

expressions - this must be something to do with the aggressive bytecode manipulation that Parboiled is performing. I expect that I will either understand this or be bitten by it again before the end of this project.

I'm a little dissatisfied by the fact that the lines in the `Block` objects contain the line separators and indents, but it's easier to throw away information than try to reconstruct it, so reason that it should stay at least for now. At least the tests document the behaviour.

Now that we have a parser that converts our source into a list of blocks, let's see how close we can get to having our original parser tests work against the new code.

Let's remind ourselves what our test looks like. Previously this was our only test, now we have a separate parser it looks like a low-level integration test.

```kotlin
    class CodeExtractorTests {

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
                """,
                expected = """
                |Title
                |=====
                |
                |This is Markdown paragraph
                |
                |```kotlin
                |  /* This is a code comment
                |  */
                |  fun aFunction() {
                |     return 42
                |  }
                |```
                |
                |More book text.
                |""")
        }

        private fun checkApprovedTranslation(source: String, expected: String) {
            assertEquals(expected.trimMargin(), translate(source.trimMargin()).joinToString(""))
        }

        fun translate(source: String): List<String> {
            TODO()
        }
    }
```

In order to get this to pass, I'm going to parse the source into blocks, and then render each one.

```kotlin
    fun translate(source: String): List<String> {
        val parser = Parboiled.createParser(ContextB4.BookParser::class.java)
        val blocks: List<Block> = ReportingParseRunner<Block>(parser.Root())
            .run(source)
            .valueStack
            .toList()
            .asReversed()
        return blocks.flatMap { render(it) }
    }

    fun render(block: Block): List<String>  = when (block) {
        is TextBlock -> block.lines
        is CodeBlock -> listOf("\n```kotlin\n") + block.lines + "```\n\n"
        else -> error("Unexpected block type")
    }
```

The `render` function took a bit of tweaking with newlines in code blocks, but it works. And I don't mind the newlines, as they allow us to express the realities of the requirements of the output Markdown. Obviously the `render` function should be moved to a method on `Block`, but I can't resist the temptation to try the new code on the whole of the book so far. We just have to plug the latest `translate` into the old file-reading code and

...

it works!

Flushed with success I reorganise things and tidy up before bed.

Firstly, the top level functions are

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

    private fun translate(file: File): Sequence<String> = Translator.translate(file.readText())

    private fun sourceFilesIn(dir: File) = dir
        .listFiles { file -> file.isSourceFile() }
        .toList()
        .sortedBy(File::getName)
        .asSequence()

    private fun File.isSourceFile() = isFile && !isHidden && name.endsWith(".kt")
```

I then pull the parser running into a `Translator` object - this allows us to lazily create the parser and runner and reuse them. Kotlin gives us other ways of achieving this aim, but I'm trying this `object` scoping out to see how it feels.

```kotlin
    object Translator {
        private val parser = Parboiled.createParser(BookParser::class.java)
        private val runner = ReportingParseRunner<Block>(parser.Root())

        fun translate(source: String): Sequence<String> = parse(source).flatMap { it.render() }.asSequence()

        fun parse(source: String): List<Block> = runner.run(source).valueStack.toList().asReversed()
    }
```

As promised I move the `render` function to a method on `Block`, with a default implementation in the interface

```kotlin
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
```

and finally use install the Kotlin All-Open compiler plugin and configure it to make `@BuildParseTree` annotated classes open, so that we can remove the `open` modifiers from every method in the `BookParser`

```kotlin
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
```


Now, a day's work from where we decided to try a proper parser, I am in a position to add the file include directive.

Firstly, a test for the parsing.

```kotlin
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
```

We can punt on how `IncludeDirective` should behave for now

```kotlin
    data class IncludeDirective(var path: String) : Block {
        override val lines: MutableList<String>
            get() = TODO("not implemented")
    }
```

and *just* implement the parsing

```kotlin
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
```

Of course it took me another hour to get `IncludeRule` working, even after discovering Parboiled's useful `TracingParseRunner`. I am beginning to internalise, if not actually understand, the interaction between `Rule`s and `Action`s - leading to the creation of `pushIncludeDirective`, so maybe I'm becoming a real programmer after all these years. Not so fast that I trust my intuition though, which is why the test has two include directives to check that I don't end up sharing state like last time.

Now we know where `includes` are in our source, we need to process them. In order to do this we need to know how to resolve a path to its contents.
For now, I'm just going to pass a `contentResolver` function to the render method of `Block`, which, now it is the parent of `IncludeDirective`, had better be restructured a bit

```kotlin
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
```

Now our `Translator` object can pass the contentResolver to each block's render

```kotlin
    object Translator {
        private val parser = Parboiled.createParser(BookParser::class.java)
        private val runner = ReportingParseRunner<Block>(parser.Root())

        fun translate(source: String, contentResolver: (String) -> List<String>): Sequence<String> = parse(source)
            .flatMap { it.render(contentResolver) }
            .asSequence()

        fun parse(source: String): List<Block> = runner.run(source).valueStack.toList().asReversed()
    }
```

leaving the top-level `translate(File)` function to supply an implementation of the `contentResolver`

```kotlin
    private fun translate(file: File): Sequence<String> =
        Translator.translate(file.readText(),
            contentResolver = { path ->
                file.parentFile.resolve(path).readLines().map { it + "\n" }
            }
        )
```

and yes it did take me a while to realise that I needed to re-add the newlines stripped by `File.readLines`.

<blockquote class="sidebar">
Sidebar

This wasn't helped by my originally writing every line to the output using `Writer.appendLn`. Now that the lines in a `Block` include their own newline, this meant that every line ended up with two newlines in the output, except for those in the included files, but Markdown rendering hid this well. Fixing the two newlines problem revealed the no-newlines problem.

Had I had any tests for the top-level rendering this would have been obvious. At this stage in the process though, my integration testing consists of rendering the book so far and looking at the Markdown as displayed by Markoff, a Mac renderer app.
</blockquote>

After an unconscionable amount of time I'm finally in a position to render the first chapter well enough to be reviewed, so I insert the required `//#include` directives and check the rendering. There is still an issue with the Markdown renderer interpreting

````markdown
``` three quotes in code blocks
````

which occurs when we include some Markdown inside files, but Googling reveals that I can mark those blocks with four `````'s.

Of course now I have another two chapters of material generated in my attempt to render the first, so I fix those up too and package up the three Markdown files for review.

