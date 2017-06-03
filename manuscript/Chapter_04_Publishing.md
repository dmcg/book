# Chapter 4 - Publishing

After an unconscionable amount of time I'm finally in a position to render the first chapter well enough to be reviewed, so I insert the required `//#include` directives and check the rendering. There is still an issue with the Markdown renderer interpreting

~~~
```
three quotes in code blocks
```
~~~

which occurs when we include some Markdown inside files, but Googling reveals that I can mark those blocks with four backticks.

Of course now I have another two chapters of material generated in my attempt to render the first, so I fix those up too and package up the three Markdown files for review.

When I review the Markdown files of the first three chapters, they still really don't seem like a good format to unleash on other people. If you view them as plain text the code is hard to read, and you need an editor with soft-wraps to view the paragraphs. There are Markdown previewers available, but each does a slightly different job. I tell myself that what I really need is rendering to PDF.

Bitter experience has taught me to be suspicious of any advice that delays getting feedback on a delivery, especially the voices in my head. Deep down I'm scared that this book isn't going to work, but I'm enjoying writing it. My ego finds it easier push my own doubts aside than other peoples' - this wouldn't be my first 6 month waste of time. Nevertheless it seems a shame to present work so far in anything less than the best light...

I had tried a week ago to find a good way to render Markdown to PDF on the Mac. I got as far as installing Pandoc, the best recommendation, but it had some Unicode issue, probably to do with my WORD-JOINER hack. In the end I square the feedback / quality circle by paying Leanpub $99 to host the book. They will then render all the files into a single PDF, generate a table of contents, index etc, and I have no excuses left.

Inevitably viewing the Leanpub generated PDF reveals some nuances in their Markdown parsing that require minor text changes, but the process is remarkable smooth. I had only to specify the location of the GitHub repo holding this text, write the translated Markdown into a `manuscript` directory rather than `build/book`, add a file `Book.txt` (currently hardcoded) with the names of the chapter markdown files in order, push the `manuscript` directory to GitHub, and click a button on a Leanpub webpage. 30 seconds later PDF, epub and mobi versions are available for download. I copy the PDF link URL and send it to some friends for feedback.

While I wait, I figure that I have little to loose by actually publishing work to date on Leanpub. Their book pages shows how complete a book is (I set the figure to 5%) in order to set expectations, so I go ahead and publish. I know that this isn't really the same as being a published author, but it feels pretty momentous nevertheless.

