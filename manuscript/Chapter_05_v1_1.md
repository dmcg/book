# Chapter 5 - Planning v1.1

## MVP

At this stage I think that I can safely say that the publishing code is a Minimal Viable Product. I recently had a disagreement with a client about the definition of MVP after they published a long document describing the features that their MVP would have. To me though, you cannot predict whether your product will be viable when it has a set of features - you can only release it with those features and see if it is. And if we wait until we have our MVP complete before release, how do we know that it wouldn't have been viable with half of them?

It follows that you can only approach an MVP from below, and you have to be continually releasing Sub-minimal Viable Products until, at some point, you discover that one of them is viable.

That is the process that our publishing code went through. Viable in this case means good enough to generate a reviewable manuscript for this book. I really can't think of any feature that the software has that I could take out and make it over that bar. I suppose that the code itself could be more minimal with the same features, but I'm in quite a happy place. This is version 1.0

Is the book at the MVP stage yet? Frankly I doubt it. I believe (without any evidence yet) that my target audience could read and maybe even learn from the chapters so far, but there is a crucial difference between a book and a product. A product is generally used more than once - if it is fit for purpose the first time you use it you will find it has more utility as it gets better. A book, on the other hand, is to some large extent used up by reading it. If I revise Chapter 2 three times it may get better, but your interest will be over after at most two readings.

Looked at like this, I suppose we might find that some number of completed chapters were minimal viable, but as soon as I start moving material around, or heavily editing existing chapters, that confuses the equation. Perhaps that is why the episodic format is attractive - it is more amenable to addition rather than revision. Perhaps the Open-Closed Principle is at work here too.

## Enhancements

One of the nice things about this process that I've slipped into is that I don't have to prioritise writing prose over writing software, I only have to do the latter and write about it. There are times when I could be cutting code quicker if I weren't documenting the process, and there is a whole layer of source complexity involved in having several generations of the same code coexisting in the same file that I am (currently) hiding from you, but on the whole the self-reflection is working well for me.

I've had my author's hat on for a while now. The last time I wore the developer hat was completing the parser in Chapter 3. Since then the author has tried to publish, found it (at least technically) successful, declared that we have an MVP, and has written about it. That process has inevitably raised some issues in the software, so I grab both hats and discuss what new features would be nice.

### Simplify Prose Comment Block Ends

Author - "Every time I type `/*-` at the beginning of a line to start a prose block, IntelliJ helpfully inserts a `*/` on the line underneath to close the comment, but I have to prepend a `~` to end our prose block. I keep forgetting to do this, and nothing alerts me until I view the rendered Markdown mess. Can you warn me?"

Developer - "I could make it an error at the point of translation I suppose, but then I'd have to find a way to tell you where the problem was. Why don't I just allow

```kotlin
/*-
This is published
 */
```

ie - let the normal block-comment-end end our prose blocks? Unless you have a need to overlap code and prose comments that should be fine, and it would be backward compatible with the current functionality."

Author - "If you could do that it would be great. How long do you think it would take?"

Developer - "Hmmm, 20 minutes if it works out like I think it should, otherwise all day"

Author - "Well it will almost certainly save me 20 minutes over the course of the book. I think that we should do it if it's done in that time, otherwise stop and we'll drop the feature."

### Unindent Source

Author - "When Kotlin is rendered into code blocks it keeps whatever level of indentation it had in the source. This often results in lines being wrapped in the PDF, which is less than ideal, and will be even worse on phone screens. Can we fix that?"

Developer - "Shouldn't be a problem - I can just remove the first line's indent from all lines. Or the minimum indent from all lines. Which would you like?"

Author - "I don't know - which would be quicker to try?"

Developer - "Well it's easier to find the first line's indent, but there really isn't much in it. Let me do that for now, and you can let me know whether you need a better job done. Maybe an hour's work, as I have some test cases to update."

### Better Code Block Inclusion

Author - "I keep on having to jump through Markdown hoops because the `//#include` directive isn't processed in prose blocks, but I can't use Markdown code block markers in Kotlin code."

Developer - "Hmmm, I could work out how to process those directives in prose blocks, or give you a special code for including code block markers at the Kotlin level. Maybe a general purpose directive

~~~Kotlin
//#literal ---
~~~

The latter would be simpler, but I'm more interested to learn how to be parsing more than one thing at once, and it would give you something to write about."

Author - "Ooh, sneaky, I like that. And the longer it takes you the more material I have."

Developer - "Yes, within reason - remember how badly that first-generation parser refactoring worked out!? Let's try to get this one done in a day."

### Streamline New Chapters

Author - "Every time I start a new chapter, I have to update the Book.txt that Leanpub uses to stitch together the constituent Markdown files, and add a line to the shell script that invokes the translator on a directory"

Developer - "Oh, so Leanpub is stitching together files as well as us? Why didn't you say so? Why don't I just gather together all our translated source files in order and generate Book.txt from those? It's probably an hour's work, and will remove some code that we currently don't have any tests for."

### Streamline Publication

Author - "You've given me a batch file to run to build the book, but every time you make changes to the publishing code I have to update that first, and I don't necessarily know / remember when that is necessary."

Developer - "Let me look at the Gradle build [sigh] - I can probably figure out a way to build the software, run the tests and build the whole book in one command. It's probably a couple of hours of pain."

## Prioritization

We have a chat and decide that, while all three features would be nice, Unindent Source would make the biggest difference to the published book. The others are all focused on speeding up writing / development, so are arguably less important, but should be achievable relatively soon after that.


