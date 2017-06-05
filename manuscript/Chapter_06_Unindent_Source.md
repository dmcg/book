# Chapter 6 - Unindent Source

I find that predicting what the consequences of a functional change in to a codebase are a good bellwether. A healthy codebase is one in which changes don't propagate far from their root - if I have to touch more than a single class to change this behaviour I would question how well factored the code is. In addition, predicting and then testing our prediction allows us to gauge how well we understand what we have built.

As we have a clean separation between parsing and rendering, `CodeBlock` is the only production code that should have to change. The tests are a different story. The only tests we have for the render methods are currently the badly named `CodeExtractorTests` - effectively an integration test of parsing and rendering, but not currently updated for the `#include` directive processing.

In my excitement to get an actual PDF published I'd overlooked this deficit. One of the main arguments in favour of a separate QA function in a software team is to reign in the natural optimism of developers, and in this case I've accumulated a little technical debt. Should we pay it down, or get on with making progress?

This is one of those situations where it often depends on the codebase and your relationship with your customer. If the codebase has a lot of debt, and it might be difficult to take time after shipping this feature to re-balance things, I'd definitely add some tests now. It is easier to ask forgiveness than permission, and even easier if you have to do neither because you invisibly do the right thing.

In this case though, the relationship between customer and developer is pretty good. I appreciate my desire to publish more readable code as soon as possible, and trust myself to give me permission to pay down debt when it starts affecting progress. So let's crack on.

Let's at least add some rendering tests first though.

```kotlin
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
```

Oh that's interesting, the CodeBlock's newlines really aren't in the place that I expect. Lets fix that first - it at very least violates the Principle of Least Surprise.

```kotlin
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
```


```kotlin
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
```

That's reasonably straightforward, and reads nicely because of our extension methods. Another Kotlin subtlety is hidden in the definition of `minimumIndent`. `Iterable.min()` will return `null` if there are no items in the list - in the same situation `Collections.min()` throws `NoSuchElementException`. Without Kotlin's insistence that I couldn't call `String.substring()` with a `Int?` (nullable Int) there would have been a failure if I ever tried to render an empty `CodeBlock`. That's the sort of thing that's embarrassing, and I can't put my hand on my heart and say that I was planning to write a test for the empty case. As it is, I wrestle with my engineering judgement and conclude that it isn't worth adding, but I'd be open to persuasion if my pair felt strongly.

Now I run the code on the source of this book, and my smugness at the type-system obviating the need for tests turns out to be a little optimistic. I get `String index out of range: -1` invoking `substring()` in `toLinesWithMinimumIndentRemoved()`. Hah - Kotlin `indexOfFirst()` returns `-1` and not null when the predicate doesn't match any item. I know that -1 is an established convention for not found, but really Kotlin, it looks like you're just trying to embarrass me.

The problem is lines that are only whitespace, or, I suppose, have no characters at all. Let's add some tests for those, and now we've bitten the bullet of multiple tests, one for the empty code block.

```kotlin
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
```

The indent for a blank line is obviously 0 not -1, so lets force that.

```kotlin
val String.indentLength get() = Math.max(0, this.indexOfFirst { !it.isWhitespace() })
```

Oh good grief, my test still fails, because now the minimum indent is 0, no indent is removed from any line. This is making me look like a fool.

OK, new strategy - any line that is entirely whitespace gets an indent of null. Then I'll remove those nulls from the list before taking the min. I'd better beef up the test a bit first.

```kotlin
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
```

And now the implementation is

```kotlin

fun List<String>.toLinesWithMinimumIndentRemoved(): List<String> {
    val minimumIndent = this.map { it.indentLength }.filterNotNull().min() ?: 0
    return this.map { it.substring(minimumIndent) }
}

val String.indentLength get() : Int? =
    when {
        this.isNullOrBlank() -> null
        else -> Math.max(0, this.indexOfFirst { !it.isWhitespace() })
    }
```

I run that and

`java.lang.StringIndexOutOfBoundsException: String index out of range: -3`

Gah. Now we're trying to remove the first four characters from the line that is just `\n`. OK, last chance to look like a professional programmer.

```kotlin
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
```

Well it's 23:40 here, so I'm glad to say that not only passes, but runs successfully on the entire book so far.

This has been another humbling episode. I have tried as far as is sensible to show the process of coding in real life, as so many books skip to "here's one I prepared earlier." If you also occasionally have trouble implementing an at-first-glance simple feature take heart that I have worked with many excellent developers who would not, at the very least, have spotted the pitfalls here until the tests failed. The code makes a monkey out of the best of us, which is why tests and an enquiring, even sceptical, mind are so useful.

Are we done? To be honest I'm not convinced that I've considered all the corner cases here. It really wouldn't surprise me to find a formatting glitch in the book and trace it back here, nor to end up with another IndexOutOfBoundsException. If this code were running in an unattended system, or anywhere that I might get a late-night call to fix, I would want to make sure that another pair of eyes had reviewed the code and tests. As it is, the consequences of failure are just that I will have to context switch back into this code and debug it. Whilst the cost of that context switch is easy to underestimate; this is engineering, and it's my job to judge the trade-offs. In this case, it's a chance I'm prepared to take.

