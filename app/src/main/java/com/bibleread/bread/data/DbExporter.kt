package com.bibleread.bread.data

import android.content.Context
import android.util.Log

object DbExporter {

    private const val TAG = "DbExporter"

    /**
     * Logs the location of the parsed database so you can pull it from Device File Explorer.
     *
     * Location in Device File Explorer:
     *   data/data/com.bibleread.bread/databases/mbbtag05.db
     *
     * Right-click → Save As → drop it into app/src/main/assets/translations/
     */
    fun exportFromXml(context: Context, xmlFileName: String) {
        val dbName = TranslationManager.dbName(xmlFileName.removeSuffix(".xml"))
        val dbFile = context.getDatabasePath(dbName)

        if (dbFile.exists()) {
            Log.d(TAG, "✅ DB ready at: ${dbFile.absolutePath}")
            Log.d(TAG, "   Device File Explorer → data/data/com.bibleread.bread/databases/$dbName")
            Log.d(TAG, "   Right-click → Save As → put it in assets/translations/")
        } else {
            Log.e(TAG, "DB file not found: ${dbFile.absolutePath}")
        }
    }
}
