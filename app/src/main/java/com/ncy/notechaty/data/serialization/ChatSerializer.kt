package com.ncy.notechaty.data.serialization

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.ncy.notechaty.data.model.ChatData
import com.ncy.notechaty.data.model.normalizedForCompatibility
import java.io.File
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

class ChatSerializer {
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    fun serialize(chatData: ChatData): String {
        return gson.toJson(chatData)
    }

    fun deserialize(json: String): ChatData {
        return requireNotNull(gson.fromJson(json, ChatData::class.java)) {
            "Chat JSON was empty or null"
        }.normalizedForCompatibility()
    }

    fun writeToFile(file: File, chatData: ChatData) {
        file.parentFile?.mkdirs()
        val temp = File(file.parentFile, ".${file.name}.${UUID.randomUUID()}.tmp")
        try {
            temp.writeText(serialize(chatData))
            replaceFile(temp, file)
        } finally {
            temp.delete()
        }
    }

    fun readFromFile(file: File): ChatData {
        return deserialize(file.readText())
    }

    private fun replaceFile(temp: File, target: File) {
        var atomic = true
        var lastError: IOException? = null
        repeat(20) { attempt ->
            try {
                val options = if (atomic) {
                    arrayOf(StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                } else {
                    arrayOf(StandardCopyOption.REPLACE_EXISTING)
                }
                Files.move(temp.toPath(), target.toPath(), *options)
                return
            } catch (_: AtomicMoveNotSupportedException) {
                atomic = false
            } catch (error: IOException) {
                lastError = error
                if (attempt < 19) Thread.sleep(2)
            }
        }
        throw lastError ?: IOException("Could not replace ${target.absolutePath}")
    }
}
