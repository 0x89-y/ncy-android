package com.ncy.notechaty.util

import android.content.Context
import android.net.Uri
import java.io.File

object FileUtils {
    fun copyUriToFile(context: Context, uri: Uri, targetFile: File): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
