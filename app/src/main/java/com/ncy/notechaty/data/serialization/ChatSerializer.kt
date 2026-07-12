package com.ncy.notechaty.data.serialization

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.ncy.notechaty.data.model.ChatData
import com.ncy.notechaty.data.model.normalizedForCompatibility
import java.io.File

class ChatSerializer {
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    fun serialize(chatData: ChatData): String {
        return gson.toJson(chatData)
    }

    fun deserialize(json: String): ChatData {
        return gson.fromJson(json, ChatData::class.java).normalizedForCompatibility()
    }

    fun writeToFile(file: File, chatData: ChatData) {
        file.writeText(serialize(chatData))
    }

    fun readFromFile(file: File): ChatData {
        return deserialize(file.readText())
    }
}
