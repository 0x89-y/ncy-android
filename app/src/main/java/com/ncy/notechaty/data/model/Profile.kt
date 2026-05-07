package com.ncy.notechaty.data.model

data class Profile(
    val id: String,
    val name: String,
    val iconKey: String? = "person",
    val pinHash: String? = null,
    val pinSalt: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val isProtected: Boolean get() = pinHash != null
}
