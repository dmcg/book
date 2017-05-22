package com.oneeyedmen.book.Chapter_02_Entrenchment

import java.io.File

/*-
Writing software is hard. We probably agree on that - I know that I find it hard, and you probably aren't investing time reading a book on a subject that you have no problem mastering. One of the many many hard problems is deciding, minute by minute, hour by hour, day by, well, you get the drift, what we should do next.

So far we have, if you'll forgive a military metaphor, taken some ground. Our previous foray resulted in some confidence that this book might be worth writing, and that the technology and process I proposed might do the job. But in order to gain this confidence we have overextended a bit. Some things don't work at all, and others are not implemented well.

Agile development calls this technical debt, but that term is a bit pejorative, and doesn't allow us to distinguish between, if you'll forgive a finance metaphor, leverage and lack of servicing. [TODO]

I was about to suggest that we review what does and doesn't work, maybe have a little refactor to make things clearer, but then a customer arrived who wanted to use the code in real life. Granted that customer was me, wanting to use the tool to simplify publishing code-heavy articles in my blog, but a customer is a customer. We're always having to balance immediately gratifying our customer or users against code quality - it can lead to some difficult negotiations - but in this case I'm insistent that writing a blog article outweighs the nagging doubt that the code just isn't good enough, and I find that I agree.

Our current main method is hard-coded to the source path in this project. Finding a way to run the code from the command-line, specifying the source directory and destination file as parameters, would get me off my back, so I set to.

-*/

object ContextA1 {

    @JvmStatic
    //`
    fun main(args: Array<String>) {
        val srcDir = File(args[0])
        val outFile = File(args[1]).apply {
            absoluteFile.parentFile.mkdirs()
        }

        val translatedLines: Sequence<String> = com.oneeyedmen.book.Chapter_01_Spike.ContextD1.sourceFilesIn(srcDir)
            .flatMap { com.oneeyedmen.book.Chapter_01_Spike.ContextD1.translate(it).plus("\n") }
            .filterNotNull()

        outFile.bufferedWriter(Charsets.UTF_8).use { writer ->
            translatedLines.forEach {
                writer.appendln(it)
            }
        }
    }
    //`
}
/*-
I test this using IntelliJ's runner and then go rummaging in the far corners of the Internet to find out how to invoke Gradle to both build and then run this class. Full-disclosure, this led to over an hour of trying to work out what Gradle was building where - code can seem very simple compared to build systems. As I know that my chances of remembering what incantations are required are low, I capture the required commands in a top level script in the project directory -

```bash
#!/usr/bin/env bash

./gradlew clean installDist distZip
echo Zip file built in build/distributions
echo Start script built in build/install/book/bin/book
```

I then put on my blogger's hat and write a couple of articles on the relationship between objects and functions in Kotlin, using the same formatting rules as this book. It goes pretty well, giving me more confidence that I could write a whole book this way.
-*/
