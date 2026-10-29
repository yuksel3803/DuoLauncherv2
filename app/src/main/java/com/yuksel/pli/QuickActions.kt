package com.yuksel.pli

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Things the search can do directly, without opening an app first:
 * "minuteur 10 min", "alarme 7h30", "rappel acheter du pain", "5 miles en km", "20 € en dollars",
 * "appeler Maman", "sms Léa".
 */
object QuickActions {

    enum class Kind { TIMER, ALARM, REMINDER, CONVERT, CURRENCY, CALL, SMS }

    /** One ready-to-run action shown at the top of the search. [run] returns a short confirmation, or null. */
    class Action(val kind: Kind, val title: String, val detail: String, val copy: String? = null, val run: (Context) -> String?)

    private val FR = Locale.FRANCE

    // ---------- Durations and times ----------

    /** "10", "10 min", "1h30", "1 h 30", "90 s", "2 heures" → seconds. */
    fun parseDuration(text: String): Int? {
        val t = text.trim().lowercase(FR).replace(",", ".")
        Regex("""^(\d+)\s*(?:s|sec|secondes?)$""").find(t)?.let { return it.groupValues[1].toInt() }
        Regex("""^(\d+)\s*(?:h|heures?)\s*(\d{1,2})?\s*(?:min|mn|m)?$""").find(t)?.let { m ->
            return m.groupValues[1].toInt() * 3600 + (m.groupValues[2].toIntOrNull() ?: 0) * 60
        }
        Regex("""^(\d+)\s*(?:min|mn|m|minutes?)?\s*(?:(\d{1,2})\s*(?:s|sec|secondes?)?)?$""").find(t)?.let { m ->
            return m.groupValues[1].toInt() * 60 + (m.groupValues[2].toIntOrNull() ?: 0)
        }
        return null
    }

    fun durationLabel(seconds: Int): String {
        val h = seconds / 3600; val m = seconds % 3600 / 60; val s = seconds % 60
        return listOfNotNull(if (h > 0) "$h h" else null, if (m > 0) "$m min" else null, if (s > 0) "$s s" else null).joinToString(" ").ifEmpty { "0 s" }
    }

    // ---------- Units ----------

    private data class Unit(val dimension: String, val factor: Double, val label: String)

    private val UNITS: Map<String, Unit> = buildMap {
        fun add(names: String, u: Unit) = names.split(",").forEach { put(it.trim(), u) }
        add("km,kilometre,kilometres,kilomètre,kilomètres", Unit("length", 1000.0, "km"))
        add("m,metre,metres,mètre,mètres", Unit("length", 1.0, "m"))
        add("cm,centimetre,centimetres,centimètre,centimètres", Unit("length", .01, "cm"))
        add("mm,millimetre,millimetres,millimètre,millimètres", Unit("length", .001, "mm"))
        add("mi,mile,miles", Unit("length", 1609.344, "miles"))
        add("ft,pied,pieds,feet,foot", Unit("length", .3048, "pieds"))
        add("in,pouce,pouces,inch,inches", Unit("length", .0254, "pouces"))
        add("yd,yard,yards", Unit("length", .9144, "yards"))
        add("kg,kilo,kilos,kilogramme,kilogrammes", Unit("mass", 1.0, "kg"))
        add("g,gramme,grammes", Unit("mass", .001, "g"))
        add("lb,lbs,livre,livres,pound,pounds", Unit("mass", .45359237, "livres"))
        add("oz,once,onces,ounce,ounces", Unit("mass", .028349523125, "onces"))
        add("t,tonne,tonnes", Unit("mass", 1000.0, "t"))
        add("l,litre,litres", Unit("volume", 1.0, "L"))
        add("cl,centilitre,centilitres", Unit("volume", .01, "cL"))
        add("ml,millilitre,millilitres", Unit("volume", .001, "mL"))
        add("gal,gallon,gallons", Unit("volume", 3.785411784, "gallons"))
        add("km/h,kmh,kph", Unit("speed", 1 / 3.6, "km/h"))
        add("mph", Unit("speed", .44704, "mph"))
        add("m/s", Unit("speed", 1.0, "m/s"))
        add("noeud,noeuds,nœud,nœuds,kt,kn", Unit("speed", .514444, "nœuds"))
        add("o,octet,octets", Unit("data", 1.0, "o"))
        add("ko,kb", Unit("data", 1e3, "Ko"))
        add("mo,mb", Unit("data", 1e6, "Mo"))
        add("go,gb", Unit("data", 1e9, "Go"))
        add("to,tb", Unit("data", 1e12, "To"))
        add("c,°c,celsius,degre,degres,degré,degrés", Unit("temp", 0.0, "°C"))
        add("f,°f,fahrenheit", Unit("temp", 1.0, "°F"))
        add("k,kelvin", Unit("temp", 2.0, "K"))
        add("h,heure,heures", Unit("time", 3600.0, "h"))
        add("min,minute,minutes", Unit("time", 60.0, "min"))
        add("s,sec,seconde,secondes", Unit("time", 1.0, "s"))
        add("j,jour,jours", Unit("time", 86400.0, "jours"))
        add("semaine,semaines", Unit("time", 604800.0, "semaines"))
    }

    private fun toCelsius(v: Double, u: Unit) = when (u.factor) { 1.0 -> (v - 32) / 1.8; 2.0 -> v - 273.15; else -> v }
    private fun fromCelsius(c: Double, u: Unit) = when (u.factor) { 1.0 -> c * 1.8 + 32; 2.0 -> c + 273.15; else -> c }

    fun number(v: Double): String {
        val abs = kotlin.math.abs(v)
        val digits = when { abs >= 1000 -> 0; abs >= 100 -> 1; abs >= 1 -> 2; else -> 4 }
        return String.format(FR, "%,.${digits}f", v).let { if (digits > 0) it.trimEnd('0').trimEnd(',') else it }
    }

    private val CONVERSION = Regex("""^(-?\d+(?:[.,]\d+)?)\s*([\p{L}°/]+(?:/h)?)\s+(?:en|in|to|=|->|vers)\s+([\p{L}°/]+(?:/h)?)$""")

    private fun convert(query: String): Action? {
        val m = CONVERSION.find(query.trim().lowercase(FR)) ?: return null
        val value = m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
        val from = UNITS[m.groupValues[2]] ?: return null
        val to = UNITS[m.groupValues[3]] ?: return null
        if (from.dimension != to.dimension || from === to) return null
        val result = if (from.dimension == "temp") fromCelsius(toCelsius(value, from), to) else value * from.factor / to.factor
        val text = "${number(value)} ${from.label} = ${number(result)} ${to.label}"
        return Action(Kind.CONVERT, text, "Conversion · touche pour copier", number(result)) { null }
    }

    // ---------- Currencies ----------

    private val CURRENCIES: Map<String, String> = buildMap {
        fun add(names: String, code: String) = names.split(",").forEach { put(it.trim(), code) }
        add("€,eur,euro,euros", "EUR"); add("$,usd,dollar,dollars,dollars us", "USD"); add("£,gbp,sterling", "GBP")
        add("chf,franc suisse,francs suisses", "CHF"); add("¥,jpy,yen,yens", "JPY"); add("cad,dollar canadien,dollars canadiens", "CAD")
        add("aud,dollar australien,dollars australiens", "AUD"); add("cny,yuan,yuans", "CNY"); add("try,livre turque,livres turques", "TRY")
        add("sek,couronne suedoise", "SEK"); add("nok,couronne norvegienne", "NOK"); add("dkk,couronne danoise", "DKK")
        add("pln,zloty,zlotys", "PLN"); add("czk,couronne tcheque", "CZK"); add("huf,forint,forints", "HUF"); add("ron,leu", "RON")
        add("inr,roupie,roupies", "INR"); add("brl,real,reais", "BRL"); add("mxn,peso mexicain", "MXN"); add("krw,won,wons", "KRW")
        add("thb,baht", "THB"); add("zar,rand", "ZAR"); add("hkd", "HKD"); add("sgd", "SGD"); add("ils,shekel,shekels", "ILS")
        add("nzd", "NZD"); add("isk", "ISK"); add("bgn", "BGN"); add("idr,roupie indonesienne", "IDR"); add("myr,ringgit", "MYR"); add("php", "PHP")
    }

    data class CurrencyQuery(val amount: Double, val from: String, val to: String)

    private val MONEY = Regex("""^([€$£¥])?\s*(\d+(?:[.,]\d+)?)\s*([\p{L}€$£¥ ]*?)\s+(?:en|in|to|=|->|vers)\s+([\p{L}€$£¥ ]+)$""")

    fun currencyQuery(query: String): CurrencyQuery? {
        val m = MONEY.find(query.trim().lowercase(FR).normalizedForSearch()) ?: return null
        val amount = m.groupValues[2].replace(',', '.').toDoubleOrNull() ?: return null
        val from = CURRENCIES[m.groupValues[1].ifEmpty { m.groupValues[3].trim() }] ?: return null
        val to = CURRENCIES[m.groupValues[4].trim()] ?: return null
        return if (from == to) null else CurrencyQuery(amount, from, to)
    }

    private val rates = HashMap<String, Pair<Double, Long>>()

    /** Converts with the European Central Bank rates (frankfurter.dev, free), cached for 6 hours. */
    suspend fun currency(q: CurrencyQuery): Action? = withContext(Dispatchers.IO) {
        val key = q.from + q.to
        val cached = synchronized(rates) { rates[key] }?.takeIf { System.currentTimeMillis() - it.second < 6 * 3_600_000L }?.first
        val rate = cached ?: runCatching {
            val c = (URL("https://api.frankfurter.dev/v1/latest?base=${q.from}&symbols=${q.to}").openConnection() as HttpURLConnection)
                .apply { connectTimeout = 6000; readTimeout = 6000; setRequestProperty("User-Agent", "FoldLauncherPro") }
            JSONObject(c.inputStream.bufferedReader().use { it.readText() }).getJSONObject("rates").getDouble(q.to)
        }.getOrNull()?.also { synchronized(rates) { rates[key] = it to System.currentTimeMillis() } } ?: return@withContext null
        val result = q.amount * rate
        Action(Kind.CURRENCY, "${number(q.amount)} ${q.from} = ${number(result)} ${q.to}",
            "Taux BCE · 1 ${q.from} = ${number(rate)} ${q.to} · touche pour copier", number(result)) { null }
    }

    // ---------- Contacts ----------

    data class Person(val name: String, val number: String)

    /** "appeler Maman" → (call, "maman"); "sms Léa" → (sms, "léa"). */
    fun contactQuery(query: String): Pair<Kind, String>? {
        val t = query.trim()
        Regex("""^(?:appeler|appelle|t[ée]l[ée]phoner(?:\s+[àa])?|tel|call)\s+(.+)$""", RegexOption.IGNORE_CASE).find(t)?.let { return Kind.CALL to it.groupValues[1] }
        Regex("""^(?:sms|texto|message|[ée]crire(?:\s+[àa])?)\s+(.+)$""", RegexOption.IGNORE_CASE).find(t)?.let { return Kind.SMS to it.groupValues[1] }
        return null
    }

    suspend fun people(context: Context, name: String): List<Person> = withContext(Dispatchers.IO) {
        if (name.isBlank() || context.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return@withContext emptyList()
        runCatching {
            val uri = Uri.withAppendedPath(ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI, Uri.encode(name.trim()))
            val out = LinkedHashMap<String, Person>()
            context.contentResolver.query(uri, arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
                null, null, null)?.use { c ->
                while (c.moveToNext() && out.size < 3) {
                    val n = c.getString(0) ?: continue; val num = c.getString(1) ?: continue
                    out.putIfAbsent(n, Person(n, num))
                }
            }
            out.values.toList()
        }.getOrDefault(emptyList())
    }

    fun contactAction(kind: Kind, p: Person) = if (kind == Kind.CALL)
        Action(Kind.CALL, "Appeler ${p.name}", p.number) { c -> c.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(p.number))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); null }
    else Action(Kind.SMS, "Écrire à ${p.name}", p.number) { c -> c.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(p.number))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); null }

    // ---------- Everything that needs no network or contacts ----------

    fun instant(query: String): List<Action> {
        val t = query.trim()
        if (t.length < 3) return emptyList()
        val out = ArrayList<Action>()
        Regex("""^(?:minuteur|timer|chrono|compte\s+[àa]\s+rebours)\s+(?:de\s+)?(.+)$""", RegexOption.IGNORE_CASE).find(t)?.let { m ->
            parseDuration(m.groupValues[1])?.takeIf { it in 1..86_399 }?.let { seconds ->
                out += Action(Kind.TIMER, "Minuteur de ${durationLabel(seconds)}", "Démarre tout de suite dans l’appli Horloge") { c ->
                    c.startActivity(Intent(AlarmClock.ACTION_SET_TIMER).putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                        .putExtra(AlarmClock.EXTRA_MESSAGE, "Fold Launcher Pro").putExtra(AlarmClock.EXTRA_SKIP_UI, true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    "Minuteur de ${durationLabel(seconds)} lancé"
                }
            }
        }
        Regex("""^(?:alarme|r[ée]veil|r[ée]veille[- ]moi)\s+(?:[àa]\s+|pour\s+)?(\d{1,2})\s*(?:h|:|heures?)\s*(\d{2})?$""", RegexOption.IGNORE_CASE).find(t)?.let { m ->
            val h = m.groupValues[1].toInt(); val min = m.groupValues[2].toIntOrNull() ?: 0
            if (h in 0..23 && min in 0..59) {
                val label = String.format(FR, "%d:%02d", h, min)
                out += Action(Kind.ALARM, "Alarme à $label", "Ajoutée dans l’appli Horloge") { c ->
                    c.startActivity(Intent(AlarmClock.ACTION_SET_ALARM).putExtra(AlarmClock.EXTRA_HOUR, h).putExtra(AlarmClock.EXTRA_MINUTES, min)
                        .putExtra(AlarmClock.EXTRA_SKIP_UI, true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    "Alarme réglée à $label"
                }
            }
        }
        Regex("""^(?:rappel|rappelle[- ]moi(?:\s+de)?|[àa]\s+faire|todo|t[âa]che)\s*:?\s+(.+)$""", RegexOption.IGNORE_CASE).find(t)?.let { m ->
            val text = m.groupValues[1].trim().replaceFirstChar { it.titlecase(FR) }
            out += Action(Kind.REMINDER, "Ajouter « $text »", "Dans la carte Rappels de la page Aujourd’hui") { c ->
                TaskStore.add(c, text); "Ajouté aux rappels"
            }
        }
        convert(t)?.let { out += it }
        return out
    }
}
