package com.oneeyedmen.book.Chapter_02_Entrenchment

/*-
So it's back to the drawing board.

I'm mulling it over when I realise that I now have two problems caused by translating our codes when we don't want them translated - this, and the last issue that I had solved using the Unicode Word Joiner. In both cases the only time we see the issue in practice is

```text
inside Markdown quote blocks
```

I had previously rejected the idea of suspending translation inside these blocks, but now that it solves two problems it's looking more attractive. But it also feels kind of arbitrary. Notwithstanding my desire to make progress quickly to publish Chapter 1 for review, the aim of this book is to show largely unedited the process of writing software. I could come back and re-write this chapter when I can make it look smooth, but the truth is that I'm having a bit of a crisis of confidence about the whole translation code. I think it's time to step back and see if we are still on the right track.

This is a very familiar stage in a software project for me. We have a kinda-cool idea - embedding the book text into the source of its examples rather than vice-versa. A spike shows that it works well for the customer - in this case me. We build on that spike, but then find that a number of tactical decisions driven by a desire to maintain momentum lead to a muddied model. It's all a bit harder than we expected, and we rightly question whether we are on the right path at all.

So let's review my options.

I could abandon the prose-in-code model - there are other ways to embed code and make sure that it compiles and works as expected. But none that I've tried allow the degree of refactoring and cross-referencing that I'm enjoying here, at least not with the immediate ability to visualise the output that this solution has. On the whole I'm inclined to continue down the path, and not just because the path is giving me things to write about.

Is there some more off-the-shelf processing system that I could use? Adopting a standard templating language would bring things like escaping and file inclusion for free. Perhaps more importantly, they will have worked through the sort of issues that I'm having - is a file included before or after expansion for example. On the downside, they are usually aimed at embedding in files like HTML rather than Kotlin source, so I might find that I end up in a dead-end where I can't solve a problem at all.

Assuming that I continue with prose-in-code, what has this experiment taught me about the translation?

1. The source files have to be parsable by the Kotlin compiler and IntelliJ has to be able to treat them as regular source. That's why the prose has to go into comment blocks.

2. I want to distinguish between prose comment blocks and code comment blocks - leading to the use of `/*-` and `-*/` markers, taking a lead from Javadoc. Actually I don't think that I've actually made use the distinction, but its cost is low.

3. Code from the file should be expanded into Markdown code blocks, but only for some sections of the file. We can't use a block comment to mark those sections because they would then not be visible to the Kotlin compiler, so instead we use `//\`` to mark the start and end of the blocks. The compiler will ignore the lines as they begin with a comment, and I can associate the backtick as Markdown's quote character. On reflection `//\`\`\`` would have been even better - maybe that's a change I can make later.

4. So far our special syntax has been able to occupy a line of its own, although in the case of the `//\`` markers they need to be indented to allow the code they are in to be reformatted.

5. There has to be a way to have the markup rules written to the output rather than interpreted.

6. I need a way of including the contents of a file in a Markdown quote block, without interpreting the contents of the file.

I can't see any way that an off-the-shelf templating system could meet these requirements - the need to interoperate with Kotlin comments being the key issue. But I still really like this way of working, so I'm going to push on with the code that I have.
-*/
