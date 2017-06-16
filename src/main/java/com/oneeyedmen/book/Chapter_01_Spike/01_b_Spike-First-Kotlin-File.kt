package com.oneeyedmen.book.Chapter_01_Spike

/*-
## The First Kotlin File

Risk reduction is high on the list of modern programming concerns. I want to know that the idea of embedding the content of this book in its own source code is viable. So I'm going to start with a spike - a prototype that goes deep into the heart of the problem but doesn't attempt breadth. This way we hope to find major problems that might scupper an idea with the least effort.

I'm typing this text as comments into a Kotlin source file called `2-spike.kt`. Kotlin uses the same `/* block comment markers */` as Java and C. So far I've found that the markers don't get in the way of my text, but that I also don't get a nice Markdown preview of the formatting. That's alright though, as the nature of Markdown is that I can \*see\* the formatting even though it's only characters.

So text is OK. What about code?
-*/
//`
import org.junit.Test
import kotlin.test.assertEquals

class SpikeTest {

    @Test fun `2 plus 2 is 4`() {
        assertEquals(4, add(2, 2))
    }
}

fun add(a: Int, b: Int) = a + b
//`
/*-

{aside}
That's a [JUnit 4](http://junit.org/junit4/) test written in Kotlin. I'm going assume that you can read and understand JUnit tests, and at least get the gist of Kotlin when it is written as a direct translation of Java.
{/aside}

Well I can run that directly in the IDE and from the Gradle build tool, so I guess that's a start. Thinking ahead it occurs to me that I'm going to want to be able to write comments in the code. These could get confused with the comments that are the book text , so I'd better start using a different marker for the latter. Taking a lead from Javadoc, which *extends* standard block comments, but doesn't change their parsing by the compiler, I'm going to go with

```text
⁠/*-
This is book text which the compiler will ignore.
⁠-*/

/* This is a standard comment block
 ⁠*/
fun the_compiler_will_process_this() {

}
```

for now and see how it goes.

It occurs to me that this is the opposite of the normal Markdown approach, where we can embed code inside blocks, with the text as the top level. Here the code is the top level, and the Markdown is embedded in it. The advantage is that we have to do nothing to our text to compile it, the disadvantage is that something is going to have to process the files to do something with the text before we can render the Markdown. That seems reasonable to me though, as I write code for a living, and an awful lot of that code does just this sort of thing to text. In fact, now that I have a programming task to do, I can see whether or not I can describe that task in a way that makes sense to both of us.
-*/