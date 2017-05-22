package com.oneeyedmen.book


import com.oneeyedmen.okeydoke.ApproverFactories.fileSystemApproverFactory
import com.oneeyedmen.okeydoke.junit.ApprovalsRule
import com.oneeyedmen.okeydoke.junit.TestNamer
import org.junit.runner.Description
import java.io.File

fun approvalsRule() = ApprovalsRule(fileSystemApproverFactory(File("src/main/java")), ContextTestNamer())

class ContextTestNamer : TestNamer {

    override fun nameFor(description: Description) =
        description.testClass.nameWithPrefixButNoPackage + "_" + description.methodName

}

private val Class<*>.nameWithPrefixButNoPackage get() = canonicalName.removePrefix(`package`.name + ".")
