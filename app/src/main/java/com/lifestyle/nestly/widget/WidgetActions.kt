package com.lifestyle.nestly.widget

import android.content.Context
import android.content.Intent
import com.lifestyle.nestly.MainActivity
import com.lifestyle.nestly.ui.entry.EntryKind

object WidgetActions {
    const val EXTRA_OPEN_ENTRY_KIND = "open_entry_kind"

    fun openEntryIntent(context: Context, kind: EntryKind): Intent =
        Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_ENTRY_KIND, kind.name)
        }

    fun entryKindFrom(intent: Intent?): EntryKind? {
        val name = intent?.getStringExtra(EXTRA_OPEN_ENTRY_KIND) ?: return null
        return runCatching { EntryKind.valueOf(name) }.getOrNull()
    }
}
