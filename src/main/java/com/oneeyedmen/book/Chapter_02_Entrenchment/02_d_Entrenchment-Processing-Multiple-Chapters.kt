package com.oneeyedmen.book.Chapter_02_Entrenchment

/*-
Ironically, writing about the attempt to fix the representing-our-own-codes-in-our-output problem has given us the same problem in another set of files, those for this chapter, currently Chapter 2. I'd like to be able to review the whole book so far, which means converting two chapters' worth of Kotlin files into Markdown for reading. Luckily at the top of this chapter I worked out how to build the current software to be invoked from the command-line, so I add a top-level script to invoke this twice for the two current chapters.

```bash
#!/usr/bin/env bash

build/install/book/bin/book src/main/java/com/oneeyedmen/book/Chapter_01_Spike build/book/Chapter_01_Spike.md

build/install/book/bin/book src/main/java/com/oneeyedmen/book/Chapter_02_Entrenchment build/book/Chapter_02_Entrenchment.md
```

Running this yields two Markdown files that I can preview with IntelliJ's Markdown plugin to check that they look sensible.

This script leaves me feeling quite dirty - it can only work with one particular working directory, it has duplication, and it will require editing every time that I add a chapter (but at least not every file within a chapter). If I was sharing this code with a team I think that pride would cause me to do a better job, but for now momentum is more important than long-term efficiency, so I suck it up and move on to the next thing keeping me from sending out a review text - including the contents of files.
-*/

