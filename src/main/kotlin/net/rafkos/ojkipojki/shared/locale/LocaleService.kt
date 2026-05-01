package net.rafkos.ojkipojki.shared.locale

import java.util.Locale
import java.util.Properties

object LocaleService {
    private val strings: Properties = loadStrings()

    private fun loadStrings(): Properties {
        val props = Properties()
        loadFile(props, "locale_en")
        val lang = Locale.getDefault().language
        if (lang != "en") loadFile(props, "locale_$lang")
        return props
    }

    private fun loadFile(props: Properties, name: String) {
        val stream = LocaleService::class.java.getResourceAsStream("/locale/$name.properties") ?: return
        stream.use { props.load(it.reader(Charsets.UTF_8)) }
    }

    fun get(key: String): String =
        strings.getProperty(key) ?: "ERR_TRANSLATION_NOT_FOUND"

    fun get(key: String, vararg args: Any): String {
        var result = strings.getProperty(key) ?: return "ERR_TRANSLATION_NOT_FOUND"
        args.forEachIndexed { i, arg -> result = result.replace("{$i}", arg.toString()) }
        return result
    }
}
