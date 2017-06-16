/*-
# Chapter 1 - Spike

I'm writing this in Lisbon airport, an hour before my flight back to London is due to leave. On this holiday I've decided I'm pretty committed to writing this book, and now I'm wondering what technologies I should use for its production.

What criteria should I use to make the decision? Well the working title of the book is "Modern Programming in Kotlin" so that might help. Defining modern programming is something that I hope will become clear by the end, but as I think it through, I realise that deciding how to write the book is fundamentally an engineering decision. Engineering is about trade-offs, choice, optimisation. It's about making the best use of your time or your client's money. And very often in programming that comes down to trading the cost to make something now against the cost of fixing it later.

I know that this book will take a long time to write, and I also know that it will be full of code samples that we'd both like to actually work by the time you come to read them. So I'm looking for a way that I can mix code and prose in a way that is easy to write and maintain. 

And that's why these words are being written into the IntelliJ IDE rather than Word. They are plain text now, although I'm already thinking of them as [Markdown](https://daringfireball.net/projects/markdown/) - I just haven't *needed* to emphasise anything yet. I suspect that this prose will end up as comments in Kotlin source files, and that some sort of build system will put the book together for me. In fact, if you're reading this on paper you can't see it yet, but this file is already surrounded by a Gradle build and is in the context of a Git repository. This is the way that modern programming projects in Kotlin will start, and so it feels rather fitting that the book does too.

-*/