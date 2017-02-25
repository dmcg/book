package com.oneeyedmen.book.Chapter_01_Spike.com.oneyedmen.book.publishing


import com.oneeyedmen.okeydoke.ApproverFactories.fileSystemApproverFactory
import com.oneeyedmen.okeydoke.junit.ApprovalsRule
import com.oneeyedmen.okeydoke.junit.TestNamer
import org.junit.runner.Description
import java.io.File

fun approvalsRule() = ApprovalsRule(fileSystemApproverFactory(File("src/test/java")), ContextTestNamer())

class ContextTestNamer : TestNamer {

    override fun nameFor(description: Description) =
        description.testClass.nameWithPrefixButNoPackage + "_" + description.methodName

}

private val Class<*>.nameWithPrefixButNoPackage get() = canonicalName.removePrefix(`package`.name + ".")
