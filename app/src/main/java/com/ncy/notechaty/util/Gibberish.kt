package com.ncy.notechaty.util

import kotlin.random.Random

private const val ALPHABET = "abcdefghjkmnpqrstuvwxyz23456789"

fun gibberishFor(chatId: String, length: Int = 10): String {
    val seed = chatId.hashCode().toLong()
    val rng = Random(seed)
    return buildString(length) {
        repeat(length) { append(ALPHABET[rng.nextInt(ALPHABET.length)]) }
    }
}
