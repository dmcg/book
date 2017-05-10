@file:JvmName("Main")
import java.io.File

fun main(args: Array<String>) {
    val srcDir = java.io.File(args[0])
    val outFile = java.io.File(args[1]).apply {
        absoluteFile.parentFile.mkdirs()
    }

    val translatedLines: Sequence<String> = sourceFilesIn(srcDir)
        .flatMap { translate(it).plus("\n") }
        .filterNotNull()

    outFile.bufferedWriter(Charsets.UTF_8).use { writer ->
        translatedLines.forEach {
            writer.appendln(it)
        }
    }
}

fun sourceFilesIn(dir: File) = dir
    .listFiles { file -> file.isSourceFile() }
    .toList()
    .sortedBy(File::getName)
    .asSequence()

private fun File.isSourceFile() = isFile && !isHidden && name.endsWith(".kt")

fun translate(source: File): Sequence<String?> = translate(source.readLines(Charsets.UTF_8))

fun translate(sourceLines: List<String>): Sequence<String?> {
    var inCodeBlock = false
    var inTextBlock = false
    return sourceLines.asSequence()
        .map { line ->
            when {
                !inCodeBlock && line.firstNonSpaceCharsAre("//`") -> { inCodeBlock = true; "```kotlin"}
                inCodeBlock && line.firstNonSpaceCharsAre("//`") -> { inCodeBlock = false; "```"}
                !inTextBlock && line.firstNonSpaceCharsAre("/*-") -> { inTextBlock = true; null}
                inTextBlock && line.firstNonSpaceCharsAre("-*/") -> { inTextBlock = false; null}
                inTextBlock -> line
                inCodeBlock -> line
                else -> null
            }
        }
}

fun String.firstNonSpaceCharsAre(s: String) = this.trimStart().startsWith(s)


