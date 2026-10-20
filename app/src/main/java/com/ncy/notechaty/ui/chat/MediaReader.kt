package com.ncy.notechaty.ui.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import java.io.File

/**
 * Turns a stored attachment into one the UI can show. In a protected chat the
 * stored file is sealed (§11.8), so it must be opened first; elsewhere it is the
 * file itself.
 */
fun interface MediaReader {
    /** A readable file, or null when it cannot be opened (locked, damaged). */
    suspend fun readable(file: File): File?
}

/** Unprotected chats: stored attachments are already plain. */
val PlainMediaReader = MediaReader { it }

val LocalMediaReader = staticCompositionLocalOf { PlainMediaReader }

/**
 * [file] made readable, recomputed when it changes or appears on disk. For a plain
 * chat that is [file] at once; a sealed one is null until it has been opened.
 */
@Composable
fun rememberReadable(file: File?, present: Boolean = file?.isFile == true): File? {
    val reader = LocalMediaReader.current
    if (file == null || !present) return null
    if (reader === PlainMediaReader) return file
    return produceState<File?>(initialValue = null, file, present, reader) {
        value = reader.readable(file)
    }.value
}
