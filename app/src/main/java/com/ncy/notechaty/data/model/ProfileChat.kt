package com.ncy.notechaty.data.model

/** A chat together with the profile it lives in, for lists that span profiles. */
data class ProfileChat(val profileId: String, val chat: Chat)
