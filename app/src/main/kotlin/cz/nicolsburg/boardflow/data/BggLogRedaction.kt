package cz.nicolsburg.boardflow.data

/** Form-encoded (`credentials%5Bpassword%5D=...`) and JSON (`"password":"..."`) password fields. */
private val FORM_PASSWORD = Regex("""((?:credentials(?:%5B|\[))?password(?:%5D|\])?=)[^&\s]*""", RegexOption.IGNORE_CASE)
private val JSON_PASSWORD = Regex(""""password"\s*:\s*"[^"]*"""", RegexOption.IGNORE_CASE)

/** Keeps the BGG password out of the debug HTTP log, which prints request bodies. */
internal fun redactBggPassword(line: String): String = line
    .replace(FORM_PASSWORD, "$1<redacted>")
    .replace(JSON_PASSWORD, "\"password\":\"<redacted>\"")
