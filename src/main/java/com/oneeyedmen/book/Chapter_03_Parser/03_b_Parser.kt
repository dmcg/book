package com.oneeyedmen.book.Chapter_03_Parser

import com.oneeyedmen.book.Chapter_02_Entrenchment.ContextG1
import com.oneeyedmen.book.approvalsRule
import org.junit.Test
import org.parboiled.BaseParser
import org.parboiled.Parboiled
import org.parboiled.Rule
import org.parboiled.annotations.BuildParseTree
import org.parboiled.parserunners.ReportingParseRunner
import com.oneeyedmen.book.Chapter_03_Parser.Context4.TextBlock
import com.oneeyedmen.book.Chapter_03_Parser.Context4.BookParser
import org.parboiled.support.ParseTreeUtils
import org.parboiled.support.Var
import kotlin.test.assertEquals
import kotlin.test.fail

/*-
Let's see how close we can get to having our original parser tests work against the new code. But before we do, I think it's time to revisit our use of approvals tests.

Looking back over the text so far I'm not convinced that my use of approvals tests is working well. Their biggest downside is that they don't show the actual results in the test source, which is a particular problem when you can't just open approved files in the source tree to understand what is going on. So I'm going to revert to plain old assertions for a while.
-*/

object ContextB1 {

    //`
    class CodeExtractorTests {

        @Test fun writes_a_markdown_file_from_Kotlin_file() {
            checkTranslation("""
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
                |-*/""",
                expected = """
                |Title
                |=====
                |
                |This is Markdown paragraph
                |
                |```kotlin
                |  /* This is a code comment
                |  */
                |  fun aFunction() {
                |     return 42
                |  }
                |```
                |
                |More book text."""
            )
        }

        private fun checkTranslation(input: String, expected: String) {
            assertEquals(expected.trimMargin(), translate(input.trimMargin()))
        }

        private fun translate(input: String): String {
            TODO()
        }
    }


}


