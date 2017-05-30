package com.oneeyedmen.book.Chapter_03_Parser

/*-
Let's see how close we can get to having our original parser tests work against the new code. But before we do, I think it's time to revisit our use of Approvals tests.

Looking back over the text so far I'm not convinced that my use of Approvals tests is working well. Their biggest downside is that they don't show the actual results in the test source, which is a particular problem when you can't just open approved files in the source tree to understand what is going on. So I'm going to revert to plain old assertions for a while.
-*/