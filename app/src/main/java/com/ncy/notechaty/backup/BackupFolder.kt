package com.ncy.notechaty.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import java.io.File

/**
 * The folder the user picked for automatic backups, reached through the storage
 * access framework. Backups used to go into the app's own storage, where nothing
 * could read them back and they vanished with the app — no use when the phone or
 * the app is what is lost.
 */
object BackupFolder {

    private const val PREFIX = "ncy_backup_"
    private const val KEEP = 5

    /** Keep access to [tree] across restarts, and drop it for the folder it replaces. */
    fun remember(context: Context, tree: Uri, previous: Uri?) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(tree, flags)
        if (previous != null && previous != tree) {
            runCatching { context.contentResolver.releasePersistableUriPermission(previous, flags) }
        }
    }

    /** Whether Ncy may still write there: the folder can be deleted or access revoked. */
    fun isUsable(context: Context, tree: Uri): Boolean =
        context.contentResolver.persistedUriPermissions.any { it.uri == tree && it.isWritePermission } &&
            displayName(context, tree) != null

    /** The folder's name as the user sees it, or null when it cannot be reached. */
    fun displayName(context: Context, tree: Uri): String? = runCatching {
        val document = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        context.contentResolver.query(
            document, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null,
        )?.use { if (it.moveToFirst()) it.getString(0) else null }
    }.getOrNull()

    /** Copy [source] into the folder as [name], then keep only the newest backups there. */
    fun store(context: Context, tree: Uri, name: String, source: File) {
        val resolver = context.contentResolver
        val parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val target = DocumentsContract.createDocument(resolver, parent, "application/zip", name)
            ?: error("Could not create $name in the backup folder")
        try {
            resolver.openOutputStream(target, "w")?.use { out ->
                source.inputStream().use { it.copyTo(out) }
            } ?: error("Could not write $name")
        } catch (e: Exception) {
            // Leave no half-written backup to be mistaken for a good one.
            runCatching { DocumentsContract.deleteDocument(resolver, target) }
            throw e
        }
        prune(context, tree)
    }

    /** Delete all but the [KEEP] newest of Ncy's own backups; other files are left alone. */
    private fun prune(context: Context, tree: Uri) {
        val resolver = context.contentResolver
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val backups = mutableListOf<Pair<String, String>>() // document id, name
        resolver.query(
            children,
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null, null, null,
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(1) ?: continue
                if (name.startsWith(PREFIX) && name.endsWith(".zip")) backups += cursor.getString(0) to name
            }
        }
        // Names carry a sortable timestamp, which unlike modification times survives
        // the file being copied elsewhere and back.
        backups.sortedByDescending { it.second }.drop(KEEP).forEach { (id, _) ->
            runCatching { DocumentsContract.deleteDocument(resolver, DocumentsContract.buildDocumentUriUsingTree(tree, id)) }
        }
    }

    fun fileName(timestamp: String): String = "$PREFIX$timestamp.zip"
}
