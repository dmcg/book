package com.oneeyedmen.book.Chapter_01_Spike

/*-
The First Kotlin File
---------------------

Risk reduction is high on the list of modern programming concerns. I want to know that idea of embedding the content of
this book in its own source code is tenable. So I'm going to start with a spike - a prototype that goes deep into the
heart of the problem but doesn't attempt breadth. This way we hope to find major problems that might scupper an idea
with the least effort.

So I'm typing this text as comments into a Kotlin source file called `2-spike.kt`. So far I've found that block comment
markers don't get in the way of my text, but that I also don't get a nice Markdown preview of the formatting. That's
alright though, as the nature of Markdown is that I can *see* the formatting even though it's only characters.

So the text is OK. What about the code?
-*/
//`
import org.junit.Assert.assertEquals
import org.junit.Test

class SpikeTest {

    @Test fun `2 plus 2 is 4`() {
        assertEquals(4, add(2, 2))
    }
}

fun add(a: Int, b: Int) = a + b
//`
/*-
Well I can run that in the IDE and from Gradle, so I guess that's a start. It does occur to me that when the book is
printed that we're going to want to see comments in the code as well as the book text, so I'd better start using a
special comment marker for the latter. I'm going to go with

```
|/*-
|this is book text
|-*/
```

for now and see how it goes.

It occurs to me that this is the opposite of the normal Markdown approach, where we can embed code inside blocks, with
the text as the top level. Here the code is the top level, and the Markdown is embedded in it. The advantage is that we
have to do nothing to our text to compile it, the disadvantage is that something is going to have to process the files
to do something with the text before we can render the Markdown. That seems reasonable to me though, as I write code for
a living, and an awful lot of that code does just this sort of thing to text. In fact we now have the opportunity to
practice explaining the writing of code on the code that publishes itself.
-*/