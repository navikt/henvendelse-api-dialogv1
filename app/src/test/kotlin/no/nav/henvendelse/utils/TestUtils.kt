package no.nav.henvendelse.utils

object TestUtils {
    fun <T> withProperty(key:String, value: String, block: () -> T):T  {
        val original: String? = System.getProperty(key)
        System.setProperty(key, value)
        val result = block()
        original
            ?.let { System.setProperty(key, it) }
            ?: System.clearProperty(key)
        return result
    }
}