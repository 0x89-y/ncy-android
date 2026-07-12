package com.ncy.notechaty.data.sync.event

import com.ncy.notechaty.data.model.Profile

/** Chooses one local profile for historical remote profile stamps. */
object ProfileRoutingPolicy {
    fun chooseProfileId(
        profiles: List<Profile>,
        activeProfileId: String,
        remoteProfileId: String,
        remoteProfileName: String,
    ): String? {
        val exact = profiles.firstOrNull { it.id == remoteProfileId }
        val name = remoteProfileName.trim()
        if (name.isNotEmpty()) {
            val sameName = profiles.filter { it.name.equals(name, ignoreCase = true) }
            if (sameName.size == 1 && !sameName.single().isProtected) {
                return sameName.single().id
            }
            // Reinstall generations of the default unprotected profile commonly
            // share a name but have different UUIDs. Merge those into the active
            // matching profile (or the oldest stable one). Never merge protected
            // profiles by name: equal labels do not imply equal PIN boundaries.
            if (sameName.size > 1 && sameName.none { it.isProtected }) {
                return sameName.firstOrNull { it.id == activeProfileId }?.id
                    ?: sameName.minWith(compareBy<Profile> { it.createdAt }.thenBy { it.id }).id
            }
        }
        return exact?.id
    }
}
