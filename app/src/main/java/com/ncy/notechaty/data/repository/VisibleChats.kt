package com.ncy.notechaty.data.repository

import com.ncy.notechaty.data.model.Profile
import com.ncy.notechaty.data.model.ProfileChat
import com.ncy.notechaty.security.ProfileLockState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The chats the home screen works with: the active profile's, or under "All chats"
 * those of every profile that is open. One definition, so the list, Favorites,
 * Reminders and search never disagree about what is shown.
 */
@Singleton
class VisibleChats @Inject constructor(
    repository: ChatRepository,
    profileRepository: ProfileRepository,
    profileLockState: ProfileLockState,
    settingsRepository: SettingsRepository,
) {
    val chats: Flow<List<ProfileChat>> = combine(
        combine(
            repository.observeChats(),
            repository.observeAllProfileChats(),
            profileRepository.showAllProfiles,
        ) { active, all, showAll -> Triple(active, all, showAll) },
        profileRepository.activeProfileId,
        profileRepository.profiles,
        profileLockState.timestamps,
        settingsRepository.settings,
    ) { (active, all, showAll), activeId, profiles, unlockedAt, settings ->
        if (!showAll) active.map { ProfileChat(activeId, it) }
        else all.filter { chat ->
            isProfileOpen(
                profiles.firstOrNull { it.id == chat.profileId } ?: return@filter false,
                activeId,
                unlockedAt,
                settings.protectionGracePeriodMinutes,
            )
        }
    }

    companion object {
        /**
         * A protected profile's chats appear under "All chats" only while it is
         * unlocked: it is the active one (opened with its PIN), or its unlock is
         * still inside the grace period. A grace of zero unlocks for one switch
         * only, so then only the active one counts.
         */
        fun isProfileOpen(
            profile: Profile,
            activeProfileId: String,
            unlockedAt: Map<String, Long>,
            graceMinutes: Int,
            now: Long = System.currentTimeMillis(),
        ): Boolean {
            if (!profile.isProtected || profile.id == activeProfileId) return true
            if (graceMinutes <= 0) return false
            val at = unlockedAt[profile.id] ?: return false
            return now - at < graceMinutes * 60_000L
        }
    }
}
