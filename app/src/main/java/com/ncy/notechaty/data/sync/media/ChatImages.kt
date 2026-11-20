package com.ncy.notechaty.data.sync.media

import java.io.File

/**
 * Finds a chat's picture or background on disk.
 *
 * Chats name them `avatar/<hash>.jpg` or `background/<hash>.jpg`, and that is where
 * a picture set on this device is written. One set on another device arrives as an
 * ordinary image blob, which is stored under `images/<hash>.jpg`, so opening the
 * named path alone missed every picture that came through sync.
 */
object ChatImages {
    fun resolve(chatDir: File, reference: String?): File? {
        if (reference.isNullOrBlank()) return null
        File(chatDir, reference).takeIf { it.isFile }?.let { return it }
        val hash = MediaHash.extractHash(reference) ?: return null
        return listOf("images", "avatar", "background")
            .map { File(File(chatDir, it), "$hash.jpg") }
            .firstOrNull { it.isFile }
    }
}
