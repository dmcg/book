import java.io.File

fun main(args: Array<String>) {
    val dir = File("src/test/java/com/oneeyedmen/book/Chapter_01_Spike")
    val translatedLines: Sequence<String> = sourceFilesIn(dir)
        .flatMap { translate(it).plus("\n") }
        .filterNotNull()

    val outDir = File("build/book").apply {
        mkdirs()
    }

    outDir.resolve(dir.name + ".md").bufferedWriter(Charsets.UTF_8).use { writer ->
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


