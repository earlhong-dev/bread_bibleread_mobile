package com.bibleread.bread.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages Bible translation selection.
 *
 * Convention:
 *   - All pre-built translation DBs live in assets/translations/
 *   - Each .db file is named after its translation code, e.g. "mbbtag05.db", "kjv.db"
 *   - The display name shown in the picker is derived from the filename (minus .db)
 *     but you can override it in DISPLAY_NAMES below.
 *
 * To add a new translation:
 *   1. Parse its XML (put the xml in assets/, run once → it exports a .db)
 *   2. Rename the exported .db to match your desired code (e.g. "kjv.db")
 *   3. Drop the .db into assets/translations/
 *   4. Optionally add a friendly display name in DISPLAY_NAMES below
 */
object TranslationManager {

    private const val PREFS_NAME = "bread_prefs"
    private const val KEY_ACTIVE_TRANSLATION = "active_translation"
    const val TRANSLATIONS_ASSET_DIR = "translations"

    /**
     * Human-readable display names keyed by db filename (without .db extension).
     * If a translation isn't listed here, the filename itself is used as the label.
     */
    val DISPLAY_NAMES = mapOf(
        "mbbtag05" to "MBB",
        "kjv"      to "KJV",
        "niv"      to "NIV",
        "esv"      to "ESV"
    )

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Returns a list of all .db files found in assets/translations/.
     * Each entry is the bare filename without the .db extension, e.g. ["mbbtag05", "kjv"].
     */
    fun getAvailableTranslations(context: Context): List<String> {
        return try {
            context.assets.list(TRANSLATIONS_ASSET_DIR)
                ?.filter { it.endsWith(".db") }
                ?.map { it.removeSuffix(".db") }
                ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Returns the currently active translation code.
     * Defaults to the first available translation, or "mbbtag05" as a last resort.
     */
    fun getActiveTranslation(context: Context): String {
        val saved = prefs(context).getString(KEY_ACTIVE_TRANSLATION, null)
        val available = getAvailableTranslations(context)

        // If saved value is still valid, use it
        if (saved != null && available.contains(saved)) return saved

        // Otherwise fall back to first available
        return available.firstOrNull() ?: "mbbtag05"
    }

    /** Persists the user's translation choice. */
    fun setActiveTranslation(context: Context, translationCode: String) {
        prefs(context).edit()
            .putString(KEY_ACTIVE_TRANSLATION, translationCode)
            .apply()
    }

    /** Returns the asset path for a given translation code. */
    fun assetPath(translationCode: String) =
        "$TRANSLATIONS_ASSET_DIR/$translationCode.db"

    /** Returns the Room DB name (used as the on-device database filename). */
    fun dbName(translationCode: String) = "$translationCode.db"

    /** Returns the display label for a translation code. */
    fun displayName(translationCode: String): String =
        DISPLAY_NAMES[translationCode] ?: translationCode.uppercase()
}
