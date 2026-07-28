package com.ncy.notechaty.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ncy.notechaty.data.model.Profile
import com.ncy.notechaty.security.PinHasher
import com.ncy.notechaty.data.sync.event.ChatEventHistory
import com.ncy.notechaty.data.sync.event.ProfileRoutingPolicy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ncy_profiles", Context.MODE_PRIVATE)

    private val gson = Gson()

    private val _profiles = MutableStateFlow<List<Profile>>(emptyList())
    val profiles: StateFlow<List<Profile>> = _profiles.asStateFlow()

    private val _activeProfileId = MutableStateFlow("")
    val activeProfileId: StateFlow<String> = _activeProfileId.asStateFlow()

    init {
        load()
    }

    private fun load() {
        val json = prefs.getString(KEY_PROFILES, null)
        val loaded: List<Profile> = if (json != null) {
            try {
                gson.fromJson(json, object : TypeToken<List<Profile>>() {}.type) ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }
        } else emptyList()

        _profiles.value = loaded
        val savedActive = prefs.getString(KEY_ACTIVE, null)
        _activeProfileId.value = when {
            savedActive != null && loaded.any { it.id == savedActive } -> savedActive
            loaded.isNotEmpty() -> loaded.first().id
            else -> ""
        }
    }

    private fun persist() {
        prefs.edit()
            .putString(KEY_PROFILES, gson.toJson(_profiles.value))
            .putString(KEY_ACTIVE, _activeProfileId.value.ifEmpty { null })
            .apply()
    }

    fun activeProfile(): Profile? =
        _profiles.value.firstOrNull { it.id == _activeProfileId.value }

    fun profileById(id: String): Profile? =
        _profiles.value.firstOrNull { it.id == id }

    /** Bootstrap: ensure at least one profile exists. Used by migration. */
    fun ensureDefault(name: String = "Personal"): Profile {
        val existing = _profiles.value.firstOrNull()
        if (existing != null) {
            if (_activeProfileId.value.isEmpty()) {
                _activeProfileId.value = existing.id
                persist()
            }
            return existing
        }
        val profile = Profile(id = UUID.randomUUID().toString(), name = name)
        _profiles.value = listOf(profile)
        _activeProfileId.value = profile.id
        persist()
        return profile
    }

    /** Used by migration only — installs an explicit profile (with a known id) and sets it active. */
    fun installInitial(profile: Profile) {
        if (_profiles.value.any { it.id == profile.id }) return
        _profiles.value = _profiles.value + profile
        if (_activeProfileId.value.isEmpty()) _activeProfileId.value = profile.id
        persist()
    }

    fun createProfile(name: String): Profile {
        val trimmed = name.trim().ifEmpty { "Profile ${_profiles.value.size + 1}" }
        val profile = Profile(id = UUID.randomUUID().toString(), name = trimmed)
        _profiles.value = _profiles.value + profile
        // Create the directory eagerly so the profile is "real" on disk.
        profileDir(profile.id).mkdirs()
        persist()
        return profile
    }

    fun renameProfile(id: String, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        _profiles.value = _profiles.value.map {
            if (it.id == id) it.copy(name = trimmed) else it
        }
        persist()
    }

    fun deleteProfile(id: String) {
        val current = _profiles.value
        require(current.size > 1) { "Cannot delete the last profile" }
        if (id == _activeProfileId.value) {
            val next = current.first { it.id != id }
            _activeProfileId.value = next.id
        }
        _profiles.value = current.filter { it.id != id }
        profileDir(id).deleteRecursively()
        persist()
    }

    /**
     * Backup restore: add any profiles from a backup manifest that this device
     * doesn't already have. Existing profiles (matched by id) are left untouched —
     * never clobber the user's current registry. Does not change the active
     * profile. Idempotent.
     */
    fun registerImported(imported: List<Profile>) {
        if (imported.isEmpty()) return
        val haveIds = _profiles.value.mapTo(HashSet()) { it.id }
        val toAdd = imported.filter { it.id !in haveIds }
        if (toAdd.isEmpty()) return
        _profiles.value = _profiles.value + toAdd
        persist()
    }

    /**
     * Backup restore fallback: make sure a profile with [id] exists so restored
     * `profile_<id>/` chat directories aren't orphaned. Used for legacy backups
     * with no manifest (so no name is known). No-op if already registered.
     */
    fun ensureProfileExists(id: String, fallbackName: String) {
        if (id.isBlank()) return
        if (_profiles.value.any { it.id == id }) return
        _profiles.value = _profiles.value + Profile(id = id, name = fallbackName)
        persist()
    }

    /**
     * Rebuild profile-registry entries when their directories survived but the
     * SharedPreferences registry did not. Chat/event data is authoritative here:
     * a directory is recovered only when it actually contains at least one chat.
     */
    @Synchronized
    fun recoverOrphanedProfileDirectories() {
        val root = rootDir()
        val knownIds = _profiles.value.mapTo(HashSet()) { it.id }
        val orphanDirs = root.listFiles()
            ?.filter { dir ->
                dir.isDirectory && dir.name.startsWith("profile_") &&
                    dir.name.removePrefix("profile_") !in knownIds &&
                    dir.listFiles()?.any {
                        it.isDirectory && it.name.startsWith("chat_")
                    } == true
            }
            .orEmpty()
        if (orphanDirs.isEmpty()) return

        val active = activeProfile()
        val activeHasChats = active?.let { profile ->
            profileDir(profile.id).listFiles()?.any {
                it.isDirectory && it.name.startsWith("chat_")
            } == true
        } ?: false
        val fallbackName = active?.name?.takeIf { !activeHasChats } ?: "Recovered profile"

        val recovered = orphanDirs.mapNotNull { dir ->
            val id = dir.name.removePrefix("profile_").takeIf(::isSafeProfileId)
                ?: return@mapNotNull null
            val eventName = dir.listFiles()
                ?.asSequence()
                ?.filter { it.isDirectory && it.name.startsWith("chat_") }
                ?.flatMap { ChatEventHistory.read(it).asSequence() }
                ?.map { it.profileName.trim() }
                ?.firstOrNull { it.isNotEmpty() }
            Profile(id = id, name = eventName ?: fallbackName)
        }
        if (recovered.isEmpty()) return

        _profiles.value = _profiles.value + recovered
        if (!activeHasChats) {
            _activeProfileId.value = recovered.maxBy { profile ->
                profileDir(profile.id).listFiles()?.count {
                    it.isDirectory && it.name.startsWith("chat_")
                } ?: 0
            }.id
        }
        persist()
        Log.i(
            "ProfileRepo",
            "Recovered ${recovered.size} profile directories; active=${_activeProfileId.value.take(8)}",
        )
    }

    /** Resolve a remote profile stamp without multiplying reinstall generations. */
    @Synchronized
    fun resolveSyncedProfile(remoteId: String, remoteName: String): Profile {
        val chosen = ProfileRoutingPolicy.chooseProfileId(
            _profiles.value,
            _activeProfileId.value,
            remoteId,
            remoteName,
        )?.let(::profileById)
        if (chosen != null) return chosen
        val exact = profileById(remoteId)
        if (exact != null) return exact
        val id = remoteId.takeIf { it.isNotBlank() } ?: return ensureDefault()
        val profile = Profile(id = id, name = remoteName.ifBlank { "Synced profile ${id.take(8)}" })
        _profiles.value = _profiles.value + profile
        persist()
        return profile
    }

    /**
     * Backup restore: drop a stale profile that has no chats and isn't the
     * active one — typically the fresh-install default left behind when the
     * user imports a manifest-rich backup. No-op if the profile is active,
     * unknown, or non-empty.
     */
    fun removeOrphanProfile(id: String) {
        if (id.isBlank()) return
        if (_activeProfileId.value == id) return
        if (_profiles.value.none { it.id == id }) return
        _profiles.value = _profiles.value.filter { it.id != id }
        profileDir(id).deleteRecursively()
        persist()
    }

    fun setActive(id: String) {
        if (_profiles.value.none { it.id == id }) return
        if (_activeProfileId.value == id) return
        _activeProfileId.value = id
        persist()
    }

    fun setProfilePin(id: String, pin: String) {
        val hashed = PinHasher.newHash(pin)
        _profiles.value = _profiles.value.map {
            if (it.id == id) it.copy(pinHash = hashed.hash, pinSalt = hashed.saltB64) else it
        }
        persist()
    }

    fun clearProfilePin(id: String) {
        _profiles.value = _profiles.value.map {
            if (it.id == id) it.copy(pinHash = null, pinSalt = null) else it
        }
        persist()
    }

    fun setProfileIcon(id: String, iconKey: String) {
        _profiles.value = _profiles.value.map {
            if (it.id == id) it.copy(iconKey = iconKey) else it
        }
        persist()
    }

    fun verifyProfilePin(id: String, pin: String): Boolean {
        val profile = _profiles.value.firstOrNull { it.id == id } ?: return false
        val hash = profile.pinHash ?: return false
        val salt = profile.pinSalt ?: return false
        if (!PinHasher.verify(pin, hash, salt)) return false
        // Re-write legacy bare-SHA-256 hashes now that we hold the plaintext PIN;
        // see PinHasher. `setProfilePin` persists, so this is self-contained.
        if (PinHasher.needsRehash(hash)) setProfilePin(id, pin)
        return true
    }

    /** Walk every profile directory to find the one owning a given chat id. Profile-agnostic. */
    fun findProfileForChat(chatId: String): String? {
        val root = rootDir()
        if (!root.exists()) return null
        return root.listFiles()
            ?.firstOrNull { dir ->
                dir.isDirectory && dir.name.startsWith("profile_") &&
                    File(dir, "chat_$chatId").exists()
            }
            ?.name
            ?.removePrefix("profile_")
    }

    fun rootDir(): File {
        val dir = File(context.filesDir, "NoteChaty")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun profileDir(id: String): File {
        val dir = File(rootDir(), "profile_$id")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    companion object {
        private const val KEY_PROFILES = "profiles"
        private const val KEY_ACTIVE = "active_profile_id"
    }

    private fun isSafeProfileId(id: String): Boolean =
        id.isNotEmpty() && id.length <= 128 &&
            id.all { it.isLetterOrDigit() || it == '-' || it == '_' }
}
