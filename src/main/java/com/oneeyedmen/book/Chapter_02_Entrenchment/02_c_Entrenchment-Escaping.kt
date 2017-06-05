package com.oneeyedmen.book.Chapter_02_Entrenchment


/*-
What was the problem again? I want to be able to write express in Markdown the syntax for my 'book text' markers, namely `/*-` and `-*/`, but in a quoted section, so that you can read

```text
⁠/*-
this is book text
⁠-*/
```

but when I type that and run it through the translation, it (correctly) interprets the markers and messes up the formatting. I had bodged around the problem by prepending `|` characters to the lines that cause problems, but that makes them very hard to read.

I talk the problem through with my friend Alan, and we hatch the plan of interpreting the traditional `\` as an escape character which will just be removed from the output, allowing me to write `/*-` and `*/` at the beginning of a line. Of course in order to write `\` there will need to be additional logic to replace a double-escape with a single.

Thinking through the consequences this looks a less good plan, as I may wish to use genuine escape sequences in the Kotlin code that is output as part of the book. I *could* just ignore `/*-` `-*/` pair in quote blocks, but then I'd be having to parse Markdown which looks like a can of pigeons.

So instead I guess that we should pick a character other than `\` to prevent text matches but not be rendered in our final output. This looks like a job for Unicode, so now we have 2 problems.

A bit of Googling reveals Unicode U+2060 - "WORD JOINER", which is apparently similar to a zero-width space, but not a point at which a line can be broken. This seems perfect - I can enter it with the Mac hex input keyboard mode, and it prevents our special text being interpreted at the beginning of a line. It is rendered properly in IntelliJ, which is to say, not at all, which is great if you're reading, but not so good when editing. I can probably use its HTML entity form `\&#2060;` if I want to make it more visible.

Frankly this is all making my head hurt. My spidey senses tell me that this going to cost me in the future, but for now, I need to get on with things and this change required no code, so I find all the places where I used the `|` hack and replace them with the invisible word joiner.

-*/

