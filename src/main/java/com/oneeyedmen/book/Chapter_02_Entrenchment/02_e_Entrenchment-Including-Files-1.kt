package com.oneeyedmen.book.Chapter_02_Entrenchment

import com.oneeyedmen.book.Chapter_01_Spike.ContextC7
import com.oneeyedmen.book.Chapter_01_Spike.ContextC7.firstNonSpaceCharsAre
import com.oneeyedmen.book.approvalsRule
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

/*-
To review - the issue is that currently the book text has `[TODO]` markers in place of the contents of the Approvals Tests approved files. I could copy and paste the text from the file into the manuscript of course, but that breaks one of those rules that we ignore at our peril - the Single Point of Truth. If I do copy and paste the text then it can get out of sync with the file contents, which would be confusing for you, the reader. It doesn't seem like automatically including files should be hard, and I can think of some other places that it might come in handy, so let's just get on and implement it.

Our approach to special codes so far has been to enrich Kotlin comments, so that the compiler ignores our information. It's easy to extend that approach, and I settle on

```
//#include "filename.txt"
```

as worth trying.

Let's get on and write a new test for including files.
-*/

object ContextC1 {

    //`
    class CodeExtractorTests {

        @Rule @JvmField val approver = approvalsRule()

        @Test fun writes_a_markdown_file_from_Kotlin_file() {
            val source = """
            |package should.not.be.shown
            |/*-
            |Title
            |=====
            |
            |This is Markdown paragraph
            |-*/
            |
            |object HiddenContext {
            |  //`
            |  /* This is a code comment
            |  */
            |  fun aFunction() {
            |     return 42
            |  }
            |  //`
            |}
            |
            |/*-
            |More book text.
            |-*/
            """.trimMargin()
            approver.assertApproved(translate(source).joinToString("\n"))
        }

        @Test fun includes_a_file() {
            val source = """
            |/*-
            |Book text
            |
            |//#include "included-file.txt"
            |
            |more text
            |/*-
            """.trimMargin()
            approver.assertApproved(translate(source).joinToString("\n"))
        }
    }
    //`

    fun translate(source: String) = ContextA2.translate(source)
}

/*-
Whereas previously we had a single test for all of our formatting, #include seems like an orthogonal concern, so it gets its own test.

Before we go on I decide to remove the duplication in the test methods
-*/

object ContextC2 {

    //`
    class CodeExtractorTests {

        @Rule @JvmField val approver = approvalsRule()

        @Test fun writes_a_markdown_file_from_Kotlin_file() {
            checkApprovedTranslation("""
            |package should.not.be.shown
            |/*-
            |Title
            |=====
            |
            |This is Markdown paragraph
            |-*/
            |
            |object HiddenContext {
            |  //`
            |  /* This is a code comment
            |  */
            |  fun aFunction() {
            |     return 42
            |  }
            |  //`
            |}
            |
            |/*-
            |More book text.
            |-*/
            """)
        }

        @Test fun includes_a_file() {
            checkApprovedTranslation("""
            |/*-
            |Book text
            |
            |//#include "included-file.txt"
            |
            |more text
            |/*-
            """)
        }

        private fun checkApprovedTranslation(source: String) {
            approver.assertApproved(translate(source.trimMargin()).joinToString("\n"))
        }
    }
    //`

    fun translate(source: String) = ContextA2.translate(source)
}

/*-
which by removing clutter lets us focus on what we are really trying to demonstrate, albeit the actual result is hidden in the approved files.

Speaking of which, we can't really approve the output for `includes_a_file` as we don't. In order to get a little further lets create a file next to this Kotlin file with some text to be included

```
[TODO]
included file first line
included file last line
```

It's worth noting at this point that what to put in this file took some thought. I originally wrote `included file line 1` and `included file line 2` but realised that you would have to know that there wasn't a line 3 to know that the results were correct. I have also not put a line break at the end of the included file, because the files that we want to include do not have trailing break. Should the inclusion add a break? My gut feeling is yes, but I'm not going to think too hard. Instead I'll go with that gut feeling and see if it works out when run over the book text so far.

Where to put the file also raises some questions. The files that we want to include are currently in the same directory as the file that includes them. Until I have a need for another behaviour I guess that

```
//#include "filename.txt"
```

should resolve `filename.txt` relative to the the source file. But so far, our tests have just translated strings and have no concept of their fileyness.

We could change our tests to read the source from the filesystem, but that would mean that we had to look in a source file, an approved file, and the test, to work out what is going on. Instead I'm going to try punting the problem upstream, by requiring that callers of the `translate` function provide a way of resolving a file path to the contents of that filename, in addition to the string that they want translated. In the test, we'll use a lambda that just returns a known string.
-*/


object ContextC3 {

    @Ignore
    class CodeExtractorTests {

        @Rule @JvmField val approver = approvalsRule()

        //`
        @Test fun includes_a_file() {
            checkApprovedTranslation(
                source = """
                    |/*-
                    |Book text
                    |
                    |//#include "included-file.txt"
                    |
                    |more text
                    |/*-""",
                fileContents = """
                    |included file first line
                    |included file last line""")
        }

        private fun checkApprovedTranslation(source: String, fileContents: String) {
            approver.assertApproved(translate(source.trimMargin(), { fileContents }))
        }

        fun translate(source: String, fileReader: (String) -> String): String {
            TODO()
        }
        //`
    }
}

/*-
With this scheme it turns out that I needn't have created the file, so I delete it. I'm also regretting the technical overdraft that is `translate`, but I really don't have a better idea at the moment, so I'm inclined to leave the logic as-is, but at least try to make it not much worse. Perhaps the easiest way of achieving that is to treat file inclusion as a pre-processor step before our previous logic.
-*/

object ContextC4 {

    //`
    fun translate(source: String, fileReader: (String) -> String) =
        translate(preprocess(source, fileReader))

    fun preprocess(source: String, fileReader: (String) -> String): String {
        TODO()
    }
    //`

    fun translate(source: String) = ContextC7.translate(source)
}


/*-
This keeps things simple, but as I think through the consequences I realise that it means that the contents of the included file will be subject to the translation process, which would result in a complete mess when our use-case is including a file which sometimes includes our un-translated codes.
-*/