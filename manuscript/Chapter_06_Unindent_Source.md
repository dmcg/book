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

data class CodeBlock(override val lines: MutableList<String>) : SourceBlock() {
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
    return this.map { if (minimumIndent >= it.length) it else it.substring(minimumIndent) }
}

val String.indentLength get() =
    if (this.isBlank())
        Integer.MAX_VALUE
    else
        Math.max(this.indexOfFirst { !it.isWhitespace() }, 0)
```

That's reasonably straightforward, and reads nicely because of our extension methods. Another Kotlin subtlety is hidden in the definition of `minimumIndent`. `Iterable.min()` will return `null` if there are no items in the list - in the same situation `Collections.min()` throws `NoSuchElementException`. Without Kotlin's insistence that I couldn't call `String.substring()` with a `Int?` (nullable Int) there would have been a failure if I ever tried to render an empty `CodeBlock`. That's the sort of thing that's embarrassing, and I can't put my hand on my heart and say that I was planning to write a test for the empty case. As it is, I wrestle with my engineering judgement and conclude that it isn't worth adding, but I'd be open to persuasion if my pair felt strongly.

[TODO - the Math.max(..., 0) and the problem of empty lines]



