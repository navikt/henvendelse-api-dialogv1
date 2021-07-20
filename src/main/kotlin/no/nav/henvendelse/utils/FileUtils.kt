package no.nav.henvendelse.utils

object FileUtils {
    fun readFileContent(path: String): String {
        return FileUtils::class.java
            .getResource(path)
            ?.readText()
            ?: throw IllegalStateException("Could not find file at $path")
    }
}
